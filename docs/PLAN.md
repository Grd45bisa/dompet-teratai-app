# Dompet Teratai App (Android) — Plan

Goal: Android app to scan receipts and extract structured transaction data.

## Milestone 0 — Repo + skeleton
- Android project (Kotlin + Jetpack Compose)
- CI-ready gradle config (later)

## Milestone 1 — Camera capture
- Capture image from camera
- Preview + basic image pipeline

## Milestone 2 — On-device OCR
- ML Kit Text Recognition
- Display OCR text

## Milestone 3 — Parsing (baseline)
- Heuristic/rule-based extraction for: merchant, date, total
- JSON output + editable UI

## Milestone 4 — Student model (on-device)
- Add TFLite inference for field extraction (small model)
- Confidence + fallback behavior

## Milestone 5 — Teacher (optional)
- Manual "Improve" action that sends OCR text to Gemini to generate labels
- Store labels locally (and optionally sync)
