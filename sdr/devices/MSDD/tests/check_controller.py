from msdd_connection import *

import sys
r=MSDDRadio(sys.argv[1],23,
            enable_inline_swddc=True, 
            enable_secondary_tuners=False, 
            enable_fft_channels=True)

#### Console Module ####
print r.console.ID
print r.console.address
print r.console.ip_address
print r.console.ip_port
print r.console.sw_part_number
print r.console.echo
print r.console.cpu_load
print r.console.model
print r.console.serial
print r.console.minFreq_Hz
print r.console.maxFreq_Hz
print r.console.dspRef_Hz
print r.console.adcClk_Hz
print r.console.rf_board_type
print r.console.num_if_ports
print r.console.bit_adc_exists
print r.console.num_eth_ports
print r.console.cpu_type
print r.console.cpu_rate
print r.console.fpga_type
print r.console.dsp_type
print r.console.pps_termination
print r.console.pps_voltage
print r.console.num_wb_channels
print r.console.num_nb_channels
print r.console.filename_app
print r.console.filename_fpga
print r.console.filename_batch
print r.console.filename_boot
print r.console.filename_loader
print r.console.filename_config
print r.console.filename_cal
             
status=r.rx_channels[0].getStatus()
print status.enabled
print status.decimation
print status.available_decimation
print status.attenuation
print status.available_attenuation
print status.gain
print status.available_gain
print status.input_sample_rate
print status.sample_rate
print status.available_sample_rate
print status.bandwidth
print status.available_bandwidth
print status.center_frequency
print status.available_frequency
print status.ddc_gain

status=r.rx_channels[0].getOutputStatus()
print status.output_multicast
print status.output_port
print status.output_enabled
print status.output_protocol
print status.output_vlan_enabled
print status.output_vlan_tci
print status.output_vlan
print status.output_flow
print status.output_timestamp_offset
print status.output_channel
print status.output_endianess
print status.output_mfp_flush

fft=r.get_fft_channel()
r.rx_channels[0].addFFTChannel(fft)
status=r.rx_channels[0].getFFTStatus()
print status.psd_fft_size
print status.psd_averages
print status.psd_time_between_ffts
print status.psd_output_bin_size
print status.psd_window_type
print status.psd_peak_mode

status=r.rx_channels[1].getStatus()
print status.enabled
print status.decimation
print status.available_decimation
print status.attenuation
print status.available_attenuation
print status.gain
print status.available_gain
print status.input_sample_rate
print status.sample_rate
print status.available_sample_rate
print status.bandwidth
print status.available_bandwidth
print status.center_frequency
print status.available_frequency
print status.ddc_gain

status=r.rx_channels[1].getOutputStatus()
print status.output_multicast
print status.output_port
print status.output_enabled
print status.output_protocol
print status.output_vlan_enabled
print status.output_vlan_tci
print status.output_vlan
print status.output_flow
print status.output_timestamp_offset
print status.output_channel
print status.output_endianess
print status.output_mfp_flush

fft=r.get_fft_channel()
r.rx_channels[1].addFFTChannel(fft)
status=r.rx_channels[1].getFFTStatus()
print status.psd_fft_size
print status.psd_averages
print status.psd_time_between_ffts
print status.psd_output_bin_size
print status.psd_window_type
print status.psd_peak_mode


status=r.rx_channels[6].getStatus()
print status.enabled
print status.decimation
print status.available_decimation
print status.attenuation
print status.available_attenuation
print status.gain
print status.available_gain
print status.input_sample_rate
print status.sample_rate
print status.available_sample_rate
print status.bandwidth
print status.available_bandwidth
print status.center_frequency
print status.available_frequency
print status.ddc_gain

status=r.rx_channels[6].getOutputStatus()
print status.output_multicast
print status.output_port
print status.output_enabled
print status.output_protocol
print status.output_vlan_enabled
print status.output_vlan_tci
print status.output_vlan
print status.output_flow
print status.output_timestamp_offset
print status.output_channel
print status.output_endianess
print status.output_mfp_flush

fft=r.get_fft_channel()
r.rx_channels[6].addFFTChannel(fft)
status=r.rx_channels[6].getFFTStatus()
print status.psd_fft_size
print status.psd_averages
print status.psd_time_between_ffts
print status.psd_output_bin_size
print status.psd_window_type
print status.psd_peak_mode


