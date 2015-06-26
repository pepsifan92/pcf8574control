# pcf8574control
Controlling the chip pcf8574 (Port expander) via I2C from Raspberry Pi from openHAB

The chip PCF8591 is an I2C-Bus Portexpander and provides setting and getting the state of 8 Pins. A Pin can be high and low.
**Addressrange of PCF8574:** 32-39 (0x20-0x27)<br>
**Addressrange of PCF8574A:** 56-63 (0x38-0x3F)<br>
This binding supports reading and writing of pin-states.

## Config in *.item file
In the **items-file** of openHAB the following <b>configuration</b> is needed:<br>
Contact Name-of-Item { pcf8574control="I2CAddressInDecimal;PinNumber;direction" }

**Input-Pin example:**<br>
Contact motionSensor { pcf8574control="32;0;in" }<br>
This would get the state of the Pin 0 from the PCF8574 chip with the address 0x21 (33 in decimal)

**Output-Pin example:**<br>
Contact led { pcf8574control="32;1;out" }<br>
This would set the state of the Pin 0 from the PCF8574 chip with the address 0x21 (33 in decimal)

## Config in *.sitemap file
**Example** to show the read value on the website:<br>
Text item=motionSensor label="motionSensor: [%s]"

**Example** to set Pin from the website:<br>
Switch item=led