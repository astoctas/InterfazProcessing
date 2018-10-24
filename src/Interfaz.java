/**
 * Arduino.java - Arduino/firmata library for Processing
 * Copyright (C) 2006-08 David A. Mellis
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General
 * Public License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA  02111-1307  USA
 *
 * Processing code to communicate with the Arduino Firmata 2 firmware.
 * http://firmata.org/
 *
 * $Id$
 */

package cc.interfaz;

import processing.core.PApplet;
import processing.serial.Serial;
import java.lang.reflect.*;
import java.util.HashMap;
import java.util.Observer;
import java.util.Observable;

import org.firmata.Firmata;
//import cc.digitalobserver.*;

class DigitalObserver implements Observer {
  Interfaz.DIGITAL dg;

  public void setInstance(Interfaz.DIGITAL _dg) {
    dg = _dg;
  }
  public void update(Observable obs, Object obj) {
    dg.digitalEvent();
  }    
}

/**
 * Together with the Firmata 2 firmware (an Arduino sketch uploaded to the
 * Arduino board), this class allows you to control the Arduino board from
 * Processing: reading from and writing to the digital pins and reading the
 * analog inputs.
 */
public class Interfaz {
  /**
   * Constant to set a pin to input mode (in a call to pinMode()).
   */
  public static final int INPUT = 0;
  /**
   * Constant to set a pin to output mode (in a call to pinMode()).
   */
  public static final int OUTPUT = 1;
  /**
   * Constant to set a pin to analog mode (in a call to pinMode()).
   */
  public static final int ANALOG = 2;
  /**
   * Constant to set a pin to PWM mode (in a call to pinMode()).
   */
  public static final int PWM = 3;
  /**
   * Constant to set a pin to servo mode (in a call to pinMode()).
   */
  public static final int SERVO = 4;
  /**
   * Constant to set a pin to shiftIn/shiftOut mode (in a call to pinMode()).
   */
  public static final int SHIFT = 5;
  /**
   * Constant to set a pin to I2C mode (in a call to pinMode()).
   */
  public static final int I2C = 6;
  /**
   * Constant to set a pin to input mode and enable the pull-up resistor (in a call to pinMode()).
   */
  public static final int INPUT_PULLUP = 11;

  /**
   * Constant to write a high value (+5 volts) to a pin (in a call to
   * digitalWrite()).
   */
  public static final int LOW = 0;
  /**
   * Constant to write a low value (0 volts) to a pin (in a call to
   * digitalWrite()).
   */
  public static final int HIGH = 1;

  private final int FIRMATA_REPORT_ANALOG          = 0xC0; // enable analog input by pin #

  private static final int FIRMATA_LCD_REQUEST = 3;
  private static final int FIRMATA_LCD_PRINT = 0;
  private static final int FIRMATA_LCD_PUSH = 1;
  private static final int FIRMATA_LCD_CLEAR = 2;

  private static final int  FIRMATA_DC_REQUEST		 = 2;
  private static final int  FIRMATA_DC_CONFIG		 = 0;
  private static final int  FIRMATA_DC_ON			 = 1;
  private static final int  FIRMATA_DC_OFF			 = 2;
  private static final int  FIRMATA_DC_BRAKE		 = 3;
  private static final int  FIRMATA_DC_INVERSE		 = 4;
  private static final int  FIRMATA_DC_DIR			 = 5;
  private static final int FIRMATA_DC_SPEED = 6;

  private static final int FIRMATA_STEPPER_REQUEST = 0x62;
  private static final int FIRMATA_STEPPER_CONFIG = 0x00;
  private static final int FIRMATA_STEPPER_STEP = 0x02;
  private static final int FIRMATA_STEPPER_ENABLE = 0x04;
  private static final int FIRMATA_STEPPER_STOP = 0x05;
  private static final int FIRMATA_STEPPER_REPORT_POSITION = 0x06;
  private static final int FIRMATA_STEPPER_ACCEL = 0x08;
  private static final int FIRMATA_STEPPER_SPEED = 0x09;
  private static final int FIRMATA_STEPPER_MOVE_COMPLETE = 0x0a;
  
