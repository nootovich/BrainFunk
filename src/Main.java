import java.io.File;

public class Main {

    public static String filename;

    public static void main(String[] args) {
        if (args.length < 1) error("Please provide a .bf or .bfn file as a command line argument.");
        String filepath = args[0];
        filename = new File(filepath).getName();
        info("Running %s file.".formatted(filename));

        String  code  = FileSystem.loadFile(filepath);
        Token[] lexed = Lexer.lex(code, filepath);
        info("Lexer OK.");

        Token[] parsed = Parser.parse(lexed);
        info("Parser OK.");

        Interpreter.execute(parsed);
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
