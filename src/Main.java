import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {

    static boolean DEBUG = false;

    static byte[] tape    = new byte[256];
    static int    pointer = 0;

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("No argument were provided!");
            System.exit(1);
        }
        char[] code = preprocessData(loadFile(args[0]).toCharArray());
        executeChunk(code);
    }

    public static void executeChunk(char[] inputData) {
        int repetitionCount = 0;
        for (int i = 0; i < inputData.length; i++) {
            char c = inputData[i];
            if (Character.isDigit(c)) {
                repetitionCount *= 10;
                repetitionCount += c-'0';
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
                        if (j == inputData.length-1) {
                            System.out.println("Unmatched brackets!\nFrom: "+i);
                            System.exit(1);
                        }
                        t.append(g);
                    }
                    char[] r = t.toString().toCharArray();
                    while (tape[pointer] != 0) {
                        executeChunk(r);
                    }
                    i += r.length;
                }
                case '.' -> {
                    if (repetitionCount == 0) System.out.print(DEBUG ? (char) tape[pointer]+"("+tape[pointer]+")\n" : (char) tape[pointer]);
                    for (int j = 0; j < repetitionCount; j++) System.out.print(DEBUG ? (char) tape[pointer]+"("+tape[pointer]+")\n" : (char) tape[pointer]);
                }
                case ',' -> System.out.println("\nInput is not implemented yet");
                default -> {}
            }
            pointer         = pointer%tape.length;
            repetitionCount = 0;
        }
    }

    public static char[] preprocessData(char[] inputData) {
        StringBuilder processed = new StringBuilder();
        for (int i = 0; i < inputData.length; i++) {
            char c = inputData[i];
            if (c == '/' && i+1 < inputData.length && inputData[i+1] == '/') {
                while (i < inputData.length && inputData[i] != '\n') i++;
            } else if (Character.isDigit(c) || c == '+' || c == '-' || c == '>' || c == '<' || c == '[' || c == ']' || c == '.' || c == ',') {
                processed.append(c);
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
}
