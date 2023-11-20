public class Main {

    public static void main(String[] args) {
        if (args.length < 1) exit("Not enough arguments were provided!");
        String  filepath    = args[0];
        Token[] lexedTokens = Lexer.lexFile(filepath);
        Interpreter.executeBF(lexedTokens);
        // String settings = args[1];
        // String fileData = FileSystem.loadFile(fileName);
        // switch (settings) {
        //     case "r" -> {
        //         if (fileName.endsWith(".bfnx")) {
        //             String parsed = Parser.parseBrainFunkExtended(fileData);
        //             Interpreter.executeBrainFunkExtended(parsed);
        //         } else if (fileName.endsWith(".bfn")) {
        //             String parsed = Parser.parseBrainFunk(fileData);
        //             Interpreter.executeBrainFunk(parsed);
        //         } else if (fileName.endsWith(".bf")) {
        //             String parsed = Parser.parseBF(fileData);
        //             Interpreter.executeBF(parsed);
        //         } else exit("[ERROR]: Invalid file format. Expected a .bf, .bfn or .bfx file.");
        //     }
        //     case "tr" -> {
        //         if (fileName.endsWith(".bfx")) {
        //             exit("[ERROR]: BrainFunkExtended(.bfx) files can't be transpiled since "
        //                  +"they use functionality not available in pure bf (like syscalls).");
        //         } else if (fileName.endsWith(".bfn")) {
        //             String preparsed  = Parser.parseBrainFunk(fileData);
        //             String transpiled = Transpiler.transpile(preparsed);
        //             String parsedPure = Parser.parseBrainFunk(transpiled);
        //             Interpreter.executeBF(parsedPure);
        //         } else exit("[ERROR]: Invalid file format. Expected a .bfn file.");
        //     }
        //     // and so on...
        //     default -> exit("[ERROR]: Invalid settings.");
        // }
    }

    public static void exit(String message) {
        System.out.println(message);
        System.exit(1);
    }

    // TODO: proper logging system

}
