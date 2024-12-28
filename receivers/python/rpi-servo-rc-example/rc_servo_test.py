import os
import sys
import time
import board

from adafruit_motor import servo
from adafruit_pca9685 import PCA9685
from subprocess import check_output

# A hack to allow importing tcp_rc_receiver from the parent folder,
# don't do this in real projects
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

from tcp_rc_receiver import TcpRcReceiver

def clamp(x, min, max):
    if x < min:
        return min
    if x > max:
        return max
    return x

def run():

    # Init Servo controller attaced via I2C
    i2c = board.I2C()
    pca = PCA9685(i2c)
    pca.frequency = 50

    # Iniservos
    h_servo = servo.Servo(pca.channels[0])
    v_servo = servo.Servo(pca.channels[1])

    # Init RC receiver
    rc = TcpRcReceiver()
    h_pos = 90;
    v_pos = 90;

    # Guess the Raspberry Pi IPs
    ips = check_output(['hostname', '-I'], text=True).strip().split(' ')
    for ip in ips:
        print(f'Accepting RC connections on {ip}:{rc.get_port()}')

    # Run the main loop
    print('Ctrl+C to stop')
    try:
        while True:
            h_pos = clamp(h_pos + rc.get_x1()*0.005, 0, 180)
            v_pos = clamp(v_pos + rc.get_y1()*0.005, 0, 180)

            h_servo.angle = h_pos
            v_servo.angle = v_pos

            rc.send(f'  H: {int(h_pos)}\n  V: {int(v_pos)}')

            time.sleep(0.010)


    except KeyboardInterrupt:
        print('Stopped')
    finally:
        pca.deinit()

run()
