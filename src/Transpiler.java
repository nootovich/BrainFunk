import java.nio.file.Path;
import java.util.HashMap;

public class Transpiler {

    private static final String                  TRANSPILED_FOLDER = "./transpiled/";
    private static       HashMap<String, String> patterns          = new HashMap<>();

    public static void main(String[] args) {
        if (args.length == 0) Main.exit("No argument was provided!");
        if (Path.of(args[0]).getFileName().toString().endsWith(".bfx")) {
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
        for (int i = 0; i < dataLen; i++) {
            char c = data.charAt(i);
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
            } else if (c != ':' && c != ' ') Main.exit("Unexpected character '"+c+"'\nFrom: "+i);
            switch (c) {
                case '+', '-', '>', '<', '[', ']', '.', ',', '\n' -> {
                    if (repetitionCount == 0) output.append(c);
                    else output.append(String.valueOf(c).repeat(repetitionCount));
                }
                case ':' -> i = addPattern(data, i, name.toString());
                case ' ' -> {
                    if (repetitionCount == 0) output.append(unfoldPattern(name.toString()));
                    else for (int j = 0; j < repetitionCount; j++) output.append(unfoldPattern(name.toString()));
                }
                case '"' -> {
                    StringBuilder t = new StringBuilder();
                    for (int j = i+1; j < dataLen; j++) {
                        char g = data.charAt(j);
                        if (g == '"') break;
                        if (j == dataLen-1) Main.exit("Unmatched double-quotes!\nFrom: "+i);
                        t.append(g);
                    }
                    String r = t.toString();
                    for (int j = 0; j < r.length(); j++) output.append("[-]").append("+".repeat((int) r.charAt(j)&0xFF)).append(">");
                    i += r.length()+1;
                }
                default -> Main.exit("Unknown character '"+c+"'");
            }
            repetitionCount = 0;
            nameStarted     = false;
            name.setLength(0);
        }
        return output.toString();
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