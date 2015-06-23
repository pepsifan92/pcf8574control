/**
 * Copyright (c) 2010-2015, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.pcf8574control.internal;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.openhab.binding.pcf8574control.pcf8574controlBindingProvider;
import org.openhab.core.binding.AbstractActiveBinding;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.OpenClosedType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pi4j.gpio.extension.pca.PCA9685Pin;
import com.pi4j.gpio.extension.pcf.PCF8574GpioProvider;
import com.pi4j.gpio.extension.pcf.PCF8574Pin;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinMode;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.event.PinEvent;
import com.pi4j.io.gpio.event.PinListener;
import com.pi4j.wiringpi.Gpio;
import com.pi4j.wiringpi.GpioInterrupt;
import com.pi4j.wiringpi.GpioInterruptEvent;
import com.pi4j.wiringpi.GpioInterruptListener;

	

/**
 * Implement this class if you are going create an actively polling service
 * like querying a Website/Device.
 * 
 * @author MichaelP
 * @since 1.0
 */
public class pcf8574controlBinding extends AbstractActiveBinding<pcf8574controlBindingProvider> {

	private static final Logger logger = 
		LoggerFactory.getLogger(pcf8574controlBinding.class);	
		
	/**
	 * The BundleContext. This is only valid when the bundle is ACTIVE. It is set in the activate()
	 * method and must not be accessed anymore once the deactivate() method was called or before activate()
	 * was called.
	 */
	private BundleContext bundleContext;

	private long millisSinceLastCall;
	
	private TreeMap<AddressAndPin, Boolean> PinStateMap = new TreeMap<>();
	
	/** 
	 * the refresh interval which is used to poll values from the pcf8574control
	 * server (optional, defaults to 60000ms)
	 */
	private long refreshInterval = 400;
	
	public pcf8574controlBinding() {
		logger.debug("pcf8574controlBinding binding started");
	}
			
	
	/* ADDED THIS FOR GETTING ALL COMMANDS... ****************************/
	/**
	 * @{inheritDoc}
	 */
	@Override
	public void receiveCommand(String itemName, Command command) {
		// does any provider contain a binding config?
		if (!providesBindingFor(itemName)) {
			return;
		}
		internalReceiveCommand(itemName, command);
	}
	/* ADDED THIS FOR GETTING ALL COMMANDS... ****************************/
	
	
	/**
	 * Called by the SCR to activate the component with its configuration read from CAS
	 * 
	 * @param bundleContext BundleContext of the Bundle that defines this component
	 * @param configuration Configuration properties for this component obtained from the ConfigAdmin service
	 */
	public void activate(final BundleContext bundleContext, final Map<String, Object> configuration) {
		this.bundleContext = bundleContext;

		// the configuration is guaranteed not to be null, because the component definition has the
		// configuration-policy set to require. If set to 'optional' then the configuration may be null
		
			
		// to override the default refresh interval one has to add a 
		// parameter to openhab.cfg like <bindingName>:refresh=<intervalInMs>
		String refreshIntervalString = (String) configuration.get("refresh");
		if (StringUtils.isNotBlank(refreshIntervalString)) {
			refreshInterval = Long.parseLong(refreshIntervalString);
		}

		// read further config parameters here ...
//		GpioInterruptListener pcf8574InterruptListener = new GpioInterruptListener() {			
//			@Override
//			public void pinStateChange(GpioInterruptEvent event) {				
//				if(System.currentTimeMillis() - millisSinceLastCall >= 10){
//					millisSinceLastCall = System.currentTimeMillis();
//					readAllInputPins();
//				}
//			}
//		};
//		providers.iterator().next().setupInterruptPinForPortExpanderInt(pcf8574InterruptListener);
		
		setProperlyConfigured(true);
	}
	
