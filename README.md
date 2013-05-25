(C) 2013 tobias.gierke@code-sourcery.de

Simple mass-spring simulation n Java
-------------------------------------

This project contains a simple mass-spring simulation along with a Swing App that showcases it.

To compile and run:

  mvn clean package
  java -jar target/springmass.jar

Controls
--------

The viewer features some secret (read: undocumented) controls:

- Hitting the 's' key will start/pause the simulation
- Left-clicking and dragging can be used to manually displace point-masses
- Right-clicking locks (or unlocks) point-masses in space

A LOT of other knobs are currently only available in the source code, have a look at
constants at the top of de.codesourcery.springmass.springmass.ClothPanel to see what's there.

