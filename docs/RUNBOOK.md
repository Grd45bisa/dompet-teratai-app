# Runbook

## What works now
- Android app (Kotlin + Compose)
- Camera permission flow
- CameraX preview + capture to cache
- ML Kit Text Recognition on captured image
- Baseline heuristic parsing (merchant/date/total)

## How to run
1. Open in Android Studio
2. Set `sdk.dir` in `local.properties`
3. Run on an Android device (recommended) or emulator (camera may require virtual camera)

## Next
- Improve UI/UX (show captured image, better layout)
- Add edit + save transaction locally (Room)
- Add on-device TFLite student model (later)

## UI quality improvements added
- Receipt image persisted in internal storage and displayed in review/detail
- Total normalized + displayed as IDR currency format
- Human-readable timestamp for saved transactions

## Phase 2.1 (image pre-processing)
- Rotate 90° + rerun OCR
- Resize/compress before OCR
- Manual crop flow + rerun OCR
- Re-OCR button (rerun OCR without recapturing)

- OCR raw text is collapsible in Scan and Detail screens

## Export
- From Riwayat screen, tap **Export CSV** to share `dompet-teratai-export.csv`
- From Riwayat screen, tap **Export JSON** to share `dompet-teratai-export.json`
- From Riwayat screen, tap **Export Dataset** to share `receipt_labels.jsonl`
