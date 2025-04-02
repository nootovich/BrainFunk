package debugger;

import BrainFunk.*;
import java.io.File;
import nootovich.nglib.NGFileSystem;
import nootovich.nglib.NGUtils;

public class Main {

    public static void main(String[] args) {
        // TODO: [NGLib] Just some nice lil arg-utils
        if (args.length < 1) NGUtils.error("No file was provided. Please provide a `.bf`, `.bfn` or `.bfnx` file as a command line argument.");

        String   filepath      = args[0].replace('\\', '/');
        String   filename      = new File(filepath).getName();
        String[] filenameParts = filename.split("\\.");
        String   extension     = filenameParts[filenameParts.length - 1];

        if (!extension.equals("bf") && !extension.equals("bfn") && !extension.equals("bfnx"))
            NGUtils.error("Invalid file type `%s`. Please provide a `.bf`, `.bfn` or `.bfnx` file as a command line argument.");

        String code = NGFileSystem.loadFile(filepath);
        DebuggerRenderer.filedata = code.split("\n", -1);

        Token[] lexed = Lexer.lex(code, filepath);
        Debugger.tokens.put(filepath, lexed);

        Parser.debug = true;
        Op[] parsed = Parser.parse(lexed, 0);

        // Op[] optimized = Optimizer.optimize(parsed);
        Op[] optimized = parsed;

        Interpreter.loadProgram(optimized);
        new Debugger().main();
    }
}
