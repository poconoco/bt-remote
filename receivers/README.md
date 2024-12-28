# Receiver backends and examples for the DIY RC - android remote control app.

## cpp

### BaseRcReceiver.h

An abstract class not to be used itself, but to be inherited by the other two below

### BtRcReceiver.h

A Bluetooth serial interface wrapper to work with DIY RC app.

It's a C++ template class that accepts serial interface object. To be used
with Bluetooth modules with serial interfaces. Accept objects like Serial, Serial1, 
etc global vars on the Arduino platform, or anything with similar semantics, like UART
object on Pi Pico platform. Any serial-like object with the following methods:
 - begin()
 - end
 - println()
 - available()
 - read()
   
Example usages:
 - https://github.com/poconoco/simple-rc-car - PlatformIO project, Arduino platform
 - https://github.com/poconoco/balancer - Android IDE project, Pi Pico platform, 
   code is a bit deprecated, see README.md there

### Esp32TcpRcClient.h

A TCP server in C++ that accepts connections from DIY RC app.

It is designed for ESP32 platform and uses its WiFiServer and WiFiClient classes to
crate a TCP server. Has similar public interface to get the controls state from the
RC app and transmit messages back. 

Example usage:
 - https://github.com/poconoco/esp32-cam-rc-control - a fork of the easytarget/esp32-cam-webserver
   project that has the Esp32TcpRcClient integrated to work with DIY RC and servo motors 
   simultaneously with serving the camera stream. Also controls the flashlight intensity with
   a slider on DIY RC. The RC app is also capable to conenct to the video stream serving 
   from this project, making it the ready to use platform for simple FPV bots.

## python

### tcp_rc_receiver.py

A TCP server in Python that accepts connection from DIY RC app.

Simple interface, similar to C++ counterpart. Also has embedded main routine,
so itself can be launched as a test app:

```
python tcp_rc_receiver.py
```

And will accept connections and in text mode display the received data from RC.
Teted from Windows on x86_64 and Linux on arm64, but supposed to work also on
Micro Python. Not tested though, sorry.

### rpi-servo-rc-example

An example usage of the TcpRcReceiver class from tcp_rc_receiver module to control
servo motors on Raspbery Pi with Servo Control shield based on PCA9685 I2C chip.
See the README.md in its folder for more information.

### rpi-camera-server-example

An example of camera server for Raspberry Pi that DIY RC can connect to. See the
README.md in its folder.
