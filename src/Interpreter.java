import java.util.ArrayList;
import java.util.Scanner;
import java.util.Stack;

public class Interpreter {

    private static final int TAPE_LEN       = 256;
    private static final int REPETITION_CAP = 10;

    private static byte[]         tape        = new byte[TAPE_LEN];
    private static int            pointer     = 0;
    private static Stack<Integer> returnStack = new Stack<>();

    private static Scanner         input       = new Scanner(System.in);
    public static  ArrayList<Byte> inputBuffer = new ArrayList<>();
    public static  StringBuilder   inputMemory = new StringBuilder();

    public static void reset() {
        tape    = new byte[TAPE_LEN];
        pointer = 0;
        returnStack.clear();
        inputBuffer.clear();
        inputMemory.setLength(0);
    }

    public static void execute(Token[] tokens) {
        for (int i = 0; i < tokens.length; i++) {
            switch (tokens[i].type) {
                case INC -> tape[pointer] += (byte) tokens[i].num;
                case DEC -> tape[pointer] -= (byte) tokens[i].num;
                case RGT -> pointer = ((pointer + tokens[i].num) % TAPE_LEN + TAPE_LEN) % TAPE_LEN;
                case LFT -> pointer = ((pointer - tokens[i].num) % TAPE_LEN + TAPE_LEN) % TAPE_LEN;
                case JEZ, UNSAFEJEZ -> {
                    if (tape[pointer] == 0) i = tokens[i].num;
                }
                case JNZ, UNSAFEJNZ -> {
                    if (tape[pointer] != 0) i = tokens[i].num;
                }
                case OUT -> System.out.print(String.valueOf((char) tape[pointer]).repeat(tokens[i].num));
                case INP -> read(tokens[i].num);
                case STR -> {
                    for (char c: tokens[i].str.toCharArray()) {
                        tape[pointer] = (byte) c;
                        pointer       = (++pointer % TAPE_LEN + TAPE_LEN) % TAPE_LEN;
                    }
                }
                case PTR -> {
                    returnStack.push(pointer);
                    pointer = tokens[i].num;
                }
                case RET -> {
                    if (returnStack.isEmpty()) Utils.error("Return stack is empty, but a `RET` token was encountered.\n" + tokens[i]);
                    pointer = returnStack.pop();
                }
                case COL -> {} // TODO: this is temporary
                case SYS -> syscall();
                default -> Utils.error("Unexpected token in execution. Probably a bug in `Parser`.\n" + tokens[i]);
            }
        }
    }

    private static void read(int amount) {
        for (int repetitions = 0; inputBuffer.size() < amount && repetitions < REPETITION_CAP; repetitions++) {
            char[] in = input.nextLine().toCharArray();
            for (char inChar: in) {
                inputBuffer.add((byte) inChar);
                inputMemory.append(inChar);
            }
        }
        if (inputBuffer.size() < amount) Utils.error("Not enough data for `READ` token. This should be unreachable unless there is a bug in processing user input.");
        for (int i = 0; i < amount; i++) {
            tape[pointer] = inputBuffer.get(0);
            inputBuffer.remove(0);
        }
    }

    private static void syscall() {
        switch (tape[pointer]) {
            case 60 -> syscall1();
            case 35 -> syscall4();
            default -> Utils.error("This syscall is not implemented yet");
        }
    }

    private static void syscall1() {
        switch (tape[pointer]) {
            case 60 -> System.exit(tape[pointer - 1]);
            default -> Utils.error("This type of syscall1 is not implemented yet");
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
            default -> Utils.error("This type of syscall4 is not implemented yet");
        }
    }
}
