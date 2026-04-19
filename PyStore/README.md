# PyStore

A Cydia-style PyPI app store for [Pyto](https://pyto.app) on iOS — built with Python `curses`, zero external dependencies.

## Usage

```bash
python3 PyStore/pystore.py
```

Or in Pyto on iOS, open `pystore.py` and tap Run.

## Navigation

| Key | Action |
|-----|--------|
| `↑` / `↓` or `j` / `k` | Move selection |
| `←` / `→` or `h` / `l` | Switch buttons (detail screen) |
| `Enter` / `Space` | Select / confirm |
| `1` | Home screen |
| `2` | Browse screen |
| `3` | Search screen |
| `Esc` / `b` / `Backspace` | Go back |
| `q` / `Q` | Quit |

## Screens

- **Home** — Featured banner + curated package list
- **Browse** — Categories (Tools, Web, Science, Data, Images, Crypto, Utilities, Testing)
- **Search** — Live PyPI search via XML-RPC; type query then press Enter
- **Detail** — Package info + Install / Uninstall buttons (runs `pip` in background thread)

## Requirements

- Python 3.8+
- `curses` (stdlib — included in Pyto)
- `pip` available via `sys.executable -m pip`
- Network access to `pypi.org`
