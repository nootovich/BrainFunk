import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class Interpreter {

    public static boolean TESTING = false;
    static        boolean DEBUG   = false;

    static byte[]                  tape        = new byte[256];
    static int                     pointer     = 0;
    static HashMap<String, String> patterns    = new HashMap<>();
    static Scanner                 input       = new Scanner(System.in);
    static ArrayList<Byte>         inputBuffer = new ArrayList<>();
    static StringBuilder           inputMemory = new StringBuilder();
    static byte                    exitCode    = 0;


    public static String executeBF(String data, boolean consoleOut) {
        int           dataLen = data.length();
        StringBuilder output  = new StringBuilder();
        for (int i = 0; i < dataLen; i++) {
            char c = data.charAt(i);
            switch (c) {
                case '+' -> tape[pointer]++;
                case '-' -> tape[pointer]--;
                case '>' -> pointer++;
                case '<' -> pointer--;
                case '[' -> {
                    int           nestingCount = 0;
                    StringBuilder t            = new StringBuilder();
                    for (int j = i+1; j < dataLen; j++) {
                        char g = data.charAt(j);
                        if (g == '[') nestingCount++;
                        else if (g == ']') {
                            if (nestingCount == 0) break;
                            else nestingCount--;
                        }
                        if (j == dataLen-1) Main.exit("[ERROR]: Unmatched brackets:\n"+data.substring(Math.max(i-50, 0), Math.min(i+50, dataLen-1)));
                        t.append(g);
                    }
                    while (tape[pointer] != 0) output.append(executeBF(t.toString(), consoleOut));
                    i += t.length()+1;
                }
                case '.' -> {
                    if (consoleOut) printChar();
                    output.append((char) tape[pointer]);
                }
                case ']' -> {}
                case ',' -> {
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
                default -> Main.exit("Unknown character '"+c+"'");
            }
            pointer = (pointer%tape.length+tape.length)%tape.length;
        }
        return output.toString();
    }

    public static String executeBrainFunkExtended(String data, boolean consoleOut) {
        int           dataLen         = data.length();
        int           repetitionCount = 0;
        boolean       nameStarted     = false;
        StringBuilder name            = new StringBuilder();
        StringBuilder output          = new StringBuilder();
        for (int i = 0; i < dataLen; i++) {
            char c = data.charAt(i);
            if (!nameStarted) {
                if (Character.isDigit(c)) {
                    repetitionCount = repetitionCount*10+c-'0';
                    continue;
                } else if (Character.isLetter(c)) {
                    nameStarted = true;
                    name.append(c);
                    continue;
                }
            } else if (Character.isLetterOrDigit(c)) {
                name.append(c);
                continue;
            } else if (c != ':' && c != ' ') Main.exit("Unexpected character '"+c+"'\nFrom: "+i);
            switch (c) {
                case '+' -> tape[pointer] += (byte) (repetitionCount > 0 ? repetitionCount : 1);
                case '-' -> tape[pointer] -= (byte) (repetitionCount > 0 ? repetitionCount : 1);
                case '>' -> pointer += repetitionCount > 0 ? repetitionCount : 1;
                case '<' -> pointer -= repetitionCount > 0 ? repetitionCount : 1;
                case '[' -> {
                    // TODO: what should this thing even do if repetition count is > 1?
                    int           nestingCount = 0;
                    StringBuilder t            = new StringBuilder();
                    for (int j = i+1; j < dataLen; j++) {
                        char g = data.charAt(j);
                        if (g == '[') nestingCount++;
                        if (g == ']') {
                            if (nestingCount == 0) {
                                break;
                            } else nestingCount--;
                        }
                        if (j == dataLen-1) Main.exit("Unmatched brackets!\nFrom: "+i);
                        t.append(g);
                    }
                    String r = t.toString();
                    while (tape[pointer] != 0) output.append(executeBrainFunkExtended(r, consoleOut));
                    i += r.length()+1;
                }
                case '.' -> {
                    if (repetitionCount == 0) {
                        if (consoleOut) printChar();
                        output.append((char) tape[pointer]);
                    } else for (int j = 0; j < repetitionCount; j++) {
                        if (consoleOut) printChar();
                        output.append((char) tape[pointer]);
                    }
                }
                case ']' -> {}
                case ',' -> {
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
                case '@' -> syscall();
                case ':' -> i = addPattern(data, i, name.toString());
                case ' ' -> {
                    if (repetitionCount == 0) output.append(executePattern(name.toString(), consoleOut));
                    else for (int j = 0; j < repetitionCount; j++) output.append(executePattern(name.toString(), consoleOut));
                }
                case '"' -> {
                    StringBuilder t = new StringBuilder();
                    for (int j = i+1; j < dataLen; j++) {
                        char g = data.charAt(j);
                        if (g == '"') break;
                        if (j == dataLen-1) Main.exit("Unmatched double-quotes!\nFrom: "+i);
                        t.append(g);
                    }
                    String r = t.toString();
                    for (int j = 0; j < r.length(); j++) {
                        tape[pointer] = (byte) r.charAt(j);
                        pointer       = (pointer+1%tape.length+tape.length)%tape.length;
                    }
                    i += r.length()+1;
                }
                default -> Main.exit("Unknown character '"+c+"'");
            }
            pointer         = (pointer%tape.length+tape.length)%tape.length;
            repetitionCount = 0;
            nameStarted     = false;
            name.setLength(0);
        }
        return output.toString();
    }

    public static void syscall() {
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

    public static void syscall1() {
        switch (tape[pointer]) {
            case 60 -> {
                if (!TESTING) System.exit(tape[pointer-1]);
                else exitCode = tape[pointer-1];
            }
            default -> Main.exit("This type of syscall1 is not implemented yet");
        }
    }

    public static void syscall4() throws InterruptedException {
        switch (tape[pointer]) {
            case 35 -> {
                int pos0 = (pointer-4%tape.length+tape.length)%tape.length;
                int pos1 = (pointer-3%tape.length+tape.length)%tape.length;
                int pos2 = (pointer-2%tape.length+tape.length)%tape.length;
                int pos3 = (pointer-1%tape.length+tape.length)%tape.length;
                Thread.sleep(Math.min(tape[pos0]<<24|tape[pos1]<<16|tape[pos2]<<8|(int) tape[pos3]&0xff, 99999999));
            }
            default -> Main.exit("This type of syscall4 is not implemented yet");
        }
    }

    public static int addPattern(String data, int i, String name) {
        int           start   = i;
        StringBuilder pattern = new StringBuilder();
        for (i++; i < data.length(); i++) {
            char c = data.charAt(i);
            if (c == ';') break;
            pattern.append(c);
        }
        if (data.charAt(i) != ';') Main.exit("Unmatched semicolon!\nFrom: "+start);
        patterns.put(name, pattern.toString());
        return i;
    }

    public static String executePattern(String name, boolean consoleOut) {
        if (patterns.get(name) == null) Main.exit("Pattern '"+name+"' was not found!");
        return executeBrainFunkExtended(patterns.get(name), consoleOut);
    }

    public static void reset() {
        tape     = new byte[256];
        pointer  = 0;
        exitCode = 0;
        patterns.clear();
        inputBuffer.clear();
        inputMemory.setLength(0);
    }

    public static void printChar() {
        System.out.print(DEBUG ? (char) tape[pointer]+"("+tape[pointer]+")\n" : (char) tape[pointer]);
    }

}
