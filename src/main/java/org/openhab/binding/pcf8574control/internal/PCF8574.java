//package org.openhab.binding.pcf8574control.internal;
//
//import java.io.IOException;
//import java.util.ArrayList;
//
//import com.pi4j.gpio.extension.pcf.PCF8574GpioProvider;
//import com.pi4j.gpio.extension.pcf.PCF8574Pin;
//
//import com.pi4j.io.gpio.GpioController;
//import com.pi4j.io.gpio.GpioFactory;
//import com.pi4j.io.gpio.GpioPinDigitalInput;
//import com.pi4j.io.gpio.GpioPinDigitalOutput;
//import com.pi4j.io.gpio.PinState;
//import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
//import com.pi4j.io.gpio.event.GpioPinListenerDigital;
//import com.pi4j.io.i2c.I2CBus;
//import com.sun.xml.internal.bind.v2.schemagen.xmlschema.List;
//
//public class PCF8574 {
//	private void run() throws IOException, InterruptedException{
//		// create gpio controller
//	    final GpioController gpio = GpioFactory.getInstance();
//	    
//	    // create custom MCP23017 GPIO provider
//	    final PCF8574GpioProvider gpioProvider = new PCF8574GpioProvider(I2CBus.BUS_1, PCF8574GpioProvider.PCF8574A_0x3F);
//	    
//	    // provision gpio input pins from MCP23017
//	    GpioPinDigitalInput myInputs[] = {
//	            gpio.provisionDigitalInputPin(gpioProvider, PCF8574Pin.GPIO_00),
//	            gpio.provisionDigitalInputPin(gpioProvider, PCF8574Pin.GPIO_01),
//	            gpio.provisionDigitalInputPin(gpioProvider, PCF8574Pin.GPIO_02)
//	        };
//	    
////	    ArrayList<GpioPinDigitalInput> myInputs = new ArrayList<>();
////	    for (int i = 0; i < 3; i++) {
////	    	myInputs.add(gpio.provisionDigitalInputPin(gpioProvider, PCF8574Pin.ALL[i]));
////		}	    
//	    
//	    // create and register gpio pin listener
//	    gpio.addListener(new GpioPinListenerDigital() {
//	        @Override
//	        public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
//	            // display pin state on console
//	            System.out.println(" --> GPIO PIN STATE CHANGE: " + event.getPin() + " = "
//	                    + event.getState());
//	        }
//	    }, myInputs);
//	    
//	    // provision gpio output pins and make sure they are all LOW at startup
//	    GpioPinDigitalOutput myOutputs[] = { 
//	        gpio.provisionDigitalOutputPin(gpioProvider, PCF8574Pin.GPIO_04, PinState.LOW),
//	        gpio.provisionDigitalOutputPin(gpioProvider, PCF8574Pin.GPIO_05, PinState.LOW),
//	        gpio.provisionDigitalOutputPin(gpioProvider, PCF8574Pin.GPIO_06, PinState.LOW)
//	      };
//	
//	    // on program shutdown, set the pins back to their default state: HIGH 
//	    gpio.setShutdownOptions(true, PinState.HIGH, myOutputs);
//	    
//	    // keep program running for 20 seconds
//	    for (int count = 0; count < 10; count++) {
//	        gpio.setState(true, myOutputs);
//	        gpio.setState(false, gpio.provisionDigitalOutputPin(gpioProvider, PCF8574Pin.ALL[0]));
//	        Thread.sleep(1000);
//	        gpio.setState(false, myOutputs);
//	        Thread.sleep(1000);
//	    }
//	}
//}
