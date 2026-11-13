package examguard.ui;

import examguard.model.*;
import examguard.service.AuthService;
import examguard.service.DataStore;
import examguard.service.ExamService;
import examguard.util.FileUtil;
import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

/** Student portal: exams, history, rankings, subject analysis and profile. Only for the STUDENT role. */
public class StudentDashboard extends JFrame {
    private final DataStore ds = DataStore.get();
    private final Student me;
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final JPanel sidebar = new JPanel();
    private final Map<String, Runnable> refreshers = new LinkedHashMap<>();
    private final Map<String, Theme.RButton> nav = new LinkedHashMap<>();

    public StudentDashboard() {
        AuthService.requireRole(Role.STUDENT);
        me = (Student) AuthService.current();
        setTitle("ExamGuard - Student Portal");
        setSize(1280, 760);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(Theme.SIDEBAR);
        sidebar.setBorder(new EmptyBorder(20, 12, 20, 12));
        sidebar.setPreferredSize(new Dimension(230, 100));
        sidebar.add(Theme.label("ExamGuard", 24, true, Color.WHITE));
        sidebar.add(Theme.label(me.getFullName() + " (" + me.getRollNo() + ")", 12, false, new Color(0x9CA3AF)));
        sidebar.add(Box.createVerticalStrut(24));

        buildHome(); buildExams(); buildHistory(); buildRankings(); buildAnalysis(); buildProfile();

        sidebar.add(Box.createVerticalGlue());
        Theme.RButton out = Theme.button("Logout", Theme.DANGER, e -> {
            if (!Theme.confirm(this, "Log out?")) return;
            AuthService.logout(); dispose(); new LoginFrame().setVisible(true);
        });
        out.setAlignmentX(Component.LEFT_ALIGNMENT);
        out.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        sidebar.add(out);
        add(sidebar, BorderLayout.WEST);
        add(content, BorderLayout.CENTER);
        showPage("Home");
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

    // ================= HOME =================
    private void buildHome() {
        JPanel p = Theme.page("Welcome, " + me.getFullName(), "Your performance at a glance", null);
        Theme.StatCard c1 = new Theme.StatCard("Exams taken", "0", Theme.PRIMARY), c2 = new Theme.StatCard("Average score", "0%", Theme.INFO),
                c3 = new Theme.StatCard("Best score", "0%", Theme.SUCCESS), c4 = new Theme.StatCard("Pass rate", "0%", Theme.WARN),
                c5 = new Theme.StatCard("Exams open now", "0", Theme.DANGER);
        JPanel row = new JPanel(new GridLayout(1, 5, 12, 0));
        row.setOpaque(false);
        row.setPreferredSize(new Dimension(100, 95));
        for (Theme.StatCard c : new Theme.StatCard[]{c1, c2, c3, c4, c5}) row.add(c);
        DefaultTableModel m = Theme.model("Exam", "Score", "Percent", "Rank", "Result", "Submitted");
        JPanel body = new JPanel(new BorderLayout(0, 16));
        body.setOpaque(false);
        body.add(row, BorderLayout.NORTH);
        body.add(Theme.titled("Recent results", Theme.scroll(Theme.table(m))), BorderLayout.CENTER);
        p.add(body, BorderLayout.CENTER);
        addPage("Home", p, () -> {
            List<Result> rs = ExamService.resultsOf(me.getId());
            double best = 0;
            for (Result r : rs) best = Math.max(best, r.percent());
            int open = 0;
            for (Exam e : ds.exams) if (e.status().equals("Open")) open++;
            c1.setValue("" + rs.size());
            c2.setValue(Theme.fmt(ExamService.avgPercent(rs)) + "%");
            c3.setValue(Theme.fmt(best) + "%");
            c4.setValue(Theme.fmt(ExamService.passRate(rs)) + "%");
            c5.setValue("" + open);
            m.setRowCount(0);
            List<Result> sorted = new ArrayList<>(rs);
            sorted.sort((a, b) -> b.getSubmittedAt().compareTo(a.getSubmittedAt()));
            for (int i = 0; i < sorted.size() && i < 6; i++) {
                Result r = sorted.get(i);
                m.addRow(new Object[]{r.getExamTitle(), r.getScore() + " / " + r.getTotalMarks(), Theme.fmt(r.percent()) + "%",
                        "#" + ExamService.rankOf(r), r.isPassed() ? "PASS" : "FAIL", r.getSubmittedAt().format(FileUtil.DT)});
            }
        });
    }

    // ================= EXAMS =================
    private void buildExams() {
        DefaultTableModel m = Theme.model("ID", "Title", "Subject", "Questions", "Duration (min)", "Total Marks", "Negative", "Start", "End", "Status", "Attempts");
        JTable t = Theme.table(m);
        JTextField search = Theme.field(16);
        JComboBox<String> status = Theme.combo("All Status", "Upcoming", "Open", "Closed");
        Theme.attachFilter(t, search, new Theme.ColFilter(status, 9));
        JPanel tb = Theme.toolbar(new JLabel("Search:"), search, status,
                Theme.button("Start Exam", Theme.SUCCESS, e -> {
                    int r = Theme.selected(this, t); if (r < 0) return;
                    startExam(ds.exam(Theme.idOf(t, r)));
                }));
        JPanel p = Theme.page("Available Exams", "Select an exam and press Start Exam", tb);
        p.add(Theme.scroll(t), BorderLayout.CENTER);
        addPage("Exams", p, () -> {
            m.setRowCount(0);
            for (Exam e : ds.exams)
                m.addRow(new Object[]{e.getId(), e.getTitle(), e.getSubject(), e.getQuestionIds().size(), e.getDurationMin(),
                        e.totalMarks(), e.getNegativeMarks(), e.getStart().format(FileUtil.DT), e.getEnd().format(FileUtil.DT),
                        e.status(), ExamService.attemptsUsed(me.getId(), e.getId()) + " / " + e.getMaxAttempts()});
        });
    }

    private void startExam(Exam e) {
        String block = ExamService.startBlocker(me, e);
        if (block != null) { Theme.warn(this, block); return; }
        String msg = "<html><b>" + e.getTitle() + "</b><br><br>Questions: " + ExamService.questionsOf(e).size()
                + "<br>Duration: " + e.getDurationMin() + " minutes<br>Marks per question: " + e.getMarksPerQuestion()
                + "<br>Negative marking: " + e.getNegativeMarks() + " per wrong answer<br>Pass mark: " + e.getPassPercent() + "%"
                + "<br><br>The exam is auto-submitted when the timer ends.<br>You can skip and bookmark questions.<br><br>Start now?</html>";
        if (JOptionPane.showConfirmDialog(this, msg, "Exam instructions", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) != JOptionPane.YES_OPTION) return;
        ds.log(me.getUsername(), "Started exam '" + e.getTitle() + "'");
        setVisible(false);
        new ExamWindow(e, me, () -> { refreshAll(); showPage("History"); setVisible(true); }).setVisible(true);
    }

    // ================= HISTORY =================
    private void buildHistory() {
        DefaultTableModel m = Theme.model("ID", "Exam", "Score", "Total", "Percent", "Rank", "Result", "Time", "Submitted");
        JTable t = Theme.table(m);
        JTextField search = Theme.field(16);
        JComboBox<String> res = Theme.combo("Pass & Fail", "PASS", "FAIL");
        Theme.attachFilter(t, search, new Theme.ColFilter(res, 6));
        JPanel tb = Theme.toolbar(new JLabel("Search:"), search, res,
                Theme.button("View Details & Review", Theme.INFO, e -> {
                    int r = Theme.selected(this, t); if (r < 0) return;
                    new ResultDialog(this, ds.result(Theme.idOf(t, r)), true).setVisible(true);
                }),
                Theme.button("Certificate", Theme.SUCCESS, e -> {
                    int r = Theme.selected(this, t); if (r < 0) return;
                    Result x = ds.result(Theme.idOf(t, r));
                    if (!x.isPassed()) { Theme.warn(this, "Certificates are only issued for passed exams."); return; }
                    new CertificateDialog(this, x).setVisible(true);
                }));
        JPanel p = Theme.page("My History", "All your exam attempts", tb);
        p.add(Theme.scroll(t), BorderLayout.CENTER);
        addPage("History", p, () -> {
            m.setRowCount(0);
            for (Result r : ExamService.resultsOf(me.getId()))
                m.addRow(new Object[]{r.getId(), r.getExamTitle(), r.getScore(), r.getTotalMarks(), ExamService.r2(r.percent()),
                        "#" + ExamService.rankOf(r), r.isPassed() ? "PASS" : "FAIL", r.timeText(), r.getSubmittedAt().format(FileUtil.DT)});
        });
    }

    // ================= RANKINGS =================
    private void buildRankings() {
        DefaultTableModel exM = Theme.model("Rank", "Student", "Score", "Percent", "Time", "Submitted");
        DefaultTableModel boardM = Theme.model("Rank", "Student", "Attempts", "Avg %", "Best %", "Total Score");
        JComboBox<String> exam = new JComboBox<>();
        exam.setFont(Theme.BASE);
        JLabel myRank = Theme.label(" ", 14, true, Theme.PRIMARY);
        JPanel left = new JPanel(new BorderLayout(0, 6));
        left.setOpaque(false);
        JPanel lt = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        lt.setOpaque(false);
        lt.add(Theme.label("Exam ranking:", 15, true, Theme.TEXT)); lt.add(exam); lt.add(myRank);
        left.add(lt, BorderLayout.NORTH);
        left.add(Theme.scroll(Theme.table(exM)), BorderLayout.CENTER);
        JLabel overall = Theme.label(" ", 13, true, Theme.PRIMARY);
        JPanel right = Theme.titled("Overall leaderboard - Top 10", Theme.scroll(Theme.table(boardM)));
        right.add(overall, BorderLayout.SOUTH);
        JPanel grid = new JPanel(new GridLayout(2, 1, 0, 14));
        grid.setOpaque(false);
        grid.add(left); grid.add(right);
        JPanel p = Theme.page("Rankings", "Compare yourself with other students", null);
        p.add(grid, BorderLayout.CENTER);

        final boolean[] loading = {false};
        Runnable fillExam = () -> {
            exM.setRowCount(0);
            myRank.setText(" ");
            String sel = (String) exam.getSelectedItem();
            if (sel == null) return;
            String id = sel.substring(0, sel.indexOf(" - "));
            int i = 1;
            for (Result r : ExamService.ranking(id)) {
                exM.addRow(new Object[]{i, r.getStudentName(), r.getScore(), ExamService.r2(r.percent()), r.timeText(), r.getSubmittedAt().format(FileUtil.DT)});
                if (r.getStudentId().equals(me.getId()) && !myRank.getText().startsWith("Your")) myRank.setText("Your best rank: #" + i);
                i++;
            }
        };
        exam.addActionListener(e -> { if (!loading[0]) fillExam.run(); });
        addPage("Rankings", p, () -> {
            loading[0] = true;
            Object prev = exam.getSelectedItem();
            exam.removeAllItems();
            for (Exam e : ds.exams) if (!ExamService.ranking(e.getId()).isEmpty()) exam.addItem(e.getId() + " - " + e.getTitle());
            if (prev != null) exam.setSelectedItem(prev);
            loading[0] = false;
            fillExam.run();
            boardM.setRowCount(0);
            overall.setText("Not ranked yet");
            for (Object[] r : ExamService.leaderboard(1000)) {
                if (((Integer) r[0]) <= 10) boardM.addRow(r);
                if (r[1].equals(me.getFullName())) overall.setText("Your overall position: #" + r[0]);
            }
        });
    }

    // ================= ANALYSIS =================
    private void buildAnalysis() {
        DefaultTableModel m = Theme.model("Subject", "Correct", "Wrong", "Skipped", "Accuracy %");
        BarChart chart = new BarChart("Your accuracy by subject");
        JPanel grid = new JPanel(new GridLayout(1, 2, 14, 0));
        grid.setOpaque(false);
        grid.add(Theme.titled("Subject-wise breakdown", Theme.scroll(Theme.table(m))));
        grid.add(chart);
        JPanel p = Theme.page("Subject Analysis", "Find your strong and weak subjects", null);
        p.add(grid, BorderLayout.CENTER);
        addPage("Analysis", p, () -> {
            m.setRowCount(0);
            Map<String, Double> cd = new LinkedHashMap<>();
            for (Map.Entry<String, int[]> en : ExamService.subjectStats(ExamService.resultsOf(me.getId())).entrySet()) {
                int[] a = en.getValue();
                m.addRow(new Object[]{en.getKey(), a[0], a[1], a[2], ExamService.accuracy(a)});
                cd.put(en.getKey(), ExamService.accuracy(a));
            }
            chart.setData(cd);
        });
    }

    // ================= PROFILE =================
    private void buildProfile() {
        JPanel p = Theme.page("Profile", "Account details and password", null);
        JPanel card = new JPanel(new BorderLayout(0, 12));
        card.setBackground(Theme.WHITE);
        card.setBorder(new EmptyBorder(20, 20, 20, 20));
        JPanel info = Theme.form(new String[]{"Name", "Roll no", "Username", "Email", "Member since"},
                new JComponent[]{Theme.label(me.getFullName(), 14, false, Theme.TEXT), Theme.label(me.getRollNo(), 14, false, Theme.TEXT),
                        Theme.label(me.getUsername(), 14, false, Theme.TEXT), Theme.label(me.getEmail(), 14, false, Theme.TEXT),
                        Theme.label(me.getCreatedAt(), 14, false, Theme.TEXT)});
        JPasswordField oldP = Theme.pass(18), newP = Theme.pass(18), conf = Theme.pass(18);
        JPanel pw = Theme.form(new String[]{"Current password", "New password", "Confirm new"}, new JComponent[]{oldP, newP, conf});
        Theme.RButton change = Theme.button("Change Password", Theme.PRIMARY, e -> {
            try {
                AuthService.changePassword(me, new String(oldP.getPassword()), new String(newP.getPassword()), new String(conf.getPassword()));
                oldP.setText(""); newP.setText(""); conf.setText("");
                Theme.info(this, "Password changed successfully.");
            } catch (AuthService.AuthException ex) { Theme.error(this, ex.getMessage()); }
        });
        JPanel pwBox = new JPanel(new BorderLayout(0, 8));
        pwBox.setOpaque(false);
        pwBox.add(Theme.label("Change password", 16, true, Theme.TEXT), BorderLayout.NORTH);
        pwBox.add(pw, BorderLayout.CENTER);
        JPanel bw = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bw.setOpaque(false); bw.add(change);
        pwBox.add(bw, BorderLayout.SOUTH);
        card.add(info, BorderLayout.NORTH);
        card.add(pwBox, BorderLayout.CENTER);
        JPanel wrap = new JPanel(new FlowLayout(FlowLayout.LEFT));
        wrap.setOpaque(false);
        card.setPreferredSize(new Dimension(520, 460));
        wrap.add(card);
        p.add(wrap, BorderLayout.CENTER);
        addPage("Profile", p, () -> {});
    }
}
