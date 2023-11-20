import java.util.ArrayList;

public class Lexer {

    public static Token[] lexFile(String filepath) {
        String rawData = FileSystem.loadFile(filepath);
        if (rawData == null) error("Could not get file data.");
        String[]         lines  = rawData.split("\n");
        ArrayList<Token> tokens = new ArrayList<>();
        for (int row = 0; row < lines.length; row++) {
            String line = lines[row];
            for (int col = 0; col < line.length(); col++) {
                char c = line.charAt(col);

                Token.Type tokenType = null;
                int        value     = 0;

                // VANILLA
                if (c == '+') tokenType = Token.Type.ADD;
                else if (c == '-') tokenType = Token.Type.SUB;
                else if (c == '>') tokenType = Token.Type.PTRADD;
                else if (c == '<') tokenType = Token.Type.PTRSUB;
                else if (c == '[') tokenType = Token.Type.WHILE;
                else if (c == ']') tokenType = Token.Type.ENDWHILE;
                else if (c == '.') tokenType = Token.Type.WRITE;
                else if (c == ',') tokenType = Token.Type.READ;

                else if (c == '/' && col < line.length()-1 && line.charAt(col+1) == '/') break;
                else if (Character.isDigit(c)) {
                    tokenType = Token.Type.NUMBER;
                    value     = c-'0';
                    while (col < line.length()-1 && Character.isDigit(line.charAt(col+1))) {
                        c     = line.charAt(++col);
                        value = value*10+c-'0';
                    }
                }

                if (tokenType != null) {
                    Token tk = new Token(tokenType, row, col);
                    tokens.add(tk);
                    if (tokenType == Token.Type.NUMBER) tk.value = value;
                }
            }
        }
        return tokens.toArray(new Token[0]);
    }

    private static void error(String message) {
        System.out.printf("[LEXER_ERROR]: %s%n", message);
        System.exit(1);
    }
}

