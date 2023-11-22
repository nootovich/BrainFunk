import java.util.ArrayList;
import java.util.Scanner;

public class Interpreter {

    private static final int TAPE_LEN       = 256;
    private static final int REPETITION_CAP = 1000;

    public static boolean WRITE_ALLOWED = true;

    private static byte[] tape    = new byte[TAPE_LEN];
    private static int    pointer = 0;

    private static int savedVal = -1;

    private static Scanner         input       = new Scanner(System.in);
    public static  ArrayList<Byte> inputBuffer = new ArrayList<>();
    public static  StringBuilder   inputMemory = new StringBuilder();

    public static void reset() {
        tape     = new byte[TAPE_LEN];
        pointer  = 0;
        savedVal = -1;
        inputBuffer.clear();
    }

    public static String executeBF(Token[] tokens) {
        inputMemory.setLength(0);
        String result = privateExecuteBF(tokens);
        reset();
        return result;
    }

    private static String privateExecuteBF(Token[] tokens) {
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < tokens.length; i++) {
            Token tk = tokens[i];
            switch (tk.type) {
                case ADD -> tape[pointer]++;
                case SUB -> tape[pointer]--;
                case PTRADD -> ptradd(1);
                case PTRSUB -> ptradd(-1);
                case WHILE -> {
                    int start = i;
                    int depth = 1;
                    int len   = 0;
                    while (depth > 0) {
                        len++;
                        tk = tokens[++i];
                        if (tk.type == Token.Type.WHILE) depth++;
                        else if (tk.type == Token.Type.ENDWHILE) depth--;
                        if (i == tokens.length-1) error("Unmatched brackets at: "+tokens[start]);
                    }
                    Token[] innerTokens = new Token[len-1];
                    System.arraycopy(tokens, start+1, innerTokens, 0, innerTokens.length);
                    while (tape[pointer] != 0) output.append(privateExecuteBF(innerTokens));
                }
                case ENDWHILE -> {
                    // TODO: report an error when there is more `ENDWHILE` than `WHILE` tokens
                }
                case WRITE -> {write(); output.append((char) tape[pointer]);}
                case READ -> read(1);
                default -> error("Unknown token type `"+tk.type+"`");
            }
        }
        return output.toString();
    }

    public static String executeBrainFunk(Token[] tokens) {
        inputMemory.setLength(0);
        String result = privateExecuteBrainFunk(tokens);
        reset();
        return result;
    }

    private static String privateExecuteBrainFunk(Token[] tokens) {
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < tokens.length; i++) {
            Token tk = tokens[i];
            switch (tk.type) {
                case ADD -> tape[pointer] += (byte) getVal();
                case SUB -> tape[pointer] -= (byte) getVal();
                case PTRADD -> ptradd(getVal());
                case PTRSUB -> ptradd(-getVal());
                case WHILE -> {
                    if (i == tokens.length-1) error("Unmatched brackets at: "+tokens[i]);
                    int start = i;
                    int depth = getVal();
                    int len   = 0;
                    while (depth > 0) {
                        len++;
                        tk = tokens[++i];
                        if (tk.type == Token.Type.WHILE) depth += getVal();
                        else if (tk.type == Token.Type.ENDWHILE) depth -= getVal();
                        else if (tk.type == Token.Type.NUMBER) saveVal(tk);
                        if (depth != 0 && i == tokens.length-1) error("Unmatched brackets at: "+tokens[start]);
                    }
                    Token[] innerTokens = new Token[len-1];
                    System.arraycopy(tokens, start+1, innerTokens, 0, innerTokens.length);
                    while (tape[pointer] != 0) output.append(privateExecuteBrainFunk(innerTokens));
                }
                case ENDWHILE -> {
                    // TODO: report an error when there is more `ENDWHILE` than `WHILE` tokens
                }
                case WRITE -> {
                    int val = getVal();
                    for (int j = 0; j < val; j++) {write(); output.append((char) tape[pointer]);}
                }
                case READ -> read(getVal());
                case NUMBER -> saveVal(tk);
                case STRING -> {
                    for (int j = 0; j < tk.strValue.length(); j++) {
                        tape[pointer] = (byte) tk.strValue.charAt(j);
                        ptradd(1);
                    }
                }
                // case '$' -> {
                //     int target = -1;
                //     for (int i = op+1; i < dataLen; i++) {
                //         char b = data.charAt(i);
                //         if (!Character.isDigit(b)) break;
                //         if (target == -1) target = 0;
                //         target = target*10+b-'0';
                //     }
                //     ptrHistory.push(pointer);
                //     pointer = target;
                // }
                // case '#' -> {
                //     if (ptrHistory.isEmpty())
                //         error("There is not enough pointer history to go back to.");
                //     pointer = ptrHistory.pop();
                // }
                // case '@' -> syscall();
                default -> error("Unknown token type `"+tk.type+"`");
            }
        }
        return output.toString();
    }

    private static void ptradd(int n) {
        pointer = ((pointer+n)%TAPE_LEN+TAPE_LEN)%TAPE_LEN;
    }

    private static void saveVal(Token tk) {
        if (savedVal >= 0) error("Two consecutive numbers after one another are not supported: "+tk);
        savedVal = tk.numValue;
    }

    private static int getVal() {
        if (savedVal < 0) return 1;
        int temp = savedVal;
        savedVal = -1;
        return temp;
    }

    private static void read(int amount) {
        for (int repetitions = 0; inputBuffer.size() < amount && repetitions < REPETITION_CAP; repetitions++) {
            System.out.print("Awaiting input: ");
            char[] in = input.nextLine().toCharArray();
            for (char inChar: in) {
                inputBuffer.add((byte) inChar);
                inputMemory.append(inChar);
            }
        }
        if (inputBuffer.size() < amount) error("Not enough data for `READ` token. This should be unreachable unless there is a bug in processing user input.");
        for (int i = 0; i < amount; i++) {
            tape[pointer] = inputBuffer.get(0);
            inputBuffer.remove(0);
        }
    }

    private static void write() {
        if (WRITE_ALLOWED) System.out.print((char) tape[pointer]);
    }

    private static void syscall() {
        try {
            switch (tape[pointer]) {
                case 60 -> syscall1();
                case 35 -> syscall4();
                default -> error("This syscall is not implemented yet");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static void syscall1() {
        switch (tape[pointer]) {
            case 60 -> {
                System.exit(tape[pointer-1]);
            }
            default -> error("This type of syscall1 is not implemented yet");
        }
    }

    private static void syscall4() throws InterruptedException {
        switch (tape[pointer]) {
            case 35 -> {
                // TODO: a getValueAtPosition function or something
                int pos0 = (pointer-4%TAPE_LEN+TAPE_LEN)%TAPE_LEN;
                int pos1 = (pointer-3%TAPE_LEN+TAPE_LEN)%TAPE_LEN;
                int pos2 = (pointer-2%TAPE_LEN+TAPE_LEN)%TAPE_LEN;
                int pos3 = (pointer-1%TAPE_LEN+TAPE_LEN)%TAPE_LEN;
                Thread.sleep(Math.min(tape[pos0]<<24|tape[pos1]<<16|tape[pos2]<<8|(int) tape[pos3]&0xff, 99999999));
            }
            default -> error("This type of syscall4 is not implemented yet");
        }
    }

    private static void error(String message) {
        StackTraceElement errSrc = Thread.currentThread().getStackTrace()[2];
        System.out.printf("%s:%d [ERROR]: %s%n", errSrc.getFileName(), errSrc.getLineNumber(), message);
        System.exit(1);
    }

}
