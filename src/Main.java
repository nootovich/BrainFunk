import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

public class Main {

    static boolean DEBUG = false;

    static byte[]                  tape     = new byte[256];
    static int                     pointer  = 0;
    static HashMap<String, String> patterns = new HashMap<>();

    public static void main(String[] args) {
        if (args.length == 0) exit("No argument was provided!");
        char[] fileData = loadFile(args[0]).toCharArray();
        char[] code     = preprocessData(fileData);
        executeChunk(code);
    }

    public static void executeChunk(char[] inputData) {
        int           repetitionCount = 0;
        boolean       nameStarted     = false;
        StringBuilder name            = new StringBuilder();
        for (int i = 0; i < inputData.length; i++) {
            char c = inputData[i];
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
            } else if (c != ':' && c != ';') exit("Unexpected character '"+c+"'\nFrom: "+i);
            switch (c) {
                case '+' -> tape[pointer] += (byte) (repetitionCount > 0 ? repetitionCount : 1);
                case '-' -> tape[pointer] -= (byte) (repetitionCount > 0 ? repetitionCount : 1);
                case '>' -> pointer += repetitionCount > 0 ? repetitionCount : 1;
                case '<' -> pointer -= repetitionCount > 0 ? repetitionCount : 1;
                case '[' -> {
                    StringBuilder t = new StringBuilder();
                    for (int j = i+1; j < inputData.length; j++) {
                        char g = inputData[j];
                        if (g == ']') break;
                        if (j == inputData.length-1) exit("Unmatched brackets!\nFrom: "+i);
                        t.append(g);
                    }
                    char[] r = t.toString().toCharArray();
                    while (tape[pointer] != 0) executeChunk(r);
                    i += r.length;
                }
                case '.' -> {
                    if (repetitionCount == 0) printChar();
                    for (int j = 0; j < repetitionCount; j++) printChar();
                }
                case ']' -> {}
                case ',' -> System.out.println("\nInput is not implemented yet\n");
                case '@' -> syscall();
                case ':' -> i = addPattern(inputData, i, name.toString());
                case ';' -> executePattern(name.toString());
                default -> exit("Unknown character '"+c+"'");
            }
            pointer         = pointer%tape.length;
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
            case 35 -> Thread.sleep(Math.min(tape[pointer-4]<<24|tape[pointer-3]<<16|tape[pointer-2]<<8|(int) tape[pointer-1]&0xff, 99999999));
            default -> exit("This type of syscall4 is not implemented yet");
        }
    }

    public static int addPattern(char[] data, int i, String name) {
        int           start   = i;
        StringBuilder pattern = new StringBuilder();
        for (i++; i < data.length; i++) {
            char c = data[i];
            if (c == ';') break;
            pattern.append(c);
        }
        if (data[i] != ';') exit("Unmatched semicolon!\nFrom: "+start);
        patterns.put(name, pattern.toString());
        return i;
    }

    public static void executePattern(String name) {
        if (patterns.get(name) == null) exit("Pattern '"+name+"' was not found!");
        executeChunk(patterns.get(name).toCharArray());
    }

    public static char[] preprocessData(char[] inputData) {
        char[]        allowed   = new char[]{'+', '-', '>', '<', '[', ']', '.', ',', '@', ':', ';'};
        StringBuilder processed = new StringBuilder();
        for (int i = 0; i < inputData.length; i++) {
            char c = inputData[i];
            if (c == '/' && i+1 < inputData.length && inputData[i+1] == '/') {
                while (i < inputData.length && inputData[i] != '\n') i++;
            } else if (Character.isLetterOrDigit(c)) {
                processed.append(c);
            } else for (char value: allowed)
                if (c == value) {
                    processed.append(c);
                    break;
                }
        }
        return processed.toString().toCharArray();
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
