package assembler;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;

import memory.Memory;

public class Assembler {
    public void assembleAndLoad(String src, Memory memory) {
    	
    int addr = 0xFC00;       // Début ROM (comme ton CPU)
    String[] lines = src.split("\n");

    // Effacer la ROM avant de charger
    for (int i = 0xFC00; i <= 0xFFFF; i++)
        memory.writeByte(i, 0x00);

    // Helper lambdas (Java 8-compatible)
    AtomicInteger cursor = new AtomicInteger(0xFC00);

 // writeByte
 IntConsumer writeByte = b ->
         memory.writeByte(cursor.getAndIncrement(), b & 0xFF);

 // writeWord
 IntConsumer writeWord = w -> {
     int pos = cursor.getAndIncrement();
     memory.writeByte(pos, (w >> 8) & 0xFF);
     memory.writeByte(cursor.getAndIncrement(), w & 0xFF);
 };
 
 
 



    // utilitaires internes
    java.util.function.Function<String, Integer> parseNumber = (s) -> {
        s = s.trim();
        if (s.startsWith("$")) {
            return Integer.parseInt(s.substring(1), 16);
        } else if (s.startsWith("0x") || s.startsWith("0X")) {
            return Integer.parseInt(s.substring(2), 16);
        } else {
            return Integer.parseInt(s, 10);
        }
    };

    for (String rawLine : lines) {
        String line = rawLine.trim();

        // ignorer vide ou commentaires
        if (line.isEmpty() || line.startsWith(";"))
            continue;

        // séparer mnemonic + operande (si existante)
        String mnemonic;
        String operand = null;
        int sp = line.indexOf(' ');
        if (sp >= 0) {
            mnemonic = line.substring(0, sp).trim().toUpperCase();
            operand = line.substring(sp + 1).trim();
        } else {
            mnemonic = line.toUpperCase();
        }

        try {
            // ----- INHERENT / NOP / RTS -----
            if (mnemonic.equals("NOP")) {
                writeByte.accept(0x12);
                continue;
            }
            if (mnemonic.equals("RTS")) {
                writeByte.accept(0x39);
                continue;
            }

            // ----- Helpers pour détecter mode d'adressage -----
            boolean isImmediate = operand != null && operand.startsWith("#");

            boolean isIndexed = operand != null && operand.matches("^.*?,\\s*[xXyY]$");

            
            boolean isExtended = operand != null && !isImmediate && !isIndexed &&
                                 parseNumber.apply(operand) > 0xFF;

            boolean isDirect = operand != null && !isImmediate && !isIndexed &&
                               !isExtended && parseNumber.apply(operand) <= 0xFF;
   
            // utilitaire pour écrire indexed postbyte + offset si nécessaire
            java.util.function.Consumer<String> emitIndexed = (opnd) -> {
                // opnd example: ",X" or "5,X" or "$05,X" or "$1234,X"
                String o = opnd.trim();
                boolean useY = o.endsWith(",Y") || o.endsWith(",y");
                // enlever suffixe ,X / ,Y
                String before = o.substring(0, o.length() - 2).trim();
                // si nothing before (i.e. ",X")
                if (before.isEmpty()) {
                    int post = 0x00 | (useY ? 0x20 : 0x00);
                    writeByte.accept(post);
                } else {
                    // parse offset (can be hex $nn ou decimal)
                    int val = parseNumber.apply(before);
                    if (val >= -128 && val <= 127) {
                        int off8 = val & 0xFF;
                        int post = 0x08 | (useY ? 0x20 : 0x00);
                        writeByte.accept(post);
                        writeByte.accept(off8);
                    } else {
                        // offset16
                        int off16 = val & 0xFFFF;
                        int post = 0x09 | (useY ? 0x20 : 0x00);
                        writeByte.accept(post);
                        writeByte.accept((off16 >> 8) & 0xFF);
                        writeByte.accept(off16 & 0xFF);
                    }
                }
            };

            // utilitaire pour déterminer valeur immédiate (sans '#')
            java.util.function.Function<String, Integer> immValue = (opnd) -> {
                String x = opnd.substring(1).trim(); // enlever '#'
                return parseNumber.apply(x);
            };

            // utilitaire pour écrire direct (1 octet) ou extended (2 octets) adresse
            java.util.function.Function<String, Integer> asNumber = (opnd) -> parseNumber.apply(opnd);

            // ----- SWITCH sur mnemonique -----
            switch (mnemonic) {
                // ----------------------
                // LOADS  (LDA, LDB, LDX, LDU)
                // ----------------------
                case "LDA":
                    if (isImmediate) {
                        writeByte.accept(0x86);
                        writeByte.accept(immValue.apply(operand) & 0xFF);
                    } else if (isDirect) {
                        writeByte.accept(0x96);
                        writeByte.accept(asNumber.apply(operand) & 0xFF);
                    } else if (isIndexed) {
                        writeByte.accept(0xA6);
                        emitIndexed.accept(operand);
                    } else { // extended
                        writeByte.accept(0xB6);
                        int v = asNumber.apply(operand) & 0xFFFF;
                        writeByte.accept((v >> 8) & 0xFF);
                        writeByte.accept(v & 0xFF);
                    }
                    continue;

                case "LDB":
                    if (isImmediate) {
                        writeByte.accept(0xC6);
                        writeByte.accept(immValue.apply(operand) & 0xFF);
                    } else if (isDirect) {
                        writeByte.accept(0xD6);
                        writeByte.accept(asNumber.apply(operand) & 0xFF);
                    } else if (isIndexed) {
                        writeByte.accept(0xE6);
                        emitIndexed.accept(operand);
                    } else {
                        writeByte.accept(0xF6);
                        int v = asNumber.apply(operand) & 0xFFFF;
                        writeByte.accept((v >> 8) & 0xFF);
                        writeByte.accept(v & 0xFF);
                    }
                    continue;

                case "LDX":
                    if (isImmediate) {
                        writeByte.accept(0x8E);
                        int val = immValue.apply(operand) & 0xFFFF;
                        writeByte.accept((val >> 8) & 0xFF);
                        writeByte.accept(val & 0xFF);
                    } else if (isDirect) {
                        writeByte.accept(0x9E);
                        writeByte.accept(asNumber.apply(operand) & 0xFF);
                    } else if (isIndexed) {
                        writeByte.accept(0xAE);
                        emitIndexed.accept(operand);
                    } else {
                        writeByte.accept(0xBE);
                        int v = asNumber.apply(operand) & 0xFFFF;
                        writeByte.accept((v >> 8) & 0xFF);
                        writeByte.accept(v & 0xFF);
                    }
                    continue;

                case "LDU":
                    if (isImmediate) {
                        writeByte.accept(0xCE);
                        int val = immValue.apply(operand) & 0xFFFF;
                        writeByte.accept((val >> 8) & 0xFF);
                        writeByte.accept(val & 0xFF);
                    } else if (isDirect) {
                        writeByte.accept(0xDE);
                        writeByte.accept(asNumber.apply(operand) & 0xFF);
                    } else if (isIndexed) {
                        writeByte.accept(0xEE);
                        emitIndexed.accept(operand);
                    } else {
                        writeByte.accept(0xFE);
                        int v = asNumber.apply(operand) & 0xFFFF;
                        writeByte.accept((v >> 8) & 0xFF);
                        writeByte.accept(v & 0xFF);
                    }
                    continue;

                // ----------------------
                // STORE (STA, STB, STX, STU)
                // ----------------------
                case "STA":
                    if (isDirect) {
                        writeByte.accept(0x97);
                        writeByte.accept(asNumber.apply(operand) & 0xFF);
                    } else if (isIndexed) {
                        writeByte.accept(0xA7);
                        emitIndexed.accept(operand);
                    } else {
                        writeByte.accept(0xB7);
                        int v = asNumber.apply(operand) & 0xFFFF;
                        writeByte.accept((v >> 8) & 0xFF);
                        writeByte.accept(v & 0xFF);
                    }
                    continue;

                case "STB":
                    if (isDirect) {
                        writeByte.accept(0xD7);
                        writeByte.accept(asNumber.apply(operand) & 0xFF);
                    } else if (isIndexed) {
                        writeByte.accept(0xE7);
                        emitIndexed.accept(operand);
                    } else {
                        writeByte.accept(0xF7);
                        int v = asNumber.apply(operand) & 0xFFFF;
                        writeByte.accept((v >> 8) & 0xFF);
                        writeByte.accept(v & 0xFF);
                    }
                    continue;

                case "STX":
                    if (isDirect) {
                        writeByte.accept(0x9F);
                        writeByte.accept(asNumber.apply(operand) & 0xFF);
                    } else if (isIndexed) {
                        writeByte.accept(0xAF);
                        emitIndexed.accept(operand);
                    } else {
                        writeByte.accept(0xBF);
                        int v = asNumber.apply(operand) & 0xFFFF;
                        writeByte.accept((v >> 8) & 0xFF);
                        writeByte.accept(v & 0xFF);
                    }
                    continue;

                case "STU":
                    if (isDirect) {
                        writeByte.accept(0xDF);
                        writeByte.accept(asNumber.apply(operand) & 0xFF);
                    } else if (isIndexed) {
                        writeByte.accept(0xEF);
                        emitIndexed.accept(operand);
                    } else {
                        writeByte.accept(0xFF);
                        int v = asNumber.apply(operand) & 0xFFFF;
                        writeByte.accept((v >> 8) & 0xFF);
                        writeByte.accept(v & 0xFF);
                    }
                    continue;

                // ----------------------
                // ARITHMETIC (ADDA/ADDB/SUBA/SUBB)
                // ----------------------
                case "ADDA":
                    if (isImmediate) {
                        writeByte.accept(0x8B);
                        writeByte.accept(immValue.apply(operand) & 0xFF);
                    } else if (isDirect) {
                        writeByte.accept(0x9B);
                        writeByte.accept(asNumber.apply(operand) & 0xFF);
                    } else if (isIndexed) {
                        writeByte.accept(0xAB);
                        emitIndexed.accept(operand);
                    } else {
                        writeByte.accept(0xBB);
                        int v = asNumber.apply(operand) & 0xFFFF;
                        writeByte.accept((v >> 8) & 0xFF);
                        writeByte.accept(v & 0xFF);
                    }
                    continue;

                case "ADDB":
                    if (isImmediate) {
                        writeByte.accept(0xCB);
                        writeByte.accept(immValue.apply(operand) & 0xFF);
                    } else if (isDirect) {
                        writeByte.accept(0xDB);
                        writeByte.accept(asNumber.apply(operand) & 0xFF);
                    } else if (isIndexed) {
                        writeByte.accept(0xEB);
                        emitIndexed.accept(operand);
                    } else {
                        writeByte.accept(0xFB);
                        int v = asNumber.apply(operand) & 0xFFFF;
                        writeByte.accept((v >> 8) & 0xFF);
                        writeByte.accept(v & 0xFF);
                    }
                    continue;

                case "SUBA":
                    if (isImmediate) {
                        writeByte.accept(0x80);
                        writeByte.accept(immValue.apply(operand) & 0xFF);
                    } else if (isDirect) {
                        writeByte.accept(0x90);
                        writeByte.accept(asNumber.apply(operand) & 0xFF);
                    } else if (isIndexed) {
                        writeByte.accept(0xA0);
                        emitIndexed.accept(operand);
                    } else {
                        writeByte.accept(0xB0);
                        int v = asNumber.apply(operand) & 0xFFFF;
                        writeByte.accept((v >> 8) & 0xFF);
                        writeByte.accept(v & 0xFF);
                    }
                    continue;

                case "SUBB":
                    if (isImmediate) {
                        writeByte.accept(0xC0);
                        writeByte.accept(immValue.apply(operand) & 0xFF);
                    } else if (isDirect) {
                        writeByte.accept(0xD0);
                        writeByte.accept(asNumber.apply(operand) & 0xFF);
                    } else if (isIndexed) {
                        writeByte.accept(0xE0);
                        emitIndexed.accept(operand);
                    } else {
                        writeByte.accept(0xF0);
                        int v = asNumber.apply(operand) & 0xFFFF;
                        writeByte.accept((v >> 8) & 0xFF);
                        writeByte.accept(v & 0xFF);
                    }
                    continue;

                // ----------------------
                // LOGIC (ANDA/ANDB/ORA/ORB/EORA/EORB)
                // ----------------------
                case "ANDA":
                    if (isImmediate) {
                        writeByte.accept(0x84);
                        writeByte.accept(immValue.apply(operand) & 0xFF);
                    } else if (isDirect) {
                        writeByte.accept(0x94);
                        writeByte.accept(asNumber.apply(operand) & 0xFF);
                    } else if (isIndexed) {
                        writeByte.accept(0xA4);
                        emitIndexed.accept(operand);
                    } else {
                        writeByte.accept(0xB4);
                        int v = asNumber.apply(operand) & 0xFFFF;
                        writeByte.accept((v >> 8) & 0xFF);
                        writeByte.accept(v & 0xFF);
                    }
                    continue;

                case "ANDB":
                    if (isImmediate) {
                        writeByte.accept(0xC4);
                        writeByte.accept(immValue.apply(operand) & 0xFF);
                    } else if (isDirect) {
                        writeByte.accept(0xD4);
                        writeByte.accept(asNumber.apply(operand) & 0xFF);
                    } else if (isIndexed) {
                        writeByte.accept(0xE4);
                        emitIndexed.accept(operand);
                    } else {
                        writeByte.accept(0xF4);
                        int v = asNumber.apply(operand) & 0xFFFF;
                        writeByte.accept((v >> 8) & 0xFF);
                        writeByte.accept(v & 0xFF);
                    }
                    continue;

                case "ORA":
                    if (isImmediate) {
                        writeByte.accept(0x8A);
                        writeByte.accept(immValue.apply(operand) & 0xFF);
                    } else if (isDirect) {
                        writeByte.accept(0x9A);
                        writeByte.accept(asNumber.apply(operand) & 0xFF);
                    } else if (isIndexed) {
                        writeByte.accept(0xAA);
                        emitIndexed.accept(operand);
                    } else {
                        writeByte.accept(0xBA); // ORA EXT
                        int v = asNumber.apply(operand) & 0xFFFF;
                        writeByte.accept((v >> 8) & 0xFF);
                        writeByte.accept(v & 0xFF);
                    }

                    continue;

                case "ORB":
                    if (isImmediate) {
                        writeByte.accept(0xCA);
                        writeByte.accept(immValue.apply(operand) & 0xFF);
                    } else if (isDirect) {
                        writeByte.accept(0xDA);
                        writeByte.accept(asNumber.apply(operand) & 0xFF);
                    } else if (isIndexed) {
                        writeByte.accept(0xEA);
                        emitIndexed.accept(operand);
                    } else {
                        writeByte.accept(0xFA);
                        int v = asNumber.apply(operand) & 0xFFFF;
                        writeByte.accept((v >> 8) & 0xFF);
                        writeByte.accept(v & 0xFF);
                    }
                    continue;

                case "EORA":
                    if (isImmediate) {
                        writeByte.accept(0x88);
                        writeByte.accept(immValue.apply(operand) & 0xFF);
                    } else {
                        // Only immediate implemented in table for EORA; other modes rarely used here
                        throw new IllegalArgumentException("Mode non supporté pour EORA : " + operand);
                    }

                case "EORB":
                    if (isImmediate) {
                        writeByte.accept(0xC8);
                        writeByte.accept(immValue.apply(operand) & 0xFF);
                    } else if (isDirect) {
                        writeByte.accept(0xD8);
                        writeByte.accept(asNumber.apply(operand) & 0xFF);
                    } else if (isIndexed) {
                        writeByte.accept(0xE8);
                        emitIndexed.accept(operand);
                    } else {
                        writeByte.accept(0xF8);
                        int v = asNumber.apply(operand) & 0xFFFF;
                        writeByte.accept((v >> 8) & 0xFF);
                        writeByte.accept(v & 0xFF);
                    }
                    continue;

                // ----------------------
                // INC / DEC / CLR / NEG
                // ----------------------
                case "INC":
                    if (isDirect) {
                        writeByte.accept(0x0C);
                        writeByte.accept(asNumber.apply(operand) & 0xFF);
                    } else if (isIndexed) {
                        writeByte.accept(0x6C); // sometimes INC idx is 0x6C/0x6A; earlier you used 0x6A for DEC IDX, but in table DEC IDX used 0x6A, INC IDX wasn't listed -- we map IDX -> 0x6C assumed
                        emitIndexed.accept(operand);
                    } else {
                        // try EXT version (if present in build): use 0x7C maybe; fallback: use DIR with extended encoding
                        writeByte.accept(0x7C);
                        int v = asNumber.apply(operand) & 0xFFFF;
                        writeByte.accept((v >> 8) & 0xFF);
                        writeByte.accept(v & 0xFF);
                    }
                    continue;
                 // INCA 
                case "INCA":
                    writeByte.accept(0x4C);
                    continue;
                    // INCB
                case "INCB":
                    writeByte.accept(0x5C);
                    continue;

               
                case "DEC":
                    if (isDirect) {
                        writeByte.accept(0x0A);
                        writeByte.accept(asNumber.apply(operand) & 0xFF);
                    } else if (isIndexed) {
                        writeByte.accept(0x6A);
                        emitIndexed.accept(operand);
                    } else {
                        writeByte.accept(0x7A);
                        int v = asNumber.apply(operand) & 0xFFFF;
                        writeByte.accept((v >> 8) & 0xFF);
                        writeByte.accept(v & 0xFF);
                    }
                    continue;
                    // DECA 
                case "DECA":
                    writeByte.accept(0x4A);
                    continue;
                    // DECB
                case "DECB":
                    writeByte.accept(0x5A);
                    continue; 
                    
                case "CLR":
                    if (isDirect) {
                        writeByte.accept(0x0F);
                        writeByte.accept(asNumber.apply(operand) & 0xFF);
                    } else if (isIndexed) {
                        writeByte.accept(0x6F);
                        emitIndexed.accept(operand);
                    } else {
                        writeByte.accept(0x7F);
                        int v = asNumber.apply(operand) & 0xFFFF;
                        writeByte.accept((v >> 8) & 0xFF);
                        writeByte.accept(v & 0xFF);
                    }
                    continue;

                case "NEG":
                    if (isDirect) {
                        writeByte.accept(0x00);
                        writeByte.accept(asNumber.apply(operand) & 0xFF);
                    } else if (isIndexed) {
                        writeByte.accept(0x60);
                        emitIndexed.accept(operand);
                    } else {
                        writeByte.accept(0x70);
                        int v = asNumber.apply(operand) & 0xFFFF;
                        writeByte.accept((v >> 8) & 0xFF);
                        writeByte.accept(v & 0xFF);
                    }
                    continue;

                // ----------------------
                // JMP / JSR
                // ----------------------
                case "JMP":
                    if (isDirect) {
                        writeByte.accept(0x0E);
                        writeByte.accept(asNumber.apply(operand) & 0xFF);
                    } else if (isIndexed) {
                        writeByte.accept(0x6E);
                        emitIndexed.accept(operand);
                    } else {
                        writeByte.accept(0x7E);
                        int v = asNumber.apply(operand) & 0xFFFF;
                        writeByte.accept((v >> 8) & 0xFF);
                        writeByte.accept(v & 0xFF);
                    }
                    continue;

                case "JSR":
                    if (isDirect) {
                        writeByte.accept(0x9D);
                        writeByte.accept(asNumber.apply(operand) & 0xFF);
                    } else if (isIndexed) {
                        writeByte.accept(0xAD);
                        emitIndexed.accept(operand);
                    } else {
                        writeByte.accept(0xBD);
                        int v = asNumber.apply(operand) & 0xFFFF;
                        writeByte.accept((v >> 8) & 0xFF);
                        writeByte.accept(v & 0xFF);
                    }
                    continue;
              

                // ----------------------
                // STACK: PSHS / PULS / PSHU / PULU
                // ----------------------
                case "PSHS":
                    writeByte.accept(0x34);
                    writeByte.accept(parseStackMask(operand));
                    continue;
                case "PULS":
                    writeByte.accept(0x35);
                    writeByte.accept(parseStackMask(operand));

                    continue;
                case "PSHU":
                    writeByte.accept(0x36);
                    writeByte.accept(parseStackMask(operand));
                    continue;
                case "PULU":
                    writeByte.accept(0x37);
                    writeByte.accept(parseStackMask(operand));
                    continue;

                default:
                    throw new IllegalArgumentException("Instruction inconnue ou non supportée par l'assembleur : " + mnemonic);
            }
        } catch (Exception ex) {
            // Renvoyer message utile pour debugging
            throw new IllegalArgumentException("Erreur assembleur at line: \"" + rawLine + "\" -> " + ex.getMessage(), ex);
        }
    }

    // === Vecteur RESET ===
    memory.writeByte(0xFFFE, 0xFC);
    memory.writeByte(0xFFFF, 0x00);

    System.out.println("Programme assemblé et chargé !");
}
    int parseStackMask(String op) {
	    int mask = 0;
	    for (String r : op.split(",")) {
	        switch (r.trim().toUpperCase()) {
	            case "PC": mask |= 0x80; break;
	            case "U":  mask |= 0x40; break;
	            case "Y":  mask |= 0x20; break;
	            case "X":  mask |= 0x10; break;
	            case "DP": mask |= 0x08; break;
	            case "B":  mask |= 0x04; break;
	            case "A":  mask |= 0x02; break;
	            case "CC": mask |= 0x01; break;
	            default:
	                throw new IllegalArgumentException("Registre pile inconnu : " + r);
	        }
	    }
	    return mask;
	}}
