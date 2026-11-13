package examguard.ui;

import examguard.model.Result;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.format.DateTimeFormatter;
import javax.imageio.ImageIO;
import javax.swing.*;

/** Draws a certificate of achievement with Java2D and lets the student save it as PNG. */
public class CertificateDialog extends JDialog {
    private static final int W = 860, H = 600;

    public CertificateDialog(Window owner, Result r) {
        super(owner, "Certificate of Achievement", ModalityType.APPLICATION_MODAL);
        JPanel canvas = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                paintCertificate((Graphics2D) g, W, H, r);
            }
        };
        canvas.setPreferredSize(new Dimension(W, H));
        JPanel btns = Theme.toolbar(
                Theme.button("Save as PNG", Theme.PRIMARY, e -> save(r)),
                Theme.button("Close", Theme.SIDEBAR, e -> dispose()));
        add(canvas, BorderLayout.CENTER);
        add(btns, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(owner);
    }

    private void save(Result r) {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("Certificate_" + r.getId() + ".png"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            BufferedImage img = new BufferedImage(W * 2, H * 2, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.scale(2, 2);
            paintCertificate(g, W, H, r);
            g.dispose();
            ImageIO.write(img, "png", fc.getSelectedFile());
            Theme.info(this, "Saved to " + fc.getSelectedFile().getAbsolutePath());
        } catch (Exception ex) {
            Theme.error(this, "Could not save: " + ex.getMessage());
        }
    }

    private static void center(Graphics2D g, String s, int y) {
        g.drawString(s, (W - g.getFontMetrics().stringWidth(s)) / 2, y);
    }

    static void paintCertificate(Graphics2D g, int w, int h, Result r) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(new Color(0xFFFBEB)); g.fillRect(0, 0, w, h);
        Color gold = new Color(0xB45309);
        g.setColor(gold); g.setStroke(new BasicStroke(6)); g.drawRect(18, 18, w - 36, h - 36);
        g.setStroke(new BasicStroke(2)); g.drawRect(30, 30, w - 60, h - 60);
        g.setColor(new Color(0x312E81));
        g.setFont(new Font("Serif", Font.BOLD, 20)); center(g, "E X A M G U A R D", 90);
        g.setFont(new Font("Serif", Font.BOLD, 46)); center(g, "Certificate of Achievement", 155);
        g.setColor(gold); g.setStroke(new BasicStroke(2)); g.drawLine(250, 175, w - 250, 175);
        g.setColor(Color.DARK_GRAY);
        g.setFont(new Font("Serif", Font.ITALIC, 20)); center(g, "This is proudly presented to", 225);
        g.setColor(new Color(0x111827));
        g.setFont(new Font("Serif", Font.BOLD, 44)); center(g, r.getStudentName(), 290);
        g.setColor(Color.DARK_GRAY);
        g.setFont(new Font("Serif", Font.ITALIC, 20)); center(g, "for successfully passing the examination", 340);
        g.setColor(new Color(0x4F46E5));
        g.setFont(new Font("Serif", Font.BOLD, 30)); center(g, r.getExamTitle(), 390);
        g.setColor(Color.DARK_GRAY);
        g.setFont(new Font("Serif", Font.PLAIN, 20));
        center(g, "with a score of " + Theme.fmt(r.getScore()) + " / " + Theme.fmt(r.getTotalMarks())
                + "  (" + Theme.fmt(r.percent()) + "%)", 435);
        g.setFont(new Font("Serif", Font.PLAIN, 16));
        g.drawString("Date: " + r.getSubmittedAt().format(DateTimeFormatter.ofPattern("dd MMMM yyyy")), 80, h - 80);
        String id = "Certificate ID: EG-" + r.getId();
        g.drawString(id, w - 80 - g.getFontMetrics().stringWidth(id), h - 80);
        g.drawLine(w / 2 - 110, h - 90, w / 2 + 110, h - 90);
        g.setFont(new Font("Serif", Font.ITALIC, 15)); center(g, "Chief Examiner", h - 68);
    }
}
