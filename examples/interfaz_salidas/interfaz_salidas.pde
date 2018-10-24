/* //<>//
interfaz_salidas

*/

import processing.serial.*;

import cc.interfaz.*;


Interfaz ifaz;

Interfaz.OUTPUT s;

void setup() {
  surface.setVisible(false);

  // Prints out the available serial ports.
  println(Interfaz.list());
  
  ifaz = new Interfaz(this, "COM6");
  s = ifaz.output(1);
}

void draw() {
  s.power(255);
  s.on();
  delay(500);
  s.off();
  delay(500);
  s.direction(1);
  s.on();
  delay(500);
  s.direction(0);
  delay(500);
  s.brake();
  delay(500);
  s.on();
  s.power(100);
  delay(500);
}
