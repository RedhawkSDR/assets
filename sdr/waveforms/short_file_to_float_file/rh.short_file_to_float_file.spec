###############################################################################
# This file is protected by Copyright. Please refer to the COPYRIGHT file
# distributed with this source distribution.
# 
# This file is part of REDHAWK short_file_to_float_file.
# 
# REDHAWK short_file_to_float_file is free software: you can redistribute it and/or modify it under
# the terms of the GNU Lesser General Public License as published by the Free
# Software Foundation, either version 3 of the License, or (at your option) any
# later version.
# 
# REDHAWK short_file_to_float_file is distributed in the hope that it will be useful, but WITHOUT ANY
# WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
# A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more
# details.
# 
# You should have received a copy of the GNU Lesser General Public License along
# with this program.  If not, see http://www.gnu.org/licenses/.
###############################################################################

# RPM package for rh.short_file_to_float_file

%global _sdrroot /var/redhawk/sdr
%global _prefix %{_sdrroot}

Name: rh.short_file_to_float_file
Summary: Waveform rh.short_file_to_float_file
Version: 1.0.2
Release: 2%{?dist}
License: LGPLv3+
Group: REDHAWK/Waveforms
Source: %{name}-%{version}.tar.gz

# Require the controller whose SPD is referenced
Requires: rh.FileReader

# Require each referenced component
Requires: rh.FileReader rh.DataConverter rh.FileWriter

BuildArch: noarch
BuildRoot: %{_tmppath}/%{name}-%{version}

%description
 * Commit: __REVISION__
 * Source Date/Time: __DATETIME__

%prep
%setup

%install
%__rm -rf $RPM_BUILD_ROOT
%__mkdir_p "$RPM_BUILD_ROOT%{_prefix}/dom/waveforms/rh/short_file_to_float_file"
%__install -m 644 short_file_to_float_file.sad.xml $RPM_BUILD_ROOT%{_prefix}/dom/waveforms/rh/short_file_to_float_file/short_file_to_float_file.sad.xml
%__install -m 644 short_file_to_float_file.sad_GDiagram $RPM_BUILD_ROOT%{_prefix}/dom/waveforms/rh/short_file_to_float_file/short_file_to_float_file.sad_GDiagram

%files
%defattr(-,redhawk,redhawk)
%dir %{_prefix}/dom/waveforms/rh
%dir %{_prefix}/dom/waveforms/rh/short_file_to_float_file
%{_prefix}/dom/waveforms/rh/short_file_to_float_file/short_file_to_float_file.sad.xml
%{_prefix}/dom/waveforms/rh/short_file_to_float_file/short_file_to_float_file.sad_GDiagram
