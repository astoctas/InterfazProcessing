/* //<>//
interfaz_servo

*/

import processing.serial.*;

import cc.interfaz.*;


Interfaz ifaz;

Interfaz.SERVO s;

void setup() {
  surface.setVisible(false);

  // Prints out the available serial ports.
  println(Interfaz.list());
  
  ifaz = new Interfaz(this, "COM6");
  s = ifaz.servo(1);
}


void draw() {
  s.position(floor(random(180)));
  delay(500);

}