  private static final int FIRMATA_EXTENDED_ANALOG = 0x6F;
 
  private static final int  FIRMATA_I2C_REQUEST	 = 0x76;
  private static final int  FIRMATA_I2C_REPLY	 = 0x77;
  private static final int  FIRMATA_I2C_CONFIG	 = 0x78;
  private static final int  FIRMATA_I2C_AUTO_RESTART	 = 0x40;
  private static final int  FIRMATA_I2C_10_BIT		 = 0x20;
  private static final int  FIRMATA_I2C_WRITE		 = 0x00;
  private static final int  FIRMATA_I2C_READ_ONCE		 = 0x08;
  private static final int  FIRMATA_I2C_READ_CONTINUOUS	 = 0x10;
  private static final int FIRMATA_I2C_STOP_READING = 0x18;
  private static final double MAX_SIGNIFICAND = 8388608; // 2^23

  
  PApplet parent;
  Serial serial;
  SerialProxy serialProxy;
  Firmata firmata;

  // We need a class descended from PApplet so that we can override the
  // serialEvent() method to capture serial data.  We can't use the Arduino
  // class itself, because PApplet defines a list() method that couldn't be
  // overridden by the static list() method we use to return the available
  // serial ports.  This class needs to be public so that the Serial class
  // can access its serialEvent() method.
  public class SerialProxy extends PApplet {
    public SerialProxy() {
    }

    public void serialEvent(Serial which) {
      try {
        // Notify the Arduino class that there's serial data for it to process.
        while (which.available() > 0)
          firmata.processInput(which.read());
      } catch (Exception e) {
        e.printStackTrace();
        throw new RuntimeException("Error inside Arduino.serialEvent()");
      }
    }
  }

  public class FirmataWriter implements Firmata.Writer {
    public void write(int val) {
      serial.write(val);
      //      System.out.print("<" + val + " ");
    }
  }

  public void dispose() {
    this.serial.dispose();
  }

  /**
   * Get a list of the available Arduino boards; currently all serial devices
   * (i.e. the same as Serial.list()).  In theory, this should figure out
   * what's an Arduino board and what's not.
   */
  public static String[] list() {
    return Serial.list();
  }

  /**
   * Create a proxy to an Arduino board running the Firmata 2 firmware at the
   * default baud rate of 57600.
   *
   * @param parent the Processing sketch creating this Arduino board
   * (i.e. "this").
   * @param iname the name of the serial device associated with the Arduino
   * board (e.g. one the elements of the array returned by Arduino.list())
   */
  public Interfaz(PApplet parent, String iname) {
    this(parent, iname, 57600);
  }

  /**
   * Create a proxy to an Arduino board running the Firmata 2 firmware.
   *
   * @param parent the Processing sketch creating this Arduino board
   * (i.e. "this").
   * @param iname the name of the serial device associated with the Arduino
   * board (e.g. one the elements of the array returned by Arduino.list())
   * @param irate the baud rate to use to communicate with the Arduino board
   * (the firmata library defaults to 57600, and the examples use this rate,
   * but other firmwares may override it)
   */
  public Interfaz(PApplet parent, String iname, int irate) {
    this.parent = parent;
    this.firmata = new Firmata(new FirmataWriter());
    this.serialProxy = new SerialProxy();
    this.serial = new Serial(serialProxy, iname, irate);

    parent.registerMethod("dispose", this);
    /*
    try {
      Thread.sleep(3000); // let bootloader timeout
    } catch (InterruptedException e) {
    }
    */
    firmata.init();
    try {
      Thread.sleep(4000); // let firmware communication timeout
    } catch (InterruptedException e) {
    }

  }


  /**
   * Returns the last known value read from the digital pin: HIGH or LOW.
   *
   * @param pin the digital pin whose value should be returned (from 2 to 13,
   * since pins 0 and 1 are used for serial communication)
   */
  public int digitalRead(int pin) {
    return firmata.digitalRead(pin);
  }

