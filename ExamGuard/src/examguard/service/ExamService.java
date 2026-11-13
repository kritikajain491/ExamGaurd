package examguard.service;

import examguard.model.*;
import examguard.util.FileUtil;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;

/** Grading, ranking and analytics. */
public class ExamService {
    private ExamService() {}

    public static double r2(double v) { return Math.round(v * 100.0) / 100.0; }

    public static List<Question> questionsOf(Exam e) {
        List<Question> l = new ArrayList<>();
        for (String id : e.getQuestionIds()) { Question q = DataStore.get().question(id); if (q != null) l.add(q); }
        return l;
    }

    public static int attemptsUsed(String studentId, String examId) {
        int n = 0;
        for (Result r : DataStore.get().results) if (r.getStudentId().equals(studentId) && r.getExamId().equals(examId)) n++;
        return n;
    }

    /** Returns null if the student may start the exam, otherwise the reason. */
    public static String startBlocker(Student s, Exam e) {
        if (questionsOf(e).isEmpty()) return "This exam has no questions yet.";
        String st = e.status();
        if (st.equals("Upcoming")) return "This exam has not started yet. It opens on " + e.getStart().format(FileUtil.DT) + ".";
        if (st.equals("Closed")) return "This exam window has closed.";
        if (attemptsUsed(s.getId(), e.getId()) >= e.getMaxAttempts())
            return "You have used all " + e.getMaxAttempts() + " attempt(s) for this exam.";
        return null;
    }

    public static Result grade(Exam e, Student s, Map<String, Integer> answers, int timeSec) {
        DataStore ds = DataStore.get();
        double score = 0; int c = 0, w = 0, sk = 0, n = 0;
        for (Map.Entry<String, Integer> en : answers.entrySet()) {
            Question q = ds.question(en.getKey());
            if (q == null) continue;
            n++;
            int sel = en.getValue();
            if (sel < 0) sk++;
            else if (sel == q.getCorrect()) { c++; score += e.getMarksPerQuestion(); }
            else { w++; score -= e.getNegativeMarks(); }
        }
        score = r2(Math.max(0, score));
        double total = r2(n * e.getMarksPerQuestion());
        boolean passed = total > 0 && score / total * 100.0 >= e.getPassPercent();
        return new Result(DataStore.newId("R"), e.getId(), e.getTitle(), s.getId(), s.getFullName(), score, total, c, w, sk,
                passed, timeSec, LocalDateTime.now(), answers);
    }

    public static void saveResult(Result r) {
        DataStore ds = DataStore.get();
        ds.results.add(r);
        ds.saveResults();
        ds.log(r.getStudentName(), "Submitted exam '" + r.getExamTitle() + "' score " + r.getScore() + "/" + r.getTotalMarks());
    }

    public static List<Result> resultsOf(String studentId) {
        List<Result> l = new ArrayList<>();
        for (Result r : DataStore.get().results) if (r.getStudentId().equals(studentId)) l.add(r);
        return l;
    }

    public static List<Result> ranking(String examId) {
        List<Result> l = new ArrayList<>();
        for (Result r : DataStore.get().results) if (r.getExamId().equals(examId)) l.add(r);
        l.sort((a, b) -> {
            int c = Double.compare(b.getScore(), a.getScore());
            return c != 0 ? c : Integer.compare(a.getTimeTakenSec(), b.getTimeTakenSec());
        });
        return l;
    }

    public static int rankOf(Result r) {
        List<Result> l = ranking(r.getExamId());
        for (int i = 0; i < l.size(); i++) if (l.get(i).getId().equals(r.getId())) return i + 1;
        return 0;
    }

    public static double avgPercent(List<Result> l) {
        if (l.isEmpty()) return 0;
        double s = 0;
        for (Result r : l) s += r.percent();
        return s / l.size();
    }

    public static double passRate(List<Result> l) {
        if (l.isEmpty()) return 0;
        int p = 0;
        for (Result r : l) if (r.isPassed()) p++;
        return p * 100.0 / l.size();
    }

    /** subject -> {correct, wrong, skipped}. */
    public static Map<String, int[]> subjectStats(List<Result> rs) {
        Map<String, int[]> m = new TreeMap<>();
        DataStore ds = DataStore.get();
        for (Result r : rs)
            for (Map.Entry<String, Integer> en : r.getAnswers().entrySet()) {
                Question q = ds.question(en.getKey());
                if (q == null) continue;
                int[] a = m.computeIfAbsent(q.getSubject(), k -> new int[3]);
                int sel = en.getValue();
                if (sel < 0) a[2]++; else if (sel == q.getCorrect()) a[0]++; else a[1]++;
            }
        return m;
    }

    public static double accuracy(int[] a) {
        int t = a[0] + a[1] + a[2];
        return t == 0 ? 0 : r2(a[0] * 100.0 / t);
    }

