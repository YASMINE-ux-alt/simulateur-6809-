package cpu;

import java.util.function.Consumer;

public class Instruction {

    // Modes d’adressage supportés
 
    public enum AddressingMode {
        IMMEDIATE8,
        IMMEDIATE16,
        DIRECT,
        INDEXED,
        EXTENDED,
        INHERENT
    }

  
    // Données de l’instruction
   
    private final String mnemonic;
    private final int opcode;     //
    private final int size;
    private final int cycles;
    private final AddressingMode mode;
    private final Consumer<CPU6809> executor;

   
    // Constructeur
    
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

    
    // Getters
    
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

    
    // Exécution
    
    public void execute(CPU6809 cpu) {
        executor.accept(cpu);
    }

    
    // Debug
    
    @Override
    public String toString() {
        return String.format(
                "%s (opcode=%02X, size=%d, mode=%s)",
                mnemonic, opcode, size, mode
        );
    }
}


