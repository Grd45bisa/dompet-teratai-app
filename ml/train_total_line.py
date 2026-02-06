"""Baseline training: choose which OCR line contains the FINAL TOTAL.

Why this baseline:
- It's the most valuable field.
- It can be learned from weak labels derived from the user-confirmed `total`.

Input dataset:
- receipt_labels.jsonl (export from the app)

Output:
- model_total_line.joblib

This model is *not yet* exported to TFLite. Next step will be to port to a tiny TF/Keras model.
"""

from __future__ import annotations

import json
import re
from dataclasses import dataclass
from pathlib import Path

import numpy as np
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import classification_report
from sklearn.model_selection import train_test_split
from sklearn.pipeline import Pipeline


MONEY_RX = re.compile(r"(\d{1,3}([.,]\d{3})+|\d+)([.,]\d{2})?")


def load_jsonl(path: Path):
    for line in path.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line:
            continue
        yield json.loads(line)


def normalize_money(s: str) -> str:
    s = s.strip()
    if not s:
        return ""
    cleaned = "".join(ch for ch in s if ch.isdigit() or ch in ".,")
    if not cleaned:
        return ""

    last_comma = cleaned.rfind(",")
    last_dot = cleaned.rfind(".")

    if last_comma != -1 and last_dot != -1:
        # decimal is whichever is last
        if last_comma > last_dot:
            return cleaned.replace(".", "").replace(",", ".")
        return cleaned.replace(",", "")

    if last_comma != -1:
        parts = cleaned.split(",")
        if len(parts) == 2 and 1 <= len(parts[1]) <= 2:
            return parts[0].replace(".", "") + "." + parts[1]
        return cleaned.replace(",", "")

    if last_dot != -1:
        parts = cleaned.split(".")
        if len(parts) == 2 and 1 <= len(parts[1]) <= 2:
            return cleaned
        return cleaned.replace(".", "")

    return cleaned


def extract_candidates(line: str) -> list[str]:
    return [normalize_money(m.group(0)) for m in MONEY_RX.finditer(line)]


def weak_label_total_line(ocr_text: str, total_norm: str) -> tuple[list[str], list[int]]:
    """Return (lines, y) where y[i]=1 if line i contains total_norm."""
    lines = [ln.strip() for ln in ocr_text.splitlines() if ln.strip()]
    y = [0] * len(lines)
    for i, ln in enumerate(lines):
        cands = extract_candidates(ln)
        if any(c == total_norm for c in cands):
            y[i] = 1
    return lines, y


@dataclass
class SampleStats:
    receipts: int = 0
    lines: int = 0
    positive_lines: int = 0
    receipts_with_pos: int = 0


def build_dataset(rows) -> tuple[list[str], list[int], SampleStats]:
    X: list[str] = []
    y: list[int] = []
    st = SampleStats()

    for row in rows:
        st.receipts += 1
        ocr = row.get("ocrText", "") or ""
        total = normalize_money(row.get("total", "") or "")
        if not ocr or not total:
            continue

        lines, yy = weak_label_total_line(ocr, total)
        if not lines:
            continue

        st.lines += len(lines)
        st.positive_lines += int(sum(yy))
        if any(yy):
            st.receipts_with_pos += 1

        # Create per-line samples; include keyword hints
        for ln, label in zip(lines, yy):
            X.append(ln)
            y.append(int(label))

    return X, y, st


def main():
    dataset = Path("receipt_labels.jsonl")
    if not dataset.exists():
        raise SystemExit(
            "Missing receipt_labels.jsonl. Export it from the app (Riwayat -> Export Dataset) and place it in ml/."
        )

    rows = list(load_jsonl(dataset))
    X, y, st = build_dataset(rows)

    print("Receipts:", st.receipts)
    print("Receipts with at least one positive line:", st.receipts_with_pos)
    print("Lines:", st.lines)
    print("Positive lines:", st.positive_lines)

    if sum(y) < 20:
        raise SystemExit("Not enough positives to train. Collect more labeled receipts.")

    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=42, stratify=y
    )

    model: Pipeline = Pipeline(
        steps=[
            (
                "tfidf",
                TfidfVectorizer(
                    analyzer="char_wb",
                    ngram_range=(3, 5),
                    min_df=2,
                ),
            ),
            (
                "clf",
                LogisticRegression(
                    max_iter=200,
                    class_weight="balanced",
                    n_jobs=1,
                ),
            ),
        ]
    )

    model.fit(X_train, y_train)
    y_pred = model.predict(X_test)
    print(classification_report(y_test, y_pred, digits=4))

    try:
        import joblib

        out = Path("model_total_line.joblib")
        joblib.dump(model, out)
        print("Saved", out)
    except ImportError:
        print("joblib not installed; skipping save")


if __name__ == "__main__":
    main()
