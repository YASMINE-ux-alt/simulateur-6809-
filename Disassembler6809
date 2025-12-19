package assembler;

import cpu.CPU6809;
import cpu.Instruction;
import memory.Memory;

public class Disassembler6809 {

    private final CPU6809 cpu;
    private final Memory mem;

    public Disassembler6809(CPU6809 cpu, Memory mem) {
        this.cpu = cpu;
        this.mem = mem;
    }
    public String disassemble(int start, int lines) {
        StringBuilder sb = new StringBuilder();
        int pc = start;

        for (int i = 0; i < lines; i++) {
            int opcode = mem.readByte(pc);
            Instruction instr = cpu.getInstruction(opcode);

            if (instr == null) {
                sb.append(String.format("%04X: %02X        ???\n", pc, opcode));
                pc++;
                continue;
            }

            String operand = "";
            int size = instr.getSize();

            switch (instr.getMode()) {
                case IMMEDIATE8: {
                    int v = mem.readByte(pc + 1);
                    operand = String.format("#$%02X", v);
                    break;
                }
                case IMMEDIATE16: {
                    int hi = mem.readByte(pc + 1);
                    int lo = mem.readByte(pc + 2);
                    operand = String.format("#$%04X", (hi << 8) | lo);
                    break;
                }
                case DIRECT: {
                    int addr = mem.readByte(pc + 1);
                    operand = String.format("$%02X", addr);
                    break;
                }
                case EXTENDED: {
                    int hi = mem.readByte(pc + 1);
                    int lo = mem.readByte(pc + 2);
                    operand = String.format("$%04X", (hi << 8) | lo);
                    break;
                }
                case INDEXED: {
                    operand = ",X"; // version simple (on améliorera après)
                    break;
                }
                case INHERENT:
                default:
                    operand = "";
            }

            sb.append(String.format(
                "%04X: ",
                pc
            ));

            // Affichage des octets machine
            for (int b = 0; b < size; b++) {
                sb.append(String.format("%02X ", mem.readByte(pc + b)));
            }

            sb.append(String.format(
                "  %-6s %s\n",
                instr.getMnemonic(),
                operand
            ));

            pc += size;
        }

        return sb.toString();
    }
    


}

