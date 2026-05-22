# Medisync-Diabo Android App

This is the native Android (Java) implementation of the Medisync-Diabo hospital reservation and health tracking system.

## 🚀 Features
- **Bento Dashboard:** At-a-glance health metrics and vitals.
- **Clinical Intelligence:** AI-powered medical report analysis using Gemini 1.5 Flash.
- **Document OCR:** On-device text extraction from health documents via Google ML Kit.
- **Health assistant:** AI chatbot for medical queries.
- **Room Persistence:** Local database for user profile and health history.

## 🛠️ How to Run
1. **Open in Android Studio:** Open the `DiaboAndroid` directory as an existing project.
2. **Gemini API Key:** The API key is pre-configured in `gradle.properties`. If you wish to use your own, update the `GEMINI_API_KEY` field there.
3. **Build & Install:** Click the **Run** button (green play icon). The app requires API 24 (Android 7.0) or higher.
4. **Mock Auth:** Use any email/password to login, or click "Continue as Guest".

## 📁 Project Structure
- `app/src/main/java`: Source code containing fragments, services, and database logic.
- `app/src/main/res`: UI layouts, colors, and assets ported from the original iOS design.
- `app/build.gradle`: Project dependencies and configuration.

---
*Created by Antigravity for Adithya Anand.*
