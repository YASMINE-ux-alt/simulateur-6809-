package gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

import cpu.CPU6809;
import debugger.Debugger;
import memory.Memory;

public class GUI extends JFrame {

    private static final long serialVersionUID = 1L;

    // ===== CPU / MEMOIRE =====
    private Memory memory;
    private CPU6809 cpu;
    private Debugger debugger;

    // ===== CONTROLE EXECUTION =====
    private boolean running = false;
    private SwingWorker<Void, Void> worker;

    // ===== PANELS =====
    private JPanel contentPane;
    private JScrollPane scrollPane_1; // ROM
    private JScrollPane scrollPane_2; // RAM

    // ===== TABLES =====
    private JTable table;   // ROM
    private JTable table_2; // RAM

    // ===== PANEL CPU =====
    private CPUPanel6809 cpuPanel;

    /**
     * MAIN
     */
    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            try {
                GUI frame = new GUI();
                frame.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * CONSTRUCTEUR
     */
    public GUI() {

        // ===== INIT CPU & MEMOIRE =====
        memory = new Memory();
        cpu = new CPU6809(memory);
        debugger = new Debugger(cpu, memory);

        loadTestProgram(); // Charger un petit programme test
        cpu.reset();

        // ===== FENETRE =====
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, 1308, 882);

        contentPane = new JPanel();
        contentPane.setBackground(new Color(255, 210, 210));
        contentPane.setForeground(new Color(181, 70, 142));
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        contentPane.setLayout(null);
        setContentPane(contentPane);

        // ========== ZONE CODE ==========
        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setBounds(0, 23, 216, 444);
        contentPane.add(scrollPane);

        JTextArea textArea = new JTextArea();
        scrollPane.setViewportView(textArea);

        // ========== BOUTONS ==========
        JButton BtnNew = new JButton("New");
        BtnNew.setFont(new Font("Rockwell Condensed", Font.BOLD, 14));
        BtnNew.setForeground(new Color(128, 55, 95));
        BtnNew.setBounds(0, 0, 64, 20);
        BtnNew.addActionListener(e -> textArea.setText(""));
        contentPane.add(BtnNew);

        JButton btnRam = new JButton("RAM");
        btnRam.setFont(new Font("Rockwell Condensed", Font.BOLD, 13));
        btnRam.setForeground(new Color(128, 55, 95));
        btnRam.setBounds(389, 0, 64, 20);
        btnRam.addActionListener(e -> {
            scrollPane_2.setVisible(true);
            refreshRAM();
        });
        contentPane.add(btnRam);

        JButton btnRom = new JButton("ROM");
        btnRom.setFont(new Font("Rockwell Condensed", Font.BOLD, 14));
        btnRom.setForeground(new Color(128, 55, 95));
        btnRom.setBounds(260, 0, 64, 20);
        btnRom.addActionListener(e -> {
            scrollPane_1.setVisible(true);
            refreshROM();
        });
        contentPane.add(btnRom);

        // ======== RUN ========
        JButton btnRun = new JButton("Run");
        btnRun.setFont(new Font("Rockwell Condensed", Font.BOLD, 14));
        btnRun.setForeground(new Color(128, 55, 95));
        btnRun.setBounds(505, 0, 64, 20);
        contentPane.add(btnRun);

        btnRun.addActionListener(e -> {
            if (running) return; // déjà en cours

            running = true;

            worker = new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    while (running) {
                        cpu.step();
                        cpuPanel.refresh();
                        refreshRAM();
                        refreshROM();
                        Thread.sleep(20);
                    }
                    return null;
                }
            };
            worker.execute();
        });

        // ======== STOP ========
        JButton btnStop = new JButton("Stop");
        btnStop.setFont(new Font("Rockwell Condensed", Font.BOLD, 14));
        btnStop.setForeground(new Color(128, 55, 95));
        btnStop.setBounds(569, 0, 64, 20);
        contentPane.add(btnStop);

        btnStop.addActionListener(e -> {
            running = false;
            if (worker != null)
                worker.cancel(true);
        });

        // ======== PAS A PAS ========
        JButton btnPasPas = new JButton("Pas à pas");
        btnPasPas.setFont(new Font("Rockwell Condensed", Font.BOLD, 14));
        btnPasPas.setForeground(new Color(128, 55, 95));
        btnPasPas.setBounds(631, 0, 93, 20);
        contentPane.add(btnPasPas);

        btnPasPas.addActionListener(e -> {
            try {
                cpu.step();
                cpuPanel.refresh();
                refreshRAM();
                refreshROM();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, "Erreur CPU : " + ex.getMessage());
            }
        });

        // ======== RESET ========
        JButton btnReset = new JButton("Reset");
        btnReset.setFont(new Font("Rockwell Condensed", Font.BOLD, 14));
        btnReset.setForeground(new Color(128, 55, 95));
        btnReset.setBounds(722, 0, 80, 20);
        contentPane.add(btnReset);

        btnReset.addActionListener(e -> {
            cpu.reset();
            cpuPanel.refresh();
            refreshRAM();
            refreshROM();
        });

        // ======== PANEL CPU ========
        cpuPanel = new CPUPanel6809(cpu);
        cpuPanel.lblX.setBounds(70, 330, 95, 30);
        cpuPanel.lblDP.setSize(95, 30);
        cpuPanel.lblDP.setLocation(45, 260);
        cpuPanel.lblB.setBounds(70, 200, 71, 30);
        cpuPanel.lblA.setBounds(70, 150, 71, 30);
        cpuPanel.lblU.setBounds(220, 90, 95, 30);
        cpuPanel.lblS.setBounds(70, 90, 95, 30);
        cpuPanel.lblPC.setBounds(100, 20, 163, 30);
        cpuPanel.setForeground(new Color(255, 128, 192));
        cpuPanel.setBackground(new Color(255, 132, 132));
        cpuPanel.lblU.setForeground(new Color(128, 55, 95));
        cpuPanel.setBounds(858, 62, 394, 450);
        contentPane.add(cpuPanel);

        // ======== TABLE ROM ========
        scrollPane_1 = new JScrollPane();
        scrollPane_1.setBounds(235, 28, 115, 174);
        contentPane.add(scrollPane_1);

        table = new JTable();
        scrollPane_1.setViewportView(table);

        DefaultTableModel modelROM = new DefaultTableModel();
        modelROM.addColumn("Adresse");
        modelROM.addColumn("Valeur");

        for (int i = 0xFC00; i <= 0xFFFF; i++)
            modelROM.addRow(new Object[]{String.format("%04X", i), "00"});

        table.setModel(modelROM);
        scrollPane_1.setVisible(false);

        // ======== TABLE RAM ========
        scrollPane_2 = new JScrollPane();
        scrollPane_2.setBounds(360, 30, 115, 172);
        contentPane.add(scrollPane_2);

        table_2 = new JTable();
        scrollPane_2.setViewportView(table_2);

        DefaultTableModel modelRAM = new DefaultTableModel();
        modelRAM.addColumn("Adresse");
        modelRAM.addColumn("Valeur");

        for (int i = 0x0000; i <= 0x03FF; i++)
            modelRAM.addRow(new Object[]{String.format("%04X", i), "00"});

        table_2.setModel(modelRAM);
        
        JPanel panel = new JPanel();
        panel.setBackground(new Color(255, 132, 132));
        panel.setForeground(new Color(0, 0, 0));
        panel.setToolTipText("Architecture interne du 6809");
        panel.setBounds(879, 10, 347, 25);
        contentPane.add(panel);
        scrollPane_2.setVisible(false);
    }

    // ==================================================================
    // ========================= METHODES ===============================
    // ==================================================================

    private void refreshRAM() {
        DefaultTableModel model = (DefaultTableModel) table_2.getModel();
        for (int i = 0; i < model.getRowCount(); i++) {
            int addr = Integer.parseInt(model.getValueAt(i, 0).toString(), 16);
            int value = memory.readByte(addr);
            model.setValueAt(String.format("%02X", value), i, 1);
        }
    }

    private void refreshROM() {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        for (int i = 0; i < model.getRowCount(); i++) {
            int addr = Integer.parseInt(model.getValueAt(i, 0).toString(), 16);
            int value = memory.readByte(addr);
            model.setValueAt(String.format("%02X", value), i, 1);
        }
    }

    private void loadTestProgram() {

        int[] program = {
            0x86, 0x05,      // LDA #$05
            0xC6, 0x03,      // LDB #$03
            0x8B, 0x02,      // ADDA #$02
            0xD7, 0x0A,      // STB $000A
            0x97, 0x0B,      // STA $000B
            0x7E, 0xFC, 0x00 // JMP FC00
        };

        int addr = 0xFC00;

        for (int op : program)
            memory.writeByte(addr++, op);

        memory.writeByte(0xFFFE, 0xFC);
        memory.writeByte(0xFFFF, 0x00);
    }
}
