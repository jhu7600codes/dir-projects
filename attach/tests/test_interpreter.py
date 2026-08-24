import os
import sys

sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))

from attach_lang import run_source, run_file
from attach_lang.errors import AttachRuntimeError, AttachSyntaxError

import pytest

EXAMPLES = os.path.join(os.path.dirname(__file__), "..", "examples")


def test_babel_alang_example_matches_the_original_sketch():
    out = run_file(os.path.join(EXAMPLES, "babel_alang.at"))
    assert out == ["idfk why it doesnt work babel is here"]


def test_hello_example():
    out = run_file(os.path.join(EXAMPLES, "hello.at"))
    assert out == ["<GREETING conjured by Babel>"]


def test_notice_skips_body_when_pattern_never_ran():
    source = (
        "attach Foo\n"
        'notice "Bar"\n'
        '    print "should not appear"\n'
    )
    assert run_source(source) == []


def test_notice_runs_body_when_pattern_matches_prior_line():
    source = (
        "attach Foo\n"
        'notice "Foo"\n'
        '    print "matched"\n'
    )
    assert run_source(source) == ["matched"]


def test_line_ref_and_import_pull_a_prior_result_forward():
    source = (
        "attach Babel\n"
        "Babel create PNG\n"
        "    result meadow Import &2\n"
        "    print result\n"
    )
    out = run_source(source)
    assert out == ["<PNG conjured by Babel>"]


def test_to_assignment_and_attach_list():
    source = (
        "attach Babel\n"
        "attach ALang\n"
        "AttachList to Mods\n"
        "print Mods\n"
    )
    assert run_source(source) == ["[Babel, ALang]"]


def test_undefined_name_raises():
    with pytest.raises(AttachRuntimeError):
        run_source("print Nowhere\n")


def test_line_ref_to_a_non_producing_line_raises():
    source = "attach Babel\nprint &1\n"
    with pytest.raises(AttachRuntimeError):
        run_source(source)


def test_unterminated_string_is_a_syntax_error():
    with pytest.raises(AttachSyntaxError):
        run_source('notice "oops\n')


def test_tabs_in_indentation_are_rejected():
    with pytest.raises(AttachSyntaxError):
        run_source("attach Babel\n\tBabel create PNG\n")


def test_class_body_runs_once_and_is_reusable_via_create():
    source = (
        "attach Babel\n"
        "import class Greeter\n"
        "    Babel create HI\n"
        "        line meadow Import &3\n"
        "Greeter create ignored\n"
    )
    # Just needs to run without error -- the class body executes at
    # definition time, and `create` can re-run it for a fresh instance.
    run_source(source)
