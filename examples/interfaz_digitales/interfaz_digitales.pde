/* //<>//
interfaz_digitales

*/

import processing.serial.*;

import cc.interfaz.*;


Interfaz ifaz;

Interfaz.DIGITAL d;

void setup() {
  surface.setVisible(false);

  // Prints out the available serial ports.
  println(Interfaz.list());
  
  ifaz = new Interfaz(this, "COM6");
  d = ifaz.digital();
  d.on();
}

void draw() {
  delay(1);
}

void digitalEvent() {
  // PRINTS list of bits
  print(d.value(1));
  print(d.value(2));
  print(d.value(3));
  print(d.value(4));
  print(d.value(5));
  println(d.value(6));
  // PRINTS port value
  println(d.value());
}
