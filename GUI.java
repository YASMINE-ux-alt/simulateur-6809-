package gui;

import cpu.CPU6809;
import debugger.Debugger;
import memory.Memory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;

public class GUI extends JFrame {

    private final CPU6809 cpu;
    private final Memory memory;
    private final Debugger debugger;

    private JTextArea editorArea = new JTextArea();
    private JTextArea consoleArea = new JTextArea();

    private JButton btnRun, btnStep, btnStop;

    private boolean running = false;

    private RegisterPanel registerPanel;

    public GUI(CPU6809 cpu, Memory memory, Debugger debugger) {
        super("Simulateur 6809 - Version Étudiant");

        this.cpu = cpu;
        this.memory = memory;
        this.debugger = debugger;

        initUI();
    }

    // ============================================
    //  INTERFACE
    // ============================================

    private void initUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        setJMenuBar(buildMenuBar());
        getContentPane().setLayout(null);

        // Toolbar
        JToolBar tb = buildToolBar();
        tb.setBounds(0, 0, 1086, 23);
        getContentPane().add(tb);

        // Editor
        JScrollPane editor = (JScrollPane) buildCenterPanel();
        editor.setBounds(0, 22, 880, 440);
        getContentPane().add(editor);

        // Console
        JScrollPane console = (JScrollPane) buildConsolePanel();
        console.setBounds(0, 463, 880, 128);
        getContentPane().add(console);

        // Register panel (à droite)
        registerPanel = new RegisterPanel(cpu);
        registerPanel.setBounds(880, 23, 200, 568);
        getContentPane().add(registerPanel);

