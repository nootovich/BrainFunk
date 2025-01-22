package BrainFunk;

import BrainFunk.BrainFunk.ProgramType;
import java.util.*;
import nootovich.nglib.NGUtils;

public class Interpreter {

    private static final int TAPE_LEN       = 3000;
    private static final int REPETITION_CAP = 10;

    public static ProgramType programType = ProgramType.ERR;

    public static boolean finished = false;

    public static  byte[]         tape         = new byte[TAPE_LEN];
    public static  int            pointer      = 0;
    public static  int            ip           = 0;
    public static  Token[]        tokens       = new Token[0];
    private static Stack<Integer> returnStack  = new Stack<>();
    private static Stack<Integer> pointerStack = new Stack<>();

    private static final Scanner         input       = new Scanner(System.in);
    public static        ArrayList<Byte> inputBuffer = new ArrayList<>();
    public static        StringBuilder   inputMemory = new StringBuilder();

    public static void reset() {
        programType = ProgramType.ERR;
        tokens      = new Token[0];
        restart();
    }

    public static void restart() {
        finished     = false;
        tape         = new byte[TAPE_LEN];
        pointer      = 0;
        ip           = 0;
        returnStack  = new Stack<>();
        inputBuffer  = new ArrayList<>();
        pointerStack = new Stack<>();
        inputMemory  = new StringBuilder();
        System.gc();
    }

    public static void loadProgram(Token[] program, ProgramType pType) {
        tokens      = program;
        programType = pType;
    }

    public static void execute() {
        if (programType == ProgramType.ERR) {
            NGUtils.error("The program was not loaded properly. Please use `loadProgram()` function.");
        } else if (finished) {
            NGUtils.error("Unable to execute the program because it is finished.");
        }
        switch (tokens[ip].type) {

            // BF, BFN, BFNX
            case INC -> tape[pointer] += (byte) tokens[ip].num;
            case DEC -> tape[pointer] -= (byte) tokens[ip].num;
            case RGT -> pointer = ((pointer + tokens[ip].num) % TAPE_LEN + TAPE_LEN) % TAPE_LEN;
            case LFT -> pointer = ((pointer - tokens[ip].num) % TAPE_LEN + TAPE_LEN) % TAPE_LEN;
            case JEZ -> {
                if (tape[pointer] == 0) {
                    ip = tokens[ip].num + 1;
                    return;
                } else if (programType != ProgramType.BF) {
                    pointerStack.push(pointer);
                }
            }
            case JNZ -> {
                if (tape[pointer] != 0) {
                    ip = tokens[ip].num;
                    return;
                }
            }
            case OUT -> System.out.print(String.valueOf((char) tape[pointer]).repeat(tokens[ip].num));
            case INP -> read(tokens[ip].num);

            // BFN, BFNX
            case STR -> {
                if (programType == ProgramType.BF) {
                    NGUtils.error("Invalid token for `.bf` program. This is probably a bug in `Lexer`.");
                }
                for (char c: tokens[ip].str.toCharArray()) {
                    tape[pointer] = (byte) c;
                    pointer       = (++pointer % TAPE_LEN + TAPE_LEN) % TAPE_LEN;
                }
            }
            case PTR -> {
                if (programType == ProgramType.BF) {
                    NGUtils.error("Invalid token for `.bf` program. This is probably a bug in `Lexer`.");
                }
                returnStack.push(pointer);
                pointer = tokens[ip].num;
            }
            case RET -> {
                if (programType == ProgramType.BF) {
                    NGUtils.error("Invalid token for `.bf` program. This is probably a bug in `Lexer`.");
                } else if (returnStack.isEmpty()) {
                    NGUtils.error("Return stack is empty, but a `RET` token was encountered.\n" + tokens[ip]);
                }
                pointer = returnStack.pop();
            }
            case WRD -> { } // TODO: this is temporary

            // BFNX
            case SYS -> {
                if (programType != ProgramType.BFNX) {
                    NGUtils.error("Invalid token for `.bf` program. This is probably a bug in `Lexer`.");
                }
                syscall();
            }

            // UNREACHABLE
            default -> NGUtils.error("Unexpected token in execution. Probably a bug in `Parser`.\n" + tokens[ip]);
        }
        if (ip == tokens.length - 1) finished = true;
        ip++;
    }

    private static void read(int amount) {
        for (int repetitions = 0; inputBuffer.size() < amount && repetitions < REPETITION_CAP; repetitions++) {
            char[] in = input.nextLine().toCharArray();
            for (char inChar: in) {
                inputBuffer.add((byte) inChar);
                inputMemory.append(inChar);
            }
        }
        if (inputBuffer.size() < amount) NGUtils.error("Not enough data for `READ` token. This should be unreachable unless there is a bug in processing user input.");
        for (int i = 0; i < amount; i++) {
            tape[pointer] = inputBuffer.get(0);
            inputBuffer.remove(0);
        }
    }

    private static void syscall() {
        switch (tape[pointer]) {
            case 60 -> syscall1();
            case 35 -> syscall4();
            default -> NGUtils.error("This syscall is not implemented yet");
        }
    }

    private static void syscall1() {
        switch (tape[pointer]) {
            case 60 -> System.exit(tape[pointer - 1]);
            default -> NGUtils.error("This type of syscall1 is not implemented yet");
        }
    }

    private static void syscall4() {
        switch (tape[pointer]) {
            case 35 -> {
                // TODO: a getValueAtPosition function or something
                int pos0 = (pointer - 4 % TAPE_LEN + TAPE_LEN) % TAPE_LEN;
                int pos1 = (pointer - 3 % TAPE_LEN + TAPE_LEN) % TAPE_LEN;
                int pos2 = (pointer - 2 % TAPE_LEN + TAPE_LEN) % TAPE_LEN;
                int pos3 = (pointer - 1 % TAPE_LEN + TAPE_LEN) % TAPE_LEN;
                try {
                    Thread.sleep(Math.min(tape[pos0] << 24 | tape[pos1] << 16 | tape[pos2] << 8 | (int) tape[pos3] & 0xff, 99999999));
                } catch (Exception e) {
                    System.out.println(e);
                }
            }
            default -> NGUtils.error("This type of syscall4 is not implemented yet");
        }
    }
}
