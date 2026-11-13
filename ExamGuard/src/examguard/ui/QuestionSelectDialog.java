package examguard.ui;

import examguard.model.Question;
import examguard.service.DataStore;
import java.awt.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

/** Modal picker with checkboxes, search and subject filter. getSelection() is null if cancelled. */
public class QuestionSelectDialog extends JDialog {
    private Set<String> selection;
    private final DefaultTableModel model;
    private final JTable table;

    public QuestionSelectDialog(Window owner, Set<String> preselected) {
        super(owner, "Select Questions", ModalityType.APPLICATION_MODAL);
        setSize(860, 560);
        setLocationRelativeTo(owner);
        model = new DefaultTableModel(new Object[]{"Select", "ID", "Subject", "Question", "Difficulty"}, 0) {
            @Override public Class<?> getColumnClass(int c) { return c == 0 ? Boolean.class : String.class; }
            @Override public boolean isCellEditable(int r, int c) { return c == 0; }
        };
        for (Question q : DataStore.get().questions)
            model.addRow(new Object[]{preselected.contains(q.getId()), q.getId(), q.getSubject(), q.getText(), q.getDifficulty()});
        table = Theme.table(model);
        table.getColumnModel().getColumn(0).setMaxWidth(70);
        table.getColumnModel().getColumn(3).setPreferredWidth(420);

        JTextField search = Theme.field(16);
        JComboBox<String> subj = Theme.combo("All Subjects");
        for (String s : DataStore.get().subjects()) subj.addItem(s);
        Theme.attachFilter(table, search, new Theme.ColFilter(subj, 2));
        JLabel count = Theme.label("", 13, true, Theme.PRIMARY);
        model.addTableModelListener(e -> count.setText(countSelected() + " selected"));
        count.setText(countSelected() + " selected");

        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBorder(new EmptyBorder(14, 16, 14, 16));
        root.setBackground(Theme.BG);
        root.add(Theme.toolbar(new JLabel("Search:"), search, subj,
                Theme.button("Select visible", Theme.INFO, e -> setVisibleRows(true)),
                Theme.button("Clear visible", Theme.WARN, e -> setVisibleRows(false)), count), BorderLayout.NORTH);
        root.add(Theme.scroll(table), BorderLayout.CENTER);
        JPanel btns = Theme.toolbar(Theme.button("OK", Theme.SUCCESS, e -> {
            selection = new LinkedHashSet<>();
            for (int i = 0; i < model.getRowCount(); i++) if ((Boolean) model.getValueAt(i, 0)) selection.add((String) model.getValueAt(i, 1));
            dispose();
        }), Theme.button("Cancel", Theme.SIDEBAR, e -> dispose()));
        ((FlowLayout) btns.getLayout()).setAlignment(FlowLayout.RIGHT);
        root.add(btns, BorderLayout.SOUTH);
        setContentPane(root);
    }

    private int countSelected() {
        int n = 0;
        for (int i = 0; i < model.getRowCount(); i++) if ((Boolean) model.getValueAt(i, 0)) n++;
        return n;
    }

    private void setVisibleRows(boolean v) {
        for (int i = 0; i < table.getRowCount(); i++) model.setValueAt(v, table.convertRowIndexToModel(i), 0);
    }

    public Set<String> getSelection() { return selection; }
}
