# REDHAWK rh.ArbitraryRateResampler

## Description

Contains the source and build script for the REDHAWK
`rh.ArbitraryRateResampler`.  Resamples a data stream at output rates which are not
limited to integer multiples of the input sampling rate.  This component can
increase or decrease the sample rate.  No anti-aliasing filtering is included,
so users must use this component with caution when decreasing the sampling rate
to avoid aliasing or pre-filter themselves in an upstream component if required.


## Installation

This asset requires the `rh.dsp` shared library, which must be installed in order
to build and run this asset. To build from source, run the `build.sh` script.
To install to `$SDRROOT`, run `build.sh install`.

