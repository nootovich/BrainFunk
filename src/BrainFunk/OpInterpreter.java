package BrainFunk;

import nootovich.nglib.NGUtils;

public class OpInterpreter {

    public static byte[] tape = new byte[300];
    public static int    ptr  = 0;

    public static void execute(Op[] instructions) {
        for (int i = 0; i < instructions.length; i++) {
            Op op = instructions[i];
            switch (op.type) {
                case INC -> tape[ptr]++;
                case DEC -> tape[ptr]--;
                case RGT -> ptr++;
                case LFT -> ptr--;
                case INP -> NGUtils.error("TODO: input");
                case OUT -> System.out.print((char) tape[ptr]);
                case JEZ -> { if (tape[ptr] == 0) i = op.num - 1; }
                case JNZ -> { if (tape[ptr] != 0) i = op.num - 1; }
                default -> NGUtils.error("Encountered unexpected OP in execution at %s".formatted(op.token));
            }
        }
    }
}
