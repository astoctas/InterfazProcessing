/**
 * Firmata.java - Firmata library for Java
 * Copyright (C) 2006-13 David A. Mellis
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
 * Java code to communicate with the Arduino Firmata 2 firmware.
 * http://firmata.org/
 *
 * $Id$
 */

package org.firmata; // hope this is okay!

import java.util.HashMap;
import java.util.Observable;
import java.util.Observer;

class DigitalObservable extends Observable {
  public void change() {
    setChanged();
    notifyObservers();
  }
}

/**
 * Internal class used by the Arduino class to parse the Firmata protocol.
 */
public class Firmata {
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
   * Constant to set a pin to INPUT_PULLUP mode (in a call to pinMode()).
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

  private final int MAX_DATA_BYTES = 4096;

  private final int DIGITAL_MESSAGE        = 0x90; // send data for a digital port
  private final int ANALOG_MESSAGE         = 0xE0; // send data for an analog pin (or PWM)
  private final int REPORT_ANALOG          = 0xC0; // enable analog input by pin #
  private final int REPORT_DIGITAL         = 0xD0; // enable digital input by port
  private final int SET_PIN_MODE           = 0xF4; // set a pin to INPUT/OUTPUT/PWM/etc
  private final int REPORT_VERSION         = 0xF9; // report firmware version
  private final int SYSTEM_RESET           = 0xFF; // reset from MIDI
  private final int START_SYSEX            = 0xF0; // start a MIDI SysEx message
  private final int END_SYSEX              = 0xF7; // end a MIDI SysEx message
  private final int  FIRMATA_I2C_REPLY	 = 0x77;

  // extended command set using sysex (0-127/0x00-0x7F)
  /* 0x00-0x0F reserved for user-defined commands */
  private final int SERVO_CONFIG           = 0x70; // set max angle, minPulse, maxPulse, freq
  private final int STRING_DATA            = 0x71; // a string message with 14-bits per char
  private final int SHIFT_DATA             = 0x75; // a bitstream to/from a shift register
  private final int I2C_REQUEST            = 0x76; // send an I2C read/write request
  private final int I2C_REPLY              = 0x77; // a reply to an I2C read request
  private final int I2C_CONFIG             = 0x78; // config I2C settings such as delay times and power pins
  private final int EXTENDED_ANALOG        = 0x6F; // analog write (PWM, Servo, etc) to any pin
  private final int PIN_STATE_QUERY        = 0x6D; // ask for a pin's current mode and value
  private final int PIN_STATE_RESPONSE     = 0x6E; // reply with pin's current mode and value
  private final int CAPABILITY_QUERY       = 0x6B; // ask for supported modes and resolution of all pins
  private final int CAPABILITY_RESPONSE    = 0x6C; // reply with supported modes and resolution
  private final int ANALOG_MAPPING_QUERY   = 0x69; // ask for mapping of analog to pin numbers
  private final int ANALOG_MAPPING_RESPONSE= 0x6A; // reply with mapping info
  private final int REPORT_FIRMWARE        = 0x79; // report name and version of the firmware
  private final int SAMPLING_INTERVAL      = 0x7A; // set the poll rate of the main loop
  private final int SYSEX_NON_REALTIME     = 0x7E; // MIDI Reserved for non-realtime messages
  private final int SYSEX_REALTIME         = 0x7F; // MIDI Reserved for realtime messages
  private final int FIRMATA_STEPPER_REQUEST         = 0x62; // 
  private final int FIRMATA_STEPPER_MOVE_COMPLETE         = 0x0A; // 

  int waitForData = 0;
  int executeMultiByteCommand = 0;
  int multiByteChannel = 0;
  int[] storedInputData = new int[MAX_DATA_BYTES];
  boolean parsingSysex;
  int sysexBytesRead;

  int[] digitalOutputData = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
  int[] digitalInputData  = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
  int[] analogInputData   = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
  int[] steppersData = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
  HashMap<Integer, HashMap<Integer, int[]>> i2cData = new HashMap();

  private final int MAX_PINS = 128;

  int[] pinModes = new int[MAX_PINS];
  int[] analogChannel = new int[MAX_PINS];
  int[] pinMode = new int[MAX_PINS];

  int majorVersion = 0;
  int minorVersion = 0;

  public DigitalObservable digitalObservable = new DigitalObservable();

  /**
   * An interface that the Firmata class uses to write output to the Arduino
   * board. The implementation should forward the data over the actual
   * connection to the board.
   */
  public interface Writer {
    /**
     * Write a byte to the Arduino board. The implementation should forward
     * this using the actual connection.
     *
     * @param val the byte to write to the Arduino board
     */
    public void write(int val);
  }

  Writer out;

