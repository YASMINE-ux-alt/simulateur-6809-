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
        contentPane.setBackground(new Color(173, 209, 245));
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(contentPane);
        contentPane.setLayout(null);

        // === ZONE CODE ===
        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setBounds(0, 23, 216, 444);
        contentPane.add(scrollPane);

        JTextArea textArea = new JTextArea();
        textArea.setFont(new Font("Arial Black", Font.BOLD, 13));
        scrollPane.setViewportView(textArea);

        // === BOUTONS ===
        JButton BtnNew = new JButton("New");
        BtnNew.setBounds(0, 0, 64, 20);
        BtnNew.setFont(new Font("Rockwell Condensed", Font.BOLD, 14));
        BtnNew.addActionListener(e -> textArea.setText(""));
        contentPane.add(BtnNew);

        JButton btnRom = new JButton("ROM");
        btnRom.setBounds(260, 0, 64, 20);
        btnRom.setFont(new Font("Rockwell Condensed", Font.BOLD, 14));
        btnRom.addActionListener(e -> {
            scrollPaneROM.setVisible(true);
            refreshROM();
        });
        contentPane.add(btnRom);

        JButton btnRam = new JButton("RAM");
        btnRam.setBounds(376, 0, 64, 20);
        btnRam.setFont(new Font("Rockwell Condensed", Font.BOLD, 14));
        btnRam.addActionListener(e -> {
            scrollPaneRAM.setVisible(true);
            refreshRAM();
        });
        contentPane.add(btnRam);
        JButton btnAssemble = new JButton("Assemble");
        btnAssemble.setBounds(450, 0, 80, 20);
        btnAssemble.setFont(new Font("Rockwell Condensed", Font.BOLD, 14));
        btnAssemble.addActionListener(e -> {
            assembler.assembleAndLoad(textArea.getText(), memory);
            cpu.reset();
            cpuPanel.refresh();
            refreshROM();
        });


        contentPane.add(btnAssemble);

        JButton btnRun = new JButton("Run");
        btnRun.setBounds(519, 0, 64, 20);
        btnRun.setFont(new Font("Rockwell Condensed", Font.BOLD, 14));
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
        btnStop.setBounds(579, 0, 64, 20);
        btnStop.setFont(new Font("Rockwell Condensed", Font.BOLD, 14));
        btnStop.addActionListener(e -> {
            running = false;
            if (worker != null) worker.cancel(true);
        });
        contentPane.add(btnStop);

        JButton btnPasPas = new JButton("Pas à pas");
        btnPasPas.setBounds(643, 0, 93, 20);
        btnPasPas.setFont(new Font("Rockwell Condensed", Font.BOLD, 14));
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
        btnReset.setBounds(734, 0, 80, 20);
        btnReset.setFont(new Font("Rockwell Condensed", Font.BOLD, 14));
        btnReset.addActionListener(e -> {
            cpu.reset();
            cpuPanel.refresh();
            refreshRAM();
            refreshROM();
        });
        contentPane.add(btnReset);

        // === TITRE PANEL CPU ===
        JLabel title = new JLabel("Architecture interne du 6809");
        title.setBounds(829, -3, 394, 25);
        title.setFont(new Font("Rockwell Condensed", Font.BOLD, 18));
        title.setHorizontalAlignment(SwingConstants.CENTER);
        title.setForeground(new Color(0, 0, 160));
        contentPane.add(title);

        // === PANEL CPU ===
        cpuPanel = new CPUPanel6809(cpu);
        cpuPanel.setForeground(new Color(224, 222, 224));
        cpuPanel.setBackground(new Color(224, 222, 224));
        cpuPanel.setBounds(839, 37, 394, 390);
        cpuPanel.lblY.setBounds(220, 330, 106, 30);
        cpuPanel.lblU.setBounds(220, 90, 97, 30);
        cpuPanel.lblS.setBounds(70, 90, 106, 30);
        cpuPanel.lblA.setBounds(70, 150, 69, 30);
        cpuPanel.lblB.setBounds(70, 200, 69, 30);
        cpuPanel.lblX.setBounds(70, 330, 106, 30);
        cpuPanel.lblDP.setBounds(70, 260, 106, 30);
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
