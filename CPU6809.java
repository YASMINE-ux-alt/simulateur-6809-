package cpu;

import debugger.Debugger;
import memory.Memory;

public class CPU6809 {

    // === Registres 8 bits ===
    private int A;      // Accumulateur A
    private int B;      // Accumulateur B
    private int DP;     // Direct Page
    private int CCR;    // Condition Code Register

    // === Registres 16 bits ===
    private int X;
    private int Y;
    private int S;      // Stack pointer système
    private int U;      // Stack pointer utilisateur
    private int PC;     // Program Counter

    // === Mémoire ===
    private final Memory memory;

    // === Tableau d'opcodes (256 possibles) ===
    private final Instruction[] opcodes = new Instruction[256];

    // Compteur de cycles
    private long cycles = 0;

    // Flags dans CCR : bit (de MSB à LSB) = E F H I N Z V C
    private static final int FLAG_E = 0x80;
    private static final int FLAG_F = 0x40;
    private static final int FLAG_H = 0x20;
    private static final int FLAG_I = 0x10;
    private static final int FLAG_N = 0x08;
    private static final int FLAG_Z = 0x04;
    private static final int FLAG_V = 0x02;
    private static final int FLAG_C = 0x01;

    // ====== Constructeur ======
    public CPU6809(Memory memory) {
        this.memory = memory;
        reset();
        buildInstructionTable();
    }

    // ====== Getters / setters registres ======
    public int getA() { return A & 0xFF; }
    public void setA(int value) { A = value & 0xFF; }

    public int getB() { return B & 0xFF; }
    public void setB(int value) { B = value & 0xFF; }

    public int getD() {
        return ((getA() << 8) | getB()) & 0xFFFF;
    }
    public void setD(int value) {
        A = (value >> 8) & 0xFF;
        B = value & 0xFF;
    }

    public int getX() { return X & 0xFFFF; }
    public void setX(int value) { X = value & 0xFFFF; }

    public int getY() { return Y & 0xFFFF; }
    public void setY(int value) { Y = value & 0xFFFF; }

    public int getS() { return S & 0xFFFF; }
    public void setS(int value) { S = value & 0xFFFF; }

    public int getU() { return U & 0xFFFF; }
    public void setU(int value) { U = value & 0xFFFF; }

    public int getPC() { return PC & 0xFFFF; }
    public void setPC(int value) { PC = value & 0xFFFF; }

    public int getDP() { return DP & 0xFF; }
    public void setDP(int value) { DP = value & 0xFF; }

    public int getCCR() { return CCR & 0xFF; }
    public void setCCR(int value) { CCR = value & 0xFF; }

    public long getCycles() { return cycles; }

    public Instruction getInstruction(int opcode) {
        return opcodes[opcode & 0xFF];
    }

    
    // ====== Gestion des flags ======
    private void setFlag(int flag, boolean value) {
        if (value) {
            CCR |= flag;
        } else {
            CCR &= ~flag;
        }
    }

    private boolean getFlag(int flag) {
        return (CCR & flag) != 0;
    }

    private boolean isZ() { return getFlag(FLAG_Z); }
    private boolean isN() { return getFlag(FLAG_N); }
    private boolean isC() { return getFlag(FLAG_C); }
    private boolean isV() { return getFlag(FLAG_V); }

    private void clearV() { setFlag(FLAG_V, false); }

    // Mise à jour N/Z pour 8 bits
    public void updateNZ8(int result) {
        int r = result & 0xFF;
        setFlag(FLAG_Z, r == 0);
        setFlag(FLAG_N, (r & 0x80) != 0);
    }

    // Mise à jour N/Z pour 16 bits
    public void updateNZ16(int result) {
        int r = result & 0xFFFF;
        setFlag(FLAG_Z, r == 0);
        setFlag(FLAG_N, (r & 0x8000) != 0);
    }

    // ====== Mémoire ======
    public int readByte(int addr) {
        return memory.readByte(addr);
    }

    public void writeByte(int addr, int value) {
        memory.writeByte(addr, value);
    }

    public int readWord(int addr) {
        int hi = readByte(addr);
        int lo = readByte(addr + 1);
        return ((hi << 8) | lo) & 0xFFFF;
    }

    public void writeWord(int addr, int value) {
        writeByte(addr, (value >> 8) & 0xFF);
        writeByte(addr + 1, value & 0xFF);
    }

    // ====== Fetch ======
    private int fetch8() {
        int value = readByte(PC);
        PC = (PC + 1) & 0xFFFF;
        return value;
    }

    private int fetch16() {
        int hi = fetch8();
        int lo = fetch8();
        return ((hi << 8) | lo) & 0xFFFF;
    }

    // ====== Reset ======
    public void reset() {
        A = B = DP = CCR = 0;
        X = Y = S = U = 0;
        cycles = 0;
        PC = readWord(0xFFFE);
 // ou lire FFFE/FFFF si tu veux respecter le vecteur
    }

    // ====== Modes d'adressage ======
    public int imm8() {
        return fetch8();
    }

    public int imm16() {
        return fetch16();
    }

    public int directAddress() {
        int offset = fetch8();
        int base = (DP & 0xFF) << 8;
        return (base | offset) & 0xFFFF;
    }

    public int extendedAddress() {
        return fetch16();
    }

