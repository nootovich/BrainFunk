import BrainFunk.*;
import java.io.*;
import java.util.Stack;
import nootovich.nglib.NGFileSystem;
import nootovich.nglib.NGUtils;


public class Testing {

    private static final int TEST_NAME_LIMIT = 20;

    private static final String TESTING_DIR     = "./tests/";
    private static final String EXPECTED_DIR    = TESTING_DIR + "expected/";
    private static final String SOURCE_FILE     = "src";
    private static final String LEXED_FILE      = "lex";
    private static final String PARSED_FILE     = "prs";
    private static final String OPTIMIZED_FILE  = "opt";
    private static final String TRANSPILED_FILE = "tsp";
    private static final String INPUT_FILE      = "inp";
    private static final String OUTPUT_FILE     = "out";

    private static boolean logLevenstein  = true;
    private static int     filenameMaxLen = 0;

    public static void main(String[] args) {
        Stack<String> fileStack = new Stack<>();
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-reset" -> NGFileSystem.delete(EXPECTED_DIR);
                case "-nolev" -> logLevenstein = false;
                default -> fileStack.push(args[i]);
            }
        }

        String[] files;
        if (fileStack.isEmpty()) {
            files = NGFileSystem.getDirectoryFiles(TESTING_DIR);
        } else {
            files = new String[fileStack.size()];
            for (int i = files.length - 1; i >= 0; i--) {
                files[i] = fileStack.pop();
            }
        }

        for (String file: files) {
            if (file.length() + 1 > filenameMaxLen) {
                filenameMaxLen = file.length() + 1;
            }
        }

        for (String file: files) {
            String   filepath      = TESTING_DIR + file;
            String[] filenameParts = file.split("\\.");
            String   extension     = filenameParts[filenameParts.length - 1];
            if (!extension.equals("bf") && !extension.equals("bfn")) continue;

            boolean passed = true;

            // SOURCE
            String code = NGFileSystem.loadFile(filepath);
            passed &= check(code, expectedName(file, SOURCE_FILE), getLogTemplate("source", file));

            // LEXED
            Token[] lexed = Lexer.lex(code, file);
            passed &= check(tokensToString(lexed), expectedName(file, LEXED_FILE), getLogTemplate("lexed", file));

            // PARSED
            Op[] parsed = Parser.parse(lexed, 0);
            passed &= check(opsToString(parsed), expectedName(file, PARSED_FILE), getLogTemplate("parsed", file));

            // OPTIMIZED
            Op[] optimized = Optimizer.optimize(parsed);
            passed &= check(opsToString(optimized), expectedName(file, OPTIMIZED_FILE), getLogTemplate("optimized", file));

            // INPUT
            String expectedInput = null;
            try {
                expectedInput = NGFileSystem.loadFile(expectedName(file, INPUT_FILE));
                for (char c: expectedInput.toCharArray()) Interpreter.inputBuffer.add((byte) c);
            } catch (RuntimeException ignored) { }

            // OUTPUT
            PrintStream           stdout    = System.out;
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            System.setOut(new PrintStream(outStream));
            Interpreter.loadProgram(parsed);
            while (!Interpreter.finished) Interpreter.execute();
            System.out.flush();
            System.setOut(stdout);
            passed &= check(outStream.toString(), expectedName(file, OUTPUT_FILE), getLogTemplate("output", file));
            Interpreter.reset();

            // OPTIMIZED OUTPUT
            if (expectedInput != null) for (char c: expectedInput.toCharArray()) Interpreter.inputBuffer.add((byte) c);
            outStream = new ByteArrayOutputStream();
            System.setOut(new PrintStream(outStream));
            Interpreter.loadProgram(optimized);
            while (!Interpreter.finished) Interpreter.execute();
            System.out.flush();
            System.setOut(stdout);
            passed &= check(outStream.toString(), expectedName(file, OUTPUT_FILE), getLogTemplate("optimized output", file));

            // SAVE INPUT
            String input = Interpreter.inputMemory.toString();
            if (expectedInput != null && !input.isEmpty()) {
                NGFileSystem.saveFile(expectedName(file, INPUT_FILE), input);
                NGUtils.info(getLogTemplate("input", file) + "SAVED.");
            }

            if (passed) NGUtils.info(getLogTemplate("", file) + "OK.");
            else break;

            Parser.reset();
            Interpreter.reset();
        }
    }

    private static boolean check(String actual, String expectedName, String logTemplate) {
        try {
            String expected = NGFileSystem.loadFile(expectedName);
            if (!actual.equals(expected)) {
                NGUtils.info(logTemplate + "DIFFERS!");
                if (logLevenstein) Levenstein.printDiff(actual, expected);
                return false;
            }
        } catch (UncheckedIOException ignored) {
            NGFileSystem.saveFile(expectedName, actual);
            NGUtils.info(logTemplate + "SAVED.");
        } catch (RuntimeException e) {
            NGUtils.error(e.toString());
        }
        return true;
    }

    private static String expectedName(String file, String type) {
        return EXPECTED_DIR + file.substring(0, file.lastIndexOf('.')) + "/" + type;
    }

    private static String tokensToString(Token[] tokens) {
        StringBuilder result = new StringBuilder();
        for (Token tk: tokens) result.append(tk).append('\n');
        return result.toString();
    }

    private static String opsToString(Op[] ops) {
        StringBuilder result = new StringBuilder();
        for (Op op: ops) result.append(op).append('\n');
        return result.toString();
    }

    private static String getLogTemplate(String testName, String file) {
        String filenameSpacing = " ".repeat(filenameMaxLen - file.length());
        String testnameSpacing = " ".repeat(TEST_NAME_LIMIT - testName.length());
        return "`%s`%s%s%s".formatted(file, filenameSpacing, testName, testnameSpacing);
    }

    private static class Levenstein {

        private record Patch(char action, int line, String data) { }

        public static void printDiff(String cur, String prev) {
            String[] linesA = prev.split("\n", -1);
            String[] linesB = cur.split("\n", -1);
            Patch[]  diff   = Levenstein.getDiff(linesA, linesB);

            System.out.println("-------------------------------");
            for (int i = 0; i < diff.length; i++) {
                Patch patch  = diff[i];
                char  action = patch.action;
                switch (action) {
                    case 'I' -> System.out.print("\u001B[0m" + patch.data + "\n");
                    case 'A' -> System.out.print("\u001B[42m\u001B[30m" + patch.data + "\n");
                    case 'R' -> System.out.print("\u001B[41m\u001B[30m" + patch.data + "\n");
                    default -> throw new RuntimeException("Diff messed up somehow");
                }
            }
            System.out.println("\u001B[0m-------------------------------");
        }

        public static Patch[] getDiff(String[] linesA, String[] linesB) {
            int      lenA        = linesA.length;
            int      lenB        = linesB.length;
            int[][]  lineDist    = new int[lenA + 1][lenB + 1];
            char[][] lineActions = new char[lenA + 1][lenB + 1];

            lineActions[0][0] = 'I';
            for (int i = 1; i <= lenA; i++) {
                lineDist[i][0]    = i;
                lineActions[i][0] = 'R';
            }
            for (int i = 1; i <= lenB; i++) {
                lineDist[0][i]    = i;
                lineActions[0][i] = 'A';
            }

            for (int i = 1; i <= lenA; i++) {
                for (int j = 1; j <= lenB; j++) {
                    if (linesA[i - 1].equals(linesB[j - 1])) {
                        lineDist[i][j]    = lineDist[i - 1][j - 1];
                        lineActions[i][j] = 'I';
                        continue;
                    }

                    int rem = lineDist[i - 1][j];
                    int add = lineDist[i][j - 1];
                    lineDist[i][j]    = rem;
                    lineActions[i][j] = 'R';
                    if (lineDist[i][j] > add) {
                        lineDist[i][j]    = add;
                        lineActions[i][j] = 'A';
                    }
                    lineDist[i][j] += 1;
                }
            }

            Stack<Patch> result = new Stack<>();
            int          i      = lenA;
            int          j      = lenB;
            while (i > 0 || j > 0) {
                char action = lineActions[i][j];
                if (action == 'A') {
                    j--;
                    result.push(new Patch('A', j, linesB[j]));
                } else if (action == 'R') {
                    i--;
                    result.push(new Patch('R', i, linesA[i]));
                } else if (action == 'I') {
                    i--;
                    j--;
                    result.push(new Patch('I', i, linesA[i]));
                }
            }

            Patch[] diff = new Patch[result.size()];
            for (int k = 0; k < diff.length; k++) diff[k] = result.pop();
            return diff;


            // TODO: maybe reimplement character-level diff
            //  leaving this here just in case
            /*int aidx = a.length();
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

            return diff.reverse().toString();*/
        }

        // TODO: maybe reimplement character-level diff
        //  leaving this here just in case
        /*public static int[][] getDistanceMatrix(String a, String b) {
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
        }*/
    }
}
