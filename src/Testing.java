import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Stream;

public class Testing {

    static final String TESTING_DIR        = "./tests/";
    static final String EXPECTED_DIR       = TESTING_DIR+"expected/";
    static final String EXPECTED_SRC_FILE  = "src";
    static final String EXPECTED_PREP_FILE = "prep";
    static final String EXPECTED_TRSP_FILE = "trsp";
    static final String EXPECTED_IN_FILE   = "in";
    static final String EXPECTED_OUT_FILE  = "out";
    static final String EXPECTED_EXIT_FILE = "exit";

    public static void main(String[] args) {
        Interpreter.TESTING     = true;
        Interpreter.CONSOLE_OUT = false;
        boolean updateTests = args.length > 0 && args[0].equals("-update");
        if (updateTests) deleteOldTests(EXPECTED_DIR);
        Stream<Path> files = getBFFiles(TESTING_DIR);
        if (args.length > 0 && !updateTests) files = files.filter(f -> f.getFileName().toString().equals(args[0]));
        files.forEach(f -> test(f, updateTests));
    }

    private static void test(Path f, boolean updateTests) {
        Interpreter.reset();
        String  fileName = f.getFileName().toString();
        String  basePath = EXPECTED_DIR+fileName.split("\\.")[0]+'/';
        Path    srcPath  = Path.of(basePath+EXPECTED_SRC_FILE);
        Path    prepPath = Path.of(basePath+EXPECTED_PREP_FILE);
        Path    trspPath = Path.of(basePath+EXPECTED_TRSP_FILE);
        Path    inPath   = Path.of(basePath+EXPECTED_IN_FILE);
        Path    outPath  = Path.of(basePath+EXPECTED_OUT_FILE);
        Path    exitPath = Path.of(basePath+EXPECTED_EXIT_FILE);
        boolean bfnx     = fileName.endsWith(".bfnx");
        boolean bfn      = fileName.endsWith(".bfn");
        try {
            String actualData = Files.readString(f);
            if (!updateTests) checkSource(fileName, srcPath, actualData);
            else FileSystem.saveFile(srcPath, actualData);

            String parsedData = bfnx ? Parser.parseBrainFunkExtended(actualData) :
                                bfn ? Parser.parseBrainFunk(actualData)
                                    : Parser.parsePureBF(actualData);
            if (!updateTests) checkPreprocessed(fileName, prepPath, parsedData);
            else FileSystem.saveFile(prepPath, parsedData);

            loadInputData(inPath);
            if (bfnx) Interpreter.executeBrainFunkExtended(parsedData);
            else if (bfn) Interpreter.executeBrainFunk(parsedData);
            else Interpreter.executeBF(parsedData);
            String outputData = Interpreter.output.toString();
            if (!updateTests) checkOutput(fileName, outPath, outputData);
            else FileSystem.saveFile(outPath, outputData);

            if (!bfnx) {
                Interpreter.reset();
                String transpiledData = Transpiler.transpile(parsedData);
                if (!updateTests) checkTranspiled(fileName, trspPath, transpiledData);
                else FileSystem.saveFile(trspPath, transpiledData);
                loadInputData(inPath);
                Interpreter.executeBF(Parser.parseBrainFunk(transpiledData));
                String transpiledOutData = Interpreter.output.toString();
                checkOutput(fileName, outPath, transpiledOutData);
            }

            if (!updateTests) checkInput(fileName, inPath, Interpreter.inputMemory.toString());
            else if (!Interpreter.inputMemory.isEmpty()) FileSystem.saveFile(inPath, Interpreter.inputMemory.toString());

            if (!updateTests && bfnx) checkExit(fileName, exitPath, Interpreter.exitCode);
            else if (bfnx) FileSystem.saveFile(exitPath, String.valueOf((char) Interpreter.exitCode));
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    private static Stream<Path> getBFFiles(String dir) {
        try {
            return Files.list(Path.of(dir)).filter(f -> {
                String name = f.getFileName().toString();
                return !Files.isDirectory(f) && (name.endsWith(".bf") || name.endsWith(".bfn") || name.endsWith(".bfnx"));
            });
        } catch (IOException e) {
            System.out.println(e);
            return Arrays.stream((new Path[0])).limit(0);
        }
    }

    private static void deleteOldTests(String dir) {
        try {
            Files.list(Path.of(dir)).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void checkSource(String fileName, Path srcPath, String actualData) throws IOException {
        if (Files.exists(srcPath)) {
            String expectedData = Files.readString(srcPath);
            if (actualData.equals(expectedData)) {
                System.out.println(fileName+" ".repeat(32-fileName.length())+"source OK");
            } else {
                System.out.println(fileName+" ".repeat(32-fileName.length())+"source doesn't match!");
                String[] expectedLines = expectedData.split("\n", -1);
                String[] actualLines   = actualData.split("\n", -1);
                for (int i = 0; i < expectedLines.length || i < actualLines.length; i++) {
                    int pad = (int) Math.ceil(4.f-Math.log10(Math.max(i, 2)));
                    if (i >= actualLines.length) {
                        System.out.println(" ".repeat(pad)+i+"   ACTUAL:{NULL}");
                        System.out.println(" ".repeat(pad)+i+" EXPECTED:"+expectedLines[i]);
                    } else if (i >= expectedLines.length) {
                        System.out.println(" ".repeat(pad)+i+"   ACTUAL:"+actualLines[i]);
                        System.out.println(" ".repeat(pad)+i+" EXPECTED:{NULL}");
                    } else if (!actualLines[i].equals(expectedLines[i])) {
                        System.out.println(" ".repeat(pad)+i+"   ACTUAL:"+actualLines[i]);
                        System.out.println(" ".repeat(pad)+i+" EXPECTED:"+expectedLines[i]);
                        for (int j = 0; j < actualLines[i].length() || j < expectedLines[i].length(); j++) {
                            if (j == expectedLines[i].length() || j == actualLines[i].length() || actualLines[i].charAt(j) != expectedLines[i].charAt(j)) {
                                System.out.println(" ".repeat(15+j)+"^");
                                break;
                            }
                        }
                    }
                }
            }
        } else {
            FileSystem.saveFile(srcPath, actualData);
            System.out.println(fileName+" ".repeat(32-fileName.length())+"source SAVED");
        }
    }

    private static void checkPreprocessed(String fileName, Path prepPath, String actualData) throws IOException {
        if (Files.exists(prepPath)) {
            String expectedData = Files.readString(prepPath);
            if (actualData.equals(expectedData)) {
                System.out.println(fileName+" ".repeat(26-fileName.length())+"preprocessed OK");
            } else {
                System.out.println(fileName+" ".repeat(26-fileName.length())+"preprocessed doesn't match!");
                for (int i = 0; i < expectedData.length() || i < actualData.length(); i++) {
                    if (i == expectedData.length() || i == actualData.length() || expectedData.charAt(i) != actualData.charAt(i)) {
                        System.out.println("  ACTUAL:"+actualData.substring(Math.max(i-10, 0), Math.min(i+20, actualData.length())));
                        System.out.println("EXPECTED:"+expectedData.substring(Math.max(i-10, 0), Math.min(i+20, expectedData.length())));
                        System.out.println(" ".repeat(19)+"^");
                        break;
                    }
                }
            }
        } else {
            FileSystem.saveFile(prepPath, actualData);
            System.out.println(fileName+" ".repeat(26-fileName.length())+"preprocessed SAVED");
        }
    }

    private static void checkOutput(String fileName, Path outPath, String actualData) throws IOException {
        if (Files.exists(outPath)) {
            String expectedData = Files.readString(outPath);
            if (actualData.equals(expectedData)) {
                System.out.println(fileName+" ".repeat(32-fileName.length())+"output OK");
            } else {
                System.out.println(fileName+" ".repeat(32-fileName.length())+"output doesn't match!");
                String[] expectedLines = expectedData.split("\n", -1);
                String[] actualLines   = actualData.split("\n", -1);
                for (int i = 0; i < expectedLines.length || i < actualLines.length; i++) {
                    int pad = (int) Math.ceil(4.f-Math.log10(Math.max(i, 2)));
                    if (i >= actualLines.length) {
                        System.out.println(" ".repeat(pad)+i+"   ACTUAL:{NULL}");
                        System.out.println(" ".repeat(pad)+i+" EXPECTED:"+expectedLines[i]);
                    } else if (i >= expectedLines.length) {
                        System.out.println(" ".repeat(pad)+i+"   ACTUAL:"+actualLines[i]);
                        System.out.println(" ".repeat(pad)+i+" EXPECTED:{NULL}");
                    } else if (!actualLines[i].equals(expectedLines[i])) {
                        System.out.println(" ".repeat(pad)+i+"   ACTUAL:"+actualLines[i]);
                        System.out.println(" ".repeat(pad)+i+" EXPECTED:"+expectedLines[i]);
                        for (int j = 0; j < actualLines[i].length() || j < expectedLines[i].length(); j++) {
                            if (j == expectedLines[i].length() || j == actualLines[i].length() || actualLines[i].charAt(j) != expectedLines[i].charAt(j)) {
                                System.out.println(" ".repeat(15+j)+"^");
                                break;
                            }
                        }
                    }
                }
            }
        } else {
            FileSystem.saveFile(outPath, actualData);
            System.out.println(fileName+" ".repeat(32-fileName.length())+"output SAVED");
        }
    }

    private static void checkTranspiled(String fileName, Path trspPath, String actualData) throws IOException {
        if (Files.exists(trspPath)) {
            String expectedData = Files.readString(trspPath);
            if (actualData.equals(expectedData)) {
                System.out.println(fileName+" ".repeat(28-fileName.length())+"transpiled OK");
            } else {
                System.out.println(fileName+" ".repeat(28-fileName.length())+"transpiled doesn't match!");
                String[] expectedLines = expectedData.split("\n", -1);
                String[] actualLines   = actualData.split("\n", -1);
                for (int i = 0; i < expectedLines.length || i < actualLines.length; i++) {
                    int pad = (int) Math.ceil(4.f-Math.log10(Math.max(i, 2)));
                    if (i >= actualLines.length) {
                        System.out.println(" ".repeat(pad)+i+"   ACTUAL:{NULL}");
                        System.out.println(" ".repeat(pad)+i+" EXPECTED:"+expectedLines[i]);
                    } else if (i >= expectedLines.length) {
                        System.out.println(" ".repeat(pad)+i+"   ACTUAL:"+actualLines[i]);
                        System.out.println(" ".repeat(pad)+i+" EXPECTED:{NULL}");
                    } else if (!actualLines[i].equals(expectedLines[i])) {
                        System.out.println(" ".repeat(pad)+i+"   ACTUAL:"+actualLines[i]);
                        System.out.println(" ".repeat(pad)+i+" EXPECTED:"+expectedLines[i]);
                        for (int j = 0; j < actualLines[i].length() || j < expectedLines[i].length(); j++) {
                            if (j == expectedLines[i].length() || j == actualLines[i].length() || actualLines[i].charAt(j) != expectedLines[i].charAt(j)) {
                                System.out.println(" ".repeat(15+j)+"^");
                                break;
                            }
                        }
                    }
                }
            }
        } else {
            FileSystem.saveFile(trspPath, actualData);
            System.out.println(fileName+" ".repeat(32-fileName.length())+"transpiled SAVED");
        }
    }

    private static void loadInputData(Path inPath) throws IOException {
        if (Files.exists(inPath)) {
            char[] inputs = Files.readString(inPath).toCharArray();
            for (char c: inputs) Interpreter.inputBuffer.add((byte) c);
        }
    }

    private static void checkInput(String fileName, Path inPath, String actualData) throws IOException {
        if (Files.exists(inPath)) {
            String expectedData = Files.readString(inPath);
            if (actualData.equals(expectedData)) {
                System.out.println(fileName+" ".repeat(33-fileName.length())+"input OK");
            } else {
                System.out.println(fileName+" ".repeat(33-fileName.length())+"input doesn't match!");
                for (int i = 0; i < expectedData.length() || i < actualData.length(); i++) {
                    if (i == expectedData.length() || i == actualData.length() || expectedData.charAt(i) != actualData.charAt(i)) {
                        System.out.println("  ACTUAL:"+actualData.substring(Math.max(i-10, 0), Math.min(i+20, actualData.length())));
                        System.out.println("EXPECTED:"+expectedData.substring(Math.max(i-10, 0), Math.min(i+20, expectedData.length())));
                        System.out.println(" ".repeat(19)+"^");
                        break;
                    }
                }
            }
        } else if (!actualData.isEmpty()) {
            FileSystem.saveFile(inPath, actualData);
            System.out.println(fileName+" ".repeat(33-fileName.length())+"input SAVED");
        }
    }

    private static void checkExit(String fileName, Path exitPath, byte actualData) throws IOException {
        if (Files.exists(exitPath)) {
            byte expectedData = Files.readAllBytes(exitPath)[0];
            if (actualData == expectedData) {
                System.out.println(fileName+" ".repeat(29-fileName.length())+"exit code OK");
            } else {
                System.out.println(fileName+" ".repeat(29-fileName.length())+"exit code doesn't match!"
                                   +"\n  ACTUAL:"+actualData+"\nEXPECTED:"+expectedData+"\n         ^");
            }
        } else {
            FileSystem.saveFile(exitPath, String.valueOf((char) actualData));
            System.out.println(fileName+" ".repeat(29-fileName.length())+"exit code SAVED");
        }
    }
}