    /** Rows: {rank, name, attempts, avg%, best%, totalScore} sorted by average percentage. */
    public static List<Object[]> leaderboard(int limit) {
        Map<String, List<Result>> by = new LinkedHashMap<>();
        for (Result r : DataStore.get().results) by.computeIfAbsent(r.getStudentId(), k -> new ArrayList<>()).add(r);
        List<Object[]> rows = new ArrayList<>();
        for (List<Result> l : by.values()) {
            double best = 0, total = 0;
            for (Result r : l) { best = Math.max(best, r.percent()); total += r.getScore(); }
            rows.add(new Object[]{l.get(0).getStudentName(), l.size(), r2(avgPercent(l)), r2(best), r2(total)});
        }
        rows.sort((a, b) -> {
            int c = Double.compare((Double) b[2], (Double) a[2]);
            return c != 0 ? c : Double.compare((Double) b[4], (Double) a[4]);
        });
        List<Object[]> out = new ArrayList<>();
        for (int i = 0; i < rows.size() && i < limit; i++) {
            Object[] r = rows.get(i);
            out.add(new Object[]{i + 1, r[0], r[1], r[2], r[3], r[4]});
        }
        return out;
    }

    /** Rows: {exam, attempts, avg%, highest%, lowest%, passRate%}. */
    public static List<Object[]> examStats() {
        List<Object[]> rows = new ArrayList<>();
        for (Exam e : DataStore.get().exams) {
            List<Result> l = new ArrayList<>();
            for (Result r : DataStore.get().results) if (r.getExamId().equals(e.getId())) l.add(r);
            double hi = 0, lo = l.isEmpty() ? 0 : 100;
            for (Result r : l) { hi = Math.max(hi, r.percent()); lo = Math.min(lo, r.percent()); }
            rows.add(new Object[]{e.getTitle(), l.size(), r2(avgPercent(l)), r2(hi), r2(lo), r2(passRate(l))});
        }
        return rows;
    }

    public static String buildReport() {
        DataStore ds = DataStore.get();
        StringBuilder sb = new StringBuilder();
        sb.append("EXAMGUARD REPORT - generated ").append(LocalDateTime.now().format(FileUtil.DT_SEC)).append("\n");
        sb.append("=".repeat(70)).append("\n");
        sb.append(String.format("Students: %d | Questions: %d | Exams: %d | Attempts: %d%n", ds.students().size(),
                ds.questions.size(), ds.exams.size(), ds.results.size()));
        sb.append(String.format("Average score: %.2f%% | Pass rate: %.2f%%%n%n", avgPercent(ds.results), passRate(ds.results)));
        sb.append("EXAM STATISTICS\n");
        sb.append(String.format("%-32s %8s %8s %8s %8s %8s%n", "Exam", "Attempts", "Avg%", "High%", "Low%", "Pass%"));
        for (Object[] r : examStats())
            sb.append(String.format("%-32s %8s %8s %8s %8s %8s%n", r[0], r[1], r[2], r[3], r[4], r[5]));
        sb.append("\nTOP 10 STUDENTS\n");
        sb.append(String.format("%-5s %-25s %8s %8s %8s %10s%n", "Rank", "Student", "Attempts", "Avg%", "Best%", "TotalScore"));
        for (Object[] r : leaderboard(10))
            sb.append(String.format("%-5s %-25s %8s %8s %8s %10s%n", r[0], r[1], r[2], r[3], r[4], r[5]));
        sb.append("\nSUBJECT ANALYSIS\n");
        sb.append(String.format("%-20s %8s %8s %8s %10s%n", "Subject", "Correct", "Wrong", "Skipped", "Accuracy%"));
        for (Map.Entry<String, int[]> en : subjectStats(ds.results).entrySet()) {
            int[] a = en.getValue();
            sb.append(String.format("%-20s %8d %8d %8d %10.2f%n", en.getKey(), a[0], a[1], a[2], accuracy(a)));
        }
        sb.append("\nALL RESULTS\n");
        sb.append(String.format("%-10s %-22s %-28s %8s %8s %-6s%n", "ID", "Student", "Exam", "Score", "Percent", "Result"));
        for (Result r : ds.results)
            sb.append(String.format("%-10s %-22s %-28s %8.2f %8.2f %-6s%n", r.getId(), r.getStudentName(), r.getExamTitle(),
                    r.getScore(), r.percent(), r.isPassed() ? "PASS" : "FAIL"));
        return sb.toString();
    }

    public static Path exportReport() throws IOException {
        Path dir = FileUtil.DATA_DIR.resolve("reports");
        Files.createDirectories(dir);
        Path p = dir.resolve("report_" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".txt");
        Files.write(p, buildReport().getBytes(StandardCharsets.UTF_8));
        return p;
    }
}