  /**
   * Returns the last known value read from the analog pin: 0 (0 volts) to
   * 1023 (5 volts).
   *
   * @param pin the analog pin whose value should be returned (from 0 to 5)
   */
  public int analogRead(int pin) {
    return firmata.analogRead(pin);
  }

  /**
   * Set a digital pin to input or output mode.
   *
   * @param pin the pin whose mode to set (from 2 to 13)
   * @param mode either Arduino.INPUT or Arduino.OUTPUT
   */
  public void pinMode(int pin, int mode) {
    try {
      firmata.pinMode(pin, mode);
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException("Error inside Arduino.pinMode()");
    }
  }

  /**
   * Write to a digital pin (the pin must have been put into output mode with
   * pinMode()).
   *
   * @param pin the pin to write to (from 2 to 13)
   * @param value the value to write: Arduino.LOW (0 volts) or Arduino.HIGH
   * (5 volts)
   */
  public void digitalWrite(int pin, int value) {
    try {
      firmata.digitalWrite(pin, value);
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException("Error inside Arduino.digitalWrite()");
    }
  }

  /**
   * Write an analog value (PWM-wave) to a digital pin.
   *
   * @param pin the pin to write to (must be 9, 10, or 11, as those are they
   * only ones which support hardware pwm)
   * @param value the value: 0 being the lowest (always off), and 255 the highest
   * (always on)
   */
  public void analogWrite(int pin, int value) {
    try {
      firmata.analogWrite(pin, value);
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException("Error inside Arduino.analogWrite()");
    }
  }

  /**
   * Write a value to a servo pin.
   *
   * @param pin the pin the servo is attached to
   * @param value the value: 0 being the lowest angle, and 180 the highest angle
   */
  public void servoWrite(int pin, int value) {
    try {
      firmata.servoWrite(pin, value);
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException("Error inside Arduino.servoWrite()");
    }
  }

  /*
  LCD
  */

  public class LCD {

  /**
   * Clears LCD screen
   *
   */
    public void clear() {
      int[] data = { FIRMATA_LCD_REQUEST, FIRMATA_LCD_CLEAR };
      firmata.sendSysex(data);
    }

  /**
   * Prints a text on a single row of screen
   *
   * @param row the row
   * @param str the text to print (max 16 chars)
   */
  public void print(int row, String str) {
      int[] data = new int[str.length() * 2 + 3];
      data[0] = FIRMATA_LCD_REQUEST;
      data[1] = FIRMATA_LCD_PRINT;
      data[2] = row;
      int i = 0;
      for (char c : str.toCharArray()) {
        data[i * 2 + 3] = (int) c & 0x7F;
        data[i * 2 + 4] = ((int) c >> 7) & 0x7F;
        i++;
      }
      firmata.sendSysex(data);
    }

  }

  /**
   * Returns LCD Instance
   *
   */
  public LCD lcd() {
    return new LCD();
  }

  /*
  * OUTPUT
  */
  
  public class OUTPUT {
    private int index;
    private int direction;
    private int power;

    public OUTPUT(int _index) {
      index = _index - 1;
    }

  /**
   * Turns on an output
   */
    public void on() {
      int[] data = {FIRMATA_DC_REQUEST, FIRMATA_DC_ON, index};
      firmata.sendSysex(data);
    }
  /**
   * Turns off an output
   */
  public void off() {
      int[] data = {FIRMATA_DC_REQUEST, FIRMATA_DC_OFF, index};
      firmata.sendSysex(data);
    }
  /**
   * Applies brake to an output
   */
    public void brake() {
      int[] data = { FIRMATA_DC_REQUEST, FIRMATA_DC_BRAKE, index };
      firmata.sendSysex(data);
    }
    
    /**
     * Sets direction to an output
     * 
     * @param dir the direction
     */
    public void direction(int dir) {
      direction = dir;
      int[] data = { FIRMATA_DC_REQUEST, FIRMATA_DC_DIR, index, direction };
      firmata.sendSysex(data);
    }

  /**
   * Gets direction of an output
   */
    public int direction() {
      return direction;
    }

