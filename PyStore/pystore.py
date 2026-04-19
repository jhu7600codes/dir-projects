#!/usr/bin/env python3
"""PyStore — Cydia-style PyPI app store for Pyto on iOS."""

import curses
import curses.textpad
import json
import os
import subprocess
import sys
import threading
import urllib.error
import urllib.parse
import urllib.request
import xmlrpc.client
from textwrap import wrap

# ── Colour palette (dark Cydia-ish theme) ─────────────────────────────────────
C_BG        = 0   # default terminal bg (black)
C_HEADER    = 1   # bright cyan on black
C_BANNER    = 2   # black on cyan (featured banner)
C_SELECTED  = 3   # black on yellow (highlight)
C_SECTION   = 4   # yellow on black (section headers)
C_DIM       = 5   # dark grey text
C_GREEN     = 6   # installed badge
C_RED       = 7   # error / uninstall badge
C_WHITE     = 8   # normal text

def init_colors():
    curses.start_color()
    curses.use_default_colors()
    curses.init_pair(C_HEADER,   curses.COLOR_CYAN,    -1)
    curses.init_pair(C_BANNER,   curses.COLOR_BLACK,   curses.COLOR_CYAN)
    curses.init_pair(C_SELECTED, curses.COLOR_BLACK,   curses.COLOR_YELLOW)
    curses.init_pair(C_SECTION,  curses.COLOR_YELLOW,  -1)
    curses.init_pair(C_DIM,      8,                    -1)   # dark grey
    curses.init_pair(C_GREEN,    curses.COLOR_GREEN,   -1)
    curses.init_pair(C_RED,      curses.COLOR_RED,     -1)
    curses.init_pair(C_WHITE,    curses.COLOR_WHITE,   -1)

# ── PyPI helpers ───────────────────────────────────────────────────────────────
PYPI_JSON   = "https://pypi.org/pypi/{}/json"
XMLRPC_URL  = "https://pypi.org/pypi"

FEATURED = [
    "requests", "numpy", "flask", "pillow", "pandas",
    "scipy", "matplotlib", "click", "rich", "httpx",
]

CATEGORIES = {
    "Tools & CLI":   ["click", "rich", "typer", "colorama", "tqdm", "pydantic"],
    "Web & Network": ["requests", "flask", "fastapi", "httpx", "aiohttp", "beautifulsoup4"],
    "Science & Math":["numpy", "scipy", "sympy", "statsmodels", "networkx", "numba"],
    "Data":          ["pandas", "polars", "pyarrow", "openpyxl", "xlrd", "tabulate"],
    "Images & Media":["pillow", "wand", "imageio", "moviepy", "qrcode"],
    "Crypto & Sec":  ["cryptography", "pynacl", "bcrypt", "paramiko", "pyotp"],
    "Utilities":     ["python-dateutil", "pytz", "tzdata", "humanize", "more-itertools"],
    "Testing":       ["pytest", "hypothesis", "coverage", "faker", "responses"],
}

_pkg_cache = {}

def fetch_package(name):
    if name in _pkg_cache:
        return _pkg_cache[name]
    try:
        url = PYPI_JSON.format(urllib.parse.quote(name))
        with urllib.request.urlopen(url, timeout=8) as r:
            data = json.loads(r.read())
        info = data["info"]
        pkg = {
            "name":        info.get("name", name),
            "version":     info.get("version", "?"),
            "summary":     info.get("summary", ""),
            "author":      info.get("author", ""),
            "home_page":   info.get("home_page", ""),
            "license":     info.get("license", ""),
            "description": info.get("description", "")[:2000],
        }
        _pkg_cache[name] = pkg
        return pkg
    except Exception as e:
        return {"name": name, "version": "?", "summary": str(e),
                "author": "", "home_page": "", "license": "", "description": ""}

def search_pypi(query, max_results=20):
    """Search PyPI via XML-RPC (keyword search)."""
    try:
        client = xmlrpc.client.ServerProxy(XMLRPC_URL)
        hits = client.search({"name": query, "summary": query}, "or")
        seen = {}
        for h in hits:
            n = h.get("name", "")
            if n and n not in seen:
                seen[n] = h
        results = list(seen.values())[:max_results]
        return [{"name": r["name"], "version": r.get("version", ""),
                 "summary": r.get("summary", "")} for r in results]
    except Exception as e:
        return [{"name": "Error", "version": "", "summary": str(e)}]

