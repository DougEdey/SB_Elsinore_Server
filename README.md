Strangebrew Elsinore Server
==================


Java Server for Strangebrew Elsinore

The purpose of this project is to create a Java application that can run on small systems such as the RaspberryPi and the Beaglebone series

Since these systems run on Linux, that is a requirement to using this.

The system is based off the Dallas One Wire protocol for the temperature probes (expanding to analogue inputs too), and Straightforward GPIO outputs for SSR control

Here is an example circuit (which I currently use) In this example, for RaspberryPi, Pin 4 MUST be reserved for the one wire circuit, and there's a 4k7 pullup resistor.

![Circuit Diagram](https://raw.github.com/DougEdey/SB_Elsinore_Server/master/img/rpi_circuit.png)

One Wire devices can be chained, with only one Pullup resistor before the first connection to a device. I currently have 4 Temperature probes on my circuit, connected using XLR jacks.

In this example, I have two SSRs connected to the GPIO Pin 11 and one connected to GPIO 10 (this is just an example).

Previous versions of this application used WiringPi pinout, this has changed, if you're unsure what version you have, the Software will walk you through the setup on first run, and you can remove/change the name of rpibrew.cfg to restart the setup process.

RaspberryPi or Beagleboard?
=======================

I recently purchased a beagleboard to experiment with, as a result of that I learned a lot more about how Linux deals with OneWire and GPIO. For beagleboard to get OneWire support, you need to install a Device Tree Overlay file. I have added these under the support directory:

To compile the overlay: 
``` sudo dtc -O dtb -o /lib/firmware/w1-00A0.dtbo -b 0 -@ w1-00A0.dtbo ```

Or copy the w1-00A0.dtbo in the support directory to /lib/firmware (as root), the above command copies the file for you.

Login as root (such as "sudo su") and run "echo w1 > /sys/devices/bone_capemgr.8/slots"

These need to be run as root.

The RaspberryPi works fine with the existing software, the only thing you need to do differently is naming the GPIO Pinouts.

GPIO Naming
===========

The Pinout number is obviously different between RaspberryPi and Beaglebone.

For RPi pinout: http://elinux.org/RPi_Low-level_peripherals

For Beagleboard Black Pinout: http://elinux.org/BeagleBone#P9_and_P8_-_Each_2x23_pins

RPi numbering is displayed in the setup utility on first startup, Beaglebone Black is not, but the above diagrams give details. 

For RPi, you'll want to use GPIO[X] where X is the pin number. BeagleboardBlack has multiple banks, for example GPIO2_2, this translates to physical pin 66, banks are separated by 32 outputs per bank. 

Pump Control
============

Elsinore now supports pumps, you'll need to add a new section called "pumps" and each pump must be on it's own line in the form 
``` name=gpio ```

The buttons will be RED when on, and GRAY when off. 


Config File
=========

```
[general]
scale = F
#cosm = COSM API KEY
#cosm_feed = YOUR COSM FEED

[kettle]
set_point = 168.0
duty_cycle = 100.0
cycle_time = 2.0
k_param = 41.0
i_param = 169.0
d_param = 4.0
probe = 28-0000032c449f
gpio = GPIO2_1


[mlt]
set_point = 0.0
duty_cycle = 0.0
cycle_time = 2.0
k_param = 44.0
i_param = 165.0
d_param = 4.0
probe = 28-0000032c506e
gpio = 

[pumps]
pump_a = GPIO0_8
pump_foo = GPIO0_9
```

This is a sample Setup file, you can see I have two devices setup here, the MLT is a "read only" probe that doesn't have a GPIO associated. Whereas the Kettle is setup with default PID values, and has a GPIO pinout of GPIO2_1.

The scale can be changed between C or F to use Celsius or Fahrenheit on the system.

Control Interface
============

Visit <ip of your system>:8080/control to access the webUI, which works on mobiles too:

![Browser Layout](http://i.imgur.com/j59BcFZ.png)

[Album of the UI Progress](http://imgur.com/a/jEIbc)

This is an example of the PID control interface, temperature probes are displayed on the right hand side as LCD displays. On the Raspberry Pi you'll also get the system temperature (this isn't enabled on Beagleboard yet)

There is also a Android Application which is not currently in development, search my repositories to check this, but it's not deprecated yet, I haven't changed the JSON output from the system so it should continue to work.



Thanks For reading this, if you have any queries please contact me or file a bug.
