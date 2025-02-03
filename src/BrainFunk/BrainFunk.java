package BrainFunk;

import java.io.File;
import java.util.Arrays;
import nootovich.nglib.NGFileSystem;
import nootovich.nglib.NGUtils;

public class BrainFunk {

    public static void main(String[] args) {
        boolean profiling          = false;
        boolean transpile          = false;
        String  transpiledFilepath = null;

        if (args.length < 1) {
            NGUtils.error("No file was provided. Please provide a `.bf`, `.bfn` or `.bfnx` file as a command line argument.");
        }

        String   filepath      = args[0];
        String   filename      = new File(filepath).getName();
        String[] filenameParts = filename.split("\\.");
        String   extension     = filenameParts[filenameParts.length - 1];
        if (!extension.equals("bf") && !extension.equals("bfn") && !extension.equals("bfnx"))
            NGUtils.error("Invalid file type `%s`. Please provide a `.bf`, `.bfn` or `.bfnx` file as a command line argument.");

        for (int i = 1; i < args.length; i++) {
            switch (args[i]) {
                case "-p", "--profiling" -> profiling = true;
                case "-t", "--transpile" -> {
                    if (i == args.length - 1) {
                        NGUtils.error("No name for transpiled file was provided");
                    }
                    transpile          = true;
                    transpiledFilepath = "./%s.bf".formatted(args[++i]);
                }
                default -> NGUtils.error("Unknown argument " + args[i]);
            }
        }

        NGUtils.info("Running %s program.".formatted(filename));

        String  code  = NGFileSystem.loadFile(filepath);
        Token[] lexed = Lexer.lex(code, filepath);
        NGUtils.info("Lexer  OK.");

        Op[] parsed = Parser.parse(lexed, 0);
        NGUtils.info("Parser OK.");

        // if (transpile) {
        //     int           pad = 0;
        //     StringBuilder sb  = new StringBuilder();
        //     for (int i = 0; i < parsed.length; i++) {
        //         Token t = parsed[i];
        //         if (t.type.ordinal() < 6) {
        //             sb.append(t.repr().repeat(t.num));
        //         } else if (t.type == Token.Type.LBRACKET) {
        //             if (t.num - parsed[t.num].num < 5) {
        //                 sb.append(t.repr());
        //                 i++;
        //                 while (i < t.num) {
        //                     sb.append(parsed[i].repr().repeat(parsed[i].num));
        //                     i++;
        //                 }
        //                 sb.append(parsed[i].repr());
        //             } else {
        //                 sb.append('\n').append("  ".repeat(pad++)).append(t.repr()).append('\n').append("  ".repeat(pad));
        //             }
        //         } else if (t.type == Token.Type.RBRACKET) {
        //             sb.append('\n').append("  ".repeat(--pad)).append(t.repr()).append('\n').append("  ".repeat(pad));
        //         }
        //     }
        //     NGFileSystem.saveFile(transpiledFilepath, sb.toString());
        //     System.exit(0);
        // }

        Interpreter.loadProgram(parsed);
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
            System.out.printf("Executed tokens: %d%n", Arrays.stream(counter).sum());
            System.out.printf("Total time: %f secs%n", Arrays.stream(timer).sum() * 1e-9);
        }
    }
}