    /**
     * Sets power to an output
     * 
     * @param pow the power
     */
    public void power(int pow) {
      power = pow;
      int[] data = { FIRMATA_DC_REQUEST, FIRMATA_DC_SPEED, index, power };
      firmata.sendSysex(data);
    }

    /**
   * Gets power of an output
   */
    public int power() {
      return power;
    }

  }

  /**
   * Returns OUTPUT Instance
   *
   */
  public OUTPUT output(int index) {
    if (index < 1 || index > 8) {
      throw new RuntimeException("Outputs are from 1 to 8");
    }
    return new OUTPUT(index);
  }

  /*
  * Steppers
  */
  public class STEPPER {
    private int index;
    private int direction = 0;
    private int speed = 100;

    public STEPPER(int _index) {
      index = _index - 1;
    }

    int[] encode32BitSignedInteger(int data) {
      int[] encoded = { 0, 0, 0, 0, 0 };
      boolean negative = data < 0;

      int d = Math.abs(data);

      encoded[0] = d & 0x7F;
      encoded[1] = (d >> 7) & 0x7F;
      encoded[2] = (d >> 14) & 0x7F;
      encoded[3] = (d >> 21) & 0x7F;
      encoded[4] = (d >> 28) & 0x07;

      if (negative) {
        encoded[4] |= 0x08;
      }

      return encoded;
    }

    int[] encodeCustomFloat(double input) {
      int[] encoded = { 0, 0, 0, 0 };
      int exponent = 0;
      int sign = input < 0 ? 1 : 0;
  
      //input = abs((long)input);
      double base10 = Math.floor(Math.log10(input));
  
      // Shift decimal to start of significand
      exponent += base10;
      input /= Math.pow(10, base10);
  
      // Shift decimal to the right as far as we can
      while ((input - Math.floor(input) > 0) // ES FLOAT?
        && input < MAX_SIGNIFICAND) {
        exponent -= 1;
        input *= 10;
      }
      // Reduce precision if necessary
      while (input > MAX_SIGNIFICAND) {
        exponent += 1;
        input /= 10;
      }
      input = Math.floor(input);
      exponent += 11;
  
      encoded[0] = (int)input & 0x7f;
      encoded[1] = ((int)input >> 7) & 0x7f;
      encoded[2] = ((int)input >> 14) & 0x7f;
      encoded[3] = ((int)input >> 21) & 0x03 | (exponent & 0x0f) << 2 | (sign & 0x01) << 6;
  
      return encoded;
    }    

    /**
     * Sets direction to an output
     * 
     * @param dir the direction
     */
    public void direction(int dir) {
      direction = dir;
    }

    /**
     * Gets direction of an output
     */
    public int direction() {
      return direction;
    }

    private void enableOutputsStepper() {
      int[] data = { FIRMATA_STEPPER_REQUEST, FIRMATA_STEPPER_ENABLE, index, 0x01 };
      firmata.sendSysex(data);
    }

    private void disableOutputsStepper() {
      int[] data = { FIRMATA_STEPPER_REQUEST, FIRMATA_STEPPER_ENABLE, index, 0x00 };
      firmata.sendSysex(data);
    }

    /**
     * Sets steps to move and starts movement
     * 
     * @param steps the steps to move
     */    
    public void steps(int steps) {
      steps = direction > 0 ? steps * -1: steps;
      int[] encoded = encode32BitSignedInteger(steps);
      int[] data = { FIRMATA_STEPPER_REQUEST, FIRMATA_STEPPER_STEP, index, encoded[0], encoded[1], encoded[2], encoded[3], encoded[4]  };
      status(1);
      enableOutputsStepper();
      firmata.sendSysex(data);
    }

    /**
     * Stops the motor 
     * 
     */
    public void stop() {
      int[] data = { FIRMATA_STEPPER_REQUEST, FIRMATA_STEPPER_STOP, index };
      firmata.sendSysex(data);
    }