    /**
     * Indexed addressing (X/Y) — version simplifiée
     * Postbyte :
     *   bit 5 : R (0 = X, 1 = Y)
     *   modes supportés ici :
     *     0x00 : ,R
     *     0x08 : offset8,R
     *     0x09 : offset16,R
     */
    public int indexedAddress() {
        int post = fetch8();
        boolean useY = (post & 0b0010_0000) != 0;
        int base = useY ? Y : X;
        int mode = post & 0b0001_1111;

        int addr;
        switch (mode) {
            case 0x00: // ,R
                addr = base;
                break;
            case 0x08: { // offset8,R
                int off8 = (byte)fetch8(); // signé
                addr = (base + off8) & 0xFFFF;
                break;
            }
            case 0x09: { // offset16,R
                int off16 = (short)fetch16(); // signé
                addr = (base + off16) & 0xFFFF;
                break;
            }
            default:
                throw new IllegalStateException(
                        String.format("Indexed mode %02X non géré (postbyte=%02X)", mode, post)
                );
        }

        return addr & 0xFFFF;
    }

  
    // ====== Pile S / U ======
    private void push8S(int value) {
        S = (S - 1) & 0xFFFF;
        writeByte(S, value);
    }

    private int pop8S() {
        int v = readByte(S);
        S = (S + 1) & 0xFFFF;
        return v;
    }

    private void push16S(int value) {
        push8S((value >> 8) & 0xFF);
        push8S(value & 0xFF);
    }

    private int pop16S() {
        int low = pop8S();
        int high = pop8S();
        return ((high << 8) | low) & 0xFFFF;
    }

    private void push8U(int value) {
        U = (U - 1) & 0xFFFF;
        writeByte(U, value);
    }

    private int pop8U() {
        int v = readByte(U);
        U = (U + 1) & 0xFFFF;
        return v;
    }

    private void push16U(int value) {
        push8U((value >> 8) & 0xFF);
        push8U(value & 0xFF);
    }

    private int pop16U() {
        int low = pop8U();
        int high = pop8U();
        return ((high << 8) | low) & 0xFFFF;
    }

    // ====== Helpers flags arithmétiques / logiques ======

    // ADD 8 bits (ADDA, ADDB)
    private void updateFlagsAdd8(int a, int b, int result) {
        int r = result & 0xFF;

        setFlag(FLAG_C, result > 0xFF);
        setFlag(FLAG_Z, r == 0);
        setFlag(FLAG_N, (r & 0x80) != 0);
        setFlag(FLAG_V, ((~(a ^ b) & (a ^ r)) & 0x80) != 0);
        setFlag(FLAG_H, (((a & 0x0F) + (b & 0x0F)) & 0x10) != 0);
    }

    // SUB 8 bits (SUBA, SUBB)
    private void updateFlagsSub8(int a, int b, int result) {
        int r = result & 0xFF;
        setFlag(FLAG_C, (result & 0x100) != 0); // borrow
        setFlag(FLAG_Z, r == 0);
        setFlag(FLAG_N, (r & 0x80) != 0);
        setFlag(FLAG_V, (((a ^ b) & (a ^ r)) & 0x80) != 0);
        // H pas utilisé ici pour l’instant
    }

    // LOGIC (AND, OR, EOR)
    private void updateFlagsLogic(int r) {
        r &= 0xFF;
        setFlag(FLAG_Z, r == 0);
        setFlag(FLAG_N, (r & 0x80) != 0);
        setFlag(FLAG_V, false);
        // C et H inchangés
    }

    // INC
    private void updateFlagsInc(int r) {
        r &= 0xFF;
        setFlag(FLAG_Z, r == 0);
        setFlag(FLAG_N, (r & 0x80) != 0);
        setFlag(FLAG_V, r == 0x80); // 7F -> 80
    }

    // DEC
    private void updateFlagsDec(int r) {
        r &= 0xFF;
        setFlag(FLAG_Z, r == 0);
        setFlag(FLAG_N, (r & 0x80) != 0);
        setFlag(FLAG_V, r == 0x7F); // 80 -> 7F
    }

    // CLR
    private void updateFlagsCLR() {
        setFlag(FLAG_Z, true);
        setFlag(FLAG_N, false);
        setFlag(FLAG_V, false);
        setFlag(FLAG_C, false);
    }

    // NEG
    private void updateFlagsNEG(int value, int r) {
        r &= 0xFF;
        setFlag(FLAG_Z, r == 0);
        setFlag(FLAG_N, (r & 0x80) != 0);
        setFlag(FLAG_C, value != 0);
        setFlag(FLAG_V, value == 0x80);
    }

