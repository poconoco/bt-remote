import socket
import _thread  # Use low level _thread instead of threading to support Micro Python

class TcpRcReceiver:
    def __init__(self, bind_ip='0.0.0.0', port=9876):
        self.server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

        self.server_socket.bind((bind_ip, port))
        self.server_socket.listen()
        print(f'Waiting for connections on {bind_ip}:{port}...')

        _thread.start_new_thread(self.start_server, ())

    def start_server(self):
        while True:  # Outer while, to accept reconnections
            client_socket, addr = self.server_socket.accept()
            print(f'Connection from {addr} established')

            while True:  # Inner while, to receive packets indefinitelly
                data = client_socket.recv(1024)
                if not data:
                    print(f'Connection from {addr} closed')
                    client_socket.close()
                    break

                print(f'Received packet: {data}')

if __name__ == '__main__':
    rc = TcpRcReceiver()

    print("Press Enter key to exit...") 
    input()    
