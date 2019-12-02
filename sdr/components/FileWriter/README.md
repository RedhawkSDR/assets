# REDHAWK rh.FileWriter

## Description

Contains the source and build script for the REDHAWK
`rh.FileWriter`. The `rh.FileWriter` component receives streaming data from the
BulkIO provides (input) ports and writes the data to a file on the SCA or local
file system.

## Installation

This asset requires the `rh.blueFileLib` and `rh.RedhawkDevUtils` shared libraries.
These shared libraries must be installed in order to build and run this asset.
To build from source, run the `build.sh` script. To install to `$SDRROOT`, run `build.sh install`.
Note: root privileges (`sudo`) may be required to install.

## Usage

To use `rh.FileWriter`, connect an input port to the desired input data stream
that matches the input data type. Configure the `destination_uri` property with
the path to the file to be written and optionally the `destination_uri_suffix`
if a suffix is desired. Select the desired format for the output file using the
`file_format` property. Ensure the `recording_enabled` property is set to true,
which is the default value.

There are several key words that can be used to insert stream- or data-specific
information into the file name. Any SRI Keyword name with percent symbols (%)
on either side will be replaced with the value of the SRI Keyword. Other
pre-defined key words for string replacement are listed below:

| Key word string             | Description                                   |
| --------------------------- | --------------------------------------------- |
| %STREAMID%                  | Stream ID                                     |
| %TIMESTAMP%                 | Timestamp of the first sample of the file     |
| %TIMESTAMP_NO_FRACT%        | Timestamp without fractional component        |
| %SYSTEM_TIMESTAMP%          | System time                                   |
| %SYSTEM_TIMESTAMP_NO_FRACT% | System time without fractional component      |
| %COMP_NS_NAME%              | Naming service name                           |
| %EXTENSION%                 | "bluefile" or "raw" based on file_format prop |
| %MODE%                      | "real" for Scalar or "cplx" for Complex       |
| %SR%                        | The sample rate in Hz                         |
| %DT%                        | The data type and format (described below)    |
| %COL_RF%                    | COL_RF SRI Keyword                            |
| %CHAN_RF%                   | CHAN_RF SRI Keyword                           |
| %COLRF_HZ%                  | Integer COL_RF keyword followed by "Hz"       |
| %CHANRF_HZ%                 | Integer CHAN_RF keyword followed by "Hz"      |
| %CF_HZ%                     | Integer CHAN_RF (or COL_RF) followed by "Hz"  |

By default, the data is written to the file without byte swapping and the byte
order is assumed to be host byte order. Use the `input_bulkio_byte_order` and
`swap_bytes` properties to alter this behavior as desired. The byte order of the
host is provided by the `host_byte_order` property.

The data format tag (i.e. %DT%) is based on the BulkIO input port and the byte
order of the data being written. The byte order is determined using the
`input_bulkio_byte_order` and `swap_bytes` properties.

| Data Format Tag | Input Data/Port Type | Byte Order Written to File |
| --------------- | -------------------- | -------------------------- |
| 8t              | Char or XML input    | Big or Little Endian       |
| 8o              | Octet input          | Big or Little Endian       |
| 16t             | Short input          | Big Endian                 |
| 16tr            | Short input          | Little Endian              |
| 16o             | Unsigned Short input | Big Endian                 |
| 16or            | Unsigned Short input | Little Endian              |
| 32f             | 32-bit Float input   | Big Endian                 |
| 32fr            | 32-bit Float input   | Little Endian              |
| 64f             | 64-bit Double input  | Big Endian                 |
| 64fr            | 64-bit Double input  | Little Endian              |

The `advanced_properties` struct property and the `recording_timer` struct
sequence property are available to support more complicated use cases. Each of
the features available are documented in the description of each property when
viewing the Properties Descriptor XML file (i.e. FileWriter.prf.xml).
