"""Command-line entry point: run an Attach (.at) script.

    python -m attach_lang.cli path/to/script.at
"""

from __future__ import annotations

import sys

from .errors import AttachError
from . import run_file


def main(argv: list[str] | None = None) -> int:
    argv = sys.argv[1:] if argv is None else argv
    if not argv:
        print("usage: python -m attach_lang.cli <script.at>", file=sys.stderr)
        return 1

    path = argv[0]
    try:
        run_file(path)
    except AttachError as exc:
        print(f"attach: {exc}", file=sys.stderr)
        return 1
    except FileNotFoundError:
        print(f"attach: no such file: {path}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