	/**
	 * Called by the SCR when the configuration of a binding has been changed through the ConfigAdmin service.
	 * @param configuration Updated configuration properties
	 */
	public void modified(final Map<String, Object> configuration) {
		// update the internal configuration accordingly
		logger.debug("pcf8574control: !!!!! modified !!!!!! ");
	}
	
	/**
	 * Called by the SCR to deactivate the component when either the configuration is removed or
	 * mandatory references are no longer satisfied or the component has simply been stopped.
	 * @param reason Reason code for the deactivation:<br>
	 * <ul>
	 * <li> 0 – Unspecified
     * <li> 1 – The component was disabled
     * <li> 2 – A reference became unsatisfied
     * <li> 3 – A configuration was changed
     * <li> 4 – A configuration was deleted
     * <li> 5 – The component was disposed
     * <li> 6 – The bundle was stopped
     * </ul>
	 */
	public void deactivate(final int reason) {
		this.bundleContext = null;
		// deallocate resources here that are no longer needed and 
		// should be reset when activating this binding again
	}

	
	/**
	 * @{inheritDoc}
	 */
	@Override
	protected long getRefreshInterval() {
		return refreshInterval;
	}

	/**
	 * @{inheritDoc}
	 */
	@Override
	protected String getName() {
		return "pcf8574control Refresh Service";
	}
	

	/**
	 * @{inheritDoc}
	 */
	@Override
	protected void execute() {
		// the frequently executed code (polling) goes here ...		
		//logger.debug("execute() method is called! (pcf8574control) ItemNames: {}, Addresses: {}", providers.iterator().next().getItemNames().toString(), providers.iterator().next().getPCF8574Map().keySet());
		//eventPublisher.postCommand("pcf8574controlBindingStatus", StringType.valueOf("Addresses given in item-config: " + providers.iterator().next().getPCA9685Map().keySet()));
		readAllInputPins();
	}
		
