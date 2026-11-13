# Stage 1 of the assets pipeline: build the REDHAWK asset RPMs on top of the
# core-framework + adminservice image (rh-core-admin).
#
# BASE_IMAGE already has REDHAWK, the interfaces, codegen, log4cxx, uhd-devel,
# and the /repo yum repo. This stage adds the asset build deps, then runs
# build-assets.sh (best-effort, dependency-ordered) to produce the asset RPMs,
# including the recovered libRfSimulators (needed by FmRdsSimulator).
#
#   docker build -f rck9.assets-builder.Dockerfile -t rh-assets-builder:4.0.4 .

ARG BASE_IMAGE=rh-core-admin:4.0.4
FROM ${BASE_IMAGE}

# log4cxx-devel provides /usr/lib64/liblog4cxx.so (the unversioned link C++
# assets need for -llog4cxx) and satisfies libRfSimulators' BuildRequires. Pin
# to OUR 0.11.0 from the local repo -- EPEL ships a newer log4cxx-devel (1.8.0)
# that conflicts with the installed, redhawk-pinned log4cxx 0.11.0.
RUN dnf -y install log4cxx-devel-0.11.0 && \
    dnf -y install --skip-broken \
      tinyxml-devel libsndfile-devel rtl-sdr-devel libusb1-devel \
      autoconf automake libtool gcc-c++ rpm-build \
    && dnf clean all

COPY . /src
RUN SRC=/src RPMOUT=/rpms /src/build-assets.sh

WORKDIR /rpms
