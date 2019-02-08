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
    radio.out_modules[19].object.setEnable(False)
    radio.wb_ddc_modules[0].object.setEnable(False)
    



