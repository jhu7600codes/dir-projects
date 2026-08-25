#!/usr/bin/env python3
"""
Grafts rebuilt dex files into an untouched copy of the original APK's zip,
instead of letting `apktool b` repackage resources.arsc/res/AndroidManifest
at all. `apktool b` has no "skip resources" option for its build command
(only `apktool d` can skip decoding them) - it always fully repacks the
resource table, which corrupted a resource ID reference on the first
attempt here (`Resources$NotFoundException: Resource ID #0x0` crash on
launch, building an options menu icon) despite the patch itself only
touching one method's smali. Confirmed via sha256: resources.arsc and
AndroidManifest.xml in the grafted output are byte-identical to the
original APK's.

Usage:
    python3 rebuild.py <original.apk> <apktool_build_output.apk> <out.apk>

Where <apktool_build_output.apk> is whatever `apktool b v14_decoded -o ...`
produced (only its dex files are used from that output).
"""
import re
import sys
import zipfile

# Auto-detected per run, not hardcoded: different app builds carry different
# dex counts (v14.34.54 has 4, v15.46.34 has 5 since apktool's multidex
# rebuild can shuffle which dex a class lands in) - a fixed 4-name set
# silently kept the ORIGINAL classes5.dex on a 5-dex build the first time
# this ran against v15, which risks a class ending up duplicated or missing
# depending on how the rebuild redistributed classes across dex files.
DEX_NAME_RE = re.compile(r"^classes\d*\.dex$")


def main() -> None:
    if len(sys.argv) != 4:
        print(__doc__)
        raise SystemExit(1)
    orig_path, rebuilt_path, out_path = sys.argv[1:4]

    with zipfile.ZipFile(rebuilt_path) as zrebuilt:
        dex_names = [n for n in zrebuilt.namelist() if DEX_NAME_RE.match(n)]
        if not dex_names:
            print("no classes*.dex found in rebuilt apk - aborting")
            raise SystemExit(1)
        patched_dex = {name: zrebuilt.read(name) for name in dex_names}
    print(f"found {len(dex_names)} dex file(s) in rebuilt apk: {sorted(dex_names)}")

    with zipfile.ZipFile(orig_path, "r") as zin:
        orig_dex_names = {n for n in zin.namelist() if DEX_NAME_RE.match(n)}
        extra = set(patched_dex) - orig_dex_names
        missing = orig_dex_names - set(patched_dex)
        if extra:
            print(f"note: rebuilt apk has dex file(s) original didn't: {sorted(extra)}")
        if missing:
            print(f"note: original apk has dex file(s) rebuild dropped: {sorted(missing)}")

        with zipfile.ZipFile(out_path, "w", zipfile.ZIP_DEFLATED) as zout:
            written = set()
            for info in zin.infolist():
                if info.filename in patched_dex:
                    data = patched_dex[info.filename]
                    # Fresh ZipInfo so size/CRC get recomputed for the new
                    # content rather than reusing the original entry's now-
                    # stale metadata.
                    new_info = zipfile.ZipInfo(info.filename, date_time=info.date_time)
                    new_info.compress_type = zipfile.ZIP_DEFLATED
                    new_info.external_attr = info.external_attr
                    zout.writestr(new_info, data)
                    written.add(info.filename)
                    print(f"replaced {info.filename}: {len(data)} bytes")
                else:
                    zout.writestr(info, zin.read(info.filename))

            # Any dex the rebuild produced that the original apk didn't have
            # an entry for at all (e.g. a rebuild that needed one more dex
            # file than the original did) still needs to be added, not just
            # skipped as "extra".
            for name in sorted(set(patched_dex) - written):
                data = patched_dex[name]
                new_info = zipfile.ZipInfo(name)
                new_info.compress_type = zipfile.ZIP_DEFLATED
                zout.writestr(new_info, data)
                print(f"added {name}: {len(data)} bytes (not in original apk)")

    print("done ->", out_path)


if __name__ == "__main__":
    main()
