#
# This file is protected by Copyright. Please refer to the COPYRIGHT file
# distributed with this source distribution.
#
# This file is part of REDHAWK FmRdsSimulator.
#
# REDHAWK FmRdsSimulator is free software: you can redistribute it and/or modify it under
# the terms of the GNU General Public License as published by the Free
# Software Foundation, either version 3 of the License, or (at your option) any
# later version.
#
# REDHAWK FmRdsSimulator is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
# FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
# details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see http://www.gnu.org/licenses/.
#
ossieName = rh.FmRdsSimulator
bindir = $(prefix)/dev/devices/rh/FmRdsSimulator/cpp/
bin_PROGRAMS = FmRdsSimulator

xmldir = $(prefix)/dev/devices/rh/FmRdsSimulator/
dist_xml_DATA = ../FmRdsSimulator.scd.xml ../FmRdsSimulator.prf.xml ../FmRdsSimulator.spd.xml
ACLOCAL_AMFLAGS = -I m4 -I${OSSIEHOME}/share/aclocal/ossie
AUTOMAKE_OPTIONS = subdir-objects


confdir = $(prefix)/dev/devices/rh/FmRdsSimulator/
dist_conf_SCRIPTS = ../nodeconfig.py

distclean-local:
	rm -rf m4
	rm -f config.*
	rm -rf autom4te.cache
	rm -f acinclude.m4
	rm -f aclocal.m4
	rm -f configure
	rm -f depcomp
	rm -f install-sh
	rm -f ltmain.sh
	rm -f Makefile.in
	rm -f missing
	rm -rf .deps


# Sources, libraries and library directories are auto-included from a file
# generated by the REDHAWK IDE. You can remove/modify the following lines if
# you wish to manually control these options.
include $(srcdir)/Makefile.am.ide
FmRdsSimulator_SOURCES = $(redhawk_SOURCES_auto)
FmRdsSimulator_LDADD = $(SOFTPKG_LIBS) $(PROJECTDEPS_LIBS) $(BOOST_LDFLAGS) $(BOOST_THREAD_LIB) $(BOOST_REGEX_LIB) $(BOOST_SYSTEM_LIB) $(INTERFACEDEPS_LIBS) $(redhawk_LDADD_auto)
FmRdsSimulator_CXXFLAGS = -Wall $(SOFTPKG_CFLAGS) $(PROJECTDEPS_CFLAGS) $(BOOST_CPPFLAGS) $(INTERFACEDEPS_CFLAGS) $(redhawk_INCLUDES_auto)
FmRdsSimulator_LDFLAGS = -Wall $(redhawk_LDFLAGS_auto)