    // ====== Construction du tableau d'instructions ======
    private void buildInstructionTable() {
        Instruction.AddressingMode IM8  = Instruction.AddressingMode.IMMEDIATE8;
        Instruction.AddressingMode IM16 = Instruction.AddressingMode.IMMEDIATE16;
        Instruction.AddressingMode DIR  = Instruction.AddressingMode.DIRECT;
        Instruction.AddressingMode IDX  = Instruction.AddressingMode.INDEXED;
        Instruction.AddressingMode EXT  = Instruction.AddressingMode.EXTENDED;
        Instruction.AddressingMode INH  = Instruction.AddressingMode.INHERENT;

        // =========================
        // 1. LOAD / STORE
        // =========================

        // --- LDA ---
        opcodes[0x86] = new Instruction("LDA", 0x86, 2, 2, IM8,
                cpu -> {
                    int v = cpu.imm8();
                    cpu.setA(v);
                    cpu.updateNZ8(cpu.getA());
                    cpu.clearV();
                });

        opcodes[0x96] = new Instruction("LDA", 0x96, 2, 4, DIR,
                cpu -> {
                    int addr = cpu.directAddress();
                    cpu.setA(cpu.readByte(addr));
                    cpu.updateNZ8(cpu.getA());
                    cpu.clearV();
                });

        opcodes[0xA6] = new Instruction("LDA", 0xA6, 2, 4, IDX,
                cpu -> {
                    int addr = cpu.indexedAddress();
                    cpu.setA(cpu.readByte(addr));
                    cpu.updateNZ8(cpu.getA());
                    cpu.clearV();
                });

        opcodes[0xB6] = new Instruction("LDA", 0xB6, 3, 5, EXT,
                cpu -> {
                    int addr = cpu.extendedAddress();
                    cpu.setA(cpu.readByte(addr));
                    cpu.updateNZ8(cpu.getA());
                    cpu.clearV();
                });

        // --- LDB ---
        opcodes[0xC6] = new Instruction("LDB", 0xC6, 2, 2, IM8,
                cpu -> {
                    int v = cpu.imm8();
                    cpu.setB(v);
                    cpu.updateNZ8(cpu.getB());
                    cpu.clearV();
                });

        opcodes[0xD6] = new Instruction("LDB", 0xD6, 2, 4, DIR,
                cpu -> {
                    int addr = cpu.directAddress();
                    cpu.setB(cpu.readByte(addr));
                    cpu.updateNZ8(cpu.getB());
                    cpu.clearV();
                });

        opcodes[0xE6] = new Instruction("LDB", 0xE6, 2, 4, IDX,
                cpu -> {
                    int addr = cpu.indexedAddress();
                    cpu.setB(cpu.readByte(addr));
                    cpu.updateNZ8(cpu.getB());
                    cpu.clearV();
                });

        opcodes[0xF6] = new Instruction("LDB", 0xF6, 3, 5, EXT,
                cpu -> {
                    int addr = cpu.extendedAddress();
                    cpu.setB(cpu.readByte(addr));
                    cpu.updateNZ8(cpu.getB());
                    cpu.clearV();
                });

        // --- LDX ---
        opcodes[0x8E] = new Instruction("LDX", 0x8E, 3, 3, IM16,
                cpu -> {
                    int v = cpu.imm16();
                    cpu.setX(v);
                    cpu.updateNZ16(cpu.getX());
                    cpu.clearV();
                });

        opcodes[0x9E] = new Instruction("LDX", 0x9E, 2, 4, DIR,
                cpu -> {
                    int addr = cpu.directAddress();
                    cpu.setX(cpu.readWord(addr));
                    cpu.updateNZ16(cpu.getX());
                    cpu.clearV();
                });

        opcodes[0xAE] = new Instruction("LDX", 0xAE, 2, 4, IDX,
                cpu -> {
                    int addr = cpu.indexedAddress();
                    cpu.setX(cpu.readWord(addr));
                    cpu.updateNZ16(cpu.getX());
                    cpu.clearV();
                });

        opcodes[0xBE] = new Instruction("LDX", 0xBE, 3, 5, EXT,
                cpu -> {
                    int addr = cpu.extendedAddress();
                    cpu.setX(cpu.readWord(addr));
                    cpu.updateNZ16(cpu.getX());
                    cpu.clearV();
                });

        // --- LDU ---
        opcodes[0xCE] = new Instruction("LDU", 0xCE, 3, 3, IM16,
                cpu -> {
                    int v = cpu.imm16();
                    cpu.setU(v);
                    cpu.updateNZ16(cpu.getU());
                    cpu.clearV();
                });

        opcodes[0xDE] = new Instruction("LDU", 0xDE, 2, 4, DIR,
                cpu -> {
                    int addr = cpu.directAddress();
                    cpu.setU(cpu.readWord(addr));
                    cpu.updateNZ16(cpu.getU());
                    cpu.clearV();
                });

        opcodes[0xEE] = new Instruction("LDU", 0xEE, 2, 4, IDX,
                cpu -> {
                    int addr = cpu.indexedAddress();
                    cpu.setU(cpu.readWord(addr));
                    cpu.updateNZ16(cpu.getU());
                    cpu.clearV();
                });

        opcodes[0xFE] = new Instruction("LDU", 0xFE, 3, 5, EXT,
                cpu -> {
                    int addr = cpu.extendedAddress();
                    cpu.setU(cpu.readWord(addr));
                    cpu.updateNZ16(cpu.getU());
                    cpu.clearV();
                });

        // --- STA ---
        opcodes[0x97] = new Instruction("STA", 0x97, 2, 4, DIR,
                cpu -> {
                    int addr = cpu.directAddress();
                    cpu.writeByte(addr, cpu.getA());
                    cpu.updateNZ8(cpu.getA());
                    cpu.clearV();
                });

        opcodes[0xA7] = new Instruction("STA", 0xA7, 2, 4, IDX,
                cpu -> {
                    int addr = cpu.indexedAddress();
                    cpu.writeByte(addr, cpu.getA());
                    cpu.updateNZ8(cpu.getA());
                    cpu.clearV();
                });

        opcodes[0xB7] = new Instruction("STA", 0xB7, 3, 5, EXT,
                cpu -> {
                    int addr = cpu.extendedAddress();
                    cpu.writeByte(addr, cpu.getA());
                    cpu.updateNZ8(cpu.getA());
                    cpu.clearV();
                });

        // --- STB ---
        opcodes[0xD7] = new Instruction("STB", 0xD7, 2, 4, DIR,
                cpu -> {
                    int addr = cpu.directAddress();
                    cpu.writeByte(addr, cpu.getB());
                    cpu.updateNZ8(cpu.getB());
                    cpu.clearV();
                });

        opcodes[0xE7] = new Instruction("STB", 0xE7, 2, 4, IDX,
                cpu -> {
                    int addr = cpu.indexedAddress();
                    cpu.writeByte(addr, cpu.getB());
                    cpu.updateNZ8(cpu.getB());
                    cpu.clearV();
                });

        opcodes[0xF7] = new Instruction("STB", 0xF7, 3, 5, EXT,
                cpu -> {
                    int addr = cpu.extendedAddress();
                    cpu.writeByte(addr, cpu.getB());
                    cpu.updateNZ8(cpu.getB());
                    cpu.clearV();
                });

        // --- STX ---
        opcodes[0x9F] = new Instruction("STX", 0x9F, 2, 4, DIR,
                cpu -> {
                    int addr = cpu.directAddress();
                    cpu.writeWord(addr, cpu.getX());
                    cpu.updateNZ16(cpu.getX());
                    cpu.clearV();
                });

        opcodes[0xAF] = new Instruction("STX", 0xAF, 2, 4, IDX,
                cpu -> {
                    int addr = cpu.indexedAddress();
                    cpu.writeWord(addr, cpu.getX());
                    cpu.updateNZ16(cpu.getX());
                    cpu.clearV();
                });

        opcodes[0xBF] = new Instruction("STX", 0xBF, 3, 5, EXT,
                cpu -> {
                    int addr = cpu.extendedAddress();
                    cpu.writeWord(addr, cpu.getX());
                    cpu.updateNZ16(cpu.getX());
                    cpu.clearV();
                });

        // --- STU ---
        opcodes[0xDF] = new Instruction("STU", 0xDF, 2, 4, DIR,
                cpu -> {
                    int addr = cpu.directAddress();
                    cpu.writeWord(addr, cpu.getU());
                    cpu.updateNZ16(cpu.getU());
                    cpu.clearV();
                });

        opcodes[0xEF] = new Instruction("STU", 0xEF, 2, 4, IDX,
                cpu -> {
                    int addr = cpu.indexedAddress();
                    cpu.writeWord(addr, cpu.getU());
                    cpu.updateNZ16(cpu.getU());
                    cpu.clearV();
                });

        opcodes[0xFF] = new Instruction("STU", 0xFF, 3, 5, EXT,
                cpu -> {
                    int addr = cpu.extendedAddress();
                    cpu.writeWord(addr, cpu.getU());
                    cpu.updateNZ16(cpu.getU());
                    cpu.clearV();
                });

        // =========================
        // 2. ARITHMETIC
        // =========================

        // --- ADDA ---
        opcodes[0x8B] = new Instruction("ADDA", 0x8B, 2, 2, IM8,
                cpu -> {
                    int a = cpu.getA();
                    int v = cpu.imm8();
                    int res = a + v;
                    cpu.setA(res);
                    cpu.updateFlagsAdd8(a, v, res);
                });

        opcodes[0x9B] = new Instruction("ADDA", 0x9B, 2, 4, DIR,
                cpu -> {
                    int a = cpu.getA();
                    int addr = cpu.directAddress();
                    int v = cpu.readByte(addr);
                    int res = a + v;
                    cpu.setA(res);
                    cpu.updateFlagsAdd8(a, v, res);
                });

        opcodes[0xAB] = new Instruction("ADDA", 0xAB, 2, 4, IDX,
                cpu -> {
                    int a = cpu.getA();
                    int addr = cpu.indexedAddress();
                    int v = cpu.readByte(addr);
                    int res = a + v;
                    cpu.setA(res);
                    cpu.updateFlagsAdd8(a, v, res);
                });

        opcodes[0xBB] = new Instruction("ADDA", 0xBB, 3, 5, EXT,
                cpu -> {
                    int a = cpu.getA();
                    int addr = cpu.extendedAddress();
                    int v = cpu.readByte(addr);
                    int res = a + v;
                    cpu.setA(res);
                    cpu.updateFlagsAdd8(a, v, res);
                });

        // --- ADDB ---
        opcodes[0xCB] = new Instruction("ADDB", 0xCB, 2, 2, IM8,
                cpu -> {
                    int b = cpu.getB();
                    int v = cpu.imm8();
                    int res = b + v;
                    cpu.setB(res);
                    cpu.updateFlagsAdd8(b, v, res);
                });

        opcodes[0xDB] = new Instruction("ADDB", 0xDB, 2, 4, DIR,
                cpu -> {
                    int b = cpu.getB();
                    int addr = cpu.directAddress();
                    int v = cpu.readByte(addr);
                    int res = b + v;
                    cpu.setB(res);
                    cpu.updateFlagsAdd8(b, v, res);
                });

        opcodes[0xEB] = new Instruction("ADDB", 0xEB, 2, 4, IDX,
                cpu -> {
                    int b = cpu.getB();
                    int addr = cpu.indexedAddress();
                    int v = cpu.readByte(addr);
                    int res = b + v;
                    cpu.setB(res);
                    cpu.updateFlagsAdd8(b, v, res);
                });

        opcodes[0xFB] = new Instruction("ADDB", 0xFB, 3, 5, EXT,
                cpu -> {
                    int b = cpu.getB();
                    int addr = cpu.extendedAddress();
                    int v = cpu.readByte(addr);
                    int res = b + v;
                    cpu.setB(res);
                    cpu.updateFlagsAdd8(b, v, res);
                });

        // --- SUBA ---
        opcodes[0x80] = new Instruction("SUBA", 0x80, 2, 2, IM8,
                cpu -> {
                    int a = cpu.getA();
                    int v = cpu.imm8();
                    int res = a - v;
                    cpu.setA(res);
                    cpu.updateFlagsSub8(a, v, res);
                });
        opcodes[0x90] = new Instruction("SUBA", 0x90, 2, 4, DIR,
        	    cpu -> {
        	        int a = cpu.getA();
        	        int addr = cpu.directAddress();
        	        int v = cpu.readByte(addr);
        	        int res = a - v;
        	        cpu.setA(res);
        	        cpu.updateFlagsSub8(a, v, res);
        	    });
        opcodes[0xA0] = new Instruction("SUBA", 0xA0, 2, 4, IDX,
        	    cpu -> {
        	        int a = cpu.getA();
        	        int addr = cpu.indexedAddress();
        	        int v = cpu.readByte(addr);
        	        int res = a - v;
        	        cpu.setA(res);
        	        cpu.updateFlagsSub8(a, v, res);
        	    });
        opcodes[0xB0] = new Instruction("SUBA", 0xB0, 3, 5, EXT,
        	    cpu -> {
        	        int a = cpu.getA();
        	        int addr = cpu.extendedAddress();
        	        int v = cpu.readByte(addr);
        	        int res = a - v;
        	        cpu.setA(res);
        	        cpu.updateFlagsSub8(a, v, res);
        	    });




        // --- SUBB ---
        opcodes[0xC0] = new Instruction("SUBB", 0xC0, 2, 2, IM8,
                cpu -> {
                    int b = cpu.getB();
                    int v = cpu.imm8();
                    int res = b - v;
                    cpu.setB(res);
                    cpu.updateFlagsSub8(b, v, res);
                });
        opcodes[0xD0] = new Instruction("SUBB", 0xD0, 2, 4, DIR,
        	    cpu -> {
        	        int b = cpu.getB();
        	        int addr = cpu.directAddress();
        	        int v = cpu.readByte(addr);
        	        int res = b - v;
        	        cpu.setB(res);
        	        cpu.updateFlagsSub8(b, v, res);
        	    });
        opcodes[0xE0] = new Instruction("SUBB", 0xE0, 2, 4, IDX,
        	    cpu -> {
        	        int b = cpu.getB();
        	        int addr = cpu.indexedAddress();
        	        int v = cpu.readByte(addr);
        	        int res = b - v;
        	        cpu.setB(res);
        	        cpu.updateFlagsSub8(b, v, res);
        	    });
        opcodes[0xF0] = new Instruction("SUBB", 0xF0, 3, 5, EXT,
        	    cpu -> {
        	        int b = cpu.getB();
        	        int addr = cpu.extendedAddress();
        	        int v = cpu.readByte(addr);
        	        int res = b - v;
        	        cpu.setB(res);
        	        cpu.updateFlagsSub8(b, v, res);
        	    });

        // --- INC / DEC / CLR / NEG (dir) ---
        opcodes[0x0C] = new Instruction("INC", 0x0C, 2, 6, DIR,
                cpu -> {
                    int addr = cpu.directAddress();
                    int v = cpu.readByte(addr);
                    int r = (v + 1) & 0xFF;
                    cpu.writeByte(addr, r);
                    cpu.updateFlagsInc(r);
                });
//DEC
        opcodes[0x0A] = new Instruction("DEC", 0x0A, 2, 6, DIR,
                cpu -> {
                    int addr = cpu.directAddress();
                    int v = cpu.readByte(addr);
                    int r = (v - 1) & 0xFF;
                    cpu.writeByte(addr, r);
                    cpu.updateFlagsDec(r);
                });
        opcodes[0x6A] = new Instruction("DEC", 0x6A, 2, 6, IDX,
        	    cpu -> {
        	        int addr = cpu.indexedAddress();
        	        int v = cpu.readByte(addr);
        	        int r = (v - 1) & 0xFF;
        	        cpu.writeByte(addr, r);
        	        cpu.updateFlagsDec(r);
        	    });
        opcodes[0x7A] = new Instruction("DEC", 0x7A, 3, 7, EXT,
        	    cpu -> {
        	        int addr = cpu.extendedAddress();
        	        int v = cpu.readByte(addr);
        	        int r = (v - 1) & 0xFF;
        	        cpu.writeByte(addr, r);
        	        cpu.updateFlagsDec(r);
        	    });
//CLR


        opcodes[0x0F] = new Instruction("CLR", 0x0F, 2, 6, DIR,
                cpu -> {
                    int addr = cpu.directAddress();
                    cpu.writeByte(addr, 0);
                    cpu.updateFlagsCLR();
                });
        opcodes[0x6F] = new Instruction("CLR", 0x6F, 2, 6, IDX,
        	    cpu -> {
        	        int addr = cpu.indexedAddress();
        	        cpu.writeByte(addr, 0);
        	        cpu.updateFlagsCLR();
        	    });
        opcodes[0x7F] = new Instruction("CLR", 0x7F, 3, 7, EXT,
        	    cpu -> {
        	        int addr = cpu.extendedAddress();
        	        cpu.writeByte(addr, 0);
        	        cpu.updateFlagsCLR();
        	    });
// NEG
        opcodes[0x00] = new Instruction("NEG", 0x00, 2, 6, DIR,
                cpu -> {
                    int addr = cpu.directAddress();
                    int v = cpu.readByte(addr);
                    int r = (-v) & 0xFF;
                    cpu.writeByte(addr, r);
                    cpu.updateFlagsNEG(v, r);
                });
        opcodes[0x60] = new Instruction("NEG", 0x60, 2, 6, IDX,
        	    cpu -> {
        	        int addr = cpu.indexedAddress();
        	        int v = cpu.readByte(addr);
        	        int r = (-v) & 0xFF;
        	        cpu.writeByte(addr, r);
        	        cpu.updateFlagsNEG(v, r);
        	    });
        opcodes[0x70] = new Instruction("NEG", 0x70, 3, 7, EXT,
        	    cpu -> {
        	        int addr = cpu.extendedAddress();
        	        int v = cpu.readByte(addr);
        	        int r = (-v) & 0xFF;
        	        cpu.writeByte(addr, r);
        	        cpu.updateFlagsNEG(v, r);
        	    });


        // =========================
        // 3. LOGIC
        // =========================

        // --- ANDA ---
        opcodes[0x84] = new Instruction("ANDA", 0x84, 2, 2, IM8,
                cpu -> {
                    int v = cpu.imm8();
                    int r = cpu.getA() & v;
                    cpu.setA(r);
                    cpu.updateFlagsLogic(r);
                });

        opcodes[0x94] = new Instruction("ANDA", 0x94, 2, 4, DIR,
                cpu -> {
                    int addr = cpu.directAddress();
                    int v = cpu.readByte(addr);
                    int r = cpu.getA() & v;
                    cpu.setA(r);
                    cpu.updateFlagsLogic(r);
                });
        opcodes[0xA4] = new Instruction("ANDA", 0xA4, 2, 4, IDX,
        	    cpu -> {
        	        int addr = cpu.indexedAddress();
        	        int v = cpu.readByte(addr);
        	        int r = cpu.getA() & v;
        	        cpu.setA(r);
        	        cpu.updateFlagsLogic(r);
        	    });
        opcodes[0xB4] = new Instruction("ANDA", 0xB4, 3, 5, EXT,
        	    cpu -> {
        	        int addr = cpu.extendedAddress();
        	        int v = cpu.readByte(addr);
        	        int r = cpu.getA() & v;
        	        cpu.setA(r);
        	        cpu.updateFlagsLogic(r);
        	    });
    

        // --- ANDB ---
        opcodes[0xC4] = new Instruction("ANDB", 0xC4, 2, 2, IM8,
                cpu -> {
                    int v = cpu.imm8();
                    int r = cpu.getB() & v;
                    cpu.setB(r);
                    cpu.updateFlagsLogic(r);
                });
        opcodes[0xD4] = new Instruction("ANDB", 0xD4, 2, 4, DIR,
        	    cpu -> {
        	        int addr = cpu.directAddress();
        	        int v = cpu.readByte(addr);
        	        int r = cpu.getB() & v;
        	        cpu.setB(r);
        	        cpu.updateFlagsLogic(r);
        	    });
        opcodes[0xE4] = new Instruction("ANDB", 0xE4, 2, 4, IDX,
        	    cpu -> {
        	        int addr = cpu.indexedAddress();
        	        int v = cpu.readByte(addr);
        	        int r = cpu.getB() & v;
        	        cpu.setB(r);
        	        cpu.updateFlagsLogic(r);
        	    });
        opcodes[0xF4] = new Instruction("ANDB", 0xF4, 3, 5, EXT,
        	    cpu -> {
        	        int addr = cpu.extendedAddress();
        	        int v = cpu.readByte(addr);
        	        int r = cpu.getB() & v;
        	        cpu.setB(r);
        	        cpu.updateFlagsLogic(r);
        	    });


        // --- ORA ---
        opcodes[0x8A] = new Instruction("ORA", 0x8A, 2, 2, IM8,
                cpu -> {
                    int v = cpu.imm8();
                    int r = cpu.getA() | v;
                    cpu.setA(r);
                    cpu.updateFlagsLogic(r);
                });
        opcodes[0x9A] = new Instruction("ORA", 0x9A, 2, 4, DIR,
        	    cpu -> {
        	        int addr = cpu.directAddress();
        	        int v = cpu.readByte(addr);
        	        int r = cpu.getA() | v;
        	        cpu.setA(r);
        	        cpu.updateFlagsLogic(r);
        	    });

        opcodes[0xAA] = new Instruction("ORA", 0xAA, 2, 4, IDX,
        	    cpu -> {
        	        int addr = cpu.indexedAddress();
        	        int v = cpu.readByte(addr);
        	        int r = cpu.getA() | v;
        	        cpu.setA(r);
        	        cpu.updateFlagsLogic(r);
        	    });

        // --- ORB ---
        opcodes[0xCA] = new Instruction("ORB", 0xCA, 2, 2, IM8,
                cpu -> {
                    int v = cpu.imm8();
                    int r = cpu.getB() | v;
                    cpu.setB(r);
                    cpu.updateFlagsLogic(r);
                });
        opcodes[0xDA] = new Instruction("ORB", 0xDA, 2, 4, DIR,
        	    cpu -> {
        	        int addr = cpu.directAddress();
        	        int v = cpu.readByte(addr);
        	        int r = cpu.getB() | v;
        	        cpu.setB(r);
        	        cpu.updateFlagsLogic(r);
        	    });
        opcodes[0xEA] = new Instruction("ORB", 0xEA, 2, 4, IDX,
        	    cpu -> {
        	        int addr = cpu.indexedAddress();
        	        int v = cpu.readByte(addr);
        	        int r = cpu.getB() | v;
        	        cpu.setB(r);
        	        cpu.updateFlagsLogic(r);
        	    });
        opcodes[0xFA] = new Instruction("ORB", 0xFA, 3, 5, EXT,
        	    cpu -> {
        	        int addr = cpu.extendedAddress();
        	        int v = cpu.readByte(addr);
        	        int r = cpu.getB() | v;
        	        cpu.setB(r);
        	        cpu.updateFlagsLogic(r);
        	    });

        // --- EORA ---
        opcodes[0x88] = new Instruction("EORA", 0x88, 2, 2, IM8,
                cpu -> {
                    int v = cpu.imm8();
                    int r = cpu.getA() ^ v;
                    cpu.setA(r);
                    cpu.updateFlagsLogic(r);
                });

        // --- EORB ---
        opcodes[0xC8] = new Instruction("EORB", 0xC8, 2, 2, IM8,
                cpu -> {
                    int v = cpu.imm8();
                    int r = cpu.getB() ^ v;
                    cpu.setB(r);
                    cpu.updateFlagsLogic(r);
                });
        opcodes[0xD8] = new Instruction("EORB", 0xD8, 2, 4, DIR,
        	    cpu -> {
        	        int addr = cpu.directAddress();
        	        int v = cpu.readByte(addr);
        	        int r = cpu.getB() ^ v;
        	        cpu.setB(r);
        	        cpu.updateFlagsLogic(r);
        	    });
        opcodes[0xE8] = new Instruction("EORB", 0xE8, 2, 4, IDX,
        	    cpu -> {
        	        int addr = cpu.indexedAddress();
        	        int v = cpu.readByte(addr);
        	        int r = cpu.getB() ^ v;
        	        cpu.setB(r);
        	        cpu.updateFlagsLogic(r);
        	    });
        opcodes[0xF8] = new Instruction("EORB", 0xF8, 3, 5, EXT,
        	    cpu -> {
        	        int addr = cpu.extendedAddress();
        	        int v = cpu.readByte(addr);
        	        int r = cpu.getB() ^ v;
        	        cpu.setB(r);
        	        cpu.updateFlagsLogic(r);
        	    });

        // =========================
        // 4. BRANCH / JMP / CALL
        // =========================

      
        opcodes[0x7E] = new Instruction("JMP", 0x7E, 3, 3, EXT,
                cpu -> {
                    int addr = cpu.extendedAddress();
                    cpu.setPC(addr);
                });
        opcodes[0x0E] = new Instruction("JMP", 0x0E, 2, 3, DIR,
        	    cpu -> {
        	        int addr = cpu.directAddress();
        	        cpu.setPC(addr);
        	    });
        opcodes[0x6E] = new Instruction("JMP", 0x6E, 2, 3, IDX,
        	    cpu -> {
        	        int addr = cpu.indexedAddress();
        	        cpu.setPC(addr);
        	    });
//JSR
        opcodes[0xBD] = new Instruction("JSR", 0xBD, 3, 7, EXT,
                cpu -> {
                    int addr = cpu.extendedAddress();
                    cpu.push16S(cpu.getPC());
                    cpu.setPC(addr);
                });
        opcodes[0x9D] = new Instruction("JSR", 0x9D, 2, 7, DIR,
        	    cpu -> {
        	        int addr = cpu.directAddress();
        	        cpu.push16S(cpu.getPC());
        	        cpu.setPC(addr);
        	    });
        opcodes[0xAD] = new Instruction("JSR", 0xAD, 2, 7, IDX,
        	    cpu -> {
        	        int addr = cpu.indexedAddress();
        	        cpu.push16S(cpu.getPC());
        	        cpu.setPC(addr);
        	    });
//RTS
      
        opcodes[0x39] = new Instruction("RTS", 0x39, 1, 5, INH,
                cpu -> {
                    int addr = cpu.pop16S();
                    cpu.setPC(addr);
                });
   

        // =========================
        // 5. STACK OPERATIONS
        // =========================

        // PSHS
        opcodes[0x34] = new Instruction("PSHS", 0x34, 2, 5, IM8,
                cpu -> {
                    int mask = cpu.imm8();
                    if ((mask & 0x80) != 0) cpu.push16S(cpu.getPC());
                    if ((mask & 0x40) != 0) cpu.push16S(cpu.getU());
                    if ((mask & 0x20) != 0) cpu.push16S(cpu.getY());
                    if ((mask & 0x10) != 0) cpu.push16S(cpu.getX());
                    if ((mask & 0x08) != 0) cpu.push8S(cpu.getDP());
                    if ((mask & 0x04) != 0) cpu.push8S(cpu.getB());
                    if ((mask & 0x02) != 0) cpu.push8S(cpu.getA());
                    if ((mask & 0x01) != 0) cpu.push8S(cpu.getCCR());
                });

        // PULS
        opcodes[0x35] = new Instruction("PULS", 0x35, 2, 5, IM8,
                cpu -> {
                    int mask = cpu.imm8();
                    if ((mask & 0x01) != 0) cpu.setCCR(cpu.pop8S());
                    if ((mask & 0x02) != 0) cpu.setA(cpu.pop8S());
                    if ((mask & 0x04) != 0) cpu.setB(cpu.pop8S());
                    if ((mask & 0x08) != 0) cpu.setDP(cpu.pop8S());
                    if ((mask & 0x10) != 0) cpu.setX(cpu.pop16S());
                    if ((mask & 0x20) != 0) cpu.setY(cpu.pop16S());
                    if ((mask & 0x40) != 0) cpu.setU(cpu.pop16S());
                    if ((mask & 0x80) != 0) cpu.setPC(cpu.pop16S());
                });

        // PSHU
        opcodes[0x36] = new Instruction("PSHU", 0x36, 2, 5, IM8,
                cpu -> {
                    int mask = cpu.imm8();
                    if ((mask & 0x80) != 0) cpu.push16U(cpu.getPC());
                    if ((mask & 0x40) != 0) cpu.push16U(cpu.getS());
                    if ((mask & 0x20) != 0) cpu.push16U(cpu.getY());
                    if ((mask & 0x10) != 0) cpu.push16U(cpu.getX());
                    if ((mask & 0x08) != 0) cpu.push8U(cpu.getDP());
                    if ((mask & 0x04) != 0) cpu.push8U(cpu.getB());
                    if ((mask & 0x02) != 0) cpu.push8U(cpu.getA());
                    if ((mask & 0x01) != 0) cpu.push8U(cpu.getCCR());
                });

        // PULU
        opcodes[0x37] = new Instruction("PULU", 0x37, 2, 5, IM8,
                cpu -> {
                    int mask = cpu.imm8();
                    if ((mask & 0x01) != 0) cpu.setCCR(cpu.pop8U());
                    if ((mask & 0x02) != 0) cpu.setA(cpu.pop8U());
                    if ((mask & 0x04) != 0) cpu.setB(cpu.pop8U());
                    if ((mask & 0x08) != 0) cpu.setDP(cpu.pop8U());
                    if ((mask & 0x10) != 0) cpu.setX(cpu.pop16U());
                    if ((mask & 0x20) != 0) cpu.setY(cpu.pop16U());
                    if ((mask & 0x40) != 0) cpu.setS(cpu.pop16U());
                    if ((mask & 0x80) != 0) cpu.setPC(cpu.pop16U());
                });

        // =========================
        // 6. OTHER
        // =========================

        opcodes[0x12] = new Instruction("NOP", 0x12, 1, 2, INH,
                cpu -> { /* ne fait rien */ });
    }

