package examguard.ui;

import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.JPanel;

/** Minimal bar chart (values are percentages 0-100) drawn with Java2D. */
public class BarChart extends JPanel {
    private Map<String, Double> data = new LinkedHashMap<>();
    private final String title;

    public BarChart(String title) {
        this.title = title;
        setBackground(Theme.WHITE);
        setBorder(javax.swing.BorderFactory.createLineBorder(Theme.BORDER));
        setPreferredSize(new Dimension(380, 240));
    }

    public void setData(Map<String, Double> d) { data = d; repaint(); }

    @Override protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setFont(Theme.BASE.deriveFont(Font.BOLD, 13f));
        g.setColor(Theme.TEXT);
        g.drawString(title, 14, 22);
        int w = getWidth(), h = getHeight(), left = 40, bottom = h - 34, top = 40;
        g.setFont(Theme.BASE.deriveFont(11f));
        for (int p = 0; p <= 100; p += 25) {
            int y = bottom - (bottom - top) * p / 100;
            g.setColor(new Color(0xE5E7EB)); g.drawLine(left, y, w - 12, y);
            g.setColor(Theme.MUTED); g.drawString(p + "%", 6, y + 4);
        }
        if (data.isEmpty()) { g.drawString("No data yet", left + 20, (top + bottom) / 2); return; }
        int n = data.size(), slot = (w - left - 12) / n, bw = Math.max(18, Math.min(60, slot - 24)), i = 0;
        Color[] pal = {Theme.PRIMARY, Theme.SUCCESS, Theme.WARN, Theme.INFO, Theme.DANGER};
        for (Map.Entry<String, Double> e : data.entrySet()) {
            int bh = (int) ((bottom - top) * Math.min(100, e.getValue()) / 100.0);
            int x = left + i * slot + (slot - bw) / 2;
            g.setColor(pal[i % pal.length]);
            g.fillRoundRect(x, bottom - bh, bw, bh, 6, 6);
            g.setColor(Theme.TEXT);
            String v = String.format("%.0f%%", e.getValue());
            g.drawString(v, x + (bw - g.getFontMetrics().stringWidth(v)) / 2, bottom - bh - 4);
            String name = e.getKey().length() > 12 ? e.getKey().substring(0, 11) + "…" : e.getKey();
            g.setColor(Theme.MUTED);
            g.drawString(name, x + (bw - g.getFontMetrics().stringWidth(name)) / 2, bottom + 16);
            i++;
        }
    }
}
