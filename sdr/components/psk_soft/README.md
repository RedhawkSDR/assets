# REDHAWK rh.psk_soft
 
## Description

Contains the source and build script for the REDHAWK
`rh.psk_soft` PSK demodulator component. The component takes complex baseband pre-da data and
does a PSK demodulation of either BPSK, QPSK, or 8-PSK and outputs symbols and
bits. Input must be an integer number of samples per symbol (recommended 8-10).

## Installation

To build from source, run the `build.sh` script.
To install to `$SDRROOT`, run `build.sh install`.
