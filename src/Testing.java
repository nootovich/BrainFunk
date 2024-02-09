import java.io.File;

public class Testing {

    private static final int FILE_NAME_LIMIT       = 20;
    private static final int TEST_NAME_LIMIT       = 10;
    private static final int TEST_CLASS_NAME_LIMIT = 17;
    private static final int LOG_NAME_LIMIT        = 9;

    private static final String TESTING_DIR = "./tests/";

    private static final String EXPECTED_DIR    = TESTING_DIR + "expected/";
    private static final String SOURCE_FILE     = "src";
    private static final String LEXED_FILE      = "lex";
    private static final String PARSED_FILE     = "prs";
    private static final String TRANSPILED_FILE = "tsp";
    private static final String INPUT_FILE      = "in";
    private static final String OUTPUT_FILE     = "out";

    public static void main(String[] args) {
        String[] files;
        if (args.length == 0) {
            files = FileSystem.getDirectoryFiles(TESTING_DIR);
        } else if (!args[0].equals("-update")) {
            files = new String[]{args[0]};
        } else {
            files = FileSystem.getDirectoryFiles(TESTING_DIR);
            updateTests();
        }

        for (String file: files) {
            String filenameSpacing = " ".repeat(FILE_NAME_LIMIT - file.length());

            // SOURCE
            String srcActual = FileSystem.loadFile(TESTING_DIR + file);
            check(srcActual, expectedName(file, SOURCE_FILE), getLogTemplate("source", file, filenameSpacing));

            // LEXED
            Token[] lexedTokens = Lexer.lexFile(TESTING_DIR + file);
            String  lexActual   = tokensToString(lexedTokens);
            check(lexActual, expectedName(file, LEXED_FILE), getLogTemplate("lexed", file, filenameSpacing));

            // PARSED
            Token[] parsedTokens = Parser.parseTokens(lexedTokens);
            String  prsActual    = tokensToString(parsedTokens);
            check(prsActual, expectedName(file, PARSED_FILE), getLogTemplate("parsed", file, filenameSpacing));

            // INPUT
            String inExpected = "";
            try {
                inExpected = FileSystem.loadFile(expectedName(file, INPUT_FILE));
                for (char c: inExpected.toCharArray()) Interpreter.inputBuffer.add((byte) c);
                info(getLogTemplate("input", file, filenameSpacing) + "LOADED.");
            } catch (RuntimeException ignored) {}

            // OUTPUT
            Interpreter.WRITE_ALLOWED = false;
            String outActual = Interpreter.executeBrainFunk(parsedTokens);
            check(outActual, expectedName(file, OUTPUT_FILE), getLogTemplate("output", file, filenameSpacing));

            // INPUT
            String inActual = Interpreter.inputMemory.toString();
            if (inExpected.isEmpty() && !inActual.isEmpty()) {
                FileSystem.saveFile(expectedName(file, INPUT_FILE), inActual);
                info(getLogTemplate("input", file, filenameSpacing) + "SAVED.");
            }
        }
    }

    private static void check(String actual, String expectedName, String logTemplate) {
        try {
            String expected = FileSystem.loadFile(expectedName);
            if (actual.equals(expected)) info(logTemplate + "OK.");
            else {
                error(logTemplate + "DIFFERS!\n-------------------------------");
                Levenstein.printDiff(expected, actual);
                System.out.println("-------------------------------");
            }
        } catch (RuntimeException ignored) {
            FileSystem.saveFile(expectedName, actual);
            info(logTemplate + "SAVED.");
        }
    }

    private static String expectedName(String file, String type) {
        return EXPECTED_DIR + file.substring(0, file.lastIndexOf('.')) + "/" + type;
    }

    private static String tokensToString(Token[] tokens) {
        StringBuilder result = new StringBuilder();
        for (Token tk: tokens) result.append(tk).append('\n');
        return result.toString();
    }

    private static String getLogTemplate(String testName, String file, String filenameSpacing) {
        return "`%s`%s%s%s".formatted(file, filenameSpacing, testName, " ".repeat(TEST_NAME_LIMIT - testName.length()));
    }

