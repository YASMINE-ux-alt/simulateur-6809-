package cpu;

import java.util.function.Consumer;

public class Instruction {

    // -------------------------------
    // Modes d’adressage supportés
    // -------------------------------
    public enum AddressingMode {
        IMMEDIATE8,     // #$xx
        IMMEDIATE16,    // #$xxxx
        DIRECT,         // <addr> = DP:offset
        INDEXED,        // ,X / offset,X / ,Y / offset,Y
        EXTENDED,       // $xxxx
        RELATIVE,       // branchement relatif
        INHERENT        // pas d’opérande
    }

    // -------------------------------
    // Données de l’instruction
    // -------------------------------
    private final String mnemonic;                   // "LDA", "ADDA", etc.
    private final int opcode;                        // code machine 0x86, 0x8B...
    private final int size;                          // nombre d’octets (1 à 3)
    private final int cycles;                        // cycles CPU
    private final AddressingMode mode;
    private final Consumer<CPU6809> executor;        // code à exécuter

    // -------------------------------
    // Constructeur
    // -------------------------------
    public Instruction(String mnemonic,
                       int opcode,
                       int size,
                       int cycles,
                       AddressingMode mode,
                       Consumer<CPU6809> executor) {

        this.mnemonic = mnemonic;
        this.opcode = opcode & 0xFF;
        this.size = size;
        this.cycles = cycles;
        this.mode = mode;
        this.executor = executor;
    }

    // -------------------------------
    // Getters
    // -------------------------------
    public String getMnemonic() {
        return mnemonic;
    }

    public int getOpcode() {
        return opcode;
    }

    public int getSize() {
        return size;
    }

    public int getCycles() {
        return cycles;
    }

    public AddressingMode getMode() {
        return mode;
    }

    // -------------------------------
    // Exécution
    // -------------------------------
    public void execute(CPU6809 cpu) {
        executor.accept(cpu);
    }

    // -------------------------------
    // Debug
    // -------------------------------
    @Override
    public String toString() {
        return String.format(
                "%s (opcode=%02X, size=%d, mode=%s)",
                mnemonic, opcode, size, mode
        );
    }
}
