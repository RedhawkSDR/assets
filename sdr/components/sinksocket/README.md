# REDHAWK rh.sinksocket

## Table of Contents

* [Description](#description)
* [Properties](#properties)
* [Installation](#installation)
* [Usage](#usage)

## Description

The `rh.sinksocket` component reads data from BulkIO ports and writes the same data to all TCP connections.  Each `rh.sinksocket` component can operate multiple TCP connections (TCP clients or servers).

The [Properties](#properties) section explains how to configure the component and monitor its data throughput.  The component supports the following configurations:

* TCP mode (client or server)
* IP address (for TCP client mode)
* ports
* whether Nagle's algorithm for combining TCP packets is allowed
* whether and how endian byte reordering is done

### Properties

The following table describes the top level properties of `rh.sinksocket`.

| **NAME** | **TYPE** | **ACCESS** | **DESCRIPTION** |
| :--------| :--------| :-----------| :---------------|
| `total_bytes` | `double` | readonly | Number of bytes sent over the network by all connections combined. |
| `bytes_per_sec` | `float` | readonly | Bytes-per-second, for data sent over the network, for all connections combined. |
| `Connections` | `[Connection]` | read/write | A list of data structures, each defining an `rh.sinksocket` Connection. |
| `ConnectionStats` | `[ConnectionStat]` | readonly | A list of data structures, each with information about a TCP connection. |

The following table describes the properties within a single Connection.  If an `rh.sinksocket` is created without properties specified by the user, a single Connection is created with the default values given in the table.

| **NAME** | **TYPE** | **DEFAULT** | **DESCRIPTION** |
| :--------| :--------| :-----------| :---------------|
| `connection_type` | `string` | `server` | Enumerated values:  `client`, `server`.<br/>Set whether `rh.sinksocket` is a TCP client or server. |
| `ip_address` | `string` | `""` | If this Connection is a TCP client, set the IP address to which it will connect.<br/>If this Connection is a TCP server, this value is ignored. |
| `ports` | `[ushort]` | `[32191]` | If this Connection is a TCP client, set the list of port numbers to which `rh.sinksocket` will connect.<br/>If this Connection is a TCP server, set the list of port numbers  on which `rh.sinksocket` will listen. |
| `tcp_nodelays` | `[boolean]` | `[false]` | If `tcp_nodelay` is `true`, prevent the TCP connection from combining packets with Nagle's algorithm. |
| `byte_swap` | `[ushort]` | `[0]` | Reorder bytes of integer values between little-endian and big-endian representation.<br/>Values:<br/> - `0`: no byte swapping<br/> - `1`: byte swap according to the data size for each port<br/> - `<num>`: swap bytes as if the data type length was `<num>` |

An `rh.sinksocket` Connection contains a set of TCP connections.  Each TCP connection corresponds to a TCP port number.  These numbers are listed in the `ports` list.  The `byte_swap` and `tcp_nodelays` lists relate to the `ports` list like associative arrays.  That is, the first element of each corresponds to the same TCP connection.  The same is true for the nth element of each list.

The following table describes the properties within a single `ConnectionStat`.  Each `ConnectionStat` contains information about one of the TCP connections within an `rh.sinksocket` Connection.

| **NAME** | **TYPE** | **DESCRIPTION** |
| :--------| :------- | :---------------|
| `ip_address` | `string` | The IP address of the Connection. If the Connection is type `server`, this value will be blank. |
| `port` | `ushort` | The TCP port of the connection. |
| `status` | `string` | The status of the connection.<br/>Enumerated values:  `startup`, `not_connected`, `connected`, `error`. |
| `bytes_per_second` | `float` | Number of bytes sent over the network by the connection. |
| `bytes_sent` | `double` | Bytes-per-second, for data sent over the network, for the connection. |


## Installation

The following procedure explains how to install `rh.sinksocket` from source. `rh.sinksocket` is one of the REDHAWK basic assets. For information about how to install the REDHAWK basic assets from RPMs, refer to the REDHAWK Manual.

1. Ensure `OSSIEHOME` and `SDRROOT` are both set. For a standard install, enter the following commands to set them:

```sh
$ cd /etc/profile.d
$ . redhawk.sh
$ . redhawk-sdrroot.sh
```

2. To build and install `rh.sinksocket`, enter the following commands:

```
$ cd /path/to/redhawk/assets
$ cd $(find . -name sinksocket)
$ ./build.sh install
```
The `rh.sinksocket` executable is created in the `cpp` subdirectory, and then the component is installed to `SDRROOT`.

## Usage

The following examples explain how to use the `rh.sinksocket` component.

* [Example 1](#example-1-rhsinksocket-as-a-server-in-the-redhawk-sandbox): `rh.sinksocket` as a server in the REDHAWK sandbox
* [Example 2](#example-2-rhsinksocket-as-a-client-in-the-redhawk-sandbox): `rh.sinksocket` as a client in the REDHAWK sandbox
* [Example 3](#example-3-rhsinksocket-as-a-server-in-a-redhawk-waveform): `rh.sinksocket` as a server in a REDHAWK waveform
* [Example 4](#example-4-rhsinksocket-as-a-client-in-a-redhawk-waveform): `rh.sinksocket` as a client in a REDHAWK waveform

All of the examples configure the component to use two ports.  One port does not swap byte endianness, while the other swaps bytes for a data word size of 2.  The first port allows packet combining with Nagle's algorithm, while the second does not.  However, packet combining is not observable in these examples.

**Note: For the sandbox examples, a version of `netcat` must be installed.**

### Example 1: `rh.sinksocket` as a Server in the REDHAWK Sandbox

#### Running the Example

1. Save the following python code to a file named `demo.py`.
```python
#!/usr/bin/env python3

from pprint import pprint

from ossie.utils import sb

# Use a StreamSource to provide demo data.
streamsource = sb.StreamSource()

# Create the rh.sinksocket.
props = {
    'Connections': [
        {
            'connection_type': 'server',
            'ports': [32191, 32192],
            'byte_swap': [0, 2],
            'tcp_nodelays': [False, True],
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

# Display connection information supported by rh.sinksocket.
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
2. Use three terminals to enter the following commands.  **Note: The order of these operations is important.**

* In terminal 1, enter:
```bash
$ python demo.py
```

* In terminal 2, enter:
```bash
$ nc 127.0.0.1 32191
```

* In terminal 3, enter:
```bash
$ nc 127.0.0.1 32192
```

3. In terminal 1, press `Enter` a few times.  
Each time you press `Enter`, the data `'abcd..'` is sent.

4. In terminal 1, press `q` and `Enter` to quit.
5. In terminals 2 and 3, press `Ctrl-c` to quit.


#### Analyzing the Output

Terminal 1 displays the connection information produced by the print statements.

Terminal 2 displays `'abcd..'` equivalent to the number of times you pressed `Enter` before pressing `q` in terminal 1.

Terminal 3 displays the information the same number of times as in terminal 2, but the order of the characters is `'badc..'`.  The first and second characters are swapped with each other as are the third and fourth, etc.  This change occurred because the `byte_swap` property was set to `2`.

### Example 2: `rh.sinksocket` as a Client in the REDHAWK Sandbox

This is similar to [Example 1](#example-1-rhsinksocket-as-a-server-in-the-redhawk-sandbox), except that the component is a TCP client.

1. To run the example, make these changes:

* in `demo.py`, replace `'server'` with `'client'`
* below ``'connection_type': 'client'``, add the following line: `'ip_address': '127.0.0.1',`
* in terminals 2 and 3, add the `-l` flag after `nc`

2. Then, run this example in the same way as [Example 1](#example-1-rhsinksocket-as-a-server-in-the-redhawk-sandbox).

The order of operations is less important in this case.  If `rh.sinksocket` is configured to expect a TCP server, and that server is unavailable, `rh.sinksocket` skips it.  When the server comes online, `rh.sinksocket` automatically connects and starts/resumes sending data to it.

### Example 3: `rh.sinksocket` as a Server in a REDHAWK Waveform

A waveform's SAD file contains configuration information for its components.  The following SAD file snippet contains the same configuration as [Example 1](#example-1-rhsinksocket-as-a-server-in-the-redhawk-sandbox).  To use this in a SAD file, replace or insert the `<componentproperties>` section.

```xml
<componentinstantiation id="sinksocket_1">
  <componentproperties>
    <structsequenceref refid="Connections">
      <structvalue>
        <simpleref refid="Connection::connection_type" value="server"/>
        <simpleref refid="Connection::ip_address" value=""/>
        <simplesequenceref refid="Connection::byte_swap">
          <values>
            <value>0</value>
            <value>2</value>
          </values>
        </simplesequenceref>
        <simplesequenceref refid="Connection::ports">
          <values>
            <value>32191</value>
            <value>32192</value>
          </values>
        </simplesequenceref>
        <simplesequenceref refid="Connections::tcp_nodelays">
          <values>
            <value>false</value>
            <value>true</value>
          </values>
        </simplesequenceref>
      </structvalue>
    </structsequenceref>
  </componentproperties>
</componentinstantiation>
```

### Example 4: `rh.sinksocket` as a Client in a REDHAWK Waveform

A waveform's SAD file contains configuration information for its components.  The following SAD file snippet contains the same configuration as [Example 2](#example-2-rhsinksocket-as-a-client-in-the-redhawk-sandbox).  To use this in a SAD file, replace or insert the `<componentproperties>` section.

```xml
<componentinstantiation id="sinksocket_1">
  <componentproperties>
    <structsequenceref refid="Connections">
      <structvalue>
        <simpleref refid="Connection::connection_type" value="client"/>
        <simpleref refid="Connection::ip_address" value="127.0.0.1"/>
        <simplesequenceref refid="Connection::byte_swap">
          <values>
            <value>0</value>
            <value>2</value>
          </values>
        </simplesequenceref>
        <simplesequenceref refid="Connection::ports">
          <values>
            <value>32191</value>
            <value>32192</value>
          </values>
        </simplesequenceref>
        <simplesequenceref refid="Connections::tcp_nodelays">
          <values>
            <value>false</value>
            <value>true</value>
          </values>
        </simplesequenceref>
      </structvalue>
    </structsequenceref>
  </componentproperties>
</componentinstantiation>
```

