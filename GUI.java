package debugger;

import cpu.CPU6809;
import memory.Memory;

import java.util.HashSet;
import java.util.Set;

public class Debugger {

    private final CPU6809 cpu;
    private final Memory mem;

    // Liste des breakpoints
    private final Set<Integer> breakpoints = new HashSet<>();

    // État d’exécution
    private boolean running = false;

    public Debugger(CPU6809 cpu, Memory mem) {
        this.cpu = cpu;
        this.mem = mem;
    }

    // ============================================
    // BREAKPOINTS
    // ============================================

    public void addBreakpoint(int addr) {
        addr &= 0xFFFF;
        breakpoints.add(addr);
        System.out.println("Breakpoint ajouté @ " + hex(addr));
    }

    public void removeBreakpoint(int addr) {
        addr &= 0xFFFF;
        breakpoints.remove(addr);
        System.out.println("Breakpoint supprimé @ " + hex(addr));
    }

    public boolean isBreakpoint(int addr) {
        return breakpoints.contains(addr & 0xFFFF);
    }

    // ============================================
    // EXÉCUTION
    // ============================================

    // Exécute une seule instruction (pas à pas)
    public void step() {
        int pc = cpu.getPC();
        if (isBreakpoint(pc)) {
            System.out.println("⚠ Breakpoint atteint @ " + hex(pc));
            dumpRegisters();
            return;
        }

        cpu.step();
        dumpRegisters();
    }

    // Exécution continue jusqu'à un breakpoint
    public void run() {
        running = true;
        System.out.println("▶ Exécution démarrée");

        while (running) {

            int pc = cpu.getPC();
            if (isBreakpoint(pc)) {
                System.out.println("⛔ Breakpoint atteint @ " + hex(pc));
                dumpRegisters();
                running = false;
                break;
            }

            cpu.step();
        }

        System.out.println("■ Exécution stoppée");
    }

    // Arrête l’exécution
    public void stop() {
        running = false;
        System.out.println("■ Stop demandé");
    }

    // ============================================
    // INSPECTION
    // ============================================

    public void dumpRegisters() {
        System.out.println("-----------------------------");
        System.out.println(" PC=" + hex(cpu.getPC()));
        System.out.println(" A =" + hex8(cpu.getA()));
        System.out.println(" B =" + hex8(cpu.getB()));
        System.out.println(" D =" + hex(cpu.getD()));
        System.out.println(" X =" + hex(cpu.getX()));
        System.out.println(" Y =" + hex(cpu.getY()));
        System.out.println(" S =" + hex(cpu.getS()));
        System.out.println(" U =" + hex(cpu.getU()));
        System.out.println(" DP=" + hex8(cpu.getDP()));
        System.out.println(" CCR=" + hex8(cpu.getCCR()));
        System.out.println("-----------------------------");
    }

    // Dump mémoire RAM en HEX (
    public void dumpRAM(int start, int length) {
        start &= 0xFFFF;
        System.out.println("=== RAM Dump ===");

        for (int i = 0; i < length; i += 16) {
            int addr = (start + i) & 0xFFFF;

            StringBuilder line = new StringBuilder();
            line.append(hex(addr)).append(": ");

            for (int j = 0; j < 16 && (i + j) < length; j++) {
                int value = mem.readByte((addr + j) & 0xFFFF);
                line.append(hex8(value)).append(" ");
            }

            System.out.println(line);
        }
    }public void dumpROM(int start, int length) {
        start &= 0xFFFF;
        System.out.println("=== ROM Dump ===");

        for (int i = 0; i < length; i += 16) {
            int addr = (start + i) & 0xFFFF;

            StringBuilder line = new StringBuilder();
            line.append(hex(addr)).append(": ");

            for (int j = 0; j < 16 && (i + j) < length; j++) {
                int value = mem.readByte((addr + j) & 0xFFFF);
                line.append(hex8(value)).append(" ");
            }

            System.out.println(line);
        }
    }


    // ============================================
    // FORMATAGE
    // ============================================

    private String hex(int v) {
        return String.format("%04X", v & 0xFFFF);
    }

    private String hex8(int v) {
        return String.format("%02X", v & 0xFF);
    }
}
