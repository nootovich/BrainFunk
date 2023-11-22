import java.io.File;

public class Testing {

    static final String TESTING_DIR = "./tests/";

    static final String EXPECTED_DIR    = TESTING_DIR+"expected/";
    static final String SOURCE_FILE     = "src";
    static final String LEXED_FILE      = "lex";
    static final String PARSED_FILE     = "prs";
    static final String TRANSPILED_FILE = "tsp";
    static final String INPUT_FILE      = "in";
    static final String OUTPUT_FILE     = "out";

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("-update")) updateTests();
        String[] files = FileSystem.getDirectoryFiles(TESTING_DIR);
        for (String file: files) {

            // SOURCE
            {
                String srcActual = FileSystem.loadFile(TESTING_DIR+file);
                try {
                    String srcExpected = FileSystem.loadFile(expectedName(file, SOURCE_FILE));
                    if (srcActual.equals(srcExpected)) {
                        info("`%s` source OK".formatted(file));
                    } else {
                        error("`%s` source files do not match!".formatted(file));
                    }
                } catch (RuntimeException ignored) {
                    FileSystem.saveFile(expectedName(file, SOURCE_FILE), srcActual);
                    info("`%s` expected source saved.".formatted(file));
                }
            }

            // LEXED
            Token[] lexedTokens = Lexer.lexFile(TESTING_DIR+file);
            {
                String lexActual = tokensToString(lexedTokens);
                try {
                    String lexExpected = FileSystem.loadFile(expectedName(file, LEXED_FILE));
                    if (lexActual.equals(lexExpected)) {
                        info("`%s` lexed tokens OK.".formatted(file));
                    } else {
                        error("`%s` lexed tokens do not match!".formatted(file));
                    }
                } catch (RuntimeException ignored) {
                    FileSystem.saveFile(expectedName(file, LEXED_FILE), lexActual);
                    info("`%s` expected lexed tokens saved.".formatted(file));
                }
            }

            // PARSED
            Token[] parsedTokens = Parser.parseTokens(lexedTokens);
            if (file.endsWith(".bfn")) {
                String prsActual = tokensToString(parsedTokens);
                try {
                    String prsExpected = FileSystem.loadFile(expectedName(file, PARSED_FILE));
                    if (prsActual.equals(prsExpected)) {
                        info("`%s` parsed tokens OK.".formatted(file));
                    } else {
                        error("`%s` parsed tokens do not match!".formatted(file));
                    }
                } catch (RuntimeException ignored) {
                    FileSystem.saveFile(expectedName(file, PARSED_FILE), prsActual);
                    info("`%s` expected parsed tokens saved.".formatted(file));
                }
            }

            // OUTPUT
            {
                Interpreter.WRITE_ALLOWED = false;
                String outActual = Interpreter.executeBrainFunk(parsedTokens);
                try {
                    String outExpected = FileSystem.loadFile(expectedName(file, OUTPUT_FILE));
                    if (outActual.equals(outExpected)) {
                        info("`%s` output OK.".formatted(file));
                    } else {
                        error("`%s` outputs do not match!".formatted(file));
                    }
                } catch (RuntimeException ignored) {
                    FileSystem.saveFile(expectedName(file, OUTPUT_FILE), outActual);
                    info("`%s` expected output saved.".formatted(file));
                }
            }
        }
    }

    private static String expectedName(String file, String type) {
        return EXPECTED_DIR+file.substring(0, file.lastIndexOf('.'))+"/"+type;
    }

    private static String tokensToString(Token[] tokens) {
        StringBuilder result = new StringBuilder();
        for (Token tk: tokens) result.append(tk).append('\n');
        return result.toString();
    }

    // loadInputData(inPath);
    // if (bfnx) Interpreter.executeBrainFunkExtended(parsedData);
    // else if (bfn) Interpreter.executeBrainFunk(parsedData);
    // else Interpreter.executeBF(parsedData);
    // String outputData = Interpreter.output.toString();
    // if (!updateTests) checkOutput(fileName, outPath, outputData);
    // else FileSystem.saveFile(outPath, outputData);

    // private static void loadInputData(Path inPath) throws IOException {
    //     if (Files.exists(inPath)) {
    //         char[] inputs = Files.readString(inPath).toCharArray();
    //         for (char c: inputs) Interpreter.inputBuffer.add((byte) c);
    //     }
    // }

    private static void updateTests() {
        if (new File(EXPECTED_DIR).exists()) {
            if (!FileSystem.delete(EXPECTED_DIR)) error("Could not delete directory containing expected results of tests.");
            else info("Successfully cleared expected results of tests.");
        } else info("Directory containing expected results of tests does not exist so there is nothing to clear.");
    }

    private static void info(String message) {
        StackTraceElement src = Thread.currentThread().getStackTrace()[2];
        System.out.printf("%s:%d [INFO]: %s%n", src.getFileName(), src.getLineNumber(), message);
    }

    private static void error(String message) {
        StackTraceElement src = Thread.currentThread().getStackTrace()[2];
        System.out.printf("%s:%d [ERROR]: %s%n", src.getFileName(), src.getLineNumber(), message);
    }
}
