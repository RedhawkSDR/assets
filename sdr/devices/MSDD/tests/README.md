
rh.MSDD unit tests files

* id_msdd.py - identify the FPGA load on the MSDD
```
./id_msdd.py 192.168.11.2
MSDD|3000|s98|1w5n5b1300
```

* reset_msdd - performs a reset operation of the DSP board
```
./reset_msdd 192.168.11.2
```

* test_MSDD.py - perform MSDD specific unit tests

```
# run all the unit tests
 ./test_MSDD.py  --ip=192.168.11.2 --iface=em3

 # run a set of unit tests
 ./test_MSDD.py  --ip=192.168.11.2  --iface=em3 -v IPPInterfaceTest
```

* test_MSDD_FEI.py - run Frontend unit tests against rh.MSDD

The file dut.py contains configuration settings for most of the FPGA loads used during testing. It is no longer required to edit this file if you are using one of these configurations. The FEI tests determine the FPGA load on the MSDD radio and configure the proper setting when making allocations. If a new configuration is required, consult the comments in the file and prior configurations as examples.  

```
 # run all the FEI unit tests
 ./test_MSDD_FEI.py   --ip=192.168.11.2

 # a series of FEI tests that start with testFRONTEND_2  
 ./test_MSDD_FEI.py   --ip=192.168.11.2 --debug=ERROR  --nocapture test_MSDD_FEI.py:FrontendTunerTests -m  testFRONTEND_2

```
