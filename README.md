# DIY Remote 

By Leonid Yurchenko, https://www.youtube.com/@nocomake

This is an Android application to connect to SPP bluetooth or TCP port and send commands.

- Includes corresponding code snippets to receive parse the commands on the receiver end, 
see the `receivers` folder

- Uses binary protocol, keeping packets size small and allowing high frequency 
  control

- Includes possibility to send string messages back to the Android app to be
  displayed. That may include debug information, stats, battery voltage, etc.

# Protocol

Remote sends control data in 15byte packets. Bytes in a packet:

- 3 first bytes are characters 'N', 'O', 'C' - this is header indicating a packet start
- 1 byte contains bitwise representation of 8 switches
- 2 bytes - X and Y axes of the left joystick. Each is a signed 8-bit integer, 0 when joystick is in the middle
- 2 bytes - X and Y axes of the right joystick
- 2 byte - left and Right slider values, also signed 8-bit integers
- 4 bytes - reserved for future usage
- 1 byte - XOR checksum of previous 14 bytes

![Packet diagram](packet.png)

Remote accepts string messages and displays them on the screen. Messages are 
divided by new line character (\n), and every new message completely erases
previous, so this is not a terminal or log, it's rather a remotely controlled
short string display. Since messages are divided by \n, to display a new line,
one should send a tab character (\t). so to get stats like this:

```
Voltage: 12
Temp: 12
Speed: 23
```

The remote should use line similar to the following:

```
bt->send("Voltage: "+vStr+"\tTemp: "+tStr+"\tSpeed: "+sStr);
```
