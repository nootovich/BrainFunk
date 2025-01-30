package debugger;

import BrainFunk.*;
import java.io.File;
import nootovich.nglib.NGFileSystem;
import nootovich.nglib.NGUtils;

public class Main {

    public static void main(String[] args) {
        // TODO: [NGLib] Just some nice lil arg-utils
        if (args.length < 1) NGUtils.error("No file was provided. Please provide a `.bf`, `.bfn` or `.bfnx` file as a command line argument.");

        String   filepath      = args[0];
        String   filename      = new File(filepath).getName();
        String[] filenameParts = filename.split("\\.");
        String   extension     = filenameParts[filenameParts.length - 1];

        BrainFunk.ProgramType programType = switch (extension) {
            case "bf" -> BrainFunk.ProgramType.BF;
            case "bfn" -> BrainFunk.ProgramType.BFN;
            case "bfnx" -> BrainFunk.ProgramType.BFNX;
            default -> {
                NGUtils.error("Invalid file type `%s`. Please provide a `.bf`, `.bfn` or `.bfnx` file as a command line argument.");
                yield BrainFunk.ProgramType.ERR;
            }
        };

        String code = NGFileSystem.loadFile(filepath);
        DebuggerRenderer.filedata = code.split("\n", -1);

        Parser.debug = true;
        Debugger.lexed  = Lexer.lex(code, filepath, programType);
        Token[] parsed = Parser.parse(Token.deepCopy(Debugger.lexed), filepath);
        Op[] parsed2 = Parser.parse2(Debugger.lexed);
        Interpreter.loadProgram(parsed, programType);

        new Debugger().main();
    }
}
