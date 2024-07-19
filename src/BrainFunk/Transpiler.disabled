import java.nio.file.Path;
import java.util.Stack;

public class Transpiler {

    private static final String TRANSPILED_FOLDER = "./transpiled/";

    private static int savedCount = -1;
    private static int pointer    = 0;

    private static Stack<Integer> ptrHistory = new Stack<>();

    public static void main(String[] args) {

        if (args.length == 0) {
            error("No argument was provided!");
        }
        if (args[0].endsWith(".bfx")) {
            error("`.bfx` files can't be transpiled since they use functionality not available in pure bf (like syscalls).");
        }

        Token[] sourceTokens = Parser.parseTokens(Lexer.lexFile(args[0]));
        safetyCheck(sourceTokens);
        String transpiledData = transpile(sourceTokens);
        String outputFilename = Path.of(args[0]).getFileName().toString();
        String outputFilepath = TRANSPILED_FOLDER + outputFilename.substring(0, outputFilename.lastIndexOf('.')) + ".bf";
        FileSystem.saveFile(outputFilepath, transpiledData);

        if (args.length == 2 && args[1].equals("-r")) {
            Interpreter.executeBF(Lexer.lexFile(outputFilepath));
        }
    }

    public static String transpile(Token[] tokens) {
        StringBuilder transpiled = new StringBuilder();
        for (Token tk: tokens) {
            switch (tk.type) {
                case ADD, SUB, WHILE, ENDWHILE, WRITE, READ -> transpiled.append(tk.repr().repeat(getCount()));
                case PTRADD -> {
                    int cnt = getCount();
                    transpiled.append(">".repeat(cnt));
                    pointer += cnt;
                }
                case PTRSUB -> {
                    int cnt = getCount();
                    transpiled.append("<".repeat(cnt));
                    pointer -= cnt;
                }

                // TODO: maybe add a transpiler compatible `repr()` function for these two?
                case UNSAFEWHILE -> transpiled.append("[".repeat(getCount()));
                case UNSAFEENDWHILE -> transpiled.append("]".repeat(getCount()));

                case NUMBER -> setCount(tk.numValue);
                case STRING -> {
                    for (int i = 0; i < getCount(); i++) {
                        for (char c: tk.strValue.toCharArray()) {
                            transpiled.append("+".repeat(c)).append(">");
                            pointer++;
                        }
                    }
                }
                case POINTER -> {
                    for (int i = 0; i < getCount(); i++) {
                        ptrHistory.push(pointer);
                        System.out.println("PUSH " + pointer);
                        if (pointer > tk.numValue) {
                            transpiled.append("<".repeat(pointer - tk.numValue));
                            pointer = tk.numValue;
                        } else if (pointer < tk.numValue) {
                            transpiled.append(">".repeat(tk.numValue - pointer));
                            pointer = tk.numValue;
                        }
                    }
                }
                case RETURN -> {
                    for (int i = 0; i < getCount(); i++) {
                        if (ptrHistory.empty()) error("Not enough pointer history for: " + tk);
                        int target = ptrHistory.pop();
                        System.out.println("POP " + target);
                        if (pointer > target) {
                            transpiled.append("<".repeat(pointer - target));
                            pointer = target;
                        } else if (pointer < target) {
                            transpiled.append(">".repeat(target - pointer));
                            pointer = target;
                        }
                    }
                }
                case MACRODEF, MACRO, ERROR -> error("Unexpected token `" + tk.type + "`. Probably a bug in Lexer.");
                case COLON -> {}
                default -> error("Unhandled token type `" + tk.type + "`.");
            }
        }
        return transpiled.toString();
    }

    private static void safetyCheck(Token[] tokens) {
        Stack<Integer> whilePointerHistory = new Stack<>();
        Stack<Integer> pointerHistory      = new Stack<>();
        int            pointerPosition     = 0;
        for (int i = 0; i < tokens.length; i++) {
            Token tk = tokens[i];
            switch (tk.type) {
                case ADD, SUB, READ, WRITE -> getCount();
                case PTRADD -> pointerPosition += getCount();
                case PTRSUB -> pointerPosition -= getCount();
                case WHILE, UNSAFEWHILE -> whilePointerHistory.push(pointerPosition);
                case ENDWHILE -> {
                    if (whilePointerHistory.empty()) error("Not enough while-block pointer history for safetyCheck at: " + tk);
                    int newPointerPosition = whilePointerHistory.pop();
                    if (pointerPosition != newPointerPosition) {
                        info("Safety check failed at: " + tk + "\nPointer should be at `" + newPointerPosition + "` but is at `" + pointerPosition + "`\n");
                    }
                }
                case NUMBER -> setCount(tk.numValue);
                case STRING -> pointerPosition += tk.strValue.length();
                case POINTER -> {
                    pointerHistory.push(pointerPosition);
                    pointerPosition = tk.numValue;
                }
                case RETURN -> {
                    if (pointerHistory.empty()) error("Not enough pointer history for safetyCheck() at: `" + tk + "`");
                    pointerPosition = pointerHistory.pop();
                }
                case UNSAFEENDWHILE -> {
                    if (i + 1 < tokens.length && tokens[i + 1].type == Token.Type.COLON) {
                        if (i + 2 == tokens.length || tokens[i + 2].type != Token.Type.NUMBER) error("Unknown pointer position promise.");
                        pointerPosition = tokens[i + 2].numValue;
                        whilePointerHistory.pop();
                        i += 2;
                    } else {
                        pointerPosition = whilePointerHistory.pop();
                    }
                }
                default -> info("Unhandled token in safetyCheck(): `" + tk.type + "`");
            }
        }
    }

    private static void setCount(int n) {
        if (savedCount >= 0) error("Two consecutive numbers after one another is not supported, this is a bug in Lexer.");
        savedCount = n;
    }

    private static int getCount() {
        if (savedCount < 0) return 1;
        int temp = savedCount;
        savedCount = -1;
        return temp;
    }

    private static void info(String message) {
        StackTraceElement src = Thread.currentThread().getStackTrace()[2];
        System.out.printf("%s:%d [INFO]: %s%n", src.getFileName(), src.getLineNumber(), message);
    }

    private static void error(String message) {
        StackTraceElement errSrc = Thread.currentThread().getStackTrace()[2];
        System.out.printf("%s:%d [ERROR]: %s%n", errSrc.getFileName(), errSrc.getLineNumber(), message);
        System.exit(1);
    }

}
