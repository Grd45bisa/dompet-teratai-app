"""Train a tiny TensorFlow model to pick TOTAL line from OCR.

This creates an on-device-friendly model:
- TextVectorization (character n-gram TF-IDF)
- Small dense head

Outputs:
- total_line_model_saved/ (SavedModel)
- total_line_model.tflite (TFLite)

Dataset:
- receipt_labels.jsonl (export from app)

NOTE: This is a baseline. Itemization comes later.
"""

from __future__ import annotations

import json
import re
from pathlib import Path

import numpy as np
import tensorflow as tf

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


def build_samples(rows) -> tuple[list[str], list[int]]:
    X: list[str] = []
    y: list[int] = []

    for row in rows:
        ocr = row.get("ocrText", "") or ""
        total = normalize_money(row.get("total", "") or "")
        if not ocr or not total:
            continue

        lines = [ln.strip() for ln in ocr.splitlines() if ln.strip()]
        for ln in lines:
            cands = extract_candidates(ln)
            label = 1 if any(c == total for c in cands) else 0
            X.append(ln)
            y.append(label)

    return X, y


def main():
    tf.random.set_seed(42)

    dataset = Path("receipt_labels.jsonl")
    if not dataset.exists():
        raise SystemExit("Put receipt_labels.jsonl into ml/ first")

    rows = list(load_jsonl(dataset))
    X, y = build_samples(rows)

    X = np.array(X, dtype=object)
    y = np.array(y, dtype=np.int32)

    pos = int(y.sum())
    if pos < 50:
        raise SystemExit(f"Not enough positives ({pos}). Collect more labeled receipts.")

    # train/test split
    idx = np.arange(len(X))
    rng = np.random.default_rng(42)
    rng.shuffle(idx)
    split = int(0.8 * len(idx))
    tr, te = idx[:split], idx[split:]

    X_train, y_train = X[tr], y[tr]
    X_test, y_test = X[te], y[te]

    # Class weights for imbalance
    neg = int((y_train == 0).sum())
    pos = int((y_train == 1).sum())
    class_weight = {0: 1.0, 1: (neg / max(pos, 1))}

    vectorizer = tf.keras.layers.TextVectorization(
        output_mode="tf-idf",
        split="character",
        ngrams=5,
        max_tokens=20000,
    )
    vectorizer.adapt(tf.data.Dataset.from_tensor_slices(X_train).batch(256))

    inputs = tf.keras.Input(shape=(1,), dtype=tf.string, name="line")
    x = vectorizer(inputs)
    x = tf.keras.layers.Dense(64, activation="relu")(x)
    x = tf.keras.layers.Dropout(0.2)(x)
    outputs = tf.keras.layers.Dense(1, activation="sigmoid", name="p_total")(x)

    model = tf.keras.Model(inputs=inputs, outputs=outputs)
    model.compile(
        optimizer=tf.keras.optimizers.Adam(1e-3),
        loss="binary_crossentropy",
        metrics=[
            tf.keras.metrics.AUC(name="auc"),
            tf.keras.metrics.Precision(name="precision"),
            tf.keras.metrics.Recall(name="recall"),
        ],
    )

    train_ds = tf.data.Dataset.from_tensor_slices((X_train, y_train)).batch(256).prefetch(2)
    test_ds = tf.data.Dataset.from_tensor_slices((X_test, y_test)).batch(256).prefetch(2)

    model.fit(
        train_ds,
        validation_data=test_ds,
        epochs=5,
        class_weight=class_weight,
        verbose=2,
    )

    # Save SavedModel
    out_dir = Path("total_line_model_saved")
    if out_dir.exists():
        # keep simple: remove contents
        import shutil

        shutil.rmtree(out_dir)
    model.export(str(out_dir))
    print("SavedModel ->", out_dir)

    # Convert to TFLite
    converter = tf.lite.TFLiteConverter.from_saved_model(str(out_dir))
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    tflite_model = converter.convert()

    tflite_path = Path("total_line_model.tflite")
    tflite_path.write_bytes(tflite_model)
    print("TFLite ->", tflite_path, "bytes=", tflite_path.stat().st_size)


if __name__ == "__main__":
    main()
