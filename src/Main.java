import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {

    static boolean DEBUG = false;

    static int[] tape    = new int[8];
    static int   pointer = 0;

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("No argument were provided!");
            System.exit(1);
        }
        executeChunk(loadFile(args[0]).toCharArray());
    }

    public static void executeChunk(char[] inputData) {
        for (int i = 0; i < inputData.length; i++) {
            char c = inputData[i];
            switch (c) {
                case '+' -> tape[pointer]++;
                case '-' -> tape[pointer]--;
                case '>' -> pointer++;
                case '<' -> pointer--;
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
                    System.out.print(DEBUG ? (char) tape[pointer]+"("+tape[pointer]+")\n" : (char) tape[pointer]);
                }
                case ',' -> System.out.println("\nInput is not implemented yet");
            }
            pointer = pointer%256;
        }
    }

    public static String loadFile(String fileName) {
        try {
            return Files.readString(Path.of(fileName));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