  /**
   * Create a proxy to an Arduino board running the Firmata 2 firmware.
   *
   * @param writer an instance of the Firmata.Writer interface
   */
  public Firmata(Writer writer) {
    this.out = writer;
  }

  public void init() {
    // enable all ports; firmware should ignore non-existent ones
    /*
    for (int i = 0; i < 16; i++) {
      out.write(REPORT_DIGITAL | i);
      out.write(1);
    }
    */
    //queryCapabilities();
    queryAnalogMapping();

    //    for (int i = 0; i < 16; i++) {
    //      out.write(REPORT_ANALOG | i);
    //      out.write(1);
    //    }

  }


  public void addObserver(Observable a, Observer b) {
    a.addObserver(b);
  }

  private void _delay() {
    try
    {
        Thread.sleep(10);
    }
    catch(InterruptedException ex)
    {
        Thread.currentThread().interrupt();
    }  
  }
  /**
   * Returns the last known value read from the digital pin: HIGH or LOW.
   *
   * @param pin the digital pin whose value should be returned (from 2 to 13,
   * since pins 0 and 1 are used for serial communication)
   */
  public int digitalRead(int pin) {
    return (digitalInputData[pin >> 3] >> (pin & 0x07)) & 0x01;
  }

  
  /**
   * Returns the last known value read from the digital port.
   *
   * @param port the digital port whose value should be returned 
   */
  public int digitalReadPort(int port) {
    return digitalInputData[port];
  }


  /**
   * Returns the last known value read from the analog pin: 0 (0 volts) to
   * 1023 (5 volts).
   *
   * @param pin the analog pin whose value should be returned (from 0 to 5)
   */
  public int analogRead(int pin) {
    return analogInputData[pin];
  }

  /**
   * Set a digital pin to input or output mode.
   *
   * @param pin the pin whose mode to set (from 2 to 13)
   * @param mode either Arduino.INPUT or Arduino.OUTPUT
   */
  public void pinMode(int pin, int mode) {
    out.write(SET_PIN_MODE);
    out.write(pin);
    out.write(mode);
    _delay();
  }

  
  /**
   * Sets analog reporting
   *
   * @param channel the analog channel to report
   * @param mode starts (1) or stops (0) reporting
   */
  public void reportAnalog(int channel, int mode) {
    out.write(REPORT_ANALOG | channel);
    out.write(mode);
    _delay();
  }
  
    /**
   * Sets digital reporting
   *
   * @param port the digital port  to report
   * @param mode starts (1) or stops (0) reporting
   */
  public void reportDigital(int port, int mode) {
    out.write(REPORT_DIGITAL |  port);
    out.write(mode);
    _delay();
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
    int portNumber = (pin >> 3) & 0x0F;

    if (value == 0)
      digitalOutputData[portNumber] &= ~(1 << (pin & 0x07));
    else
      digitalOutputData[portNumber] |= (1 << (pin & 0x07));

    out.write(DIGITAL_MESSAGE | portNumber);
    out.write(digitalOutputData[portNumber] & 0x7F);
    out.write(digitalOutputData[portNumber] >> 7);
    _delay();
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
    pinMode(pin, PWM);
    out.write(ANALOG_MESSAGE | (pin & 0x0F));
    out.write(value & 0x7F);
    out.write(value >> 7);
    _delay();
  }

  /**
   * Write a value to a servo pin.
   *
   * @param pin the pin the servo is attached to
   * @param value the value: 0 being the lowest angle, and 180 the highest angle
   */
  public void servoWrite(int pin, int value) {
    out.write(ANALOG_MESSAGE | (pin & 0x0F));
    out.write(value & 0x7F);
    out.write(value >> 7);
    _delay();
  }

  /**
   * Sends a sysex message.
   *
   * @param data array of bytes to send
   */
  public void sendSysex(int[] data) {
    out.write(START_SYSEX);
    for (int d : data) {
      out.write(d);
    }
    out.write(END_SYSEX);
    _delay();
  }

  public int[] getI2CInputs(int address, int register) {
    if (!i2cData.containsKey(address)) {
      int[] data = {};
      return data;
    }
    return i2cData.get(address).get(register);
  }


  public void setI2CInputs(int address, int register, int[] value) {
    if (!i2cData.containsKey(address)) {
//      Hashmap<Integer, Integer> r = new HashMap<>()
      i2cData.put(address, new HashMap());
    }
    i2cData.get(address).put(register, value);
  }

  private void setDigitalInputs(int portNumber, int portData) {
    //System.out.println("digital port " + portNumber + " is " + portData);
    digitalInputData[portNumber] = portData;
    digitalObservable.change();
  }

  private void setAnalogInput(int pin, int value) {
    //System.out.println("analog pin " + pin + " is " + value);
    analogInputData[pin] = value;
  }

