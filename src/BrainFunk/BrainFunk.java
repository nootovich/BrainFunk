package BrainFunk;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import nootovich.nglib.NGFileSystem;
import nootovich.nglib.NGUtils;

public class BrainFunk {

    public static void main(String[] args) {
        boolean profiling = false;

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
                case "-t", "--transpile" -> NGUtils.error("Transpiling to '.bf' is not implemented yet");
                default -> NGUtils.error("Unknown argument " + args[i]);
            }
        }

        NGUtils.info("Running %s program.".formatted(filename));

        String  code  = NGFileSystem.loadFile(filepath);
        Token[] lexed = Lexer.lex(code, filepath);
        NGUtils.info("Lexer  OK.");

        Op[] parsed = Parser.parse(lexed, 0);
        NGUtils.info("Parser OK.");

        // Op[] optimized = Optimizer.optimize(parsed);
        Op[] optimized = parsed;
        NGUtils.info("Optimizer OK.");

        Interpreter.loadProgram(optimized);
        if (!profiling) {
            while (!Interpreter.finished) Interpreter.execute();
        } else {
            HashMap<String, Integer> ptrMovement = new HashMap<>();
            int                      prevPtr     = Interpreter.pointer;

            long[] counter = new long[Op.Type.values().length];
            long[] timer   = new long[Op.Type.values().length];

            long executionTime;
            long prevExecutionTime = System.nanoTime();
            while (!Interpreter.finished) {
                int ord = optimized[Interpreter.ip].type.ordinal();
                Interpreter.execute();
                executionTime = System.nanoTime();
                timer[ord] += executionTime - prevExecutionTime;
                counter[ord]++;
                prevExecutionTime = executionTime;

                if (Interpreter.pointer != prevPtr) {
                    String key   = "%d:%d".formatted(Math.min(prevPtr, Interpreter.pointer), Math.max(prevPtr, Interpreter.pointer));
                    int    value = ptrMovement.getOrDefault(key, 0) + 1;
                    ptrMovement.put(key, value);
                    prevPtr = Interpreter.pointer;
                }
            }

            LinkedHashMap<String, Integer> sortedMap = ptrMovement.entrySet().stream().sorted(
                (a, b) -> b.getValue().compareTo(a.getValue())
            ).collect(
                Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new)
            );
            System.out.println(sortedMap);

            System.out.println();
            for (int i = 0; i < counter.length; i++) {
                System.out.printf("Token '%s' was encountered % 12d times and took % 12f secs%n", Op.Type.values()[i], counter[i], timer[i] * 0.000000001f);
            }
            System.out.printf("Lexed tokens count: %d%n", lexed.length);
            System.out.printf("Parsed operations count: %d%n", parsed.length);
            System.out.printf("Optimized operations count: %d%n", optimized.length);
            System.out.printf("Executed operations count: %d%n", Arrays.stream(counter).sum());
            System.out.printf("Total time: %f secs%n", Arrays.stream(timer).sum() * 1e-9);
        }
    }
}
