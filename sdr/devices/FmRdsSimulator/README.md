# REDHAWK rh.FmRdsSimulator

## Description

Contains the source and build script for the REDHAWK
`rh.FmRdsSimulator` device. This device is designed to be used in conjunction with the
`libRfSimulators` library.  Using the simulator library, this FRONTEND Interfaces
compliant REDHAWK device will generate FM modulated mono or stereo audio with
RDS encoded PI (Call Sign), PS (Short Text), and RT (Full Text) data.

Additional documentation is available within the `libRfSimulators` README.

## Installation

This asset requires the `libRfSimulators` shared library, which must be installed in order to build and run this asset.  To build and run from source, run the `build.sh` script. To install to `$SDRROOT`, run `build.sh install`.

## Notes

The Simulator creates a processing thread for each station within the currently
visible 2.28 Mhz bandwidth (even if bandwidth is set smaller).  Since each of
these threads is resampling a wav file, FM modulating, encoding RDS, and
upsampling to 2.28 Msps a non-trivial amount of CPU is used. 

## FEI Compliance Test Results

See the [FEI Compliance Results](tests/FEI_Compliance_Results.md) document.
