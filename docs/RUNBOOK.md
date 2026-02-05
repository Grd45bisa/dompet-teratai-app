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
