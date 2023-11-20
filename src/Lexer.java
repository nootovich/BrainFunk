import java.util.ArrayList;

public class Lexer {

    public static Token[] lexFile(String filepath) {
        String           rawData = FileSystem.loadFile(filepath);
        ArrayList<Token> tokens  = new ArrayList<>();
        for (char c: rawData.toCharArray()) {
            if (c == '+') tokens.add(new Token(Token.Type.ADD));
            else if (c == '-') tokens.add(new Token(Token.Type.SUB));
            else if (c == '>') tokens.add(new Token(Token.Type.PTRADD));
            else if (c == '<') tokens.add(new Token(Token.Type.PTRSUB));
            else if (c == '[') tokens.add(new Token(Token.Type.WHILE));
            else if (c == ']') tokens.add(new Token(Token.Type.ENDWHILE));
            else if (c == '.') tokens.add(new Token(Token.Type.WRITE));
            else if (c == ',') tokens.add(new Token(Token.Type.READ));
        }
        return tokens.toArray(new Token[0]);
    }
}

