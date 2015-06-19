/*
 * #%L
 * **********************************************************************
 * ORGANIZATION  :  Pi4J
 * PROJECT       :  Pi4J :: Java Examples
 * FILENAME      :  PCA9685GpioExample.java (In originial)
 *
 * This file is part of the Pi4J project. More information about
 * this project can be found here:  http://www.pi4j.com/
 * **********************************************************************
 * %%
 * Copyright (C) 2012 - 2015 Pi4J
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

package org.openhab.binding.pcf8574control.internal;

import java.io.IOException;
import java.lang.Exception;
import java.math.BigDecimal;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pi4j.gpio.extension.pca.PCA9685GpioProvider;
import com.pi4j.gpio.extension.pca.PCA9685Pin;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinPwmOutput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CFactory;

public class PCA9685PwmControl {
    private I2CBus              bus;
    private PCA9685GpioProvider gpioProvider;
    private final BigDecimal    FREQUENCY = new BigDecimal("500"); //Frequency for PWM. Limit is variable, based on the steps or whatever: Working: 100Hz:4096Steps, 400Hz:2500Steps, 500Hz: 2000Steps. So set PWM_MAX_VALUE to a matching value.
    private final BigDecimal    FREQUENCY_CORRECTION_FACTOR = new BigDecimal("1"); //For whatever
    private final int           PWM_OFF_VALUE = 0;
    private final int           PWM_MAX_VALUE = 1999;
    
    private static final Logger logger = 
    		LoggerFactory.getLogger(pcf8574controlBinding.class);	

    public PCA9685PwmControl(int I2CAddress){
    	logger.debug("PCA9685PwmControl: Constructor!");
        try {
            bus = I2CFactory.getInstance(I2CBus.BUS_1); //Select I2CBus
            gpioProvider = new PCA9685GpioProvider(bus, I2CAddress, FREQUENCY, FREQUENCY_CORRECTION_FACTOR); //Init Provider - it controls the Pin-Management: SetPWM and so on...
        } catch (IOException e) {
            e.printStackTrace();
        }
        GpioController gpio = GpioFactory.getInstance(); //Needed to get Control of the GPIOs to set them as a output
        setAllPinsAsOutput(gpio);
    }

    public void setOn(Pin pin){
        gpioProvider.setAlwaysOn(pin);
    }

    public void setOff(Pin pin){
        gpioProvider.setAlwaysOff(pin);
    }

    public void setPwm(Pin pin, int value){
        int correctedValue = correctPwmValue(value);
        //This selection is needed to turn completely off, if value is 0. gpioProvider.setPwm(pin, 0) isn't allowed normally.
        if(correctedValue == 0){
            gpioProvider.setAlwaysOff(pin);
        } else {
            gpioProvider.setPwm(pin, value);
        }
    }

    public void setAllPinsOn(){
        for (Pin pin : PCA9685Pin.ALL) {
            gpioProvider.setAlwaysOn(pin);
        }
    }

    public void setAllPinsOff(){
        for (Pin pin : PCA9685Pin.ALL) {
            gpioProvider.setAlwaysOff(pin);
        }
    }

    public void setAllPinsPwm(int value){
        for (Pin pin : PCA9685Pin.ALL) {
            setPwm(pin, value);
        }
    }

    private int correctPwmValue(int value){
        if(value > PWM_MAX_VALUE){
            return PWM_MAX_VALUE;
        } else if (value < PWM_OFF_VALUE) {
            return PWM_OFF_VALUE;
        } else {
            return value;
        }
    }

    private void setAllPinsAsOutput(GpioController gpio){
        for (Pin pin : PCA9685Pin.ALL) {
            gpio.provisionPwmOutputPin(gpioProvider, pin);
        }
    }
}
