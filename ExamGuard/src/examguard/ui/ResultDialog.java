package examguard.ui;

import examguard.model.*;
import examguard.service.DataStore;
import examguard.service.ExamService;
import java.awt.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

/** Score summary, subject analysis and full answer review with explanations. */
public class ResultDialog extends JDialog {
    private static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>");
    }

    public ResultDialog(Window owner, Result r, boolean allowCertificate) {
        super(owner, "Result - " + r.getExamTitle(), ModalityType.APPLICATION_MODAL);
        setSize(980, 720);
        setLocationRelativeTo(owner);
        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBackground(Theme.BG);
        root.setBorder(new EmptyBorder(16, 18, 16, 18));
        setContentPane(root);

        JPanel top = new JPanel(new BorderLayout(0, 10));
        top.setOpaque(false);
        top.add(Theme.label(r.getExamTitle() + " - " + r.getStudentName(), 20, true, Theme.TEXT), BorderLayout.NORTH);
        JPanel cards = new JPanel(new GridLayout(1, 5, 10, 0));
        cards.setOpaque(false);
        cards.add(new Theme.StatCard("Score", Theme.fmt(r.getScore()) + " / " + Theme.fmt(r.getTotalMarks()), Theme.PRIMARY));
        cards.add(new Theme.StatCard("Percentage", Theme.fmt(r.percent()) + "%", Theme.INFO));
        cards.add(new Theme.StatCard("Rank", "#" + ExamService.rankOf(r) + " of " + ExamService.ranking(r.getExamId()).size(), Theme.WARN));
        cards.add(new Theme.StatCard("Result", r.isPassed() ? "PASS" : "FAIL", r.isPassed() ? Theme.SUCCESS : Theme.DANGER));
        cards.add(new Theme.StatCard("Time taken", r.timeText(), Theme.MUTED));
        top.add(cards, BorderLayout.CENTER);
        top.add(Theme.label("Correct " + r.getCorrect() + "   |   Wrong " + r.getWrong() + "   |   Skipped " + r.getSkipped(),
                13, false, Theme.MUTED), BorderLayout.SOUTH);

        DefaultTableModel sm = Theme.model("Subject", "Correct", "Wrong", "Skipped", "Accuracy %");
        for (Map.Entry<String, int[]> en : ExamService.subjectStats(Collections.singletonList(r)).entrySet()) {
            int[] a = en.getValue();
            sm.addRow(new Object[]{en.getKey(), a[0], a[1], a[2], ExamService.accuracy(a)});
        }
        JScrollPane subj = Theme.scroll(Theme.table(sm));
        subj.setPreferredSize(new Dimension(100, 110));

        JEditorPane pane = new JEditorPane("text/html", buildReview(r));
        pane.setEditable(false);
        pane.setCaretPosition(0);

        JPanel mid = new JPanel(new BorderLayout(0, 8));
        mid.setOpaque(false);
        mid.add(Theme.titled("Subject analysis", subj), BorderLayout.NORTH);
        mid.add(Theme.titled("Answer review & explanations", Theme.scroll(pane)), BorderLayout.CENTER);

        JPanel btns = Theme.toolbar();
        ((FlowLayout) btns.getLayout()).setAlignment(FlowLayout.RIGHT);
        if (allowCertificate && r.isPassed())
            btns.add(Theme.button("View Certificate", Theme.SUCCESS, e -> new CertificateDialog(this, r).setVisible(true)));
        btns.add(Theme.button("Close", Theme.SIDEBAR, e -> dispose()));

        root.add(top, BorderLayout.NORTH);
        root.add(mid, BorderLayout.CENTER);
        root.add(btns, BorderLayout.SOUTH);
    }

    private String buildReview(Result r) {
        DataStore ds = DataStore.get();
        StringBuilder h = new StringBuilder("<html><body style='font-family:sans-serif;font-size:12px'>");
        int i = 1;
        for (Map.Entry<String, Integer> en : r.getAnswers().entrySet()) {
            Question q = ds.question(en.getKey());
            if (q == null) { h.append("<p>Q").append(i++).append(": (question was removed)</p>"); continue; }
            int sel = en.getValue();
            String status, color;
            if (sel < 0) { status = "SKIPPED"; color = "#B45309"; }
            else if (sel == q.getCorrect()) { status = "CORRECT"; color = "#047857"; }
            else { status = "WRONG"; color = "#B91C1C"; }
            h.append("<div style='border:1px solid #ccc;padding:6px;margin-bottom:8px'>")
             .append("<b>Q").append(i++).append(". ").append(esc(q.getText())).append("</b> ")
             .append("<span style='color:").append(color).append("'><b>[").append(status).append("]</b></span>")
             .append(" <i>(").append(esc(q.getSubject())).append(")</i><br>")
             .append("Your answer: ").append(sel < 0 ? "<i>not answered</i>" : esc((char) ('A' + sel) + ". " + q.getOptions()[sel])).append("<br>")
             .append("Correct answer: <b>").append(esc(q.correctText())).append("</b><br>")
             .append("<span style='color:#374151'>Explanation: ").append(esc(q.getExplanation())).append("</span></div>");
        }
        return h.append("</body></html>").toString();
    }
}
