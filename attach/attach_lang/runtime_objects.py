"""Values that only exist at runtime: symbols, conjured Things, modules,
and classes/instances. Nothing here is an AST node."""

from __future__ import annotations

from .errors import AttachRuntimeError


class Symbol:
    """A bareword tag, e.g. the ``PNG`` in ``Babel create PNG``."""

    def __init__(self, name: str):
        self.name = name

    def __eq__(self, other):
        return isinstance(other, Symbol) and other.name == self.name

    def __hash__(self):
        return hash(("Symbol", self.name))

    def __repr__(self):
        return self.name

    __str__ = __repr__


class Thing:
    """Something a module conjured out of nowhere via ``create``."""

    def __init__(self, kind, origin: str):
        self.kind = kind
        self.origin = origin

    def __repr__(self):
        return f"<{self.kind} conjured by {self.origin}>"

    __str__ = __repr__


class Module:
    """Every attached module can conjure things: Babel is just the
    flagship example, but any ``attach``ed name gets this for free."""

    def __init__(self, name: str):
        self.name = name

    def create(self, arg, interpreter):
        kind = arg.name if isinstance(arg, Symbol) else arg
        return Thing(kind=kind, origin=self.name)

    def __repr__(self):
        return f"<module {self.name}>"


class AttachClass:
    """A class defined with ``import class Name``. Its body already ran
    once at definition time; calling ``create`` on it re-runs the body in
    a fresh scope and hands back an Instance of the resulting bindings."""

    def __init__(self, name: str, body):
        self.name = name
        self.body = body

    def create(self, arg, interpreter):
        instance_scope: dict = {}
        interpreter.exec_block(self.body, instance_scope)
        return Instance(self.name, instance_scope)

    def __repr__(self):
        return f"<class {self.name}>"


class Instance:
    def __init__(self, class_name: str, fields: dict):
        self.class_name = class_name
        self.fields = fields

    def __repr__(self):
        return f"<{self.class_name} instance>"


def import_builtin(args: list, interpreter):
    """``Import <value>`` -- pass-through: hands back whatever it's given.
    Its role is purely narrative (importing a conjured value into the
    current expression), but it still requires an argument."""

    if not args:
        raise AttachRuntimeError("Import expects one argument")
    return args[0]
