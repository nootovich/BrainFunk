import java.nio.file.Path;

public class Main {

    public static String filename;

    public static void main(String[] args) {
        boolean showTokens = false;

        if (args.length < 1) error("Please provide a .bf or .bfn file as a command line argument.");
        String filepath = args[0];
        filename = Path.of(filepath).getFileName().toString();

        for (int i = 1; i < args.length; i++) {
            if (args[i].equals("-st")) showTokens = true;
            else error("Unknown argument `"+args[i]+"`.");
        }

        // TODO: should lexer be separate between different filetypes?
        Token[] lexedTokens = Lexer.lexFile(filepath);
        if (showTokens) for (Token tk: lexedTokens) System.out.println(tk);
        if (filename.endsWith(".bf")) {
            Interpreter.executeBF(lexedTokens);
        } else if (filename.endsWith(".bfn")) {
            Interpreter.executeBrainFunk(lexedTokens);
        } else {
            error("Invalid file format. Please provide a .bf or .bfn file as a command line argument.");
        }

    }

    private static void error(String message) {
        System.out.printf("[HUMONGOLONGOUS_ERROR!]: %s%n", message);
        System.exit(1);
    }
}
