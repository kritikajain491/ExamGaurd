package examguard.service;

import examguard.model.*;
import examguard.util.FileUtil;
import java.util.*;

/** Central in-memory store backed by text files in ./data. */
public class DataStore {
    private static final DataStore INSTANCE = new DataStore();
    public static DataStore get() { return INSTANCE; }

    public final List<User> users = new ArrayList<>();
    public final List<Question> questions = new ArrayList<>();
    public final List<Exam> exams = new ArrayList<>();
    public final List<Result> results = new ArrayList<>();
    public final List<LogEntry> logs = new ArrayList<>();

    private DataStore() {}

    public void load() {
        users.clear(); questions.clear(); exams.clear(); results.clear(); logs.clear();
        for (String[] f : FileUtil.read("users.txt")) try { users.add(User.fromRecord(f)); } catch (Exception ignored) {}
        for (String[] f : FileUtil.read("questions.txt")) try { questions.add(Question.fromRecord(f)); } catch (Exception ignored) {}
        for (String[] f : FileUtil.read("exams.txt")) try { exams.add(Exam.fromRecord(f)); } catch (Exception ignored) {}
        for (String[] f : FileUtil.read("results.txt")) try { results.add(Result.fromRecord(f)); } catch (Exception ignored) {}
        for (String[] f : FileUtil.read("logs.txt")) try { logs.add(LogEntry.fromRecord(f)); } catch (Exception ignored) {}
        if (users.isEmpty()) Seeder.seed(this);
    }

    public void saveUsers() { FileUtil.write("users.txt", rows(users)); }
    public void saveQuestions() { FileUtil.write("questions.txt", rows(questions)); }
    public void saveExams() { FileUtil.write("exams.txt", rows(exams)); }
    public void saveResults() { FileUtil.write("results.txt", rows(results)); }
    public void clearLogs() { logs.clear(); FileUtil.write("logs.txt", new ArrayList<String[]>()); }

    private List<String[]> rows(List<?> list) {
        List<String[]> out = new ArrayList<>();
        for (Object o : list) {
            if (o instanceof User) out.add(((User) o).toRecord());
            else if (o instanceof Question) out.add(((Question) o).toRecord());
            else if (o instanceof Exam) out.add(((Exam) o).toRecord());
            else if (o instanceof Result) out.add(((Result) o).toRecord());
        }
        return out;
    }

    public void log(String user, String action) {
        LogEntry e = new LogEntry(user, action);
        logs.add(e);
        FileUtil.append("logs.txt", e.toRecord());
    }

    public static String newId(String prefix) { return prefix + UUID.randomUUID().toString().substring(0, 6).toUpperCase(); }

    public User findUser(String username) {
        if (username == null) return null;
        for (User u : users) if (u.getUsername().equalsIgnoreCase(username.trim())) return u;
        return null;
    }
    public User userById(String id) { for (User u : users) if (u.getId().equals(id)) return u; return null; }
    public Question question(String id) { for (Question q : questions) if (q.getId().equals(id)) return q; return null; }
    public Exam exam(String id) { for (Exam e : exams) if (e.getId().equals(id)) return e; return null; }
    public Result result(String id) { for (Result r : results) if (r.getId().equals(id)) return r; return null; }

    public List<Student> students() {
        List<Student> l = new ArrayList<>();
        for (User u : users) if (u instanceof Student) l.add((Student) u);
        return l;
    }

    public TreeSet<String> subjects() {
        TreeSet<String> s = new TreeSet<>();
        for (Question q : questions) s.add(q.getSubject());
        return s;
    }
}
