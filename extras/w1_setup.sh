#!/bin/bash
echo "Setting up w1"
echo w1 > /sys/devices/bone_capemgr.*/slots
echo "Setting up jgpio"
echo jgpio > /sys/devices/bone_capemgr.*/slots
echo "Cape Manager updated, list of devices:"
ls -ltr /sys/devices/w1_bus_master1
