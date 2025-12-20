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
import assembler.Disassembler6809;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.GroupLayout.Alignment;

public class GUI extends JFrame {

    private static final long serialVersionUID = 1L;

    // ===== CPU / MEMOIRE =====
    private Memory memory;
    private CPU6809 cpu;
    private Debugger debugger;
    private Assembler assembler;

    private Disassembler6809 disassembler;
    private JTextArea consoleArea;

  
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
    //=======LABEL CONSOLE=======


    public GUI() {

        // === INIT CPU ===
        memory = new Memory();
        cpu = new CPU6809(memory);
        debugger = new Debugger(cpu, memory);
        assembler = new Assembler();

        disassembler = new Disassembler6809(cpu, memory);

        cpu.reset();

        // === FENÊTRE ===
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, 1308, 882);

        contentPane = new JPanel();
        contentPane.setBackground(new Color(156, 185, 216));
        contentPane.setBorder(new TitledBorder(null, "Editeur du programme", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        setContentPane(contentPane);
        contentPane.setLayout(null);
     // === CONSOLE DÉSASSEMBLEUR ===
        JScrollPane scrollConsole = new JScrollPane();
        scrollConsole.setBounds(0, 494, 350, 182);
        contentPane.add(scrollConsole);
        
                consoleArea = new JTextArea();
                scrollConsole.setViewportView(consoleArea);
                consoleArea.setFont(new Font("Consolas", Font.PLAIN, 13));
                consoleArea.setEditable(false);
                consoleArea.setText(
                	    disassembler.disassemble(0xFC00, 20)
                		);
                consoleArea.setText(
                	    disassembler.disassemble(cpu.getPC(), 10)
                	);

        // === ZONE CODE ===
        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setBounds(0, 23, 216, 444);
        contentPane.add(scrollPane);

        JTextArea textArea = new JTextArea();
        textArea.setFont(new Font("Arial Black", Font.BOLD, 13));
        scrollPane.setViewportView(textArea);

        // === BOUTONS ===
        JButton BtnNew = new JButton("Nouveau");
        BtnNew.setToolTipText("un nouveau programme");
        BtnNew.setBounds(0, 0, 64, 20);
        BtnNew.setFont(new Font("Rockwell Condensed", Font.BOLD, 14));
        BtnNew.addActionListener(e -> textArea.setText(""));
        
        JPanel panel = new JPanel();
        panel.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, new Color(255, 255, 255), new Color(160, 160, 160)), "Editeur ", TitledBorder.CENTER, TitledBorder.TOP, null, new Color(0, 0, 0)));
        scrollPane.setColumnHeaderView(panel);
        panel.setLayout(new CardLayout(0, 0));
        contentPane.add(BtnNew);

        JButton btnRom = new JButton("ROM");
        btnRom.setToolTipText("Afficher la mémoire ROM");
        btnRom.setBounds(65, 0, 64, 20);
        btnRom.setFont(new Font("Rockwell Condensed", Font.BOLD, 14));
        btnRom.addActionListener(e -> {
            scrollPaneROM.setVisible(true);
            refreshROM();
        });
        contentPane.add(btnRom);

        JButton btnRam = new JButton("RAM");
        btnRam.setToolTipText("Afficher la mémoire RAM");
        btnRam.setBounds(128, 0, 64, 20);
        btnRam.setFont(new Font("Rockwell Condensed", Font.BOLD, 14));
        btnRam.addActionListener(e -> {
            scrollPaneRAM.setVisible(true);
            refreshRAM();
        });
        contentPane.add(btnRam);
        JButton btnAssemble = new JButton("Assembler");
        btnAssemble.setBounds(190, 0, 93, 20);
        btnAssemble.setFont(new Font("Rockwell Condensed", Font.BOLD, 14));
        btnAssemble.addActionListener(e -> {
            assembler.assembleAndLoad(textArea.getText(), memory);
            cpu.reset();
            cpuPanel.refresh();
            refreshROM();
        } );
       


        contentPane.add(btnAssemble);

        JButton btnRun = new JButton("Exécuter");
        btnRun.setToolTipText("Exécuter le programme");
        btnRun.setBounds(282, 0, 79, 20);
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

                        // 👇 TOUT CE QUI TOUCHE À SWING ICI
                        SwingUtilities.invokeLater(() -> {
                            cpuPanel.refresh();
                            refreshRAM();
                            refreshROM();

                            // 👇 ICI EXACTEMENT
                            consoleArea.setText(
                                disassembler.disassemble(cpu.getPC(), 10)
                            );
                        });

                        Thread.sleep(20);
                    }
                    return null;
                }
            };


            worker.execute();
        });

        JButton btnStop = new JButton("Arrêter");
        btnStop.setToolTipText("Arrêter l'exécution");
        btnStop.setBounds(356, 0, 98, 20);
        btnStop.setFont(new Font("Rockwell Condensed", Font.BOLD, 14));
        btnStop.addActionListener(e -> {
            running = false;
            if (worker != null) worker.cancel(true);
        });
        contentPane.add(btnStop);

        JButton btnPasPas = new JButton("Pas à pas");
        btnPasPas.setToolTipText("Exécution pas à pas");
        btnPasPas.setBounds(454, 0, 93, 20);
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

        JButton btnReset = new JButton("Réinitialiser");
        btnReset.setToolTipText("Réinitialiser le simulateur");
        btnReset.setBounds(536, 0, 99, 20);
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
        title.setBounds(782, 17, 394, 25);
        title.setFont(new Font("Rockwell Condensed", Font.BOLD, 18));
        title.setHorizontalAlignment(SwingConstants.CENTER);
        title.setForeground(new Color(0, 0, 160));
        contentPane.add(title);

        // === PANEL CPU ===
        cpuPanel = new CPUPanel6809(cpu);
        cpuPanel.lblCycles.setForeground(new Color(136, 4, 37));
        cpuPanel.lblCycles.setBackground(new Color(255, 255, 255));
        cpuPanel.lblX.setText(" 0000");
        cpuPanel.lblY.setText(" 0000");
        cpuPanel.lblDP.setText(" 0000");
        cpuPanel.lblB.setText(" 0000");
        cpuPanel.lblA.setText(" 0000");
        cpuPanel.lblU.setText(" 0000");
        cpuPanel.lblS.setText(" 0000");
        cpuPanel.lblPC.setText(" 0000");
        cpuPanel.setToolTipText("");
        cpuPanel.lblCycles.setText(" 00");
        cpuPanel.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
        cpuPanel.setForeground(new Color(224, 222, 224));
        cpuPanel.setBackground(new Color(224, 222, 224));
        cpuPanel.setBounds(815, 38, 350, 429);
        contentPane.add(cpuPanel);
        GroupLayout gl_cpuPanel = new GroupLayout(cpuPanel);
        gl_cpuPanel.setHorizontalGroup(
        	gl_cpuPanel.createParallelGroup(Alignment.LEADING)
        		.addGroup(gl_cpuPanel.createSequentialGroup()
        			.addGap(96)
        			.addComponent(cpuPanel.lblPC, GroupLayout.PREFERRED_SIZE, 120, GroupLayout.PREFERRED_SIZE))
        		.addGroup(gl_cpuPanel.createSequentialGroup()
        			.addGap(66)
        			.addComponent(cpuPanel.lblS, GroupLayout.PREFERRED_SIZE, 106, GroupLayout.PREFERRED_SIZE)
        			.addGap(44)
        			.addComponent(cpuPanel.lblU, GroupLayout.PREFERRED_SIZE, 97, GroupLayout.PREFERRED_SIZE))
        		.addGroup(gl_cpuPanel.createSequentialGroup()
        			.addGap(66)
        			.addComponent(cpuPanel.lblA, GroupLayout.PREFERRED_SIZE, 69, GroupLayout.PREFERRED_SIZE))
        		.addGroup(gl_cpuPanel.createSequentialGroup()
        			.addGap(66)
        			.addComponent(cpuPanel.lblB, GroupLayout.PREFERRED_SIZE, 69, GroupLayout.PREFERRED_SIZE))
        		.addGroup(gl_cpuPanel.createSequentialGroup()
        			.addGap(66)
        			.addComponent(cpuPanel.lblDP, GroupLayout.PREFERRED_SIZE, 106, GroupLayout.PREFERRED_SIZE))
        		.addGroup(gl_cpuPanel.createSequentialGroup()
        			.addGap(66)
        			.addComponent(cpuPanel.lblX, GroupLayout.PREFERRED_SIZE, 106, GroupLayout.PREFERRED_SIZE)
        			.addGap(44)
        			.addComponent(cpuPanel.lblY, GroupLayout.PREFERRED_SIZE, 106, GroupLayout.PREFERRED_SIZE))
        		.addGroup(gl_cpuPanel.createSequentialGroup()
        			.addGap(116)
        			.addComponent(cpuPanel.lblCycles, GroupLayout.PREFERRED_SIZE, 47, GroupLayout.PREFERRED_SIZE))
        );
        gl_cpuPanel.setVerticalGroup(
        	gl_cpuPanel.createParallelGroup(Alignment.LEADING)
        		.addGroup(gl_cpuPanel.createSequentialGroup()
        			.addGap(16)
        			.addComponent(cpuPanel.lblPC, GroupLayout.PREFERRED_SIZE, 30, GroupLayout.PREFERRED_SIZE)
        			.addGap(40)
        			.addGroup(gl_cpuPanel.createParallelGroup(Alignment.LEADING)
        				.addComponent(cpuPanel.lblS, GroupLayout.PREFERRED_SIZE, 30, GroupLayout.PREFERRED_SIZE)
        				.addComponent(cpuPanel.lblU, GroupLayout.PREFERRED_SIZE, 30, GroupLayout.PREFERRED_SIZE))
        			.addGap(30)
        			.addComponent(cpuPanel.lblA, GroupLayout.PREFERRED_SIZE, 30, GroupLayout.PREFERRED_SIZE)
        			.addGap(20)
        			.addComponent(cpuPanel.lblB, GroupLayout.PREFERRED_SIZE, 30, GroupLayout.PREFERRED_SIZE)
        			.addGap(30)
        			.addComponent(cpuPanel.lblDP, GroupLayout.PREFERRED_SIZE, 30, GroupLayout.PREFERRED_SIZE)
        			.addGap(40)
        			.addGroup(gl_cpuPanel.createParallelGroup(Alignment.LEADING)
        				.addComponent(cpuPanel.lblX, GroupLayout.PREFERRED_SIZE, 30, GroupLayout.PREFERRED_SIZE)
        				.addComponent(cpuPanel.lblY, GroupLayout.PREFERRED_SIZE, 30, GroupLayout.PREFERRED_SIZE))
        			.addGap(20)
        			.addComponent(cpuPanel.lblCycles, GroupLayout.PREFERRED_SIZE, 30, GroupLayout.PREFERRED_SIZE))
        );
        cpuPanel.setLayout(gl_cpuPanel);

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

