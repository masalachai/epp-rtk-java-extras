# EPP RTK Java Extras

This library extends the Java EPP Registrar Toolkit (http://sourceforge.net/projects/epp-rtk/files/epp-rtk-java/) and adds the following features.

1) The ability to bind a TLS connection to a particular local IP address and port because registries tend to restrict access by IP.
This is useful in the case that you have mutliple interfaces on your machine.

2) The ability to pass in the path for the directory of your SSL keystore and config, instead of having to set a system property.
I have multiple EPP RTK deployments on the same machine and not all use the same keystore.

3) A method to fetch the greeting in XML form

There are more unorganized modifications that are being used for EPP RTK. I will release those as well after some refactoring.

To build the library, just clone the repo and run the ant build script.

To use it, just create objects of the inherited EPPClient class provided in this library instead of the EPP RTK one.
