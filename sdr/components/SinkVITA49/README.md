# REDHAWK rh.SinkVITA49
 
## Description

Contains the source and build script for the REDHAWK
`rh.SinkVITA49`. The `rh.SinkVITA49` component creates a UDP/multicast or TCP VITA49
packet stream and converts the data and SRI Keywords to IF data packets and
Context packets for use within/between/outside of a REDHAWK domain application.
 
## Installation

This asset requires the `rh.VITA49` shared library. This shared library  must be
installed in order to build and run this asset. To build from source, run the
`build.sh` script. To install to `$SDRROOT`, run
`build.sh install`. Note: root privileges (`sudo`) may be required to install.
 