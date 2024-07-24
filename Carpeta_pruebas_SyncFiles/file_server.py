import socket

SERVER_IP = '192.168.1.137'
SERVER_PORT = 65432
FILE_PATH = 'C:/Users/hllro/Documents/GitHub/SyncFiles/Carpeta_pruebas_SyncFiles/file_sync_prueba.txt'  # Cambia esto por la ruta del archivo en tu PC

def apply_changes(file_path, changes):
    with open(file_path, 'r') as file:
        lines = file.readlines()

    additions = []
    deletions = []
    modifications = []

    for change in changes:
        if len(change) > 1:
            action = change[0]
            content = change[1:].strip()
            if action == '+':
                additions.append(content)
            elif action == '-':
                deletions.append(content)
            elif action == '=':
                old_new = content.split('->')
                if len(old_new) == 2:
                    modifications.append((old_new[0].strip(), old_new[1].strip()))

    lines = [line.strip() for line in lines]

    # Apply deletions
    for deletion in deletions:
        if deletion in lines:
            lines.remove(deletion)

    # Apply modifications
    for old, new in modifications:
        lines = [new if line == old else line for line in lines]

    # Apply additions
    for addition in additions:
        lines.append(addition)

    # Filter out lines starting with +, - or =
    lines = [line for line in lines if not (line.startswith('+') or line.startswith('-') or line.startswith('='))]

    with open(file_path, 'w') as file:
        for line in lines:
            file.write(line + '\n')

    return additions, deletions, modifications

def sync_changes(file_path, client_socket, additions, deletions, modifications):
    with open(file_path, 'r') as file:
        lines = file.readlines()

    lines = [line.strip() for line in lines]

    # Apply additions
    for addition in additions:
        lines.append(addition)

    # Apply deletions
    for deletion in deletions:
        if deletion in lines:
            lines.remove(deletion)

    # Apply modifications
    for old, new in modifications:
        lines = [new if line == old else line for line in lines]

    with open(file_path, 'w') as file:
        for line in lines:
            file.write(line + '\n')

    writer = client_socket.makefile('w')
    for line in lines:
        writer.write(line + '\n')
    writer.flush()

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
                reader = client_socket.makefile('r')

                while True:
                    data = reader.readline()
                    if not data:
                        break
                    changes.append(data.strip())

                additions, deletions, modifications = apply_changes(FILE_PATH, changes)
                sync_changes(FILE_PATH, client_socket, additions, deletions, modifications)
                print(f"Applied changes and synced file with {client_address}")

if __name__ == "__main__":
    start_server()
