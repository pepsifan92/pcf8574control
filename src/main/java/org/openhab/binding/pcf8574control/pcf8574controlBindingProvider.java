/**
 * Copyright (c) 2010-2015, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.pcf8574control;

import java.util.TreeMap;
import org.openhab.core.binding.BindingProvider;
import com.pi4j.gpio.extension.pcf.PCF8574GpioProvider;
import com.pi4j.io.i2c.I2CDevice;


/**
 * @author MichaelP
 * @since 1.0
 */
public interface pcf8574controlBindingProvider extends BindingProvider {
	/**
	 * Map of all configured PCF8574 boards. 
	 * Key=I2CAddress, Value=PCF8574GpioProvider Object.
	 * If all given items have the same I2C Address, it should exist only one Map-Entry).    
	 * @return Returns the map with all configured Boards.
	 */
	public TreeMap<Integer, I2CDevice> getPCF8574Map();
	
	/**
	 * Get the I2C Address of the given Item
	 * @param itemName Name of the Item 
	 * @return Returns the I2C Address of the Item.
	 */
	public int getAddress(String itemName);
	
	/**
	 * Get the Pin Number of the given Item 
	 * @param itemName Name of the Item 
	 * @return Returns the Pin Number of the Item.
	 */
	public int getPinNumber(String itemName);
	
	
	/**
	 * Get the isHigh value of the given Item. 
	 * @param itemName Name of the Item 
	 * @return Returns the isHigh value which means the state of the pin. true=high, false=low
	 */
	public boolean getIsHigh(String itemName);
	
	/**
	 * Set the isHigh value of the given Item. 
	 * @param itemName Name of the Item 
	 */
	public void setIsHigh(String itemName, boolean value);
	
	/**
	 * Get the IsOutput value of the given Item. 
	 * @param itemName Name of the Item 
	 * @return Returns the IsOutput value which means the mode of the Pin.
	 */
	public boolean getIsOutput(String itemName);
	
	/**
	 * Set the IsOutput value of the given Item. 
	 * @param itemName Name of the Item 
	 */
	public void setIsOutput(String itemName, boolean value);
	
	/**
	 * Is the given Item already configured?
	 * @param itemName Name of the Item 
	 * @return Returns if the Item is configured. (true = is configured)
	 */
	public boolean isItemConfigured(String itemName);
	
}
