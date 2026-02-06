"""Training stub.

This is a placeholder to define the expected data flow.

Next steps (to implement):
- Load exported receipt_labels.jsonl
- Create training samples (e.g., classify which OCR line is TOTAL)
- Train a small model and export to TFLite
"""

import json
from pathlib import Path


def load_jsonl(path: Path):
    for line in path.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line:
            continue
        yield json.loads(line)


def main():
    dataset = Path("receipt_labels.jsonl")
    if not dataset.exists():
        raise SystemExit(
            "Missing receipt_labels.jsonl. Export it from the app (Riwayat -> Export Dataset) and place it here."
        )

    n = 0
    for row in load_jsonl(dataset):
        n += 1
        # just print a couple
        if n <= 3:
            print(row.keys())
    print("Loaded", n, "samples")


if __name__ == "__main__":
    main()
