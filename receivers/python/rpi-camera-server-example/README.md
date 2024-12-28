This example is intended for Raspberry Pi, it demonstrates how to stream
the video that can be received and displayed by the DIY RC application.

Original code copyright is preserved in the source file, my changes are minor:
just to output the addresses to connect to in the console.

Example requires camera to be attached to the R-Pi. Tested with a standard R-Pi 
camera attached to a Raspberry Pi Zero 2W via CSI connector

Setup the python env first. This manual assumes installed OS is Debian 12 bookworm or 
similar, and has Python 3 installed by default. We will use venv to not install
dependencies systemwide

1) Change directory into the folder this file is located in

```
cd bt-remote/receivers/python/rpi-camera-server-example
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
   virtual environment, contained in the .venv folder

```
python -m pip install picamera2
```

6) Now you are ready to launch the example

```
python rc_servo_test.py
```

It will show you the IP and port you can use to watch the stream in the browser
or configure in the DIY RC app, given that the rpi is on the same network and its 
IP is reachable from the phone you run DIY RC from. 

