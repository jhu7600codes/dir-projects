# Attach

A tiny, whimsical, indentation-based scripting language. Its whole
personality is: modules get **attached**, then **conjure things out of
nowhere** on request, and the only conditional in the language is
**grepping your own program's execution trace**.

This directory holds a real, working implementation (Python) that
reproduces the original sketch of the language exactly. Run it:

```sh
cd attach
python3 -m attach_lang.cli examples/babel_alang.at
```

```
idfk why it doesnt work babel is here
```

## The core idea

Attach has no `if`, no `while`, no braces, no colons. It has exactly one
control-flow primitive: `notice`.

Every statement Attach executes appends its **exact source line** to a
running **ledger** (think: a live log of what the program has done).
`notice "some text"` greps that ledger for `"some text"` and only runs
its indented block if something matched:

```
attach Babel
notice "Babel"
    print "found it"
```

`attach Babel` runs first, so the ledger now contains the line
`"attach Babel"`. When `notice "Babel"` checks, `"Babel"` is a substring
of that ledger line, so the match succeeds and `"found it"` prints.

Everything else in the language — indentation, blocks, assignment,
`create` — exists to build up an interesting ledger for `notice` to grep.

## Indentation = "this happens after / because of that"

Like Python, Attach uses indentation (spaces only — no tabs) instead of
braces. Unlike Python, there are no colons, and **any** statement may own
an indented block of children:

- By default, a statement's children just run **unconditionally**, right
  after it, in the same scope. This is purely for organizing code near
  the thing it depends on.
- `notice`'s children are the one exception: they're **conditional** on
  the grep match described above.

So indentation nesting in Attach means "this is part of what follows
that statement," not "this is a new lexical scope." All variables live in
one flat, program-wide scope.

## Statement forms

| Form | Meaning |
|---|---|
| `attach Name` | Attaches (imports) a module named `Name`. Adds `Name` to the built-in `AttachList`, and gives it a generic `create` ability if it isn't already a known builtin. |
| `import class Name` | Defines a class named `Name`. Its body runs immediately (like a script region), and the class is also stored so it can be re-instantiated later via `Name create <arg>`. |
| `Target create Arg` | Calls `Target`'s `create` behavior with `Arg`, producing a value out of nowhere. The result is remembered against **the line number this statement is on** (see `&N` below). Works on any attached module or defined class. |
| `notice "pattern"` | Greps the ledger built up so far for `pattern`. Runs its indented block only if found. |
| `print expr` | Evaluates `expr` and writes it out. |
| `expr to Ident` | Assignment: evaluates `expr`, binds it to `Ident`. Value-first, like "assign X to Y." |
| `Ident meadow expr` | Assignment: same as above, but target-first — "Ident, via meadow, becomes expr." |
| anything else | Evaluated as a bare expression (rarely useful on its own, but keeps the grammar total). |

## Expressions

- `"a string"` — string literal
- `42` — integer literal
- A bareword after `create` (e.g. `PNG` in `Babel create PNG`) is a
  **Symbol**, not a variable — it's a tag, not a lookup.
- A bareword anywhere else is a variable lookup (`AttachList` is always
  available as a builtin: the list of every module name attached so far).
- `&N` is a **line reference**: the value produced by whichever
  `create` statement sits on source line `N`. This is how Attach lets
  later lines reach back and grab an earlier line's conjured value.
- `Name arg1 arg2 ...` (more than one token, first token a bareword) is a
  function call. The only builtin function today is `Import`, which is a
  narrative pass-through: `Import &5` just hands back whatever line 5
  produced.

## Walking through the original sketch

```attach
1  attach Babel
2  attach ALang
3
4  import class BabelAlang
5      Babel create PNG
6          pngClass meadow Import &5
7      AttachList to ALangModList
8          notice "Babel"
9              print "idfk why it doesnt work babel is here"
10         notice "No Attach Modules Found"
11             print ALangModList
```

- Lines 1–2 attach `Babel` and `ALang`, logging `"attach Babel"` and
  `"attach ALang"` to the ledger.
- Line 4 opens the `BabelAlang` class; its body (lines 5–11) runs
  immediately.
- Line 5 has Babel conjure a `PNG` out of nowhere. The result is stashed
  against line number 5.
- Line 6's `&5` reaches back to that exact result and imports it into
  `pngClass`.
- Line 7 copies the running `AttachList` (`["Babel", "ALang"]`) into
  `ALangModList`.
- Line 8's `notice "Babel"` greps the ledger — which by now contains
  `"attach Babel"` and `"Babel create PNG"` — finds a match, and line 9
  prints the easter egg.
- Line 10's `notice "No Attach Modules Found"` greps for that exact
  phrase. Nothing in the ledger says that (nothing ever failed to find a
  module), so it never matches, and line 11 never runs.

Output:

```
idfk why it doesnt work babel is here
```

## Running scripts

```sh
cd attach
python3 -m attach_lang.cli examples/babel_alang.at
python3 -m attach_lang.cli examples/hello.at
```

## Running the tests

```sh
cd attach
python3 -m pytest tests/ -q
```

## Project layout

```
attach/
  attach_lang/
    lexer.py            # source text -> indentation tree -> tokens
    ast_nodes.py         # AST node dataclasses
    parser.py            # tokens -> AST
    runtime_objects.py   # Symbol, Thing, Module, AttachClass, Instance
    interpreter.py        # tree-walking evaluator + the ledger/notice logic
    cli.py                 # `python -m attach_lang.cli script.at`
  examples/
    babel_alang.at        # the original sketch, verbatim
    hello.at
  tests/
    test_interpreter.py
```

## Ideas for later

- Loops / a second control-flow primitive beyond `notice`.
- Real lexical scoping per class instance (today everything shares one
  flat, program-wide scope).
- More builtin functions besides `Import`.
- A REPL.
