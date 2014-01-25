Strangebrew Elsinore Server
==================


Java Server for Strangebrew Elsinore

The purpose of this project is to create a Java application that can run on small systems such as the RaspberryPi and the Beaglebone series

Since these systems run on Linux, that is a requirement to using this.

The system is based off the Dallas One Wire protocol for the temperature probes (expanding to analogue inputs too), and Straightforward GPIO outputs for SSR control

Setup Instructions
====================
[Beaglebone Black](extras/BeagleboneBlackSetup.md)

[RaspberryPi](extras/RaspberryPiSetup.md)

Startup Instructions
====================

Clone this repository:

``` git clone https://github.com/DougEdey/SB_Elsinore_Server ```

Move to the checked out Directory:

``` cd SB_Elsinore_Server ```

Run:

``` java -jar Elsinore.jar [options ]```

To startup the setup procedure, I am aware that it needs to be improved, if you have any suggestions, please raise a bug
Use -help to show the full list of options.

Then check the config file if you want to, then you can copy the new one to the default name

``` cp rpibrew.cfg.new rpibrew.cfg ```

And rerun Elsinore to get started

``` java -jar Elsinore.jar [options] ```


RaspberryPi or Beagleboard?
=======================

The BeagleBoard Black does have a big advantage over the RPi, it has onboard analogue inputs, but these are 1.8V.

The RaspberryPi works fine with the existing software, the only thing you need to do differently is naming the GPIO Pinouts.

Pump Control
============

Elsinore now supports pumps, you'll need to add a new section called "pumps" and each pump must be on it's own line in the form 
``` name=gpio ```

The buttons will be RED when on, and GRAY when off. 

One Wire & OWFS
==========
One Wire is fantastic (in my opinion) each sensor or device has a full 64 bit address, and you don't have to worry about the order and you can chain them!

One Wire devices can be chained, with only one Pullup resistor before the first connection to a device. I currently have 4 Temperature probes on my circuit, and a ADC, connected using XLR jacks.


[OWFS](http://owfs.org/) is a much better One Wire Implementation, it is highly recommended to install it, and it is REQUIRED if you use One Wire based Analog inputs (like the DS2450

Install it using your standard package manager, then you need to set a mountpoint up (I use /mnt/1wire) and set 

``` server: w1 ``` in the OWFS configuration File (/etc/owfs.conf by default), you can chose the ports as you want for OWFSHTTP and OWServer, the configuration tool will setup OWFS if it can.

To manually setup OWFS, use the option -owfs when starting up Elsinore

Cutoff Temperature
============
After the [incident](http://imgur.com/a/pwQVE) I decided to add a cutoff temperature, in my case I have a temperature probe on my SSRs, and when they go over a certain temperature I want to kill the server so it doesn't get badly damaged

Adding 
``` cutoff = <string> ```
To any of the devices, will kill Elsinore when the temperature for that device goes above it.

The String is in the form: <number><scale>

So in the config file below, I can use 85C as a cutoff temperature and it'll turn off when it goes above 85C

System Temperature
============

System can be enabled during setup by entering "system" (no quotes) at the prompt.

Also, to enable the System Temperature reading, please add

``` <system /> ```

In the <general> section of the configuration file.

Timers
=========

During setup you can add custom timers, just use:

```
timer <name>
```

At the command prompt, if you don't enter a name it will prompt you for one.

Config File
=========

This config file will automatically be parsed and converted to an XML file 

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

Visit 
```
<ip of your system>:8080/controller
```
to access the webUI, which works on mobiles too:

![Browser Layout](http://i.imgur.com/j59BcFZ.png)

[Album of the UI Progress](http://imgur.com/a/jEIbc)

This is an example of the PID control interface, temperature probes are displayed on the right hand side as LCD displays. On the Raspberry Pi you'll also get the system temperature (this isn't enabled on Beagleboard yet)

There is also a Android Application which is not currently in development, search my repositories to check this, but it's not deprecated yet, I haven't changed the JSON output from the system so it should continue to work.



Thanks For reading this, if you have any queries please contact me or file a bug.
