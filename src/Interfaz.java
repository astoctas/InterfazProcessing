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

import org.firmata.Firmata;

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

    try {
      Thread.sleep(3000); // let bootloader timeout
    } catch (InterruptedException e) {
    }

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

    public void enableOutputsStepper() {
      int[] data = { FIRMATA_STEPPER_REQUEST, FIRMATA_STEPPER_ENABLE, index, 0x01 };
      firmata.sendSysex(data);
    }

    public void disableOutputsStepper() {
      int[] data = { FIRMATA_STEPPER_REQUEST, FIRMATA_STEPPER_ENABLE, index, 0x00 };
      firmata.sendSysex(data);
    }

    public void steps(int steps) {
      steps = direction > 0 ? steps * -1: steps;
      int[] encoded = encode32BitSignedInteger(steps);
      int[] data = { FIRMATA_STEPPER_REQUEST, FIRMATA_STEPPER_STEP, index, encoded[0], encoded[1], encoded[2], encoded[3], encoded[4]  };
      status(1);
      enableOutputsStepper();
      firmata.sendSysex(data);
    }

    public void stop() {
      int[] data = { FIRMATA_STEPPER_REQUEST, FIRMATA_STEPPER_STOP, index };
      firmata.sendSysex(data);
    }

    /**
     * Sets power to an output
     * 
     * @param pow the power
     */
    public void speed(int pow) {
      int[] speed = encodeCustomFloat(pow);
      int[] data = { FIRMATA_STEPPER_REQUEST, FIRMATA_STEPPER_SPEED, index, speed[0], speed[1], speed[2], speed[3] };
      firmata.sendSysex(data);
    }

    /**
    * Gets power of an output
    */
    public int speed() {
      return speed;
    }

    public int status() {
      return firmata.stepperData(index);
    }

    public void status(int value) {
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

  

}