#!/usr/bin/python

import sys
import time
import traceback
import re
from datetime import datetime, timedelta
from msdd_connection import *

def get_seconds_from_start_of_year():
    utcdiff = datetime.utcnow() - datetime(int(datetime.now().year),1,1)
    whole_sec = (utcdiff.seconds + utcdiff.days * 24 * 3600)
    frac_sec = utcdiff.microseconds / 1e6
    return whole_sec+frac_sec

def get_tod(c=None):
    try:
        if not c:
            c=Connection((ip_addr,23))

        # query TOD module values
        cmd='tod get?'
        tod=c.sendStringCommand(cmd+'\n')
        # strip off command echo
        return float(tod.split('GET')[-1].lstrip())
    except:
        print traceback.print_exc()
        pass



def set_tod(seconds, c=None):
    try:
        if not c:
            c=Connection((ip_addr,23))
        c._debug=True

        # query TOD module values
        cmd='tod set '+str(seconds)
        tod=c.sendStringCommand(cmd+'\n', expect_output=False)
    except:
        print traceback.print_exc()
        pass


def get_tod_bit(c=None):
    try:
        if not c:
            c=Connection((ip_addr,23))

        # query TOD module values
        cmd='tod bit?'
        tod=c.sendStringCommand(cmd+'\n')
        # strip off command echo
        return int(tod.split('BIT')[-1].lstrip())
    except:
        print traceback.print_exc()
        pass

def get_brd_bit(c=None):
    try:
        if not c:
            c=Connection((ip_addr,23))

        # query TOD module values
        cmd='brd bit?'
        tod=c.sendStringCommand(cmd+'\n')
        # strip off command echo
        return int(tod.split('BIT')[-1].lstrip())
    except:
        print traceback.print_exc()
        pass


def run_test(ip_addr, t):
    c=Connection((ip_addr,23))
    n=20
    while n:
        pre_tod=get_tod(c)
        time.sleep(1.0-(pre_tod-int(pre_tod)))
        tb=get_tod_bit(c)
        brdb=get_brd_bit(c)
        set_tod(t,c)
        time.sleep(2.2)
        tod=get_tod(c)
        print " start ", pre_tod, " set ", t , " == ", tod
        n -=1

if __name__ == '__main__':
    c=Connection((sys.argv[1],23))
    print get_tod(c)