	/**
	 * @{inheritDoc}
	 */
	@Override
	protected void internalReceiveCommand(String itemName, Command command) {
		// the code being executed when a command was sent on the openHAB
		// event bus goes here. This method is only called if one of the
		// BindingProviders provide a binding for the given 'itemName'.
		logger.debug("pcf8574control: internalReceiveCommand({},{}) is called!", itemName, command);
				
		try {
			for (pcf8574controlBindingProvider provider : providers) {
				int i2cAddress = provider.getAddress(itemName);		
				Pin pin = PCF8574Pin.ALL[provider.getPinNumber(itemName)];
				
				if(command == OnOffType.ON){
					//gpio.setState(true, digOutput);
					//digOutput.setState(true);
					//provider.getGpioPinDigital(itemName).setState(true);
					provider.getPCF8574Map().get(i2cAddress).setState(pin, PinState.HIGH);	
					logger.debug("pcf8574control: internalReceiveCommand: --ON-- Address: {}, Pin: {}", i2cAddress, provider.getPinNumber(itemName));
				} else if(command == OnOffType.OFF) {
					//gpio.setState(false, digOutput);
					//digOutput.setState(false);
					//provider.getGpioPinDigital(itemName).setState(false);
					provider.getPCF8574Map().get(i2cAddress).setState(pin, PinState.LOW);
					logger.debug("pcf8574control: internalReceiveCommand: --OFF-- Address: {}, Pin: {}", i2cAddress, provider.getPinNumber(itemName));
				} 
				
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			
			
//			if(command == OnOffType.ON){
//				provider.getPCA9685Map().get(i2cAddress).setOn(pin);
//				provider.setPwmValue(itemName, 100);
//			} else if(command == OnOffType.OFF) {
//				provider.getPCA9685Map().get(i2cAddress).setOff(pin);
//				provider.setPwmValue(itemName, 0);
//			} else if(command == IncreaseDecreaseType.INCREASE){
//				int pwmval = provider.getPwmValue(itemName);
//				if(pwmval < 100){
//					provider.setPwmValue(itemName, pwmval+1);
//					provider.getPCA9685Map().get(i2cAddress).setPwm(pin, NaturalFading.STEPS_100[pwmval+1]);
//				}
//			} else if(command == IncreaseDecreaseType.DECREASE){
//				int pwmval = provider.getPwmValue(itemName);
//				if(pwmval > 0){
//					provider.setPwmValue(itemName, pwmval-1);
//					provider.getPCA9685Map().get(i2cAddress).setPwm(pin, NaturalFading.STEPS_100[pwmval-1]);
//				}
//			} else {				
//				try{
//					Integer value = Integer.parseInt(command.toString());
//					provider.getPCA9685Map().get(i2cAddress).setPwm(pin, NaturalFading.STEPS_100[value]);
//					provider.setPwmValue(itemName, value);
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//			}
		
	}
	
	/**
	 * @{inheritDoc}
	 */
	@Override
	protected void internalReceiveUpdate(String itemName, State newState) {
		// the code being executed when a state was sent on the openHAB
		// event bus goes here. This method is only called if one of the 
		// BindingProviders provide a binding for the given 'itemName'.
		logger.debug("internalReceiveUpdate({},{}) is called!", itemName, newState);		
	}	
	
	private void readAllInputPins(){
		//logger.debug("<<<<<<<<<<<<<<<<<<<<<< READ ALL INPUT PINS is called! >>>>>>>>>>>>>>>>>>>>>>>>>");
		for (pcf8574controlBindingProvider provider : providers) {
			
			for(Entry<Integer, PCF8574GpioProvider> entry : provider.getPCF8574Map().entrySet()){ //Every Board
				int key = entry.getKey();
				PCF8574GpioProvider prov = entry.getValue();				
				for(Pin pin : PCF8574Pin.ALL){ //every Pin
					if(prov.getMode(pin) == PinMode.DIGITAL_INPUT){
//						logger.debug("Pinstate: {} of Pin: {}", prov.getState(pin), pin);
						AddressAndPin addressAndPin = new AddressAndPin(key, pin); 
						if(PinStateMap.containsKey(addressAndPin)){
							if(PinStateMap.get(addressAndPin).booleanValue() != prov.getState(pin).isHigh()){ //If saved state is different to current state
								try {
									if(prov.getState(pin).isHigh()){
										eventPublisher.postUpdate(getItemName(addressAndPin), OpenClosedType.OPEN);
//										logger.debug("========= SEND COMMAND OPEN >>>>>>>>>>>>>>>>>>>>>>>>> {} {}", addressAndPin.address, addressAndPin.pin.getAddress());
									} else {
										eventPublisher.postUpdate(getItemName(addressAndPin), OpenClosedType.CLOSED);
//										logger.debug("========= SEND COMMAND CLOSED >>>>>>>>>>>>>>>>>>>>>>>>> {} {}", addressAndPin.address, addressAndPin.pin.getAddress());
									}
								} catch (Exception e) {
									e.printStackTrace();
								}								
								PinStateMap.replace(addressAndPin, prov.getState(pin).isHigh()); //Save current state
							}
						} else {
							PinStateMap.put(new AddressAndPin(key, pin), prov.getState(pin).isHigh());
						}						
					}
				}
			}
		}		
	}
	
	private String getItemName(AddressAndPin addressAndPin){
		for (pcf8574controlBindingProvider provider : providers) {
			for (String itemName : provider.getItemNames()) {
//				logger.debug("GET ITEM NAME >> {} ... {} {}", itemName, provider.getAddress(itemName), provider.getPinNumber(itemName));
				if(provider.getAddress(itemName) == addressAndPin.address && provider.getPinNumber(itemName) == addressAndPin.pin.getAddress()){
//					logger.debug("GET ITEM NAME >>>>>>>>>>>>>>>>>>>>>>>>> {}", itemName);
					return itemName;
				}
			}		
		}
		return "ItemNotFound in getItemName in pcf8574control";
	}
}
