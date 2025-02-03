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
        Stack<Op> output = new Stack<>();
        Arrays.fill(pseudoTape, (byte) 0);

        for (int i = 0; i < input.length; i++) {
            Op op = input[i];
            switch (op.type) {
                case INC, DEC, RGT, LFT, INP, OUT -> {
                    Op prev = output.isEmpty() ? null : output.peek();
                    if (prev == null) {
                        output.push(op);
                    } else if (prev.type == op.type) {
                        prev.num += op.num;
                    } else if (op.type == INC && prev.type == DEC ||
                               op.type == DEC && prev.type == INC ||
                               op.type == RGT && prev.type == LFT ||
                               op.type == LFT && prev.type == RGT) {
                        int val = op.num - prev.num;
                        if (val > 0) {
                            output.pop();
                            op.num = val;
                            output.push(op);
                        } else if (val < 0) {
                            prev.num = -val;
                        } else {
                            output.pop();
                        }
                    } else {
                        output.push(op);
                    }
                }
                case JEZ, JNZ, PTR, RET, DEBUG_MACRO, PUSH_STRING, SYSCALL -> output.push(op);
                default -> NGUtils.error("Unreachable");
            }
        }

        for (int i = 0; i < output.size(); i++) {
            Op op = output.get(i);
            if (op.type == JEZ) input[op.num].num = i;
            if (op.type == JNZ) output.get(op.num).num = i;
        }

        for (int i = 0; i < output.size(); i++) {
            Op op = output.get(i);
            op.token.visited = true;
            switch (op.type) {
                case INC -> { if (pseudoTape[pseudoPointer] != null) pseudoTape[pseudoPointer]++; }
                case DEC -> { if (pseudoTape[pseudoPointer] != null) pseudoTape[pseudoPointer]--; }
                case RGT -> pseudoPointer = ++pseudoPointer % Interpreter.TAPE_LEN;
                case LFT -> pseudoPointer = --pseudoPointer < 0 ? Interpreter.TAPE_LEN - 1 : pseudoPointer;
                case INP -> pseudoTape[pseudoPointer] = null;
                case OUT -> { }
                case JEZ -> {
                    if (pseudoTape[pseudoPointer] != null && pseudoTape[pseudoPointer] == 0) {
                        i                = op.num;
                        op.token.visited = output.get(i).token.visited;
                    }
                }
                case JNZ -> { if (pseudoTape[pseudoPointer] != 0) i = op.num; }
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
                    for (byte c: op.str.getBytes(StandardCharsets.UTF_8)) {
                        pseudoTape[pseudoPointer] = c;
                        pseudoPointer++; // NOTE: The only purpose of this line is to make sure that the formatter stops screwing with me
                        pseudoPointer = NGUtils.mod(pseudoPointer, Interpreter.TAPE_LEN);
                    }
                }
                default -> NGUtils.error("Unreachable");
            }
        }

        for (int i = output.size() - 1; i >= 0; i--) {
            if (!output.get(i).token.visited) output.remove(i);
        }

        Op[] result = new Op[output.size()];
        for (int i = result.length - 1; i >= 0; i--) result[i] = output.pop();
        output = new Stack<>();
        return result;
    }
}
