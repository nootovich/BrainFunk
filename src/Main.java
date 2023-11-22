import java.io.File;

public class Main {

    public static String  filename;
    public static boolean showTokens;

    public static void main(String[] args) {
        if (args.length < 1) error("Please provide a .bf or .bfn file as a command line argument.");
        String filepath = args[0];
        filename = new File(filepath).getName();
        info("Running %s file.".formatted(filename));

        for (int i = 1; i < args.length; i++) {
            if (args[i].equals("-st")) showTokens = true;
            else error("Unknown argument `"+args[i]+"`.");
        }

        // TODO: should lexer be separate between different filetypes?
        Token[] lexedTokens = Lexer.lexFile(filepath);
        if (filename.endsWith(".bf")) {
            Interpreter.executeBF(lexedTokens);
        } else if (filename.endsWith(".bfn")) {
            Token[] parsedTokens = Parser.parseTokens(lexedTokens);
            Interpreter.executeBrainFunk(parsedTokens);
        } else {
            error("Invalid file format. Please provide a .bf or .bfn file as a command line argument.");
        }

    }

    private static void info(String message) {
        StackTraceElement src = Thread.currentThread().getStackTrace()[2];
        System.out.printf("%s:%d [INFO]: %s%n", src.getFileName(), src.getLineNumber(), message);
    }

    private static void error(String message) {
        StackTraceElement errSrc = Thread.currentThread().getStackTrace()[2];
        System.out.printf("%s:%d [ERROR]: %s%n", errSrc.getFileName(), errSrc.getLineNumber(), message);
        System.exit(1);
    }
}
