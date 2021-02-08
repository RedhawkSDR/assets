import sys
import os
import traceback
try:
    mload=False
    for  path in [ '../python', os.getenv('SDRROOT')+'/dev/devices/rh/MSDD/python', './MSDD/python', '../MSDD/python'  ]:
        try:
            sys.path+=[path]
            from msddcontroller import *
            mload=True
            break
        except:
            continue

    if not mload:
        raise RuntimeError

except RuntimeError:
    import thread
    import socket
    import re


    class CommandException(Exception):
        pass

    class InvalidValue(Exception):
        pass


    class Connection(object):
        """Class to actually communicate with the radio, NB there should only be one of these per radio"""

        def __init__(self, address, timeout=0.25):
            """
            Attributes:
            ----------
            _debug : turn on print statements
            __mutexLock : synchronize access to the device
            radioAddress : (ip, port) tuple
            radioSocket : socket to write and read messages

            Parameters:
            ----------
            address: tuple (ip, port)
            timeout: default time in seconds to wait when receiving a messagec
            """
            self._debug=False
            self.__mutexLock = thread.allocate_lock()
            self.radioAddress = address
            self.radioSocket=None
            self.connect(timeout)

        def connect(self, timeout):
            """
            Open a UDP socket connection for communicating to the radio. If the socket was
            open then close and reset.

            Parameters:
            ----------
            timeout : timeout in seconds when receiving messages from the radio

            """
            self.timeout=timeout
            if self.radioSocket:
                self.radioSocket.close()
                self.radioSocket = None

            #generate socket for sending/receiving UDP packets to/from radio
            try:
                self.__mutexLock.acquire()
                self.radioSocket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
            except:
                self.radioSocket=None
            finally:
                self.__mutexLock.release()


        def _reconnect(self):
            """
            Perform close/open operation on the socket, does not lock access
            """
            if self.radioSocket:
                self.radioSocket.close()
                self.radioSocket = None

            #generate socket for sending/receiving UDP packets to/from radio
            try:
                self.radioSocket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
            except:
                self.radioSocket=None


        def disconnect(self):
            """
            Close the socket connection to the radio.

            """
            try:
                self.__mutexLock.acquire()
                if self.radioSocket:
                    self.radioSocket.close()
                    self.radioSocket = None
            finally:
                self.radioSocket=None
                self.__mutexLock.release()

        def set_timeout(self, timeout):
            """
            Sets the timeout attribute

            Parameters:
            -----------
            timeout - new timeout value
            """
            self.__mutexLock.acquire()
            self.timeout=timeout
            self.radioSocket.settimeout(timeout)
            self.__mutexLock.release()

        def flush(self, retries=None):
            self.__mutexLock.acquire()
            try:
                self._flush(retries)
            finally:
                self.__mutexLock.release()
                return

        def _flush(self, retries=None):
            if retries is None: retries=1
            self.radioSocket.sendto("\n", self.radioAddress)
            while retries != 0:
                self.radioSocket.settimeout(self.timeout/4.0)
                self.radioSocket.recv(65535)
                retries-=1


        def sendStringCommand(self, command, check_for_output=True, expect_output=True):
            """Sends command to the radio and process response

            Parameters:
            -----------
            command : command string to send to radio
            check_for_output : check for output after command echo is processed
            expect_output : when checking for output that we expected reply message
                            if not, then raise CommandException

            Returns:
            --------
            resp : response string from radio
            """

            try:
                self.__mutexLock.acquire()

                self.radioSocket.settimeout(self.timeout)
                _cmd=command.replace('\n',' ')
                if self._debug:
                    print "Sending to radio:", self.radioAddress, " command <"+ _cmd +">"

                # set exception message
                except_msg="Socket timed out receiving echo from radio for command: " + _cmd

                self.radioSocket.sendto(command, self.radioAddress)

                #The following lines were added to avoid a condition where the radio will reset back to turning Echo On and messing up
                # It has to do with additional clients hitting the radio externally, which re-use connections (0-15) on the radio
                # When the current connection is the last on the list, it gets reallocated to the new connection, and the device gets
                # a new connection when it tries to send data, but the echo is turned back on on the new socket
                _expect_output=True
                echoMsg = self.radioSocket.recv(65535)
                if self._debug:
                    resp = re.sub(r'[^\x00-\x7F]+',' ', str(echoMsg))
                    resp = resp.replace('\n',' ')
                    print "Echo from radio <", resp, "> (check for output=", check_for_output, ")"

                # early return, do not look for error conditions or response messages
                if not check_for_output:
                    return ""

                # setup for exceptions when processing response messages
                _expect_output = expect_output
                except_msg = "Socket timed out when reading results from command: " + _cmd
                # if we don't expect anything then no need to wait the entire time..
                if not expect_output: self.radioSocket.settimeout(self.timeout/4.0)
                returnMsg = self.radioSocket.recv(65535)

                # clean up return message
                returnMsg = re.sub(r'[^\x00-\x7F]+',' ', str(returnMsg))
                returnMsg = returnMsg.replace('\n',' ').rstrip()
                if self._debug:
                    print "Response from command : " + returnMsg, " (check for output:", check_for_output, ")"

                # check for unexpected output
                if not expect_output and len(returnMsg) > 0:
                    err_str =  "Unexpected output received \"" + returnMsg + "\" from command: " + _cmd
                    if self._debug:
                        print err_str
                    self._flush()
                    raise CommandException(err_str)

                return returnMsg

            except socket.timeout:
                if _expect_output:
                    if self._debug:
                        print "Socket timed out when expecting a return message, radio may not be responsive"
                    self._reconnect()
                    raise Exception(except_msg)
            finally:
                self.__mutexLock.release()
