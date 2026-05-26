"""Add scripts/ to sys.path so entry points can `from lib import ...`."""
from __future__ import annotations

import sys
from pathlib import Path

SCRIPTS_ROOT = Path(__file__).resolve().parent


def bootstrap() -> None:
    root = str(SCRIPTS_ROOT)
    if root not in sys.path:
        sys.path.insert(0, root)
