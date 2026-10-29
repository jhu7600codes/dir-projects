#!/usr/bin/env python3
# prints every symbol exported by the .tbd stubs under an sdk, one per line,
# with objc classes/ivars expanded to their real linker names.
#   tbd-symbols.py SDKDIR
import os
import re
import sys

LIST = re.compile(r"([A-Za-z][\w-]*):\s*\[([^\]]*)\]", re.S)


def expand(key, name):
    name = name.strip().strip("'\"")
    if not name:
        return []
    if key == "objc-classes":
        base = name if name.startswith("_") else "_" + name
        return ["_OBJC_CLASS_$" + base, "_OBJC_METACLASS_$" + base]
    if key in ("objc-ivars",):
        base = name if name.startswith("_") else "_" + name
        return ["_OBJC_IVAR_$" + base]
    if key in ("objc-eh-types",):
        base = name if name.startswith("_") else "_" + name
        return ["_OBJC_EHTYPE_$" + base]
    if key in ("symbols", "weak-def-symbols", "thread-local-symbols", "weak-symbols"):
        return [name]
    return []


def main():
    out = set()
    for root, _dirs, files in os.walk(sys.argv[1], followlinks=True):
        for f in files:
            if not f.endswith(".tbd"):
                continue
            try:
                text = open(os.path.join(root, f), encoding="utf-8", errors="replace").read()
            except OSError:
                continue
            for key, body in LIST.findall(text):
                for name in body.split(","):
                    out.update(expand(key, name))
    sys.stdout.write("\n".join(sorted(out)) + "\n")


if __name__ == "__main__":
    main()
