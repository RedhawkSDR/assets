#!/usr/bin/python3

"""
Find the MSDD identification string an print out the results. The resulting string is used to index into the test configurations for test_MSDD.py and dut.py
"""
import sys
import time
import traceback
import re
from msdd_connection import *

def id_msdd( ip_addr):
    # create connection to msdd device
    c=Connection((ip_addr,23))
    try:
        # query IDN of receiver
        cmd='con idn?'
        id=c.sendStringCommand(cmd+'\n', check_for_output=True)
        # strip off command echo
        full_id=id.split('IDN')[-1].lstrip()

        # query FPGA load on receiver
        cmd='con cfg? file_name_fpga?'
        fpga=c.sendStringCommand(cmd+'\n', check_for_output=True)
        fpga_load=fpga.split('FILE_NAME_FPGA?')[-1]

        # split out model from ID string
        id=full_id.split(',')[0]
        # split make and  model
        id=re.split('[-,]',full_id)

        # split out fpga file tokens
        fpga=re.split('[_-]',fpga_load)
        # put response string that can be used as primary key for dictionay
        # setting for that device
        return  "|".join([id[0], id[1], fpga[1],fpga[2]])
    except:
        traceback.print_exc()
        pass


if __name__ == '__main__':
    print(id_msdd(sys.argv[1]))
