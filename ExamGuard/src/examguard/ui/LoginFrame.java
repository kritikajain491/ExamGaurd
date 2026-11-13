package examguard.ui;

import examguard.model.Role;
import examguard.service.AuthService;
import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/** Login + student registration screen. */
public class LoginFrame extends JFrame {
    private final CardLayout cl = new CardLayout();
    private final JPanel right = new JPanel(cl);
    private JComboBox<String> role;
    private JTextField user;
    private JPasswordField pass;
    private JLabel msg;
    private Theme.RButton loginBtn, regBtn;
    private JTextField rName, rRoll, rEmail, rUser;
    private JPasswordField rPass, rConfirm;
    private JLabel rMsg;

    public LoginFrame() {
        setTitle("ExamGuard - Sign in");
        setSize(960, 640);
        setResizable(false);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new GridLayout(1, 2));
        add(brandPanel());
        right.setBackground(Theme.WHITE);
        right.add(loginCard(), "login");
        right.add(registerCard(), "register");
        add(right);
        getRootPane().setDefaultButton(loginBtn);
    }

    private JPanel brandPanel() {
        JPanel p = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setPaint(new GradientPaint(0, 0, new Color(0x312E81), getWidth(), getHeight(), new Color(0x7C3AED)));
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new EmptyBorder(80, 50, 40, 40));
        p.add(Theme.label("ExamGuard", 40, true, Color.WHITE));
        p.add(Box.createVerticalStrut(8));
        p.add(Theme.label("Secure online examination management", 16, false, new Color(0xE0E7FF)));
        p.add(Box.createVerticalStrut(50));
        String[] pts = {"Timed exams with auto-submit", "Randomised questions and options", "Instant marking with explanations",
                "Leaderboards, analytics and certificates", "Role-based access and login protection"};
        for (String s : pts) { p.add(Theme.label("\u2713  " + s, 15, false, Color.WHITE)); p.add(Box.createVerticalStrut(14)); }
        return p;
    }

    private void addRow(JPanel p, String label, JComponent c) {
        JLabel l = Theme.label(label, 13, true, Theme.TEXT);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        p.add(l); p.add(Box.createVerticalStrut(4)); p.add(c); p.add(Box.createVerticalStrut(12));
    }

    private JPanel column() {
        JPanel p = new JPanel();
        p.setBackground(Theme.WHITE);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new EmptyBorder(40, 50, 30, 50));
        return p;
    }

    private JPanel loginCard() {
        JPanel p = column();
        JLabel h = Theme.label("Welcome back", 28, true, Theme.TEXT);
        h.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(h);
        JLabel s = Theme.label("Sign in to continue", 14, false, Theme.MUTED);
        s.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(s);
        p.add(Box.createVerticalStrut(24));
        role = Theme.combo("Student", "Administrator");
        user = Theme.field(18);
        pass = Theme.pass(18);
        addRow(p, "Sign in as", role);
        addRow(p, "Username", user);
        addRow(p, "Password", pass);
        JCheckBox show = new JCheckBox("Show password");
        show.setBackground(Theme.WHITE);
        show.setAlignmentX(Component.LEFT_ALIGNMENT);
        char echo = pass.getEchoChar();
        show.addActionListener(e -> pass.setEchoChar(show.isSelected() ? (char) 0 : echo));
        p.add(show);
        p.add(Box.createVerticalStrut(8));
        msg = Theme.label(" ", 13, false, Theme.DANGER);
        msg.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(msg);
        p.add(Box.createVerticalStrut(8));
        loginBtn = Theme.button("Sign In", Theme.PRIMARY, e -> doLogin());
        loginBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        loginBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        p.add(loginBtn);
        p.add(Box.createVerticalStrut(12));
        Theme.RButton toReg = Theme.button("New student? Create an account", Theme.SIDEBAR, e -> {
            cl.show(right, "register"); getRootPane().setDefaultButton(regBtn);
        });
        toReg.setAlignmentX(Component.LEFT_ALIGNMENT);
        toReg.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        p.add(toReg);
        return p;
    }

    private JPanel registerCard() {
        JPanel p = column();
        p.setBorder(new EmptyBorder(24, 50, 20, 50));
        JLabel h = Theme.label("Create student account", 24, true, Theme.TEXT);
        h.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(h);
        p.add(Box.createVerticalStrut(14));
        rName = Theme.field(18); rRoll = Theme.field(18); rEmail = Theme.field(18); rUser = Theme.field(18);
        rPass = Theme.pass(18); rConfirm = Theme.pass(18);
        addRow(p, "Full name", rName); addRow(p, "Roll number", rRoll); addRow(p, "Email", rEmail);
        addRow(p, "Username", rUser); addRow(p, "Password (6+ chars, letter + digit)", rPass); addRow(p, "Confirm password", rConfirm);
        rMsg = Theme.label(" ", 13, false, Theme.DANGER);
        rMsg.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(rMsg);
        regBtn = Theme.button("Register", Theme.SUCCESS, e -> doRegister());
        Theme.RButton back = Theme.button("Back to sign in", Theme.SIDEBAR, e -> {
            cl.show(right, "login"); getRootPane().setDefaultButton(loginBtn);
        });
        JPanel row = new JPanel(new GridLayout(1, 2, 10, 0));
        row.setBackground(Theme.WHITE);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        row.add(regBtn); row.add(back);
        p.add(row);
        return p;
    }

    private void doLogin() {
        try {
            Role r = role.getSelectedIndex() == 0 ? Role.STUDENT : Role.ADMIN;
            AuthService.login(user.getText(), new String(pass.getPassword()), r);
            dispose();
            if (r == Role.ADMIN) new AdminDashboard().setVisible(true);
            else new StudentDashboard().setVisible(true);
        } catch (AuthService.AuthException ex) {
            msg.setText(ex.getMessage());
            pass.setText("");
        }
    }

    private void doRegister() {
        try {
            AuthService.register(rName.getText(), rRoll.getText(), rEmail.getText(), rUser.getText(),
                    new String(rPass.getPassword()), new String(rConfirm.getPassword()));
            Theme.info(this, "Account created! You can sign in now.");
            user.setText(rUser.getText().trim());
            role.setSelectedIndex(0);
            for (JTextField f : new JTextField[]{rName, rRoll, rEmail, rUser, rPass, rConfirm}) f.setText("");
            rMsg.setText(" ");
            cl.show(right, "login");
            getRootPane().setDefaultButton(loginBtn);
        } catch (AuthService.AuthException ex) {
            rMsg.setText("<html>" + ex.getMessage() + "</html>");
        }
    }
}
