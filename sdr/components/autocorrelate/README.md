# REDHAWK rh.autocorrelate
 
## Description

Contains the source and build script for the REDHAWK
`rh.autocorrelate` component. This component is a frequency domain implementation of a
windowed autocorrelation algorithm.  This algorithm works by windowing the
input data to break it up into separate frames.  Each frame is independently
autocorrelated with each other using a &quot;full&quot; autocorrelation, which
includes the full transient response.  This is efficiently computed in the
frequency domain.

## Installation

This asset requires the `rh.dsp` and `rh.fftlib` shared libraries. These must be
installed in order to build and run this asset. To build from source, run the
`build.sh` script. To install to `$SDRROOT`, run `build.sh install`.
