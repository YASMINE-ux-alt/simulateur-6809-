package memory;
import java.util.Arrays;


public class Memory {

    private final byte[] mem = new byte[65536]; // 64 Ko mémoire

    // Lectu£re d'un octet
    public int readByte(int addr) {
        addr &= 0xFFFF; // Masque pour rester dans 0x0000-0xFFFF
        return mem[addr] & 0xFF;
    }

    // Écriture d'un octet
    public void writeByte(int addr, int value) {
        addr &= 0xFFFF;
        mem[addr] = (byte)(value & 0xFF);
    }

    // Lecture d'un mot 16 bits (Big Endian)
    public int readWord(int addr) {
        addr &= 0xFFFF;
        int high = readByte(addr);
        int low = readByte(addr + 1);
        return (high << 8) | low;
    }
    
    

    // Écriture d'un mot 16 bits (Big Endian)
    public void writeWord(int addr, int value) {
        addr &= 0xFFFF;
        writeByte(addr, (value >> 8) & 0xFF); // octet haut
        writeByte(addr + 1, value & 0xFF);    // octet bas
    }

    // Charger un programme à une adresse spécifique
    public void loadProgram(byte[] program, int startAddress) {
        if (startAddress < 0 || startAddress + program.length > 65536) {
            throw new IllegalArgumentException("Adresse hors limites de la mémoire");
        }
        System.arraycopy(program, 0, mem, startAddress, program.length);
    }

    // Réinitialiser toute la mémoire à zéro
    public void clearMemory() {
        Arrays.fill(mem, (byte)0);
    }

    // Dump mémoire pour visualisation (utile pour le débogueur)
    public String memoryDump(int start, int length) {
        start &= 0xFFFF;
        int end = Math.min(start + length, 65536);
        StringBuilder sb = new StringBuilder();

        for (int addr = start; addr < end; addr += 16) {
            sb.append(String.format("%04X : ", addr));
            for (int i = 0; i < 16 && addr + i < end; i++) {
                sb.append(String.format("%02X ", mem[addr + i]));
            }
            sb.append(" | ");
            for (int i = 0; i < 16 && addr + i < end; i++) {
                char c = (char)(mem[addr + i] & 0xFF);
                sb.append((c >= 32 && c <= 126) ? c : '.');
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    // Lecture multiple octets pour le débogueur
    public int[] readBytes(int start, int length) {
        start &= 0xFFFF;
        length = Math.min(length, 65536 - start);
        int[] data = new int[length];
        for (int i = 0; i < length; i++) {
            data[i] = readByte(start + i);
        }
        return data;
    }
 // Listener pour sortie console (écrit par le CPU à FF00)
    public interface ConsoleOutListener {
        void onConsoleOut(byte b);
    }

    // Provider pour entrée console (lu par le CPU à FF01)
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

}


