import java.io.File;
import java.util.Arrays;

public class Main {

    public enum ProgramType {
        BF, BFN, BFNX, ERR
    }

    public static void main(String[] args) {
        boolean profiling = false;
        boolean profiling          = false;
        boolean transpile          = false;
        String  transpiledFilepath = null;

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

        for (int i = 1; i < args.length; i++) {
            switch (args[i]) {
                case "-p", "--profiling" -> profiling = true;
                case "-t", "--transpile" -> {
                    if (i == args.length - 1) {
                        Utils.error("No name for transpiled file was provided");
                    }
                    transpile          = true;
                    transpiledFilepath = "./%s.bf".formatted(args[++i]);
                }
                default -> Utils.error("Unknown argument " + args[i]);
            }
        }

        Utils.info("Running %s program.".formatted(filename));

        String  code  = FileSystem.loadFile(filepath);
        Token[] lexed = Lexer.lex(code, filepath, programType);
        Utils.info("Lexer  OK.");

        Token[] parsed = Parser.parse(lexed, filepath);
        Utils.info("Parser OK.");

        if (transpile) {
            int           pad = 0;
            StringBuilder sb  = new StringBuilder();
            for (int i = 0; i < parsed.length; i++) {
                Token t = parsed[i];
                if (t.type.ordinal() < 6) {
                    sb.append(t.repr().repeat(t.num));
                } else if (t.type == Token.Type.JEZ) {
                    if (t.num - parsed[t.num].num < 5) {
                        sb.append(t.repr());
                        i++;
                        while (i < t.num) {
                            sb.append(parsed[i].repr().repeat(parsed[i].num));
                            i++;
                        }
                        sb.append(parsed[i].repr());
                    } else {
                        sb.append('\n').append("  ".repeat(pad++)).append(t.repr()).append('\n').append("  ".repeat(pad));
                    }
                } else if (t.type == Token.Type.JNZ) {
                    sb.append('\n').append("  ".repeat(--pad)).append(t.repr()).append('\n').append("  ".repeat(pad));
                }
            }
            FileSystem.saveFile(transpiledFilepath, sb.toString());
            System.exit(0);
        }

        Interpreter.loadProgram(parsed, programType);
        if (!profiling) {
            while (!Interpreter.finished) Interpreter.execute();
        } else {
            int[]  counter = new int[Token.Type.values().length];
            long[] timer   = new long[Token.Type.values().length];

            long executionTime;
            long prevExecutionTime = System.nanoTime();
            while (!Interpreter.finished) {
                int ord = parsed[Interpreter.ip].type.ordinal();
                Interpreter.execute();
                executionTime = System.nanoTime();
                timer[ord] += executionTime - prevExecutionTime;
                counter[ord]++;
                prevExecutionTime = executionTime;
            }

            System.out.println();
            for (int i = 0; i < counter.length; i++) {
                System.out.printf("Token '%s' was encountered % 12d times and took % 12f secs%n", Token.Type.values()[i], counter[i], timer[i] * 0.000000001f);
            }
            System.out.printf("Parsed tokens: %d%n", parsed.length);
            System.out.printf("Total tokens: %d%n", Arrays.stream(counter).sum());
            System.out.printf("Total time: %f secs%n", Arrays.stream(timer).sum() * 0.000000001f);
        }
    }
}
