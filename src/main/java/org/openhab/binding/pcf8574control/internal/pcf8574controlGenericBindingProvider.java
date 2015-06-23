/**
 * Copyright (c) 2010-2015, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.pcf8574control.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import org.openhab.binding.pcf8574control.pcf8574controlBindingProvider;
import org.openhab.core.binding.BindingConfig;
import org.openhab.core.items.Item;
import org.openhab.core.library.items.ContactItem;
import org.openhab.core.library.items.DimmerItem;
import org.openhab.core.library.items.NumberItem;
import org.openhab.core.library.items.SwitchItem;
import org.openhab.core.library.types.OpenClosedType;
import org.openhab.model.item.binding.AbstractGenericBindingProvider;
import org.openhab.model.item.binding.BindingConfigParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pi4j.gpio.extension.pcf.PCF8574GpioProvider;
import com.pi4j.gpio.extension.pcf.PCF8574Pin;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigital;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinMode;
import com.pi4j.io.gpio.event.PinEvent;
import com.pi4j.io.gpio.event.PinListener;
import com.pi4j.io.i2c.I2CBus;
import com.pi4j.wiringpi.Gpio;
import com.pi4j.wiringpi.GpioInterrupt;
import com.pi4j.wiringpi.GpioInterruptEvent;
import com.pi4j.wiringpi.GpioInterruptListener;
import com.pi4j.wiringpi.GpioUtil;


/**
 * This class is responsible for parsing the binding configuration.
 * 
 * @author MichaelP
 * @since 1.0
 */
public class pcf8574controlGenericBindingProvider extends AbstractGenericBindingProvider implements pcf8574controlBindingProvider {

	private static final Logger logger = 
			LoggerFactory.getLogger(pcf8574controlGenericBindingProvider.class);
	
	private final int INTERRUPT_PIN = 0; //WiringPi Pin 0 = GPIO 17
	private TreeMap<Integer, PCF8574GpioProvider> PCF8574Map = new TreeMap<>();
	final 	GpioController gpio = GpioFactory.getInstance();	
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getBindingType() {
		return "pcf8574control";
	}

