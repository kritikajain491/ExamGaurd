package examguard.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Pipe-delimited text file persistence (all data lives in the ./data folder). */
public final class FileUtil {
    public static final Path DATA_DIR = Paths.get("data");
    public static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    public static final DateTimeFormatter DT_SEC = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private FileUtil() {}

    private static String esc(String s) {
        if (s == null) return "";
        StringBuilder b = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '\\': b.append("\\\\"); break;
                case '|': b.append("\\p"); break;
                case '\n': b.append("\\n"); break;
                case '\r': break;
                default: b.append(c);
            }
        }
        return b.toString();
    }

    private static String unesc(String s) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char n = s.charAt(++i);
                b.append(n == 'p' ? '|' : n == 'n' ? '\n' : n);
            } else b.append(c);
        }
        return b.toString();
    }

    private static String line(String[] f) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < f.length; i++) {
            if (i > 0) b.append('|');
            b.append(esc(f[i]));
        }
        return b.toString();
    }

    public static List<String[]> read(String file) {
        List<String[]> rows = new ArrayList<>();
        Path p = DATA_DIR.resolve(file);
        if (!Files.exists(p)) return rows;
        try (BufferedReader r = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
            String ln;
            while ((ln = r.readLine()) != null) {
                if (ln.trim().isEmpty()) continue;
                String[] parts = ln.split("\\|", -1);
                for (int i = 0; i < parts.length; i++) parts[i] = unesc(parts[i]);
                rows.add(parts);
            }
        } catch (IOException e) {
            System.err.println("Could not read " + file + ": " + e.getMessage());
        }
        return rows;
    }

    /** Writes to a temp file first, then replaces the real file (protects against corruption). */
    public static synchronized void write(String file, List<String[]> rows) {
        try {
            Files.createDirectories(DATA_DIR);
            Path tmp = DATA_DIR.resolve(file + ".tmp");
            try (BufferedWriter w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                for (String[] r : rows) { w.write(line(r)); w.newLine(); }
            }
            Files.move(tmp, DATA_DIR.resolve(file), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static synchronized void append(String file, String[] row) {
        try {
            Files.createDirectories(DATA_DIR);
            Files.write(DATA_DIR.resolve(file), (line(row) + System.lineSeparator()).getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
