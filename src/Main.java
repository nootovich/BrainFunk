import java.nio.file.Path;

public class Main {

    public static String filename;

    public static void main(String[] args) {
        boolean showTokens = false;

        if (args.length < 1) exit("Please provide a .bf file as a command line argument.");
        String filepath = args[0];
        filename = Path.of(filepath).getFileName().toString();

        for (int i = 1; i < args.length; i++) {
            if (args[i].equals("-st")) showTokens = true;
            else exit("Unknown argument `"+args[i]+"`.");
        }

        Token[] lexedTokens = Lexer.lexFile(filepath);
        if (showTokens) for (Token tk: lexedTokens) System.out.println(tk);

        Interpreter.executeBF(lexedTokens);
    }

    public static void exit(String message) {
        System.out.println(message);
        System.exit(1);
    }

    // TODO: proper logging system

}
