#
# ===================== COPYRIGHT NOTICE =====================
# This file is protected by Copyright. Please refer to the COPYRIGHT file
# distributed with this source distribution.
#
# This file is part of REDHAWK.
#
# REDHAWK is free software: you can redistribute it and/or modify it
# under the terms of the GNU Lesser General Public License as published by the
# Free Software Foundation, either version 3 of the License, or (at your
# option) any later version.
#
# REDHAWK is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
# FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
# for more details.
#
# You should have received a copy of the GNU Lesser General Public License
# along with this program. If not, see http://www.gnu.org/licenses/.
# ============================================================
#
# Make file for the Java and C++ VRT libraries and accompanying documentation.
#

################################################################################
# Java Compiler Options
################################################################################
ifdef JAVAHOME
    jar_cmd     = $(JAVAHOME)/bin/jar
    java_cmd    = $(JAVAHOME)/bin/java
    javac_cmd   = $(JAVAHOME)/bin/javac
    javah_cmd   = $(JAVAHOME)/bin/javah
    javadoc_cmd = $(JAVAHOME)/bin/javadoc
else
  ifdef JAVA_HOME
    jar_cmd     = $(JAVA_HOME)/bin/jar
    java_cmd    = $(JAVA_HOME)/bin/java
    javac_cmd   = $(JAVA_HOME)/bin/javac
    javah_cmd   = $(JAVA_HOME)/bin/javah
    javadoc_cmd = $(JAVA_HOME)/bin/javadoc
  else
    jar_cmd     = jar
    java_cmd    = java
    javac_cmd   = javac
    javah_cmd   = javah
    javadoc_cmd = javadoc
  endif
endif

java_build_flags       := $(VRT_JAVAC_FLAGS)   -deprecation -Xlint:all
java_doc_flags         := $(VRT_JAVADOC_FLAGS) -version -author -quiet -noindex
java_run_flags         := $(VRT_JAVA_FLAGS)    -Djava.net.preferIPv4Stack=true


################################################################################
# C++ Compiler Selection
################################################################################
ifeq ($(VRT_CC),)
  ifeq ($(shell uname),Darwin)
    cpp_compiler := clang++
  else
    cpp_compiler := g++
  endif
else
  cpp_compiler := $(VRT_CC)
  ifeq ($(VRT_CC),clang)
    cpp_compiler := clang++
  endif
  ifeq ($(VRT_CC),icpc)
    cpp_compiler := icc
  endif
  ifeq ($(VRT_CC),gcc)
    cpp_compiler := g++
  endif
endif

################################################################################
# C++ Link Options
#   -lrt      Required for clock_gettime(..) and clock_nanosleep(..) on Linux,
#             not available on OS X (or other BSD)
#   -lpthread Required for pthread usage
################################################################################
ifeq ($(shell uname),Linux)
  cpp_link_flags  := -lpthread -lrt
else
  cpp_link_flags  := -lpthread
endif

################################################################################
# C++ Build Options
#   VRT_FAST will enable platform-specific optimizations which may make code
#   less portable (i.e. don't use for an NFS-based install unless all systems
#   match) and may violate strict compliance with some language standards.
################################################################################
ifeq ($(cpp_compiler),icc)
  # Note that adding -guide will recommend code changes that will improve
  # parallelization (must have -parallel also set); however most of the
  # actionable suggestions have already been implemented.
  #
  # Details about the warning flags used with the Intel compilers:
  #  -Wall    - Enables warnings similar to -Wall in GCC.
  #  -w3      - Overrides -Wall and enables a ton of extra warnings (this
  #             is similar to -Wextra under GCC). The higher warning levels
  #             under Intel are only available on Windows.
  #  -wd383   - Disables diagnostic remark #383 (value copied to temporary,
  #             reference to temporary used) since it warns about uses that
  #             follow standard usage guides.
  #  -wd981   - Disables diagnostic remark #981 (operands are evaluated in
  #             unspecified order) which otherwise reports a large number
  #             of warnings that could only come up in a non-thread-safe
  #             usage.
  #  -wd1418  - Disables diagnostic remark #1418 (external function
  #             definition with no prior declaration) since it is reported
  #             for all of the non-static function templates.
  #  -wd3280  - Disables diagnostic remark #3280 (declaration hides member)
  #             since it warns about uses that follow standard usage guides
  #             (including use of an instance variable name within a static
  #             function).
  ifeq ($(VRT_FAST),true)
    cpp_build_flags  := -O3 -Wall -w3 -wd383 -wd981 -wd1418 -wd3280 -fPIC $(VRT_CC_FLAGS) -parallel -xHost
  else
    cpp_build_flags  := -O3 -Wall -w3 -wd383 -wd981 -wd1418 -wd3280 -fPIC $(VRT_CC_FLAGS) -parallel
  endif
endif
ifeq ($(cpp_compiler),clang++)
  # Use of -Ofast *will* cause some of the unit tests to fail with rounding errors
  # due to some of the overly-aggressive optimizations.
  #
  # Details about the warning flags used with the Clang compiler:
  # -Wall        - Enables warnings similar to -Wall in GCC.
  # -Wextra      - Enables "extra" warnings similar to -Wextra in GCC.
  # -Weverything - Enables **ALL** warnings. This is NOT used here since it
  #                is not compatible with Clang 2.8 which is still commonly
  #                used under CentOS 6.5 and the like.
  ifeq ($(VRT_FAST),true)
    cpp_build_flags  := -Ofast -Wall -Wextra -fPIC $(VRT_CC_FLAGS) -Wno-overloaded-virtual
  else
    cpp_build_flags  := -O3    -Wall -Wextra -fPIC $(VRT_CC_FLAGS) -Wno-overloaded-virtual
  endif
