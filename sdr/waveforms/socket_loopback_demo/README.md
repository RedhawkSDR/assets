# REDHAWK Socket Loopback Demo
 
## Description

Contains the source and build script for the REDHAWK Socket Loopback Demo
Waveform. This waveform puts REDHAWK data out onto a network socket using
`rh.sinksocket` and then reads it back from the socket into REDHAWK via `rh.sourcesocket`.

## Installation

This is a waveform project and thus does not need to be built just installed into
the `$SDRROOT/dom/waveforms` directory. One way to do that is to open the project
in the REDHAWK IDE and drag it into the Target SDR Folder.
