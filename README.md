Strangebrew Elsinore Server
==================

[JavaDoc for this project](http://dougedey.github.io/SB_Elsinore_Server/doc/)

Java Server for Strangebrew Elsinore

The purpose of this project is to create a Java application that can run on small systems such as the RaspberryPi and the Beaglebone series

Since these systems run on Linux, that is a requirement to using this.

The system is based off the Dallas One Wire protocol for the temperature probes (expanding to analogue inputs too), and Straightforward GPIO outputs for SSR control

Setup Instructions
====================
[Beaglebone Black](http://dougedey.github.io/2014/03/22/BeagleBoneBlack-Setup/)

[RaspberryPi](http://dougedey.github.io/2014/03/22/RaspberryPi-Setup/)


[More indepth RPi Documentation](http://dougedey.github.io/2014/03/22/Raspberry-Pi-Basic-Setup/)

[HBT Thread for RaspberryPi setup](http://www.homebrewtalk.com/f170/raspberry-pi-strangebrew-elsinore-basic-setup-463590/#post5961579)

[Video Instructions](http://dougedey.github.io/2014/09/27/Setup_Videos/)

Startup Instructions
====================

Clone this repository:

``` git clone https://github.com/DougEdey/SB_Elsinore_Server ```

Move to the checked out Directory:

``` cd SB_Elsinore_Server ```

Run:

``` sudo ./launch.sh ```

You may get offered to use OWFS if you have a non temperature sensor (such as a one wire analogue chip, since the basic OneWire support in Linux doesn't support them)
Then you can go to 

``` <ip/hostname of machine>:8080/controller ```

To run setup.

Installing as an init.d script
==============================

Elsinore now has a script which allows you to setup Elsinore as a service.

You must EDIT the file ``` extras/elsinore ``` to ensure the ELSINORE_PATH variable matches the location you have cloned Elsinore into.

If you're running on BeagleBone Black you'll also want to uncomment the W1_SETUP line and the lines indicated in d_start() to ensure that the One Wire and JGPIO files are exported correctly.

Then run:

```
sudo cp extras/elsinore /etc/init.d
sudo update-rc.d elsinore defaults
```

And then you can manually start elsinore using
``` sudo service elsinore start ```

Stop using:

``` sudo service elsinore stop ```

And it'll automatically start/stop on boot/shutdown

Setup
=====

You can double click the "Elsinore" name to set your own brewery name, you can upload a custom brewery image, by clicking the upload file button under the brewery image.

Double clicking on a device address allows you to add a GPIO and rename the device (such as kettle, MLT, HLT, case, etc...), as well as an auxiliary GPIO (for secondary, manual outputs)

You can add mash steps for devices you have set up with a GPIO output (so it'll change the temperature, hold temperature, and move steps as needed).

You can add pumps (name and GPIO) by pressing the "Add Pump" button

You can add timers (name only at the moment) by pressing the "Add Timer" button.

You can also delete any of the above, you can drag and drop pumps, timers, and mash steps to re-order them. Or drag them to the delete button to delete them!

When the server is shutdown, it saves all your setup!

You can also use the Edit mode of the web UI to change the settings for the data recorder, for instance, turning it off, setting the minimum change to record data, and so on...

Once you've finished setting up, double clicking the "Lock" button in the top right to remove all the setup buttons.

RaspberryPi or Beagleboard?
=======================

The BeagleBoard Black does have a big advantage over the RPi, it has onboard analogue inputs, but these are 1.8V.

The RaspberryPi works fine with the existing software, the only thing you need to do differently is naming the GPIO Pinouts.


One Wire & OWFS
==========
One Wire is fantastic (in my opinion) each sensor or device has a full 64 bit address, and you don't have to worry about the order and you can chain them!

One Wire devices can be chained, with only one Pullup resistor before the first connection to a device. I currently have 4 Temperature probes on my circuit, and a ADC, connected using XLR jacks.


[OWFS](http://owfs.org/) is a much better One Wire Implementation, it is highly recommended to install it, and it is REQUIRED if you use One Wire based Analog inputs (like the DS2450

Install it using your standard package manager, then you need to set a mountpoint up (I use /mnt/1wire) and set 

On startup, Elsinore will ask if you want to use OWFS if it detects non-temperature probes.



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

Edit Mode
=========

"Edit mode" is what I call the mechanism by which you can name the temperature probes, convert them to/from PIDs, add pumps, add timers, add mash steps, and re-organize almost everything!

Double clicking the "Edit" button on the top left (the position will probably change) will unlock everything!

* Allows you to change the temperature mode "Change Scale".
* Allows you to rename temperature probes -> Double click on any of the temperature probe headers
* Allows you to create/delete PIDs
  * Edit a temperature probe, and add a GPIO pin for PID mode
  * You can chose to add an extra GPIO for the auxilliary output that's manually controlled.
* Add custom named timers, click the "Add" button in the timer section.
* Re-order the timers, click and drag to organize them
* Add custom named Pump outputs, click the "Add" button in the pump section
* Re-order the pumps, click and drag to organize them
* [Add mash steps](http://dougedey.github.io/2014/09/01/Elsinore-Mash-Edits/)
* Add the system temperature probe

[More Pictures and information](http://dougedey.github.io/2014/05/14/New-Elsinore_Setup/)

Pump Control
============

Elsinore supports pumps, or any output which just needs to be toggled, you can add them from the Web UI when in Edit mode
The buttons will be RED when on, and GRAY when off. 


Cutoff Temperature
============
After the [incident](http://imgur.com/a/pwQVE) I decided to add a cutoff temperature, in my case I have a temperature probe on my SSRs, and when they go over a certain temperature I want to kill the server so it doesn't get badly damaged

You can set the cutoff temperature when modifying a PID from the Web UI.

System Temperature
============

This can be enabled in edit mode on the Web UI.

Timers
=========

These can be added from the edit mode of the Web UI.

Config File
=========

The config file is all in XML and everything is controlled via the Web UI!

Language Translations
=====================

Update! NLS Support!

When you start elsinore you'll see an extra output line:

```
INFO: Debug System property: null
Language: en_US
Server started, kill to stop.
```

The "Language" line should match your system language. For me, I have an English United States system.

If you want to provide translations for your language, you'll just need to send me a copy of:

[See here for the default](https://github.com/DougEdey/SB_Elsinore_Server/blob/66ebb13ab606498de19393c7d2fcd7beae9b9795/src/com/sb/elsinore/nls/messages.properties)

With the translations and the language line.

If you want to test it yourself, set your system up to build Elsinore, which should be as simple as importing the project to Eclipse (since that's what I use). 

Then create a copy of src/com/sb/elsinore/nls/messages.properties but change the name to almost match your language, just change the _ to a -. i.e. en_US becomes en-US. 

Then create the matching file: src/com/sb/elsinore/nls/messages_<language_output>.properties. for my system it would be src/com/sb/elsinore/nls/messages_en-US.properties

Thanks For reading this, if you have any queries please contact me or file a bug.
