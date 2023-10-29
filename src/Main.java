import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class Main {

    static boolean DEBUG = false;

    static byte[]                  tape        = new byte[256];
    static int                     pointer     = 0;
    static HashMap<String, String> patterns    = new HashMap<>();
    static Scanner                 input       = new Scanner(System.in);
    static ArrayList<Byte>         inputBuffer = new ArrayList<>();

    public static void main(String[] args) {
        if (args.length == 0) exit("No argument was provided!");
        String fileData = loadFile(args[0]);
        String code     = preprocessData(fileData);
        executeChunk(code);
    }

    public static void executeChunk(String data) {
        int           dataLen         = data.length();
        int           repetitionCount = 0;
        boolean       nameStarted     = false;
        StringBuilder name            = new StringBuilder();
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
            } else if (c != ':' && c != ' ') exit("Unexpected character '"+c+"'\nFrom: "+i);
            switch (c) {
                case '+' -> tape[pointer] += (byte) (repetitionCount > 0 ? repetitionCount : 1);
                case '-' -> tape[pointer] -= (byte) (repetitionCount > 0 ? repetitionCount : 1);
                case '>' -> pointer += repetitionCount > 0 ? repetitionCount : 1;
                case '<' -> pointer -= repetitionCount > 0 ? repetitionCount : 1;
                case '[' -> {
                    int nestingCount = 0;
                    StringBuilder t = new StringBuilder();
                    for (int j = i+1; j < dataLen; j++) {
                        char g = data.charAt(j);
                        if (g == '[') nestingCount++;
                        if (g == ']') {
                            if (nestingCount == 0) break;
                            else nestingCount--;
                        }
                        if (j == dataLen-1) exit("Unmatched brackets!\nFrom: "+i);
                        t.append(g);
                    }
                    String r = t.toString();
                    while (tape[pointer] != 0) executeChunk(r);
                    i += r.length()+1;
                }
                case '.' -> {
                    if (repetitionCount == 0) printChar();
                    for (int j = 0; j < repetitionCount; j++) printChar();
                }
                case ']' -> {}
                case ',' -> {
                    if (inputBuffer.isEmpty()) {
                        char[] in = input.nextLine().toCharArray();
                        for (char inChar: in) inputBuffer.add((byte) inChar);
                    }
                    if (inputBuffer.isEmpty()) exit("Not enough input data was provided!");
                    tape[pointer] = inputBuffer.get(0);
                    inputBuffer.remove(0);
                }
                case '@' -> syscall();
                case ':' -> i = addPattern(data, i, name.toString());
                case ' ' -> {
                    if (repetitionCount == 0) executePattern(name.toString());
                    for (int j = 0; j < repetitionCount; j++) executePattern(name.toString());
                }
                case '"' -> {
                    StringBuilder t = new StringBuilder();
                    for (int j = i+1; j < dataLen; j++) {
                        char g = data.charAt(j);
                        if (g == '"') break;
                        if (j == dataLen-1) exit("Unmatched double-quotes!\nFrom: "+i);
                        t.append(g);
                    }
                    String r = t.toString();
                    for (int j = 0; j < r.length(); j++) {
                        tape[pointer] = (byte) r.charAt(j);
                        pointer       = (pointer+1%tape.length+tape.length)%tape.length;
                    }
                    i += r.length()+1;
                }
                default -> exit("Unknown character '"+c+"'");
            }
            pointer         = (pointer%tape.length+tape.length)%tape.length;
            repetitionCount = 0;
            nameStarted     = false;
            name.setLength(0);
        }
    }

    public static void syscall() {
        try {
            switch (tape[pointer]) {
                case 60 -> syscall1();
                case 35 -> syscall4();
                default -> exit("This syscall is not implemented yet");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static void syscall1() {
        switch (tape[pointer]) {
            case 60 -> System.exit(tape[pointer-1]);
            default -> exit("This type of syscall1 is not implemented yet");
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
            default -> exit("This type of syscall4 is not implemented yet");
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
        if (data.charAt(i) != ';') exit("Unmatched semicolon!\nFrom: "+start);
        patterns.put(name, pattern.toString());
        return i;
    }

    public static void executePattern(String name) {
        if (patterns.get(name) == null) exit("Pattern '"+name+"' was not found!");
        executeChunk(patterns.get(name));
    }

    public static String preprocessData(String data) {
        int           dataLen     = data.length();
        char[]        allowed     = new char[]{'+', '-', '>', '<', '[', ']', '.', ',', '@', ':', ';', '"'};
        StringBuilder processed   = new StringBuilder();
        boolean       nameStarted = false;
        for (int i = 0; i < dataLen; i++) {
            char c = data.charAt(i);
            if (c == '/' && i+1 < dataLen && data.charAt(i+1) == '/') {
                while (i < dataLen && data.charAt(i) != '\n') i++;
            } else if (c == '"') {
                processed.append('"');
                for (int j = i+1; j < dataLen; j++) {
                    c = data.charAt(j);
                    if (c == '"') {
                        processed.append('"');
                        i = j+1;
                        break;
                    }
                    if (j == dataLen-1) exit("Unmatched double-quotes!\nFrom: "+i);
                    processed.append(c);
                }
            } else if (!nameStarted) {
                if (Character.isLetter(c)) {
                    nameStarted = true;
                    processed.append(c);
                } else if (Character.isDigit(c)) processed.append(c);
                else for (char value: allowed)
                        if (c == value) {
                            processed.append(c);
                            break;
                        }
            } else {
                if (Character.isLetterOrDigit(c)) processed.append(c);
                else {
                    nameStarted = false;
                    if (c == ':') processed.append(':');
                    else if (c == ';') processed.append(" ;");
                    else processed.append(' ');
                }
            }
        }
        return processed.toString();
    }

    public static String loadFile(String fileName) {
        try {
            return Files.readString(Path.of(fileName));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void exit(String message) {
        System.out.println(message);
        System.exit(1);
    }

    public static void printChar() {
        System.out.print(DEBUG ? (char) tape[pointer]+"("+tape[pointer]+")\n" : (char) tape[pointer]);
    }
}
