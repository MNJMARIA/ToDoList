# ToDoList – Mobile Task Planner with Gamification

Mobile Android application for effective task management with motivational gamification elements.  
Developed as a qualification (diploma) project for the "Software Engineering" program at the College of Engineering, Management and Land Management, National Aviation University (Kyiv, Ukraine, 2024).

The app helps users organize daily tasks, prioritize them using the **Eisenhower Matrix**, track progress, and stay motivated through points, achievements, and timely notifications.

## Features

- **Authentication**: Registration and login via email (Firebase Authentication)
- **Task Management**:
  - Create, edit, delete, and mark tasks as completed
  - Categories (Work, Personal, Study, Health, Family, Hobbies)
  - Prioritization via Eisenhower Matrix (4 quadrants)
  - Deadlines with date and time selection
- **Gamification**:
  - Earn 25 points for on-time task completion
  - Unlock 12 achievements at point milestones (500 -> 100,000 points)
  - Profile avatar updates based on current achievement level
- **Views**:
  - **Day view** – tasks filtered by priority
  - **Month view** – calendar with tasks displayed for selected date
- **Statistics**: Total tasks, completed, pending, and accumulated points
- **Notifications**: Reminders 3 days before deadline
- **Settings**:
  - Multi-language support (Ukrainian, English, French, German, Korean, Japanese)
  - Light/Dark theme toggle
  - Enable/disable notifications
  - Edit personal profile information

## Technologies

- **Kotlin**
- **Android SDK**
- **Firebase Authentication** & **Realtime Database**
- **MVVM** architecture
- **Android Jetpack** (Navigation Component, View Binding, RecyclerView)
- **Material Design**

## Architecture

The project follows the **MVVM** pattern with clear separation:
- **Model** – data classes and Firebase interaction
- **View** – XML layouts and Fragments
- **ViewModel** – business logic and data handling

UML class diagram and use-case diagrams are included in the project documentation.

## Screenshots

<!-- Authentication flow: 3 screens in one row, однакова висота -->
<img src="https://github.com/user-attachments/assets/3512d2ef-8876-4e8a-81bc-bc910b546efc" height="300">
<img src="https://github.com/user-attachments/assets/e3f97829-3b79-4b79-b2ef-86eca48fdd55" height="300">
<img src="https://github.com/user-attachments/assets/98e003cc-3824-47f8-b38a-f2eba655f79b" height="300">

<p><em>Authentication flow: registration, login, and profile setup</em></p>

<!-- Day / Month views: 2 screens, однакова висота -->
<img src="https://github.com/user-attachments/assets/2354d395-d136-4f62-b1e8-bfe2d10aab53" height="300">
<img src="https://github.com/user-attachments/assets/95b9379c-d064-4e32-848f-c89f1bb63e89" height="300">

<p><em>Task display in Day view and Month view</em></p>

<!-- Adding/editing a task: вертикальне, ширина 80% -->
<img src="https://github.com/user-attachments/assets/cc82c2ad-e282-453b-b194-3dffe72d2c61" width="80%" height="300">

<p><em>Adding and editing a task (category, priority, deadline)</em></p>

<!-- Achievements: 2 screens, однакова висота -->
<img src="https://github.com/user-attachments/assets/33ea7a76-4eb1-4760-a535-0c8ac11bfa81" height="300">
<img src="https://github.com/user-attachments/assets/8566060d-490f-4f89-ad42-c1cf87d43b83" height="300">

<p><em>Achievements screen at initial and maximum levels</em></p>

<!-- Menu & Settings: 2 screens, однакова висота -->
<img src="https://github.com/user-attachments/assets/4ad36720-1e1a-487c-ab66-946a7d7fa9ca" height="300">
<img src="https://github.com/user-attachments/assets/d595bde2-edd1-483c-a813-157000d9f5d3" height="300">

<p><em>Main menu and application settings</em></p>


## Acknowledgments

- The user interface design and some layout solutions were inspired by publicly available Android development tutorials and educational materials on YouTube (e.g., Philipp Lackner, Coding in Flow, and similar task planner projects).
- All application logic, data handling, and functionality were implemented independently as part of the diploma project.
- Special thanks to my supervisor, Eduard Ruslanovych Smilyi, for guidance and support throughout the project.

## Author

**Mariia Stepanova**  
Group 405-ІПЗ  
Software Engineering  
College of Engineering, Management and Land Management  
National Aviation University  
Kyiv, Ukraine – 2024

Supervisor: Eduard Ruslanovych Smilyi

---

## License

All rights reserved.  
Copyright © 2024 Mariia Steppanova.

This project was developed as part of an academic coursework.
Any use, copying, or distribution without the author’s permission is prohibited.
