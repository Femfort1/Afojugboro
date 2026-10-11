# AFOJÚGBỌ́RỌ̀ – live captions for deaf users (Yorùbá, English, Igbo, Hausa, Pidgin)

## Get the APK (no Android Studio needed)
1. Create a GitHub repo, upload this folder, push.
2. Actions tab > "Build APK" > download artifact `Sauti-apk` (app-debug.apk).
3. Copy to phone, allow "install unknown apps", install.
(Or open in Android Studio > Build > Build APK.)

## Offline use (v1)
Uses the phone's on-device recognizer. Download offline packs:
Google app > Settings > Voice > Offline speech recognition > English (Nigeria), Hausa, Igbo, Yoruba (if listed).
Availability differs per phone/language.

## Guaranteed offline (v2, recommended for Yorùbá)
Implement `SpeechEngine` with sherpa-onnx (k2-fsa/sherpa-onnx, Android AAR) running a bundled
Whisper/MMS model fine-tuned for Yoruba/Hausa/Igbo/Pidgin (e.g. Masakhane / NaijaVoices data).
Capture 16 kHz mono with AudioRecord, feed in ~5s chunks, call onPartial/onFinal. UI needs no change.
