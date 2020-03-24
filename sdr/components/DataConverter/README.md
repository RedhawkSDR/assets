# REDHAWK rh.DataConverter

## Description

The `rh.DataConverter` component is used to convert between BulkIO data types in
REDHAWK. With proper configuration, the `rh.DataConverter` component can convert
between any of the following data types; `Char`, `Octet`, `Unsigned Short`, `Short`,
`Float` and `Double`. The `rh.DataConverter` component is also capable of converting
real data into complex data, and similarly complex data to real data.

## Installation

This asset requires the `rh.fftlib` shared library. This shared library must be
installed in order to build and run this asset. To build from source, run the
`build.sh` script. To install to `$SDRROOT`, run
`build.sh install`. Note: root privileges (`sudo`) may be required to install.

## Usage

To use the `rh.DataConverter` component, connect an input port to the desired input data stream that matches the input data type. Connect the output to the output port of the desired output type. Only one input port can be active at a time, but the data can be converted to multiple output types at the same time. If scaling is desired, fixed point output scaling can be enabled by the `scaleOutput` boolean. For floating point scaling or normalization, use the `floatingPointRange` and `normalize_floating_point` properties. The `rh.DataConverter` can also do real to complex and complex to real transformations. The output type (real/complex/passthrough) is controlled with the `outputType` property. If a transform is required, the properties controlling that transform are specified in the `transformProperties` property. 
