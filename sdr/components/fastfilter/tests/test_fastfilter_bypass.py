import unittest
import ossie.utils.testing
import os
from omniORB import any
from ossie.cf import CF
from omniORB import CORBA
from ossie.utils import sb
import time
import random

#from filter_test_helpers import *


class testBypassMode:

    def setup(self):
    	pass

    def tearDown(self):
    	sb.release()

    def testBypassMode(self):
   	#instantiate components
  	ff = sb.launch('rh.fastfilter')
   	sg = sb.launch('rh.SigGen')
   	#set component paraeters
   	sg.shape = 'square'
   	ff.realFilterCoefficients = [1] #turn on bypass mode

   	ffOut = sb.DataSink()
   	sgOut = sb.DataSink()

   	ff.connect(ffOut)
   	sg.connect(sgOut, usesPortName = 'dataFloat_out')
   	sg.connect(ff, usesPortName = 'dataFloat_out')

   	sb.start()

   	ff_data = ffOut.getData(1000)
   	sg_data = sgOut.getData(1000)

   	ut = unittest.TestCase('run')

   	ut.assertEqual(ff_data,sg_data, 'Data was not equal')

   	ffOut.reset()
   	sgOut.reset()

   	time.sleep(1)
   	sb.stop()

    def testDeactivateBypassMode(self):
   	ff = sb.launch('rh.fastfilter')
   	sg = sb.launch('rh.SigGen')
   	#set component paraeters
   	sg.shape = 'square'
   	ff.realFilterCoefficients = [10] #turn off bypass mode

   	ffOut = sb.DataSink()
   	sgOut = sb.DataSink()
	
   	ff.connect(ffOut)
   	sg.connect(sgOut, usesPortName = 'dataFloat_out')
   	sg.connect(ff, usesPortName = 'dataFloat_out')

   	sb.start()

   	ff_data = ffOut.getData(1000)
   	sg_data = sgOut.getData(1000)

   	ut = unittest.TestCase('run')


   	ut.assertNotEqual(ff_data,sg_data, 'Data was equal')
   	ffOut.reset()
   	sgOut.reset()

   	time.sleep(1)
   	sb.stop()


