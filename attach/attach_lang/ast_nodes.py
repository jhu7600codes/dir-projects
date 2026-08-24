"""AST node definitions for Attach.

Every statement node carries ``body`` (its indented children, possibly
empty) and ``raw_text``/``lineno`` (the exact source line it came from --
used both for the ``&N`` line-reference feature and for the ledger that
``notice`` greps against).
"""

from __future__ import annotations

from dataclasses import dataclass, field


# ---- expressions -----------------------------------------------------

class Expr:
    """Base class for expression nodes."""


@dataclass
class StrLit(Expr):
    value: str


@dataclass
class NumLit(Expr):
    value: float


@dataclass
class SymbolLit(Expr):
    """A bareword argument to ``create`` -- e.g. the ``PNG`` in
    ``Babel create PNG``. Evaluates to a Symbol, not a variable lookup."""

    name: str


@dataclass
class IdentRef(Expr):
    name: str


@dataclass
class LineRef(Expr):
    """``&N`` -- the value produced by executing source line N."""

    n: int


@dataclass
class Call(Expr):
    name: str
    args: list[Expr]


# ---- statements --------------------------------------------------------

class Stmt:
    """Base class for statement nodes."""


@dataclass
class Attach(Stmt):
    name: str
    body: list[Stmt]
    lineno: int
    raw_text: str


@dataclass
class ImportClass(Stmt):
    name: str
    body: list[Stmt]
    lineno: int
    raw_text: str


@dataclass
class CreateCall(Stmt):
    target: str
    arg: Expr
    body: list[Stmt]
    lineno: int
    raw_text: str


@dataclass
class Assign(Stmt):
    target: str
    expr: Expr
    body: list[Stmt]
    lineno: int
    raw_text: str


@dataclass
class Notice(Stmt):
    pattern: str
    body: list[Stmt]
    lineno: int
    raw_text: str


@dataclass
class Print(Stmt):
    expr: Expr | None
    body: list[Stmt]
    lineno: int
    raw_text: str


@dataclass
class ExprStmt(Stmt):
    expr: Expr | None
    body: list[Stmt]
    lineno: int
    raw_text: str
