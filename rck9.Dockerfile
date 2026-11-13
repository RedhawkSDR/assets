# Multi-stage image for REDHAWK assets, built through the rfnext ci-support
# docker.yml template (TARGETS selects the stage):
#   TARGETS=builder  -> builds all asset RPMs best-effort (/rpms) via build-assets.sh
#   (default)        -> the full REDHAWK image: core + adminservice + assets, with
#                       the consolidated /repo + rh-<version>.tar.gz
#
# BASE_IMAGE is the upstream adminservice image (…/adminservice:<ref>-core-admin).

ARG BASE_IMAGE=adminservice:develop-4.0-core-admin

# ---- builder: compile every asset into RPMs (best-effort, dependency-ordered) ----
FROM ${BASE_IMAGE} AS builder
# log4cxx-devel provides /usr/lib64/liblog4cxx.so for linking C++ assets; pin to
# OUR 0.11.0 from the local repo (EPEL ships a conflicting 1.8.0). The rest are
# extra asset build deps (libRfSimulators: tinyxml/libsndfile; RTL2832U: rtl-sdr/libusb).
RUN dnf -y install log4cxx-devel-0.11.0 && \
    dnf -y install --skip-broken \
      tinyxml-devel libsndfile-devel rtl-sdr-devel libusb1-devel \
      autoconf automake libtool gcc-c++ rpm-build && \
    dnf clean all
COPY . /src
RUN SRC=/src RPMOUT=/rpms /src/build-assets.sh
WORKDIR /rpms

# ---- runtime: add assets to the consolidated repo, snapshot, install ----
FROM ${BASE_IMAGE} AS runtime
ARG VERSION=4.0.4
COPY --from=builder /rpms /tmp/asset-rpms
RUN find /tmp/asset-rpms -name '*.rpm' ! -name '*.src.rpm' -exec cp -n {} /repo/${VERSION}/ \; && \
    createrepo_c --update /repo/${VERSION}
# Consolidated snapshot of all three repos (core + adminservice + assets).
RUN tar czf /repo/rh-${VERSION}.tar.gz -C /repo ${VERSION}
# Install the asset packages (best-effort: --skip-broken tolerates any that
# failed to build and the waveforms that depend on them).
RUN dnf -y makecache && \
    pkgs="$(find /tmp/asset-rpms -name '*.rpm' ! -name '*debug*' ! -name '*.src.rpm' \
            -exec rpm -qp --qf '%{NAME}\n' {} \; | sort -u)" && \
    echo "Installing assets: ${pkgs}" && \
    dnf -y install --skip-broken ${pkgs} && \
    dnf clean all && \
    rm -rf /tmp/asset-rpms
WORKDIR /