  private void setVersion(int majorVersion, int minorVersion) {
    //System.out.println("version is " + majorVersion + "." + minorVersion);
    this.majorVersion = majorVersion;
    this.minorVersion = minorVersion;
  }

  private void queryCapabilities() {
    out.write(START_SYSEX);
    out.write(CAPABILITY_QUERY);
    out.write(END_SYSEX);
    _delay();
  }

  private void queryAnalogMapping() {
    out.write(START_SYSEX);
    out.write(ANALOG_MAPPING_QUERY);
    out.write(END_SYSEX);
    _delay();
  }

  public int stepperData(int index) {
    return steppersData[index];
  }

  public void stepperData(int index, int value) {
    steppersData[index] = value;
  }


private void processSysexMessage() {
//    System.out.print("[ ");
//    for (int i = 0; i < storedInputData.length; i++) System.out.print(storedInputData[i] + " ");
//    System.out.println("]");
    switch(storedInputData[0]) { //first byte in buffer is command
//      case CAPABILITY_RESPONSE:
//        for (int pin = 0; pin < pinModes.length; pin++) {
//          pinModes[pin] = 0;
//        }
//        for (int i = 1, pin = 0; pin < pinModes.length; pin++) {
//          for (;;) {
//            int val = storedInputData[i++];
//            if (val == 127) break;
//            pinModes[pin] |= (1 << val);
//            i++; // skip mode resolution for now
//          }
//          if (i == sysexBytesRead) break;
//        }
//        for (int port = 0; port < pinModes.length; port++) {
//          boolean used = false;
//          for (int i = 0; i < 8; i++) {
//            if (pinModes[port * 8 + pin] & (1 << INPUT) != 0) used = true;
//          }
//          if (used) {
//            out.write(REPORT_DIGITAL | port);
//            out.write(1);
//          }
//        }
//        break;
      case ANALOG_MAPPING_RESPONSE:
        for (int pin = 0; pin < analogChannel.length; pin++)
          analogChannel[pin] = 127;
        for (int i = 1; i < sysexBytesRead; i++)
          analogChannel[i - 1] = storedInputData[i];
        /*  
        for (int pin = 0; pin < analogChannel.length; pin++) {
          if (analogChannel[pin] != 127) {
            out.write(REPORT_ANALOG | analogChannel[pin]);
            out.write(1);
          }
        }
        */
      break;
      case FIRMATA_STEPPER_REQUEST:
        if (storedInputData[1] == FIRMATA_STEPPER_MOVE_COMPLETE) {
          steppersData[storedInputData[2]] = 0;
        }
      break;
    case FIRMATA_I2C_REPLY:
        int[] reply_buffer = new int[(sysexBytesRead - 5) / 2];
        int address = storedInputData[1] | storedInputData[2] << 7;
        int register = storedInputData[3] | storedInputData[4] << 7;
        int j = 0;
        for (int i = 5; i + 1 < sysexBytesRead; i = i + 2) {
          int reply_byte = (storedInputData[i] | storedInputData[i + 1] << 7) ;
          reply_buffer[j++] = reply_byte;
        }
        setI2CInputs(address, register, reply_buffer);
      break;
    }
  }

  public void processInput(int inputData) {
    int command;

//    System.out.print(">" + inputData + " ");

    if (parsingSysex) {
      if (inputData == END_SYSEX) {
        parsingSysex = false;
        processSysexMessage();
      } else {
        storedInputData[sysexBytesRead] = inputData;
        sysexBytesRead++;
      }
    } else if (waitForData > 0 && inputData < 128) {
      waitForData--;
      storedInputData[waitForData] = inputData;

      if (executeMultiByteCommand != 0 && waitForData == 0) {
        //we got everything
        switch(executeMultiByteCommand) {
        case DIGITAL_MESSAGE:
          setDigitalInputs(multiByteChannel, (storedInputData[0] << 7) + storedInputData[1]);
          break;
        case ANALOG_MESSAGE:
          setAnalogInput(multiByteChannel, (storedInputData[0] << 7) + storedInputData[1]);
          break;
        case REPORT_VERSION:
          setVersion(storedInputData[1], storedInputData[0]);
          break;
        }
      }
    } else {
      if(inputData < 0xF0) {
        command = inputData & 0xF0;
        multiByteChannel = inputData & 0x0F;
      } else {
        command = inputData;
        // commands in the 0xF* range don't use channel data
      }
      switch (command) {
      case DIGITAL_MESSAGE:
      case ANALOG_MESSAGE:
      case REPORT_VERSION:
        waitForData = 2;
        executeMultiByteCommand = command;
        break;
      case START_SYSEX:
        parsingSysex = true;
        sysexBytesRead = 0;
        break;
      }
    }
  }
}
