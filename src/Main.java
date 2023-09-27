import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {

    static boolean DEBUG = false;

    static byte[] tape    = new byte[256];
    static int    pointer = 0;

    public static void main(String[] args) {
        if (args.length == 0) exit("No argument were provided!");
        char[] fileData = loadFile(args[0]).toCharArray();
        char[] code     = preprocessData(fileData);
        executeChunk(code);
    }

    public static void executeChunk(char[] inputData) {
        int repetitionCount = 0;
        for (int i = 0; i < inputData.length; i++) {
            char c = inputData[i];
            if (Character.isDigit(c)) {
                repetitionCount = repetitionCount*10+c-'0';
                continue;
            }
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
                case '@' -> syscall(inputData, i);
                default -> exit("Unknown character '"+c+"'");
            }
            pointer         = pointer%tape.length;
            repetitionCount = 0;
        }
    }

    public static void syscall(char[] data, int i) {
        if (i >= data.length-1) exit("Amount of arguments for syscall was not provided");
        switch (data[i+1]) {
            case '1' -> syscall1();
            default -> exit("Not implemented yet");
        }
    }

    public static void syscall1() {
        if (tape[pointer] != 60) exit("Not implemented yet");
        System.exit(tape[pointer-1]);
    }

    public static char[] preprocessData(char[] inputData) {
        char[]        allowed   = new char[]{'+', '-', '>', '<', '[', ']', '.', ',', '@'};
        StringBuilder processed = new StringBuilder();
        for (int i = 0; i < inputData.length; i++) {
            char c = inputData[i];
            if (c == '/' && i+1 < inputData.length && inputData[i+1] == '/') {
                while (i < inputData.length && inputData[i] != '\n') i++;
            } else if (Character.isDigit(c)) {
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
