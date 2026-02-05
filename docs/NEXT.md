# Next Steps (Milestone 1)

## Goal
Add CameraX preview + capture, then run ML Kit OCR on the captured image and show the extracted text.

## Proposed UI
- Screen with Camera preview
- Button: Capture
- After capture:
  - show OCR text
  - show extracted fields (merchant/date/total) (later)

## Implementation notes
- Use `androidx.camera:camera-view` + `PreviewView`
- Capture using `ImageCapture`
- Convert `ImageProxy` to `InputImage` (ML Kit) and run `TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)`.

