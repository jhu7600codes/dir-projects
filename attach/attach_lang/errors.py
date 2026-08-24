"""Exception types for the Attach language."""


class AttachError(Exception):
    """Base class for all Attach language errors."""


class AttachSyntaxError(AttachError):
    """Raised while turning source text into an AST."""


class AttachRuntimeError(AttachError):
    """Raised while executing a parsed Attach program."""
