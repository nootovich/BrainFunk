import java.io.File;
import java.util.ArrayList;
import java.util.Stack;

public class Lexer {

    public static Token[] lex(String data, String filename) {
        Stack<Token> tokens = new Stack<>();
        String[]     lines  = data.split("\n", -1);
        for (int row = 0; row < lines.length; row++) {
            String line = lines[row];
            for (int col = 0; col < line.length(); col++) {
                char c = line.charAt(col);
                if (c == ' ' || c == '\n' || c == '\r' || c == '\t') continue;

                else if (c == '/' && col < line.length() - 1 && line.charAt(col + 1) == '/') break;
                else if (c == '+') tokens.push(new Token(Token.Type.INC, filename, row, col));
                else if (c == '-') tokens.push(new Token(Token.Type.DEC, filename, row, col));
                else if (c == '>') tokens.push(new Token(Token.Type.RGT, filename, row, col));
                else if (c == '<') tokens.push(new Token(Token.Type.LFT, filename, row, col));
                else if (c == ',') tokens.push(new Token(Token.Type.INP, filename, row, col));
                else if (c == '.') tokens.push(new Token(Token.Type.OUT, filename, row, col));
                else if (c == '[') tokens.push(new Token(Token.Type.JEZ, filename, row, col));
                else if (c == ']') tokens.push(new Token(Token.Type.JNZ, filename, row, col));
                else if (c == '$') tokens.push(new Token(Token.Type.PTR, filename, row, col));
                else if (c == '#') tokens.push(new Token(Token.Type.RET, filename, row, col));
                else if (c == ':') tokens.push(new Token(Token.Type.COL, filename, row, col));
                else if (c == ';') tokens.push(new Token(Token.Type.SCL, filename, row, col));
                else if (c == '{') tokens.push(new Token(Token.Type.UNSAFEJEZ, filename, row, col));
                else if (c == '}') tokens.push(new Token(Token.Type.UNSAFEJNZ, filename, row, col));

                else if (c == '"') {
                    int           scol = col;
                    StringBuilder sb   = new StringBuilder();
                    for (col++; col < line.length(); col++) {
                        c = line.charAt(col);
                        if (c == '"') break;
                        if (col == line.length() - 1) {
                            Token tk = new Token(Token.Type.ERR, filename, row, scol);
                            Utils.error("Unfinished string literal at: " + tk);
                        }
                        sb.append(c);
                    }
                    Token tk = new Token(Token.Type.STR, filename, row, scol);
                    tk.strValue = sb.toString();
                    tokens.push(tk);
                }

                else if (Character.isDigit(c)) {
                    int scol = col;
                    int num = c - '0';
                    for (col++; col < line.length(); col++) {
                        c = line.charAt(col);
                        if (!Character.isDigit(c)) {
                            col--;
                            break;
                        }
                        num = num*10 + c-'0';
                    }
                    Token tk = new Token(Token.Type.NUM, filename, row, scol);
                    tk.numValue = num;
                    tokens.push(tk);
                }

                else if (Character.isLetter(c)) {
                    int scol = col;
                    StringBuilder sb = new StringBuilder().append(c);
                    for (col++; col<line.length(); col++) {
                        c = line.charAt(col);
                        if(!Character.isLetterOrDigit(c) && c != '_') {
                            col--;
                            break;
                        }
                        sb.append(c);
                    }
                    Token tk = new Token(Token.Type.WRD, filename, row, scol);
                    tk.strValue = sb.toString();
                    tokens.push(tk);
                }

                else {
                   Utils.error("Should be unreachable: "+new Token(Token.Type.ERR, filename, row, col));
                }

            }
        }

        Token[] result = new Token[tokens.size()];
        for (int i = result.length - 1; i >= 0; i--) result[i] = tokens.pop();

        return result;
    }

}

