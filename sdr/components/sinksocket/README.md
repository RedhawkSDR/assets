# REDHAWK sinksocket
 
## Description

The `rh.sinksocket` component reads data from BULKIO ports and writes it to TCP connections.  The data from all ports goes to each of the connections.  Each `rh.sinksocket` component can operate multiple TCP connections; each of which can be either a TCP client or server.

The tables in the Properties section provide some details about how you can configure the component, and about how you can monitor its data thruput.  The component can be configured regarding:

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
| `Connections` | `[Connection]` | read/write | A list of data structures, each defining a `rh.sinksocket` Connection. |
| `ConnectionStats` | `[ConnectionStat]` | readonly | A list of data structures, each with information about a TCP connection. |


The following table describes the properties within a single Connection.  If a `rh.sinksocket` is created without properties specified by the user, a single Connection will be created, with the default values given in the table.

| **NAME** | **TYPE** | **DEFAULT** | **DESCRIPTION** |
| :--------| :--------| :-----------| :---------------|
| `connection_type` | `string` | `server` | Enumerated values:  `client`, `server`.<br/>Set whether `rh.sinksocket` is a TCP client or server. |
| `ip_address` | `string` | `""` | If this Connection is a TCP client, set the ip address to which it will connect.<br/>If this Connection is a TCP server, this value is ignored. |
| `ports` | `[ushort]` | `[32191]` | If this Connection is a TCP client, set the list of port numbers to which `rh.sinksocket` will connect.<br/>If this Connection is a TCP server, set the list of port numbers  on which `rh.sinksocket` will listen. |
| `tcp_nodelays` | `[boolean]` | `[false]` | If `tcp_nodelay` is `true`, prevent the TCP connection from combining packets with Nagle's algorithm. |
| `byte_swap` | `[ushort]` | `[0]` | Reorder bytes of integer values between little-endian and big-endian representation.<br/>Values:<br/> - `0`: no byte swapping<br/> - `1`: byte swap according to the data size for each port<br/> - `<num>`: swap bytes as if the data type length was `<num>` |

A `rh.sinksocket` Connection contains a set of TCP connections.  Each TCP connection corresponds to a TCP port number.  These numbers are listed in the `ports` list.  The `byte_swap` and `tcp_nodelays` lists relate to the `ports` list like associative arrays.  That is, the first element of each corresponds to the same TCP connection.  The same is true for the nth element of each list.


The following table describes the properties within a single ConnectionStat.  Each ConnectionStat contains information about one of the TCP connections within a `rh.sinksocket` Connection.

| **NAME** | **TYPE** | **DESCRIPTION** |
| :--------| :------- | :---------------|
| `ip_address` | `string` | The IP address of the Connection. If the Connection is type `server`, this value will be blank. |
| `port` | `ushort` | The TCP port of the connection. |
| `status` | `string` | The status of the connection.<br/>Enumerated values:  `startup`, `not_connected`, `connected`, `error`. |
| `bytes_per_second` | `float` | Number of bytes sent over the network by the connection. |
| `bytes_sent` | `double` | Bytes-per-second, for data sent over the network, for the connection. |


## Installation Instructions

For instructions on how to install the REDHAWK basic assets from RPMs, see the REDHAWK manual.

To install from source, continue with the following steps.

### Step 1:  Prerequisites

Ensure these prerequisites are satisfied:

* `OSSIEHOME` is set
* `SDRROOT` is set

For a standard install, this can be done as follows:
```sh
$ cd /etc/profile.d
$ . redhawk.sh
$ . redhawk-sdrroot.sh
```

### Step 2:  Build and Install

This creates the `rh.sinksocket` executable in the `cpp` subdirectory, then installs the component to `SDRROOT`:
```
$ cd /path/to/redhawk/assets
$ cd $(find . -name sinksocket)
$ ./build.sh install
```

## Asset Use

This section contains examples of how to use the component.

* [Example 1](#example-1-rhsinksocket-as-a-server-in-the-redhawk-sandbox): `rh.sinksocket` as a server in the REDHAWK sandbox
* [Example 2](#example-2-rhsinksocket-as-a-client-in-the-redhawk-sandbox): `rh.sinksocket` as a client in the REDHAWK sandbox
* [Example 3](#example-3-rhsinksocket-as-a-server-in-a-redhawk-waveform): `rh.sinksocket` as a server in a REDHAWK waveform
* [Example 4](#example-4-rhsinksocket-as-a-client-in-a-redhawk-waveform): `rh.sinksocket` as a client in a REDHAWK waveform

All of the examples configure the component to use two ports.  One port does not swap byte endianness, while the other swaps bytes for a data word size of 2.  The first port allows packet combining with Nagle's algorithm, while the second does not.  However, packet combining will not be observable in these examples.

For the sandbox examples, have a version of `netcat` installed.

### Example 1: `rh.sinksocket` as a server in the REDHAWK sandbox

#### Instructions to Run the Example

Save this python code to a file name `demo.py`.
```python
#!/usr/bin/env python

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

# Display information supported by rh.sinksocket.
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

Terminal 3 should look like terminal 2, but the order of the characters should be `'badc..'`.  The first and second characters are swapped with each other.  So are the third and fourth, etc.  This happened because of the `byte_swap` property setting of `2`.

The last parts of the code and the output show how to get information about the connections.

### Example 2: `rh.sinksocket` as a client in the REDHAWK sandbox

This is similar to Example 1, except that the component is a TCP client.  To run the example, make these changes:

* in `demo.py`, replace `'server'` with `'client'`
* add a line below that with `'ip_address': '127.0.0.1',`
* in terminals 2 and 3, add the `-l` flag after `nc`

Then, run this example in the same way as Example 1.

The order of operations is less important in this case.  If `rh.sinksocket` is configured to expect a TCP server, and that server is unavailable, `rh.sinksocket` skips it.  When the server comes online, `rh.sinksocket` will automatically connect and start/resume sending data to it.

### Example 3: `rhsinksocket` as a server in a REDHAWK waveform

You can replace xml code in a waveform's SAD file to configure its components. The following xml code creates the same component configuration as Example 1.

```xml
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
```

### Example 4: `rhsinksocket` as a client in a REDHAWK waveform

You can replace xml code in a waveform's SAD file to configure its components. The following xml code creates the same component configuration as Example 2.

```xml
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
```

## Branches and Tags

This content is currently under development.

## REDHAWK 2.2 Version Compatibility

These versions of `rh.sinksocket` are compatible with REDHAWK 2.2:

* 2.0.x

## Copyright

This work is protected by Copyright. Please refer to the [Copyright File](COPYRIGHT) for updated copyright information.

## License

REDHAWK rh.sinksocket is licensed under the GNU Lesser General Public License (LGPL).
