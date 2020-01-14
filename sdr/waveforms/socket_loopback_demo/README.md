# REDHAWK socket_loopback_demo
 
## Description

Contains the source and build script for the REDHAWK `socket_loopback_demo`
waveform. This waveform puts REDHAWK data out onto a network socket using
`rh.sinksocket` and then reads it back from the socket into REDHAWK via `rh.sourcesocket`.

## Installation

This is a waveform project; therefore, it does not need to be built.  It must be installed into
the `$SDRROOT/dom/waveforms` directory. To install it, open the project
in the REDHAWK IDE and drag it into the Target SDR folder.
