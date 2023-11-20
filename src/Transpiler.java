import java.nio.file.Path;
import java.util.HashMap;
import java.util.Stack;

public class Transpiler {

    // TODO: for some reason Transpiler is not working correctly but i can't understand why

    private static final String                  TRANSPILED_FOLDER = "./transpiled/";
    private static       HashMap<String, String> patterns          = new HashMap<>();
    private static       int                     pointer           = 0;
    private static       Stack<Integer>          ptrHistory        = new Stack<>();

    public static void main(String[] args) {
        if (args.length == 0) error("No argument was provided!");
        if (args[0].endsWith(".bfx")) {
            error("[ERROR]: BrainFunkExtended(.bfx) files can't be transpiled since "
                  +"they use functionality not available in pure bf (like syscalls).");
        }
        String code = Parser.parseBrainFunk(FileSystem.loadFile(args[0]));
        FileSystem.saveFile(Path.of(TRANSPILED_FOLDER+Path.of(args[0]).getFileName()), transpile(code));
    }

    public static String transpile(String data) {
        int           dataLen     = data.length();
        int           amount      = 0;
        boolean       nameStarted = false;
        StringBuilder name        = new StringBuilder();
        StringBuilder output      = new StringBuilder();
        for (int op = 0; op < dataLen; op++) {
            char c = data.charAt(op);
            if (!nameStarted) {
                if (Character.isDigit(c)) {
                    amount = amount*10+c-'0';
                    continue;
                } else if (Character.isLetter(c)) {
                    nameStarted = true;
                    name.append(c);
                    continue;
                }
            } else if (Character.isLetterOrDigit(c)) {
                name.append(c);
                continue;
            } else if (c != ':' && c != ' ') error("Unexpected character '"+c+"'\nFrom: "+op);
            switch (c) {
                case '>' -> {
                    movePointer(true, amount);
                    output.append(">".repeat(amount > 0 ? amount : 1));
                }
                case '<' -> {
                    movePointer(false, amount);
                    output.append("<".repeat(amount > 0 ? amount : 1));
                }
                case '+', '-', '[', ']', '.', ',' -> {
                    if (amount == 0) output.append(c);
                    else output.append(String.valueOf(c).repeat(amount));
                }
                case ':' -> op = addPattern(data, op, name.toString());
                case ' ' -> {
                    if (amount == 0) output.append(unfoldPattern(name.toString()));
                    else for (int j = 0; j < amount; j++) output.append(unfoldPattern(name.toString()));
                }
                case '"' -> {
                    StringBuilder t = new StringBuilder();
                    for (int j = op+1; j < dataLen; j++) {
                        char g = data.charAt(j);
                        if (g == '"') break;
                        if (j == dataLen-1) error("Unmatched double-quotes!\nFrom: "+op);
                        t.append(g);
                    }
                    String r = t.toString();
                    for (int j = 0; j < r.length(); j++) output.append("[-]").append("+".repeat((int) r.charAt(j)&0xFF)).append(">\n");
                    movePointer(true, r.length());
                    op += r.length()+1;
                }
                case '$' -> {
                    int target = -1;
                    for (int i = op+1; i < dataLen; i++) {
                        char b = data.charAt(i);
                        if (!Character.isDigit(b)) break;
                        if (target == -1) target = 0;
                        target = target*10+b-'0';
                    }
                    if (pointer > target) output.append("<".repeat(pointer-target));
                    else if (pointer < target) output.append(">".repeat(target-pointer));
                    ptrHistory.push(pointer);
                    pointer = target;
                }
                case '#' -> {
                    if (ptrHistory.isEmpty())
                        error("ERROR: there is not enough pointer history to go back to.");
                    int target = ptrHistory.pop();
                    if (pointer > target) output.append("<".repeat(pointer-target));
                    else if (pointer < target) output.append(">".repeat(target-pointer));
                    pointer = target;
                }
                case '_' -> {}
                default -> error("Unknown character '"+c+"'");
            }
            amount      = 0;
            nameStarted = false;
            name.setLength(0);
        }
        return output.toString();
    }

    private static void movePointer(boolean right, int amount) {
        amount = (amount > 0 ? amount : 1);
        amount = (right ? amount : -amount);
        int tapeLen = Interpreter.TAPE_LEN;
        pointer = (((pointer+amount)%tapeLen)+tapeLen)%tapeLen;
    }

    public static int addPattern(String data, int i, String name) {
        int           start   = i;
        StringBuilder pattern = new StringBuilder();
        for (i++; i < data.length(); i++) {
            char c = data.charAt(i);
            if (c == ';') break;
            pattern.append(c);
        }
        if (data.charAt(i) != ';') error("Unmatched semicolon!\nFrom: "+start);
        patterns.put(name, pattern.toString());
        return i;
    }

    public static String unfoldPattern(String name) {
        if (patterns.get(name) == null) error("Pattern '"+name+"' was not found!");
        return "\n"+transpile(patterns.get(name))+"\n";
    }

    private static void error(String message) {
        System.out.printf("[TRANSPILER_ERROR]: %s%n", message);
        System.exit(1);
    }

    private static void error(Token tk, String message) {
        System.out.printf("[TRANSPILER_ERROR]: %s: %s%n", tk, message);
        System.exit(1);
    }
}
