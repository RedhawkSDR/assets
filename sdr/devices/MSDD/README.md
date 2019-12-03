 # REDHAWK `rh.MSDD`


 ## Table of Contents

* [Description](#description)
* [Properties](#properties)
* [Installation](#installation)
* [Troubleshooting](#troubleshooting)
* [FEI Compliance Test Results](#fei-compliance-test-results)

## Description

The REDHAWK `rh.MSDD` device is a FRONTEND 2.0 Interfaces (FEI) compliant device for the Midwest Microwave MSDD-3000/6000 series receiver. The device is written in the Python programming language and communicates with receiver using the host's network interface.  The descriptions below make reference to MSDD commands; consult the MSDD API documentation for specific commands and their definitions.

## Properties

| *NAME* | *TYPE* | *ACCESS* | *DESCRIPTION* |
| :--------| :--------| :-----------| :---------------|
| device_kind  | string | readonly | kind of device, FRONTEND::TUNER for FEI 2.0 |
| device_model | string | readonly | Derived from the MSDD CON IDN? command |
| clock_ref | short | readwrite | Configures the external reference. INTERNAL reference clock (0), EXTERNAL 10Mhz reference signal (10) (MSDD BRD EXR) |
| [advanced](#advanced) | structure | readwrite | Maps MSDD tuner types to FEI tuner types. Defines threshold states to control device usage state |
| [msdd_configuration](*msdd_configuration*) | structure | readwrite | MSDD connection information |
| [msdd_time_of_day_configuration](#msdd_time_of_day_configuration) |structure | readwrite | MSDD Time of Day module configuration |
| [msdd_output_configuration](#msdd_output_configuration) | structure | readonly | Network output configuration for specific MSDD tuners |
| [msdd_block_output_configuration](#msdd_block_output_configuration) | structure | readonly | Network output configuration for blocks of MSDD tuners |
| [msdd_psd_output_configuration](#msdd_psd_output_configuration) | structure | readonly | Network output configuration for FFT channels |
| [msdd_status](#msdd_status) | structure | readonly | MSDD status information |
| [msdd_advanced_debugging_tools](#msdd_advanced_debugging_tools) | structure | readwrite | Send a command to the MSDD |
| [msdd_gain_configuration](#msdd_gain_configuration) | structure | readwrite | Gain settings for each MSDD tuner type |
| [FRONTEND::tuner_status](#frontend_tuner_status) |  structure | readonly | Tuner status information for each MSDD tuner. Listing of additional properties to the FRONTEND tuner status structure |
| [FRONTEND::listener_allocation](#frontend_listener_allocation) |  structure | writeonly | FRONTEND tuner listener allocation structure, consult REDHAWK manual for details|
| [FRONTEND::tuner_allocation](#frontend_tuner_allocation) |  structure | writeonly | FRONTEND tuner allocation structure, consult REDHAWK manual for details |


### advanced
The *advanced* structured property maps MSDD tuner types to FRONTEND tuner types, enables/disables FFT and MSDD SWDDC channels, and defines thresholds that control the device's usage state.

| *NAME* | *TYPE* |  *DESCRIPTION* |
| :--------| :---------| :---------------|
| advanced::rcvr_mode | short | Maps MSDD RCV module tuners to FEI tuner type, Not allocatable (0), RX Only (8)|
| advanced::wb_ddc_mode | short | Maps MSDD WBDDC module tuners to FEI tuner type. Not allocatable (0), DDC only (1), RX_DIGITIZER_CHANNELIZER Only (2), RX_DIGITIZER_CHANNELIZER or DDC (3), RX_DIGITIZER Only (4), RX_DIGITIZER or DDC (5), RX_DIGITIZER or RX_DIGITIZER_CHANNELIZER (6), RX_DIGITIZER or RX_DIGITIZER_CHANNELIZER or DDC (7) |
| advanced::hw_ddc_mode | short | Maps MSDD NBDDC module tuners to FEI tuner type. Not allocatable (0), DDC only (1), RX_DIGITIZER_CHANNELIZER Only (2), RX_DIGITIZER_CHANNELIZER or DDC (3), RX_DIGITIZER Only (4), RX_DIGITIZER or DDC (5), RX_DIGITIZER or RX_DIGITIZER_CHANNELIZER (6), RX_DIGITIZER or RX_DIGITIZER_CHANNELIZER or DDC (7) |
| advanced::sw_ddc_mode | short | Maps MSDD SWDDC tuner type to FEI tuner type. Not allocatable (0), DDC only (1), RX_DIGITIZER_CHANNELIZER Only (2), RX_DIGITIZER_CHANNELIZER or DDC (3), RX_DIGITIZER Only (4), RX_DIGITIZER or DDC (5), RX_DIGITIZER or RX_DIGITIZER_CHANNELIZER (6), RX_DIGITIZER or RX_DIGITIZER_CHANNELIZER or DDC (7) |
| advanced::enable_inline_swddc | boolean | True : Assigns a SWDDC module to each NBDDC for additional decimation, False : SWDDC are not assigned to each NBDDC. |
| advanced::enable_secondary_tuners | boolean | True: Links remaining SWDDC modules to each NBDDC as piggyback tuners. False : Do not link SWDDC modules.|
| advanced::enable_fft_channels | boolean | True: Link a FFT channel to each NBDDC tuner's output for PSD output. Only controlling allocations will be assigned a FFT channel, False : Disable FTT channels and linking. |
| advanced::max_cpu_load | float | Maximum cpu load on the MSDD receiver that enables a device BUSY state. If the value of MSDD CON CPL? command exceeds this property, a device BUSY state is enabled.|
| advanced::max_nic_percentage | float | Maximum network utilization (as a percentage 0.0 to 100.0) that enables a device BUSY state. The MSDD NET BRT command defines the bit rate for a network interface. The total bit output rate is calculated for each enabled output module (i.e. tuner output). If this value exceeds the stated network utilization limit, the device enables a BUSY state. During all tuner allocations, this limit is checked to ensure the limit is not exceeded. If exceeded a failed allocation will occur. |
| advanced::minimum_connected_nic_rate | float | If the connected host's network interface supports a lower rate than the MSDD's network interface |

### msdd_configuration
The *msdd_configuration* structure defines the connection properties for the MSDD device.

| *NAME* | *TYPE* |  *DESCRIPTION* |
| :--------| :---------| :---------------|
| msdd_configuration::msdd_ip_addres | string | IP address of the MSDD in dot notation (MSDD CON IPP) |
| msdd_configuration::msdd_port | string | Port number the MSDD listens on, default value is 23, (MSDD CON IPP)|

### msdd_time_of_day_configuration
The *msdd_time_of_day_configuration* structure controls how the MSDD Time of Day module is configured.

| *NAME* | *TYPE* |  *DESCRIPTION* |
| :--------| :---------| :---------------|
| msdd_time_of_day::mode | string | MSDD TOD MOD, SIM : simulated, ONE PPS : one pulse per second, IRIG : IRIG timing source, NAV - Navigation timing source (requires external module), TFN - Time frequency navigation timing source (requires external module)|
| msdd_time_of_day::reference_adjust| short | MSDD TOD RFA, Adjust internal reference adjustment in ns/sec drift |
| msdd_time_of_day::reference_track| short | MSDD TOD RTK, Controls the tracking mode for the reference signal. OFF (0), CALIBRATE (1), TRACK (2)|
| msdd_time_of_day::toy| short | MSDD TOD TOY, Time of year offset for IRIGB source, 24 hour offset (0),  Direct from signal (1) |

### msdd_output_configuration
The *msdd_output_configuration* structure defines the network output configuration for a specific MSDD tuner.  If *msdd_output_configuration* and *msdd_bock_configuration* values are defined, the *msdd_output_configuration* overrides and *msdd_block_output_configuration* information for a specific tuner.

| *NAME* | *TYPE* |  *DESCRIPTION* |
| :--------| :---------| :---------------|
| tuner_number | short | Index of MSDD tuner in sequence of frontend_tuner_status structures (0 based)|
| enabled | boolean | Controls if tuner output is enabled (MSDD OUT ENB)  |
| protocol | string | Controls output packet format (MSDD OUT POL). UDP_SDDS : SDDS packet format, UDP_SDDSX - SDDSX packet format, UDP_SDDSA - SDDSA packet format, UDP_RAW - Raw packet format, UDP_VITA49 - VITA49 Data packet format |
| ip_address | string | Destination IP address (MSDD OUT IPP) |
| port | short | Destination IP port (MSDD OUT IPP ) |
| vlan_enable | boolean | Enable VLAN tagging (MSDD OUT VLANEN) |
| vlan_enable | short | VLAN ID for 802.1q packets (MSDD OUT VLANTCI)|
| timestamp_offset |  short | Offset applied in 1ns increments (MSDD OUT TSOFS) |
| endianess | short | BIG_ENDIAN (0). LITTLE ENDIAN (1) (MSDD OUT END) |
| mfp_flush | long | Packet flush control bit mask (MSDD OUT MFP) |

### msdd_block_output_configuration
The *msdd_block_output_configuration* structure defines the network output configuration for a block of MSDD tuners. If *msdd_output_configuration* and *msdd_bock_configuration* values are defined, the *msdd_output_configuration* overrides and *msdd_block_output_configuration* information for a specific tuner.

| *NAME* | *TYPE* |  *DESCRIPTION* |
| :--------| :---------| :---------------|
| tuner_number_start | short | Starting tuner number for block |
| tuner_number_stop | short | Ending tuner number of block |
| enabled | boolean | Controls if output is enabled for this tuner (MSDD OUT ENB)|
| protocol | string | Controls output packet format (MSDD OUT POL). UDP_SDDS : SDDS packet format, UDP_SDDSX - SDDSX packet format, UDP_SDDSA - SDDSA packet format, UDP_RAW - Raw packet format, UDP_VITA49 - VITA49 Data packet format |
| ip_address | string | Destination IP address (see MSDD OUT IPP) |
| port | short | Destination IP port (MSDD OUT IPP ) |
| vlan_enable | boolean | Enable VLAN tagging (MSDD OUT VLANEN) |
| vlan_enable | short | VLAN ID for 802.1q packets (MSDD OUT VLANTCI)|
| timestamp_offset |  short | Offset applied in 1ns increments (MSDD OUT TSOFS) |
| endianess | short | BIG ENDIAN (0). LITTLE ENDIAN (1) (MSDD OUT END) |
| mfp_flush | long | Packet flush control bit mask (MSDD OUT MFP) |

### msdd_psd_output_configuration
The *msdd_psd_output_configuration* structure defines the network output configuration for a block of MSDD tuners.

| *NAME* | *TYPE* |  *DESCRIPTION* |
| :--------| :---------| :---------------|
| fft_channel_start | short | Starting FFT channel of block (0 based) |
| fft_channel_stop | short | Ending FFT channel of block |
| enabled | boolean | Controls if FFT channel output is enabled for assigned tuner (MSDD OUT ENB) |
| protocol | string | Controls output packet format (MSDD OUT POL). UDP_SDDS : SDDS packet format, UDP_SDDSX - SDDSX packet format, UDP_SDDSA - SDDSA packet format, UDP_RAW - Raw packet format, UDP_VITA49 - VITA49 Data packet format |
| ip_address | string | Destination IP address (MSDD OUT IPP) |
| port | short | Destination IP port (MSDD OUT IPP ) |
| vlan_enable | boolean | Enable VLAN tagging (MSDD OUT VLANEN) |
| vlan_enable | short | VLAN ID for 802.1q packets (MSDD OUT VLANTCI)|
| timestamp_offset |  short | Offset applied in 1ns increments (MSDD OUT TSOFS) |
| endianess | short | BIG ENDIAN (0), LITTLE ENDIAN (1) (MSDD OUT END) |
| mfp_flush | long | Packet flush control bit mask (MSDD OUT MFP) |

### msdd_status
The *msdd_status* structure defines the set of readonly properties that define the state of the MSDD receiver.

| *NAME* | *TYPE* |  *DESCRIPTION* |
| :--------| :---------| :---------------|
| msdd_status::connected | boolean | True if connected to MSDD receiver |
| msdd_status::ip_address | string | IP Address from MSDD CON IPP? |
| msdd_status::port | string | Port number from MSDD CON IPP? |
| msdd_status::model | string | Model name from MSDD CON CFG? MODEL |
| msdd_status::serial | string |  Serical number from MSDD CON CFG? SERIAL |
| msdd_status::software_part_number | string | Part number from MSDD CON IDN? |
| msdd_status::rf_board_type | string | Value from MSDD CON CFG? RF_BRD_TYPE |
| msdd_status::fpga_type | string | Value from MSDD CON CFG? FPGA_TYPE |
| msdd_status::dsp_type | string | Value from MSDD CON CFG? DSP_BRD_TYPE |
| msdd_status::minimum_frequency_hz | string | Value from MSDD CON CFG? MIN_FREQ |
| msdd_status::maximum_frequency_hz | string | Value from MSDD CON CFG? MAX_FREQ |
| msdd_status::dsp_reference_frequency_hz | string | Value MSDD CON CFG? DSP_REF |
| msdd_status::adc_clock_frequency_hz | string | Value from MSDD CON CFG? ADC_CLK |
| msdd_status::num_if_ports | string | Value from MSDD CON CFG? IF_PORTS |
| msdd_status::num_eth_ports | string | Value from MSDD CON CFG? ETHR_PORTS |
| msdd_status::cpu_type | string | Value from MSDD CON CFG? CPU_TYPE |
| msdd_status::cpu_rate | string | Value from MSDD CON CFG? CPU_FREQ  |
| msdd_status::cpu_load | string | Value from MSDD CON CPL? |
| msdd_status::pps_termination | string | Value from MSDD CON CFG? 1PPS_TERM |
| msdd_status::pps_voltage | string | Value from MSDD CON CFG? 1PPS_VOLTAGE |
| msdd_status::number_wb_ddc_channels | string | Value from MSDD CON CFG? FPGA_WBDDC_CHANNELS |
| msdd_status::number_nb_ddc_channels | string | Value from MSDD CON CFG? FPGA_NBDDC_CHANNELS |
| msdd_status::filename_app | string | Value from MSDD CON CFG? FILE_NAME_APP |
| msdd_status::filename_fpga | string | Value from MSDD CON CFG? FILE_NAME_FPGA |
| msdd_status::filename_batch | string |Value from MSDD CON CFG? FILE_NAME_BATCH|
| msdd_status::filename_boot | string | Value from MSDD CON CFG? FILE_NAME_BOOT |
| msdd_status::filename_loader  | string | Value from MSDD CON CFG? FILE_NAME_LOADER |
| msdd_status::filename_config  | string | Value from MSDD CON CFG? FILE_NAME_CONFIG |
| msdd_status::tod_module  | string | Value from MSDD TOD MOD? |
| msdd_status::tod_available_module | string | Value from MSDD TOD MODL? |
| msdd_status::tod_meter_list | string | Value from MSDD TOD MTR? |
| msdd_status::tod_reference_adjust | string | Value from MSDD TOD RFA? |
| msdd_status::tod_track_mode_state | string | Value from MSDD TOD RTK? |
| msdd_status::tod_bit_state | string | Value from MSDD TOD BIT? |
| msdd_status::tod_toy | string | Value from MSDD TOY? |

## msdd_advanced_debugging_tools-instructions
The *msdd_advanced_debugging_tools* structure will pass the command property as MSDD command and return the response as the response property

| *NAME* | *TYPE* |  *DESCRIPTION* |
| :--------| :---------| :---------------|
| msdd_advanced_debugging_tools::command | string | Command to send to the MSDD |
| msdd_advanced_debugging_tools::response | string | Response from the MSDD |

### msdd_gain_configuration
The *msdd_gain_configuration* structure controls the gain setting for MSDD  tuner type.

| *NAME* | *TYPE* |  *DESCRIPTION* |
| :--------| :---------| :---------------|
| msdd_gain_configuration::rcvr_gain | float | Gain setting in db for MSDD RCV modules |
| msdd_gain_configuration::wb_ddc_gain | float | Gain setting in db for MSDD WBDDC modules |
| msdd_gain_configuration::hw_ddc_gain | float | Gain setting in db for MSDD NBDDC modules |
| msdd_gain_configuration::sw_ddc_gain | float | Gain setting in db for MSDD SWDDC modules |

### FRONTEND::tuner_status
The *FRONTEND::tuner_status* structure defines the `FRONTEND` tuner status properties for each defined tuner.  The standard tuner status properties are defined in the REDHAWK documentation.  This table describes the additional properties defined for each MSDD tuner.

| *NAME* | *TYPE* |  *DESCRIPTION* |
| :--------| :---------| :---------------|
| FRONTEND::tuner_status::output_format | string | SDDS data format, fixed to `CI` (16bit IQ)|
| FRONTEND::tuner_status::output_multicast | string | Destination IP address of data packets, (MSDD OUT IPP?) |
| FRONTEND::tuner_status::output_port | long | Destination port of data packets, (MSDD OUT IPP?) |
| FRONTEND::tuner_status::output_vlan | long | VLAN number if VLAN is enabled, (MSDD OUT VLANTCI?)|
| FRONTEND::tuner_status::msdd_channel_type | string | RX, WBDDC, NBDDC, SWDDC |
| FRONTEND::tuner_status::msdd_installation_name_csv | string | List of MSDD modules assigned to this tuner |
| FRONTEND::tuner_status::msdd_registration_name_csv | string | List of MSDD registration names assigned to this tuner |
| FRONTEND::tuner_status::bits_per_sample | short | Number of bits per data sample. |
| FRONTNED::tuner_status::adc_meter_value | string | Meter value from MSDD RCV AMD? |
| FRONTEND::tuner_status::rcvr_gain | float | Value from MSDD RCV GAI? |
| FRONTEND::tuner_status::ddc_gain | float | Value from MSDD WBDDC/NBDDC GAI? |
| FRONTEND::tuner_status::allocated | boolean | True if tuner is allocated, False otherwise |
| FRONTEND::tuner_status::input_sample_rate | double | Value from MSDD OUT ISR? |
| FRONTEND::tuner_status::output_channel | string | MSDD output module assigned to this tuner |
| FRONTEND::tuner_status::output_enabled | boolean | True if output is enabled, False otherwise |
| FRONTEND::tuner_status::output_vlan_enabled | boolean | True if vlan tagging is enabled, MSDD OUT VLANEN? |
| FRONTEND::tuner_status::output_vlane_tci | string | VLAN number if tagging is enabled, MSDD OUT VLANTCI? |
| FRONTEND::tuner_status::output_flow | string | MSDD module chain feeding this output module |
| FRONTEND::tuner_status::output_timestamp_offset | string | Value from MSDD OUT TSOFS?)
| FRONTEND::tuner_status::endianess | short | Value from MSDD OUT END? |
| FRONTEND::tuner_status::output_mfp_flush | long | Value from MSDD OUT MFP? |
| FRONTEND::tuner_status::psd_fft_size | double | Value from MSDD FFT PNT? |
| FRONTEND::tuner_status::psd_averages | double | Value from MSDD FFT AVG? |
| FRONTEND::tuner_status::psd_time_between_ffts | double | Value from MSDD FFT RAT? |
| FRONTEND::tuner_status::psd_output_bin_size | double | Value from MSDD FFT BIN? |
| FRONTEND::tuner_status::psd_window_type | string | Value from MSDD FFT WND? |
| FRONTEND::tuner_status::psd_peak_mode | string | Value from MSDD FFT PMD?


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

The [msdd_configuration property](#msdd_configuration) is used to setup the network connectivity between the MSDD hardware and host computer running the REDHAWK rh.MSDD device.  Set the `msdd_ip_address` and `msdd_port` properties for the MSDD receiver you are connecting.


## FEI Compliance Test Results

See the [FEI Compliance Results](tests/FEI_Compliance_Results.md) document.
