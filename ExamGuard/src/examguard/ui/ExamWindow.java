package examguard.ui;

import examguard.model.*;
import examguard.service.ExamService;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

/** The exam-taking screen: timer, question palette, skip, bookmark and auto-submit. */
public class ExamWindow extends JFrame {
    private final Exam exam;
    private final Student student;
    private final Runnable onClose;
    private final List<Question> qs;
    private final List<int[]> orders = new ArrayList<>();
    private final Map<String, Integer> answers = new LinkedHashMap<>();
    private final Set<Integer> bookmarked = new HashSet<>(), visited = new HashSet<>();
    private int cur = 0, remaining;
    private boolean done;
    private final javax.swing.Timer timer;
    private JLabel timerLbl, progLbl, countsLbl;
    private JTextArea qText;
    private final JRadioButton[] opts = new JRadioButton[4];
    private final ButtonGroup group = new ButtonGroup();
    private Theme.RButton[] palette;
    private Theme.RButton bmBtn, prevBtn, nextBtn;

    public ExamWindow(Exam exam, Student student, Runnable onClose) {
        this.exam = exam; this.student = student; this.onClose = onClose;
        qs = new ArrayList<>(ExamService.questionsOf(exam));
        if (exam.isRandomize()) Collections.shuffle(qs);
        for (Question q : qs) {
            List<Integer> o = new ArrayList<>(Arrays.asList(0, 1, 2, 3));
            if (exam.isRandomize()) Collections.shuffle(o);
            orders.add(new int[]{o.get(0), o.get(1), o.get(2), o.get(3)});
            answers.put(q.getId(), -1);
        }
        remaining = exam.getDurationMin() * 60;
        setTitle("ExamGuard - " + exam.getTitle());
        setSize(1180, 740);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                if (!done && Theme.confirm(ExamWindow.this, "Closing the window will submit your exam now. Continue?")) submit(false);
            }
        });
        build();
        timer = new javax.swing.Timer(1000, e -> tick());
        updateTimer();
        show(0);
        timer.start();
    }

    private void build() {
        JPanel root = new JPanel(new BorderLayout(14, 14));
        root.setBackground(Theme.BG);
        root.setBorder(new EmptyBorder(14, 16, 14, 16));
        setContentPane(root);

        // top bar
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Theme.SIDEBAR);
        bar.setBorder(new EmptyBorder(12, 18, 12, 18));
        bar.add(Theme.label(exam.getTitle() + "  \u2022  " + student.getFullName(), 18, true, Color.WHITE), BorderLayout.WEST);
        timerLbl = Theme.label("00:00", 26, true, Color.WHITE);
        bar.add(timerLbl, BorderLayout.EAST);
        root.add(bar, BorderLayout.NORTH);

        // question area
        JPanel card = new JPanel(new BorderLayout(0, 14));
        card.setBackground(Theme.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(new LineBorder(Theme.BORDER), new EmptyBorder(18, 20, 18, 20)));
        progLbl = Theme.label("", 13, true, Theme.PRIMARY);
        qText = new JTextArea();
        qText.setEditable(false); qText.setLineWrap(true); qText.setWrapStyleWord(true);
        qText.setFont(Theme.BASE.deriveFont(Font.BOLD, 18f));
        JPanel head = new JPanel(new BorderLayout(0, 8));
        head.setOpaque(false);
        head.add(progLbl, BorderLayout.NORTH);
        head.add(qText, BorderLayout.CENTER);
        card.add(head, BorderLayout.NORTH);

        JPanel optPanel = new JPanel();
        optPanel.setOpaque(false);
        optPanel.setLayout(new BoxLayout(optPanel, BoxLayout.Y_AXIS));
        for (int i = 0; i < 4; i++) {
            final int k = i;
            JRadioButton rb = new JRadioButton();
            rb.setFont(Theme.BASE.deriveFont(16f));
            rb.setOpaque(true); rb.setBackground(Theme.WHITE);
            rb.setBorder(BorderFactory.createCompoundBorder(new LineBorder(Theme.BORDER, 1, true), new EmptyBorder(12, 12, 12, 12)));
            rb.setAlignmentX(Component.LEFT_ALIGNMENT);
            rb.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
            rb.addActionListener(e -> {
                answers.put(qs.get(cur).getId(), orders.get(cur)[k]);
                refreshPalette();
            });
            group.add(rb);
            opts[i] = rb;
            optPanel.add(rb);
            optPanel.add(Box.createVerticalStrut(10));
        }
        card.add(optPanel, BorderLayout.CENTER);

        prevBtn = Theme.button("\u25C0 Previous", Theme.SIDEBAR, e -> show(cur - 1));
        nextBtn = Theme.button("Next \u25B6", Theme.PRIMARY, e -> show(cur + 1));
        Theme.RButton skip = Theme.button("Skip \u21B7", Theme.INFO, e -> { if (cur < qs.size() - 1) show(cur + 1); else Theme.info(this, "This is the last question. Use the palette to revisit skipped questions."); });
        Theme.RButton clear = Theme.button("Clear Answer", Theme.MUTED, e -> {
            answers.put(qs.get(cur).getId(), -1); group.clearSelection(); refreshPalette();
        });
        bmBtn = Theme.button("Bookmark", Theme.WARN, e -> {
            if (!bookmarked.remove(cur)) bookmarked.add(cur);
            updateBookmarkText(); refreshPalette();
        });
        Theme.RButton submit = Theme.button("Submit Exam", Theme.SUCCESS, e -> confirmSubmit());
        JPanel nav = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        nav.setOpaque(false);
        nav.add(prevBtn); nav.add(skip); nav.add(clear); nav.add(bmBtn); nav.add(nextBtn);
        JPanel south = new JPanel(new BorderLayout());
        south.setOpaque(false);
        south.add(nav, BorderLayout.WEST);
        south.add(submit, BorderLayout.EAST);
        card.add(south, BorderLayout.SOUTH);
        root.add(card, BorderLayout.CENTER);

        // palette
        JPanel side = new JPanel(new BorderLayout(0, 8));
        side.setBackground(Theme.WHITE);
        side.setBorder(BorderFactory.createCompoundBorder(new LineBorder(Theme.BORDER), new EmptyBorder(12, 12, 12, 12)));
        side.setPreferredSize(new Dimension(300, 100));
        side.add(Theme.label("Question palette", 15, true, Theme.TEXT), BorderLayout.NORTH);
        JPanel grid = new JPanel(new GridLayout(0, 5, 6, 6));
        grid.setOpaque(false);
        palette = new Theme.RButton[qs.size()];
        for (int i = 0; i < qs.size(); i++) {
            final int idx = i;
            palette[i] = Theme.button(String.valueOf(i + 1), Theme.MUTED, e -> show(idx));
            palette[i].setBorder(new EmptyBorder(8, 4, 8, 4));
            grid.add(palette[i]);
        }
        JPanel gridWrap = new JPanel(new BorderLayout());
        gridWrap.setOpaque(false);
        gridWrap.add(grid, BorderLayout.NORTH);
        side.add(Theme.scroll(gridWrap), BorderLayout.CENTER);
        JPanel legend = new JPanel(new GridLayout(0, 1, 0, 3));
        legend.setOpaque(false);
        countsLbl = Theme.label("", 12, true, Theme.TEXT);
        legend.add(countsLbl);
        legend.add(legendItem(Theme.PRIMARY, "Current"));
        legend.add(legendItem(Theme.SUCCESS, "Answered"));
        legend.add(legendItem(Theme.WARN, "Bookmarked"));
        legend.add(legendItem(new Color(0xF87171), "Visited / skipped"));
        legend.add(legendItem(Theme.MUTED, "Not visited"));
        legend.add(Theme.label(String.format("Marking: +%s / -%s per wrong", Theme.fmt(exam.getMarksPerQuestion()), Theme.fmt(exam.getNegativeMarks())), 12, false, Theme.MUTED));
        side.add(legend, BorderLayout.SOUTH);
        root.add(side, BorderLayout.EAST);
    }

    private JLabel legendItem(Color c, String text) {
        JLabel l = Theme.label("\u25A0 " + text, 12, false, Theme.TEXT);
        l.setIcon(null);
        l.setForeground(Theme.TEXT);
        l.setText("<html><font color='#" + String.format("%06x", c.getRGB() & 0xFFFFFF) + "'>\u25A0</font> " + text + "</html>");
        return l;
    }

    private void show(int i) {
        if (i < 0 || i >= qs.size()) return;
        cur = i;
        visited.add(i);
        Question q = qs.get(i);
        int[] ord = orders.get(i);
        group.clearSelection();
        int chosen = answers.get(q.getId());
        for (int k = 0; k < 4; k++) {
            opts[k].setText((char) ('A' + k) + ".  " + q.getOptions()[ord[k]]);
            if (chosen == ord[k]) opts[k].setSelected(true);
        }
        qText.setText(q.getText());
        progLbl.setText("Question " + (i + 1) + " of " + qs.size() + "   \u2022   " + q.getSubject());
        prevBtn.setEnabled(i > 0);
        nextBtn.setEnabled(i < qs.size() - 1);
        updateBookmarkText();
        refreshPalette();
    }

    private void updateBookmarkText() { bmBtn.setText(bookmarked.contains(cur) ? "Remove Bookmark" : "Bookmark"); }

    private void refreshPalette() {
        int answered = 0;
        for (int i = 0; i < qs.size(); i++) {
            boolean ans = answers.get(qs.get(i).getId()) >= 0;
            if (ans) answered++;
            Color c;
            if (i == cur) c = Theme.PRIMARY;
            else if (bookmarked.contains(i)) c = Theme.WARN;
            else if (ans) c = Theme.SUCCESS;
            else if (visited.contains(i)) c = new Color(0xF87171);
            else c = Theme.MUTED;
            palette[i].setBase(c);
        }
        countsLbl.setText("Answered " + answered + "/" + qs.size() + "  |  Bookmarked " + bookmarked.size());
    }

    private void tick() {
        remaining--;
        updateTimer();
        if (remaining <= 0) submit(true);
    }

    private void updateTimer() {
        int r = Math.max(0, remaining);
        timerLbl.setText(String.format("\u23F1 %02d:%02d", r / 60, r % 60));
        timerLbl.setForeground(r <= 60 ? new Color(0xFCA5A5) : Color.WHITE);
    }

    private void confirmSubmit() {
        int answered = 0;
        for (int v : answers.values()) if (v >= 0) answered++;
        String msg = "Answered: " + answered + "\nUnanswered: " + (qs.size() - answered) + "\nBookmarked: " + bookmarked.size()
                + "\n\nSubmit the exam now?";
        if (Theme.confirm(this, msg)) submit(false);
    }

    private void submit(boolean auto) {
        if (done) return;
        done = true;
        timer.stop();
        int used = exam.getDurationMin() * 60 - Math.max(remaining, 0);
        Result r = ExamService.grade(exam, student, answers, used);
        ExamService.saveResult(r);
        dispose();
        if (auto) Theme.info(null, "Time is up! Your exam was submitted automatically.");
        new ResultDialog(null, r, true).setVisible(true);
        onClose.run();
    }
}
