"""Tree-walking interpreter for Attach.

The core idea: every executed statement's exact source line gets appended
to a running ``ledger`` (an execution trace). ``notice "pattern"`` is the
language's only conditional -- it greps the ledger built up *so far* for
``pattern`` and only runs its indented body if something matches. Every
other statement's indented children just run unconditionally, right after
their parent, in the same (single, flat, whole-program) scope.
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
from .errors import AttachRuntimeError
from .runtime_objects import AttachClass, Module, import_builtin

Scope = dict


class Interpreter:
    def __init__(self):
        self.builtins: dict = {"Import": import_builtin}
        self.attached_modules: list[str] = []
        self.classes: dict[str, AttachClass] = {}
        self.line_results: dict[int, object] = {}
        self.ledger: list[str] = []
        self.stdout: list[str] = []
        self.global_scope: Scope = {}

    # ---- driving a whole program -------------------------------------

    def run(self, program: list[Stmt]) -> list[str]:
        self.exec_block(program, self.global_scope)
        return self.stdout

    def exec_block(self, stmts: list[Stmt], scope: Scope) -> None:
        for stmt in stmts:
            self.exec_stmt(stmt, scope)

    # ---- statement dispatch -------------------------------------------

    def exec_stmt(self, stmt: Stmt, scope: Scope) -> None:
        if isinstance(stmt, Notice):
            matched = any(stmt.pattern in line for line in self.ledger)
            self.ledger.append(stmt.raw_text)
            if matched:
                self.exec_block(stmt.body, scope)
            return

        self.ledger.append(stmt.raw_text)

        if isinstance(stmt, Attach):
            self._do_attach(stmt, scope)
            self.exec_block(stmt.body, scope)
        elif isinstance(stmt, ImportClass):
            self._do_import_class(stmt, scope)
        elif isinstance(stmt, CreateCall):
            self._do_create(stmt, scope)
            self.exec_block(stmt.body, scope)
        elif isinstance(stmt, Assign):
            self._do_assign(stmt, scope)
            self.exec_block(stmt.body, scope)
        elif isinstance(stmt, Print):
            self._do_print(stmt, scope)
            self.exec_block(stmt.body, scope)
        elif isinstance(stmt, ExprStmt):
            if stmt.expr is not None:
                self.eval_expr(stmt.expr, scope)
            self.exec_block(stmt.body, scope)
        else:  # pragma: no cover - defensive
            raise AttachRuntimeError(f"unknown statement node: {stmt!r}")

    def _do_attach(self, stmt: Attach, scope: Scope) -> None:
        self.attached_modules.append(stmt.name)
        self.builtins.setdefault(stmt.name, Module(stmt.name))

    def _do_import_class(self, stmt: ImportClass, scope: Scope) -> None:
        # The class body runs once, immediately, in the shared scope (like
        # a script region) -- and is also stashed so `Name create ...`
        # can instantiate it again later, per-instance.
        self.exec_block(stmt.body, scope)
        cls = AttachClass(stmt.name, stmt.body)
        self.classes[stmt.name] = cls
        scope[stmt.name] = cls

    def _do_create(self, stmt: CreateCall, scope: Scope) -> None:
        target = self._lookup(stmt.target, scope)
        if not hasattr(target, "create"):
            raise AttachRuntimeError(
                f"line {stmt.lineno}: '{stmt.target}' has no 'create' behavior"
            )
        arg_val = self.eval_expr(stmt.arg, scope)
        result = target.create(arg_val, self)
        self.line_results[stmt.lineno] = result

    def _do_assign(self, stmt: Assign, scope: Scope) -> None:
        scope[stmt.target] = self.eval_expr(stmt.expr, scope)

    def _do_print(self, stmt: Print, scope: Scope) -> None:
        value = self.eval_expr(stmt.expr, scope) if stmt.expr is not None else ""
        text = self.stringify(value)
        self.stdout.append(text)
        print(text)

    # ---- expressions ----------------------------------------------------

    def eval_expr(self, expr: Expr, scope: Scope):
        if isinstance(expr, StrLit):
            return expr.value
        if isinstance(expr, NumLit):
            return expr.value
        if isinstance(expr, SymbolLit):
            from .runtime_objects import Symbol

            return Symbol(expr.name)
        if isinstance(expr, LineRef):
            if expr.n not in self.line_results:
                raise AttachRuntimeError(f"no value was produced on line {expr.n}")
            return self.line_results[expr.n]
        if isinstance(expr, IdentRef):
            return self._lookup(expr.name, scope)
        if isinstance(expr, Call):
            fn = self.builtins.get(expr.name)
            if fn is None:
                raise AttachRuntimeError(f"unknown function: {expr.name}")
            args = [self.eval_expr(a, scope) for a in expr.args]
            return fn(args, self)
        raise AttachRuntimeError(f"cannot evaluate expression: {expr!r}")  # pragma: no cover

    def _lookup(self, name: str, scope: Scope):
        if name == "AttachList":
            return list(self.attached_modules)
        if name in scope:
            return scope[name]
        if name in self.builtins:
            return self.builtins[name]
        raise AttachRuntimeError(f"undefined name: {name}")

    # ---- output -----------------------------------------------------------

    def stringify(self, value) -> str:
        if isinstance(value, str):
            return value
        if isinstance(value, list):
            return "[" + ", ".join(self.stringify(v) for v in value) + "]"
        return str(value)
