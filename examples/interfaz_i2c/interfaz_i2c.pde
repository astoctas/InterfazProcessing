/*
interfaz_i2c

*/

import processing.serial.*;

// Import Library
import cc.interfaz.*;

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
  sht11 = ifaz.i2c(0x40);

// Assign SHT11 temperature register to reg
  temp = sht11.register(0xE3);
// Assign SHT11 humidity register to reg
  hum = sht11.register(0xE5); 
  
// Start reporting (3 bytes per register)
  temp.on(3);
  hum.on(3);
}

void draw() {
  delay(500); // Rest to wait first reporting
  // Get values
  int[] temp_reg = temp.value();
  int[] hum_reg = hum.value();
  
  // Process values
  float temp =  temp_reg[0] << 8 | temp_reg[1];
  temp *= 175.72;
  temp /= 65536;
  temp -= 46.85;
  print("Temperature: "); println(temp);

  float hum =  hum_reg[0] << 8 | hum_reg[1];
  hum *= 125;
  hum /= 65536;
  hum -=6;
  print("Humidity: "); println(hum);
  
  
}
