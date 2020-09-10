 # REDHAWK `rh.MSDD`


 ## Table of Contents

* [Description](#description)
* [Properties](#properties)
* [Installation](#installation)
* [Troubleshooting](#troubleshooting)

## Description

The REDHAWK `rh.MSDD` device is a FRONTEND 2.0 Interfaces (FEI) compliant device for the Midwest Microwave MSDD-3000/6000/0660 series receiver. The `rh.MSDD` device is written in the Python programming language and communicates with receiver using the host's network interface.  Setting the `rh.MSDD` properties described below will command and control of the MSDD radio. The command formats and descriptions are documented in the MSDD API document from Midwest Microwave Systems.  

The `rh.MSDD` device is meant to be configured a single RF flow. This is controlled using the 'msdd::receiver_id' property at device startup.  This property will be referenced to filter out the MSDD tuner modules from the specified RCV module instance.  For single channel MSDD radios this value will be the default to `RCV:1`. All WBDDC and NBDDC tuner modules from this RCV module instance will configured as Frontend tuner devices.  For dual Channel MSDD radios, you can use the `rh.MSDD` device to process a single RF flow (i.e. 'RCV:1') on the radio.  If you want to process both RCV modules on a dual channel radio you will need to use the `rh.MSDD_Controller` device.

The `rh.MSDD` will communicate to the MSDD radio using the `msdd` property structure at device start up. The following properties are used during start up to configure the MSDD radio.

- `msdd` : Communications context and Board module's (BRD) reference clock
- `time_of_day` : Time of Day (TOD) module configuration
- `block_tuner_output`, `tuner_output` : Configure OUTPUT Modules (OUT) assigned to WBDDC and NBDDC hardware tuners
- `block_psd_output` : Configure OUTPUT Modules (OUT) assigned to FFT modules (FFT) of controlling allocations.

Once the `rh.MSDD` device has completed initialization, the following run time properties will affect tuner allocations, tuner gain and PSD calculations.

- `frontend_tuner_allocation` : FEI allocation request structure
- `frontend_listener_allocation` : FEI listener allocation request structure
- `advanced` : Controls how hardware tuners respond to FEI allocation requests.  Sets threshold values that are monitored to determine if the MSDD radio is in BUSY state. Enables FFT channels for controlling allocations.
- `gain_configuration` : Defined gain settings that can be assigned during an tuner allocations for RCV, WBDDC, and MBDDC modules.
- `psd_configuration` : FFT module settings applied to a controlling allocation when FFT channels are enabled (`advanced.enable_fft_channels`)

The following run time properties describe the state of the MSDD radio and the frontend tuner devices associated with the MSDD tuner module hardware.

- `frontend_tuner_status` : FEI tuner status structure (readonly) describes the current state of each tuner being managed by `rh.MSDD`
- `connectionTable` : Maps connection identifiers to stream identifiers, use to filter out messages to specific connections
- `msdd_status` : Provides the current state of the MSDD radio (readonly)

## Properties

| *NAME* | *TYPE* | *ACCESS* | *DESCRIPTION* |
| :--------| :--------| :-----------| :---------------|
| device_kind  | string | readonly | kind of device, FRONTEND::TUNER for FEI 2.0 |
| device_model | string | readonly | Information from the console identity command. (MSDD CON IDN?) |
| frontend_group_id | string | readwrite | Group id for FEI group allocation requests |
| [msdd](#msdd) | structure | readwrite | MSDD connection and board setup information |
| [time_of_day](#time_of_day) |structure | readwrite | MSDD Time of Day (TOD) module configuration |
| [advanced](#advanced) | structure | readwrite | Advanced controls to configure MSDD tuner types, and threshold states to control device usage state |
| [tuner_output](#tuner_output) | structureseq | readonly | Output stream configuration from MSDD digital tuners. |
| [ block_tuner_output](#block_tuner_output) | structureseq | readonly | Output stream configuration for blocks of MSDD digital tuners |
| [block_psd_output](#block_psd_output) | structureseq | readonly | Output stream configuration for blocks FFT channels assigned to tuners. |
| [psd_configuration](#psd_configuration) | structure | readwrite | FFT module (FFT) parameters used to configure a FFT channel for controlling allocations. |
| [gain_configuration](#gain_configuration) | structure | readwrite | Gain settings (GAI) for each MSDD tuner type (RCV,WBDDC,NBDDC)|
| [msdd_status](#msdd_status) | structure | readonly | MSDD status information |
| [FRONTEND::tuner_status](#frontend_tuner_status) |  structureseq | readonly | Frontend tuner status information for each MSDD tuner. |
| [FRONTEND::listener_allocation](#frontend_listener_allocation) |  structure | writeonly | FRONTEND tuner listener allocation structure, consult REDHAWK manual for details|
| [FRONTEND::tuner_allocation](#frontend_tuner_allocation) |  structure | writeonly | FRONTEND tuner allocation structure, consult REDHAWK manual for details |

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
| time_of_day::tracking_interval| double | Time between host and radio time status checks. Default is 0 (turn off check) |

### advanced
The *advanced* structured property defines a mapping of MSDD tuner modules to FRONTEND tuner types, defines thresholds to control device busy state, enables/disables FFT channels, and controls the use of SWDDC channels for NBDDC modues to provide additional decimation.

| *NAME* | *TYPE* |  *DESCRIPTION* |
| :--------| :---------| :---------------|
| advanced::rcvr_mode | short | Maps MSDD RCV module to FEI tuner types: Not allocatable (0), RX Only (8). Default is 0.|
| advanced::wb_ddc_mode | short | Maps MSDD WBDDC module to FEI tuner types: Not allocatable (0), RX_DIGITIZER_CHANNELIZER Only (2), RX_DIGITIZER Only (4), RX_DIGITIZER or RX_DIGITIZER_CHANNELIZER (6). Default is 6. |
| advanced::hw_ddc_mode | short | Maps MSDD NBDDC module  to FEI tuner types: Not allocatable (0), DDC only (1), RX_DIGITIZER Only (4), RX_DIGITIZER or DDC (5). Default is 5.|
| advanced::enable_fft_channels | boolean | True: Link a FFT channel to each NBDDC tuner's output for PSD output. Only controlling allocations will be assigned a FFT channel, False : Disable FTT channels and linking. Default is False. |
| advanced::max_cpu_load | float | Maximum allowable cpu load on the MSDD receiver. A device's BUSY state is enabled if the load (MSDD CON CPL?) exceeds the threshold. Default is 95.0.|
| advanced::max_nic_percentage | float | Maximum network utilization (as a percentage 0.0 to 100.0) that enables a device BUSY state. The MSDD NET BRT command defines the bit rate for a network interface. The total bit output rate is calculated for each enabled output module (i.e. tuner output). If this value exceeds the stated network utilization limit, a BUSY state is enabled. During all tuner allocations, this limit is checked to ensure the limit is not exceeded. Default is 90.0. |
| advanced::minimum_connected_nic_rate | float | Validates the MSDD radio's network interface supports the specified minimum data rate. Controls if the `rh.MSDD` should connect to the MSDD radio. Default is 1000 Mbps.|


### tuner_output
The *tuner_output* sequence contains the *tuner_output_definition* structure that defines the stream output configuration from a specific MSDD digital tuner.  If *tuner_output* and *block_tuner_output* values are defined, the *tuner_output* properties override the *block_tuner_output* information for a specific tuner.

| *NAME* | *TYPE* |  *DESCRIPTION* |
| :--------| :---------| :---------------|
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
The *block_tuner_output* sequence contains the *block_tuner_output_definition* structure that defines the stream output configuration for a block of MSDD tuners. See *tuner_output* property for precedence of  stream output configuration settings.

| *NAME* | *TYPE* |  *DESCRIPTION* |
| :--------| :---------| :---------------|
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
The *block_psd_output* sequence contains the *block_psd_output_definition* that defines the output stream configuration for a block of MSDD FFT channels.

| *NAME* | *TYPE* |  *DESCRIPTION* |
| :--------| :---------| :---------------|
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
| msdd_status::ip_address | string | IP Address assigned to radio  |
| msdd_status::port | string | Port number assigned to radio |
| msdd_status::model | string | Model name from CON CFG? MODEL |
| msdd_status::control_host | string | IP Address number from CON IPP? |
| msdd_status::control_host_port | string | Port number from CON IPP? |
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


### FRONTEND::tuner_status
The *FRONTEND::tuner_status* structure defines the `FRONTEND` tuner status properties for each defined tuner.  The standard tuner status properties are defined in the REDHAWK documentation.  This table describes the additional properties defined for each MSDD tuner.

| *NAME* | *TYPE* |  *DESCRIPTION* |
| :--------| :---------| :---------------|
| FRONTEND::tuner_status::output_format | string | SDDS data format, fixed to `CI` (16bit IQ)|
| FRONTEND::tuner_status::output_multicast | string | Destination IP address of data packets, (MSDD OUT IPP?) |
| FRONTEND::tuner_status::output_port | long | Destination port of data packets, (OUT IPP?) |
| FRONTEND::tuner_status::output_vlan | long | VLAN number if VLAN is enabled, (OUT VLANTCI?)|
| FRONTEND::tuner_status::msdd_channel_type | string | RX, WBDDC, NBDDC, SWDDC |
| FRONTEND::tuner_status::msdd_installation_name_csv | string | List of MSDD modules assigned to this tuner |
| FRONTEND::tuner_status::msdd_registration_name_csv | string | List of MSDD registration names assigned to this tuner |
| FRONTEND::tuner_status::bits_per_sample | short | Number of bits per data sample. |
| FRONTNED::tuner_status::adc_meter_value | string | Meter value from MSDD RCV AMD? |
| FRONTEND::tuner_status::rcvr_gain | float | Value from RCV GAI? |
| FRONTEND::tuner_status::ddc_gain | float | Value from WBDDC/NBDDC GAI? |
| FRONTEND::tuner_status::allocated | boolean | True if tuner is allocated, False otherwise |
| FRONTEND::tuner_status::input_sample_rate | double | Value from OUT ISR? |
| FRONTEND::tuner_status::output_channel | string | MSDD output module assigned to this tuner |
| FRONTEND::tuner_status::output_enabled | boolean | True if output is enabled, False otherwise |
| FRONTEND::tuner_status::output_vlan_enabled | boolean | True if vlan tagging is enabled, OUT VLANEN? |
| FRONTEND::tuner_status::output_vlane_tci | string | VLAN number if tagging is enabled, OUT VLANTCI? |
| FRONTEND::tuner_status::output_flow | string | MSDD module chain feeding this output module |
| FRONTEND::tuner_status::output_timestamp_offset | string | Value from OUT TSOFS?)
| FRONTEND::tuner_status::endianess | short | Value from OUT END? |
| FRONTEND::tuner_status::output_mfp_flush | long | Value from OUT MFP? |
| FRONTEND::tuner_status::psd_fft_size | double | Value from FFT PNT? |
| FRONTEND::tuner_status::psd_averages | double | Value from FFT AVG? |
| FRONTEND::tuner_status::psd_time_between_ffts | double | Value from FFT RAT? |
| FRONTEND::tuner_status::psd_output_bin_size | double | Value from FFT BIN? |
| FRONTEND::tuner_status::psd_window_type | string | Value from FFT WND? |
| FRONTEND::tuner_status::psd_peak_mode | string | Value from FFT PMD?


## Installation

The following procedure explains how to install `rh.MSDD` from source. `rh.MSDD` is one of the `REDHAWK Basic Devices`. For information about how to install the `REDHAWK Basic Devices` from RPMs, refer to the `REDHAWK Manual`.

1. Ensure `OSSIEHOME` and `SDRROOT` are both set.

```sh
$ source /etc/profile.d/redhawk.sh
$ source /etc/profile.d/redhawk-sdrroot.sh
```

2. To build and install `rh.MSDD`, enter the following commands from the root directory of the assets repository:

```
$ cd $(find . -name MSDD)
$ ./build.sh install
```

## Troubleshooting

The [msdd property](#msdd) is used to setup the network connectivity between the MSDD hardware and host computer running the REDHAWK rh.MSDD device.  Set the `msdd::ip_address` and `msdd::port` properties for the MSDD receiver you are connecting.
