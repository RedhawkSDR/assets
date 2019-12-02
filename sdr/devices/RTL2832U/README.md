# REDHAWK rh.RTL2832U

## Description

Contains the source and build script for the REDHAWK `rh.RTL2832U`.
Realtek RTL2832U usb dongle device using `librtlsdr`. Supports various tuners,
including Elonics E4000, Rafael Micro R820T and R828D, Fitipower FC0012 and
FC0013, and FCI FC2580.

## Dependencies

This device depends on the RTL SDR library, version 0.5.2.

## Installation

This asset requires the `librtlsdr` library. This must be installed in order to
build and run this asset. To build from source, run the `build.sh` script.
To install to `$SDRROOT`, run `build.sh install`.

## Usage

Prior to launching this device, the `LD_LIBRARY_PATH` may need to be updated to
include the `librtlsdr` library
(i.e. `export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/usr/local/lib)`.

There is currently a bug where the RTL interface may not work when the device
debug level is set at 'DEBUG' or 'TRACE'.  If issues occur and your debug level
is at 'DEBUG' or 'TRACE', try and set the debug level as 'INFO'.

## FEI Compliance Test Results

See the [FEI Compliance Results](tests/FEI_Compliance_Results.md) document.
