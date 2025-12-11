package gui;

import javax.swing.*;
import java.awt.*;
import cpu.CPU6809;

public class CPUPanel6809 extends JPanel {

    private CPU6809 cpu;

    // Labels affichés
    public JLabel lblPC, lblS, lblU, lblA, lblB, lblD, lblDP, lblX, lblY, lblCCR;

    public CPUPanel6809(CPU6809 cpu) {
        this.cpu = cpu;

        setLayout(null);
        setBackground(new Color(200, 200, 200));

        Font font = new Font("Consolas", Font.BOLD, 18);
        Color blue = new Color(0, 0, 255);

        // === Création des labels ===
        lblPC = makeValueLabel(100, 20, font, blue);
        lblS  = makeValueLabel(70, 90, font, blue);
        lblU  = makeValueLabel(220, 90, font, blue);

        lblA  = makeValueLabel(70, 150, font, blue);
        lblB  = makeValueLabel(70, 200, font, blue);

        lblDP = makeValueLabel(70, 260, font, blue);
        lblCCR = makeValueLabel(180, 260, new Font("Consolas", Font.BOLD, 16), blue);

        lblX  = makeValueLabel(70, 330, font, blue);
        lblY  = makeValueLabel(220, 330, font, blue);

        // === Labels textuels ===
        add(makeNameLabel("PC", 20, 20));
        add(makeNameLabel("S",  20, 90));
        add(makeNameLabel("U", 180, 90));
        add(makeNameLabel("A",  20, 150));
        add(makeNameLabel("B",  20, 200));
        add(makeNameLabel("DP", 20, 260));
        add(makeNameLabel("X",  20, 330));
        add(makeNameLabel("Y", 180, 330));
        add(makeNameLabel("EFHINZVC", 180, 240, new Font("Arial", Font.BOLD, 12)));

    }

    private JLabel makeValueLabel(int x, int y, Font f, Color c) {
        JLabel lbl = new JLabel("0000");
        lbl.setBounds(x, y, 120, 30);
        lbl.setOpaque(true);
        lbl.setBackground(Color.WHITE);
        lbl.setForeground(c);
        lbl.setFont(f);
        add(lbl);
        return lbl;
    }

    private JLabel makeNameLabel(String text, int x, int y) {
        return makeNameLabel(text, x, y, new Font("Arial", Font.BOLD, 18));
    }

    private JLabel makeNameLabel(String text, int x, int y, Font f) {
        JLabel lbl = new JLabel(text);
        lbl.setBounds(x, y, 150, 30);
        lbl.setFont(f);
        lbl.setForeground(Color.BLACK);
        return lbl;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        // --- Dessin de la UAL ---
        Graphics2D g2 = (Graphics2D) g;
        g2.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke(3));

        int[] x = {140, 200, 200, 140};
        int[] y = {140, 170, 210, 240};

        g2.drawPolygon(x, y, 4);
        g2.drawString("UAL", 155, 195);
    }

    // Mettre à jour les valeurs du CPU
    public void refresh() {
        lblPC.setText(String.format("%04X", cpu.getPC()));
        lblS.setText(String.format("%04X", cpu.getS()));
        lblU.setText(String.format("%04X", cpu.getU()));
        lblA.setText(String.format("%02X", cpu.getA()));
        lblB.setText(String.format("%02X", cpu.getB()));
        lblDP.setText(String.format("%02X", cpu.getDP()));
        lblCCR.setText(String.format("%8s", Integer.toBinaryString(cpu.getCCR())).replace(' ', '0'));
        lblX.setText(String.format("%04X", cpu.getX()));
        lblY.setText(String.format("%04X", cpu.getY()));
    }
}
