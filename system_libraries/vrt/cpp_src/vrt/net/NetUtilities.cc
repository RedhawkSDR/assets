/* ===================== COPYRIGHT NOTICE =====================
 * This file is protected by Copyright. Please refer to the COPYRIGHT file
 * distributed with this source distribution.
 *
 * This file is part of REDHAWK.
 *
 * REDHAWK is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * REDHAWK is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 * ============================================================
 */

#include "NetUtilities.h"
#include <errno.h>       // for errno
#include <stdlib.h>      // for atoi(..)
#include <unistd.h>      // for close(..) on OS X
//#include <arpa/inet.h>   // for inet_aton(..)
#include <net/if.h>      // see "man 7 netdevice"
#include <netinet/in.h>  // for IP options
#include <netinet/tcp.h> // for IP options
#include <sys/ioctl.h>   // for ioctl(..)
#include <sys/select.h>  // for select(..)
#include <sys/types.h>   // for select(..)
#include <sys/time.h>    // for select(..)
#include <sys/types.h>   // for getsockopt(..) / setsockopt(..)
#include <sys/socket.h>
#include <vector>

// The "struct ip_mreqn" was added to Linux 2.2 and is simply a copy of "struct ip_mreq"
// with an additional "imr_ifindex" field added to the end, it is not present under
// OS X, so we simply bypass the use of "imr_ifindex". It is likely "struct ip_mreqn"
// is missing under BSD too, but this is untested.
#if (defined(__APPLE__) && defined(__MACH__))
# define using_ip_mreq_for_ip_mreqn 1
# define ip_mreqn    ip_mreq
# define imr_address imr_multiaddr
#endif

using namespace std;
using namespace vrt;

#define MAX_DEVICE_COUNT 512 /** Used in createUDPSocket(..) */

/** Default SO_TIMEOUT in ms. We intentionally default to a non-zero value
 *  so that we never get stuck in a blocking call.
 */
static const int32_t DEF_SO_TIMEOUT   = 1000;

/** Default SO_REUSEADDR. We intentionally set this to true so the Java
 *  and C++ versions will match (Java tends to default SO_REUSEADDR to true
 *  despite the O/S setting being false as has been observed with Java 7 on
 *  RHEL 6.1). Additionally, we've always defaulted this to true with
 *  UDP/Multicast since that is almost always the correct choice.
 */
static const int32_t DEF_SO_REUSEADDR = 1;

/** Calls setsockopt(..) with return value check. */
static void _setsockopt (int sockfd, int level, int optname,
                         const void *optval, socklen_t optlen) {
  int status = setsockopt(sockfd, level, optname, optval, optlen);
  if (status < 0) {
    throw VRTException(errno);
  }
}

/** Calls setsockopt(..) with return value check. */
static void _setsockopt (int sockfd, int level, int optname, int val) {
  _setsockopt(sockfd, level, optname, &val, sizeof(val));
}

