# LisTree 🛒

A modern, offline-first list management application for Android, built entirely with Jetpack Compose and Material 3. This app provides a clean and intuitive interface for managing hierarchical lists for any purpose.

## ✨ Features

*   **Intuitive List Management:** Create, edit, and delete multiple lists.
*   **Organize with Groups:** Structure your lists with nested groups for better organization (e.g., "Groceries", "Hardware Store").
*   **Dynamic Item Handling:** Add items, check them off, edit names, and mark items as section headers.
*   **Drag & Drop Reordering:** Easily re-prioritize items within a list with smooth drag-and-drop functionality.
*   **Swipe to Delete:** Quickly remove items with a swipe gesture, with a timed "Undo" option to prevent mistakes.
*   **Voice Input:** Add or edit items hands-free using your voice.
*   **Modern UI:** Built with Material 3 components and a dynamic theme.
*   **Offline First:** All data is stored locally on your device using a SQLite database, ensuring the app is always fast and available.

## 🛠️ Tech Stack & Architecture

*   **UI:** [Jetpack Compose](https://developer.android.com/jetpack/compose) with [Material 3](https://m3.material.io/).
*   **Architecture:** Follows the Model-View-ViewModel (MVVM) pattern.
*   **Asynchronous:** [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) and [Flow](https://developer.android.com/kotlin/flow) are used for background operations and reactive updates.
*   **Database:** [Room](https://developer.android.com/training/data-storage/room) for robust, local SQLite database storage.
*   **Reordering:** Implemented using the lightweight and powerful [reorderable](https://github.com/Calvin-LL/reorderable) library.
*   **Navigation:** Handled with [Compose Navigation](https://developer.android.com/jetpack/compose/navigation).

## 🚀 Getting Started

1.  Clone the repository:
    ```bash
    git clone https://github.com/Mitte76/Listree.git
    ```
2.  Open the project in the latest stable version of [Android Studio](https://developer.android.com/studio).
3.  Let Gradle sync the project dependencies.
4.  Build and run the app on an Android emulator or a physical device.

## 🔮 Future Plans

*   **Self-Hosted Sync:** A companion open-source server application is planned as a separate project. This will allow users to self-host their own sync service (e.g., on a Raspberry Pi), giving them full control over their data while enabling synchronization across multiple devices.

---
