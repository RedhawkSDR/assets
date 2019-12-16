# REDHAWK Short File to Float File Demo
 
## Description

Contains the source and build script for the REDHAWK Short File to Float File
Waveform. This waveform reads in a file containing `shorts`, converts them to
`floats` in REDHAWK and then writes out the file. This waveform uses three
components, `rh.FileReader`, `rh.DataConverter`, and `rh.FileWriter`.

## Installation

This is a waveform project; therefore, it does not need to be built.  It must be installed into
the `$SDRROOT/dom/waveforms` directory. To install it, open the project
in the REDHAWK IDE and drag it into the Target SDR folder..