endif
ifeq ($(cpp_compiler),g++)
  # -rdynamic is required if using INCLUDE_STACK_TRACE and doesn't hurt to
  # include at other times (this is NOT the same as using -idynamic in icc).
  #
  # Details about the warning flags used with the Clang compiler:
  # -Wall        - Enables "all" (which is not really all) warnings.
  # -Wextra      - Enables "extra" (but still not all) warnings.
  # -Wcast-align - Enables warnings about casting pointers in a manner that
  #                is incompatible with the current architecture (e.g. a
  #                cast of char* to double* where double* must be on 8-byte
  #                boundaries). [There are situations in the code where
  #                casts of this nature are done without alignment checks.
  #                This works fine (and fast) on x86 and x86_64 but may
  #                need an alternate implementation for other processors.]
  ifeq ($(VRT_FAST),true)
    cpp_build_flags  := -O3 -Wall -Wextra -Wcast-align -fPIC $(VRT_CC_FLAGS) -rdynamic
  else
    cpp_build_flags  := -O3 -Wall -Wextra -Wcast-align -fPIC $(VRT_CC_FLAGS) -rdynamic -mtune=native -march=native
  endif
endif

ifeq ($(VRT_JNI),true)
  jni_jar              := java_lib/vrt-jni.jar
else
  jni_jar              :=
  VRT_JNI_DEBUG_PREFIX :=
endif

ifeq ($(VRT_CC_DEBUG_PREFIX),)
  ifeq ($(VRT_JNI_DEBUG_PREFIX),)
    # No debugging in use
  else
    cpp_build_flags    := $(cpp_build_flags) -g
  endif
else
  cpp_build_flags      := $(cpp_build_flags) -g
endif

ifeq ($(INTERNAL_FLOAT16_LIB),0)
  ifeq ($(VRT_CC),icc)
    cpp_build_flags    := $(cpp_build_flags) -DINTERNAL_FLOAT16_LIB=0
  else
    # Additional flags required if building with GCC's IEEE 754-2008 16-bit
    # floating-point number support (only valid with GCC 4.5+).
    cpp_build_flags    := $(cpp_build_flags) -DINTERNAL_FLOAT16_LIB=0 -mfp16-format=ieee
  endif
else
    cpp_build_flags    := $(cpp_build_flags) -DINTERNAL_FLOAT16_LIB=1
endif

ifeq ($(INTERNAL_UUID_LIB),1)
    cpp_build_flags    := $(cpp_build_flags) -DINTERNAL_UUID_LIB=1
else
  ifeq ($(INTERNAL_UUID_LIB),2)
    gcrypt_build_flags := $(shell libgcrypt-config --cflags --libs)
    cpp_build_flags    := $(cpp_build_flags) -DINTERNAL_UUID_LIB=2 $(gcrypt_build_flags)
  else
    cpp_build_flags    := $(cpp_build_flags) -DINTERNAL_UUID_LIB=0
    cpp_link_flags     := $(cpp_link_flags)  -luuid
  endif
endif

ifeq ($(DYNAMIC_LIBS),1)
  cpp_build_flags := $(cpp_build_flags) -DDYNAMIC_LIBS
  cpp_link_flags  := $(cpp_link_flags)  -ldl
endif

ifndef VRT_PREFIX
  VRT_PREFIX := /usr/local
endif

ifndef VRT_PROC
  VRT_PROC := $(shell uname -p)
endif

ifndef VRTHOME
  VRTHOME := $(shell pwd)
endif

ifdef LD_LIBRARY_PATH
  SET_LD_LIBRARY_PATH := export LD_LIBRARY_PATH="cpp_lib/:java_lib/:${LD_LIBRARY_PATH}"
else
  SET_LD_LIBRARY_PATH := export LD_LIBRARY_PATH="cpp_lib/:java_lib/"
endif

vrt_expected := $(shell /bin/pwd)
vrt_actual   := $(shell cd $(VRTHOME:/=) && /bin/pwd)

ifneq ($(vrt_expected),$(vrt_actual))
  vrt_error       := "ERROR: setting of VRTHOME=$(VRTHOME) conflicts with current build directory, $(vrt_expected)"
else
  vrt_error       := ""
endif

ifeq (TRUE,$(shell (test $(VRT_PROC) != "i386") \
                && (test $(VRT_PROC) != "i686") \
                && (test $(VRT_PROC) != "x86_64") && echo "TRUE"))
  processor_error := "ERROR: Could not determine processor type (got $(VRT_PROC)), \
	              use 'setenv VRT_PROC i386' or 'setenv VRT_PROC x86_64' to override."
else
  processor_error := ""
endif


#######################################################
# Build options
.NOTPARALLEL:          # required for this file
.PHONY:                usage all test cpp java cpptest javatest dist tidy \
                       clean cleanjava cleancpp cleanall scripts install \
                       javanettest cppnettest javanetserver default
