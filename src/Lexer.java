import java.util.ArrayList;

public class Lexer {

    public static Token[] lexFile(String filepath) {
        String           rawData = FileSystem.loadFile(filepath);
        String[]         lines   = rawData.split("\n");
        ArrayList<Token> tokens  = new ArrayList<>();
        for (int row = 0; row < lines.length; row++) {
            String line = lines[row];
            for (int col = 0; col < line.length(); col++) {
                Token.Type tokenType = null;

                char c = line.charAt(col);
                if (c == '+') tokenType = Token.Type.ADD;
                else if (c == '-') tokenType = Token.Type.SUB;
                else if (c == '>') tokenType = Token.Type.PTRADD;
                else if (c == '<') tokenType = Token.Type.PTRSUB;
                else if (c == '[') tokenType = Token.Type.WHILE;
                else if (c == ']') tokenType = Token.Type.ENDWHILE;
                else if (c == '.') tokenType = Token.Type.WRITE;
                else if (c == ',') tokenType = Token.Type.READ;

                if (tokenType != null) tokens.add(new Token(tokenType, row, col));
            }
        }
        return tokens.toArray(new Token[0]);
    }
}

