#!/bin/bash
# the ios 10.3 sdk's tapi-tbd-v2 stubs list i386/x86_64 next to the device
# archs under "platform: ios". current apple ld (and lld) then treat them as
# ios-simulator libraries and refuse to link a device binary. we only build
# arm64, so drop the intel archs in place.
#   fix-sdk-tbd.sh SDKDIR
set -euo pipefail
find "${1:?usage: fix-sdk-tbd.sh SDKDIR}" -name '*.tbd' -type f -print0 | xargs -0 perl -pi -e '
  if (/archs:\s*\[/) {
    s/,\s*(i386|x86_64)\b//g;
    s/\[\s*(i386|x86_64)\s*,\s*/[ /g;
    s/\[\s*(i386|x86_64)\s*\]/[ ]/g;
  }'
echo "stripped intel archs from $(find "$1" -name '*.tbd' -type f | wc -l | tr -d ' ') tbd files"
