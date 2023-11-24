import java.io.File;

public class Testing {

    private static final int FILE_NAME_LIMIT       = 20;
    private static final int TEST_NAME_LIMIT       = 10;
    private static final int TEST_CLASS_NAME_LIMIT = 17;
    private static final int LOG_NAME_LIMIT        = 9;

    private static final String TESTING_DIR = "./tests/";

    private static final String EXPECTED_DIR    = TESTING_DIR+"expected/";
    private static final String SOURCE_FILE     = "src";
    private static final String LEXED_FILE      = "lex";
    private static final String PARSED_FILE     = "prs";
    private static final String TRANSPILED_FILE = "tsp";
    private static final String INPUT_FILE      = "in";
    private static final String OUTPUT_FILE     = "out";

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("-update")) updateTests();
        String[] files = FileSystem.getDirectoryFiles(TESTING_DIR);
        for (String file: files) {
            String filenameSpacing = " ".repeat(FILE_NAME_LIMIT-file.length());

            // SOURCE
            String srcActual = FileSystem.loadFile(TESTING_DIR+file);
            check(srcActual, expectedName(file, SOURCE_FILE), getLogTemplate("source", file, filenameSpacing));

            // LEXED
            Token[] lexedTokens = Lexer.lexFile(TESTING_DIR+file);
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
                info(getLogTemplate("input", file, filenameSpacing)+"LOADED.");
            } catch (RuntimeException ignored) {}

            // OUTPUT
            Interpreter.WRITE_ALLOWED = false;
            String outActual = Interpreter.executeBrainFunk(parsedTokens);
            check(outActual, expectedName(file, OUTPUT_FILE), getLogTemplate("output", file, filenameSpacing));

            // INPUT
            String inActual = Interpreter.inputMemory.toString();
            if (inExpected.isEmpty() && !inActual.isEmpty()) {
                FileSystem.saveFile(expectedName(file, INPUT_FILE), inActual);
                info(getLogTemplate("input", file, filenameSpacing)+"SAVED.");
            }
        }
    }

    private static void check(String actual, String expectedName, String logTemplate) {
        try {
            String expected = FileSystem.loadFile(expectedName);
            if (actual.equals(expected)) info(logTemplate+"OK.");
            else error(logTemplate+"DIFFERS!");
        } catch (RuntimeException ignored) {
            FileSystem.saveFile(expectedName, actual);
            info(logTemplate+"SAVED.");
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

    private static String getLogTemplate(String testName, String file, String filenameSpacing) {
        return "`%s`%s%s%s".formatted(file, filenameSpacing, testName, " ".repeat(TEST_NAME_LIMIT-testName.length()));
    }

    private static void updateTests() {
        if (new File(EXPECTED_DIR).exists()) {
            if (!FileSystem.delete(EXPECTED_DIR)) error("Could not delete directory containing expected results of tests.");
            else info("Successfully cleared expected results of tests.");
        } else info("Directory containing expected results of tests does not exist so there is nothing to clear.");
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
        String            testingClassNameSpacing = " ".repeat(TEST_CLASS_NAME_LIMIT-testingClassName.length());
        String            logSpacing              = " ".repeat(LOG_NAME_LIMIT-type.length());
        System.out.printf("%s%s%s%s%s%n", testingClassName, testingClassNameSpacing, type, logSpacing, message);
    }

}
