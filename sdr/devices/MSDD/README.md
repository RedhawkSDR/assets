# REDHAWK Basic Devices rh.MSDD
 
## Description

Contains the source and build script for the REDHAWK Basic Devices rh.MSDD
device. This device is a FRONTEND Interfaces compliant device for the Midwest Microwave MSDD-3000/6000 receiver.

## Branches and Tags

All REDHAWK core assets use the same branching and tagging policy. Upon release,
the `master` branch is rebased with the specific commit released, and that
commit is tagged with the asset version number. For example, the commit released
as version 1.0.0 is tagged with `1.0.0`.

Development branches (i.e. `develop` or `develop-X.X`, where *X.X* is an asset
version number) contain the latest unreleased development code for the specified
version. If no version is specified (i.e. `develop`), the branch contains the
latest unreleased development code for the latest released version.

## REDHAWK Version Compatibility

| Asset Version | Minimum REDHAWK Version Required |
| ------------- | -------------------------------- |
| 3.x           | 2.1.1                              |


## Installation Instructions

To build from source, run the `build.sh` script found at the
top level directory. To install to $SDRROOT, run `build.sh install`. Note: root
privileges (`sudo`) may be required to install.

## Troubleshooting

The msdd_configuration property is used to setup the basic network conductivity between the MSDD hardware and the REDHAWK device. In this struct property, the IP and port of the MSDD must be setup in order the software to connect.

## FEI Compliance Test Results

See the [FEI Compliance Results](tests/FEI_Compliance_Results.md) document.

## Copyrights

This work is protected by Copyright. Please refer to the
[Copyright File](COPYRIGHT) for updated copyright information.

## License

REDHAWK Basic Devices rh.MSDD is licensed under the GNU Lesser General
Public License (LGPL).