/** Used in the initSocket(..) functions. */
#define SETSOCKOPT_INT(level,optname,def) \
  if (NetUtilities::hasOpt(opts, #optname)) { \
    _setsockopt(sock, level, optname, NetUtilities::getSocketOptionInt(opts, #optname)); \
  } \
  else if (!isNull(def)) {\
    _setsockopt(sock, level, optname, def); \
  }

/** Used in the initSocket(..) functions. */
#define SETSOCKOPT_BOOL(level,optname,def) \
  if (NetUtilities::hasOpt(opts, #optname)) { \
    _setsockopt(sock, level, optname, NetUtilities::getSocketOptionBool(opts, #optname)); \
  } \
  else if (!isNull(def)) {\
    _setsockopt(sock, level, optname, def); \
  }

/** Calls ioctl(..) with return value check. */
static void _ioctl (int sockfd, int request, void *ptr) {
  int status = ioctl(sockfd, request, ptr);
  if (status < 0) {
    throw VRTException(errno);
  }
}


string vrt::NetUtilities::getNetworkDeviceName (const string &nic, int32_t vlan) {
  if (vlan < 0) return nic;

  if (nic.length() == 0) {
    throw VRTException("Can not specify VLAN of %d without specifying a NIC", vlan);
  }
  char dev[32];
  snprintf(dev, 32, "%s.%d", nic.c_str(), vlan);
  return dev;
}

InetAddress vrt::NetUtilities::getHostAddress (const string &hostPort) {
  string host = getHost(hostPort);
  return (isNull(host))? InetAddress() : InetAddress(host);
}

string vrt::NetUtilities::getHost (const string &hostPort) {
  if (hostPort.find("[") == 0) {
    // IPv6 address
    size_t i = hostPort.find("]");
    return hostPort.substr(0,i+1);
  }
  size_t i = hostPort.rfind(":");
  if (i == string::npos) return hostPort; // no port, just a host name
  if (i == 0           ) return "";     // no host, just a port number
  return hostPort.substr(0,i);
}

int16_t vrt::NetUtilities::getPort (const string &hostPort) {
  if (hostPort.find("[") == 0) {
    // IPv6 address
    size_t i = hostPort.find("]:");
    if (i == string::npos) return INT16_NULL; // no port, just a host name
    return (int16_t)atoi(hostPort.substr(i+2).c_str());
  }
  size_t i = hostPort.rfind(':');
  if (i == string::npos) return INT16_NULL; // no port, just a host name
  return (int16_t)atoi(hostPort.substr(i+1).c_str());
}

ifreq vrt::NetUtilities::toInterface (int sockfd, const string &device, bool isMCast) {
  ifconf devs;
  devs.ifc_len = MAX_DEVICE_COUNT * sizeof(struct ifreq);
  devs.ifc_buf = (char*)malloc(devs.ifc_len);
  if (devs.ifc_buf == NULL) {
    throw VRTException("Unable to allocate memory for list of network devices");
  }

  try {
    _ioctl(sockfd, SIOCGIFCONF, &devs);
    for (int32_t i = 0; i < MAX_DEVICE_COUNT; i++) {
      // On the next line we use "ifr_name" (which is usually a pre-processor
      // replacement for "ifr_ifrn.ifrn_name") since ifr_ifrn does not work with
      // OS X (or other BSD versions?).
      string devName = devs.ifc_req[i].ifr_name;
      if (isNull(device) || (device == devName)) {
        ifreq dev = devs.ifc_req[i];

        // ==== SANITY CHECKS ==================================================
        _ioctl(sockfd, SIOCGIFFLAGS, &dev);

        if (!(dev.ifr_flags & IFF_UP)) {
          if (isNull(device)) continue; // go on to next
          throw VRTException("Network device %s is not up", devName.c_str());
        }
        if (isMCast && ((dev.ifr_flags & IFF_LOOPBACK) || !(dev.ifr_flags & IFF_MULTICAST))) {
          if (isNull(device)) continue; // go on to next
          throw VRTException("Network device %s does not support UDP/Multicast", devName.c_str());
        }

        // ==== FILL IN DEVICE INDEX ===========================================
        //   OS X does not support SIOCGIFINDEX (see "man 7 netdevice" on Linux),
        //   rather it provides if_nametoindex(..) (see "man 3 if_nametoindex" on
        //   OS X). Although if_nametoindex(..) is available on a number of Linux
        //   distributions, it is not used here due to performance issues (at one
        //   point if_nametoindex(..) had very high overhead on Linux/BSD) and
        //   possible compatibility issues (read this last reason as "don't break
        //   what we know has worked for years"). It also appears that SIOCGIFINDEX
        //   isn't available on some some versions of BSD, so hopefully this will
        //   work there too.
        //
        //   Update... for now omit the else clause that uses if_nametoindex(..)
        //   since its intended use (see createUDPSocket(..) below) isn't applicable
        //   on OS X (or other BSD versions?).
#ifdef SIOCGIFINDEX
        if (ioctl(sockfd, SIOCGIFINDEX, &dev) != 0) {
          throw VRTException("Can not get device index for network device %s", device.c_str());
        }
#endif
        // ==== RETURN DEVICE ==================================================
        safe_free(devs.ifc_buf);
        return dev;
      }
    }

    // If we get here, we've failed
    throw VRTException("Could not find network device '%s'", device.c_str());
  }
  catch (VRTException e) {
    safe_free(devs.ifc_buf);
    throw e;
  }
}

map<string,string> vrt::NetUtilities::getDefaultSocketOptions (bool optimize, TransportProtocol transport) {
  UNUSED_VARIABLE(transport);
  map<string,string> options;

  if (optimize) {
    options["RCVBUF_EAGER"] = "true";
    options["TCP_NODELAY" ] = "true";
    options["VRL_CRC"     ] = "false";
  }
  else {
    // Use o/s default for RCVBUF_EAGER
    // Use o/s default for TCP_NODELAY
    options["VRL_CRC"     ] = "false";
  }

  return options;
}

static void parseHeader (const vector<string> &hdr, map<string,string> &header) {
  size_t pos;
  string str;
  if (hdr.size() < 1) {
    throw VRTException("Invalid HTTP response");
  }

  str = hdr[0];
  pos = str.find(" ");     if (pos == string::npos) throw VRTException("Invalid HTTP response (no version)");
  header["version"] = str.substr(0,pos);
  str = str.substr(pos+1);
  pos = str.find(" ");     if (pos == string::npos) throw VRTException("Invalid HTTP response (no status-code)");
  header["status-code"   ] = str.substr(0,pos);
  header["status-message"] = str.substr(pos+1);

  for (size_t i = 1; i < hdr.size(); i++) {
    str = hdr[i];
    pos = str.find(":");   if (pos == string::npos) throw VRTException("Invalid HTTP response (bad header line)");

    string key = str.substr(0,pos);
    string val = str.substr(pos+1);

    header[Utilities::toUpperCase(Utilities::trim(key))] = Utilities::trim(val);
  }
}

void NetUtilities::doHttpGet (const string &url, map<string,string> &header, string &data) {
  if (url.find("http://") != 0) {
    throw VRTException("Invalid URL '%s'.", url.c_str());
  }
  string _url = url.substr(7);
  string hostPort;
  string path;


  size_t slash = _url.find("/",7);
  if (slash != string::npos) {
    hostPort = _url.substr(0,slash);
    path     = _url.substr(slash);
  }
  else {
    hostPort = _url;
    path     = "/";
  }

  string  host = getHost(hostPort);
  int32_t port = getPort(hostPort);
  string  dev  = "";
  string  req  = "";

  req += "GET "+path+" HTTP/1.1\r\n";
  req += "Accept: */*\r\n";
  req += "Accept-Language: en-us\r\n";
  req += "Host: "+host+"\r\n";
  req += "Connection: Keep-Alive\r\n";
  req += "\r\n";
  req += "\r\n";


  map<string,string> options = getDefaultSocketOptions(false, TransportProtocol_TCP);
  options["TCP_NODELAY"] = "false";
  options["SO_TIMEOUT" ] = "100";

  Socket *socket = Socket::createTCPSocket(false, host, port, dev, options, 4096);
  socket->write(req.c_str(), req.size());

  char           buf[1024];
  string         hdrLine     = "";  // header text
  vector<string> hdrLines;
  int32_t        contentLen  = -1; // -1=In Header, -2=Unknown
  int32_t        contentRead = 0;
  int32_t        numRead     = socket->read(buf, 1024);

  header.clear();
  data = "";
  while ((numRead >= 0) && (contentLen != contentRead)) {
    for (int32_t i = 0; i < numRead; i++) {
      char c = buf[i];

      if (contentLen == -1) {
        hdrLine += c;

        size_t eol = hdrLine.find("\r\n");
        if (eol != string::npos) { // now at start of data
          if (eol == 0) {
            if (hdrLines.size() < 1) {
              throw VRTException("Invalid HTTP response (no header)");
            }
            parseHeader(hdrLines, header);
            contentLen = (header.count("CONTENT-LENGTH"))? atoi(header["CONTENT-LENGTH"].c_str()) : -2;
          }
          else {
            hdrLines.push_back(hdrLine.substr(0,eol));
            hdrLine = "";
          }
        }
      }
      else {
        contentRead++;
        if ((contentLen >= 0) && (contentRead > contentLen)) {
          throw VRTException("Content exceeds specified content-length");
        }
        data += c;
      }
    }

    numRead = socket->read(buf, 1024);
  }
  socket->close();
  safe_delete(socket);
}

Socket::Socket (TransportProtocol transport) :
  transport(transport),
  sock(-1),
  mtu(-1),
  soTimeout(0),
  shutdownRW(false)
{
  if (true) {
    addr.sin_family      = AF_INET;
    addr.sin_port        = 0;
    addr.sin_addr.s_addr = INADDR_ANY;
  }
  else {
    throw VRTException("IPv6 not supported yet");
  }
}

Socket::Socket (TransportProtocol transport, const string &host, int32_t port) :
  transport(transport),
  sock(-1),
  mtu(-1),
  soTimeout(0),
  shutdownRW(false)
{
  int         domain; // AF_INET or AF_INET6
  InetAddress ipAddr = (isNull(host))? InetAddress::getLocalHost() : InetAddress(host);

  if (ipAddr.isIPv4()) {
    addr.sin_family = AF_INET;
    addr.sin_port   = htons((int16_t)port);
    addr.sin_addr   = ipAddr.toIPv4();
    domain          = AF_INET;
  }
  else {
    domain          = AF_INET6;
    throw VRTException("IPv6 not supported yet");
  }

  switch (transport) {
    case TransportProtocol_UDP:        sock = ::socket(domain, SOCK_DGRAM,  IPPROTO_UDP); break;
    case TransportProtocol_TCP_SERVER: sock = ::socket(domain, SOCK_STREAM, IPPROTO_TCP); break;
    case TransportProtocol_TCP_CLIENT: sock = ::socket(domain, SOCK_STREAM, IPPROTO_TCP); break;
    default: throw VRTException("Unexpected TransportProtocol %d", transport);
  }
  if (sock < 0) {
    throw VRTException(errno);
  }
}

Socket::~Socket () {
  close();
}


string Socket::toString () const {
  if (isNullValue()) return getClassName()+": <null>";

  string _transport;
  switch (transport) {
    case TransportProtocol_UDP:        _transport = (isMulticast())? "UDP/Multicast"
                                                                   : "UDP"; break;
    case TransportProtocol_TCP_SERVER: _transport = "TCP_SERVER"; break;
    case TransportProtocol_TCP_CLIENT: _transport = "TCP_CLIENT"; break;
    default:                           _transport = "ERROR";      break;
  }

  ostringstream str;
  str << getClassName() << ": " << _transport << " on "
                        << getInetAddress().toString() << ":" << getPort()
                        << " (sockfd=" << sock << " mtu=" << getMTU() << ")";
  return str.str();
}

void Socket::shutdownSocket () {
  SYNCHRONIZED(this);
  shutdownRW = true;
  if (isOpen()) {
    if (shutdown(sock, SHUT_RDWR) != 0) {
      throw VRTException(errno);
    }
  }
}

void Socket::close () {
  if (!shutdownRW && (transport == TransportProtocol_TCP_CLIENT)) {
    // Force shutdown for TCP client sockets
    shutdownSocket();
  }

  SYNCHRONIZED(this);
  if (isOpen()) {
#ifdef _WIN32
    // TODO: On Windows this would need to be closesocket(..), however we just
    // error-out for now since we haven't tested on Windows yet!
# error "Need to use closesocket(..) on Windows"
#else
    if (::close(sock) != 0) {
      throw VRTException(errno);
    }
#endif
    sock = -1;
  }
}

bool Socket::isSocketReady () {
  if (soTimeout <= 0) return true;
  if (shutdownRW    ) return true; // force flushing of buffers without waiting
  if (!isOpen()     ) return true; // illegal to call select(..) in this state

  fd_set readset;
  FD_ZERO(&readset);
  FD_SET(sock, &readset);
  int nfds = sock + 1;

  struct timeval tv;
  tv.tv_sec  = (soTimeout / 1000);
  tv.tv_usec = (soTimeout % 1000) * 1000;

  int ok = select(nfds, &readset, NULL, NULL, &tv); // <-- may modify tv
  if (ok < 0) {
    throw VRTException(errno);
  }
  return (ok != 0);
}

int32_t Socket::read (void *buf, int32_t len) {
  // We intentionally use send(..) and recv(..) rather than write(..) and read(..)
  // since some platforms (i.e. Windows) restrict the later to files.
  if (transport == TransportProtocol_TCP_SERVER) {
    throw VRTException("Socket recv(..) not supported for TCP_SERVER");
  }

  if (!isSocketReady()) {
    return 0; // socket timeout
  }
  int32_t numRead = (int32_t)::recv(sock, buf, len, 0);

  if (numRead > 0) {
    return numRead;
  }
  else if (numRead == 0) {
    return (shutdownRW)? -3 : -1; // socket closed
  }
  else if ((errno == EAGAIN) || (errno == EWOULDBLOCK)) {
    return 0; // socket timeout
  }
  else {
    throw VRTException(errno);
  }
}

int32_t Socket::write (const void *buf, int32_t len) {
  // We intentionally use send(..) and recv(..) rather than write(..) and read(..)
  // since some platforms (i.e. Windows) restrict the later to files.
  if (transport == TransportProtocol_TCP_SERVER) {
    throw VRTException("Socket send(..) not supported for TCP_SERVER");
  }
  if (shutdownRW) {
    return -3; // not accepting any additional writes
  }

  if (len == 0) return 0; // useless write

  int32_t numWritten = 0;
  if (transport == TransportProtocol_UDP) {
    numWritten = (int32_t)sendto(sock, buf, len, 0, (struct sockaddr*)&addr, sizeof(addr));
  }
  else {
    // Need a loop here so we force all data to be written even if we can't
    // write it all in one go (i.e. because the TCP output buffer is full).
    const char *bbuf = (const char*)buf;
    while (numWritten < len) {
      int32_t n = (int32_t)send(sock, &bbuf[numWritten], len - numWritten, 0);
      if (n <= 0) break; // EOF or error
      numWritten += n;
    }
  }

  if (numWritten > 0) {
    return numWritten;
  }
  else {
    throw VRTException(errno);
  }
}

Socket* Socket::accept (map<string,string> &options) {
  if (transport != TransportProtocol_TCP_SERVER) {
    throw VRTException("Socket accept() only supported for TCP_SERVER");
  }
  if (shutdownRW) {
    return NULL; // not accepting any additional connections
  }
  if (!isSocketReady()) {
    return NULL; // socket timeout
  }

  Socket *s = NULL;
  socklen_t size = sizeof(s->addr);

  try {
    s = new Socket(TransportProtocol_TCP_CLIENT);
    s->sock = ::accept(sock, (struct sockaddr*)&(s->addr), &size);
    if (s->sock < 0) {
      safe_delete(s);
      if ((errno == EAGAIN) || (errno == EWOULDBLOCK)) {
        return NULL; // socket timeout
      }
      throw VRTException(errno);
    }
    if (size != sizeof(s->addr)) {
      safe_delete(s);
      throw VRTException("Invalid socket address size for new TCP client connection.");
    }
    return s->initSocket(options);
  }
  catch (VRTException e) {
    safe_delete(s);
    throw e;
  }
}

Socket* Socket::createUDPSocket (bool isOutput, const string &host, int32_t port,
                                 const string &device, map<string,string> &options,
                                 int32_t bufLength) {
  // ==== CREATE SOCKET OBJECT =================================================
  Socket *socket = new Socket(TransportProtocol_UDP, host, port);

  // ==== INIT SOCKET OBJECT / OPEN SOCKET======================================
  try {
    // ==== FIND DEVICE ========================================================
    bool  isMCast  = socket->isMulticast();
    ifreq dev      = NetUtilities::toInterface(socket->sock, device, isMCast);

    // ==== FIND MTU FOR DEVICE ================================================
#ifdef SIOCGIFMTU
    if (ioctl(socket->sock, SIOCGIFMTU, &dev) != 0) {
      socket->mtu = -3; // failed
    }
    socket->mtu = dev.ifr_mtu;
#else
    socket->mtu = -2; // not available
#endif

    // ==== CONFIGURE SOCKET ===================================================
    struct in_addr localInterface = ((struct sockaddr_in*)(&dev.ifr_addr))->sin_addr;
    struct ip_mreq mcastGroup;
    memset(&mcastGroup, 0, sizeof(mcastGroup));
    mcastGroup.imr_multiaddr = socket->getInetAddress().toIPv4();
    mcastGroup.imr_interface = localInterface;

    if (isMCast) {
      if (isOutput) {
        // Setting IP_MULTICAST_IF seems to always result in a "Cannot assign
        // requested address" error in this scenario
      }
      else {
        _setsockopt(socket->sock, IPPROTO_IP, IP_MULTICAST_IF, &localInterface, sizeof(localInterface));
      }
    }

    if (isOutput) {
      // n/a
    }
    else {  // only bind if we are reading input
      int status = ::bind(socket->sock, (struct sockaddr*)&socket->addr, sizeof(socket->addr));
      if (status != 0) {
        throw VRTException(errno);
      }
      if (isMCast) {
        _setsockopt(socket->sock, IPPROTO_IP, IP_ADD_MEMBERSHIP, &mcastGroup, sizeof(mcastGroup));
      }
    }

    if (isOutput) socket->initSocket(options, INT32_NULL, bufLength+NetUtilities::EXTRA_BUFFER_SIZE);
    else          socket->initSocket(options, bufLength+NetUtilities::EXTRA_BUFFER_SIZE, INT32_NULL);

    if (!isOutput && NetUtilities::getSocketOptionBool(options, "RCVBUF_EAGER", false)) {
      // Attempt to force "eager" initialization of the receive buffer (see
      // description for RCVBUF_EAGER at top of class) via an early call to
      // receive (which will frequently time out). Note that we have observed
      // that (at least in Java) a timeout that is too small (e.g. 1 ms) can
      // let the o/s return on a time-out prior to allocating the buffer.
      int32_t timeout = socket->soTimeout;
      socket->soTimeout = 100;
      char buf[NetUtilities::MAX_UDP_LEN];
      socket->read(buf, NetUtilities::MAX_UDP_LEN);
      socket->soTimeout = timeout;
    }

    return socket;
  }
  catch (VRTException e) {
    safe_delete(socket);
    throw e;
  }
}

Socket* Socket::createTCPServerSocket (bool isOutput, const string &host, int32_t port,
                                       const string &device, map<string,string> &options,
                                       int32_t bufLength) {
  UNUSED_VARIABLE(device); // TODO: Allow device selection
  Socket  *socket  = NULL;
  int32_t  backlog = NetUtilities::getSocketOptionInt(options, "SERVER_BACKLOG", 50);

  try {
    socket = new Socket(TransportProtocol_TCP_SERVER, host, port);

    if (isOutput) socket->initSocket(options, INT32_NULL, bufLength+NetUtilities::EXTRA_BUFFER_SIZE);
    else          socket->initSocket(options, bufLength+NetUtilities::EXTRA_BUFFER_SIZE, INT32_NULL);

    int status = ::bind(socket->sock, (struct sockaddr*)&socket->addr, sizeof(socket->addr));
    if (status != 0) {
      throw VRTException(errno);
    }

    status = ::listen(socket->sock, backlog);
    if (status != 0) {
      throw VRTException(errno);
    }

    return socket;
  }
  catch (VRTException e) {
    safe_delete(socket);
    throw e;
  }
}

Socket* Socket::createTCPSocket (bool isOutput, const string &host, int32_t port,
                                 const string &device, map<string,string> &options,
                                 int32_t bufLength) {
  UNUSED_VARIABLE(device); // TODO: Allow device selection
  Socket *socket = NULL;

  try {
    socket = new Socket(TransportProtocol_TCP_CLIENT, host, port);
    socket->initSocket(options);

    if (isOutput) socket->initSocket(options, INT32_NULL, bufLength+NetUtilities::EXTRA_BUFFER_SIZE);
    else          socket->initSocket(options, bufLength+NetUtilities::EXTRA_BUFFER_SIZE, INT32_NULL);

    int status = ::connect(socket->sock, (sockaddr*)&(socket->addr), sizeof(socket->addr));
    if (status != 0) {
      throw VRTException(errno);
    }
    return socket;
  }
  catch (VRTException e) {
    safe_delete(socket);
    throw e;
  }
}

Socket* Socket::initSocket (map<string,string> &opts, int32_t recvBuf, int32_t sendBuf) {
  switch (transport) {
    case TransportProtocol_UDP:
      // Not supported: IP_MULTICAST_IF
      if (isMulticast()) {
        SETSOCKOPT_INT( IPPROTO_IP, IP_MULTICAST_LOOP, INT32_NULL);
        SETSOCKOPT_BOOL(IPPROTO_IP, IP_MULTICAST_TTL,  INT32_NULL);
      }
      SETSOCKOPT_INT( IPPROTO_IP,  IP_TOS,       INT32_NULL);
      SETSOCKOPT_BOOL(SOL_SOCKET,  SO_BROADCAST, INT32_NULL);
      SETSOCKOPT_INT( SOL_SOCKET,  SO_RCVBUF,    recvBuf);
      SETSOCKOPT_INT( SOL_SOCKET,  SO_SNDBUF,    sendBuf);
      SETSOCKOPT_BOOL(SOL_SOCKET,  SO_REUSEADDR, DEF_SO_REUSEADDR);
      break;
    case TransportProtocol_TCP_SERVER:
      SETSOCKOPT_INT( SOL_SOCKET,  SO_RCVBUF,    INT32_NULL);
      SETSOCKOPT_BOOL(SOL_SOCKET,  SO_REUSEADDR, DEF_SO_REUSEADDR);
      break;
    case TransportProtocol_TCP_CLIENT:
      SETSOCKOPT_INT( IPPROTO_IP,  IP_TOS,       INT32_NULL);
      SETSOCKOPT_INT( SOL_SOCKET,  SO_KEEPALIVE, INT32_NULL);
      SETSOCKOPT_INT( IPPROTO_TCP, TCP_NODELAY,  INT32_NULL);
      SETSOCKOPT_INT( SOL_SOCKET,  SO_SNDBUF,    INT32_NULL);
      SETSOCKOPT_INT( SOL_SOCKET,  SO_RCVBUF,    INT32_NULL);
      SETSOCKOPT_BOOL(SOL_SOCKET,  SO_REUSEADDR, DEF_SO_REUSEADDR);
      SETSOCKOPT_BOOL(SOL_SOCKET,  SO_LINGER,    INT32_NULL);
      break;
    default:
      throw VRTException("Unexpected TransportProtocol %d", transport);
  }

  // The following "socket option" (which applies to all of the protocol types)
  // requires special handling, so it is just stored internal to the socket
  // object. We keep the Java "SO_TIMEOUT" name since it is convenient and
  // since any web search on it will get hits that are relevant.
  soTimeout = NetUtilities::getSocketOptionInt(opts, "SO_TIMEOUT", DEF_SO_TIMEOUT);

  return this;
}
