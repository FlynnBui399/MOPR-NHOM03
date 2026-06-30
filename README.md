# Group 03 — MOPR (Mobile Programming) Android Projects

Welcome to the central repository for **Group 03**'s Mobile Programming (MOPR) projects. This repository contains three distinct Android applications, all built natively with **Java** and **Android Studio**, utilizing modern architectures, cloud services, and AI APIs.

---

## 👥 Team Members

| Name | Student ID | Roles & Responsibilities |
| :--- | :---: | :--- |
| **Huỳnh Gia Hân** | 23110019 | Developer (Fonos, Pocket, Calculator) |
| **Bùi Trần Tấn Phát** | 23110052 | Developer (Fonos, Pocket, Calculator) |
| **Nguyễn Nhật Phát** | 23110053 | Developer (Fonos, Pocket, Calculator) |

---

## 📱 Project Suite Overview

This workspace acts as a mono-repository containing the following three Android applications:

| Project | Description | Primary Technologies | Key Highlight |
| :--- | :--- | :--- | :--- |
| **[Calculator](file:///c:/Users/Admin/Documents/FUTURE/MOPR/MOPR-NHOM03/Calculator)** | iPhone-style scientific/basic expression calculator. | Java, Android XML, Custom Parser | Recursive descent parsing for math expressions |
| **[Fonos](file:///c:/Users/Admin/Documents/FUTURE/MOPR/MOPR-NHOM03/Fonos)** | Audiobook application clone (browsing, library, background player). | Java, Firestore, Services, Glide | Foreground Audio Player & Background Download Services |
| **[Pocket](file:///c:/Users/Admin/Documents/FUTURE/MOPR/MOPR-NHOM03/Pocket)** | Real-time photo sharing and messaging network (Locket Widget clone). | Java, Firebase Auth/Firestore/Storage, FCM, Gemini SDK | Gemini Vision AI captioning & real-time widget synchronization |

---

## 🛠️ Detailed Projects Breakdown

### 1. Calculator
A clean, minimalist calculator application inspired by the iOS design system. 

*   **Features:**
    *   Basic arithmetic operations (addition, subtraction, multiplication, division).
    *   Full support for parentheses and nested expressions.
    *   Correct operator precedence handling (multiplication/division evaluated before addition/subtraction).
    *   Real-time equation preview, showing the result only after tapping `=`.
    *   Delete (backspace) and All Clear (AC) functionality.
    *   Fully responsive XML layout adapting seamlessly to portrait and landscape modes.
*   **Core Technical Aspect:** Uses a custom-built **Recursive Descent Parser** in Java to parse and evaluate mathematical expression strings safely and efficiently without using unsafe JavaScript engines or `eval()`.

<p align="center">
  <img src="Calculator/screenshot_portrait.jpg" width="220" alt="Calculator Portrait"/>
  &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
  <img src="Calculator/screenshot_landscape.jpg" width="420" alt="Calculator Landscape"/>
</p>

---

### 2. Fonos (Audiobook App)
An audiobook streaming application clone, providing a rich media experience with online and offline listening features.

*   **Features:**
    *   **Book Browsing & Searching:** Real-time search using Firebase Firestore queries with client-side filters.
    *   **Personal Library:** Allows authenticated users to save book markers, review ratings, and track books in their private collection.
    *   **Offline Downloads:** A background service manages audiobook downloading, status tracking, and offline storage.
    *   **Audio Player Service:** A background Android service that plays book audios, handles state changes (play, pause, seek), and updates the UI in real-time.
    *   **History & Recent list:** Automatically saves and displays recently played audiobooks.
*   **Core Technical Aspect:** Leverages Android **Services** for audio playback (`AudioPlayerService`) and downloading (`AudioDownloadService`), ensuring that long-running operations survive lifecycle events and screen rotation.

---

### 3. Pocket (Locket Widget Clone)
A real-time photo-sharing and messaging network designed for close friends. Send photos instantly to your friends' home screen widgets and chat about them.

*   **Features:**
    *   **Secure Authentication:** Phone number login via Firebase Auth (OTP verification) with persistent login sessions.
    *   **Friend Management:** Add friends by phone number (number is obfuscated/hidden from lists for privacy), approve requests, and remove friends.
    *   **Instant Capture:** Custom camera module allowing quick capture and upload of pictures directly to Firebase Cloud Storage.
    *   **AI Auto-Caption:** Integrated with **Gemini Vision API** to generate funny and engaging Vietnamese captions automatically based on photo content.
    *   **Real-time Chat:** Instant 1-on-1 messaging using Firestore snapshot listeners (`addSnapshotListener`) with read-receipts.
    *   **Push Notifications:** Sends immediate alerts to receivers using client-side Firebase Cloud Messaging (FCM HTTP v1 API) when photos or messages are sent.
    *   **Photo Timeline (Memories):** A chronological scrollable feed of all shared photo history.
*   **Core Technical Aspect:** Adheres strictly to the **MVVM Architecture** (Model-View-ViewModel) paired with Firestore real-time subscriptions, keeping the UI state synchronized with the cloud backend without manual polling.

<p align="center">
  <img src="Pocket/docs/mvvm_structure.png" width="500" alt="Pocket MVVM Architecture"/>
</p>

---

## 🚀 Getting Started

### Prerequisites
*   **Android Studio** (latest stable release recommended)
*   **Android SDK API 21+** (Android 5.0 Lollipop or newer)
*   A physical Android device or emulator configured with Google Play Services (required for Firebase & Gemini APIs)

### Setup & Installation

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/FlynnBui399/MOPR-NHOM03.git
    ```

2.  **Open a project in Android Studio:**
    Since this is a multi-project repository, do not open the root directory. Instead, open Android Studio and import the specific project folder you wish to run:
    *   Select **Open** $\rightarrow$ Navigate to and select either `Calculator`, `Fonos`, or `Pocket`.

3.  **Setup Configuration Files:**
    *   For **Fonos** and **Pocket**, make sure to place your generated `google-services.json` file inside the corresponding `/app` directory to connect your Firebase projects.
    *   For **Pocket**, ensure your Gemini API key is properly configured (e.g., in a local configuration file like `secrets.properties` or environment variables as specified in the project).

4.  **Sync and Run:**
    *   Let the Gradle sync complete.
    *   Press the **Run** button (`Shift + F10`) to build and deploy to your device.

---

## 📂 Repository Directory Structure

```text
MOPR-NHOM03/
├── Calculator/                # iPhone-style Calculator Project
│   ├── app/                   # App source code (Java, layouts, resources)
│   └── build.gradle.kts       # Project configurations
│
├── Fonos/                     # Fonos Audiobook Clone Project
│   ├── app/                   # App source code (Services, fragments, adapters)
│   └── build.gradle.kts       # Project configurations
│
└── Pocket/                    # Pocket Locket-Widget Clone Project
    ├── app/                   # App source code (MVVM components, Gemini helper)
    ├── docs/                  # Project proposals, architecture diagrams
    ├── functions/             # Firebase functions code (optional/future)
    └── build.gradle.kts       # Project configurations
```

---

## 🛡️ License

These projects are developed solely for educational purposes as part of the Mobile Programming (MOPR) course. All rights to third-party assets (e.g., Fonos branding/assets) belong to their respective owners.
