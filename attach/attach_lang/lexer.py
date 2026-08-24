"""Turns Attach source text into (1) an indentation tree of raw lines and
(2) a flat token stream for each line.

Attach is indentation-based (spaces only, like Python) but has no colons --
*any* statement may own an indented block of children. What the children
mean depends on the parent statement (see interpreter.py): by default they
just run right after their parent, but ``notice`` treats them as
conditional on a grep match against the run's ledger.
"""

from __future__ import annotations

from dataclasses import dataclass, field

from .errors import AttachSyntaxError


@dataclass
class RawLine:
    indent: int
    text: str
    lineno: int
    children: list["RawLine"] = field(default_factory=list)


def build_tree(source: str) -> list[RawLine]:
    """Parse source text into a tree of RawLine nodes using the off-side
    (indentation) rule. Blank lines and ``#`` comment lines are skipped."""

    root: list[RawLine] = []
    stack: list[tuple[int, list[RawLine]]] = [(-1, root)]

    for lineno, raw in enumerate(source.splitlines(), start=1):
        stripped = raw.strip()
        if not stripped or stripped.startswith("#"):
            continue

        leading = raw[: len(raw) - len(raw.lstrip(" "))]
        if "\t" in raw[: len(raw) - len(raw.lstrip())]:
            raise AttachSyntaxError(f"line {lineno}: tabs are not allowed for indentation")
        indent = len(leading)

        node = RawLine(indent=indent, text=stripped, lineno=lineno)

        while indent <= stack[-1][0]:
            stack.pop()
        stack[-1][1].append(node)
        stack.append((indent, node.children))

    return root


# A token is (kind, value) where kind is "WORD" or "STR".
Token = tuple[str, str]


def tokenize(text: str) -> list[Token]:
    """Split one statement's text into WORD / STR tokens. Whitespace
    separates words; double-quoted spans become single STR tokens."""

    tokens: list[Token] = []
    i, n = 0, len(text)
    while i < n:
        c = text[i]
        if c.isspace():
            i += 1
            continue
        if c == '"':
            j = i + 1
            buf = []
            while j < n and text[j] != '"':
                buf.append(text[j])
                j += 1
            if j >= n:
                raise AttachSyntaxError(f"unterminated string in: {text!r}")
            tokens.append(("STR", "".join(buf)))
            i = j + 1
            continue
        j = i
        while j < n and not text[j].isspace():
            j += 1
        tokens.append(("WORD", text[i:j]))
        i = j
    return tokens
