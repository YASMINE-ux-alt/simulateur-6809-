package gui;

import javax.swing.*;
import java.awt.*;
import cpu.CPU6809;

public class CPUPanel6809 extends JPanel {

    private CPU6809 cpu;

    // Labels affichés
    public JLabel lblPC, lblS, lblU, lblA, lblB, lblDP, lblX, lblY;

    // Labels des flags
    private JLabel lblE, lblF, lblH, lblI, lblN, lblZ, lblV, lblC;

    public CPUPanel6809(CPU6809 cpu) {
        this.cpu = cpu;

        setLayout(null);
        setBackground(new Color(200, 200, 200));

        Font font = new Font("Consolas", Font.BOLD, 18);
        Color blue = new Color(0, 0, 255);

        // === REGISTRES ===
        lblPC = makeValueLabel(100, 20, font, blue);
        lblS  = makeValueLabel(70, 90, font, blue);
        lblU  = makeValueLabel(220, 90, font, blue);

        lblA  = makeValueLabel(70, 150, font, blue);
        lblB  = makeValueLabel(70, 200, font, blue);

        lblDP = makeValueLabel(70, 260, font, blue);

        lblX  = makeValueLabel(70, 330, font, blue);
        lblY  = makeValueLabel(220, 330, font, blue);

        // === LABELS TEXTUELS ===
        add(makeNameLabel("PC", 20, 20));
        add(makeNameLabel("S",  20, 90));
        add(makeNameLabel("U", 180, 90));
        add(makeNameLabel("A",  20, 150));
        add(makeNameLabel("B",  20, 200));
        add(makeNameLabel("DP", 20, 260));
        add(makeNameLabel("X",  20, 330));
        add(makeNameLabel("Y", 180, 330));

        add(makeNameLabel("E F H I N Z V C", 180, 240, new Font("Arial", Font.BOLD, 14)));

        // === FLAGS ===
        lblE = makeFlagLabel(180, 270);
        lblF = makeFlagLabel(200, 270);
        lblH = makeFlagLabel(220, 270);
        lblI = makeFlagLabel(240, 270);
        lblN = makeFlagLabel(260, 270);
        lblZ = makeFlagLabel(280, 270);
        lblV = makeFlagLabel(300, 270);
        lblC = makeFlagLabel(320, 270);

        add(lblE); add(lblF); add(lblH); add(lblI);
        add(lblN); add(lblZ); add(lblV); add(lblC);
    }

    // === Méthodes utilitaires ===
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
        lbl.setBounds(x, y, 200, 30);
        lbl.setFont(f);
        lbl.setForeground(Color.BLACK);
        return lbl;
    }

    private JLabel makeFlagLabel(int x, int y) {
        JLabel lbl = new JLabel("0", SwingConstants.CENTER);
        lbl.setBounds(x, y, 20, 20);
        lbl.setOpaque(true);
        lbl.setBackground(Color.WHITE);
        lbl.setFont(new Font("Consolas", Font.BOLD, 14));
        return lbl;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke(3));

        int[] x = {140, 200, 200, 140};
        int[] y = {140, 170, 210, 240};
        g2.drawPolygon(x, y, 4);
        g2.drawString("UAL", 155, 195);
    }

    // === MISE À JOUR ===
    public void refresh() {
        lblA.setText(String.format("%02X", cpu.getA()));
        lblB.setText(String.format("%02X", cpu.getB()));
        lblX.setText(String.format("%04X", cpu.getX()));
        lblY.setText(String.format("%04X", cpu.getY()));
        lblS.setText(String.format("%04X", cpu.getS()));
        lblU.setText(String.format("%04X", cpu.getU()));
        lblPC.setText(String.format("%04X", cpu.getPC()));
        lblDP.setText(String.format("%02X", cpu.getDP()));

        updateFlags(cpu);
    }

    public void updateFlags(CPU6809 cpu) {
        int ccr = cpu.getCCR();

        lblE.setText(((ccr & 0x80) != 0) ? "1" : "0");
        lblF.setText(((ccr & 0x40) != 0) ? "1" : "0");
        lblH.setText(((ccr & 0x20) != 0) ? "1" : "0");
        lblI.setText(((ccr & 0x10) != 0) ? "1" : "0");
        lblN.setText(((ccr & 0x08) != 0) ? "1" : "0");
        lblZ.setText(((ccr & 0x04) != 0) ? "1" : "0");
        lblV.setText(((ccr & 0x02) != 0) ? "1" : "0");
        lblC.setText(((ccr & 0x01) != 0) ? "1" : "0");
    }
}