.SUFFIXES:             # don't want and don't need these set
.DEFAULT:              # don't want and don't need these set
.VPATH:                cpp_src/vrt/lib cpp_src/vrt/net cpp_src/vrt/libm
.EXPORT_ALL_VARIABLES:
#export

#######################################################
# The next three lines based on an example in the GNU Make user's guide
empty :=
colon := :
space := $(empty) $(empty)
comma := ,
#######################################################
cpp_includes          := -Icpp_src/vrt/lib/ -Icpp_src/vrt/net/ -Icpp_src/vrt/libm/
cpp_header_files      := $(wildcard cpp_src/vrt/lib/*.h ) \
                         $(patsubst %/multicast.h,$(space),$(wildcard cpp_src/vrt/net/*.h)) \
                         cpp_src/vrt/lib/PackUnpack.h cpp_src/vrt/libm/PacketFactory.h
cpp_source_files      := $(patsubst %.h,%.cc,$(cpp_header_files))
cpp_obj_files         := $(patsubst %.h,%.o,$(cpp_header_files))
cpptest_includes      := $(cpp_includes) \
                         -Icpp_test/vrttest/inc/ -Icpp_test/vrttest/lib/ \
                         -Icpp_test/vrttest/net/ -Icpp_test/vrttest/libm/
cpptest_header_files  := $(wildcard cpp_test/vrttest/*/*.h )
cpptest_source_files  := $(patsubst %.h,%.cc,$(cpptest_header_files))
cpptest_obj_files     := $(patsubst %.h,%.o,$(cpptest_header_files))

java_libg_sourceX     := $(wildcard java_src/nxm/vrt/dat/*.java) \
                         $(wildcard java_src/nxm/vrt/lib/*.java) \
                         $(wildcard java_src/nxm/vrt/libg/*.java) \
                         $(wildcard java_src/nxm/vrt/net/*.java) \
                         java_src/nxm/vrt/lib/PackUnpack.java
java_libg_source      := $(subst java_src/nxm/vrt/lib/PackUnpack.java,$(space),$(java_libg_sourceX))
java_source_files     := $(java_libg_source) \
                         $(wildcard java_src/nxm/vrt/libm/*.java)
javatest_source_files := $(wildcard java_test/nxm/vrttest/*/*.java)

