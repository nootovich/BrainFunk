import java.io.File;

public class Main {

    public static String filename;

    public static void main(String[] args) {
        if (args.length < 1) Utils.error("Please provide a .bf or .bfn file as a command line argument.");
        String filepath = args[0];
        filename = new File(filepath).getName();
        Utils.info("Running %s file.".formatted(filename));

        String  code  = FileSystem.loadFile(filepath);
        Token[] lexed = Lexer.lex(code, filepath);
        Utils.info("Lexer OK.");

        Token[] parsed = Parser.parse(lexed);
        Utils.info("Parser OK.");

        Interpreter.execute(parsed);
    }

}
