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
        return lexData(rawData, 0, 0).toArray(new Token[0]);
    }

    protected static ArrayList<Token> lexData(String data, int rowOffset, int colOffset) {
        String[]         lines  = data.split("\n");
        ArrayList<Token> tokens = new ArrayList<>();
        for (int dataRow = 0; dataRow < lines.length; dataRow++) {
            int    row  = dataRow + rowOffset;
            String line = lines[dataRow];

            for (int dataCol = 0; dataCol < line.length(); dataCol++) {
                int col = dataCol + colOffset;
                char c = line.charAt(dataCol);

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
                else if (c == '/' && dataCol < line.length() - 1 && line.charAt(dataCol + 1) == '/') break;
                else if (c == '$') tk = new Token(Token.Type.POINTER, filename, row, col);
                else if (c == '#') tk = new Token(Token.Type.RETURN, filename, row,  col);
                else if (c == ':') error("Unfinished macro definition at " + new Token(Token.Type.ERROR, filename, row, col));
                else if (c == '"') {
                    int start = dataCol;
                    while (dataCol < line.length() - 1 && line.charAt(++dataCol) != '"') {}
                    tk = new Token(Token.Type.STRING, filename, row, start + colOffset);
                    if (line.charAt(dataCol) != '"' || dataCol - start < 1) error("Unfinished string literal at " + tk);
                    tk.strValue = line.substring(start + 1, dataCol);
                } else if (Character.isDigit(c)) {
                    int start = dataCol;
                    int val   = c - '0';
                    while (dataCol < line.length() - 1 && Character.isDigit(line.charAt(dataCol + 1))) val = val * 10 + line.charAt(++dataCol) - '0';
                    tk = new Token(Token.Type.NUMBER, filename, row, start + colOffset);
                    if (val < 0) error("Invalid value for a `NUMBER` token `" + val + "` at " + tk);
                    tk.numValue = val;
                } else if (Character.isLetter(c) || c == '_') {
                    int startCol = dataCol;
                    for (; dataCol < line.length(); dataCol++) {
                        char cc = line.charAt(dataCol);
                        if (!Character.isLetterOrDigit(cc) && cc != '_') break;
                    }
                    if (dataCol - startCol < 0) error("Unfinished macro definition at " + new Token(Token.Type.ERROR, filename, row, col));
                    if (dataCol < line.length() && line.charAt(dataCol) == ':') {
                        tk          = new Token(Token.Type.MACRODEF, filename, row, startCol + colOffset);
                        tk.strValue = line.substring(startCol, dataCol);
                        startCol    = ++dataCol;
                        showTokens     = false;
                        ArrayList<Token> macroTokens     = new ArrayList<>();
                        for (int col2 = startCol; dataRow < lines.length; dataRow++) {
                            line = lines[dataRow];
                            for (; dataCol < line.length(); dataCol++) {
                                if (line.charAt(dataCol) == ';') break;
                            }
                            macroTokens.addAll(lexData(line.substring(col2, dataCol), dataRow, col2 + colOffset));
                            if (dataCol < line.length() && line.charAt(dataCol) == ';') break;
                            dataCol = 0;
                            col2    = 0;
                        }
                        tk.macroTokens = macroTokens.toArray(new Token[0]);
                        showTokens = Main.showTokens;
                    } else {
                        tk          = new Token(Token.Type.MACRO, filename, row, startCol + colOffset);
                        tk.strValue = line.substring(startCol, dataCol);
                        dataCol--;
                    }
                }
                if (tk != null) tokens.add(tk);
                if (showTokens && tk != null) System.out.println(tk);
            }
        }
        return tokens;
    }

    private static void error(String message) {
        StackTraceElement errSrc = Thread.currentThread().getStackTrace()[2];
        System.out.printf("%s:%d [ERROR]: %s%n", errSrc.getFileName(), errSrc.getLineNumber(), message);
        System.exit(1);
    }
}