	/**
	 * @{inheritDoc}
	 */
	@Override
	public void validateItemType(Item item, String bindingConfig) throws BindingConfigParseException {
		if (!(item instanceof SwitchItem || item instanceof DimmerItem || item instanceof ContactItem)) {
			throw new BindingConfigParseException("item '" + item.getName()
					+ "' is of type '" + item.getClass().getSimpleName()
					+ "', only Switch- and DimmerItems and ContactItems are allowed - please check your *.items configuration");
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void processBindingConfiguration(String context, Item item, String bindingConfig) throws BindingConfigParseException {		
		super.processBindingConfiguration(context, item, bindingConfig);
			
		//Format: "I2CAddress;PinNumber;isOutput" => f.e. "32;0;out" or "32;1;in"
		String[] properties = bindingConfig.split(";");		
		pcf8574controlConfig config = new pcf8574controlConfig();
		try{
			
			config.address = Integer.parseInt(properties[0]);
			config.pinNumber = Integer.parseInt(properties[1]);
			
			checkOfValidValues(config, item.getName());
			addBindingConfig(item, config);	
			handleBoards(config); //Create new PCF8574GpioProvider for eventually new boards.
				
			logger.debug("processBindingConfiguration: (pcf8574control) ItemName: {}, Addresses: {}", item.toString(), PCF8574Map.keySet());
			
			Pin pin = new PCF8574Pin().ALL[config.pinNumber];
			PCF8574Map.get(config.address).unexport(pin);
			if(properties[2].toLowerCase().equals("out")){
				logger.debug("processBindingConfiguration: (pcf8574control) ---<<<< gpioPinDigitalOutput >>>>---");				
				PCF8574Map.get(config.address).export(pin, PinMode.DIGITAL_OUTPUT);
			} else if (properties[2].toLowerCase().equals("in")){
				PCF8574Map.get(config.address).export(pin, PinMode.DIGITAL_INPUT);				
			}
			
		}catch(Exception e){
			e.printStackTrace();
		}
		logger.debug("pcf8574controlGenericBindingProvider: processBindingConfiguration({},{}) is called!", config.address, config.pinNumber);
		//parse bindingconfig here ...
				
	}
	
	/* ================================= SELF WRITTEN METHODS - BEGIN ===============================*/
	
	private void checkOfValidValues(pcf8574controlConfig config, String itemName){
		//isHigh is no item-file value, so it isn't checked here.
		if(config.address < 32 || config.address > 63 || (config.address > 39 && config.address < 56)){
			throw new IllegalArgumentException("The given address '" + config.address + "'of the item '" + itemName + "' is invalid! " +
					"PCA8574 must be between 32-39 or 56-63 (0x20-0x27 or for A-Model 0x38-0x3F)");
		}
		
		if(config.pinNumber < 0 || config.pinNumber > 7){
			throw new IllegalArgumentException("The pinNumber of the item '" + itemName + "'is invalid! Must be between 0-7.");
		}				
	}
		
	private void handleBoards(pcf8574controlConfig config){
		try {
			if(!PCF8574Map.containsKey(config.address)){
				try{
					PCF8574Map.put(config.address, new PCF8574GpioProvider(I2CBus.BUS_1, config.address));	
					logger.debug("handleBoards: added PCF8574 board with address: {} !", config.address);
				} catch (Exception e) {
					e.printStackTrace();
				}				
			}
			removeUnusedBoardsFromMap(config);
		} catch (Exception e) {
			e.printStackTrace();
			//logger.debug("Exception in PCF8574 handleBoards... however, it works.");
		}
	}
	
	private void removeUnusedBoardsFromMap(pcf8574controlConfig config){
		keyLoop:
		for(Integer mapKey : PCF8574Map.keySet()){
			logger.debug("handleBoards: mapKey {} !", mapKey);
			for(BindingConfig bindingConfig : bindingConfigs.values()){
				pcf8574controlConfig conf = (pcf8574controlConfig) bindingConfig;				
				logger.debug("handleBoards: check {} !", conf.address);
				if(mapKey == conf.address){
					logger.debug("handleBoards: board found with address: {} !", conf.address);
					continue keyLoop;
				}				
			}
			if(!bindingConfigs.values().isEmpty()){
				PCF8574Map.remove(mapKey);
				logger.debug("handleBoards: removed board with address: {} !", mapKey);
			}
		}
	}
	
	public void setupInterruptPinForPortExpanderInt(GpioInterruptListener pcf8574InterruptListener){
		if(Gpio.wiringPiSetup() == -1){
			logger.debug("setupInterruptPinForPortExpanderInt: WiringPiSetup ERROR!");
			return;
		}
				
//		logger.debug("setupInterruptPinForPortExpanderInt");

		if(!GpioInterrupt.hasListener(pcf8574InterruptListener)){
			GpioInterrupt.addListener(pcf8574InterruptListener);
		}
		GpioUtil.export(INTERRUPT_PIN, GpioUtil.DIRECTION_IN);
		GpioUtil.setEdgeDetection(INTERRUPT_PIN, GpioUtil.EDGE_BOTH); //Only if State is low = false. (Interrupt is inverted, low says there was a change on one InputPin of the PCF8574)
		Gpio.pinMode(INTERRUPT_PIN, Gpio.INPUT);
		Gpio.pullUpDnControl(INTERRUPT_PIN, Gpio.PUD_DOWN); //For whatever..!?		
		GpioInterrupt.enablePinStateChangeCallback(INTERRUPT_PIN);		
	}
	
	
	@Override
	public int getAddress(String itemName) {
		pcf8574controlConfig config = (pcf8574controlConfig) bindingConfigs.get(itemName);
		
		if (config == null) {
			throw new IllegalArgumentException("The item name '" + itemName + "'is invalid or the item isn't configured");
		}
		
		return config.address;
	}

	@Override
	public int getPinNumber(String itemName) {
		pcf8574controlConfig config = (pcf8574controlConfig) bindingConfigs.get(itemName);
		
		if (config == null) {
			throw new IllegalArgumentException("The item name '" + itemName + "'is invalid or the item isn't configured");
		}
		
		return config.pinNumber;
	}
	
	@Override
	public boolean getIsHigh(String itemName) {
		pcf8574controlConfig config = (pcf8574controlConfig) bindingConfigs.get(itemName);
		
		if (config == null) {
			throw new IllegalArgumentException("The item name '" + itemName + "'is invalid or the item isn't configured");
		}
		
		return config.isHigh;
	}
	
	@Override
	public void setIsHigh(String itemName, boolean value) {
		pcf8574controlConfig config = (pcf8574controlConfig) bindingConfigs.get(itemName);
		
		if (config == null) {
			throw new IllegalArgumentException("The item name '" + itemName + "'is invalid or the item isn't configured");
		}
		config.isHigh = value;
	}
	
	@Override
	public boolean isItemConfigured(String itemName) {
		if (bindingConfigs.containsKey(itemName)) {
			return true;
		}
		return false;
	}
	
	
	public class pcf8574controlConfig implements BindingConfig{
		int address;
		int pinNumber;
		boolean isHigh;
	}


	@Override
	public TreeMap<Integer, PCF8574GpioProvider> getPCF8574Map() {		
		return PCF8574Map;
	}

	
	/* ================================= SELF WRITTEN METHODS - END ===============================*/
	
}
