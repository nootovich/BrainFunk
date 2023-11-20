import java.nio.file.Path;
import java.util.HashMap;
import java.util.Stack;

public class Transpiler {

    private static final String                  TRANSPILED_FOLDER = "./transpiled/";
    private static       HashMap<String, String> patterns          = new HashMap<>();
    private static       int                     pointer           = 0;
    private static       Stack<Integer>          ptrHistory        = new Stack<>();

    public static void main(String[] args) {
        if (args.length == 0) Main.exit("No argument was provided!");
        if (args[0].endsWith(".bfx")) {
            Main.exit("[ERROR]: BrainFunkExtended(.bfx) files can't be transpiled since "
                      +"they use functionality not available in pure bf (like syscalls).");
        }
        String code = Parser.parseBrainFunk(FileSystem.loadFile(args[0]));
        FileSystem.saveFile(Path.of(TRANSPILED_FOLDER+Path.of(args[0]).getFileName()), transpile(code));
    }

    public static String transpile(String data) {
        int           dataLen         = data.length();
        int           repetitionCount = 0;
        boolean       nameStarted     = false;
        StringBuilder name            = new StringBuilder();
        StringBuilder output          = new StringBuilder();
        for (int op = 0; op < dataLen; op++) {
            char c = data.charAt(op);
            if (!nameStarted) {
                if (Character.isDigit(c)) {
                    repetitionCount = repetitionCount*10+c-'0';
                    continue;
                } else if (Character.isLetter(c)) {
                    nameStarted = true;
                    name.append(c);
                    continue;
                }
            } else if (Character.isLetterOrDigit(c)) {
                name.append(c);
                continue;
            } else if (c != ':' && c != ' ') Main.exit("Unexpected character '"+c+"'\nFrom: "+op);
            switch (c) {
                case '>' -> {
                    movePointer(true, repetitionCount);
                    output.append('>');
                }
                case '<' -> {
                    movePointer(false, repetitionCount);
                    output.append('<');
                }
                case '+', '-', '[', ']', '.', ',', '\n' -> {
                    if (repetitionCount == 0) output.append(c);
                    else output.append(String.valueOf(c).repeat(repetitionCount));
                }
                case ':' -> op = addPattern(data, op, name.toString());
                case ' ' -> {
                    if (repetitionCount == 0) output.append(unfoldPattern(name.toString()));
                    else for (int j = 0; j < repetitionCount; j++) output.append(unfoldPattern(name.toString()));
                }
                case '"' -> {
                    StringBuilder t = new StringBuilder();
                    for (int j = op+1; j < dataLen; j++) {
                        char g = data.charAt(j);
                        if (g == '"') break;
                        if (j == dataLen-1) Main.exit("Unmatched double-quotes!\nFrom: "+op);
                        t.append(g);
                    }
                    String r = t.toString();
                    output.append("// push \"").append(r).append("\"\n");
                    for (int j = 0; j < r.length(); j++) output.append("[-]").append("+".repeat((int) r.charAt(j)&0xFF)).append(">");
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
                        Main.exit("ERROR: there is not enough pointer history to go back to.");
                    int target = ptrHistory.pop();
                    if (pointer > target) output.append("<".repeat(pointer-target));
                    else if (pointer < target) output.append(">".repeat(target-pointer));
                    pointer = target;
                }
                default -> Main.exit("Unknown character '"+c+"'");
            }
            repetitionCount = 0;
            nameStarted     = false;
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
        if (data.charAt(i) != ';') Main.exit("Unmatched semicolon!\nFrom: "+start);
        patterns.put(name, pattern.toString());
        return i;
    }

    public static String unfoldPattern(String name) {
        if (patterns.get(name) == null) Main.exit("Pattern '"+name+"' was not found!");
        return "\n// "+name+"\n"+transpile(patterns.get(name))+"\n";
    }

}
