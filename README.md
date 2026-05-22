# Medisync-Diabo Android App

Native Android app for diabetes tracking, report upload, OCR, and AI-assisted health insights.

## Features

- **Bento Dashboard:** At-a-glance health metrics and vitals.
- **Clinical Intelligence:** Medical report analysis through a local Ollama backend.
- **Document OCR:** On-device text extraction from health documents through Google ML Kit.
- **Health Assistant:** AI chatbot for medical queries.
- **Room Persistence:** Local database for user profile, medications, reports, and health history.

## How to Run

1. Open this repository in Android Studio.
2. Copy `gradle.properties.example` to `gradle.properties`.
3. Set `OLLAMA_BASE_URL` and `OLLAMA_MODEL` for your local Ollama server.
4. Build and install the app on Android API 24 or higher.

For a physical phone, set `OLLAMA_BASE_URL` to your computer's LAN address, for example `http://192.168.1.10:11434/`.
For the Android emulator, `http://10.0.2.2:11434/` usually works.

## Project Structure

- `app/src/main/java/com/medisync/diabo`: Activities and app entry points.
- `app/src/main/java/com/medisync/diabo/fragment`: Screen fragments.
- `app/src/main/java/com/medisync/diabo/service`: OCR, Ollama, notification, and validation services.
- `app/src/main/java/com/medisync/diabo/db`: Room database and DAO classes.
- `app/src/main/java/com/medisync/diabo/model`: Room entities and app models.
- `app/src/main/java/com/medisync/diabo/adapter`: RecyclerView adapters.
- `app/src/main/res`: Layouts, drawables, navigation, menus, animations, and values.
- `gradle/`, `build.gradle`, `settings.gradle`: Gradle wrapper and project build configuration.

## Local Files

Local environment files such as `gradle.properties`, `local.properties`, `.idea/`, and `.vscode/` are intentionally ignored.
