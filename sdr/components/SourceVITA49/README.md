# REDHAWK rh.SourceVITA49
 
## Description

Contains the source and build script for the REDHAWK
`rh.SourceVITA49`. The `rh.SourceVITA49` component connects to a UDP/multicast or
TCP VITA49 packet stream and converts the headers to SRI Keywords and data to
the BULKIO interface of the user's choice for use within REDHAWK domain
applications.
 
## Installation

This asset requires the `rh.VITA49` shared library. This shared library  must be
installed in order to build and run this asset. To build from source, run the
`build.sh` script. To install to `$SDRROOT`, run
`build.sh install`. Note: root privileges (`sudo`) may be required to install.
 