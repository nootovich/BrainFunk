package BrainFunk;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Stack;
import nootovich.nglib.NGUtils;

import static BrainFunk.Op.Type.*;

public class Optimizer {

    private static Stack<Integer> pseudoReturnStack = new Stack<>();
    private static Byte[]         pseudoTape        = new Byte[Interpreter.TAPE_LEN];
    private static int            pseudoPointer     = 0;

    public static Op[] optimize(Op[] input) {
        Stack<Integer> OpIndeces = new Stack<>();
        Arrays.fill(pseudoTape, (byte) 0);

        // DEAD CODE ELIMINATION
        if (true) {
            for (int i = 0; i < input.length; i++) {
                Op op = input[i];
                op.visited = true;
                switch (op.type) {
                    case INC -> {
                        if (pseudoTape[pseudoPointer] != null) {
                            pseudoTape[pseudoPointer] = (byte) (pseudoTape[pseudoPointer] + op.num);
                        }
                    }
                    case DEC -> {
                        if (pseudoTape[pseudoPointer] != null) {
                            pseudoTape[pseudoPointer] = (byte) (pseudoTape[pseudoPointer] - op.num);
                        }
                    }
                    case RGT -> pseudoPointer = (pseudoPointer + op.num) % Interpreter.TAPE_LEN;
                    case LFT -> {
                        int newPseudoPointer = pseudoPointer - op.num;
                        pseudoPointer = (newPseudoPointer) < 0 ? Interpreter.TAPE_LEN - 1 : newPseudoPointer;
                    }
                    case INP -> pseudoTape[pseudoPointer] = null;
                    case OUT -> { }
                    case JEZ -> {
                        if (pseudoTape[pseudoPointer] != null && pseudoTape[pseudoPointer] == 0) {
                            i                = op.link.num;
                            op.visited = op.link.visited;
                        }
                    }
                    case JNZ -> {
                        if (pseudoTape[pseudoPointer] != null && pseudoTape[pseudoPointer] != 0) {
                            i = op.link.num;
                        }
                    }
                    case PTR -> {
                        pseudoReturnStack.push(pseudoPointer);
                        pseudoPointer = op.num;
                    }
                    case RET -> {
                        if (pseudoReturnStack.isEmpty()) NGUtils.error("Optimizer tried to perform a pseudo-return but the return stack is empty: " + op);
                        for (int j = 0; j < op.num; j++) pseudoPointer = pseudoReturnStack.pop();
                    }
                    case DEBUG_MACRO -> { }
                    case PUSH_STRING -> {
                        for (int j = 0; j < op.num; j++) {
                            for (byte c: op.str.getBytes(StandardCharsets.UTF_8)) {
                                pseudoTape[pseudoPointer] = c;
                                pseudoPointer++; // NOTE: The only purpose of this line is to make sure that the formatter stops screwing with me
                                pseudoPointer = NGUtils.mod(pseudoPointer, Interpreter.TAPE_LEN);
                            }
                        }
                    }
                    default -> NGUtils.error("Unreachable");
                }
            }

            for (int i = 0; i < input.length; i++) {
                if (input[i].visited) OpIndeces.push(i);
            }
        } else {
            for (int i = 0; i < input.length; i++) {
                OpIndeces.push(i);
            }
        }

        // ACCUMULATION
        if (false) {
            for (int i = OpIndeces.size() - 2; i >= 0; i--) {
                Op op   = input[OpIndeces.get(i)];
                Op next = input[OpIndeces.get(i + 1)];
                switch (op.type) {
                    case INC, DEC, RGT, LFT, INP, OUT -> {
                        if (op.type == next.type) {
                            op.num += next.num;
                            OpIndeces.remove(i + 1);
                        } else if (op.type == INC && next.type == DEC ||
                                   op.type == DEC && next.type == INC ||
                                   op.type == RGT && next.type == LFT ||
                                   op.type == LFT && next.type == RGT) {
                            int val = op.num - next.num;
                            if (val > 0) {
                                OpIndeces.remove(i + 1);
                                op.num = val;
                            } else if (val < 0) {
                                OpIndeces.remove(i);
                                next.num = -val;
                            } else {
                                OpIndeces.remove(i);
                                OpIndeces.remove(i--);
                            }
                        }
                    }
                }
            }
        }

        // FIXING JUMP ADDRESSES
        for (int i = 0; i < OpIndeces.size(); i++) {
            Op op = input[OpIndeces.get(i)];
            if (op.type == JEZ || op.type == JNZ) op.num = i;
        }

        Op[] result = new Op[OpIndeces.size()];
        for (int i = result.length - 1; i >= 0; i--) result[i] = input[OpIndeces.pop()];
        OpIndeces = new Stack<>();
        System.gc();
        return result;
    }
}
