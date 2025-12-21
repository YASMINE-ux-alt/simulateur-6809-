package memory;

import java.util.Arrays;

public class Memory {

    private final byte[] mem = new byte[65536]; 

    //Interfaces pour E/S mappées
    public interface ConsoleOutListener {
        void onConsoleOut(byte b);
    }

    public interface ConsoleInProvider {
        byte readConsoleIn();
    }

    private ConsoleOutListener consoleOutListener;
    private ConsoleInProvider consoleInProvider;

    public void setConsoleOutListener(ConsoleOutListener l) {
        this.consoleOutListener = l;
    }

    public void setConsoleInProvider(ConsoleInProvider p) {
        this.consoleInProvider = p;
    }

    // LECTURE / ÉCRITURE 
    public int readByte(int addr) {
        addr &= 0xFFFF;

        // Port d entrée console FF01
        if (addr == 0xFF01 && consoleInProvider != null) {
            return consoleInProvider.readConsoleIn() & 0xFF;
        }

        return mem[addr] & 0xFF;
    }

    public void writeByte(int addr, int value) {
        addr &= 0xFFFF;
        value &= 0xFF;

        // Port de sortie console FF00
        if (addr == 0xFF00 && consoleOutListener != null) {
            consoleOutListener.onConsoleOut((byte) value);
        }

        // on laisse la valeur dans la memoire
        mem[addr] = (byte) value;
    }

    // LECTURE / ÉCRITURE 
    
    public int readWord(int addr) {
        addr &= 0xFFFF;

        int high = readByte(addr);
        int low  = readByte(addr + 1);

        return ((high << 8) | low) & 0xFFFF;
    }

    public void writeWord(int addr, int value) {
        addr &= 0xFFFF;

        writeByte(addr, (value >> 8) & 0xFF);     // octet haut
        writeByte(addr + 1, value & 0xFF);        // octet bas
    }

    
    // CHARGER UN PROGRAMME
   
    public void loadProgram(byte[] program, int startAddress) {
        startAddress &= 0xFFFF;

        if (startAddress + program.length > 65536) {
            throw new IllegalArgumentException(
                    "Programme trop long, dépassement mémoire.");
        }

        System.arraycopy(program, 0, mem, startAddress, program.length);
    }

    
    // EFFACER LA MEMOIRE COMPLETE
    
    public void clearMemory() {
        Arrays.fill(mem, (byte) 0);
    }

    
    // DUMP MEMOIRE 
    public String memoryDump(int start, int length) {
        start &= 0xFFFF;
        int end = Math.min(start + length, 65536);

        StringBuilder sb = new StringBuilder();

        for (int addr = start; addr < end; addr += 16) {
            sb.append(String.format("%04X : ", addr));

            // octets
            for (int i = 0; i < 16 && addr + i < end; i++) {
                sb.append(String.format("%02X ", mem[addr + i] & 0xFF));
            }

            sb.append(" | ");

            // caracteres
            for (int i = 0; i < 16 && addr + i < end; i++) {
                char c = (char) (mem[addr + i] & 0xFF);
                sb.append((c >= 32 && c <= 126) ? c : '.');
            }

            sb.append("\n");
        }

        return sb.toString();
    }

   
    // LECTURE multiple 
    public int[] readBytes(int start, int length) {
        start &= 0xFFFF;
        length = Math.min(length, 65536 - start);

        int[] data = new int[length];

        for (int i = 0; i < length; i++) {
            data[i] = readByte(start + i);
        }

        return data;
    }
}

