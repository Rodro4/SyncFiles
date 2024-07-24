import socket
import tkinter as tk
from tkinter import filedialog
import json

SERVER_PORT = 65432
CONFIG_FILE = 'config.json'

def load_config():
    try:
        with open(CONFIG_FILE, 'r') as file:
            config = json.load(file)
            return config.get('last_ip', ''), config.get('last_file', '')
    except FileNotFoundError:
        return '', ''

def save_config(ip, file_path):
    with open(CONFIG_FILE, 'w') as file:
        json.dump({'last_ip': ip, 'last_file': file_path}, file)

def apply_changes(file_path, changes):
    with open(file_path, 'r') as file:
        lines = file.readlines()

    lines = [line.strip() for line in lines]

    additions = []
    deletions = []

    for change in changes:
        if len(change) > 1:
            action = change[0]
            content = change[1:].strip()
            if action == '+':
                additions.append(content)
            elif action == '-':
                deletions.append(content)

    # Apply deletions
    for deletion in deletions:
        if deletion in lines:
            lines.remove(deletion)

    # Apply additions
    for addition in additions:
        lines.append(addition)

    # Filter out lines starting with + or -
    lines = [line for line in lines if not (line.startswith('+') or line.startswith('-'))]

    with open(file_path, 'w') as file:
        for line in lines:
            file.write(line + '\n')

    return additions, deletions

def sync_changes(file_path, client_socket, additions, deletions):
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

    with open(file_path, 'w') as file:
        for line in lines:
            file.write(line + '\n')

    # Send the updated file to the client
    writer = client_socket.makefile('w')
    for line in lines:
        writer.write(line + '\n')
    writer.flush()

def start_server(ip, file_path):
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as server_socket:
        server_socket.bind((ip, SERVER_PORT))
        server_socket.listen()
        print(f"Server listening on {ip}:{SERVER_PORT}")

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

                additions, deletions = apply_changes(file_path, changes)
                sync_changes(file_path, client_socket, additions, deletions)
                print(f"Applied changes and synced file with {client_address}")

def on_start():
    ip = ip_entry.get()
    file_path = file_entry.get()
    save_config(ip, file_path)
    start_button.config(state=tk.DISABLED)
    try:
        start_server(ip, file_path)
    except Exception as e:
        print(f"Error: {e}")
        start_button.config(state=tk.NORMAL)

# Load last used IP and file path
last_ip, last_file = load_config()

# Create GUI
root = tk.Tk()
root.title("Server Configuration")

tk.Label(root, text="Server IP:").grid(row=0, column=0, padx=10, pady=10)
ip_entry = tk.Entry(root)
ip_entry.grid(row=0, column=1, padx=10, pady=10)
ip_entry.insert(0, last_ip)

tk.Label(root, text="File Path:").grid(row=1, column=0, padx=10, pady=10)
file_entry = tk.Entry(root)
file_entry.grid(row=1, column=1, padx=10, pady=10)
file_entry.insert(0, last_file)

tk.Button(root, text="Browse", command=lambda: file_entry.insert(0, filedialog.askopenfilename())).grid(row=1, column=2, padx=10, pady=10)
start_button = tk.Button(root, text="Start Server", command=on_start)
start_button.grid(row=2, column=0, columnspan=3, padx=10, pady=10)

root.mainloop()
