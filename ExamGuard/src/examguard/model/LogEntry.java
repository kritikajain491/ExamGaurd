package examguard.model;

import examguard.util.FileUtil;
import java.time.LocalDateTime;

public class LogEntry {
    private final String time, user, action;

    public LogEntry(String user, String action) {
        this(LocalDateTime.now().format(FileUtil.DT_SEC), user, action);
    }
    public LogEntry(String time, String user, String action) { this.time = time; this.user = user; this.action = action; }

    public String getTime() { return time; }
    public String getUser() { return user; }
    public String getAction() { return action; }
    public String[] toRecord() { return new String[]{time, user, action}; }
    public static LogEntry fromRecord(String[] f) { return new LogEntry(f[0], f[1], f[2]); }
}
