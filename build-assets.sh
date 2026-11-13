#!/bin/bash
#
# Build the REDHAWK asset RPMs (best-effort) in dependency order:
#   0. libRfSimulators   (recovered dependency; needed by FmRdsSimulator)
#   1. sdr/libraries      (dsp first -- rh.fftlib BuildRequires rh.dsp-devel)
#   2. sdr/components
#   3. sdr/devices
#   4. sdr/waveforms
#
# Each asset is built from its own *.spec via `rpmbuild -ta`, and its freshly
# built RPMs are installed so later assets can resolve them. Failures are logged
# and skipped (best-effort); the script still exits 0 and prints a summary.
#
# Env: SRC (asset tree, default /src), RPMOUT (default /rpms).

SRC=${SRC:-/src}
RPMOUT=${RPMOUT:-/rpms}
TOP=/root/rpmbuild

mkdir -p "$TOP"/{BUILD,RPMS,SOURCES,SPECS,SRPMS} "$RPMOUT"

BUILT=""
FAILED=""

build_asset() {
    local dir="$1"
    [ -d "$dir" ] || return 0
    local spec
    spec=$(ls "$dir"/*.spec 2>/dev/null | head -1)
    if [ -z "$spec" ]; then
        echo "SKIP (no spec): $dir"
        return 0
    fi
    local name ver tn
    name=$(awk '/^Name:/{print $2; exit}' "$spec")
    ver=$(awk '/^Version:/{print $2; exit}' "$spec")
    tn="${name}-${ver}"
    echo "==== building ${tn}  (${dir#$SRC/})"

    rm -rf "/tmp/${tn}"
    cp -a "$dir" "/tmp/${tn}"
    rm -rf "/tmp/${tn}/.git"
    tar czf "$TOP/SOURCES/${tn}.tar.gz" --exclude=.git -C /tmp "${tn}"
    rm -rf "/tmp/${tn}"

    if rpmbuild --define "_topdir $TOP" \
                --define "_ossiehome /usr/local/redhawk/core" \
                --define "_sdrroot /var/redhawk/sdr" \
                -ta "$TOP/SOURCES/${tn}.tar.gz" > "/tmp/build-${name}.log" 2>&1; then
        # Install this asset's just-built binary RPMs so dependents resolve.
        rpm -Uvh --force --nodeps $TOP/RPMS/*/${name}-*.rpm 2>/dev/null || true
        echo "OK:   ${name}"
        BUILT="${BUILT} ${name}"
    else
        echo "FAIL: ${name}  (see /tmp/build-${name}.log)"
        tail -n 20 "/tmp/build-${name}.log" | sed 's/^/      | /'
        FAILED="${FAILED} ${name}"
    fi
}

# 0. recovered dependency
build_asset "$SRC/libraries/libRfSimulators"

# 1. SDR libraries -- dsp must be first (rh.fftlib BuildRequires rh.dsp-devel)
build_asset "$SRC/sdr/libraries/dsp"
for d in "$SRC"/sdr/libraries/*/; do
    [ "$(basename "$d")" = "dsp" ] && continue
    build_asset "$d"
done

# 2. components
for d in "$SRC"/sdr/components/*/; do build_asset "$d"; done

# 3. devices
for d in "$SRC"/sdr/devices/*/; do build_asset "$d"; done

# 4. waveforms
for d in "$SRC"/sdr/waveforms/*/; do build_asset "$d"; done

# collect everything that built (binary RPMs only -- src RPMs aren't used
# downstream and would otherwise ride along in an image layer)
find "$TOP/RPMS" -name '*.rpm' -exec cp {} "$RPMOUT/" \;

echo "========================================================"
echo "BUILT: ${BUILT:-<none>}"
echo "FAILED:${FAILED:-<none>}"
echo "RPMs in ${RPMOUT}: $(ls -1 "$RPMOUT"/*.rpm 2>/dev/null | wc -l)"
echo "========================================================"
exit 0
