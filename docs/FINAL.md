# Final checklist (release-ready)

## App features
- Scan receipt (CameraX)
- OCR on-device (ML Kit)
- Pre-processing: crop / rotate / re-OCR
- Review & edit (with date picker)
- Save locally (Room)
- History + detail + delete
- Export CSV / JSON
- Reset all data
- Optional: dataset logging + export dataset (jsonl)

## On-device student model (TOTAL-line)

### Train + export TFLite
1. Collect labeled receipts in the app (Review -> keep "Simpan sebagai data latih" ON)
2. Export dataset: Riwayat -> Export Dataset
3. Place `receipt_labels.jsonl` into `ml/`
4. Train:

```bash
cd ml
python -m venv .venv
source .venv/bin/activate
pip install -r requirements-tf.txt
python train_total_line_tf.py
```

Output: `ml/total_line_model.tflite`

### Install model on device
- Open Scan screen
- Tap **Pasang Model** and select `total_line_model.tflite`
- Tap **Debug** to see confidence + compare model vs heuristic