        setSize(1100, 650);
        setLocationRelativeTo(null);
    }


    // ============================================
    //  BARRE DE MENU
    // ============================================

    private JMenuBar buildMenuBar() {
        JMenuBar bar = new JMenuBar();

        return bar;
    }

    // ============================================
    //  TOOLBAR
    // ============================================

    private JToolBar buildToolBar() {
        JToolBar tb = new JToolBar();
        tb.setBounds(0, 0, 1086, 23);
        tb.setFloatable(false);
        
                JMenu fileMenu = new JMenu("Fichier");
                tb.add(fileMenu);
                
                        fileMenu.add(new JMenuItem(new AbstractAction("Nouveau") {
                            public void actionPerformed(ActionEvent e) {
                                newFile();
                            }
                        }));
                        
                                fileMenu.add(new JMenuItem(new AbstractAction("Ouvrir…") {
                                    public void actionPerformed(ActionEvent e) {
                                        openFile();
                                    }
                                }));
                                
                                        fileMenu.add(new JMenuItem(new AbstractAction("Enregistrer") {
                                            public void actionPerformed(ActionEvent e) {
                                                saveFile(false);
                                            }
                                        }));
                                        
                                                fileMenu.add(new JMenuItem(new AbstractAction("Enregistrer sous…") {
                                                    public void actionPerformed(ActionEvent e) {
                                                        saveFile(true);
                                                    }
                                                }));

        tb.add(new JButton(new AbstractAction("New") {
            public void actionPerformed(ActionEvent e) { newFile(); }
        }));

        tb.add(new JButton(new AbstractAction("Save") {
            public void actionPerformed(ActionEvent e) { saveFile(false); }
        }));

        tb.addSeparator();

        tb.add(new JButton(new AbstractAction("RAM") {
            public void actionPerformed(ActionEvent e) { showMemoryWindow(false); }
        }));

        tb.add(new JButton(new AbstractAction("ROM") {
            public void actionPerformed(ActionEvent e) { showMemoryWindow(true); }
        }));

        tb.addSeparator();

        btnRun = new JButton(new AbstractAction("Run") {
            public void actionPerformed(ActionEvent e) { runProgram(); }
        });
        tb.add(btnRun);

        btnStep = new JButton(new AbstractAction("Pas à pas") {
            public void actionPerformed(ActionEvent e) { stepOnce(); }
        });
        tb.add(btnStep);

        btnStop = new JButton(new AbstractAction("Stop") {
            public void actionPerformed(ActionEvent e) { stopProgram(); }
        });
        tb.add(btnStop);

        tb.addSeparator();

        tb.add(new JButton(new AbstractAction("Reset") {
            public void actionPerformed(ActionEvent e) { resetCPU(); }
        }));

        return tb;
    }

    // ============================================
    //  ZONE EDITOR
    // ============================================

    private Component buildCenterPanel() {
        editorArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        editorArea.setText("LDA #$01\nEND\n");
        JScrollPane scrollPane = new JScrollPane(editorArea);
        scrollPane.setBounds(0, 23, 1086, 440);
        return scrollPane;
    }

    // ============================================
    //  CONSOLE
    // ============================================

    private Component buildConsolePanel() {
        consoleArea.setEditable(false);
        consoleArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        consoleArea.setRows(6);
        JScrollPane sp = new JScrollPane(consoleArea);
        sp.setBounds(10, 423, 1086, 128);
        sp.setBorder(BorderFactory.createTitledBorder("Console"));
        return sp;
    }

    // ============================================
    //  FICHIER
    // ============================================

    private File currentFile = null;

    private void newFile() {
        editorArea.setText("");
        currentFile = null;
        log("Nouveau fichier créé.");
    }

    private void openFile() {
        JFileChooser ch = new JFileChooser();
        if (ch.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            currentFile = ch.getSelectedFile();
            try (BufferedReader br = new BufferedReader(new FileReader(currentFile))) {
                editorArea.read(br, null);
                log("Ouvert : " + currentFile.getName());
            } catch (Exception ex) {
                showError(ex.getMessage());
            }
        }
    }

    private void saveFile(boolean choose) {
        if (choose || currentFile == null) {
            JFileChooser ch = new JFileChooser();
            if (ch.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
            currentFile = ch.getSelectedFile();
        }

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(currentFile))) {
            editorArea.write(bw);
            log("Enregistré : " + currentFile.getName());
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    // ============================================
    //  SIMULATEUR
    // ============================================

    private void assembleAndLoad() {
        int base = 0xFC00;

        memory.writeByte(base,     0x86); // LDA immédiat
        memory.writeByte(base + 1, 0x01); // Valeur
        memory.writeByte(base + 2, 0x12); // NOP pour END

        cpu.setPC(base);

        log("Programme chargé à $" + Integer.toHexString(base).toUpperCase());
    }

    private void runProgram() {
        if (running) return;

        assembleAndLoad();
        running = true;

        new Thread(() -> {
            while (running) {
                try {
                    cpu.step();
                    SwingUtilities.invokeLater(() -> registerPanel.refresh());
                    Thread.sleep(10);
                } catch (Exception ex) {
                    log("Erreur : " + ex.getMessage());
                    running = false;
                }
            }
        }).start();

        log("Exécution…");
    }

    private void stepOnce() {
        assembleAndLoad();

        try {
            cpu.step();
        } catch (Exception ex) {
            log("Erreur CPU : " + ex.getMessage());
        }

        registerPanel.refresh();
        log("Step : PC = $" + String.format("%04X", cpu.getPC()));
    }

    private void stopProgram() {
        running = false;
        log("Stop.");
    }

    private void resetCPU() {
        running = false;
        cpu.reset();
        registerPanel.refresh();
        log("CPU reset.");
    }

    // ============================================
    //  AFFICHAGE MÉMOIRE
    // ============================================

    private void showMemoryWindow(boolean rom) {

        JDialog dlg = new JDialog(this, rom ? "ROM" : "RAM", false);
        JTextArea ta = new JTextArea();
        ta.setEditable(false);
        ta.setFont(new Font("Monospaced", Font.PLAIN, 12));

        int start = rom ? 0xFC00 : 0x0000;
        int end   = rom ? 0xFFFF : 0x00FF;

        StringBuilder sb = new StringBuilder();

        for (int addr = start; addr <= end; addr += 16) {
            sb.append(String.format("%04X : ", addr));
            for (int i = 0; i < 16 && addr + i <= end; i++) {
                sb.append(String.format("%02X ", memory.readByte(addr + i) & 0xFF));
            }
            sb.append("\n");
        }

        ta.setText(sb.toString());

        dlg.getContentPane().add(new JScrollPane(ta));
        dlg.setSize(500, 400);
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }

    // ============================================
    //  CONSOLE / LOG
    // ============================================

    private void log(String msg) {
        consoleArea.append(msg + "\n");
        consoleArea.setCaretPosition(consoleArea.getDocument().getLength());
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Erreur", JOptionPane.ERROR_MESSAGE);
        log("Erreur : " + msg);
    }

    // ============================================
    //  PANNEAU REGISTRES + FLAGS
    // ============================================

    private class RegisterPanel extends JPanel {

        private JTextField pcField, instrField;
        private JTextField sField, uField;
        private JTextField aField, bField;
        private JTextField dpField, dpBitsField;
        private JTextField xField, yField;

        private JLabel fE, fF, fH, fI, fN, fZ, fV, fC;

        public RegisterPanel(CPU6809 cpu) {

            setLayout(null);
            setBackground(new Color(210,210,210));
            setBorder(BorderFactory.createTitledBorder("Architecture interne du 6809"));
            setPreferredSize(new Dimension(240, 600));

            int y = 20;

            // ----- PC -----
            JLabel pcLabel = makeLabel("PC", 10, y);
            pcField = makeField(70, y, 80);
            y += 35;

            // ----- Instruction courante -----
            instrField = makeField(10, y, 200);
            instrField.setForeground(Color.BLUE);
            y += 40;

            // ----- S et U -----
            JLabel sLabel = makeLabel("S", 10, y);
            sField = makeField(30, y, 70);

            JLabel uLabel = makeLabel("U", 120, y);
            uField = makeField(140, y, 70);
            y += 40;

            // ----- A -----
            JLabel aLabel = makeLabel("A", 10, y);
            aField = makeField(30, y, 70);
            y += 40;

            // ----- B -----
            JLabel bLabel = makeLabel("B", 10, y);
            bField = makeField(30, y, 70);
            y += 40;

            // ----- UAL dessinée -----
            UALPanel ual = new UALPanel();
            ual.setBounds(110, y - 110, 110, 140);
            add(ual);

            // ----- DP -----
            JLabel dpLabel = makeLabel("DP", 10, y);
            dpField = makeField(40, y, 40);
            dpBitsField = makeField(90, y, 120);
            y += 50;

            // ----- Flags -----
            JPanel flags = new JPanel(new GridLayout(1,8,2,2));
            flags.setBounds(20, y, 180, 30);
            flags.setBackground(new Color(210,210,210));

            fE = makeFlag("E"); flags.add(fE);
            fF = makeFlag("F"); flags.add(fF);
            fH = makeFlag("H"); flags.add(fH);
            fI = makeFlag("I"); flags.add(fI);
            fN = makeFlag("N"); flags.add(fN);
            fZ = makeFlag("Z"); flags.add(fZ);
            fV = makeFlag("V"); flags.add(fV);
            fC = makeFlag("C"); flags.add(fC);

            add(flags);
            y += 50;

            // ----- X -----
            JLabel xLabel = makeLabel("X", 10, y);
            xField = makeField(40, y, 70);
            y += 40;

            // ----- Y -----
            JLabel yLabel2 = makeLabel("Y", 10, y);
            yField = makeField(40, y, 70);

            refresh();
        }

        // ----- Champ texte -----
        private JTextField makeField(int x, int y, int w) {
            JTextField f = new JTextField();
            f.setBounds(x, y, w, 25);
            f.setFont(new Font("Monospaced", Font.BOLD, 16));
            f.setEditable(false);
            f.setForeground(Color.BLUE);
            f.setHorizontalAlignment(JTextField.CENTER);
            add(f);
            return f;
        }

        // ----- Label -----
        private JLabel makeLabel(String s, int x, int y) {
            JLabel l = new JLabel(s);
            l.setBounds(x, y, 50, 25);
            l.setFont(new Font("SansSerif", Font.BOLD, 16));
            add(l);
            return l;
        }

        // ----- Flag -----
        private JLabel makeFlag(String s) {
            JLabel l = new JLabel(s, SwingConstants.CENTER);
            l.setOpaque(true);
            l.setBackground(new Color(230,230,230));
            l.setBorder(BorderFactory.createLineBorder(Color.BLACK));
            return l;
        }

        // ----- Rafraîchissement -----
        public void refresh() {
            pcField.setText(hex(cpu.getPC(), 4));
            instrField.setText(cpu.getCurrentInstructionText());

            sField.setText(hex(cpu.getS(), 4));
            uField.setText(hex(cpu.getU(), 4));
            aField.setText(hex(cpu.getA(), 2));
            bField.setText(hex(cpu.getB(), 2));
            dpField.setText(hex(cpu.getDP(), 2));

            dpBitsField.setText(String.format("%8s",
                    Integer.toBinaryString(cpu.getDP() & 0xFF)).replace(' ', '0'));

            xField.setText(hex(cpu.getX(), 4));
            yField.setText(hex(cpu.getY(), 4));

            int CCR = cpu.getCCR();
            setFlag(fC, (CCR & 0x01)!=0);
            setFlag(fV, (CCR & 0x02)!=0);
            setFlag(fZ, (CCR & 0x04)!=0);
            setFlag(fN, (CCR & 0x08)!=0);
            setFlag(fI, (CCR & 0x10)!=0);
            setFlag(fH, (CCR & 0x20)!=0);
            setFlag(fF, (CCR & 0x40)!=0);
            setFlag(fE, (CCR & 0x80)!=0);
        }

        private void setFlag(JLabel f, boolean active) {
            f.setBackground(active ? Color.GREEN : new Color(230,230,230));
        }

        private String hex(int v, int len) {
            return String.format("%0" + len + "X", v);
        }
    }
    private class UALPanel extends JPanel {
        public UALPanel() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g;
            g2.setStroke(new BasicStroke(2));
            g2.setColor(Color.BLACK);

            // Forme UAL
            int[] x = {10, 80, 60, 80, 10};
            int[] y = {10, 10, 40, 70, 70};
            g2.drawPolygon(x, y, 5);

            g2.setFont(new Font("SansSerif", Font.BOLD, 20));
            g2.drawString("UAL", 25, 45);
        }
    }


    // ============================================
    //  MAIN
    // ============================================

    public static void main(String[] args) {

        Memory memory = new Memory();
        CPU6809 cpu   = new CPU6809(memory);
        Debugger dbg  = new Debugger(cpu, memory);

        SwingUtilities.invokeLater(() -> {
            new GUI(cpu, memory, dbg).setVisible(true);
        });
    }
}
