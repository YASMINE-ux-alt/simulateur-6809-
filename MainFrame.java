package gui;

import cpu.CPU6809;
import memory.Memory;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {

    private final CPU6809 cpu;
    private final Memory mem;

    private final RegistersPanel registersPanel;
    private final MemoryPanel memoryPanel;

    // Adresse de base du dump mémoire (modifiable si tu veux plus tard)
    private int memoryBase = 0x2000;
    private int memoryLen  = 0x80;

    public MainFrame(CPU6809 cpu, Memory mem) {
        super("Motorola 6809 Emulator");
        this.cpu = cpu;
        this.mem = mem;

        // config fenêtre
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(8, 8));

        // Panels
        registersPanel = new RegistersPanel();
        memoryPanel    = new MemoryPanel();

        // === Haut : barre de boutons ===
        JPanel topBar = buildTopBar();

        // === Gauche : registres ===
        JPanel left = new JPanel(new BorderLayout());
        left.add(registersPanel, BorderLayout.CENTER);
        left.setBorder(BorderFactory.createTitledBorder("Registres"));

        // === Centre : mémoire ===
        JPanel center = new JPanel(new BorderLayout());
        center.add(memoryPanel, BorderLayout.CENTER);
        center.setBorder(BorderFactory.createTitledBorder(
                String.format("Mémoire (0x%04X - 0x%04X)", memoryBase, memoryBase + memoryLen)
        ));

        add(topBar, BorderLayout.NORTH);
        add(left, BorderLayout.WEST);
        add(center, BorderLayout.CENTER);

        // Init état
        refreshAll();
    }

    private JPanel buildTopBar() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton btnReset      = new JButton("Reset");
        JButton btnLoadSample = new JButton("Charger programme test");
        JButton btnStep       = new JButton("Step");
        JButton btnRun100     = new JButton("Run x100");

        btnReset.addActionListener(e -> {
            cpu.reset();
            refreshAll();
        });

        btnLoadSample.addActionListener(e -> {
            loadSampleProgram();
            cpu.reset();
            // PC=0x0000 dans reset, donc ok
            refreshAll();
            JOptionPane.showMessageDialog(this,
                    "Programme test chargé à l'adresse 0x0000.",
                    "Info", JOptionPane.INFORMATION_MESSAGE);
        });

        btnStep.addActionListener(e -> {
            try {
                cpu.step();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Erreur pendant step():\n" + ex.getMessage(),
                        "Erreur CPU",
                        JOptionPane.ERROR_MESSAGE);
            }
            refreshAll();
        });

        btnRun100.addActionListener(e -> {
            try {
                for (int i = 0; i < 100; i++) {
                    cpu.step();
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Erreur pendant run x100:\n" + ex.getMessage(),
                        "Erreur CPU",
                        JOptionPane.ERROR_MESSAGE);
            }
            refreshAll();
        });

        panel.add(btnReset);
        panel.add(btnLoadSample);
        panel.add(btnStep);
        panel.add(btnRun100);

        return panel;
    }

    /**
     * Programme de test (même idée que dans ton Main console) :
     *
     * 0000: 86 10        LDA #$10
     * 0002: 8B 05        ADDA #$05
     * 0004: B7 20 00     STA $2000
     * 0007: 7E 00 06     JMP $0006   ; boucle
     */
    private void loadSampleProgram() {
        byte[] program = {
                (byte)0x86, 0x10,       // LDA #$10
                (byte)0x8B, 0x05,       // ADDA #$05
                (byte)0xB7, 0x20, 0x00, // STA $2000
                (byte)0x7E, 0x00, 0x06  // JMP $0006
        };
        mem.loadProgram(program, 0x0000);
    }

    private void refreshAll() {
        registersPanel.updateFromCpu(cpu);
        memoryPanel.updateFromMemory(mem, memoryBase, memoryLen);
    }
}
