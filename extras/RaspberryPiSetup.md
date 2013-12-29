Raspberry Pi Setup
===================

The RaspberryPi is a $35 computer which works well as a brewery base

Example Wiring Diagram
==========

Here is an example circuit (which I currently use) In this example, for RaspberryPi, Pin 4 MUST be reserved for the one wire circuit, and there's a 4k7 pullup resistor.

![Circuit Diagram](https://raw.github.com/DougEdey/SB_Elsinore_Server/master/img/rpi_circuit.png)

In this example, I have two SSRs connected to the GPIO Pin 11 and one connected to GPIO 10 (this is just an example).

Previous versions of this application used WiringPi pinout, this has changed, if you're unsure what version you have, the Software will walk you through the setup on first run, and you can remove/change the name of rpibrew.cfg to restart the setup process.


GPIO Naming
===========

The Pinout number is obviously different between RaspberryPi and Beaglebone.

For RPi pinout: http://elinux.org/RPi_Low-level_peripherals

RPi numbering is displayed in the setup utility on first startup.

For RPi, you'll want to use GPIO[X] where X is the pin number. 


Setup Kernel Modules
=============

The RaspberryPi requires some kernel modules to be installed, you may not need these if you use OWFS, I'm unable to test but would like feedback...

```
sudo modprobe w1-gpio
sudo modprobe w1-therm
````

If you do not use OWFS, you cannot use anything except for temperature probes on the one-wire bus