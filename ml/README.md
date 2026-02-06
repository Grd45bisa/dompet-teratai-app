# ML (Student model) — Dompet Teratai

This folder is for training/exporting a **small student model** that can run on-device (Android via TFLite).

## Phase 3 approach
Start with field extraction for:
- merchant
- dateIso
- total

### Dataset source
Export dataset from the Android app:
- `receipt_labels.jsonl`
Each line is JSON with:
- `merchant`, `dateIso`, `total`, `ocrText`

## Planned pipeline
1. Convert JSONL → supervised samples
2. Train a small model (baseline: per-line classifier / token classifier)
3. Export to TFLite
4. Integrate into Android parsing (fallback to heuristics)

## Notes
We intentionally start with **total/date/merchant** (no item list yet) to keep model/data requirements minimal.

## Baseline: total line classifier (scikit-learn)

1) Put `receipt_labels.jsonl` into `ml/`
2) Install deps:

```bash
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

3) Train:

```bash
python train_total_line.py
```

This produces `model_total_line.joblib`.