    /**
     * Sets speed of motor
     * 
     * @param speed the speed in steps per second
     */
    public void speed(int pow) {
      int[] speed = encodeCustomFloat(pow);
      int[] data = { FIRMATA_STEPPER_REQUEST, FIRMATA_STEPPER_SPEED, index, speed[0], speed[1], speed[2], speed[3] };
      firmata.sendSysex(data);
    }

    /**
    * Gets speed of a motor
    */
    public int speed() {
      return speed;
    }

    /**
     * Gets running status - 0: stopped, 1: running
     * 
     */    
    public int status() {
      return firmata.stepperData(index);
    }

    private void status(int value) {
      firmata.stepperData(index, value);
    }

  }

  /**
   * Returns STEPPER Instance
   *
   */
  public STEPPER stepper(int index) {
    if (index < 1 || index > 3) {
      throw new RuntimeException("Steppers are from 1 to 3");
    }    
    return new STEPPER(index);
  }

   /*
  * SERVOS
  */
  public class SERVO {
    private int index;
    private int position = 90;
    private int[] pins = { 10, 11, 12 };

    public SERVO(int _index) {
      index = _index - 1;
    }

    /**
     * Sets servo position
     * 
     * @param pos the position of servo
     */    
    public void position(int pos) {
      position = pos;
      int[] data = { FIRMATA_EXTENDED_ANALOG, pins[index], position & 0x7F, (position >> 7) & 0x7F };
      firmata.sendSysex(data);
    }
  }

  /**
   * Returns SERVO Instance
   *
   */
  public SERVO servo(int index) {
    if (index < 1 || index > 3) {
      throw new RuntimeException("Servos are from 1 to 3");
    }
    return new SERVO(index);
  }
  
  
   /*
  * ANALOG
  */
  public class ANALOG {
    private int index;
    //private int[] pins = {54,55,56,57,58,59,60,61};

    public ANALOG(int _index) {
      index = _index - 1;
    }

    /**
     * Starts reporting
     * 
     */    
    public void on() {
      firmata.reportAnalog(index, 1);
    }

    /**
     * Stops reporting
     * 
     */    
    public void off() {
      firmata.reportAnalog(index, 0);
    }
    
    /**
     * Gets last received value of analog
     * 
     */        
    public int value() {
      return firmata.analogRead(index);
    }

  }

  /**
   * Returns ANALOG Instance
   *
   */
  public ANALOG analog(int index) {
    if (index < 1 || index > 8) {
      throw new RuntimeException("Analogs are from 1 to 8");
    }    
    return new ANALOG(index);
  }    


   /*
  * DIGITAL
  */
  public class DIGITAL  {
    private int[] pins = { 64, 65, 66, 67, 68, 69 };
    private int port = 0x08;
    Method digitalEventMethod;
    DigitalObserver digitalObserver = new DigitalObserver();
    
    public DIGITAL() {
      digitalEventMethod = findCallback("digitalEvent");
      digitalObserver.setInstance(this);
      firmata.addObserver(firmata.digitalObservable, digitalObserver);
    }

    public void digitalEvent() {
      if(digitalEventMethod == null) return;
      try {
        digitalEventMethod.invoke(parent);
      } catch (Exception e) {
        throw new RuntimeException("Callback error");
      }      
    }

    private Method findCallback(final String name) {
      try {
        return parent.getClass().getMethod(name);
      } catch (Exception e) {
      }
      // Permit callback(Object) as alternative to callback(Serial).
      try {
        return parent.getClass().getMethod(name, this.getClass());
      } catch (Exception e) {
      }
      return null;
    }

    /**
     * Starts reporting
     * 
     */    
    public void on() {
      firmata.reportDigital(port, 1);
    }

    /**
     * Stops reporting
     * 
     */    
    public void off() {
      firmata.reportDigital(port, 0);
    }
    
    /**
     * Gets last received value of digital pin
     * 
     * @param index the digital pin
     */        
    public int value(int index) {
      return firmata.digitalRead(pins[index - 1]);
    }

     /**
     * Gets last received value of digital port
     * 
     */        
    public int value() {
      return firmata.digitalReadPort(port);
    }
    