def get_installed():
    """Return set of installed package names (lowercase)."""
    try:
        out = subprocess.check_output(
            [sys.executable, "-m", "pip", "list", "--format=json"],
            stderr=subprocess.DEVNULL, timeout=10
        )
        return {p["name"].lower() for p in json.loads(out)}
    except Exception:
        return set()

def pip_install(name, callback):
    def run():
        try:
            out = subprocess.check_output(
                [sys.executable, "-m", "pip", "install", name, "--user"],
                stderr=subprocess.STDOUT, timeout=120
            )
            callback(True, out.decode(errors="replace"))
        except subprocess.CalledProcessError as e:
            callback(False, e.output.decode(errors="replace"))
        except Exception as e:
            callback(False, str(e))
    threading.Thread(target=run, daemon=True).start()

def pip_uninstall(name, callback):
    def run():
        try:
            out = subprocess.check_output(
                [sys.executable, "-m", "pip", "uninstall", name, "-y"],
                stderr=subprocess.STDOUT, timeout=60
            )
            callback(True, out.decode(errors="replace"))
        except subprocess.CalledProcessError as e:
            callback(False, e.output.decode(errors="replace"))
        except Exception as e:
            callback(False, str(e))
    threading.Thread(target=run, daemon=True).start()

# ── Drawing helpers ────────────────────────────────────────────────────────────
def safe_addstr(win, y, x, text, attr=0):
    h, w = win.getmaxyx()
    if y < 0 or y >= h or x < 0:
        return
    avail = w - x - 1
    if avail <= 0:
        return
    try:
        win.addstr(y, x, text[:avail], attr)
    except curses.error:
        pass

def draw_header(win, title):
    h, w = win.getmaxyx()
    win.attron(curses.color_pair(C_HEADER) | curses.A_BOLD)
    win.hline(0, 0, " ", w)
    safe_addstr(win, 0, 2, f"  PyStore  |  {title}", curses.color_pair(C_HEADER) | curses.A_BOLD)
    win.attroff(curses.color_pair(C_HEADER) | curses.A_BOLD)
    win.hline(1, 0, curses.ACS_HLINE, w, curses.color_pair(C_HEADER))

def draw_footer(win, hints):
    h, w = win.getmaxyx()
    win.attron(curses.color_pair(C_DIM))
    win.hline(h - 1, 0, " ", w)
    safe_addstr(win, h - 1, 1, hints[:w - 2], curses.color_pair(C_DIM))
    win.attroff(curses.color_pair(C_DIM))

def draw_row(win, y, text, selected=False, badge=None, badge_color=C_GREEN, dim=False):
    h, w = win.getmaxyx()
    if y < 0 or y >= h:
        return
    attr = curses.color_pair(C_SELECTED) | curses.A_BOLD if selected else (
           curses.color_pair(C_DIM) if dim else curses.color_pair(C_WHITE))
    win.hline(y, 0, " ", w, attr)
    safe_addstr(win, y, 2, text, attr)
    if badge:
        bx = w - len(badge) - 3
        if bx > 0:
            badge_attr = curses.color_pair(badge_color) | curses.A_BOLD
            safe_addstr(win, y, bx, f"[{badge}]", badge_attr if not selected else curses.color_pair(C_SELECTED))

def draw_section(win, y, text):
    h, w = win.getmaxyx()
    if y < 0 or y >= h:
        return
    win.hline(y, 0, " ", w, curses.color_pair(C_SECTION))
    safe_addstr(win, y, 1, f" {text.upper()} ", curses.color_pair(C_SECTION) | curses.A_BOLD)

