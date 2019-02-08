#
# This file is protected by Copyright. Please refer to the COPYRIGHT file
# distributed with this source distribution.
#
# This file is part of REDHAWK rh.MSDD.
#
# REDHAWK rh.MSDD is free software: you can redistribute it and/or modify it under
# the terms of the GNU Lesser General Public License as published by the Free
# Software Foundation, either version 3 of the License, or (at your option) any
# later version.
#
# REDHAWK rh.MSDD is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
# FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more
# details.
#
# You should have received a copy of the GNU Lesser General Public License
# along with this program.  If not, see http://www.gnu.org/licenses/.
#
from msddcontroller import *

import os,sys,time

if __name__ == "__main__":

    ip = "192.168.1.250"
    port = 23

    radio = MSDDRadio(ip, port)

    
    radio.spectral_scan_modules[0].object.setEnable(False)
    radio.spectral_scan_modules[0].object.setFrequencies(250000000.0, 500000000.0)
    print "Current Frequencies: "
    print radio.spectral_scan_modules[0].object.getFrequencies()
    print "FFT Size List: "
    print radio.spectral_scan_modules[0].object.getFFTSizeList()
    print "Current FFT Size: "
    print radio.spectral_scan_modules[0].object.getFFTSize()
    print "FFT Rate List: "
    print radio.spectral_scan_modules[0].object.getFFTRateList()
    print "Current FFT Rate: "
    print radio.spectral_scan_modules[0].object.getFFTRate()
    print "FFT Average List: "
    print radio.spectral_scan_modules[0].object.getAverageList()
    print "Current FFT Average: "
    print radio.spectral_scan_modules[0].object.getAverage()
    print "FFT Bin Size List: "
    print radio.spectral_scan_modules[0].object.getBinSizeList()
    print "Current Bin Size: "
    print radio.spectral_scan_modules[0].object.getBinSize()
    print "Current Tipping Mode: "
    print radio.spectral_scan_modules[0].object.getTippingMode()
    print "Current Peak Mode: "
    print radio.spectral_scan_modules[0].object.getPeakMode()
    print "Setup for SPC: "
    print radio.spectral_scan_modules[0].object.send_custom_command("SRT SRCL? WBDDC:0\n") #SRT DSTL? SPC:0")
    radio.spectral_scan_modules[0].object.send_custom_command("SRT UNL OUT:19\n", False)
    radio.spectral_scan_modules[0].object.send_custom_command("SRT LNK RCV:1 WBDDC:0 SPC:0 OUT:19\n", False)
    print radio.spectral_scan_modules[0].object.send_custom_command("SRT DSTL? SPC:0\n") #SRT DSTL? SPC:0")
    print "Setup for SPC: "
    print radio.spectral_scan_modules[0].object.send_custom_command("SRT DSTL? WBDDC:0\n") #SRT DSTL? SPC:0")
    print "Setup for SPC: "
    print radio.spectral_scan_modules[0].object.send_custom_command("SRT SRCL? OUT:0\n") #SRT DSTL? SPC:0")
    print "Setup for SPC: "
    print radio.spectral_scan_modules[0].object.send_custom_command("SRT SRCL? OUT:2\n") #SRT DSTL? SPC:0")
    print "Setup for SPC: "
    print radio.spectral_scan_modules[0].object.send_custom_command("SRT SRCL? OUT:19\n") #SRT DSTL? SPC:0")
    print "Output Protocol List: "
    print radio.out_modules[19].object.getOutputProtocolList()
    print "Current Output Protocol: "
    print radio.out_modules[19].object.getOutputProtocol()
    print "Output Data Width List: "
    print radio.out_modules[19].object.getOutputDataWidthList()
    print "Current Output DataWidth: "
    print radio.out_modules[19].object.getOutputDataWidth()
    print "Current WB Decimation: " 
    print radio.wb_ddc_modules[0].object.getDecimation()
 
    radio.log_modules[0].object.setIPP("192.168.1.201:8900")
    radio.log_modules[0].object.setMask(7)
    radio.log_modules[0].object.setLogChannelEnable(True)

    radio.spectral_scan_modules[0].object.setEnable(False)
    radio.out_modules[19].object.setEnable(False)
    radio.wb_ddc_modules[0].object.setEnable(False)
    time.sleep(2)
    #Set radio setting needed to enable output
    radio.wb_ddc_modules[0].object.setEnable(True)
    radio.out_modules[19].object.setOutputProtocol(1)
    radio.out_modules[19].object.setIP('192.168.1.201', 8880)
    radio.out_modules[19].object.setEnable(True)
    print "Current Output 2 Enabled: " 
    print radio.out_modules[19].object.getEnable()
    print "Current Output 2 BIT: " 
    print radio.out_modules[19].object.getBIT()
    print "Current Output 2 UDP Packet Rate: " 
    print radio.out_modules[19].object.getUdpPacketRate()
    print "Current Output 2 UDP Packet Length: " 
    print radio.out_modules[19].object.getOutputSamplesPerFrame()
    radio.spectral_scan_modules[0].object.setFFTSize(4096)
    radio.spectral_scan_modules[0].object.setFFTRate(200)
    radio.spectral_scan_modules[0].object.setAverage(1)
    radio.spectral_scan_modules[0].object.setBinSize(4096)
    time.sleep(1)
    radio.spectral_scan_modules[0].object.setEnable(True)
    



