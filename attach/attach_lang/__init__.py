"""Attach: a tiny, whimsical, indentation-based language where the only
conditional is `notice`, which greps the program's own execution trace."""

from .interpreter import Interpreter
from .parser import parse_program


def run_source(source: str) -> list[str]:
    """Parse and run Attach source text, returning the printed lines."""
    program = parse_program(source)
    interp = Interpreter()
    interp.run(program)
    return interp.stdout


def run_file(path: str) -> list[str]:
    with open(path, "r", encoding="utf-8") as f:
        return run_source(f.read())


__all__ = ["Interpreter", "parse_program", "run_source", "run_file"]