    /**
     * Enables or disables pullup in digital pin
     * 
     * @param index the digital pin
     * @param enable true to enable, false to disable pullup
     */        
    public void pullup(int index, boolean enable) {
      int mode = (enable) ? 11 : 0;
      firmata.pinMode(pins[index - 1], mode);
    }

  }

  /**
   * Returns DIGITAL Instance
   *
   */
  public DIGITAL digital() {
    return new DIGITAL();
  }   

   /*
  * I2C
  */
  public class I2C {
    protected int address;
    private int delay;
    protected HashMap<Integer, REG> registers = new HashMap();

    public class REG {
      int register;

      public REG(int _register) {
        register = _register;
      }

      /**
       * Starts reporting
       * 
       * @param bytes the amount of bytes to report from register
       */    
      public void on(int bytes) {

        int address_lsb = address & 0x7F;
        int address_msb = (address >> 7) & 0x7F;
    
        if (address_msb > 0) {
          address_msb |= FIRMATA_I2C_10_BIT;
        }
        if (bytes == 0) {
          address_msb |= FIRMATA_I2C_STOP_READING;
        }
        else {
          address_msb |= FIRMATA_I2C_READ_CONTINUOUS;		
        }
    
        int bytes_lsb = (bytes & 0x7F);
        int bytes_msb = (bytes >> 7) & 0x7F;
    
        int register_lsb = (register & 0x7F);
        int register_msb = (register >> 7) & 0x7F;
    
        int[] data = { FIRMATA_I2C_REQUEST, address_lsb, address_msb, register_lsb, register_msb, bytes_lsb, bytes_msb };
        firmata.sendSysex(data);
            
      }
  
      /**
       * Stops reporting
       * 
       */    
      public void off() {
        on(0);
      }
      
      /**
       * Gets last received value of analog
       * 
       */        
      public int[] value() {
        return firmata.getI2CInputs(address, register);
      }

      /**
       * Performs a write of data on the register 
       * 
       * @param data the array of data to write into register
       */        
      public void write(int[] data) {
        int address_lsb = address & 0x7F;
        int address_msb = (address >> 7) & 0x7F;
        if (address_msb > 0) {
          address_msb |= FIRMATA_I2C_10_BIT;
        }
        address_msb |= FIRMATA_I2C_WRITE;
        int register_lsb = (register & 0x7F);
        int register_msb = (register >> 7) & 0x7F;

        int[] dataToWrite = new int[data.length * 2 + 5];
        dataToWrite[0] = FIRMATA_I2C_REQUEST;
        dataToWrite[1] = address_lsb;
        dataToWrite[2] = address_msb;
        dataToWrite[3] = register_lsb;
        dataToWrite[4] = register_msb;
        int j = 5;
        for (int d : data) {
          dataToWrite[j++] = d & 0x7F;
          dataToWrite[j++] = (d >> 7) & 0x7F;
        }
       	firmata.sendSysex(dataToWrite);
      }   

    }

    public I2C(int _address) {
      address = _address;
      delay = 50;
      int[] data = { FIRMATA_I2C_CONFIG, delay, 0 };
      firmata.sendSysex(data);
    }

    public I2C(int _address, int _delay) {
      address = _address;
      delay = _delay;
      int[] data = { FIRMATA_I2C_CONFIG, delay & 0x7F, (delay>>7) & 0x7F };
      firmata.sendSysex(data);
    }

    /**
     * Returns REGISTER Instance.  If the instance exists, returns the same existent
     *
     * @param _register the address of the register on device
     */    
    public REG register(int _register) {
      if(!registers.containsKey(_register)) {
        REG r = new REG(_register);
        registers.put(_register, r);
      }
      return registers.get(_register);

    }


  }

  /**
   * Returns I2C Instance
   *
   * @param address the address of device
   */
  public I2C i2c(int address) {
    return new I2C(address);
  }
  
  /**
   * Returns I2C Instance with delay
   *
   * @param address the address of device
   * @param delay the delay between write and read on device in microseconds
   */
  public I2C i2c(int address, int delay) {
    return new I2C(address, delay);
  }
  
}