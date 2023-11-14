import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class Interpreter {

    public static boolean TESTING     = false;
    public static boolean DEBUG       = false;
    public static boolean CONSOLE_OUT = true;
    public static boolean EXTENDED    = false;

    static byte[]                  tape        = new byte[256];
    static int                     pointer     = 0;
    static HashMap<String, String> patterns    = new HashMap<>();
    static Scanner                 input       = new Scanner(System.in);
    static ArrayList<Byte>         inputBuffer = new ArrayList<>();
    static StringBuilder           inputMemory = new StringBuilder();
    static StringBuilder           output      = new StringBuilder();
    static byte                    exitCode    = 0;


    public static void executeBF(String data) {
        int dataLen = data.length();
        for (int op = 0; op < dataLen; op++) {
            switch (data.charAt(op)) {
                case '+' -> tape[pointer]++;
                case '-' -> tape[pointer]--;
                case '>' -> movePointer(true, 1);
                case '<' -> movePointer(false, 1);
                case '[' -> processCycle(data, op, 1, true);
                case '.' -> printChar(1);
                case ']' -> {}
                case ',' -> processInput(1);
                default -> Main.exit("Unknown character '"+data.charAt(op)+"'");
            }
        }
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
            } else if (c != ':' && c != ' ') Main.exit("Unexpected character '"+c+"'\nFrom: "+op);
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
                case ' ' -> executePattern(name.toString(), amount);
                case '"' -> op = processString(data, op);
                case '@' -> syscall();
                default -> Main.exit("Unknown character '"+c+"'");
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
        pointer = (((pointer+amount)%tape.length)+tape.length)%tape.length;
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
            if (j == dataLen-1) Main.exit("[ERROR]: Unmatched brackets:\n"+data.substring(Math.max(op-50, 0), Math.min(op+50, dataLen-1)));
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
        if (inputBuffer.isEmpty()) Main.exit("Not enough input data was provided!");
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
        if (data.charAt(op) != ';') Main.exit("Unmatched semicolon!\nFrom: "+start);
        patterns.put(name, pattern.toString());
        return op;
    }

    private static void executePattern(String name, int amount) {
        if (patterns.get(name) == null) Main.exit("Pattern '"+name+"' was not found!");
        amount = (amount > 0 ? amount : 1);
        for (int i = 0; i < amount; i++) executeBrainFunk(patterns.get(name));
    }

    private static int processString(String data, int op) {
        int           dataLen = data.length();
        StringBuilder t       = new StringBuilder();
        for (int j = op+1; j < dataLen; j++) {
            char g = data.charAt(j);
            if (g == '"') break;
            if (j == dataLen-1) Main.exit("Unmatched double-quotes!\nFrom: "+op);
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
        if (!EXTENDED) Main.exit("Unknown character '@'");
        try {
            switch (tape[pointer]) {
                case 60 -> syscall1();
                case 35 -> syscall4();
                default -> Main.exit("This syscall is not implemented yet");
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
            default -> Main.exit("This type of syscall1 is not implemented yet");
        }
    }

    private static void syscall4() throws InterruptedException {
        switch (tape[pointer]) {
            case 35 -> {
                // TODO: a getValueAtPosition function or something
                int pos0 = (pointer-4%tape.length+tape.length)%tape.length;
                int pos1 = (pointer-3%tape.length+tape.length)%tape.length;
                int pos2 = (pointer-2%tape.length+tape.length)%tape.length;
                int pos3 = (pointer-1%tape.length+tape.length)%tape.length;
                Thread.sleep(Math.min(tape[pos0]<<24|tape[pos1]<<16|tape[pos2]<<8|(int) tape[pos3]&0xff, 99999999));
            }
            default -> Main.exit("This type of syscall4 is not implemented yet");
        }
    }

    public static void reset() {
        tape     = new byte[256];
        pointer  = 0;
        exitCode = 0;
        patterns.clear();
        inputBuffer.clear();
        inputMemory.setLength(0);
        output.setLength(0);
    }

}
