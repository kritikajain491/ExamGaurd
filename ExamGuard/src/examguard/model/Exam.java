package examguard.model;

import examguard.util.FileUtil;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** An exam definition: schedule, duration, marking scheme and the questions it uses. */
public class Exam {
    private final String id;
    private String title, subject;
    private int durationMin, maxAttempts;
    private double marksPerQuestion, negativeMarks, passPercent;
    private LocalDateTime start, end;
    private boolean randomize;
    private List<String> questionIds = new ArrayList<>();

    public Exam(String id, String title, String subject, int durationMin, double marksPerQuestion, double negativeMarks,
                double passPercent, LocalDateTime start, LocalDateTime end, boolean randomize, int maxAttempts,
                List<String> questionIds) {
        this.id = id; this.title = title; this.subject = subject; this.durationMin = durationMin;
        this.marksPerQuestion = marksPerQuestion; this.negativeMarks = negativeMarks; this.passPercent = passPercent;
        this.start = start; this.end = end; this.randomize = randomize; this.maxAttempts = maxAttempts;
        this.questionIds = new ArrayList<>(questionIds);
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getSubject() { return subject; }
    public int getDurationMin() { return durationMin; }
    public int getMaxAttempts() { return maxAttempts; }
    public double getMarksPerQuestion() { return marksPerQuestion; }
    public double getNegativeMarks() { return negativeMarks; }
    public double getPassPercent() { return passPercent; }
    public LocalDateTime getStart() { return start; }
    public LocalDateTime getEnd() { return end; }
    public boolean isRandomize() { return randomize; }
    public List<String> getQuestionIds() { return questionIds; }
    public double totalMarks() { return questionIds.size() * marksPerQuestion; }

    public void update(String title, String subject, int durationMin, double marks, double neg, double pass,
                       LocalDateTime start, LocalDateTime end, boolean randomize, int maxAttempts, List<String> qids) {
        this.title = title; this.subject = subject; this.durationMin = durationMin; this.marksPerQuestion = marks;
        this.negativeMarks = neg; this.passPercent = pass; this.start = start; this.end = end;
        this.randomize = randomize; this.maxAttempts = maxAttempts; this.questionIds = new ArrayList<>(qids);
    }

    public String status() {
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(start)) return "Upcoming";
        if (now.isAfter(end)) return "Closed";
        return "Open";
    }

    public String[] toRecord() {
        return new String[]{id, title, subject, String.valueOf(durationMin), String.valueOf(marksPerQuestion),
                String.valueOf(negativeMarks), String.valueOf(passPercent), start.format(FileUtil.DT),
                end.format(FileUtil.DT), String.valueOf(randomize), String.valueOf(maxAttempts),
                String.join(",", questionIds)};
    }

    public static Exam fromRecord(String[] f) {
        List<String> ids = new ArrayList<>();
        for (String s : f[11].split(",")) if (!s.isEmpty()) ids.add(s);
        return new Exam(f[0], f[1], f[2], Integer.parseInt(f[3]), Double.parseDouble(f[4]), Double.parseDouble(f[5]),
                Double.parseDouble(f[6]), LocalDateTime.parse(f[7], FileUtil.DT), LocalDateTime.parse(f[8], FileUtil.DT),
                Boolean.parseBoolean(f[9]), Integer.parseInt(f[10]), ids);
    }
}
