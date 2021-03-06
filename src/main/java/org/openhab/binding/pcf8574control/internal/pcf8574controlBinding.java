/**
 * Copyright (c) 2010-2015, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.pcf8574control.internal;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.openhab.binding.pcf8574control.pcf8574controlBindingProvider;
import org.openhab.core.binding.AbstractActiveBinding;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.OpenClosedType;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pi4j.gpio.extension.pcf.PCF8574GpioProvider;
import com.pi4j.gpio.extension.pcf.PCF8574Pin;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinMode;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.i2c.I2CDevice;
import com.sun.corba.se.spi.orbutil.fsm.Guard.Result;
import com.sun.scenario.effect.impl.prism.PrImage;
import com.sun.xml.internal.bind.v2.runtime.unmarshaller.XsiNilLoader.Array;

	

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
	private TreeMap<AddressAndPin, Boolean> PinStateMap = new TreeMap<>();
	
	/** 
	 * the refresh interval which is used to poll values from the pcf8574control
	 * server (optional, defaults to 60000ms)
	 */
	private long refreshInterval = 200;
	
	public pcf8574controlBinding() {
		logger.debug("pcf8574controlBinding binding started");
	}
	
	
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
//		logger.debug("pcf8574control: internalReceiveCommand({},{}) is called!", itemName, command);
				
		try {
			for (pcf8574controlBindingProvider provider : providers) {
				int i2cAddress = provider.getAddress(itemName);		
				Pin pin = PCF8574Pin.ALL[provider.getPinNumber(itemName)];
				
				if(command == OnOffType.ON){
					provider.setIsHigh(itemName, true);
//					logger.debug("pcf8574control: internalReceiveCommand: --ON-- Address: {}, Pin: {}", i2cAddress, provider.getPinNumber(itemName));
				} else if(command == OnOffType.OFF) {
					provider.setIsHigh(itemName, false);
//					logger.debug("pcf8574control: internalReceiveCommand: --OFF-- Address: {}, Pin: {}", i2cAddress, provider.getPinNumber(itemName));
				} 	

				Boolean[] boardOutputState = {false,false,false,false, false,false,false,false};
				for (String iName : provider.getItemNames()) {
					if(provider.getAddress(iName) == i2cAddress){
						if(!provider.getIsOutput(iName)){
							logger.debug("pcf8574control: internalReceiveCommand: INPUT Pin! ItemName {}", iName);
							return; //Its a Input.
						}
						boardOutputState[provider.getPinNumber(iName)] = provider.getIsHigh(iName);
					}
				}
//				logger.debug("pcf8574control: internalReceiveCommand:  Address: {}, Pin: {}", i2cAddress, provider.getPinNumber(itemName));
				Byte portByte = 0; 
				for (int i = 0; i < boardOutputState.length; i++) {
					if (boardOutputState[i]) {
						portByte = (byte) (portByte | (0x01 << i));
					}					
				}
//				logger.debug("pcf8574control: internalReceiveCommand:  Byte {}", portByte);
				
				for(Entry<Integer, I2CDevice> entry : provider.getPCF8574Map().entrySet()){ //Every Board
					int key = entry.getKey();
					I2CDevice prov = entry.getValue();
					if(key == i2cAddress){
						prov.write(portByte); //only write, if the address of Board of this Pin is equal to the given I2CDevice to prevent writing on a InputChip 
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}					
	}
	
	/**
	 * @{inheritDoc}
	 */
	@Override
	protected void internalReceiveUpdate(String itemName, State newState) {
		// the code being executed when a state was sent on the openHAB
		// event bus goes here. This method is only called if one of the 
		// BindingProviders provide a binding for the given 'itemName'.
		//logger.debug("internalReceiveUpdate({},{}) is called!", itemName, newState);		
	}	
	
	private void readAllInputPins(){
//		logger.debug("<<<<<<<<<<<<<<<<<<<<<< READ ALL INPUT PINS is called! >>>>>>>>>>>>>>>>>>>>>>>>>");
		for (pcf8574controlBindingProvider provider : providers) {			
			for(Entry<Integer, I2CDevice> entry : provider.getPCF8574Map().entrySet()){ //Every Board
				int key = entry.getKey();
				I2CDevice prov = entry.getValue();
				int readValue = 0;
				try {
					readValue = prov.read(); //Read via I2C only once per Device/Board.
				} catch (IOException e1) {
					e1.printStackTrace();
				} 
				for(Pin pin : PCF8574Pin.ALL){ //every Pin
					AddressAndPin addressAndPin = new AddressAndPin(key, pin);
					String itemName = getItemName(addressAndPin);
//					logger.debug("========= addressAndPin: key: {} pin: {} itemName: {}", key, pin.getAddress(), itemName);
					if (itemName == null) {
						continue;
					}
					if(!provider.getIsOutput(itemName)){
//						logger.debug("Pinstate: {} of Pin: {}", prov.getState(pin), pin);					
						boolean TempPinState = getSinglePortState(readValue, pin.getAddress());	
						if(PinStateMap.containsKey(addressAndPin)){
//							logger.debug("========= Digital_Input Address and Pin contained: key {} pin: {}", key, pin);
							if(PinStateMap.get(addressAndPin).booleanValue() != TempPinState){ //If saved state is different to current state
//								logger.debug("========= TempPinState != saved State: {} - {} {}", PinStateMap.get(addressAndPin).booleanValue(), key, pin);
								try {
									if(TempPinState){
										eventPublisher.postUpdate(getItemName(addressAndPin), OpenClosedType.OPEN);
										logger.debug("========= SEND COMMAND OPEN >>>>>>>>>>>>>>>>>>>>>>>>> {} {}", addressAndPin.address, addressAndPin.pin.getAddress());
									} else {
										eventPublisher.postUpdate(getItemName(addressAndPin), OpenClosedType.CLOSED);
										logger.debug("========= SEND COMMAND CLOSED >>>>>>>>>>>>>>>>>>>>>>>>> {} {}", addressAndPin.address, addressAndPin.pin.getAddress());
									}
								} catch (Exception e) {
									e.printStackTrace();
								}								
								PinStateMap.replace(addressAndPin, TempPinState); //Save current state
							}
						} else {
							PinStateMap.put(new AddressAndPin(key, pin), TempPinState);
							//Update status on first call 
//							if(TempPinState){
//								eventPublisher.postUpdate(getItemName(addressAndPin), OpenClosedType.OPEN);
//							} else {
//								eventPublisher.postUpdate(getItemName(addressAndPin), OpenClosedType.CLOSED);
//							}
						}						
					}
				}
			}
		}		
	}
	
	private String getItemName(AddressAndPin addressAndPin){
		for (pcf8574controlBindingProvider provider : providers) {
			for (String itemName : provider.getItemNames()) {
//				logger.debug("++++++++++++ getItemName {} - {}", provider.getAddress(itemName), provider.getPinNumber(itemName));
				if(provider.getAddress(itemName) == addressAndPin.address && provider.getPinNumber(itemName) == addressAndPin.pin.getAddress()){
					return itemName;
				}
			}		
		}
		return null;
	}
	
	private boolean getSinglePortState(int portReadValue, int pinNumber){
		byte readVal = (byte) portReadValue;			
		byte result = (byte) ((readVal >> pinNumber) & 0x01); 
//		logger.debug("OOOOO getSinglePortState  {} - {}", readVal, result);
		if (result == 1) {
//			logger.debug("OOOOO getSinglePortState TRUEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEES");
			return true;
		}
		return false;		
	}
}
