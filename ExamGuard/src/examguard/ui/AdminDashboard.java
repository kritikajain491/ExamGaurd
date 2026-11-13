package examguard.ui;

import examguard.model.*;
import examguard.service.AuthService;
import examguard.service.DataStore;
import examguard.service.ExamService;
import examguard.util.FileUtil;
import examguard.util.Validator;
import java.awt.*;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

/** Admin console: sidebar + CardLayout pages. Only accessible with the ADMIN role. */
public class AdminDashboard extends JFrame {
    private final DataStore ds = DataStore.get();
    private final User admin;
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final JPanel sidebar = new JPanel();
    private final Map<String, Runnable> refreshers = new LinkedHashMap<>();
    private final Map<String, Theme.RButton> nav = new LinkedHashMap<>();

    public AdminDashboard() {
        AuthService.requireRole(Role.ADMIN);
        admin = AuthService.current();
        setTitle("ExamGuard - Admin Console");
        setSize(1320, 780);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(Theme.SIDEBAR);
        sidebar.setBorder(new EmptyBorder(20, 12, 20, 12));
        sidebar.setPreferredSize(new Dimension(230, 100));
        sidebar.add(Theme.label("ExamGuard", 24, true, Color.WHITE));
        sidebar.add(Theme.label("Admin: " + admin.getFullName(), 12, false, new Color(0x9CA3AF)));
        sidebar.add(Box.createVerticalStrut(24));

        content.setBackground(Theme.BG);
        buildDashboard(); buildStudents(); buildQuestions(); buildExams(); buildResults(); buildAnalytics(); buildLogs();

        sidebar.add(Box.createVerticalGlue());
        Theme.RButton out = Theme.button("Logout", Theme.DANGER, e -> logout());
        out.setAlignmentX(Component.LEFT_ALIGNMENT);
        out.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        sidebar.add(out);

        add(sidebar, BorderLayout.WEST);
        add(content, BorderLayout.CENTER);
        showPage("Dashboard");
    }

    private void addPage(String name, JPanel page, Runnable refresh) {
        content.add(page, name);
        refreshers.put(name, refresh);
        Theme.RButton b = Theme.button("   " + name, Theme.SIDEBAR, e -> showPage(name));
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        nav.put(name, b);
        sidebar.add(b);
        sidebar.add(Box.createVerticalStrut(4));
        refresh.run();
    }

    private void showPage(String name) {
        refreshers.get(name).run();
        cards.show(content, name);
        for (Map.Entry<String, Theme.RButton> e : nav.entrySet()) e.getValue().setBase(e.getKey().equals(name) ? Theme.PRIMARY : Theme.SIDEBAR);
    }

    private void refreshAll() { for (Runnable r : refreshers.values()) r.run(); }

    private void logout() {
        if (!Theme.confirm(this, "Log out of the admin console?")) return;
        AuthService.logout();
        dispose();
        new LoginFrame().setVisible(true);
    }

    // ================= DASHBOARD =================
    private void buildDashboard() {
        JPanel p = Theme.page("Dashboard", "Overview of the examination system", null);
        Theme.StatCard s1 = new Theme.StatCard("Students", "0", Theme.PRIMARY), s2 = new Theme.StatCard("Questions", "0", Theme.INFO),
                s3 = new Theme.StatCard("Exams", "0", Theme.WARN), s4 = new Theme.StatCard("Total attempts", "0", Theme.SUCCESS),
                s5 = new Theme.StatCard("Average score", "0%", Theme.PRIMARY), s6 = new Theme.StatCard("Pass rate", "0%", Theme.DANGER);
        JPanel grid = new JPanel(new GridLayout(2, 3, 14, 14));
        grid.setOpaque(false);
        grid.setPreferredSize(new Dimension(100, 190));
        for (Theme.StatCard c : new Theme.StatCard[]{s1, s2, s3, s4, s5, s6}) grid.add(c);
        DefaultTableModel top = Theme.model("Rank", "Student", "Attempts", "Avg %", "Best %", "Total Score");
        DefaultTableModel logs = Theme.model("Time", "User", "Action");
        JPanel lower = new JPanel(new GridLayout(1, 2, 14, 0));
        lower.setOpaque(false);
        lower.add(Theme.titled("Top 10 students", Theme.scroll(Theme.table(top))));
        lower.add(Theme.titled("Recent activity", Theme.scroll(Theme.table(logs))));
        JPanel body = new JPanel(new BorderLayout(0, 16));
        body.setOpaque(false);
        body.add(grid, BorderLayout.NORTH);
        body.add(lower, BorderLayout.CENTER);
        p.add(body, BorderLayout.CENTER);
        addPage("Dashboard", p, () -> {
            s1.setValue(String.valueOf(ds.students().size()));
            s2.setValue(String.valueOf(ds.questions.size()));
            s3.setValue(String.valueOf(ds.exams.size()));
            s4.setValue(String.valueOf(ds.results.size()));
            s5.setValue(Theme.fmt(ExamService.avgPercent(ds.results)) + "%");
            s6.setValue(Theme.fmt(ExamService.passRate(ds.results)) + "%");
            top.setRowCount(0);
            for (Object[] r : ExamService.leaderboard(10)) top.addRow(r);
            logs.setRowCount(0);
            for (int i = ds.logs.size() - 1; i >= 0 && i >= ds.logs.size() - 12; i--) {
                LogEntry l = ds.logs.get(i);
                logs.addRow(new Object[]{l.getTime(), l.getUser(), l.getAction()});
            }
        });
    }

