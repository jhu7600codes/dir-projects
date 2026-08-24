"""Recursive-descent parser: RawLine tree -> Stmt AST.

Statement forms recognized (checked in this order):

    attach <Name>                          -- import/attach a module
    import class <Name>                    -- define a class
    notice "<pattern>"                     -- grep the ledger; run body if found
    print <expr>                           -- write a value out
    <expr...> to <Ident>                   -- assignment (value-first)
    <Ident> meadow <expr...>                -- assignment (target-first)
    <Ident> create <arg>                   -- call target.create(arg)
    <anything else>                        -- bare expression statement

Any statement may own an indented block of children; see interpreter.py
for what each statement type does with them.
"""

from __future__ import annotations

from .ast_nodes import (
    Assign,
    Attach,
    Call,
    CreateCall,
    Expr,
    ExprStmt,
    IdentRef,
    ImportClass,
    LineRef,
    Notice,
    NumLit,
    Print,
    Stmt,
    StrLit,
    SymbolLit,
)
from .errors import AttachSyntaxError
from .lexer import Token, RawLine, build_tree, tokenize


def parse_program(source: str) -> list[Stmt]:
    return [parse_stmt(node) for node in build_tree(source)]


def parse_stmt(node: RawLine) -> Stmt:
    toks = tokenize(node.text)
    if not toks:
        raise AttachSyntaxError(f"line {node.lineno}: empty statement")

    def body() -> list[Stmt]:
        return [parse_stmt(c) for c in node.children]

    head = toks[0]

    if head == ("WORD", "attach") and len(toks) == 2 and toks[1][0] == "WORD":
        return Attach(name=toks[1][1], body=body(), lineno=node.lineno, raw_text=node.text)

    if (
        head == ("WORD", "import")
        and len(toks) >= 3
        and toks[1] == ("WORD", "class")
        and toks[2][0] == "WORD"
    ):
        return ImportClass(name=toks[2][1], body=body(), lineno=node.lineno, raw_text=node.text)

    if head == ("WORD", "notice") and len(toks) == 2 and toks[1][0] == "STR":
        return Notice(pattern=toks[1][1], body=body(), lineno=node.lineno, raw_text=node.text)

    if head == ("WORD", "print"):
        expr = parse_expr(toks[1:], node.lineno)
        return Print(expr=expr, body=body(), lineno=node.lineno, raw_text=node.text)

    if len(toks) >= 3 and toks[-2] == ("WORD", "to") and toks[-1][0] == "WORD":
        target = toks[-1][1]
        expr = parse_expr(toks[:-2], node.lineno)
        return Assign(target=target, expr=expr, body=body(), lineno=node.lineno, raw_text=node.text)

    if len(toks) >= 3 and toks[0][0] == "WORD" and toks[1] == ("WORD", "meadow"):
        target = toks[0][1]
        expr = parse_expr(toks[2:], node.lineno)
        return Assign(target=target, expr=expr, body=body(), lineno=node.lineno, raw_text=node.text)

    if len(toks) >= 3 and toks[0][0] == "WORD" and toks[1] == ("WORD", "create"):
        target = toks[0][1]
        arg_toks = toks[2:]
        if len(arg_toks) == 1 and arg_toks[0][0] == "WORD":
            arg: Expr = SymbolLit(arg_toks[0][1])
        else:
            arg = parse_expr(arg_toks, node.lineno)
        return CreateCall(target=target, arg=arg, body=body(), lineno=node.lineno, raw_text=node.text)

    expr = parse_expr(toks, node.lineno)
    return ExprStmt(expr=expr, body=body(), lineno=node.lineno, raw_text=node.text)


def parse_expr(toks: list[Token], lineno: int) -> Expr | None:
    if not toks:
        return None
    if len(toks) == 1:
        return parse_atom(toks[0], lineno)
    head_kind, head_val = toks[0]
    if head_kind == "WORD":
        return Call(name=head_val, args=[parse_atom(t, lineno) for t in toks[1:]])
    raise AttachSyntaxError(f"line {lineno}: cannot parse expression starting with {toks[0]!r}")


def parse_atom(tok: Token, lineno: int) -> Expr:
    kind, val = tok
    if kind == "STR":
        return StrLit(val)
    if val.startswith("&") and val[1:].isdigit():
        return LineRef(int(val[1:]))
    stripped = val[1:] if val.startswith("-") else val
    if stripped.isdigit():
        return NumLit(int(val))
    return IdentRef(val)
