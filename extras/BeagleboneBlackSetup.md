BeagleBone Black Setup
===============

The Beaglebone Black is similar to the Raspberry Pi, but with more GPIO and onboard Analogue in (that has a max of 1.8V)

Please do not use a Kernel above 3.8, if you do get a new image, install the 3.8 kernel with ``` sudo apt-get install linux-image-3.8.13-bone67 ```


Launching
============

EDIT: No longer needed! The system will lookup this by default. But you can override it if you want to! Thanks to jangeeva for the poke...

You _can_ set the "gpio_definition" property on launch 

``` 
java -Dgpio_definition=extras/beaglebone.json -jar Elsinore.jar
```

If there's an issue with the default file. Or you're on a system I haven't supported yet officially. Contact me if you have!

GPIO Naming
===========

The Pinout number is obviously different between RaspberryPi and Beaglebone.

For Beagleboard Black Pinout: http://elinux.org/BeagleBone#P9_and_P8_-_Each_2x23_pins

BeagleboardBlack has multiple banks, for example GPIO2_2, this translates to physical pin 66, banks are separated by 32 outputs per bank. 


Overlays
============

For beagleboard to get OneWire support, and GPIO control, you need to install a Device Tree Overlay file. I have added these under the *extras* directory.

To compile the overlay: 

``` sudo dtc -O dtb -o /lib/firmware/w1-00A0.dtbo -b 0 -@ w1-00A0.dtbo ```

Or copy the w1-00A0.dtbo in the support directory to /lib/firmware (as root), the above command copies the file for you.

Login as root (such as "sudo su") and run 

``` echo w1 > /sys/devices/bone_capemgr.8/slots ```

Note: bone_capemgr.8 may be bone.capemgr.9 depending on your setup

I have also provided a sample jGPIO created overlay for GPIO0_7 as a pinout (jgpio-00A0.dtbo) which can be used in the same was as above

``` sudo cp extras/jgpio-00A0.dtbo /lib/firmware/ ```

``` echo jgpio > /sys/devices/bone_capemgr.8/slots ```

Creating a custom Overlay
============

If you want to create a DTC file for custom GPIO pinout, you can read the instructions in the [jGPIO](https://github.com/DougEdey/jGPIO) repository

But the synopsis is

``` java -cp Elsinore.jar -Dgpio_definition=extras/beaglebone.json jGPIO.DTOTest <List of GPIO pins> ```

For example

``` java -cp Elsinore.jar -Dgpio_definition=extras/beaglebone.json jGPIO.DTOTest GPIO0_7 ``` 

Will recreate the default GPIO0_7 file as jGPIO-00A0.dto, you will then need to compile this as the same as above but for jgpio
 
``` sudo dtc -O dtb -o /lib/firmware/jgpio-00A0.dtbo -b 0 -@ jgpio-00A0.dto ```


Analogue inputs
===========

There is already a precompiled AIN DTO for the Beaglebone Black, BB-ADC, as above:

``` echo BB-ADC > /sys/devices/bone_capemgr.8/slots ```

To Activate them.