# ── App state ──────────────────────────────────────────────────────────────────
class PyStore:
    SCREENS = ["home", "browse", "search", "detail"]

    def __init__(self, stdscr):
        self.scr = stdscr
        self.screen = "home"
        self.prev_screen = None
        self.installed = get_installed()
        self.status_msg = ""
        self.status_ok = True

        # home
        self.home_sel = 0
        self.home_offset = 0

        # browse
        self.cat_list  = list(CATEGORIES.keys())
        self.browse_sel = 0
        self.browse_offset = 0
        self.browse_in_cat = False
        self.cat_pkgs = []
        self.cat_pkg_sel = 0
        self.cat_pkg_offset = 0

        # search
        self.search_query = ""
        self.search_results = []
        self.search_sel = 0
        self.search_offset = 0
        self.search_state = "input"  # input | results | loading

        # detail
        self.detail_pkg = None
        self.detail_sel = 0  # 0=install/uninstall, 1=back
        self.install_log = ""
        self.installing = False

    def _content_rows(self):
        h, _ = self.scr.getmaxyx()
        return h - 4  # header(2) + footer(1) + 1

    def set_status(self, msg, ok=True):
        self.status_msg = msg
        self.status_ok = ok

    # ── Screens ────────────────────────────────────────────────────────────────
    def draw(self):
        self.scr.erase()
        if self.screen == "home":
            self.draw_home()
        elif self.screen == "browse":
            self.draw_browse()
        elif self.screen == "search":
            self.draw_search()
        elif self.screen == "detail":
            self.draw_detail()
        self.scr.refresh()

    # HOME ─────────────────────────────────────────────────────────────────────
    def draw_home(self):
        win = self.scr
        h, w = win.getmaxyx()
        draw_header(win, "Home")

        # Banner
        banner = " ★  PyStore — Python packages for iOS  ★ "
        bx = max(0, (w - len(banner)) // 2)
        win.hline(2, 0, " ", w, curses.color_pair(C_BANNER))
        safe_addstr(win, 2, bx, banner, curses.color_pair(C_BANNER) | curses.A_BOLD)

        # Nav tabs row
        tabs = ["[1] Home", "[2] Browse", "[3] Search"]
        tx = 1
        for t in tabs:
            safe_addstr(win, 3, tx, t, curses.color_pair(C_DIM))
            tx += len(t) + 3

        # Section header
        draw_section(win, 4, "Featured Packages")

        rows = self._content_rows() - 3
        for i in range(rows):
            idx = i + self.home_offset
            if idx >= len(FEATURED):
                break
            name = FEATURED[idx]
            selected = (idx == self.home_sel)
            badge = "installed" if name.lower() in self.installed else None
            draw_row(win, 5 + i, f"  {name}", selected, badge)

        draw_footer(win, "↑↓/num select  ENTER=detail  2=Browse  3=Search  Q=quit")
        if self.status_msg:
            col = C_GREEN if self.status_ok else C_RED
            safe_addstr(win, h - 2, 1, self.status_msg[:w - 2], curses.color_pair(col))

    def handle_home(self, key):
        max_i = len(FEATURED) - 1
        rows = self._content_rows() - 3
        if key in (curses.KEY_DOWN, ord('j')):
            if self.home_sel < max_i:
                self.home_sel += 1
                if self.home_sel >= self.home_offset + rows:
                    self.home_offset += 1
        elif key in (curses.KEY_UP, ord('k')):
            if self.home_sel > 0:
                self.home_sel -= 1
                if self.home_sel < self.home_offset:
                    self.home_offset -= 1
        elif key in (curses.KEY_ENTER, 10, 13, ord(' ')):
            self._open_detail(FEATURED[self.home_sel])
        elif key == ord('2'):
            self.screen = "browse"
        elif key == ord('3'):
            self.screen = "search"

    # BROWSE ───────────────────────────────────────────────────────────────────
    def draw_browse(self):
        win = self.scr
        h, w = win.getmaxyx()
        draw_header(win, "Browse")
        tabs = ["[1] Home", "[2] Browse", "[3] Search"]
        tx = 1
        for t in tabs:
            safe_addstr(win, 2, tx, t, curses.color_pair(C_DIM))
            tx += len(t) + 3

        if not self.browse_in_cat:
            draw_section(win, 3, "Categories")
            rows = self._content_rows() - 2
            for i in range(rows):
                idx = i + self.browse_offset
                if idx >= len(self.cat_list):
                    break
                cat = self.cat_list[idx]
                count = len(CATEGORIES[cat])
                selected = (idx == self.browse_sel)
                draw_row(win, 4 + i, f"  {cat}", selected,
                         badge=str(count), badge_color=C_DIM)
            draw_footer(win, "↑↓ select  ENTER=open  1=Home  3=Search  Q=quit")
        else:
            cat = self.cat_list[self.browse_sel]
            draw_section(win, 3, cat)
            rows = self._content_rows() - 2
            for i in range(rows):
                idx = i + self.cat_pkg_offset
                if idx >= len(self.cat_pkgs):
                    break
                name = self.cat_pkgs[idx]
                selected = (idx == self.cat_pkg_sel)
                badge = "installed" if name.lower() in self.installed else None
                draw_row(win, 4 + i, f"  {name}", selected, badge)
            draw_footer(win, "↑↓ select  ENTER=detail  ESC=categories  Q=quit")

        if self.status_msg:
            col = C_GREEN if self.status_ok else C_RED
            safe_addstr(win, h - 2, 1, self.status_msg[:w - 2], curses.color_pair(col))

    def handle_browse(self, key):
        if not self.browse_in_cat:
            rows = self._content_rows() - 2
            max_i = len(self.cat_list) - 1
            if key in (curses.KEY_DOWN, ord('j')):
                if self.browse_sel < max_i:
                    self.browse_sel += 1
                    if self.browse_sel >= self.browse_offset + rows:
                        self.browse_offset += 1
            elif key in (curses.KEY_UP, ord('k')):
                if self.browse_sel > 0:
                    self.browse_sel -= 1
                    if self.browse_sel < self.browse_offset:
                        self.browse_offset -= 1
            elif key in (curses.KEY_ENTER, 10, 13, ord(' ')):
                cat = self.cat_list[self.browse_sel]
                self.cat_pkgs = CATEGORIES[cat]
                self.cat_pkg_sel = 0
                self.cat_pkg_offset = 0
                self.browse_in_cat = True
            elif key == ord('1'):
                self.screen = "home"
            elif key == ord('3'):
                self.screen = "search"
        else:
            rows = self._content_rows() - 2
            max_i = len(self.cat_pkgs) - 1
            if key in (curses.KEY_DOWN, ord('j')):
                if self.cat_pkg_sel < max_i:
                    self.cat_pkg_sel += 1
                    if self.cat_pkg_sel >= self.cat_pkg_offset + rows:
                        self.cat_pkg_offset += 1
            elif key in (curses.KEY_UP, ord('k')):
                if self.cat_pkg_sel > 0:
                    self.cat_pkg_sel -= 1
                    if self.cat_pkg_sel < self.cat_pkg_offset:
                        self.cat_pkg_offset -= 1
            elif key in (curses.KEY_ENTER, 10, 13, ord(' ')):
                self._open_detail(self.cat_pkgs[self.cat_pkg_sel])
            elif key in (27, curses.KEY_BACKSPACE, ord('b')):
                self.browse_in_cat = False

    # SEARCH ───────────────────────────────────────────────────────────────────
    def draw_search(self):
        win = self.scr
        h, w = win.getmaxyx()
        draw_header(win, "Search")
        tabs = ["[1] Home", "[2] Browse", "[3] Search"]
        tx = 1
        for t in tabs:
            safe_addstr(win, 2, tx, t, curses.color_pair(C_DIM))
            tx += len(t) + 3

        # Search box
        box_w = min(w - 6, 50)
        safe_addstr(win, 3, 2, "Search PyPI: ", curses.color_pair(C_WHITE) | curses.A_BOLD)
        safe_addstr(win, 3, 15, "[" + self.search_query.ljust(box_w) + "]",
                    curses.color_pair(C_SELECTED) if self.search_state == "input"
                    else curses.color_pair(C_DIM))

        if self.search_state == "loading":
            safe_addstr(win, 5, 2, "Searching PyPI…", curses.color_pair(C_DIM))
        elif self.search_state == "results":
            draw_section(win, 4, f"Results for '{self.search_query}'")
            rows = self._content_rows() - 3
            for i in range(rows):
                idx = i + self.search_offset
                if idx >= len(self.search_results):
                    break
                r = self.search_results[idx]
                name = r["name"]
                summary = r.get("summary", "")
                selected = (idx == self.search_sel)
                badge = "installed" if name.lower() in self.installed else None
                label = f"  {name:<20}  {summary[:w - 28]}"
                draw_row(win, 5 + i, label, selected, badge)
            if not self.search_results:
                safe_addstr(win, 5, 2, "No results.", curses.color_pair(C_DIM))

        draw_footer(win, "Type query  ENTER=search/select  ESC=clear  1=Home  2=Browse  Q=quit")
        if self.status_msg:
            col = C_GREEN if self.status_ok else C_RED
            safe_addstr(win, h - 2, 1, self.status_msg[:w - 2], curses.color_pair(col))

    def handle_search(self, key):
        if self.search_state == "input":
            if key in (curses.KEY_ENTER, 10, 13) and self.search_query:
                self._do_search()
            elif key in (curses.KEY_BACKSPACE, 127, 8):
                self.search_query = self.search_query[:-1]
            elif key == 27:
                self.search_query = ""
                self.search_results = []
            elif key == ord('1'):
                self.screen = "home"
            elif key == ord('2'):
                self.screen = "browse"
            elif 32 <= key <= 126:
                self.search_query += chr(key)
        elif self.search_state == "results":
            rows = self._content_rows() - 3
            max_i = len(self.search_results) - 1
            if key in (curses.KEY_DOWN, ord('j')):
                if self.search_sel < max_i:
                    self.search_sel += 1
                    if self.search_sel >= self.search_offset + rows:
                        self.search_offset += 1
            elif key in (curses.KEY_UP, ord('k')):
                if self.search_sel > 0:
                    self.search_sel -= 1
                    if self.search_sel < self.search_offset:
                        self.search_offset -= 1
            elif key in (curses.KEY_ENTER, 10, 13, ord(' ')):
                if self.search_results:
                    self._open_detail(self.search_results[self.search_sel]["name"])
            elif key in (27, ord('b')):
                self.search_state = "input"
            elif key == ord('1'):
                self.screen = "home"
            elif key == ord('2'):
                self.screen = "browse"

    def _do_search(self):
        self.search_state = "loading"
        self.set_status("")
        self.draw()
        query = self.search_query

        def run():
            results = search_pypi(query)
            self.search_results = results
            self.search_sel = 0
            self.search_offset = 0
            self.search_state = "results"
            # wake up getch
            curses.ungetch(0)

        threading.Thread(target=run, daemon=True).start()

    # DETAIL ───────────────────────────────────────────────────────────────────
    def draw_detail(self):
        win = self.scr
        h, w = win.getmaxyx()
        pkg = self.detail_pkg
        if not pkg:
            return
        draw_header(win, pkg["name"])

        y = 2
        name_line = f"  {pkg['name']}  v{pkg['version']}"
        safe_addstr(win, y, 0, name_line, curses.color_pair(C_WHITE) | curses.A_BOLD)
        y += 1

        if pkg["author"]:
            safe_addstr(win, y, 2, f"by {pkg['author']}", curses.color_pair(C_DIM))
            y += 1

        if pkg["license"]:
            safe_addstr(win, y, 2, f"License: {pkg['license']}", curses.color_pair(C_DIM))
            y += 1

        win.hline(y, 0, curses.ACS_HLINE, w, curses.color_pair(C_DIM))
        y += 1

        # Summary / description
        desc = pkg["summary"] or pkg["description"] or "No description."
        for line in wrap(desc, w - 4)[:6]:
            if y >= h - 5:
                break
            safe_addstr(win, y, 2, line, curses.color_pair(C_WHITE))
            y += 1

        y += 1
        if y < h - 3:
            win.hline(y, 0, curses.ACS_HLINE, w, curses.color_pair(C_DIM))
            y += 1

        # Buttons
        is_inst = pkg["name"].lower() in self.installed
        if self.installing:
            safe_addstr(win, y, 2, "Installing…", curses.color_pair(C_DIM))
        else:
            btn_install = "  Uninstall  " if is_inst else "  Install   "
            btn_back    = "  ← Back    "
            inst_col = C_RED if is_inst else C_GREEN
            inst_sel = (self.detail_sel == 0)
            back_sel = (self.detail_sel == 1)

            safe_addstr(win, y, 2, btn_install,
                        (curses.color_pair(C_SELECTED) if inst_sel
                         else curses.color_pair(inst_col)) | curses.A_BOLD)
            safe_addstr(win, y, 2 + len(btn_install) + 2, btn_back,
                        (curses.color_pair(C_SELECTED) if back_sel
                         else curses.color_pair(C_DIM)) | curses.A_BOLD)

        # Install log
        if self.install_log:
            y += 2
            for line in self.install_log.splitlines()[-4:]:
                if y >= h - 1:
                    break
                safe_addstr(win, y, 2, line[:w - 4], curses.color_pair(C_DIM))
                y += 1

        draw_footer(win, "←→ select button  ENTER=confirm  ESC/B=back  Q=quit")
        if self.status_msg:
            col = C_GREEN if self.status_ok else C_RED
            safe_addstr(win, h - 2, 1, self.status_msg[:w - 2], curses.color_pair(col))

    def handle_detail(self, key):
        if self.installing:
            return
        if key in (curses.KEY_LEFT, ord('h')):
            self.detail_sel = 0
        elif key in (curses.KEY_RIGHT, ord('l')):
            self.detail_sel = 1
        elif key in (curses.KEY_ENTER, 10, 13, ord(' ')):
            if self.detail_sel == 1:
                self._go_back()
            else:
                pkg = self.detail_pkg
                is_inst = pkg["name"].lower() in self.installed
                self.installing = True
                self.install_log = ""
                self.set_status("Working…")
                self.draw()
                if is_inst:
                    pip_uninstall(pkg["name"], self._install_done)
                else:
                    pip_install(pkg["name"], self._install_done)
        elif key in (27, ord('b'), curses.KEY_BACKSPACE):
            self._go_back()

    def _install_done(self, ok, log):
        self.installing = False
        self.install_log = log
        self.installed = get_installed()
        pkg = self.detail_pkg
        if ok:
            action = "Removed" if pkg["name"].lower() not in self.installed else "Installed"
            self.set_status(f"{action} {pkg['name']} successfully.", ok=True)
        else:
            self.set_status(f"Failed: check log below.", ok=False)
        curses.ungetch(0)

    # helpers ──────────────────────────────────────────────────────────────────
    def _open_detail(self, name):
        self.set_status("Loading…")
        self.draw()
        self.detail_pkg = fetch_package(name)
        self.detail_sel = 0
        self.install_log = ""
        self.installing = False
        self.prev_screen = self.screen
        self.screen = "detail"
        self.set_status("")

    def _go_back(self):
        self.screen = self.prev_screen or "home"
        self.set_status("")

    # ── Main loop ──────────────────────────────────────────────────────────────
    def run(self):
        self.scr.keypad(True)
        curses.cbreak()
        curses.noecho()
        try:
            curses.curs_set(0)
        except curses.error:
            pass

        while True:
            self.draw()
            key = self.scr.getch()

            # Global quit
            if key in (ord('q'), ord('Q')):
                break

            # Global tab shortcuts from any screen
            if self.screen != "detail":
                if key == ord('1'):
                    self.screen = "home"
                    continue
                elif key == ord('2'):
                    self.screen = "browse"
                    continue
                elif key == ord('3'):
                    self.screen = "search"
                    continue

            if self.screen == "home":
                self.handle_home(key)
            elif self.screen == "browse":
                self.handle_browse(key)
            elif self.screen == "search":
                self.handle_search(key)
            elif self.screen == "detail":
                self.handle_detail(key)


# ── Entry point ────────────────────────────────────────────────────────────────
def main(stdscr):
    init_colors()
    app = PyStore(stdscr)
    app.run()

if __name__ == "__main__":
    curses.wrapper(main)
