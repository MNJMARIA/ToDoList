# ToDoList – Mobile Task Planner with Gamification

[![Java](https://img.shields.io/badge/Java-ED8B00?style=flat&logo=java&logoColor=white)](https://www.java.com/)
[![Android](https://img.shields.io/badge/Android-3DDC84?style=flat&logo=android&logoColor=white)](https://developer.android.com/)
[![Firebase](https://img.shields.io/badge/Firebase-039BE5?style=flat&logo=firebase&logoColor=white)](https://firebase.google.com/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

## Overview

Mobile Android application for effective task management with motivational gamification elements.  
Developed as a qualification (diploma) project for the "Software Engineering" program at the **College of Engineering, Management and Land Management, National Aviation University** (Kyiv, Ukraine, 2024).

The app helps users organize daily tasks, prioritize them using the **Eisenhower Matrix**, track progress, and stay motivated through points, achievements, and timely notifications.

## Features

### Authentication
- Registration and login via email (Firebase Authentication)

### Task Management
- Create, edit, delete, and mark tasks as completed
- Categories: Work, Personal, Study, Health, Family, Hobbies
- Prioritization via Eisenhower Matrix (4 quadrants: Urgent & Important, Not Urgent & Important, Urgent & Not Important, Not Urgent & Not Important)
- Deadlines with date and time selection

### Gamification
- Earn 25 points for on-time task completion
- Unlock 12 achievements at point milestones (500 → 100,000 points)
- Profile avatar updates based on current achievement level

### Views
- **Day View** – tasks filtered by priority
- **Month View** – calendar with tasks displayed for selected date

### Statistics
- Total tasks, completed, pending, and accumulated points

### Notifications
- Reminders 3 days before task deadline

### Settings
- Multi-language support: Ukrainian, English, French, German, Korean, Japanese
- Light/Dark theme toggle
- Enable/disable notifications
- Edit personal profile information

## Technologies

| Category | Technologies |
|----------|--------------|
| **Language** | Java |
| **Framework** | Android SDK |
| **Backend** | Firebase Authentication & Realtime Database |
| **Architecture** | MVVM |
| **Android Jetpack** | Navigation Component, View Binding, RecyclerView |
| **Design** | Material Design |

## Architecture

The project follows the **MVVM** (Model-View-ViewModel) pattern with clear separation of concerns:

- **Model** – data classes and Firebase interaction layer
- **View** – XML layouts and Fragments (UI components)
- **ViewModel** – business logic and data handling

UML class diagram and use-case diagrams are included in the project documentation.

## Screenshots

### Authentication Flow
*Registration, login, and profile setup*

<img src="https://github.com/user-attachments/assets/3512d2ef-8876-4e8a-81bc-bc910b546efc" height="400">
<img src="https://github.com/user-attachments/assets/e3f97829-3b79-4b79-b2ef-86eca48fdd55" height="400">
<img src="https://github.com/user-attachments/assets/98e003cc-3824-47f8-b38a-f2eba655f79b" height="400">

---

### Task Views
*Task display in Day view and Month view*

<img src="https://github.com/user-attachments/assets/2354d395-d136-4f62-b1e8-bfe2d10aab53" height="400">
<img src="https://github.com/user-attachments/assets/95b9379c-d064-4e32-848f-c89f1bb63e89" height="400">

---

### Task Management
*Adding and editing a task (category, priority, deadline)*

<img src="https://github.com/user-attachments/assets/cc82c2ad-e282-453b-b194-3dffe72d2c61" height="200">

---

### Achievements
*Achievements screen at initial and maximum levels*

<img src="https://github.com/user-attachments/assets/33ea7a76-4eb1-4760-a535-0c8ac11bfa81" height="400">
<img src="https://github.com/user-attachments/assets/8566060d-490f-4f89-ad42-c1cf87d43b83" height="400">

---

### Menu & Settings
*Main menu and application settings*

<img src="https://github.com/user-attachments/assets/4ad36720-1e1a-487c-ab66-946a7d7fa9ca" height="400">
<img src="https://github.com/user-attachments/assets/d595bde2-edd1-483c-a813-157000d9f5d3" height="400">

---

## How to Run the Project

1. Clone the repository:
   ```bash
   git clone https://github.com/your-username/todolist-android.git
   ```

2. Open the project in **Android Studio**

3. Configure Firebase:
   - Download `google-services.json` from Firebase Console
   - Place it in the `app/` directory

4. Build and run the application on an Android device or emulator

## Acknowledgments

- The user interface design and some layout solutions were inspired by publicly available Android development tutorials and educational materials on YouTube (e.g., Philipp Lackner, Coding in Flow, and similar task planner projects)
- All application logic, data handling, and functionality were implemented independently as part of the diploma project
- Special thanks to my supervisor, **Eduard Ruslanovych Smilyi**, for guidance and support throughout the project

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

**Author:** Mariia Stepanova  
**Program:** Software Engineering  
**Institution:** College of Engineering, Management and Land Management, National Aviation University (Kyiv, Ukraine)  
**Year:** 2024
