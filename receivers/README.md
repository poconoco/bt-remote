# Receiver backends for the android remote control app.


## cpp

A C++ template class that accepts serial interface object. To be used
with Bluetooth modules with serial interfaces. Accept objects like Serial, Serial1, 
etc global var on Arduino platform, or anything with similar semantics, like UART
object on Pi Pico. Any serial-like object with the following methods:
 - begin()
 - end
 - println()
 - available()
 - read()
   
Example usages:
 - https://github.com/poconoco/simple-rc-car (PlatformIO project, Arduino platform)
 - https://github.com/poconoco/balancer (Android IDE project, Pi Pico platform, 
                                         code is a bit deprecated, see README.md there)


