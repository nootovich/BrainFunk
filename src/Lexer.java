import java.io.File;
import java.util.ArrayList;

public class Lexer {

    private static String  filename;
    private static boolean showTokens = false;

    public static Token[] lexFile(String filepath) {
        filename   = new File(filepath).getName();
        showTokens = Main.showTokens;
        String rawData = FileSystem.loadFile(filepath);
        if (rawData == null) error("Could not get file data.");
        String[]         lines  = rawData.split("\n");
        ArrayList<Token> tokens = new ArrayList<>();
        for (int row = 0; row < lines.length; row++) tokens.addAll(lexLine(lines[row], row));
        return tokens.toArray(new Token[0]);
    }

    protected static ArrayList<Token> lexLine(String line, int row) {
        ArrayList<Token> tokens = new ArrayList<>();
        for (int col = 0; col < line.length(); col++) {
            char c = line.charAt(col);

            Token.Type tokenType = null;
            Token      tk        = null;

            // VANILLA
            if (c == '+') tokenType = Token.Type.ADD;
            else if (c == '-') tokenType = Token.Type.SUB;
            else if (c == '>') tokenType = Token.Type.PTRADD;
            else if (c == '<') tokenType = Token.Type.PTRSUB;
            else if (c == '[') tokenType = Token.Type.WHILE;
            else if (c == ']') tokenType = Token.Type.ENDWHILE;
            else if (c == '.') tokenType = Token.Type.WRITE;
            else if (c == ',') tokenType = Token.Type.READ;
            if (tokenType != null) {tk = new Token(tokenType, filename, row, col);}

            // BFN STUFF
            else if (c == '/' && col < line.length()-1 && line.charAt(col+1) == '/') break;
            else if (c == '$') tk = new Token(Token.Type.POINTER, filename, row, col);
            else if (c == '#') tk = new Token(Token.Type.RETURN, filename, row, col);
            else if (c == '"') {
                int start = col;
                while (col < line.length()-1 && line.charAt(++col) != '"') {}
                tk = new Token(Token.Type.STRING, filename, row, start);
                if (line.charAt(col) != '"' || col-start < 1) error("Unfinished string literal at "+tk);
                tk.strValue = line.substring(start+1, col);
            } else if (Character.isDigit(c)) {
                int start = col;
                int val   = c-'0';
                while (col < line.length()-1 && Character.isDigit(line.charAt(col+1))) val = val*10+line.charAt(++col)-'0';
                tk = new Token(Token.Type.NUMBER, filename, row, start);
                if (val < 0) error("Invalid value for a `NUMBER` token `"+val+"` at "+tk);
                tk.numValue = val;
            } else if (Character.isLetter(c)) {
                int start = col;
                for (; col < line.length(); col++) if (!Character.isLetterOrDigit(line.charAt(col))) break;
                if (col-start < 0) error("Unfinished macro definition at "+new Token(Token.Type.ERROR, filename, row, col));
                if (col < line.length() && line.charAt(col) == ':') {
                    tk          = new Token(Token.Type.MACRODEF, filename, row, start);
                    tk.strValue = line.substring(start, col);
                    start       = ++col;
                    for (; col < line.length(); col++) if (line.charAt(col) == ';') break;
                    if (col == line.length() || line.charAt(col) != ';' || col-start < 0) {tk.col = col; error("Unfinished macro body at "+tk);}
                    showTokens     = false;
                    tk.macroTokens = lexLine(line.substring(start, col+1), row).toArray(new Token[0]);
                    for (Token t: tk.macroTokens) t.col += start;
                    showTokens = Main.showTokens;
                } else {
                    tk          = new Token(Token.Type.MACRO, filename, row, start);
                    tk.strValue = line.substring(start, col);
                    col--;
                }
            }
            if (tk != null) tokens.add(tk);
            if (showTokens && tk != null) System.out.println(tk);
        }
        return tokens;
    }

    private static void error(String message) {
        StackTraceElement errSrc = Thread.currentThread().getStackTrace()[2];
        System.out.printf("%s:%d [ERROR]: %s%n", errSrc.getFileName(), errSrc.getLineNumber(), message);
        System.exit(1);
    }
}