pkt_source            := $(wildcard pkt_src/*.xml)

# Commands for running PacketGen internally, the first is "bootstrap" variant
# used when building Java code, the second is the "normal" variant.
run_pkt_gen1      := $(java_cmd) -classpath java_src/ -Dvrt.packet_factory="" \
                     nxm.vrt.libg.PacketGen --package nxm.vrt.libm --namespace vrt
run_pkt_gen2      := $(java_cmd) -classpath java_lib/vrt.jar -Dvrt.packet_factory="" \
                     nxm.vrt.libg.PacketGen --package nxm.vrt.libm --namespace vrt
run_make_script   := $(java_cmd) -classpath java_lib/vrt.jar -DVRTHOME=$(VRTHOME) \
                     -DFLAGS="$(java_run_flags)"

ifeq ($(VRT_PROC),x86_64)
  install_inc_dir := $(VRT_PREFIX)/include/vrt/
  install_bin_dir := $(VRT_PREFIX)/bin/
  install_lib_dir := $(VRT_PREFIX)/lib64/
  install_jar_dir := $(VRT_PREFIX)/java/
else
  install_inc_dir := $(VRT_PREFIX)/include/vrt/
  install_bin_dir := $(VRT_PREFIX)/bin/
  install_lib_dir := $(VRT_PREFIX)/lib/
  install_jar_dir := $(VRT_PREFIX)/java/
endif

# Top-Level Targets (One-Liners)
default:					usage
all:						java cpp
test:						all javatest cpptest
java:						java_lib/vrt.jar  java_doc pkt_doc scripts $(jni_jar)
cpp:						cpp_lib/libvrt.so cpp_doc  pkt_doc scripts
scripts:					bin/niccheck bin/packetgen bin/udpdump
clean:						cleanjava cleancpp

# Target Files (One-Liners)
cpp_doc:					cpp_doc/html/index.html
java_doc:					java_doc/index.html
pkt_doc:					pkt_doc/index.html
bin/niccheck:					bin/packetgen
bin/udpdump:					bin/packetgen
cpp_src/vrt/lib/PackUnpack.cc:			cpp_src/vrt/lib/PackUnpack.h
cpp_src/vrt/libm/PacketFactory.cc:		cpp_src/vrt/libm/PacketFactory.h
java_src/jni_manifest.mf:			java_src/manifest.mf
java_test/manifest.mf:				java_src/manifest.mf
java_src/nxm/vita/cfg/manifest.mf:		java_src/manifest.mf
java_src/nxm/vrt/cfg/manifest.mf:		java_src/manifest.mf
java_src/nxm/vrt/lib/nxm_vrt_lib_PackUnpack.cc:	java_lib/vrt.jar
java_src/nxm/vrt/lib/nxm_vrt_lib_PackUnpack.h:	java_lib/vrt-jni.jar
java_lib/PackUnpack.so:				java_lib/vrt-jni.jar

cppstatic:
	@echo "ERROR: Build target 'cppstatic' is no longer a stand-alone option, please use 'cpp'"
	@test 1 == 0                # <- forces make to abort

javatest:					java_lib/vrttest.jar $(jni_jar)
	@echo ""
	@echo "================================================================"
	@echo "Run Java Tests"
	@echo "================================================================"
	$(SET_LD_LIBRARY_PATH) && $(VRT_JNI_DEBUG_PREFIX) java $(VRT_JAVA_DEBUG_FLAGS) -jar java_lib/vrttest.jar UnitTest

cpptest:					cpp_lib/vrttest
	@echo ""
	@echo "================================================================"
	@echo "Run C++ Tests"
	@echo "================================================================"
	$(SET_LD_LIBRARY_PATH) && $(VRT_CC_DEBUG_PREFIX) cpp_lib/vrttest UnitTest

javanetserver:					java_lib/vrttest.jar
	@echo ""
	@echo "================================================================"
	@echo "Run Java Networking Test Server"
	@echo "================================================================"
	java $(VRT_JAVA_DEBUG_FLAGS) -jar java_lib/vrttest.jar NetTestServer

javanettest:					java_lib/vrttest.jar
	@echo ""
	@echo "================================================================"
	@echo "Run Java Networking Tests"
	@echo "================================================================"
	java $(VRT_JAVA_DEBUG_FLAGS) -jar java_lib/vrttest.jar NetTestClient

cppnettest:					cpp_lib/vrttest
	@echo ""
	@echo "================================================================"
	@echo "Run C++ Networking Tests"
	@echo "================================================================"
	$(SET_LD_LIBRARY_PATH) && $(VRT_CC_DEBUG_PREFIX) cpp_lib/vrttest NetTestClient

dist:						all
	@echo ""
	@echo "================================================================"
	@echo "Build Distribution"
	@echo "================================================================"
	rm -Rf cpp_lib
	rm -f  java_src/nxm/vrt/*/*.class
	rm -Rf java_src/nxm/vrt/libm java_src/nxm/vrt/lib/PackUnpack.java
	rm -f  java_lib/distro.txt
	echo "Distro built on `date +%FT%T%z` " > java_lib/distro.txt
	touch cpp_src/vrt/lib/PackUnpack.h cpp_src/vrt/lib/PackUnpack.cc $(cpp_libm_header) $(cpp_libm_source)

tidy:
	@echo ""
	@echo "================================================================"
	@echo "Remove Java Class Files"
	@echo "================================================================"
	rm -f  java_src/nxm/vrt/*/*.class
	rm -f  java_test/nxm/vrttest/*/*.class
	@echo ""
	@echo "================================================================"
	@echo "Remove C++ Object Files"
	@echo "================================================================"
	rm -f  cpp_src/vrt/*/*.o
	rm -f  cpp_test/vrttest/*/*.o

cleanjava:
	@echo ""
	@echo "================================================================"
	@echo "Remove Java Docs/Libs"
	@echo "================================================================"
	rm -Rf java_doc java_lib java_src/nxm/vrt/*/*.class java_test/nxm/vrttest/*/*.class
	@echo ""
	@echo "================================================================"
	@echo "Remove Auto-Generated Java Code"
	@echo "================================================================"
	rm -Rf java_src/nxm/vrt/libm java_src/nxm/vrt/lib/PackUnpack.java
	rm -f  java_lib/distro.txt
	rm -f  java_src/manifest.mf java_src/jni_manifest.mf java_test/manifest.mf
	rm -f  java_src/nxm/vita/cfg/manifest.mf java_src/nxm/vrt/cfg/manifest.mf

cleancpp:
	@echo ""
	@echo "================================================================"
	@echo "Remove C++ Docs/Libs"
	@echo "================================================================"
	rm -Rf cpp_doc cpp_lib cpp_src/vrt/*/*.o cpp_test/vrttest/*/*.o

cleanall:					clean
	@echo ""
	@echo "================================================================"
	@echo "Remove Auto-Generated C++ Code"
	@echo "================================================================"
	rm -Rf cpp_src/vrt/libm
	rm -f  cpp_src/vrt/lib/PackUnpack.h cpp_src/vrt/lib/PackUnpack.cc
	@echo ""
	@echo "================================================================"
	@echo "Remove Auto-Generated Packet Type Docs"
	@echo "================================================================"
	rm -Rf pkt_doc
	@echo ""
	@echo "================================================================"
	@echo "Remove Auto-Generated Scripts"
	@echo "================================================================"
	rm -Rf bin
	@echo ""
	@echo "================================================================"
	@echo "Check for files that may cause distribution issues"
	@echo "================================================================"
	@find . -iname "*.ade" # Rejected by GMail
	@find . -iname "*.adp" # Rejected by GMail
	@find . -iname "*.bat" # Rejected by GMail
	@find . -iname "*.chm" # Rejected by GMail
	@find . -iname "*.cmd" # Rejected by GMail
	@find . -iname "*.com" # Rejected by GMail
	@find . -iname "*.cpl" # Rejected by GMail
	@find . -iname "*.exe" # Rejected by GMail
	@find . -iname "*.hta" # Rejected by GMail
	@find . -iname "*.ins" # Rejected by GMail
	@find . -iname "*.isp" # Rejected by GMail
	@find . -iname "*.jse" # Rejected by GMail
	@find . -iname "*.lib" # Rejected by GMail
	@find . -iname "*.mde" # Rejected by GMail
	@find . -iname "*.msc" # Rejected by GMail
	@find . -iname "*.msp" # Rejected by GMail
	@find . -iname "*.mst" # Rejected by GMail
	@find . -iname "*.scr" # Rejected by GMail
	@find . -iname "*.sct" # Rejected by GMail
	@find . -iname "*.shb" # Rejected by GMail
	@find . -iname "*.sys" # Rejected by GMail
	@find . -iname "*.vb"  # Rejected by GMail
	@find . -iname "*.vbe" # Rejected by GMail
	@find . -iname "*.vbs" # Rejected by GMail
	@find . -iname "*.vxd" # Rejected by GMail
	@find . -iname "*.wsc" # Rejected by GMail
	@find . -iname "*.wsf" # Rejected by GMail
	@find . -iname "*.wsh" # Rejected by GMail
	@find . -iname "*.zip" # Container rejected by GMail if it contains a "rejected" type
	@find . -iname "*.tar" # Container rejected by GMail if it contains a "rejected" type
	@find . -iname "*.tgz" # Container rejected by GMail if it contains a "rejected" type
	@find . -iname "*.taz" # Container rejected by GMail if it contains a "rejected" type
	@find . -iname "*.z"   # Container rejected by GMail if it contains a "rejected" type
	@find . -iname "*.gz"  # Container rejected by GMail if it contains a "rejected" type
	@find . -iname "*.rar" # Container rejected by GMail if it contains a "rejected" type

bin/packetgen:					java_lib/vrt.jar
	@echo ""
	@echo "================================================================"
	@echo "Build Scripts"
	@echo "================================================================"
ifneq ($(vrt_error),"")
	@echo $(vrt_error)
	@test 1 == 0                # <- forces make to abort
endif
	mkdir -p bin
	$(run_make_script) nxm.vrt.libg.PacketGen --dumpcshscript >& bin/packetgen
	$(run_make_script) nxm.vrt.net.NICCheck   --dumpcshscript >& bin/niccheck
	$(run_make_script) nxm.vrt.net.UDPDump    --dumpcshscript >& bin/udpdump
	chmod a+x bin/packetgen bin/niccheck bin/udpdump

install:					all
	@echo ""
	@echo "================================================================"
	@echo "Install Java/C++ Libraries"
	@echo "================================================================"
ifneq ($(processor_error),"")
	@echo $(processor_error)
	@test 1 == 0                # <- forces make to abort
endif
	mkdir -p $(install_inc_dir)
	mkdir -p $(install_bin_dir)
	mkdir -p $(install_lib_dir)
	mkdir -p $(install_jar_dir)

	cp -p cpp_src/vrt/*/*.h $(install_inc_dir)
	cp -p cpp_lib/*.so      $(install_lib_dir)
	cp -p java_lib/*.jar    $(install_jar_dir)

	$(run_make_script) -DJARDIR=$(install_jar_dir) nxm.vrt.libg.PacketGen --dumpcshscript >& $(install_bin_dir)/packetgen
	$(run_make_script) -DJARDIR=$(install_jar_dir) nxm.vrt.net.NICCheck   --dumpcshscript >& $(install_bin_dir)/niccheck
	$(run_make_script) -DJARDIR=$(install_jar_dir) nxm.vrt.net.UDPDump    --dumpcshscript >& $(install_bin_dir)/udpdump
	chmod a+x $(install_bin_dir)/packetgen $(install_bin_dir)/niccheck $(install_bin_dir)/udpdump


## Packet Definitions #################################
# The "Pkt" build relies on the JAR files from the Java build, note that the dependency
# for the java_src files is different since they need to be done mid-build for Java,
# while the cpp_src and pkt_doc can be done after-the-fact (the "after-the-fact approach"
# is compatible with the C++ only build).
pkt_doc/index.html:				$(pkt_source) java_lib/vrt.jar
	@echo ""
	@echo "================================================================"
	@echo "Auto-Generate Packet Documentation"
	@echo "================================================================"
	mkdir -p pkt_doc
	$(run_pkt_gen2) --HTML pkt_doc/ $(pkt_source)

## C++ ################################################
# The C++ build relies on the JAR files from the Java build
cpp_lib/libvrt.so:				$(cpp_header_files) $(cpp_source_files) \
                                                cpp_src/vrt/lib/PackUnpack.h \
						cpp_src/vrt/lib/PackUnpack.cc \
						cpp_src/vrt/libm/PacketFactory.h \
						cpp_src/vrt/libm/PacketFactory.cc \
						$(wildcard cpp_src/vrt/libm/*.h) \
						$(wildcard cpp_src/vrt/libm/*.cc) \
						cpp_lib/tai-utc.dat
	@echo ""
	@echo "================================================================"
	@echo "Build C++ Object Files"
	@echo "  (warnings about the 'hot' attribute being ignored by the"
	@echo "   compiler are normal for some platforms)"
	@echo "================================================================"
	$(MAKE) -f Makefile_cpp cpp_lib/libvrt.so

cpp_lib/vrttest:				cpp_lib/libvrt.so \
                                                $(cpptest_header_files) $(cpptest_source_files)
	@echo ""
	@echo "================================================================"
	@echo "Build C++ Test Object Files"
	@echo "================================================================"
	$(MAKE) -f Makefile_cpp cpp_lib/vrttest

cpp_src/vrt/lib/PackUnpack.h:			java_lib/vrt.jar java_src/nxm/vrt/dat/PackUnpackGen.code
	@echo ""
	@echo "================================================================"
	@echo "Auto-Generate PackUnpack.h / PackUnpack.cc"
	@echo "================================================================"
	$(java_cmd) -classpath java_lib/vrt.jar nxm.vrt.dat.PackUnpackGen --cpp \
	    cpp_src/vrt/lib/PackUnpack.h cpp_src/vrt/lib/PackUnpack.cc

cpp_doc/html/index.html:			cpp_lib/libvrt.so doxygen.cfg
	@echo ""
	@echo "================================================================"
	@echo "Build C++ Docs"
	@echo "================================================================"
	rm -Rf cpp_doc
ifneq ($(shell (which doxygen 1>> /dev/null 2>> /dev/null) && (echo "TRUE")),TRUE)
	# This is common on OS X, and is also seen on some Linux configurations.
	@echo "WARNING: doxygen not found, skipping build of C++ documentation"
	mkdir cpp_doc
	mkdir cpp_doc/html
	echo "<html>"                                 >  cpp_doc/html/index.html
	echo "  <head>"                               >> cpp_doc/html/index.html
	echo "    <title>VRT: Main Page</title>"      >> cpp_doc/html/index.html
	echo "  </head>"                              >> cpp_doc/html/index.html
	echo "  <body>"                               >> cpp_doc/html/index.html
	echo "    <h3>Could not find doxygen, build"  >> cpp_doc/html/index.html
	echo "    of C++ documentation skipped.</h3>" >> cpp_doc/html/index.html
	echo "  </body>"                              >> cpp_doc/html/index.html
	echo "</html>"                                >> cpp_doc/html/index.html
else
	doxygen doxygen.cfg
endif

cpp_src/vrt/libm/PacketFactory.h:		$(pkt_source) java_lib/vrt.jar
	@echo ""
	@echo "================================================================"
	@echo "Auto-Generate C++ Packet Handlers"
	@echo "================================================================"
	$(run_pkt_gen2) --CPP cpp_src/vrt/libm/ $(pkt_source)

cpp_lib/tai-utc.dat:				java_src/nxm/vrt/dat/tai-utc.dat
	@echo ""
	@echo "================================================================"
	@echo "Copy tai-utc.dat for use from C++"
	@echo "================================================================"
	mkdir -p cpp_lib
	cp $< $@
	touch -m $@

## Java ###############################################
# The Java build is more complicated since some of the Java code is auto-generated
# from other Java code and both need to be built and inserted into the JAR file
java_src/manifest.mf:
	@echo ""
	@echo "================================================================"
	@echo "Build Java Manifest Files"
	@echo "================================================================"
	echo "Manifest-Version: 1.0"               >| java_src/manifest.mf
	echo "Main-Class: nxm.vrt.lib.Main"        >> java_src/manifest.mf
	echo ""                                    >> java_src/manifest.mf

	echo "Manifest-Version: 1.0"               >| java_src/jni_manifest.mf
	echo "Main-Class: nxm.vrt.lib.Main"        >> java_src/jni_manifest.mf
	echo "Class-Path: vrt.jar"                 >> java_src/jni_manifest.mf
	echo ""                                    >> java_src/jni_manifest.mf

	echo "Manifest-Version: 1.0"               >| java_test/manifest.mf
	echo "Main-Class: nxm.vrttest.lib.Main"    >> java_test/manifest.mf
	echo "Class-Path: vrt.jar"                 >> java_test/manifest.mf
	echo ""                                    >> java_test/manifest.mf

	echo "Manifest-Version: 1.0"               >| java_src/nxm/vita/cfg/manifest.mf
	echo "Main-Class: nxm.sys.lib.NeXtMidas"   >> java_src/nxm/vita/cfg/manifest.mf
	echo "Class-Path: nxm-vrt.jar nxm-sys.jar" >> java_src/nxm/vita/cfg/manifest.mf
	echo ""                                    >> java_src/nxm/vita/cfg/manifest.mf

	echo "Manifest-Version: 1.0"               >| java_src/nxm/vrt/cfg/manifest.mf
	echo "Main-Class: nxm.sys.lib.NeXtMidas"   >> java_src/nxm/vrt/cfg/manifest.mf
	echo "Class-Path: nxm-sys.jar"             >> java_src/nxm/vrt/cfg/manifest.mf
	echo ""                                    >> java_src/nxm/vrt/cfg/manifest.mf

java_lib/vrt.jar:				$(java_source_files) \
						java_src/nxm/vrt/dat/PackUnpackGen.code \
						java_src/nxm/vrt/dat/PackUnpackGen.java \
						$(wildcard java_src/nxm/vrt/libm/*.java) \
						java_src/manifest.mf
	@echo ""
	@echo "================================================================"
	@echo "Build Java Code"
	@echo "================================================================"
	mkdir -p java_lib
	$(javac_cmd) $(java_build_flags) -classpath java_src -d java_src \
		java_src/nxm/vrt/dat/PackUnpackGen.java

ifeq ($(VRT_JNI),true)
	$(java_cmd) -classpath java_src/ nxm.vrt.dat.PackUnpackGen --jni \
	    java_src/nxm/vrt/lib/PackUnpack.java java_src/nxm/vrt/lib/nxm_vrt_lib_PackUnpack.cc
else
	$(java_cmd) -classpath java_src/ nxm.vrt.dat.PackUnpackGen --java \
	    java_src/nxm/vrt/lib/PackUnpack.java
endif

	$(javac_cmd) $(java_build_flags) -classpath java_src -d java_src \
		java_src/nxm/vrt/libg/PacketGen.java
	$(run_pkt_gen1) --Java java_src/ $(pkt_source)

	$(javac_cmd) $(java_build_flags) -classpath java_src -d java_src \
		java_src/nxm/vrt/*/*.java
	cd java_src && $(jar_cmd) cmf manifest.mf ../$@ nxm/vrt
	touch -m java_src $@ java_src/nxm/vrt/dat/PackUnpackGen.java \
	                     java_src/nxm/vrt/dat/PackUnpackGen.class \
	                     java_src/nxm/vrt/lib/PackUnpack.java \
	                     java_src/nxm/vrt/lib/PackUnpack.class

java_lib/vrt-jni.jar:				java_lib/vrt.jar \
						cpp_src/vrt/lib/PackUnpack.h \
						cpp_src/vrt/lib/PackUnpack.cc \
						cpp_src/vrt/lib/VRTObject.h \
						cpp_src/vrt/lib/VRTMath.h \
						cpp_src/vrt/lib/VRTMath.cc \
						java_src/jni_manifest.mf
	@echo ""
	@echo "================================================================"
	@echo "Build JNI Code"
	@echo "================================================================"
	mkdir -p java_lib
	$(javah_cmd) -classpath java_lib/vrt.jar \
	    -o java_src/nxm/vrt/lib/nxm_vrt_lib_PackUnpack.h nxm.vrt.lib.PackUnpack

	$(cpp_compiler) $(cpp_build_flags) $(cpp_includes) -DNOT_USING_JNI=0 -shared \
	    -o java_lib/libPackUnpack.so java_src/nxm/vrt/lib/nxm_vrt_lib_PackUnpack.cc \
					 cpp_src/vrt/lib/VRTMath.cc
	cd java_lib/ && $(jar_cmd) cmf ../java_src/jni_manifest.mf vrt-jni.jar libPackUnpack.so

java_lib/vrttest.jar:				java_lib/vrt.jar $(javatest_source_files) \
						java_test/manifest.mf
	@echo ""
	@echo "================================================================"
	@echo "Build Java Test Code"
	@echo "================================================================"
	$(javac_cmd) $(java_build_flags) -classpath java_lib/vrt.jar:java_test -d java_test \
		java_test/nxm/vrttest/*/*.java
	cd java_test && $(jar_cmd) cmf manifest.mf ../$@ nxm
	touch -m java_test $@

java_doc/index.html:				java_lib/vrt.jar
	@echo ""
	@echo "================================================================"
	@echo "Build Java Docs"
	@echo "================================================================"
	mkdir -p java_doc
	$(javadoc_cmd) $(java_doc_flags) -classpath java_src -d java_doc \
		java_src/nxm/vrt/*/*.java

#######################################################
usage:
	@echo "Builds the Java and C++ VRT libraries."
	@echo ""
	@echo "BUILD TARGETS"
	@echo "================================================================================"
	@echo "  usage     - Prints this message"
	@echo ""
	@echo "  all       - Build the all  code"
	@echo "  java      - Build the Java code"
	@echo "  cpp       - Build the C++  code (will build Java dependencies not built yet)"
	@echo ""
	@echo "  install   - Builds everything and installs it (see 'Install Options', below)"
	@echo "  dist      - Internal Use Only: Builds everything for distribution"
	@echo ""
	@echo "  test      - Build the all  code and run the unit tests"
	@echo "  javatest  - Build the Java code and run the unit tests"
	@echo "  cpptest   - Build the C++  code and run the unit tests"
	@echo ""
	@echo "  tidy      - Remove all *.class/*.o files (not required at run time)"
	@echo "  cleanall  - Remove all  build products"
	@echo "  clean     - Remove all  build products (see note)"
	@echo "  cleancpp  - Remove C++  build products (see note)"
	@echo "  cleanjava - Remove Java build products"
	@echo ""
	@echo "  Note: 'clean' and 'cleancpp' do NOT remove any of the build products required"
	@echo "        for a C++ only build, whereas 'cleanall' does."
	@echo ""
	@echo "A distribution that has been prepared for a C++ only install will not re-build"
	@echo "any of the autogenerated code since the code generators are Java-based. Such a"
	@echo "distribution can be 'reset' to support a full build with 'make cleanall'."
	@echo ""
	@echo ""
	@echo "INSTALL OPTIONS"
	@echo "================================================================================"
	@echo "The installation files will be installed in the following locations (VRT_PREFIX"
	@echo "defaults to /usr/local and can be set any time before calling 'make install'):"
	@echo "     $$VRT_PREFIX/include/vrt/        C++ include files"
	@echo "     $$VRT_PREFIX/lib/                C++ shared objects (32-bit systems)"
	@echo "     $$VRT_PREFIX/lib64/              C++ shared objects (64-bit systems)"
	@echo "     $$VRT_PREFIX/java/               Java JAR files"
	@echo "     $$VRT_PREFIX/bin/                Executable scripts/commands"
	@echo ""
	@echo ""
	@echo "JAVA BUILD OPTIONS"
	@echo "================================================================================"
	@echo "Java Compiler Selection:"
	@echo "  Setting JAVAHOME (or JAVA_HOME) will override the default Java components in"
	@echo "  the path. This setting should NOT be used under Mac OS X (particularly with"
	@echo "  JDK 7 and later)."
	@echo ""
	@echo "  Additional Java compiler settings can be specified for use during the build"
	@echo "  by setting VRT_JAVAC_FLAGS."
	@echo ""
	@echo ""
	@echo "C++ BUILD OPTIONS"
	@echo "================================================================================"
	@echo "C++ Compiler Selection:"
	@echo "  By default the GNU C++ compiler (g++) is used for building C++ code on Linux"
	@echo "  and clang++ is used for building C++ code on OS X, but this can be changed by"
	@echo "  setting the VRT_CC environment variable before calling make:"
	@echo "    Clang C++ Compiler:               setenv VRT_CC clang++"
	@echo "    GNU   C++ Compiler:               setenv VRT_CC g++"
	@echo "    Intel C++ Compiler:               setenv VRT_CC icc"
	@echo ""
	@echo "  Additional C++ compiler settings can be specified for use during the build"
	@echo "  by setting VRT_CC_FLAGS."
	@echo ""
	@echo "  Note that if using an older version of the Intel C++ compiler (particularly"
	@echo "  version 10.x or 11.x) it is necessary to set the following option to avoid"
	@echo "  extraneous 'remark' printouts:"
	@echo "    Suppress Extraneous Rempark:      setenv VRT_CC_FLAGS -w1"
	@echo ""
	@echo "  Note that Clang differs significantly from the GUN/Intel compilers and as a"
	@echo "  result the build procedure does not produce a staticly-linked archive (.a)."
	@echo ""
	@echo "C++ Optimizations:"
	@echo "  More aggressive compiler optimizations can be enabled by setting VRT_FAST to"
	@echo "  true. These optimizations are ones that are disabled by default since they"
	@echo "  tell the compiler to optimize for the local host processor which may restrict"
	@echo "  portability of the shared object. They may also enable extremely aggressive"
	@echo "  optimizations which may violate some of the more strict language rules (e.g."
	@echo "  rounding errors may be introduced)."
	@echo "    Use safe/portable optimizations:  setenv VRT_FAST false"
	@echo "    Use aggressive optimizations:     setenv VRT_FAST true"
	@echo ""
	@echo "  Note that the use of VRT_FAST is a 'USE AT YOUR OWN RISK' feature since it"
	@echo "  is known to cause some test cases to fail on some platforms (mostly due to"
	@echo "  additional rounding errors). Novice users are advised to avoid this feature"
	@echo "  as it is only intended for the most demanding environments."
	@echo ""
	@echo "C++ Floating-Point Library Options:"
	@echo "  By default the build process will use an set of internal library calls to"
	@echo "  provide support for 16-bit floating-point numbers ('half precision') as"
	@echo "  defined in IEEE 754-2008. Since the internal library calls are portable C++,"
	@echo "  they do not take advantage of any hardware-acceleration that may be available."
	@echo "  Both the Intel and GNU C++ compilers (GNU only since GCC 4.5) have library"
	@echo "  routines that will make use of hardware-acceleration (where possible), but"
	@echo "  their use is very platform-specific. Accordingly the use of the internal"
	@echo "  libraries is the default, but this behavior can be altered by setting:"
	@echo "    Use compiler-specific code:       setenv INTERNAL_FLOAT16_LIB 0"
	@echo "    Use internal library code:        setenv INTERNAL_FLOAT16_LIB 1"
	@echo ""
	@echo "C++ UUID Library Options:"
	@echo "  By default the standard UUID library (-luuid) will be used when working with"
	@echo "  UUIDs in the C++ code. Since many platforms do not have this library"
	@echo "  installed, there is a minimal UUID library internal to the VRT C++ code."
	@echo "  The internal library has the option of generating RFC 4122-compliant UUIDs"
	@echo "  (which requires the gcrypt libraries be available) or disabling generation"
	@echo "  of new UUIDs."
	@echo "    Standard UUID library:            setenv INTERNAL_UUID_LIB 0"
	@echo "    Internal UUID library (no gen):   setenv INTERNAL_UUID_LIB 1"
	@echo "    Internal UUID library (random):   setenv INTERNAL_UUID_LIB 2"
	@echo ""
	@echo "C++ Dynamic Library Options:"
	@echo "  This is experimental, and may be removed, do not use."
	@echo "    ???:                              setenv DYNAMIC_LIBS 0"
	@echo "    ???:                              setenv DYNAMIC_LIBS 1"
	@echo ""
	@echo ""
	@echo "PLATFORM-SPECIFIC NOTES"
	@echo "================================================================================"
	@echo "Mac OS X:"
	@echo "  Prior to building the C++ code it is necessary to make sure the required"
	@echo "  developer modules are installed (these are usually bundled with XCode)."
	@echo "  At this time only Clang is supported on OS X."
	@echo ""
	@echo ""
