(C) 2013 tobias.gierke@code-sourcery.de

Simple mass-spring simulation in Java
-------------------------------------

This project contains a simple mass-spring simulation along with a Swing App that showcases it.

Requirements
------------

- Maven 2.2.1 (should work with Maven3 as well)
- JDK 1.7 or higher

Running
-------

To compile and run:

  mvn compile exec:java

Controls
--------

The viewer features some secret (read: undocumented) controls:

- Hitting the 's' key will start/pause the simulation
- Left-clicking and dragging can be used to manually displace point-masses
- Right-clicking locks (or unlocks) point-masses in space
