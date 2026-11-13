# Stage 2 of the assets pipeline: add the asset RPMs to the consolidated yum
# repo, snapshot it, and install everything. The result is the full REDHAWK
# image: core-framework + adminservice + assets.
#
# Expects the stage-1 RPMs in the build context under ./rpms/ (CI artifacts).
# BASE_IMAGE (rh-core-admin) already carries /repo/<version> with core +
# adminservice RPMs installed; this stage layers the assets on top.
#
#   /repo/<version>/           now also contains the asset RPMs
#   /repo/rh-<version>.tar.gz   consolidated snapshot of all three repos
#   assets installed (best-effort)
#
#   docker build -f rck9.assets-install.Dockerfile -t rh:4.0.4 .

ARG BASE_IMAGE=rh-core-admin:4.0.4
FROM ${BASE_IMAGE}

ARG VERSION=4.0.4

# Asset RPMs from stage 1 (CI artifacts).
COPY rpms/ /tmp/asset-rpms/

# 1. Add assets to the existing /repo/<version> and refresh repo metadata.
RUN find /tmp/asset-rpms -name '*.rpm' ! -name '*.src.rpm' -exec cp -n {} /repo/${VERSION}/ \; && \
    createrepo_c --update /repo/${VERSION}

# 2. Consolidated snapshot of all three repos (core + adminservice + assets).
RUN tar czf /repo/rh-${VERSION}.tar.gz -C /repo ${VERSION}

# 3. Install the asset packages (best-effort: --skip-broken tolerates any assets
#    that failed to build in stage 1 and the waveforms that depend on them).
RUN dnf -y makecache && \
    pkgs="$(find /tmp/asset-rpms -name '*.rpm' ! -name '*debug*' ! -name '*.src.rpm' \
            -exec rpm -qp --qf '%{NAME}\n' {} \; | sort -u)" && \
    echo "Installing assets: ${pkgs}" && \
    dnf -y install --skip-broken ${pkgs} && \
    dnf clean all && \
    rm -rf /tmp/asset-rpms

WORKDIR /
