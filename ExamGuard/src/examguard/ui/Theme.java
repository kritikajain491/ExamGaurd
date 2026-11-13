package examguard.ui;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.FontUIResource;
import javax.swing.table.*;

/** Shared colours, fonts and component factories for a consistent modern look. */
public final class Theme {
    public static final Color PRIMARY = new Color(0x4F46E5), SIDEBAR = new Color(0x111827), BG = new Color(0xF3F4F6),
            WHITE = Color.WHITE, SUCCESS = new Color(0x10B981), DANGER = new Color(0xEF4444), WARN = new Color(0xF59E0B),
            INFO = new Color(0x0EA5E9), TEXT = new Color(0x111827), MUTED = new Color(0x6B7280), BORDER = new Color(0xE5E7EB);
    public static final Font BASE = new Font("SansSerif", Font.PLAIN, 14);

    private Theme() {}

    public static void install() {
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); } catch (Exception ignored) {}
        for (Object k : Collections.list(UIManager.getDefaults().keys()))
            if (k.toString().endsWith(".font") && UIManager.get(k) instanceof Font)
                UIManager.put(k, new FontUIResource(BASE));
        UIManager.put("OptionPane.background", WHITE);
        UIManager.put("Panel.background", WHITE);
    }

    // ---------- buttons ----------
    public static class RButton extends JButton {
        private Color base;
        private boolean hover;
        public RButton(String text, Color base) {
            super(text);
            this.base = base;
            setContentAreaFilled(false); setFocusPainted(false); setBorderPainted(false);
            setForeground(Color.WHITE); setFont(BASE.deriveFont(Font.BOLD, 13f));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(9, 16, 9, 16));
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            });
        }
        public void setBase(Color c) { base = c; repaint(); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color c = !isEnabled() ? new Color(0x9CA3AF) : getModel().isPressed() ? base.darker() : hover ? base.brighter() : base;
            g2.setColor(c);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    public static RButton button(String text, Color color, ActionListener a) {
        RButton b = new RButton(text, color);
        if (a != null) b.addActionListener(a);
        return b;
    }

    // ---------- basic components ----------
    public static JLabel label(String text, int size, boolean bold, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(BASE.deriveFont(bold ? Font.BOLD : Font.PLAIN, (float) size));
        l.setForeground(color);
        return l;
    }

    public static void styleField(JComponent f) {
        f.setFont(BASE);
        f.setBorder(BorderFactory.createCompoundBorder(new LineBorder(BORDER, 1, true), new EmptyBorder(6, 8, 6, 8)));
    }
    public static JTextField field(int cols) { JTextField f = new JTextField(cols); styleField(f); return f; }
    public static JPasswordField pass(int cols) { JPasswordField f = new JPasswordField(cols); styleField(f); return f; }
    public static JTextArea area(int rows, int cols) {
        JTextArea a = new JTextArea(rows, cols);
        a.setLineWrap(true); a.setWrapStyleWord(true); styleField(a);
        return a;
    }
    public static JComboBox<String> combo(String... items) {
        JComboBox<String> c = new JComboBox<>(items);
        c.setFont(BASE); c.setBackground(WHITE);
        return c;
    }
    /** Rebuilds a combo with a leading "all" entry while keeping the current selection if possible. */
    public static void resetCombo(JComboBox<String> box, String first, Collection<String> items) {
        Object sel = box.getSelectedItem();
        box.removeAllItems();
        box.addItem(first);
        for (String s : items) box.addItem(s);
        box.setSelectedItem(sel);
        if (box.getSelectedIndex() < 0) box.setSelectedIndex(0);
    }

    public static void onChange(JTextField f, Runnable r) {
        f.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { r.run(); }
            public void removeUpdate(DocumentEvent e) { r.run(); }
            public void changedUpdate(DocumentEvent e) { r.run(); }
        });
    }

    // ---------- cards & layout ----------
    public static class StatCard extends JPanel {
        private final JLabel value;
        public StatCard(String title, String v, Color accent) {
            setLayout(new BorderLayout(0, 4));
            setBackground(WHITE);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createCompoundBorder(new LineBorder(BORDER), new MatteBorder(0, 5, 0, 0, accent)),
                    new EmptyBorder(14, 16, 14, 16)));
            value = label(v, 26, true, TEXT);
            add(label(title, 13, false, MUTED), BorderLayout.NORTH);
            add(value, BorderLayout.CENTER);
        }
        public void setValue(String v) { value.setText(v); }
    }

    public static JPanel titled(String title, JComponent body) {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setOpaque(false);
        p.add(label(title, 15, true, TEXT), BorderLayout.NORTH);
        p.add(body, BorderLayout.CENTER);
        return p;
    }

    public static JPanel toolbar(Component... comps) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        p.setOpaque(false);
        for (Component c : comps) p.add(c);
        return p;
    }

    /** Standard page: title, subtitle and optional toolbar on top, content added to CENTER by the caller. */
    public static JPanel page(String title, String sub, JComponent toolbar) {
        JPanel p = new JPanel(new BorderLayout(0, 12));
        p.setBackground(BG);
        p.setBorder(new EmptyBorder(20, 24, 20, 24));
        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        JLabel t = label(title, 24, true, TEXT), s = label(sub, 13, false, MUTED);
        t.setAlignmentX(Component.LEFT_ALIGNMENT); s.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.add(t); top.add(s);
        if (toolbar != null) {
            top.add(Box.createVerticalStrut(12));
            toolbar.setAlignmentX(Component.LEFT_ALIGNMENT);
            top.add(toolbar);
        }
        p.add(top, BorderLayout.NORTH);
        return p;
    }

    public static JPanel form(String[] labels, JComponent[] comps) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(WHITE);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 5, 5, 5);
        g.anchor = GridBagConstraints.NORTHWEST;
        for (int i = 0; i < labels.length; i++) {
            g.gridx = 0; g.gridy = i; g.weightx = 0; g.fill = GridBagConstraints.NONE;
            p.add(label(labels[i], 13, true, TEXT), g);
            g.gridx = 1; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL;
            p.add(comps[i], g);
        }
        return p;
    }

    // ---------- tables ----------
    public static DefaultTableModel model(String... cols) {
        return new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int c) {   // lets numeric columns sort numerically
                Object v = getRowCount() > 0 ? getValueAt(0, c) : null;
                return v == null ? Object.class : v.getClass();
            }
        };
    }

    public static JTable table(TableModel m) {
        JTable t = new JTable(m) {
            @Override public Component prepareRenderer(TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                if (!isRowSelected(row)) c.setBackground(row % 2 == 0 ? WHITE : new Color(0xF9FAFB));
                return c;
            }
        };
        t.setRowHeight(30);
        t.setShowGrid(false);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setSelectionBackground(new Color(0xE0E7FF));
        t.setSelectionForeground(TEXT);
        t.setFillsViewportHeight(true);
        t.setAutoCreateRowSorter(true);
        t.setFont(BASE);
        JTableHeader h = t.getTableHeader();
        h.setReorderingAllowed(false);
        h.setPreferredSize(new Dimension(100, 34));
        h.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable tb, Object v, boolean s, boolean f, int r, int c) {
                super.getTableCellRendererComponent(tb, v, false, false, r, c);
                setBackground(new Color(0x1F2937)); setForeground(Color.WHITE);
                setFont(BASE.deriveFont(Font.BOLD, 13f));
                setBorder(new EmptyBorder(6, 8, 6, 8));
                setOpaque(true);
                return this;
            }
        });
        return t;
    }

    public static JScrollPane scroll(JComponent c) {
        JScrollPane s = new JScrollPane(c);
        s.setBorder(new LineBorder(BORDER));
        s.getViewport().setBackground(WHITE);
        return s;
    }

    public static class ColFilter {
        final JComboBox<String> box; final int col;
        public ColFilter(JComboBox<String> box, int col) { this.box = box; this.col = col; }
    }

    /** Live search box + exact-match combo filters on a sortable table. */
    @SuppressWarnings("unchecked")
    public static void attachFilter(JTable t, JTextField search, ColFilter... fs) {
        TableRowSorter<TableModel> sorter = (TableRowSorter<TableModel>) t.getRowSorter();
        Runnable apply = () -> {
            List<RowFilter<TableModel, Object>> list = new ArrayList<>();
            String q = search.getText().trim();
            if (!q.isEmpty()) list.add(RowFilter.regexFilter("(?i)" + Pattern.quote(q)));
            for (ColFilter f : fs)
                if (f.box.getSelectedIndex() > 0)
                    list.add(RowFilter.regexFilter("^" + Pattern.quote((String) f.box.getSelectedItem()) + "$", f.col));
            sorter.setRowFilter(list.isEmpty() ? null : RowFilter.andFilter(list));
        };
        onChange(search, apply);
        for (ColFilter f : fs) f.box.addActionListener(e -> apply.run());
    }

    /** Selected row converted to model index, or -1 (with a warning) if nothing is selected. */
    public static int selected(Component parent, JTable t) {
        int r = t.getSelectedRow();
        if (r < 0) { warn(parent, "Please select a row first."); return -1; }
        return t.convertRowIndexToModel(r);
    }
    public static String idOf(JTable t, int modelRow) { return t.getModel().getValueAt(modelRow, 0).toString(); }

    // ---------- dialogs ----------
    public static void info(Component p, String m) { JOptionPane.showMessageDialog(p, m, "ExamGuard", JOptionPane.INFORMATION_MESSAGE); }
    public static void warn(Component p, String m) { JOptionPane.showMessageDialog(p, m, "ExamGuard", JOptionPane.WARNING_MESSAGE); }
    public static void error(Component p, String m) { JOptionPane.showMessageDialog(p, m, "Error", JOptionPane.ERROR_MESSAGE); }
    public static boolean confirm(Component p, String m) {
        return JOptionPane.showConfirmDialog(p, m, "Please confirm", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
    }

    public static String fmt(double v) { return String.format("%.2f", v); }
}
