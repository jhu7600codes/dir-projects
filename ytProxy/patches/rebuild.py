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
import sys
import zipfile

DEX_NAMES = {"classes.dex", "classes2.dex", "classes3.dex", "classes4.dex"}


def main() -> None:
    if len(sys.argv) != 4:
        print(__doc__)
        raise SystemExit(1)
    orig_path, rebuilt_path, out_path = sys.argv[1:4]

    with zipfile.ZipFile(rebuilt_path) as zrebuilt:
        patched_dex = {name: zrebuilt.read(name) for name in DEX_NAMES}

    with zipfile.ZipFile(orig_path, "r") as zin:
        with zipfile.ZipFile(out_path, "w", zipfile.ZIP_DEFLATED) as zout:
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
                    print(f"replaced {info.filename}: {len(data)} bytes")
                else:
                    zout.writestr(info, zin.read(info.filename))

    print("done ->", out_path)


if __name__ == "__main__":
    main()
