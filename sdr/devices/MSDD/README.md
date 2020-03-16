# REDHAWK rh.MSDD
 
## Description

Contains the source and build script for the REDHAWK `rh.MSDD`
device. This device is a FRONTEND Interfaces compliant device for the Midwest Microwave MSDD-3000/6000 receiver.

## Installation

To build from source, run the `build.sh` script.
To install to `$SDRROOT`, run `build.sh install`. Note: root
privileges (`sudo`) may be required to install.

## Troubleshooting

The `msdd_configuration` property is used to setup the basic network connectivity between the MSDD hardware and the REDHAWK device. In this struct property, the IP and port of the MSDD must be setup in order the software to connect.

## FEI Compliance Test Results

See the [FEI Compliance Results](tests/FEI_Compliance_Results.md) document.
