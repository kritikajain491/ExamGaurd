package examguard.model;

import examguard.util.FileUtil;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/** One graded exam attempt. 'answers' maps question id -> selected option index (-1 = skipped). */
public class Result {
    private final String id, examId, examTitle, studentId, studentName;
    private final double score, totalMarks;
    private final int correct, wrong, skipped, timeTakenSec;
    private final boolean passed;
    private LocalDateTime submittedAt;
    private final Map<String, Integer> answers;

    public Result(String id, String examId, String examTitle, String studentId, String studentName, double score,
                  double totalMarks, int correct, int wrong, int skipped, boolean passed, int timeTakenSec,
                  LocalDateTime submittedAt, Map<String, Integer> answers) {
        this.id = id; this.examId = examId; this.examTitle = examTitle; this.studentId = studentId;
        this.studentName = studentName; this.score = score; this.totalMarks = totalMarks; this.correct = correct;
        this.wrong = wrong; this.skipped = skipped; this.passed = passed; this.timeTakenSec = timeTakenSec;
        this.submittedAt = submittedAt; this.answers = new LinkedHashMap<>(answers);
    }

    public String getId() { return id; }
    public String getExamId() { return examId; }
    public String getExamTitle() { return examTitle; }
    public String getStudentId() { return studentId; }
    public String getStudentName() { return studentName; }
    public double getScore() { return score; }
    public double getTotalMarks() { return totalMarks; }
    public int getCorrect() { return correct; }
    public int getWrong() { return wrong; }
    public int getSkipped() { return skipped; }
    public boolean isPassed() { return passed; }
    public int getTimeTakenSec() { return timeTakenSec; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime t) { submittedAt = t; }
    public Map<String, Integer> getAnswers() { return answers; }

    public double percent() { return totalMarks > 0 ? Math.max(0, score) / totalMarks * 100.0 : 0; }
    public String timeText() { return String.format("%02d:%02d", timeTakenSec / 60, timeTakenSec % 60); }

    public String[] toRecord() {
        StringBuilder a = new StringBuilder();
        for (Map.Entry<String, Integer> e : answers.entrySet()) {
            if (a.length() > 0) a.append(',');
            a.append(e.getKey()).append(':').append(e.getValue());
        }
        return new String[]{id, examId, examTitle, studentId, studentName, String.valueOf(score), String.valueOf(totalMarks),
                String.valueOf(correct), String.valueOf(wrong), String.valueOf(skipped), String.valueOf(passed),
                String.valueOf(timeTakenSec), submittedAt.format(FileUtil.DT_SEC), a.toString()};
    }

    public static Result fromRecord(String[] f) {
        Map<String, Integer> ans = new LinkedHashMap<>();
        for (String s : f[13].split(",")) {
            int i = s.lastIndexOf(':');
            if (i > 0) ans.put(s.substring(0, i), Integer.parseInt(s.substring(i + 1)));
        }
        return new Result(f[0], f[1], f[2], f[3], f[4], Double.parseDouble(f[5]), Double.parseDouble(f[6]),
                Integer.parseInt(f[7]), Integer.parseInt(f[8]), Integer.parseInt(f[9]), Boolean.parseBoolean(f[10]),
                Integer.parseInt(f[11]), LocalDateTime.parse(f[12], FileUtil.DT_SEC), ans);
    }
}