    // ================= STUDENTS =================
    private void buildStudents() {
        DefaultTableModel m = Theme.model("ID", "Name", "Username", "Roll No", "Email", "Status", "Exams Taken", "Registered");
        JTable t = Theme.table(m);
        JTextField search = Theme.field(16);
        JComboBox<String> status = Theme.combo("All Status", "Active", "Locked", "Disabled");
        Theme.attachFilter(t, search, new Theme.ColFilter(status, 5));
        JPanel tb = Theme.toolbar(new JLabel("Search:"), search, status,
                Theme.button("+ Add", Theme.PRIMARY, e -> { studentForm(null); refreshAll(); }),
                Theme.button("Edit", Theme.INFO, e -> {
                    int r = Theme.selected(this, t); if (r < 0) return;
                    studentForm((Student) ds.userById(Theme.idOf(t, r))); refreshAll();
                }),
                Theme.button("Reset Password", Theme.WARN, e -> {
                    int r = Theme.selected(this, t); if (r < 0) return;
                    resetPassword((Student) ds.userById(Theme.idOf(t, r))); refreshAll();
                }),
                Theme.button("Toggle Status", Theme.SIDEBAR, e -> {
                    int r = Theme.selected(this, t); if (r < 0) return;
                    toggleStudent((Student) ds.userById(Theme.idOf(t, r))); refreshAll();
                }),
                Theme.button("Delete", Theme.DANGER, e -> {
                    int r = Theme.selected(this, t); if (r < 0) return;
                    deleteStudent((Student) ds.userById(Theme.idOf(t, r))); refreshAll();
                }));
        JPanel p = Theme.page("Students", "Manage student accounts", tb);
        p.add(Theme.scroll(t), BorderLayout.CENTER);
        addPage("Students", p, () -> {
            m.setRowCount(0);
            for (Student s : ds.students())
                m.addRow(new Object[]{s.getId(), s.getFullName(), s.getUsername(), s.getRollNo(), s.getEmail(), s.status(),
                        ExamService.resultsOf(s.getId()).size(), s.getCreatedAt()});
        });
    }

