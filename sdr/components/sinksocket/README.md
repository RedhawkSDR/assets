# REDHAWK sinksocket
 
## Description

The `sinksocket` component reads data from BULKIO ports and writes it to TCP connections.  The data from all ports goes to each of the connections.  Each `sinksocket` component can operate multiple TCP connections; each of which can be either a TCP client or server.


### Properties

The following table describes the top level properties of `sinksocket`.

| **NAME** | **TYPE** | **ACCESS** | **DESCRIPTION** |
| :--------| :--------| :-----------| :---------------|
| `total_bytes` | `double` | readonly | Number of bytes sent over the network by all connections combined. |
| `bytes_per_sec` | `float` | readonly | Bytes-per-second, for data sent over the network, for all connections combined. |
| `Connections` | `[Connection]` | read/write | A list of data structures, each defining a `sinksocket` Connection. |
| `ConnectionStats` | `[ConnectionStat]` | readonly | A list of data structures, each with information about a TCP connection. |


The following table describes the properties within a single Connection.  If a `sinksocket` is created without properties specified by the user, a single Connection will be created, with the default values given in the table.

| **NAME** | **TYPE** | **DEFAULT** | **DESCRIPTION** |
| :--------| :--------| :-----------| :---------------|
| `connection_type` | `string` | `server` | Enumerated values:  `client`, `server`.<br/>Set whether `sinksocket` is a TCP client or server. |
| `ip_address` | `string` | `""` | If this Connection is a TCP client, set the ip address to which it will connect.<br/>If this Connection is a TCP server, this value is ignored. |
| `ports` | `[ushort]` | `[32191]` | If this Connection is a TCP client, set the list of port numbers to which `sinksocket` will connect.<br/>If this Connection is a TCP server, set the list of port numbers  on which `sinksocket` will listen. |
| `tcp_nodelays` | `[boolean]` | `[false]` | If this Connection is a TCP client and `tcp_nodelay` is `true`, disable the TCP connection from combining packets with Nagle's algorithm.<br/>If this Connection is a TCP server, this value is ignored. |
| `byte_swap` | `[ushort]` | `[0]` | Reorder bytes of integer values between little-endian and big-endian representation.<br/>Values:<br/> - `0`: no byte swapping<br/> - `1`: byte swap according to the data size for each port<br/> - `<num>`: swap bytes as if the data type length was `<num>` |

A `sinksocket` Connection contains a set of TCP connections.  Each TCP connection corresponds to a TCP port number.  These numbers are listed in the `ports` list.  The `byte_swap` and `tcp_nodelays` lists relate to the `ports` list like associative arrays.  That is, the first element of each corresponds to the same TCP connection.  The same is true for the nth element of each list.


The following table describes the properties within a single ConnectionStat.  Each ConnectionStat contains information about one of the TCP connections within a `sinksocket` Connection.

| **NAME** | **TYPE** | **DESCRIPTION** |
| :--------| :------- | :---------------|
| `ip_address` | `string` | The IP address of the Connection. If the Connection is type `server`, this value will be blank. |
| `port` | `ushort` | The TCP port of the connection. |
| `status` | `string` | The status of the connection.<br/>Enumerated values:  `startup`, `not_connected`, `connected`, `error`. |
| `bytes_per_second` | `float` | Number of bytes sent over the network by the connection. |
| `bytes_sent` | `double` | Bytes-per-second, for data sent over the network, for the connection. |


## Installation Instructions

For instructions on how to install the REDHAWK basic assets from RPMs, see the REDHAWK manual.

To install from source, rather than from RPMs, continue with the following steps.

### Step 1:  Prerequisites

Ensure these prerequisites are satisfied:

* the `rh.dsp` shared library is installed
* `OSSIEHOME` is set
* `SDRROOT` is set


### Step 2:  Build

To create the `sinksocket` executable in the `cpp` subdirectory, but not install it to `SDRROOT`:
```
$ cd /path/to/redhawk/assets
$ cd $(find . -name sinksocket)
$ ./build.sh
```

### Step 3 (Optional):  Test

With Step 2 complete, you can run the tests:
```
$ cd tests
$ python test_sinksocket.py
```

### Step 4:  Install

In order to install `sinksocket` to `SDRROOT`, repeat Step 2, but replace `build.sh` with `./build.sh install`.


## Asset Use

### Example: `sinksocket` as a TCP Server

For the examples, have a version of `netcat` installed.

#### Instructions to Run the Example

Save this python code to a file name `demo.py`.
```python
#!/usr/bin/env python

from pprint import pprint

from ossie.utils import sb

# Use a StreamSource to provide demo data.
streamsource = sb.StreamSource()

# Create the sinksocket.
props = {
    'Connections': [
        {
            'connection_type': 'server',
            'ports': [32191, 32192],
            'byte_swap': [0, 2],
            'tcp_nodelays': [False, False],
        }
    ]
}
sinksocket = sb.launch('rh.sinksocket', properties=props)
streamsource.connect(sinksocket, providesPortName='dataChar_in')

sb.start()

# Manually send data, or quit.
while raw_input() != 'q':
    streamsource.write('abcd..')
streamsource.close()

# Display information supported by sinksocket.
print 'total bytes:  {0}'.format(sinksocket.total_bytes)
print 'bytes_per_second:  {0}'.format(sinksocket.bytes_per_sec)
print
for c in sinksocket.Connections:
    pprint(c, indent=4)
print
for c in sinksocket.ConnectionStats:
    pprint(c, indent=4)
print

sb.stop()
```

Use 3 terminals.  The order of these operations is important.  In terminal 1:
```bash
$ python demo.py
```

In terminal 2:
```bash
$ nc 127.0.0.1 32191
```

In terminal 3:
```bash
$ nc 127.0.0.1 32192
```

Back in terminal 1, each time you press `ENTER`, the data `'abcd..'` will be sent.  Press `ENTER` a few times. Then press `q` and `ENTER` to quit.

In terminals 2 and 3, press `ctrl-c` to quit.


#### Discussion of the Output

Terminal 2 should show `'abcd..'` for as many times as you pressed `ENTER` before `q` in terminal 1.

Terminal 3 should look like terminal 2, but the order of the characters should be `'badc..'`.  The first and second characters are swapped with each other.  So are the third and fourth, etc.  This happened because of the `byte_swap` property setting of `2` when the `sinksocket` was created.

The rest of the output in terminal 1, and the python code, show how to interact with `sinksocket`.


### Example: `sinksocket` as a TCP Client

This example is similar to the one in which `sinksocket` is a TCP server.  To run the example, make these changes:

* in `demo.py`, replace `'server'` with `'client'`
* in terminals 2 and 3, add the `-l` flag after `nc`

Then, run this example in the same way as the other one.

The order of operations is less important in this case.  If `sinksocket` is configured to expect a TCP server, and that server is unavailable, `sinksocket` skips it.  When the server comes online, `sinksocket` will automatically connect and start/resume sending data to it.


## Branches and Tags

This content is currently under development.

## REDHAWK Version Compatibility

This version of `sinksocket` is compatible with REDHAWK 2.2.

## Copyright

This work is protected by Copyright. Please refer to the [Copyright File](COPYRIGHT) for updated copyright information.

## License

REDHAWK rh.sinksocket is licensed under the GNU Lesser General Public License (LGPL).
