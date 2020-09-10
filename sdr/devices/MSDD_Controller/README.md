 # REDHAWK `rh.MSDD_Controller`


 ## Table of Contents

* [Description](#description)
* [Properties](#properties)
* [Installation](#installation)
* [Troubleshooting](#troubleshooting)

## Description

The REDHAWK `rh.MSDD_Controller` is the AggregateDevice parent for each child receiver channel (`CompositeMSDD`) of the source MSDD radio. The `CompositeMSDD` is a specialized version of the the `rh.MSDD` device. The `MSDD_Controller` implements the life-cycle methods that will be used in FEI 3.0 to define parent/child relationships. A proper FEI 3.0 design would have one `MSDD_Controller` (parent) and N `WBDDC modules` (child/parent) for M `NBDDC modules` (child).  The design choice to reuse the existing `rh.MSDD` device to manage 1 WBDDC Module and M NBDDC modules, was based on reduce maintenance and missing FEI 3.0 API methods to manage allocations. To assist with the deployment process, the `rh.MSDD_Contrtoller` has links to the `rh.MSDD` source directories for proper installation and access the `rh.MSDD's` software profile during deployment.

The `MSDD_Controller` established a connection to the source MSDD radio defined by the `msdd` property. The `MSDD_Controller` performs an initial configuration of the radio and tuner discovery (RCV,WBDDC,NBDDC) using the `MSDDRadio` object. This `MSDDRadio` object will be shared by all child devices (`CompositeMSDD`) to synchronize access to the radio.

The `MSDD_Controller` manages the life-cycle and initialization of each child device (`CompositeMSDD`). The following sequence is performed during deployment and initialization of each child device:
1. Create required exec parameters, launch a new thread of execution and start the new child device in this thread.
1. Assign the MSDDRadio object, receiver identifier, shared properties, and context properties to the child device.
1. Call postConstructor method on the child device.

For each `RCV module (RCV)` on the source MSDD radio, the `MSDD_Controller` will assign the RCV:X identifier using the `CompositeMSDD::setSignalFlow` method. In the `CompositeMSDD::postConstructor` method, the `CompositeMSDD` will filter `WBDDC` and `NBDDCS` modules that are linked to the specified receiver identifier.

To establish the initial `CompositeMSDD` device configuration, `rh.MSDD_Controller's` property set contains both shared properties (`psd_configuration`, `gain_configuration`, `advanced`) common to all child instances, and context properties (`tuner_output`, `block_tuner_output`, `block_psd_output`) sourced by the receiver identifier. After the child device is launched, the `MSDD_Controller` will assign all the shared and context properties to each child.  

After all `CompositeMSDD` devices are launched and configured, the `MSDD_Controller` is only responsible for monitoring the time of day settings and periodically updating it's `msdd_status` property.  The `CompositeMSDD` device will manage it's own allocations, as well as psd and gain configuration changes.

The `rh.MSDD_Controller` will communicate to the MSDD radio using the `msdd` property structure at device start up. The following properties are used during start up to configure the MSDD radio.

- `msdd` : Communications context and Board module's (BRD) reference clock
- `time_of_day` : Time of Day (TOD) module configuration


## Properties

| *NAME* | *TYPE* | *ACCESS* | *DESCRIPTION* |
| :--------| :--------| :-----------| :---------------|
| device_kind  | string | readonly | kind of device, MSDD_Controller |
| device_model | string | readonly | Information from the console identity command. (MSDD CON IDN?) |
| [msdd](#msdd) | structure | readwrite | MSDD connection and board setup information |
| [time_of_day](#time_of_day) |structure | readwrite | MSDD Time of Day (TOD) module configuration |
| [advanced](#advanced) | structure | readwrite | Advanced controls to configure MSDD tuner types, and threshold states to control device usage state |
| [psd_configuration](#psd_configuration) | structure | readwrite | FFT module (FFT) parameters used to configure a FFT channel for controlling allocations. |
| [gain_configuration](#gain_configuration) | structure | readwrite | Gain settings (GAI) for each MSDD tuner type (RCV,WBDDC,NBDDC)|
| [msdd_status](#msdd_status) | structure | readonly | MSDD status information |
| [tuner_output](#tuner_output) | structureseq | readonly | Output stream configuration from MSDD digital tuners. |
| [ block_tuner_output](#block_tuner_output) | structureseq | readonly | Output stream configuration for blocks of MSDD digital tuners |
| [block_psd_output](#block_psd_output) | structureseq | readonly | Output stream configuration for blocks FFT channels assigned to tuners. |


### msdd
The *msdd* structure defines the connection, reference clock, and receiver_identifier properties for the MSDD device.

| *NAME* | *TYPE* |  *DESCRIPTION* |
| :--------| :---------| :---------------|
| msdd::ip_address | string | IP address of the MSDD in dot notation (MSDD CON IPP). Default is 127.0.0.1 |
| msdd::port | string | Port number the MSDD listens on, default value is 23, (MSDD CON IPP)|
| msdd::time_out | double | Time out to wait if MSDD does not respond to a command request, default value is 0.2. |
| msdd::clock_ref | short | Configures the external reference. INTERNAL reference clock (0), EXTERNAL 10Mhz reference signal (10) (MSDD BRD EXR) |
| msdd::receiver_id | string | Filter WBDDC and NBDDC channels from this receiver source (RCV:1)  |

### time_of_day
The *time_of_day* structure controls the configuration of the MSDD Time of Day module (TOD).  The state of the Time of Day module is reported in the `msdd_status` structure.

| *NAME* | *TYPE* |  *DESCRIPTION* |
| :--------| :---------| :---------------|
| time_of_day::mode | string | Time source (TOD MOD): SIM - simulated, ONE PPS - one pulse per second, IRIG - IRIGB timing source, NAV - Navigation timing source (requires external module), TFN - Time frequency navigation timing source (requires external module). Default is SIM.|
| time_of_day::reference_adjust| short | Adjust internal reference adjustment in ns/sec drift (TOD RFA). Default is 0.|
| time_of_day::reference_track| short |  Tracking mode for the reference signal (TOD RTK): OFF (0), CALIBRATE (1), TRACK (2). Default is 0.|
| time_of_day::toy| short | (TOD TOY), Time of year offset for IRIGB source, 24 hour offset (0),  Direct from signal (1). Default is 0. |

### advanced
The *advanced* structured property defines a mapping of MSDD tuner modules to FRONTEND tuner types, defines thresholds to control device busy state, enables/disables FFT channels, and controls the use of SWDDC channels for NBDDC modues to provide additional decimation.

| *NAME* | *TYPE* |  *DESCRIPTION* |
| :--------| :---------| :---------------|
| advanced::rcvr_mode | short | Maps MSDD RCV module to FEI tuner types: Not allocatable (0), RX Only (8). Default is 0.|
| advanced::wb_ddc_mode | short | Maps MSDD WBDDC module to FEI tuner types: Not allocatable (0), RX_DIGITIZER_CHANNELIZER Only (2), RX_DIGITIZER Only (4), RX_DIGITIZER or RX_DIGITIZER_CHANNELIZER (6). Default is 6. |
| advanced::hw_ddc_mode | short | Maps MSDD NBDDC module  to FEI tuner types: Not allocatable (0), DDC only (1), RX_DIGITIZER Only (4), RX_DIGITIZER or DDC (5). Default is 5.|
| advanced::enable_inline_swddc | boolean | True : Assigns a SWDDC module to each NBDDC for additional decimation. Default is False.|
| advanced::enable_fft_channels | boolean | True: Link a FFT channel to each NBDDC tuner's output for PSD output. Only controlling allocations will be assigned a FFT channel, False : Disable FTT channels and linking. Default is False. |
| advanced::max_cpu_load | float | Maximum allowable cpu load on the MSDD receiver. A device's BUSY state is enabled if the load (MSDD CON CPL?) exceeds the threshold. Default is 95.0.|
| advanced::max_nic_percentage | float | Maximum network utilization (as a percentage 0.0 to 100.0) that enables a device BUSY state. The MSDD NET BRT command defines the bit rate for a network interface. The total bit output rate is calculated for each enabled output module (i.e. tuner output). If this value exceeds the stated network utilization limit, a BUSY state is enabled. During all tuner allocations, this limit is checked to ensure the limit is not exceeded. Default is 90.0. |
| advanced::minimum_connected_nic_rate | float | Validates the MSDD radio's network interface supports the specified minimum data rate. Controls if the `rh.MSDD` should connect to the MSDD radio. Default is 1000 Mbps.|

### psd_configuration
The *psd_configuration* structure defines the FFT module settings.  These settings are applied to the FFT module when controlling allocation is requested and `advanced.enable_fft_channels` is True.

| *NAME* | *TYPE* |  *DESCRIPTION* |
| :--------| :---------| :---------------|
| psd_configuration::fft_size | double | Number of FFT bins (FFT PNT)|
| psd_configuration::time_average | double | Number of averages based on time in seconds. (FFT AVG) |
| psd_configuration::time_between_ffts | double | Time between FFT operations on a tuner channel. (FFT RAT) |
| psd_configuration::output_bin_size | double | The number of FFT bins to output (FFT BIN) |
| psd_configuration::window_type | enum | Windows to apply to each FFT. HANNING, HAMMING, BLACKMAN, RECT (FFT WND)|
| psd_configuration::peak_mode |  enum | Peak mode setting: INST, PEAK, BOTH (FFT PMD)|

### gain_configuration
The *gain_configuration* structure controls the gain setting for each MSDD tuner module type.

| *NAME* | *TYPE* |  *DESCRIPTION* |
| :--------| :---------| :---------------|
| gain_configuration::rcvr_gain | float | Gain setting in db for MSDD RCV modules |
| gain_configuration::wb_ddc_gain | float | Gain setting in db for MSDD WBDDC modules |
| gain_configuration::hw_ddc_gain | float | Gain setting in db for MSDD NBDDC modules |
| gain_configuration::sw_ddc_gain | float | Gain setting in db for MSDD SWDDC modules |

### msdd_status
The *msdd_status* structure defines the set of readonly properties that define the state of the MSDD receiver. This structure is updated after initialization, allocation requests and deallocation requests.

| *NAME* | *TYPE* |  *DESCRIPTION* |
| :--------| :---------| :---------------|
| msdd_status::connected | boolean | True if connected to MSDD receiver |
| msdd_status::receiver_identifier| string | Receiver identifier for filtering MSDD tuner modules. |
| msdd_status::ip_address | string | IP Address assigned to radio |
| msdd_status::port | string | Port number assigned to radio. |
| msdd_status::control_host | string | IP Address from CON IPP? |
| msdd_status::control_host_port | string | Port number from CON IPP? |
| msdd_status::model | string | Model name from CON CFG? MODEL |
| msdd_status::serial | string |  Serical number from CON CFG? SERIAL |
| msdd_status::software_part_number | string | Part number from CON IDN? |
| msdd_status::rf_board_type | string | Value from CON CFG? RF_BRD_TYPE |
| msdd_status::fpga_type | string | Value from CON CFG? FPGA_TYPE |
| msdd_status::dsp_type | string | Value from CON CFG? DSP_BRD_TYPE |
| msdd_status::minimum_frequency_hz | string | Value from CON CFG? MIN_FREQ |
| msdd_status::maximum_frequency_hz | string | Value from CON CFG? MAX_FREQ |
| msdd_status::dsp_reference_frequency_hz | string | Value CON CFG? DSP_REF |
| msdd_status::adc_clock_frequency_hz | string | Value from CON CFG? ADC_CLK |
| msdd_status::num_if_ports | string | Value from CON CFG? IF_PORTS |
| msdd_status::num_eth_ports | string | Value from CON CFG? ETHR_PORTS |
| msdd_status::cpu_type | string | Value from CON CFG? CPU_TYPE |
| msdd_status::cpu_rate | string | Value from CON CFG? CPU_FREQ  |
| msdd_status::cpu_load | string | Value from CON CPL? |
| msdd_status::pps_termination | string | Value from CON CFG? 1PPS_TERM |
| msdd_status::pps_voltage | string | Value from CON CFG? 1PPS_VOLTAGE |
| msdd_status::number_wb_ddc_channels | string | Value from CON CFG? FPGA_WBDDC_CHANNELS |
| msdd_status::number_nb_ddc_channels | string | Value from CON CFG? FPGA_NBDDC_CHANNELS |
| msdd_status::filename_app | string | Value from CON CFG? FILE_NAME_APP |
| msdd_status::filename_fpga | string | Value from CON CFG? FILE_NAME_FPGA |
| msdd_status::filename_batch | string |Value from CON CFG? FILE_NAME_BATCH|
| msdd_status::filename_boot | string | Value from CON CFG? FILE_NAME_BOOT |
| msdd_status::filename_loader  | string | Value from CON CFG? FILE_NAME_LOADER |
| msdd_status::filename_config  | string | Value from CON CFG? FILE_NAME_CONFIG |
| msdd_status::tod_module  | string | Value from TOD MOD? |
| msdd_status::tod_available_module | string | Value from TOD MODL? |
| msdd_status::tod_meter_list | string | Value from TOD MTR? |
| msdd_status::tod_reference_adjust | string | Value from MSDD TOD RFA? |
| msdd_status::tod_track_mode_state | string | Value from MSDD TOD RTK? |
| msdd_status::tod_bit_state | string | Value from MSDD TOD BIT? |
| msdd_status::tod_toy | string | Value from MSDD TOY? |
| msdd_status::tod_host_delta | double | Variance between host's time of day and MSDD clock |
| msdd_status::ntp_running | bool | If the host system is running NTP service for clock synchronization. |


### tuner_output
The *tuner_output* sequence contains the *tuner_output_definition* structure that defines the stream output configuration from a specific MSDD digital tuner module.  Output definitions are filtered on `receiver_identifier` and passed to the appropriate `CompositeMSDD` child device.

| *NAME* | *TYPE* |  *DESCRIPTION* |
| :--------| :---------| :---------------|
| tuner_output::receiver_identifier | string | MSDD RCV module linked to the digitial tuner. Default `RCV:1`.|
| tuner_output::tuner_number | short | Index of MSDD tuner in sequence of frontend_tuner_status structures (0 based)|
| tuner_output::enabled | boolean | Controls output module's enable state for the assigned tuner. (OUT ENB). Default is True. |
| tuner_output::protocol | string | Controls output packet format (OUT POL). UDP_SDDS : SDDS packet format, UDP_SDDSX - SDDSX packet format, UDP_SDDSA - SDDSA packet format, UDP_RAW - Raw packet format, UDP_VITA49 - VITA49 Data packet format. Default is UDP_SDDS.|
| tuner_output::interface | short | Network interface to publish data (0 based). Default 0.|
| tuner_output::ip_address | string | Destination IP address (OUT IPP) |
| tuner_output::port | short | Destination IP port (OUT IPP ). Default is 8800 |
| tuner_output::vlan_enable | boolean | Enable VLAN tagging (OUT VLANEN). Default is True. |
| tuner_output::vlan | short | VLAN ID for 802.1q packets (OUT VLANTCI). Default is -1.|
| tuner_output::timestamp_offset |  short | Offset applied in 1ns increments (OUT TSOFS). Default is 0. |
| tuner_output::endianess | short | BIG_ENDIAN (0). LITTLE ENDIAN (1) (OUT END). Default is LITTLE_ENDIAN. |
| tuner_output::mfp_flush | long | Packet flush control bit mask (OUT MFP). Default is all bits enabled. |

### block_tuner_output
The *block_tuner_output* sequence contains the *block_tuner_output_definition* structure that defines the stream output configuration for a block of MSDD digital tuner modules. Output definitions are filtered on `receiver_identifier` and passed to the appropriate `CompositeMSDD` child device.

| *NAME* | *TYPE* |  *DESCRIPTION* |
| :--------| :---------| :---------------|
| block_tuner_output::receiver_identifier | string | MSDD RCV module linked to the digitial tuner. Default `RCV:1`.|
| block_tuner_output::tuner_number_start | short | Starting tuner number for block |
| block_tuner_output::tuner_number_stop | short | Ending tuner number of block |
| block_tuner_output::enabled | boolean | Controls output module's enable state for the assigned tuner. (OUT ENB). Default is True. |
| block_tuner_output::protocol | string | Controls output packet format (OUT POL). UDP_SDDS : SDDS packet format, UDP_SDDSX - SDDSX packet format, UDP_SDDSA - SDDSA packet format, UDP_RAW - Raw packet format, UDP_VITA49 - VITA49 Data packet format. Default is UDP_SDDS.|
| block_tuner_output::interface | short | Network interface to publish data (0 based). Default 0.|
| block_tuner_output::ip_address | string | Destination IP address (OUT IPP) |
| block_tuner_output::port | short | Destination IP port (OUT IPP ). Default is 8800 |
| block_tuner_output::vlan_enable | boolean | Enable VLAN tagging (OUT VLANEN). Default is True. |
| block_tuner_output::vlan | short | VLAN ID for 802.1q packets (OUT VLANTCI). Default is -1.|
| block_tuner_output::timestamp_offset |  short | Offset applied in 1ns increments (OUT TSOFS). Default is 0. |
| block_tuner_output::endianess | short | BIG_ENDIAN (0). LITTLE ENDIAN (1) (OUT END). Default is LITTLE_ENDIAN. |
| block_tuner_output::mfp_flush | long | Packet flush control bit mask (OUT MFP). Default is all bits enabled. |

### block_psd_output
The *block_psd_output* sequence contains the *block_psd_output_definition* that defines the output stream configuration for a block of MSDD FFT channels. Output definitions are filtered on `receiver_identifier` and passed to the appropriate `CompositeMSDD` child device.

| *NAME* | *TYPE* |  *DESCRIPTION* |
| :--------| :---------| :---------------|
| block_psd_output::receiver_identifier | string | MSDD RCV module linked to the FFT module. Default `RCV:1`.|
| block_psd_output::fft_channel_start | short | Starting FFT channel of block (0 based) |
| block_psd_output::fft_channel_stop | short | Ending FFT channel of block |
| block_psd_output::enabled | boolean | Controls output module's enable state for the assigned tuner. (OUT ENB). Default is True. |
| block_psd_output::protocol | string | Controls output packet format (OUT POL). UDP_SDDS : SDDS packet format, UDP_SDDSX - SDDSX packet format, UDP_SDDSA - SDDSA packet format, UDP_RAW - Raw packet format, UDP_VITA49 - VITA49 Data packet format. Default is UDP_SDDS.|
| block_psd_output::interface | short | Network interface to publish data (0 based). Default 0.|
| block_psd_output::ip_address | string | Destination IP address (OUT IPP) |
| block_psd_output::port | short | Destination IP port (OUT IPP ). Default is 8800 |
| block_psd_output::vlan_enable | boolean | Enable VLAN tagging (OUT VLANEN). Default is True. |
| block_psd_output::vlan | short | VLAN ID for 802.1q packets (OUT VLANTCI). Default is -1.|
| block_psd_output::timestamp_offset |  short | Offset applied in 1ns increments (OUT TSOFS). Default is 0. |
| block_psd_output::endianess | short | BIG_ENDIAN (0). LITTLE ENDIAN (1) (OUT END). Default is LITTLE_ENDIAN. |
| block_psd_output::mfp_flush | long | Packet flush control bit mask (OUT MFP). Default is all bits enabled. |

## Installation

The following procedure explains how to install `rh.MSDD_Controller` from source. For information about RPM installation, refer to the `REDHAWK Manual`.

1. Ensure `OSSIEHOME` and `SDRROOT` are both set.

```sh
$ source /etc/profile.d/redhawk.sh
$ source /etc/profile.d/redhawk-sdrroot.sh
```

2. To build and install `rh.MSDD_Controller`, enter the following commands from the root directory of the assets repository:

```
$ ./build.sh install
```

The installation of the `rh.MSDD_Contoller` will also install the `rh.MSDD` into the install location.  This is required to satisfy the source code linkage between the parent's python code and the child device.

## Troubleshooting

To resolve issues with core framework bases class the following lines are required in the `MSDD_Controller_base.py`. If you regen this device, added back the `from fix_cf_import *` line
```c++
# Source: MSDD_Controller.spd.xml
from ossie.cf import CF
from ossie.cf import CF__POA
from ossie.utils import uuid

# require to fix framework base class issues when setting properties
from fix_cf import *
```


The [msdd property](#msdd) is used to setup the network connectivity between the MSDD hardware and host computer running the `rh.MSDD_Controller` device.  Set the `msdd::ip_address` and `msdd::port` properties for the MSDD receiver you are connecting.