    private void studentForm(Student ex) {
        JTextField name = Theme.field(20), roll = Theme.field(20), email = Theme.field(20), user = Theme.field(20);
        JPasswordField pass = Theme.pass(20);
        if (ex != null) { name.setText(ex.getFullName()); roll.setText(ex.getRollNo()); email.setText(ex.getEmail()); user.setText(ex.getUsername()); user.setEditable(false); }
        JPanel form = ex == null
                ? Theme.form(new String[]{"Full name", "Roll no", "Email", "Username", "Password"}, new JComponent[]{name, roll, email, user, pass})
                : Theme.form(new String[]{"Full name", "Roll no", "Email", "Username"}, new JComponent[]{name, roll, email, user});
        while (true) {
            if (JOptionPane.showConfirmDialog(this, form, ex == null ? "Add Student" : "Edit Student",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
            String err = Validator.name(name.getText());
            if (err == null) err = Validator.required(roll.getText(), "Roll number");
            if (err == null) err = Validator.email(email.getText());
            if (err == null && ex == null) {
                err = Validator.username(user.getText());
                if (err == null && ds.findUser(user.getText()) != null) err = "Username already exists.";
                if (err == null) err = Validator.password(new String(pass.getPassword()));
            }
            if (err != null) { Theme.error(this, err); continue; }
            if (ex == null) {
                Student s = new Student(DataStore.newId("S"), user.getText().trim(),
                        AuthService.hash(user.getText().trim(), new String(pass.getPassword())), name.getText().trim(),
                        email.getText().trim(), roll.getText().trim());
                ds.users.add(s);
                ds.log(admin.getUsername(), "Added student " + s.getUsername());
            } else {
                ex.setFullName(name.getText().trim()); ex.setEmail(email.getText().trim()); ex.setRollNo(roll.getText().trim());
                ds.log(admin.getUsername(), "Edited student " + ex.getUsername());
            }
            ds.saveUsers();
            return;
        }
    }

    private void resetPassword(Student s) {
        JPasswordField pw = Theme.pass(20);
        JPanel form = Theme.form(new String[]{"New password for " + s.getUsername()}, new JComponent[]{pw});
        while (true) {
            if (JOptionPane.showConfirmDialog(this, form, "Reset Password", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
            String err = Validator.password(new String(pw.getPassword()));
            if (err != null) { Theme.error(this, err); continue; }
            s.setPasswordHash(AuthService.hash(s.getUsername(), new String(pw.getPassword())));
            s.setFailedAttempts(0); s.setLockedUntil(0);
            ds.saveUsers();
            ds.log(admin.getUsername(), "Reset password for " + s.getUsername());
            Theme.info(this, "Password updated.");
            return;
        }
    }

    private void toggleStudent(Student s) {
        if (!s.isActive()) { s.setActive(true); ds.log(admin.getUsername(), "Enabled " + s.getUsername()); }
        else if (s.isLocked()) { s.setLockedUntil(0); s.setFailedAttempts(0); ds.log(admin.getUsername(), "Unlocked " + s.getUsername()); }
        else {
            if (!Theme.confirm(this, "Disable account '" + s.getUsername() + "'? They will not be able to log in.")) return;
            s.setActive(false);
            ds.log(admin.getUsername(), "Disabled " + s.getUsername());
        }
        ds.saveUsers();
    }

    private void deleteStudent(Student s) {
        if (!Theme.confirm(this, "Delete student '" + s.getFullName() + "' and ALL their results?\nThis cannot be undone.")) return;
        ds.users.remove(s);
        ds.results.removeIf(r -> r.getStudentId().equals(s.getId()));
        ds.saveUsers(); ds.saveResults();
        ds.log(admin.getUsername(), "Deleted student " + s.getUsername());
    }

    // ================= QUESTIONS =================
    private void buildQuestions() {
        DefaultTableModel m = Theme.model("ID", "Subject", "Question", "Difficulty", "Correct Answer");
        JTable t = Theme.table(m);
        t.getColumnModel().getColumn(2).setPreferredWidth(420);
        JTextField search = Theme.field(16);
        JComboBox<String> subj = Theme.combo("All Subjects");
        JComboBox<String> diff = Theme.combo("All Levels", "Easy", "Medium", "Hard");
        Theme.attachFilter(t, search, new Theme.ColFilter(subj, 1), new Theme.ColFilter(diff, 3));
        JPanel tb = Theme.toolbar(new JLabel("Search:"), search, subj, diff,
                Theme.button("+ Add", Theme.PRIMARY, e -> { questionForm(null); refreshAll(); }),
                Theme.button("Edit", Theme.INFO, e -> {
                    int r = Theme.selected(this, t); if (r < 0) return;
                    questionForm(ds.question(Theme.idOf(t, r))); refreshAll();
                }),
                Theme.button("Delete", Theme.DANGER, e -> {
                    int r = Theme.selected(this, t); if (r < 0) return;
                    deleteQuestion(ds.question(Theme.idOf(t, r))); refreshAll();
                }));
        JPanel p = Theme.page("Question Bank", "Create and organise multiple-choice questions", tb);
        p.add(Theme.scroll(t), BorderLayout.CENTER);
        addPage("Questions", p, () -> {
            Theme.resetCombo(subj, "All Subjects", ds.subjects());
            m.setRowCount(0);
            for (Question q : ds.questions) m.addRow(new Object[]{q.getId(), q.getSubject(), q.getText(), q.getDifficulty(), q.correctText()});
        });
    }

    private void questionForm(Question ex) {
        JComboBox<String> subject = new JComboBox<>(ds.subjects().toArray(new String[0]));
        subject.setEditable(true); subject.setFont(Theme.BASE);
        subject.setSelectedItem(ex == null ? "" : ex.getSubject());
        JComboBox<String> diff = Theme.combo("Easy", "Medium", "Hard");
        JTextArea text = Theme.area(3, 34), expl = Theme.area(3, 34);
        JTextField[] o = {Theme.field(34), Theme.field(34), Theme.field(34), Theme.field(34)};
        JComboBox<String> correct = Theme.combo("A", "B", "C", "D");
        if (ex != null) {
            diff.setSelectedItem(ex.getDifficulty()); text.setText(ex.getText()); expl.setText(ex.getExplanation());
            for (int i = 0; i < 4; i++) o[i].setText(ex.getOptions()[i]);
            correct.setSelectedIndex(ex.getCorrect());
        }
        JPanel form = Theme.form(new String[]{"Subject", "Difficulty", "Question", "Option A", "Option B", "Option C", "Option D", "Correct", "Explanation"},
                new JComponent[]{subject, diff, new JScrollPane(text), o[0], o[1], o[2], o[3], correct, new JScrollPane(expl)});
        while (true) {
            if (JOptionPane.showConfirmDialog(this, form, ex == null ? "Add Question" : "Edit Question",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
            String subj = String.valueOf(subject.getEditor().getItem()).trim();
            String err = Validator.required(subj, "Subject");
            if (err == null) err = Validator.required(text.getText(), "Question text");
            Set<String> uniq = new HashSet<>();
            for (int i = 0; i < 4 && err == null; i++) {
                err = Validator.required(o[i].getText(), "Option " + (char) ('A' + i));
                if (err == null && !uniq.add(o[i].getText().trim().toLowerCase())) err = "Options must be different from each other.";
            }
            if (err == null) err = Validator.required(expl.getText(), "Explanation");
            if (err != null) { Theme.error(this, err); continue; }
            String[] opts = {o[0].getText().trim(), o[1].getText().trim(), o[2].getText().trim(), o[3].getText().trim()};
            if (ex == null) {
                Question q = new Question(DataStore.newId("Q"), subj, text.getText().trim(), opts, correct.getSelectedIndex(),
                        expl.getText().trim(), (String) diff.getSelectedItem());
                ds.questions.add(q);
                ds.log(admin.getUsername(), "Added question " + q.getId());
            } else {
                int i = ds.questions.indexOf(ex);
                Question q = new Question(ex.getId(), subj, text.getText().trim(), opts, correct.getSelectedIndex(),
                        expl.getText().trim(), (String) diff.getSelectedItem());
                ds.questions.set(i, q);
                ds.log(admin.getUsername(), "Edited question " + q.getId());
            }
            ds.saveQuestions();
            return;
        }
    }

    private void deleteQuestion(Question q) {
        for (Exam e : ds.exams)
            if (e.getQuestionIds().contains(q.getId())) {
                Theme.error(this, "This question is used in exam '" + e.getTitle() + "'.\nRemove it from the exam first.");
                return;
            }
        if (!Theme.confirm(this, "Delete this question?\n\n" + q.getText())) return;
        ds.questions.remove(q);
        ds.saveQuestions();
        ds.log(admin.getUsername(), "Deleted question " + q.getId());
    }

    // ================= EXAMS =================
    private void buildExams() {
        DefaultTableModel m = Theme.model("ID", "Title", "Subject", "Questions", "Duration (min)", "Marks/Q", "Negative", "Total Marks", "Pass %", "Start", "End", "Status");
        JTable t = Theme.table(m);
        JTextField search = Theme.field(16);
        JComboBox<String> status = Theme.combo("All Status", "Upcoming", "Open", "Closed");
        Theme.attachFilter(t, search, new Theme.ColFilter(status, 11));
        JPanel tb = Theme.toolbar(new JLabel("Search:"), search, status,
                Theme.button("+ Create Exam", Theme.PRIMARY, e -> { examForm(null); refreshAll(); }),
                Theme.button("Edit", Theme.INFO, e -> {
                    int r = Theme.selected(this, t); if (r < 0) return;
                    examForm(ds.exam(Theme.idOf(t, r))); refreshAll();
                }),
                Theme.button("Delete", Theme.DANGER, e -> {
                    int r = Theme.selected(this, t); if (r < 0) return;
                    Exam ex = ds.exam(Theme.idOf(t, r));
                    if (!Theme.confirm(this, "Delete exam '" + ex.getTitle() + "'?\nExisting results are kept.")) return;
                    ds.exams.remove(ex); ds.saveExams();
                    ds.log(admin.getUsername(), "Deleted exam " + ex.getTitle());
                    refreshAll();
                }));
        JPanel p = Theme.page("Exams", "Schedule exams, set duration, marks and negative marking", tb);
        p.add(Theme.scroll(t), BorderLayout.CENTER);
        addPage("Exams", p, () -> {
            m.setRowCount(0);
            for (Exam e : ds.exams)
                m.addRow(new Object[]{e.getId(), e.getTitle(), e.getSubject(), e.getQuestionIds().size(), e.getDurationMin(),
                        e.getMarksPerQuestion(), e.getNegativeMarks(), e.totalMarks(), e.getPassPercent(),
                        e.getStart().format(FileUtil.DT), e.getEnd().format(FileUtil.DT), e.status()});
        });
    }

    private void examForm(Exam ex) {
        JTextField title = Theme.field(24), dur = Theme.field(24), marks = Theme.field(24), neg = Theme.field(24), pass = Theme.field(24),
                start = Theme.field(24), end = Theme.field(24), att = Theme.field(24);
        List<String> subs = new ArrayList<>(ds.subjects()); subs.add("Mixed");
        JComboBox<String> subject = new JComboBox<>(subs.toArray(new String[0]));
        subject.setEditable(true); subject.setFont(Theme.BASE);
        JCheckBox rnd = new JCheckBox("Randomise question and option order", true);
        rnd.setBackground(Theme.WHITE);
        LinkedHashSet<String> chosen = new LinkedHashSet<>();
        JLabel countLbl = Theme.label("0 selected", 13, true, Theme.PRIMARY);
        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
        if (ex == null) {
            dur.setText("30"); marks.setText("1"); neg.setText("0"); pass.setText("40"); att.setText("1");
            start.setText(now.format(FileUtil.DT)); end.setText(now.plusDays(7).format(FileUtil.DT));
        } else {
            title.setText(ex.getTitle()); subject.setSelectedItem(ex.getSubject()); dur.setText("" + ex.getDurationMin());
            marks.setText("" + ex.getMarksPerQuestion()); neg.setText("" + ex.getNegativeMarks()); pass.setText("" + ex.getPassPercent());
            att.setText("" + ex.getMaxAttempts()); start.setText(ex.getStart().format(FileUtil.DT)); end.setText(ex.getEnd().format(FileUtil.DT));
            rnd.setSelected(ex.isRandomize()); chosen.addAll(ex.getQuestionIds());
        }
        countLbl.setText(chosen.size() + " selected");
        Theme.RButton pick = Theme.button("Select Questions...", Theme.INFO, e -> {
            QuestionSelectDialog d = new QuestionSelectDialog(this, chosen);
            d.setVisible(true);
            if (d.getSelection() != null) { chosen.clear(); chosen.addAll(d.getSelection()); countLbl.setText(chosen.size() + " selected"); }
        });
        JPanel qrow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        qrow.setBackground(Theme.WHITE);
        qrow.add(pick); qrow.add(countLbl);
        JPanel form = Theme.form(new String[]{"Title", "Subject", "Duration (minutes)", "Marks per question", "Negative marks per wrong",
                "Pass percentage", "Start (yyyy-MM-dd HH:mm)", "End (yyyy-MM-dd HH:mm)", "Max attempts", "Options", "Questions"},
                new JComponent[]{title, subject, dur, marks, neg, pass, start, end, att, rnd, qrow});
        while (true) {
            if (JOptionPane.showConfirmDialog(this, form, ex == null ? "Create Exam" : "Edit Exam",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
            try {
                String err = Validator.required(title.getText(), "Title");
                String subj = String.valueOf(subject.getEditor().getItem()).trim();
                if (err == null) err = Validator.required(subj, "Subject");
                int d = Integer.parseInt(dur.getText().trim());
                double mk = Double.parseDouble(marks.getText().trim()), ng = Double.parseDouble(neg.getText().trim()),
                        pc = Double.parseDouble(pass.getText().trim());
                int at = Integer.parseInt(att.getText().trim());
                LocalDateTime s = LocalDateTime.parse(start.getText().trim(), FileUtil.DT), en = LocalDateTime.parse(end.getText().trim(), FileUtil.DT);
                if (err == null && (d < 1 || d > 300)) err = "Duration must be between 1 and 300 minutes.";
                if (err == null && mk <= 0) err = "Marks per question must be greater than 0.";
                if (err == null && (ng < 0 || ng > mk)) err = "Negative marks must be between 0 and marks per question.";
                if (err == null && (pc < 1 || pc > 100)) err = "Pass percentage must be between 1 and 100.";
                if (err == null && (at < 1 || at > 10)) err = "Max attempts must be between 1 and 10.";
                if (err == null && !en.isAfter(s)) err = "End time must be after start time.";
                if (err == null && chosen.isEmpty()) err = "Select at least one question.";
                if (err != null) { Theme.error(this, err); continue; }
                if (ex == null) {
                    Exam e = new Exam(DataStore.newId("E"), title.getText().trim(), subj, d, mk, ng, pc, s, en, rnd.isSelected(), at, new ArrayList<>(chosen));
                    ds.exams.add(e);
                    ds.log(admin.getUsername(), "Created exam " + e.getTitle());
                } else {
                    ex.update(title.getText().trim(), subj, d, mk, ng, pc, s, en, rnd.isSelected(), at, new ArrayList<>(chosen));
                    ds.log(admin.getUsername(), "Edited exam " + ex.getTitle());
                }
                ds.saveExams();
                return;
            } catch (NumberFormatException ex1) {
                Theme.error(this, "Duration, marks, negative marks, pass % and attempts must be valid numbers.");
            } catch (java.time.format.DateTimeParseException ex2) {
                Theme.error(this, "Dates must use the format yyyy-MM-dd HH:mm (e.g. 2026-12-01 09:30).");
            }
        }
    }

    // ================= RESULTS =================
    private void buildResults() {
        DefaultTableModel m = Theme.model("ID", "Student", "Exam", "Score", "Total", "Percent", "Correct", "Wrong", "Skipped", "Result", "Time", "Submitted");
        JTable t = Theme.table(m);
        JTextField search = Theme.field(16);
        JComboBox<String> exam = Theme.combo("All Exams");
        JComboBox<String> res = Theme.combo("Pass & Fail", "PASS", "FAIL");
        Theme.attachFilter(t, search, new Theme.ColFilter(exam, 2), new Theme.ColFilter(res, 9));
        JPanel tb = Theme.toolbar(new JLabel("Search:"), search, exam, res,
                Theme.button("View Details", Theme.INFO, e -> {
                    int r = Theme.selected(this, t); if (r < 0) return;
                    new ResultDialog(this, ds.result(Theme.idOf(t, r)), false).setVisible(true);
                }),
                Theme.button("Delete", Theme.DANGER, e -> {
                    int r = Theme.selected(this, t); if (r < 0) return;
                    Result x = ds.result(Theme.idOf(t, r));
                    if (!Theme.confirm(this, "Delete result of " + x.getStudentName() + " for '" + x.getExamTitle() + "'?")) return;
                    ds.results.remove(x); ds.saveResults();
                    ds.log(admin.getUsername(), "Deleted result " + x.getId());
                    refreshAll();
                }),
                Theme.button("Export Report", Theme.SUCCESS, e -> exportReport()));
        JPanel p = Theme.page("Results", "All exam attempts with search and filters", tb);
        p.add(Theme.scroll(t), BorderLayout.CENTER);
        addPage("Results", p, () -> {
            Set<String> titles = new TreeSet<>();
            for (Result r : ds.results) titles.add(r.getExamTitle());
            Theme.resetCombo(exam, "All Exams", titles);
            m.setRowCount(0);
            for (Result r : ds.results)
                m.addRow(new Object[]{r.getId(), r.getStudentName(), r.getExamTitle(), r.getScore(), r.getTotalMarks(),
                        ExamService.r2(r.percent()), r.getCorrect(), r.getWrong(), r.getSkipped(), r.isPassed() ? "PASS" : "FAIL",
                        r.timeText(), r.getSubmittedAt().format(FileUtil.DT)});
        });
    }

    private void exportReport() {
        try {
            Path p = ExamService.exportReport();
            ds.log(admin.getUsername(), "Exported report " + p.getFileName());
            Theme.info(this, "Report saved to:\n" + p.toAbsolutePath());
        } catch (Exception ex) {
            Theme.error(this, "Could not export report: " + ex.getMessage());
        }
    }

    // ================= ANALYTICS =================
    private void buildAnalytics() {
        DefaultTableModel exams = Theme.model("Exam", "Attempts", "Avg %", "Highest %", "Lowest %", "Pass Rate %");
        DefaultTableModel board = Theme.model("Rank", "Student", "Attempts", "Avg %", "Best %", "Total Score");
        DefaultTableModel subj = Theme.model("Subject", "Correct", "Wrong", "Skipped", "Accuracy %");
        BarChart chart = new BarChart("Accuracy by subject (all students)");
        JPanel grid = new JPanel(new GridLayout(2, 2, 14, 14));
        grid.setOpaque(false);
        grid.add(Theme.titled("Exam statistics", Theme.scroll(Theme.table(exams))));
        grid.add(Theme.titled("Leaderboard - Top 10", Theme.scroll(Theme.table(board))));
        grid.add(Theme.titled("Subject analysis", Theme.scroll(Theme.table(subj))));
        grid.add(chart);
        JPanel tb = Theme.toolbar(Theme.button("Refresh", Theme.INFO, e -> refreshAll()),
                Theme.button("Export Report", Theme.SUCCESS, e -> exportReport()));
        JPanel p = Theme.page("Analytics", "Leaderboard, averages, pass rates and subject performance", tb);
        p.add(grid, BorderLayout.CENTER);
        addPage("Analytics", p, () -> {
            exams.setRowCount(0);
            for (Object[] r : ExamService.examStats()) exams.addRow(r);
            board.setRowCount(0);
            for (Object[] r : ExamService.leaderboard(10)) board.addRow(r);
            subj.setRowCount(0);
            Map<String, Double> cd = new LinkedHashMap<>();
            for (Map.Entry<String, int[]> en : ExamService.subjectStats(ds.results).entrySet()) {
                int[] a = en.getValue();
                subj.addRow(new Object[]{en.getKey(), a[0], a[1], a[2], ExamService.accuracy(a)});
                cd.put(en.getKey(), ExamService.accuracy(a));
            }
            chart.setData(cd);
        });
    }

    // ================= LOGS =================
    private void buildLogs() {
        DefaultTableModel m = Theme.model("Time", "User", "Action");
        JTable t = Theme.table(m);
        t.getColumnModel().getColumn(2).setPreferredWidth(600);
        JTextField search = Theme.field(20);
        Theme.attachFilter(t, search);
        JPanel tb = Theme.toolbar(new JLabel("Search:"), search,
                Theme.button("Refresh", Theme.INFO, e -> refreshAll()),
                Theme.button("Clear Logs", Theme.DANGER, e -> {
                    if (!Theme.confirm(this, "Permanently delete all activity logs?")) return;
                    ds.clearLogs(); ds.log(admin.getUsername(), "Cleared logs"); refreshAll();
                }));
        JPanel p = Theme.page("Activity Logs", "Logins, changes and exam submissions", tb);
        p.add(Theme.scroll(t), BorderLayout.CENTER);
        addPage("Logs", p, () -> {
            m.setRowCount(0);
            for (int i = ds.logs.size() - 1; i >= 0 && i >= ds.logs.size() - 2000; i--) {
                LogEntry l = ds.logs.get(i);
                m.addRow(new Object[]{l.getTime(), l.getUser(), l.getAction()});
            }
        });
    }
}
