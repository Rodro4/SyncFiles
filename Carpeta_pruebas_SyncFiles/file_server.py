import socket
import os

SERVER_IP = '192.168.1.137'
SERVER_PORT = 65432
FILE_PATH = 'C:/Users/hllro/Documents/GitHub/SyncFiles/Carpeta_pruebas_SyncFiles/file_sync_prueba.txt' # Cambia esto por la ruta del archivo en tu PC

def apply_changes(file_path, changes):
    with open(file_path, 'r') as file:
        lines = file.readlines()

    for change in changes:
        action = change[0]
        content = change[1:].strip()
        if action == '+':
            lines.append(content + '\n')
        elif action == '-':
            lines = [line for line in lines if not line.strip() == content]
        elif action == '=':
            old, new = content.split('->')
            lines = [line.replace(old, new) if old in line else line for line in lines]

    with open(file_path, 'w') as file:
        file.writelines(lines)

def start_server():
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as server_socket:
        server_socket.bind((SERVER_IP, SERVER_PORT))
        server_socket.listen()
        print(f"Server listening on {SERVER_IP}:{SERVER_PORT}")
        
        while True:
            client_socket, client_address = server_socket.accept()
            with client_socket:
                print(f"Connected by {client_address}")
                changes = []
                while True:
                    data = client_socket.recv(1024)
                    if not data:
                        break
                    changes.append(data.decode('utf-8'))

                apply_changes(FILE_PATH, changes)
                print(f"Applied changes from {client_address}")

if __name__ == "__main__":
    start_server()
