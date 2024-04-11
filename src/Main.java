import java.io.File;

public class Main {

    public enum ProgramType {
        BF, BFN, BFNX, ERR
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            Utils.error("No file was provided. Please provide a `.bf`, `.bfn` or `.bfnx` file as a command line argument.");
        }
        String   filepath      = args[0];
        String   filename      = new File(filepath).getName();
        String[] filenameParts = filename.split("\\.");
        String   extension     = filenameParts[filenameParts.length - 1];
        ProgramType programType = switch (extension) {
            case "bf" -> ProgramType.BF;
            case "bfn" -> ProgramType.BFN;
            case "bfnx" -> ProgramType.BFNX;
            default -> {
                Utils.error("Invalid file type `%s`. Please provide a `.bf`, `.bfn` or `.bfnx` file as a command line argument.");
                yield ProgramType.ERR;
            }
        };
        Utils.info("Running %s file.".formatted(filename));

        String  code  = FileSystem.loadFile(filepath);
        Token[] lexed = Lexer.lex(code, filepath, programType);
        Utils.info("Lexer OK.");

        Token[] parsed = Parser.parse(lexed, filepath);
        Utils.info("Parser OK.");

        Interpreter.loadProgram(parsed, programType);
        while (!Interpreter.finished) Interpreter.execute();
    }

}
