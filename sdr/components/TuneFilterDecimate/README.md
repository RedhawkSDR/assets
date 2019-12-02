# REDHAWK rh.TuneFilterDecimate
 
## Description

Contains the source and build script for the REDHAWK
`rh.TuneFilterDecimate`. This component selects a narrowband cut from an input
signal. Tuning, filtering and decimation are used to remove noise and
interference in other frequency bands and reduce the sampling rate for more
efficient downstream processing.
 
## Installation

This asset requires the `rh.dsp` and `rh.fftlib` shared libraries. These must be
installed in order to build and run this asset. To build from source, run the
`build.sh` script. To install to `$SDRROOT`, run `build.sh install`.
