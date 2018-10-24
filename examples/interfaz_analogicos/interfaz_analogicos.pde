/* //<>//
interfaz_analogicos

*/

import processing.serial.*;

import cc.interfaz.*;


Interfaz ifaz;

Interfaz.ANALOG a;

void setup() {
  surface.setVisible(false);

  // Prints out the available serial ports.
  println(Interfaz.list());
  
  ifaz = new Interfaz(this, "COM6");
  a = ifaz.analog(2);
  a.on();
}

void draw() {
  println(a.value());
  delay(500);
}
