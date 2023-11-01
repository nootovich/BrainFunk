import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public class Testing {

    static final String TESTING_DIR       = "./tests";
    static final String EXPECTED_DIR      = TESTING_DIR+"/expected";
    static final String EXPECTED_SRC_EXT  = ".src";
    static final String EXPECTED_PREP_EXT = ".prep";
    static final String EXPECTED_IN_EXT   = ".in";
    static final String EXPECTED_OUT_EXT  = ".out";
    static final String EXPECTED_EXIT_EXT = ".exit";

    public static void main(String[] args) {
        Main.TESTING = true;
        try {
            Stream<Path> files = Files.list(Path.of(TESTING_DIR)).filter(f -> !Files.isDirectory(f));
            if (args.length > 0) files = files.filter(f -> f.getFileName().toString().equals(args[0]));
            files.forEach(f -> {
                try {
                    Main.reset();

                    String fileName = f.getFileName().toString();
                    String basePath = EXPECTED_DIR+"/"+fileName.split("\\.")[0];

                    String actualData = Files.readString(f);
                    checkSource(fileName, Path.of(basePath+EXPECTED_SRC_EXT), actualData);

                    String preprocessedData = Main.preprocessData(actualData);
                    checkPreprocessed(fileName, Path.of(basePath+EXPECTED_PREP_EXT), preprocessedData);

                    loadInputData(Path.of(basePath+EXPECTED_IN_EXT));
                    String outputData = Main.executeChunk(preprocessedData, false);
                    checkOutput(fileName, Path.of(basePath+EXPECTED_OUT_EXT), outputData);

                    checkInput(fileName, Path.of(basePath+EXPECTED_IN_EXT), Main.inputMemory.toString());

                    checkExit(fileName, Path.of(basePath+EXPECTED_EXIT_EXT), Main.exitCode);

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
            Files.createFile(srcPath);
            Files.writeString(srcPath, actualData);
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
            Files.createFile(prepPath);
            Files.writeString(prepPath, actualData);
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
            Files.createFile(outPath);
            Files.writeString(outPath, actualData);
            System.out.println(fileName+" ".repeat(32-fileName.length())+"output SAVED");
        }
    }

    private static void loadInputData(Path inPath) throws IOException {
        if (Files.exists(inPath)) {
            char[] inputs = Files.readString(inPath).toCharArray();
            for (char c: inputs) Main.inputBuffer.add((byte) c);
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
        } else {
            Files.createFile(inPath);
            Files.writeString(inPath, actualData);
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
            Files.createFile(exitPath);
            Files.write(exitPath, new byte[]{actualData});
            System.out.println(fileName+" ".repeat(29-fileName.length())+"exit code SAVED");
        }
    }
}
