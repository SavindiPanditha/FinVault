# FinVault - Personal Finance Tracker

A modern Android application for tracking personal finances, managing expenses, and monitoring your budget effectively.

## 📱 About

FinVault (ImiliPocket) is a comprehensive personal finance management application built with Kotlin for Android. It helps users track their income, expenses, and maintain a healthy financial overview with an intuitive and user-friendly interface.

## ✨ Features

- 💰 Track income and expenses
- 📊 Visual financial insights and reports
- 🔔 Budget alerts and notifications
- 📅 Scheduled transaction reminders
- 🎯 Category-based expense tracking
- 🔐 Email/password authentication with Supabase
- 📱 Modern Material Design UI
- 💾 Local data storage using Room Database
- 🚀 Smooth onboarding experience

## 🛠️ Tech Stack

- **Language:** Kotlin
- **UI:** Android ViewBinding
- **Database:** Room Persistence Library
- **Networking/Auth:** OkHttp + Supabase Auth REST API
- **Architecture:** MVVM (ViewModel + Repository + DAO)
- **Min SDK:** 24 (Android 7.0)
- **Target SDK:** 35 (Android 15)

## 📋 Prerequisites

- Android Studio (latest version recommended)
- JDK 11 or higher
- Android SDK 35
- Gradle 8.x

## 🚀 Getting Started

### Clone the repository

```bash
git clone https://github.com/SavindiPanditha/FinVault.git
cd FinVault
```

### Build the project

```bash
./gradlew build
```

### Configure Supabase (required for authentication)

Add these values to `local.properties`:

```properties
SUPABASE_URL=your_supabase_project_url
SUPABASE_ANON_KEY=your_supabase_anon_key
```

### Run the app

1. Open the project in Android Studio
2. Connect an Android device or start an emulator
3. Click on the "Run" button or press Shift + F10

## 📦 Dependencies

Key libraries used in this project:

- AndroidX Core KTX
- AndroidX AppCompat
- Material Design Components
- Room Database
- Kotlin Coroutines (for async operations)
- OkHttp
- Gson

## 📱 Permissions

The app requires the following permissions:

- `INTERNET` - For Supabase authentication API requests
- `POST_NOTIFICATIONS` - For budget alerts and reminders
- `SCHEDULE_EXACT_ALARM` - For scheduled transaction notifications

## 🏗️ Project Structure

```
app/
├── src/
│   ├── main/
│   │   ├── java/com/example/imilipocket/
│   │   │   ├── auth/         # Supabase auth service and session management
│   │   │   ├── data/         # Database and data models
│   │   │   └── ui/           # Activities and UI components
│   │   └── res/              # Resources (layouts, drawables, etc.)
│   ├── androidTest/          # Instrumented tests
│   └── test/                 # Unit tests
└── schemas/                  # Room database schemas
```

## 🤝 Contributing

Contributions, issues, and feature requests are welcome!

## 📄 License

This project is open source and available under the MIT License.

## 👤 Author

**Savindi Panditha**

- GitHub: [@SavindiPanditha](https://github.com/SavindiPanditha)

## 📞 Support

If you have any questions or need help, please open an issue in the GitHub repository.

---

⭐ Star this repository if you find it helpful!
