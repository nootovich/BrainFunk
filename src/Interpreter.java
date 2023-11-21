import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Stack;

public class Interpreter {

    public static boolean TESTING     = false;
    public static boolean DEBUG       = false;
    public static boolean CONSOLE_OUT = true;
    public static boolean EXTENDED    = false;

    static Stack<Integer>          ptrHistory  = new Stack<>();
    static HashMap<String, String> patterns    = new HashMap<>();
    static StringBuilder           inputMemory = new StringBuilder();
    static StringBuilder           output      = new StringBuilder();
    static byte                    exitCode    = 0;


    public static final int             TAPE_LEN    = 256;
    private static      int             savedVal    = -1;
    private static      int             pointer     = 0;
    private static      byte[]          tape        = new byte[TAPE_LEN];
    private static      Scanner         input       = new Scanner(System.in);
    public static       ArrayList<Byte> inputBuffer = new ArrayList<>();

    public static void executeBF(String data) {
        int dataLen = data.length();
        for (int op = 0; op < dataLen; op++) {
            switch (data.charAt(op)) {
                case '+' -> tape[pointer]++;
                case '-' -> tape[pointer]--;
                case '>' -> movePointer(true, 1);
                case '<' -> movePointer(false, 1);
                case '[' -> op = processCycle(data, op, 1, true);
                case '.' -> printChar(1);
                case ']' -> {}
                case ',' -> processInput(1);
                default -> error("Unknown character: '"+data.charAt(op)+"' (op="+op+")\n"+
                                 data.substring(Math.max(0, op-5), Math.min(op+5, dataLen-1)));
            }
        }
    }

