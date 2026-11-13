package examguard.service;

import examguard.model.*;
import java.time.LocalDateTime;
import java.util.*;

/** Creates sample data on first run. */
final class Seeder {
    private Seeder() {}

    private static Question q(DataStore ds, String subj, String text, String a, String b, String c, String d, int ans, String expl, String diff) {
        Question qu = new Question(DataStore.newId("Q"), subj, text, new String[]{a, b, c, d}, ans, expl, diff);
        ds.questions.add(qu);
        return qu;
    }

    static void seed(DataStore ds) {
        ds.users.add(new Admin("A001", "admin", AuthService.hash("admin", "Admin@123"), "System Administrator", "admin@examguard.local", "Chief Examiner"));
        String[][] st = {{"student", "Student@123", "Demo Student", "CS001"}, {"alice", "Alice@123", "Alice Johnson", "CS002"}, {"bob", "Bob@1234", "Bob Smith", "CS003"}};
        for (String[] s : st)
            ds.users.add(new Student(DataStore.newId("S"), s[0], AuthService.hash(s[0], s[1]), s[2], s[0] + "@examguard.local", s[3]));

        List<String> java = new ArrayList<>(), math = new ArrayList<>(), gk = new ArrayList<>();
        java.add(q(ds, "Java", "Which keyword is used to inherit a class in Java?", "extends", "implements", "inherits", "super", 0, "'extends' creates a subclass; 'implements' is for interfaces.", "Easy").getId());
        java.add(q(ds, "Java", "Which collection does not allow duplicate elements?", "ArrayList", "HashSet", "LinkedList", "Vector", 1, "A Set stores unique elements only.", "Easy").getId());
        java.add(q(ds, "Java", "What is the default value of an int instance variable?", "0", "null", "undefined", "-1", 0, "Numeric instance fields default to 0.", "Easy").getId());
        java.add(q(ds, "Java", "Which of these is NOT a primitive type?", "int", "boolean", "String", "char", 2, "String is a class (reference type).", "Medium").getId());
        java.add(q(ds, "Java", "Which exception is thrown by integer division by zero?", "ArithmeticException", "NullPointerException", "IOException", "NumberFormatException", 0, "Integer division by zero throws ArithmeticException.", "Medium").getId());
        math.add(q(ds, "Mathematics", "What is 15% of 200?", "20", "25", "30", "35", 2, "200 x 0.15 = 30.", "Easy").getId());
        math.add(q(ds, "Mathematics", "Value of 7 x 8 + 6?", "56", "62", "64", "58", 1, "56 + 6 = 62 (multiplication first).", "Easy").getId());
        math.add(q(ds, "Mathematics", "Square root of 144?", "11", "12", "13", "14", 1, "12 x 12 = 144.", "Easy").getId());
        math.add(q(ds, "Mathematics", "Next number in 2, 6, 12, 20, 30, ?", "40", "42", "44", "36", 1, "Differences grow 4,6,8,10,12 so 30+12 = 42.", "Medium").getId());
        math.add(q(ds, "Mathematics", "Sum of the angles of a triangle?", "90", "180", "270", "360", 1, "Interior angles of a triangle add up to 180 degrees.", "Easy").getId());
        gk.add(q(ds, "General Knowledge", "Capital of Australia?", "Sydney", "Melbourne", "Canberra", "Perth", 2, "Canberra is the capital, not Sydney.", "Medium").getId());
        gk.add(q(ds, "General Knowledge", "Chemical symbol for gold?", "Ag", "Au", "Gd", "Go", 1, "Au comes from Latin 'aurum'.", "Easy").getId());
        gk.add(q(ds, "General Knowledge", "Largest planet in the Solar System?", "Earth", "Saturn", "Jupiter", "Neptune", 2, "Jupiter is the largest planet.", "Easy").getId());
        gk.add(q(ds, "General Knowledge", "Who wrote 'Romeo and Juliet'?", "Charles Dickens", "William Shakespeare", "Mark Twain", "Jane Austen", 1, "Written by William Shakespeare.", "Easy").getId());
        gk.add(q(ds, "General Knowledge", "Which is the largest ocean?", "Atlantic", "Indian", "Arctic", "Pacific", 3, "The Pacific covers about a third of the Earth's surface.", "Easy").getId());

        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
        Exam e1 = new Exam("E001", "Java Fundamentals", "Java", 10, 2, 0.5, 40, now.minusDays(1), now.plusDays(365), true, 2, java);
        List<String> mix = new ArrayList<>(math); mix.addAll(gk);
        Exam e2 = new Exam("E002", "Aptitude & GK Mix", "Mixed", 15, 1, 0.25, 50, now.minusDays(1), now.plusDays(365), true, 1, mix);
        Exam e3 = new Exam("E003", "Mathematics Mock (Scheduled)", "Mathematics", 8, 2, 0.5, 40, now.plusDays(2), now.plusDays(30), true, 1, math);
        ds.exams.add(e1); ds.exams.add(e2); ds.exams.add(e3);
        ds.saveUsers(); ds.saveQuestions(); ds.saveExams();

        seedResult(ds, e1, (Student) ds.findUser("alice"), 1, 3);
        seedResult(ds, e1, (Student) ds.findUser("bob"), 0, 2);
        seedResult(ds, e2, (Student) ds.findUser("bob"), 2, 1);
        ds.saveResults();
        ds.log("system", "Sample data created");
    }

    /** mode 0: perfect except one skipped, 1: one wrong, 2: every third wrong. */
    private static void seedResult(DataStore ds, Exam e, Student s, int mode, int daysAgo) {
        Map<String, Integer> ans = new LinkedHashMap<>();
        int i = 0;
        for (Question q : ExamService.questionsOf(e)) {
            int sel = q.getCorrect();
            if (mode == 0 && i == 3) sel = -1;
            if (mode == 1 && i == 1) sel = (q.getCorrect() + 1) % 4;
            if (mode == 2 && i % 3 == 0) sel = (q.getCorrect() + 2) % 4;
            ans.put(q.getId(), sel);
            i++;
        }
        Result r = ExamService.grade(e, s, ans, 240 + daysAgo * 37);
        r.setSubmittedAt(LocalDateTime.now().minusDays(daysAgo));
        ds.results.add(r);
    }
}
