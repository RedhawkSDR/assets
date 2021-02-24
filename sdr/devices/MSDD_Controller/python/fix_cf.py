
from ossie.properties import PropertyStorage
from ossie.properties import _property
import copy

'''
This module applies a patch the redhawk framework libraries to fix the following issue:

 - Create copies of property objects when starting up multiple resources in the same process space.
 - Resource shutdown - For resources with parent child relationships, the child resources will
   be shutdown when a releaseObject request is made to the parent.
'''

def localLoadProperties(self):
    for name in dir(type(self._PropertyStorage__resource)):
        attr = getattr(type(self._PropertyStorage__resource), name)
        if isinstance(attr, _property):
            self._addProperty(copy.deepcopy(attr))

PropertyStorage._loadProperties=localLoadProperties
import traceback
from ossie.device import Device
from ossie.cf import CF
def localDeviceReleaseObject(self):
    self._deviceLog.debug("(localDevice) releaseObject()")
    if self._adminState == CF.Device.UNLOCKED:
        self._adminState = CF.Device.SHUTTING_DOWN

    skipShutdown=False
    try:
        # release all of the child devices
        # if they have included the AggregateDeviceMixIn
        try:
            childDevice = self._childDevices
        except AttributeError:
            pass
        else:
            while len(self._childDevices)>0:
                child = self._childDevices.pop()
                child.releaseObject()
        # remove device from parent and set compositeDevice to None
        if self._compositeDevice:
            skipShutdown=True
            self._compositeDevice.removeDevice(self._this())
            self._compositeDevice = None

        self._unregister()

    except Exception as e:
        raise CF.LifeCycle.ReleaseError(str(e))

    self._adminState = CF.Device.LOCKED
    try:
        self._cmdLock.release()
    except:
        pass
    try:
        if not skipShutdown:
            import ossie.resource as resource
            self._resourceLog.debug("(localDevice) Shutdown the ORB!!!")
            resource.Resource.releaseObject(self)
        else:
            import ossie.logger
            from  ossie.events import  Manager
            self._resourceLog.debug("(localDevice) releaseObject()")
            self.stopPropertyChangeMonitor()
            # disable logging that uses EventChannels
            ossie.logger.SetEventChannelManager(None)
            # release all event channels
            if self._ecm: ossie.events.Manager.Terminate()
            objid = self._default_POA().servant_to_id(self)
            self._default_POA().deactivate_object(objid)
    except:
        self._deviceLog.error("(localDevice) failed releaseObject()")
Device.releaseObject=localDeviceReleaseObject
