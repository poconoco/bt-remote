import socket
import struct
import math
import time
import _thread  # Use low level _thread instead of threading to support Micro Python

class UdpRcReceiver:
    def __init__(self, bind_ip='0.0.0.0', port=9876, print_debug=False, timeout=1.0):
        self.port = port
        # Use SOCK_DGRAM for UDP
        self.server_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        self.remote_addr = None
        self.last_packet_time = 0
        self.timeout = timeout

        self.switches = [False, False, False, False, False, False, False, False]
        self.axes = [0, 0, 0, 0]
        self.sliders = [0, 0]
        self.orientation = [0, 0, 0]
        self.reserved = 0
        self.print_debug = print_debug
        self.connection_start = 0

        self.server_socket.bind((bind_ip, port))
        if self.print_debug:
            print(f'Waiting for UDP packets on {bind_ip}:{port}...')

        _thread.start_new_thread(self._start_server, ())

    def send(self, message):
        # We can only send if we have received a packet and know the remote address
        if self.is_connected():
            try:
                message = message.replace('\n', '\t') + '\n'
                # In UDP, we send via the server socket directed to the remote_addr
                self.server_socket.sendto(message.encode(), self.remote_addr)
            except Exception as e:
                pass # Connection resets aren't handled the same in UDP

    def get_port(self):
        return self.port

    def is_connected(self):
        if self.remote_addr is None:
            return False

        # In UDP, "connected" means we've received a packet recently
        if time.time() - self.last_packet_time > self.timeout:
            if self.print_debug:
                print(f'\nConnection from {self.remote_addr} timed out')
            self.remote_addr = None
            return False

        return True

    def get_switch_a(self):
        return self.switches[0]

    def get_switch_b(self):
        return self.switches[1]

    def get_switch_c(self):
        return self.switches[2]

    def get_switch_d(self):
        return self.switches[3]

    def get_switch_e(self):
        return self.switches[4]

    def get_switch_f(self):
        return self.switches[5]

    def get_switch_g(self):
        return self.switches[6]

    def get_switch_h(self):
        return self.switches[7]

    def get_x1(self):
        return self.axes[0]

    def get_y1(self):
        return self.axes[1]

    def get_x2(self):
        return self.axes[2]

    def get_y2(self):
        return self.axes[3]

    def get_slider_left(self):
        return self.sliders[0]

    def get_slider_right(self):
        return self.sliders[1]

    def get_pitch(self):
        return self.orientation[0]

    def get_roll(self):
        return self.orientation[1]

    def get_yaw(self):
        return self.orientation[2]

    def get_reserved(self):
        return self.reserved

    def get_connected_time(self):
        if not self.is_connected():
            return 0
        else:
            return time.time() - self.connection_start

    def _start_server(self):
        while True:
            try:
                # Receive packet and sender address
                data, addr = self.server_socket.recvfrom(1024)

                if not data:
                    continue

                # If this is a new sender, update state
                if self.remote_addr != addr:
                    self.remote_addr = addr
                    self.connection_start = time.time()
                    if self.print_debug:
                        print(f'\nReceiving UDP stream from {self.remote_addr}')

                # Update the watchdog timer
                self.last_packet_time = time.time()

                if not self._parse_packet(data):
                    if self.print_debug:
                        print(f'\nInvalid packet received: {data}')
            except Exception as e:
                if self.print_debug:
                    print(f'\nUDP Error: {e}')
                time.sleep(0.1)

    def _parse_packet(self, data):
        if len(data) < 15:
            if self.print_debug:
                print(f'Invalid len: {len(data)}')
            return False

        # Sometimes we get two packets at once (though less common in UDP than TCP)
        if len(data) > 15:
            data = data[-15:]

        if data[0] != ord('N') or data[1] != ord('O') or data[2] != ord('C'):
            if self.print_debug:
                print(f'Invalid header: {data[0:3]}')
            return False

        checksum = 0
        for i in range(0, 14):
            checksum ^= data[i]

        if checksum != data[14]:
            if self.print_debug:
                print(f'Invalid checksum')
            return False

        # Verifications done, now read the data
        signed_data = struct.unpack('11b', data[3:14])

        for i in range(0, 8):
            self.switches[i] = (signed_data[0] & (1 << i)) != 0

        for i in range(0, 4):
            self.axes[i] = signed_data[1 + i]

        for i in range(0, 2):
            self.sliders[i] = signed_data[5 + i]

        for i in range(0, 3):
            self.orientation[i] = signed_data[7 + i]

        self.reserved = signed_data[10]

        return True


def switch_to_str(bool_switch):
    return '+' if bool_switch else '-'


def axis_or_slider_to_str(value):
    normalized_value = value + 128

    str_slider_length = 9
    str_slider_scale_down = 256 / str_slider_length
    str_slider_position = math.floor(normalized_value / str_slider_scale_down)
    str_slider = ['=' for _ in range(str_slider_length)]
    str_slider[str_slider_position] = '0'

    return f'[{"".join(str_slider)}]'


def rc_state_to_str(receiver):
    switches = ''
    switches += switch_to_str(receiver.get_switch_a())
    switches += switch_to_str(receiver.get_switch_b())
    switches += switch_to_str(receiver.get_switch_c())
    switches += switch_to_str(receiver.get_switch_d())
    switches += switch_to_str(receiver.get_switch_e())
    switches += switch_to_str(receiver.get_switch_f())
    switches += switch_to_str(receiver.get_switch_g())
    switches += switch_to_str(receiver.get_switch_h())

    x1 = axis_or_slider_to_str(receiver.get_x1())
    y1 = axis_or_slider_to_str(receiver.get_y1())
    x2 = axis_or_slider_to_str(receiver.get_x2())
    y2 = axis_or_slider_to_str(receiver.get_y2())

    sl = axis_or_slider_to_str(receiver.get_slider_left())
    sr = axis_or_slider_to_str(receiver.get_slider_right())

    pitch = axis_or_slider_to_str(receiver.get_pitch())
    roll = axis_or_slider_to_str(receiver.get_roll())
    yaw = axis_or_slider_to_str(receiver.get_yaw())

    return f'J1: {x1}{y1} J2: {x2}{y2} SW: {switches} Sldr: {sl}{sr}\n' \
           f'O: {pitch}{roll}{yaw}'


if __name__ == '__main__':
    rc = UdpRcReceiver(print_debug=True)

    print("\nPress Ctrl+C key to exit...")
    while True:
        if rc.is_connected():
            print(f'\033[1F\033[J{rc_state_to_str(rc)}', end='', flush=True)
            rc.send(f'Live for: {round(rc.get_connected_time())}s\n[next line]')

        time.sleep(0.1)
