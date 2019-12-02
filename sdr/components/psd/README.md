# REDHAWK rh.psd
 
## Description

Contains the source and build script for the REDHAWK `rh.psd`.
FFT-based power spectral density (PSD) component that transforms data from the
time domain to the frequency domain.  Output data is framed data where each
frame contains the frequency domain representation of a subsection of the input.

## Installation

This asset requires the `rh.dsp` and `rh.fftlib` shared libraries. These must be
installed in order to build and run this asset. To build from source, run the
`build.sh` script. To install to `$SDRROOT`, run `build.sh install`.
