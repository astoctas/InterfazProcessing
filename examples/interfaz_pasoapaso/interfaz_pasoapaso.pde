/* //<>//
interfaz_salidas

*/

import processing.serial.*;

import cc.interfaz.*;


Interfaz ifaz;

Interfaz.STEPPER s;

void setup() {
  surface.setVisible(false);

  // Prints out the available serial ports.
  println(Interfaz.list());
  
  ifaz = new Interfaz(this, "COM6");
  s = ifaz.stepper(1);
}


void draw() {
  s.direction(0);
  s.speed(100);
  s.steps(255);
  while(s.status() > 0) {
    delay(1);
  }
  s.direction(1);
  s.steps(1000);
  delay(500);
  s.stop();
  delay(500);
  s.speed(50);
  s.steps(100);
  while(s.status() > 0) {
    delay(1);
   }

}