    // ====== Exécution d'une instruction ======

    public  void step() {
        int opcode = fetch8();
        Instruction instr = opcodes[opcode & 0xFF];

        if (instr == null) {
            throw new IllegalStateException(
                    String.format("Opcode 0x%02X non implémenté à PC=0x%04X", opcode, (PC - 1) & 0xFFFF)
            );
        }

        instr.execute(this);
        cycles += instr.getCycles();
    }
    /*  <<< PLACE LE ICI >>>  */
    public boolean stepWithDebugger(Debugger dbg) {

        int currentPC = getPC();

        // Breakpoint ?
        if (dbg != null && dbg.isBreakpoint(currentPC)) {
            System.out.println(String.format("Arrêt sur breakpoint @ %04X", currentPC));
            dbg.dumpRegisters();
            return false; // STOP execution
        }

        int opcode = fetch8();
        Instruction instr = opcodes[opcode];

        if (instr == null) {
            throw new IllegalStateException(
                String.format("Opcode %02X non implémenté @ PC=%04X",
                        opcode, (currentPC & 0xFFFF))
            );
        }

        // Log simple vers la console
        System.out.println(String.format("EXEC @ %04X : %s", 
                 currentPC, instr.getMnemonic()));

        // Exécuter l'instruction
        instr.execute(this);
        cycles += instr.getCycles();

        // Montrer les registres après exécution
        if (dbg != null) dbg.dumpRegisters();

        return true; // continue execution
    }


}
