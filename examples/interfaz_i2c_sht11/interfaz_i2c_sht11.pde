/*
interfaz_i2c_sht11  USING SHT11 Library
*/
 
import processing.serial.*;

// Import Libraries
import cc.interfaz.*;
import lab.sht11.*;

// Arduino handle
Interfaz ifaz;

// I2C handles
Interfaz.I2C sht11;
Interfaz.I2C.REG temp;
Interfaz.I2C.REG hum;

void setup() {
  surface.setVisible(false);
  
  // Prints out the available serial ports.
  println(Interfaz.list());
  
  // Modify this line, by changing the "0" to the index of the serial
  // port corresponding to your Arduino board (as it appears in the list
  // printed by the line above).
  //  arduino = new Interfaz(this, Interfaz.list()[0], 57600);
  // Alternatively, use the name of the serial port corresponding to your
  // Arduino (in double-quotes), as in the following line.
  ifaz = new Interfaz(this, "COM6");

// Assign SHT11 address to dev
  sht11 = ifaz.i2c(SHT11.address);

// Assign SHT11 temperature register to reg
  temp = sht11.register(SHT11.temperature.register);
// Assign SHT11 humidity register to reg
  hum = sht11.register(SHT11.humidity.register);
  
// Start reporting (3 bytes per register)
  temp.on(SHT11.temperature.bytes);
  hum.on(SHT11.humidity.bytes);
}

void draw() {
  delay(500); // Rest to wait first reporting
  // Get values
  print("Temperature: "); println(SHT11.temperature.value(temp.value()));
  print("Humidity: "); println(SHT11.humidity.value(hum.value()));
  
  
}