    public static void executeBF(Token[] tokens) {
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
                        if (i == tokens.length-1) error(tokens[start], "Unmatched brackets.");
                    }
                    Token[] innerTokens = new Token[len-1];
                    System.arraycopy(tokens, start+1, innerTokens, 0, innerTokens.length);
                    while (tape[pointer] != 0) executeBF(innerTokens);
                }
                case ENDWHILE -> {
                    // TODO: report an error when there is more `ENDWHILE` than `WHILE` tokens
                }
                case WRITE -> System.out.print((char) tape[pointer]);
                case READ -> processInput(1);
                default -> error(tk, "Unknown token type `"+tk.type+"`");
            }
        }
    }

    public static void executeBrainFunk(Token[] tokens) {
        for (int i = 0; i < tokens.length; i++) {
            Token tk = tokens[i];
            switch (tk.type) {
                case ADD -> tape[pointer] += (byte) getVal();
                case SUB -> tape[pointer] -= (byte) getVal();
                case PTRADD -> ptradd(getVal());
                case PTRSUB -> ptradd(-getVal());
                case WHILE -> {
                    if (i == tokens.length-1) error(tokens[i], "Unmatched brackets.");
                    int start = i;
                    int depth = getVal();
                    int len   = 0;
                    while (depth > 0) {
                        len++;
                        tk = tokens[++i];
                        if (tk.type == Token.Type.WHILE) depth += getVal();
                        else if (tk.type == Token.Type.ENDWHILE) depth -= getVal();
                        else if (tk.type == Token.Type.NUMBER) saveVal(tk);
                        if (depth != 0 && i == tokens.length-1) error(tokens[start], "Unmatched brackets.");
                    }
                    Token[] innerTokens = new Token[len-1];
                    System.arraycopy(tokens, start+1, innerTokens, 0, innerTokens.length);
                    while (tape[pointer] != 0) executeBrainFunk(innerTokens);
                }
                case ENDWHILE -> {
                    // TODO: report an error when there is more `ENDWHILE` than `WHILE` tokens
                }
                case WRITE -> {
                    int val = getVal();
                    for (int j = 0; j < val; j++) System.out.print((char) tape[pointer]);
                }
                case READ -> processInput(getVal());
                case NUMBER -> saveVal(tk);
                case STRING -> {
                    for (int j = 0; j < tk.strValue.length(); j++) {
                        tape[pointer] = (byte) tk.strValue.charAt(j);
                        ptradd(1);
                    }
                }
                // case ':' -> op = addPattern(data, op, name.toString());
                // case ' ' -> executePattern(name.toString(), amount);
                // case '"' -> op = processString(data, op);
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
                default -> error(tk, "Unknown token type `"+tk.type+"`");
            }
        }
    }

    private static void ptradd(int n) {
        pointer = ((pointer+n)%TAPE_LEN+TAPE_LEN)%TAPE_LEN;
    }

    private static void saveVal(Token tk) {
        if (savedVal >= 0) error(tk, "Two consecutive numbers after one another are not supported. "+
                                     "Or this might be a bug in Lexer.");
        savedVal = tk.numValue;
    }

    private static int getVal() {
        if (savedVal < 0) return 1;
        int temp = savedVal;
        savedVal = -1;
        return temp;
    }

    public static void executeBrainFunk(String data) {
        int           dataLen     = data.length();
        int           amount      = 0;
        boolean       nameStarted = false;
        StringBuilder name        = new StringBuilder();
        for (int op = 0; op < dataLen; op++) {
            char c = data.charAt(op);
            if (!nameStarted) {
                if (Character.isDigit(c)) {
                    amount = amount*10+c-'0';
                    continue;
                } else if (Character.isLetter(c)) {
                    nameStarted = true;
                    name.append(c);
                    continue;
                }
            } else if (Character.isLetterOrDigit(c)) {
                name.append(c);
                continue;
            } else if (c != ':' && c != ' ')
                error("Unknown character: '"+data.charAt(op)+"' (op="+op+")\n"+
                      data.substring(Math.max(0, op-5), Math.min(op+5, dataLen-1)));
            switch (c) {
                case '+' -> addsub(true, amount);
                case '-' -> addsub(false, amount);
                case '>' -> movePointer(true, amount);
                case '<' -> movePointer(false, amount);
                case '[' -> op = processCycle(data, op, amount, false);
                case '.' -> printChar(amount);
                case ']' -> {}
                case ',' -> processInput(amount);
                case ':' -> op = addPattern(data, op, name.toString());
                case ' ' -> {
                    // TODO: this thing gotta change
                    // executePattern(name.toString(), amount);
                }
                case '"' -> op = processString(data, op);
                case '$' -> {
                    int target = -1;
                    for (int i = op+1; i < dataLen; i++) {
                        char b = data.charAt(i);
                        if (!Character.isDigit(b)) break;
                        if (target == -1) target = 0;
                        target = target*10+b-'0';
                    }
                    ptrHistory.push(pointer);
                    pointer = target;
                }
                case '#' -> {
                    if (ptrHistory.isEmpty())
                        error("There is not enough pointer history to go back to.");
                    pointer = ptrHistory.pop();
                }
                case '_' -> {}
                case '@' -> syscall();
                default -> error("Unknown character: '"+data.charAt(op)+"' (op="+op+")\n"+
                                 data.substring(Math.max(0, op-5), Math.min(op+5, dataLen-1)));
            }
            amount      = 0;
            nameStarted = false;
            name.setLength(0);
        }
    }

    public static void executeBrainFunkExtended(String data) {
        EXTENDED = true;
        executeBrainFunk(data);
        EXTENDED = false;
    }

    private static void addsub(boolean addition, int amount) {
        amount = (amount > 0 ? amount : 1);
        amount = (addition ? amount : -amount);
        tape[pointer] += (byte) amount;
    }

    private static void movePointer(boolean right, int amount) {
        amount  = (amount > 0 ? amount : 1);
        amount  = (right ? amount : -amount);
        pointer = (((pointer+amount)%TAPE_LEN)+TAPE_LEN)%TAPE_LEN;
    }

    private static int processCycle(String data, int op, int amount, boolean pureBF) {
        // TODO: what should this thing even do if repetition count is > 1?
        int           dataLen      = data.length();
        int           nestingCount = 0;
        StringBuilder t            = new StringBuilder();
        for (int j = op+1; j < dataLen; j++) {
            char g = data.charAt(j);
            if (g == '[') nestingCount++;
            if (g == ']') {
                if (nestingCount == 0) break;
                else nestingCount--;
            }
            // if (j == dataLen-1)
            //     error("Unmatched brackets: (op="+op+")\n"
            //           +data.substring(Math.max(0, op-10), Math.min(op+10, dataLen-1)));
            t.append(g);
        }
        while (tape[pointer] != 0)
            if (pureBF) executeBF(t.toString());
            else executeBrainFunk(t.toString());
        return op+t.length()+1;
    }

    private static void printChar(int amount) {
        amount = (amount > 0 ? amount : 1);
        for (int j = 0; j < amount; j++) {
            output.append((char) tape[pointer]);
            if (CONSOLE_OUT) System.out.print(DEBUG ? (char) tape[pointer]+"("+tape[pointer]+")\n" : (char) tape[pointer]);
        }
    }

    private static void processInput(int amount) {
        // TODO: forgot to think about repetition count
        if (inputBuffer.isEmpty()) {
            System.out.print("Awaiting input: ");
            char[] in = input.nextLine().toCharArray();
            for (char inChar: in) inputBuffer.add((byte) inChar);
        }
        if (inputBuffer.isEmpty()) error("Not enough input data was provided!");
        tape[pointer] = inputBuffer.get(0);
        inputMemory.append((char) tape[pointer]);
        inputBuffer.remove(0);
    }

    private static int addPattern(String data, int op, String name) {
        int           start   = op;
        StringBuilder pattern = new StringBuilder();
        for (op++; op < data.length(); op++) {
            char c = data.charAt(op);
            if (c == ';') break;
            pattern.append(c);
        }
        if (data.charAt(op) != ';')
            error("Unmatched semicolon: (op="+start+")\n"
                  +data.substring(Math.max(0, start-10), Math.min(start+10, data.length()-1)));
        patterns.put(name, pattern.toString());
        return op;
    }

    private static void executePattern(String name, int amount) {
        if (patterns.get(name) == null) error("Pattern '"+name+"' was not found!");
        amount = (amount > 0 ? amount : 1);
        for (int i = 0; i < amount; i++) executeBrainFunk(patterns.get(name));
    }

    private static int processString(String data, int op) {
        int           dataLen = data.length();
        StringBuilder t       = new StringBuilder();
        for (int j = op+1; j < dataLen; j++) {
            char g = data.charAt(j);
            if (g == '"') break;
            if (j == dataLen-1)
                error("Unmatched semicolon: (op="+op+")\n"
                      +data.substring(Math.max(0, op-10), Math.min(op+10, dataLen-1)));
            t.append(g);
        }
        String r = t.toString();
        for (int j = 0; j < r.length(); j++) {
            tape[pointer] = (byte) r.charAt(j);
            movePointer(true, 1);
        }
        return op+r.length()+1;
    }

    private static void syscall() {
        if (!EXTENDED) error("Unknown character '@'");
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
                if (!TESTING) System.exit(tape[pointer-1]);
                else exitCode = tape[pointer-1];
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

    public static void reset() {
        tape     = new byte[TAPE_LEN];
        pointer  = 0;
        exitCode = 0;
        patterns.clear();
        inputBuffer.clear();
        inputMemory.setLength(0);
        output.setLength(0);
    }

    private static void error(String message) {
        System.out.println("[INTERPRETER_ERROR]: "+message);
        System.exit(1);
    }

    private static void error(Token tk, String message) {
        System.out.printf("[INTERPRETER_ERROR]: %s: %s%n", tk, message);
        System.exit(1);
    }

}