    private static void updateTests() {
        if (new File(EXPECTED_DIR).exists()) {
            if (!FileSystem.delete(EXPECTED_DIR)) error("Could not delete directory containing expected results of tests.");
            else info("Successfully cleared expected results of tests.");
        } else info("The Directory containing expected results of tests doesn't exist, so there is nothing to clear.");
    }

    // TODO: extract to other classes
    private static void info(String message) {
        log("[INFO]:", message);
    }

    private static void error(String message) {
        log("[ERROR]:", message);
    }

    private static void fatal(String message) {
        log("[FATAL]:", message);
        System.exit(1);
    }

    private static void log(String type, String message) {
        StackTraceElement src                     = Thread.currentThread().getStackTrace()[2];
        String            testingClassName        = "%s:%d".formatted(src.getFileName(), src.getLineNumber());
        String            testingClassNameSpacing = " ".repeat(TEST_CLASS_NAME_LIMIT - testingClassName.length());
        String            logSpacing              = " ".repeat(LOG_NAME_LIMIT - type.length());
        System.out.printf("%s%s%s%s%s%n", testingClassName, testingClassNameSpacing, type, logSpacing, message);
    }

    private static class Levenstein {

        public static void printDiff(String a, String b) {
            String diff = Levenstein.getDiff(a, b);

            int j = 0, k = 0;
            for (int i = 0; i < diff.length(); i++) {
                char c = diff.charAt(i);

                if (c == ' ') System.out.print("\u001B[0m" + a.charAt(i + k));
                if (c == 'I') {
                    System.out.print("\u001B[42m\u001B[30m" + b.charAt(i + j));
                    k--;
                }
                if (c == 'D') {
                    System.out.print("\u001B[41m\u001B[30m" + a.charAt(i));
                    j--;
                }
                if (c == 'S') System.out.print("\u001B[44m\u001B[30m" + b.charAt(i + j));
            }
        }

        public static String getDiff(String a, String b) {
            int aidx = a.length();
            int bidx = b.length();

            int[][] distances = getDistanceMatrix(a, b);

            if (aidx + 1 != distances.length || bidx + 1 != distances[0].length) {
                throw new RuntimeException("Invalid Distance Matrix!");
            }

            StringBuilder diff = new StringBuilder();

            while (aidx != 0 || bidx != 0) {
                int cur = distances[aidx][bidx];

                int sub = aidx > 0 && bidx > 0 ? distances[aidx - 1][bidx - 1] : Integer.MAX_VALUE;
                int del = aidx > 0 ? distances[aidx - 1][bidx] : Integer.MAX_VALUE;
                int ins = bidx > 0 ? distances[aidx][bidx - 1] : Integer.MAX_VALUE;

                int min = Math.min(Math.min(sub, del), ins);

                if (min == cur) {
                    diff.append(" ");
                    aidx--;
                    bidx--;
                } else if (min == cur - 1) {
                    if (min == ins) {
                        diff.append("I");
                        bidx--;
                    } else if (min == del) {
                        diff.append("D");
                        aidx--;
                    } else if (min == sub) {
                        diff.append("S");
                        aidx--;
                        bidx--;
                    } else {
                        throw new RuntimeException("Invalid Distance Matrix!");
                    }
                } else {
                    throw new RuntimeException("Invalid Distance Matrix!");
                }
            }

            return diff.reverse().toString();
        }

        public static int[][] getDistanceMatrix(String a, String b) {
            int     n = a.length();
            int     m = b.length();
            int[][] d = new int[n + 1][m + 1];

            for (int i = 1; i <= n; i++) d[i][0] = i;
            for (int j = 1; j <= m; j++) d[0][j] = j;

            for (int j = 1; j <= m; j++) {
                for (int i = 1; i <= n; i++) {
                    int sc = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                    d[i][j] = Math.min(Math.min(d[i - 1][j] + 1, d[i][j - 1] + 1), d[i - 1][j - 1] + sc);
                }
            }

            return d;
        }
    }

}
