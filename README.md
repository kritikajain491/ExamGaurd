# ExamGuard - Examination Management System

Java Swing desktop app. Core Java + OOP + Collections + File Handling only. No database, no external libraries.

## Folder structure
```
ExamGuard/
├── .vscode/ (settings.json, launch.json)
├── run.bat / run.sh
├── src/examguard/
│   ├── Main.java
│   ├── model/    Role, User (abstract), Admin, Student, Question, Exam, Result, LogEntry
│   ├── util/     FileUtil (file handling), Validator
│   ├── service/  DataStore, AuthService, ExamService, Seeder
│   └── ui/       Theme, BarChart, LoginFrame, AdminDashboard, StudentDashboard,
│                 ExamWindow, ResultDialog, CertificateDialog, QuestionSelectDialog
├── out/    (compiled classes, created on build)
└── data/   (created on first run: users.txt, questions.txt, exams.txt, results.txt, logs.txt, reports/)
```

## Sample credentials
| Role    | Username | Password    |
|---------|----------|-------------|
| Admin   | admin    | Admin@123   |
| Student | student  | Student@123 |
| Student | alice    | Alice@123   |
| Student | bob      | Bob@1234    |

Three wrong passwords lock an account for 5 minutes (admin can unlock students via Toggle Status).
To reset all data, close the app and delete the `data` folder.

## Run in VS Code
1. Install JDK 17+ and the "Extension Pack for Java".
2. File > Open Folder > select `ExamGuard`.
3. Open `src/examguard/Main.java` and click Run, or press F5 (uses `.vscode/launch.json`).

Or from a terminal in the project folder: `run.bat` (Windows) or `./run.sh` (Linux/macOS).
Manual: `javac -d out @sources.txt` then `java -cp out examguard.Main`.

