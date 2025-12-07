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

    // Log interne
    private final StringBuilder logBuffer = new StringBuilder();

    public Debugger(CPU6809 cpu, Memory mem) {
        this.cpu = cpu;
        this.mem = mem;
    }

    // =====================================================
    //  BREAKPOINTS
    // =====================================================

 
	public void addBreakpoint(int addr) {
        addr &= 0xFFFF;
        breakpoints.add(addr);
        log(String.format("Breakpoint ajouté @ %04X", addr));
    }

    public void removeBreakpoint(int addr) {
        addr &= 0xFFFF;
        breakpoints.remove(addr);
        log(String.format("Breakpoint supprimé @ %04X", addr));
    }

    public void clearBreakpoints() {
        breakpoints.clear();
        log("Tous les breakpoints ont été supprimés.");
    }

    public boolean isBreakpoint(int addr) {
        return breakpoints.contains(addr & 0xFFFF);
    }

    public Set<Integer> getBreakpoints() {
        return new HashSet<>(breakpoints);
    }

    // =====================================================
    //  LOGGING
    // =====================================================

    public void log(String msg) {
        logBuffer.append(msg).append("\n");
        System.out.println(msg);
    }

    public String getLog() {
        return logBuffer.toString();
    }

    public void clearLog() {
        logBuffer.setLength(0);
    }

    // =====================================================
    //  EXECUTION
    // =====================================================

    /**
     * Exécute une seule instruction.
     */
    public void step() {
        cpu.stepWithDebugger(this);
    }

    /**
     * Exécute jusqu'à un breakpoint ou fin du programme.
     */
    public void run() {
        log("=== Exécution démarrée ===");

        while (true) {
            boolean cont = cpu.stepWithDebugger(this);
            if (!cont) {
                log("=== Exécution stoppée ===");
                break;
            }
        }
    }

    // =====================================================
    //  OUTILS DEBUG
    // =====================================================

    public void dumpRegisters() {
        log("--- Registres ---");
        log(cpu.dumpRegisters());
    }

    public void dumpMemory(int start, int end) {
        log("--- Dump mémoire ---");

        for (int addr = start & 0xFFFF; addr <= (end & 0xFFFF); addr++) {
            int val = mem.readByte(addr);
            log(String.format("%04X : %02X", addr, val));
        }
    }

    public void dumpMemoryBlock(int addr, int size) {
        dumpMemory(addr, addr + size - 1);
    }
    public int readMemory(int addr) {
        return mem.readByte(addr & 0xFFFF);
    }
    public void writeMemory(int addr, int value) {
        mem.writeByte(addr & 0xFFFF, value & 0xFF);
        log(String.format("Mémoire modifiée @ %04X = %02X", addr, value));
    }

}
