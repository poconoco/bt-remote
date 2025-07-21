This example is intended for Raspberry Pi, it requires Servo Driver hat with PCA9685, 
a 16-channel, 12-bit PWM. This controller communicates with Pi over i2c bus that needs
to be enabled in the raspi-config:

1) Run `sudo raspi-config`
2) Go to "Interface options"
3) Select "I2C"
4) Answer "Yes" to enable it
5) Use "Finish" button to exit the raspi-config

Then setup the python env. This manual assumes installed OS is Debian 12 bookworm
or similar, and has Python 3 installed by default. We need to use venv to not install
dependencies systemwide, python strongly discourages against it. So:

1) Change directory into the folder this file is located in

```
cd bt-remote/receivers/python/servo-rc-example
```

2) Create virtual environment

```
python3 -m venv .venv
```

3) Activate it (you'll need to do it each time you close and re-open the terminal session)

```
source .venv/bin/activate
```

4) Now you can install python dependencies, they will be installed only in the current 
   virtual environment, contained in the .venv folder. The main dependency is the adafruit
   python module to interface with the PCA9685 module. Also include the adafruit motor library
   which simplifies 

```
python -m pip -r requirements.txt
```

6) Now you are ready to launch the example

```
python rc_servo_test.py
```

It will show you the IP and port you should use in the DIY RC app to connect to, given that
the rpi is on the same network and its IP is reachable from the phone you run DIY RC from. 

