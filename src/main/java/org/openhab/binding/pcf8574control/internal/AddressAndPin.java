package org.openhab.binding.pcf8574control.internal;

import com.pi4j.io.gpio.Pin;

public class AddressAndPin implements Comparable<AddressAndPin> {
	public int address;
	public Pin pin;
	
	public AddressAndPin(int address, Pin pin) {
		this.address = address;
		this.pin = pin;
	}

	@Override
	public int compareTo(AddressAndPin addressAndPin) {
		if(address == addressAndPin.address && pin == addressAndPin.pin){
			return 0;
		} else {
			return -1;			
		}
	}
}
