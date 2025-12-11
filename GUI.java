package gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

import cpu.CPU6809;
import debugger.Debugger;
import memory.Memory;
import assembler.Assembler;

public class GUI extends JFrame {

    private static final long serialVersionUID = 1L;

    // ===== CPU / MEMOIRE =====
    private Memory memory;
    private CPU6809 cpu;
    private Debugger debugger;
    private Assembler assembler;

  

  
    // ===== CONTROLE EXECUTION =====
    private boolean running = false;
    private SwingWorker<Void, Void> worker;

    // ===== PANELS =====
    private JPanel contentPane;
    private JScrollPane scrollPaneROM;
    private JScrollPane scrollPaneRAM;

    // ===== TABLES =====
    private JTable tableROM;
    private JTable tableRAM;

    // ===== PANEL CPU =====
    private CPUPanel6809 cpuPanel;

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

    public GUI() {

        // === INIT CPU ===
        memory = new Memory();
        cpu = new CPU6809(memory);
        debugger = new Debugger(cpu, memory);
        assembler = new Assembler();


        cpu.reset();

        // === FENÊTRE ===
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, 1308, 882);

        contentPane = new JPanel();
        contentPane.setBackground(new Color(255, 210, 210));
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        contentPane.setLayout(null);
        setContentPane(contentPane);

        // === ZONE CODE ===
        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setBounds(0, 23, 216, 444);
        contentPane.add(scrollPane);

        JTextArea textArea = new JTextArea();
        scrollPane.setViewportView(textArea);

        // === BOUTONS ===
        JButton BtnNew = new JButton("New");
        BtnNew.setFont(new Font("Rockwell Condensed", Font.BOLD, 14));
        BtnNew.setBounds(0, 0, 64, 20);
        BtnNew.addActionListener(e -> textArea.setText(""));
        contentPane.add(BtnNew);

        JButton btnRom = new JButton("ROM");
        btnRom.setFont(new Font("Rockwell Condensed", Font.BOLD, 14));
        btnRom.setBounds(260, 0, 64, 20);
        btnRom.addActionListener(e -> {
            scrollPaneROM.setVisible(true);
            refreshROM();
        });
        contentPane.add(btnRom);

        JButton btnRam = new JButton("RAM");
        btnRam.setFont(new Font("Rockwell Condensed", Font.BOLD, 14));
        btnRam.setBounds(330, 0, 64, 20);
        btnRam.addActionListener(e -> {
            scrollPaneRAM.setVisible(true);
            refreshRAM();
        });
        contentPane.add(btnRam);
        JButton btnAssemble = new JButton("Assemble");
        btnAssemble.setFont(new Font("Rockwell Condensed", Font.BOLD, 14));
        btnAssemble.setBounds(190, 0, 80, 20);
        btnAssemble.addActionListener(e -> {
            assembler.assembleAndLoad(textArea.getText(), memory);
            cpu.reset();
            cpuPanel.refresh();
            refreshROM();
        });


        contentPane.add(btnAssemble);

        JButton btnRun = new JButton("Run");
        btnRun.setFont(new Font("Rockwell Condensed", Font.BOLD, 14));
        btnRun.setBounds(505, 0, 64, 20);
        contentPane.add(btnRun);

        btnRun.addActionListener(e -> {
            if (running) return;
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

        JButton btnStop = new JButton("Stop");
        btnStop.setFont(new Font("Rockwell Condensed", Font.BOLD, 14));
        btnStop.setBounds(569, 0, 64, 20);
        btnStop.addActionListener(e -> {
            running = false;
            if (worker != null) worker.cancel(true);
        });
        contentPane.add(btnStop);

        JButton btnPasPas = new JButton("Pas à pas");
        btnPasPas.setFont(new Font("Rockwell Condensed", Font.BOLD, 14));
        btnPasPas.setBounds(631, 0, 93, 20);
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
        contentPane.add(btnPasPas);

        JButton btnReset = new JButton("Reset");
        btnReset.setFont(new Font("Rockwell Condensed", Font.BOLD, 14));
        btnReset.setBounds(722, 0, 80, 20);
        btnReset.addActionListener(e -> {
            cpu.reset();
            cpuPanel.refresh();
            refreshRAM();
            refreshROM();
        });
        contentPane.add(btnReset);

        // === TITRE PANEL CPU ===
        JLabel title = new JLabel("Architecture interne du 6809");
        title.setFont(new Font("Rockwell Condensed", Font.BOLD, 18));
        title.setHorizontalAlignment(SwingConstants.CENTER);
        title.setBounds(858, 10, 394, 25);
        title.setForeground(new Color(128, 55, 95));
        contentPane.add(title);

        // === PANEL CPU ===
        cpuPanel = new CPUPanel6809(cpu);
        cpuPanel.lblDP.setBounds(70, 260, 106, 30);
        cpuPanel.setBounds(858, 64, 394, 450);
        contentPane.add(cpuPanel);

        // === TABLE ROM ===
        scrollPaneROM = new JScrollPane();
        scrollPaneROM.setBounds(235, 28, 115, 174);
        contentPane.add(scrollPaneROM);

        tableROM = new JTable();
        scrollPaneROM.setViewportView(tableROM);

        DefaultTableModel modelROM = new DefaultTableModel();
        modelROM.addColumn("Adresse");
        modelROM.addColumn("Valeur");
        for (int i = 0xFC00; i <= 0xFFFF; i++)
            modelROM.addRow(new Object[]{String.format("%04X", i), "00"});
        tableROM.setModel(modelROM);
        scrollPaneROM.setVisible(false);

        // === TABLE RAM ===
        scrollPaneRAM = new JScrollPane();
        scrollPaneRAM.setBounds(360, 30, 115, 172);
        contentPane.add(scrollPaneRAM);

        tableRAM = new JTable();
        scrollPaneRAM.setViewportView(tableRAM);

        DefaultTableModel modelRAM = new DefaultTableModel();
        modelRAM.addColumn("Adresse");
        modelRAM.addColumn("Valeur");
        for (int i = 0x0000; i <= 0x03FF; i++)
            modelRAM.addRow(new Object[]{String.format("%04X", i), "00"});
        tableRAM.setModel(modelRAM);
        scrollPaneRAM.setVisible(false);
    }

    // === MISE À JOUR MEMOIRE ===
    private void refreshRAM() {
        DefaultTableModel model = (DefaultTableModel) tableRAM.getModel();
        for (int i = 0; i < model.getRowCount(); i++) {
            int addr = Integer.parseInt(model.getValueAt(i, 0).toString(), 16);
            int value = memory.readByte(addr);
            model.setValueAt(String.format("%02X", value), i, 1);
        }
    }

    private void refreshROM() {
        DefaultTableModel model = (DefaultTableModel) tableROM.getModel();
        for (int i = 0; i < model.getRowCount(); i++) {
            int addr = Integer.parseInt(model.getValueAt(i, 0).toString(), 16);
            int value = memory.readByte(addr);
            model.setValueAt(String.format("%02X", value), i, 1);
        }
    }

    
}
