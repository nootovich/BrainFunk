package BrainFunk;

import BrainFunk.Token.Type;
import java.util.Stack;
import nootovich.nglib.NGUtils;

import static BrainFunk.BrainFunk.ProgramType;

public class Lexer {

    // TODO: add back checks related to 'programType'
    public static Token[] lex(String data, String filepath, ProgramType programType) {
        Stack<Token> lexed = new Stack<>();

        int row = 0;
        int col = 0;
        int bol = 0;

        char[] dataChars = data.toCharArray();
        for (int i = 0; i < dataChars.length; i++) {
            boolean skip = false;
            col = i - bol;
            char c = dataChars[i];
            Token token = switch (c) {
                case ' ', '\t', '\r' -> { skip = true; yield null; }
                case '\n' -> { bol = i + 1; row++; skip = true; yield null; }
                case '+' -> new Token(Type.PLUS, filepath, row, col);
                case '-' -> new Token(Type.MINUS, filepath, row, col);
                case '>' -> new Token(Type.GREATER, filepath, row, col);
                case '<' -> new Token(Type.LESS, filepath, row, col);
                case ',' -> new Token(Type.COMMA, filepath, row, col);
                case '.' -> new Token(Type.DOT, filepath, row, col);
                case '[' -> new Token(Type.LBRACKET, filepath, row, col);
                case ']' -> new Token(Type.RBRACKET, filepath, row, col);
                case '$' -> new Token(Type.PTR, filepath, row, col);
                case '#' -> new Token(Type.RET, filepath, row, col);
                case ':' -> new Token(Type.COL, filepath, row, col);
                case ';' -> new Token(Type.SCL, filepath, row, col);
                case '!' -> new Token(Type.IMP, filepath, row, col);
                case '@' -> new Token(Type.SYS, filepath, row, col);
                case '/' -> {
                    if (i < dataChars.length - 1 && dataChars[i + 1] == '/') {
                        skip = true;
                        while (dataChars[++i] != '\n') { }
                        i--;
                    }
                    yield null;
                }
                case '"' -> {
                    StringBuilder sb = new StringBuilder();
                    for (i++; i < dataChars.length; i++) {
                        if (dataChars[i] == '"') break;
                        else if (i == '\n' || i == dataChars.length - 1) {
                            yield NGUtils.error("Unfinished string literal at: " + new Token(Type.ERR, filepath, row, col));
                        }
                        sb.append(dataChars[i]);
                    }
                    yield new Token(Type.STR, sb.toString(), filepath, row, col);
                }
                default -> null;
            };
            if (token != null) {
                lexed.push(token);
                continue;
            } else if (skip) {
                continue;
            } else if (Character.isDigit(c)) {
                int num = c - '0';
                for (i++; i < dataChars.length; i++) {
                    if (!Character.isDigit(dataChars[i])) {
                        i--;
                        break;
                    }
                    num = num * 10 + dataChars[i] - '0';
                }
                lexed.push(new Token(Type.NUM, num, filepath, row, col));
                continue;
            } else if (Character.isLetter(c)) {
                StringBuilder sb = new StringBuilder().append(c);
                for (i++; i < dataChars.length; i++) {
                    if (!Character.isLetterOrDigit(dataChars[i]) && dataChars[i] != '_') {
                        i--;
                        break;
                    }
                    sb.append(dataChars[i]);
                }
                lexed.push(new Token(Type.WRD, sb.toString(), filepath, row, col));
                continue;
            }
            NGUtils.error("Undefined token '%c'. This is probably a bug in Lexer. %s".formatted(c, new Token(Type.ERR, filepath, row, col)));
        }

        Token[] result = new Token[lexed.size()];
        for (int i = result.length - 1; i >= 0; i--) result[i] = lexed.pop();
        lexed = new Stack<>();

        return result;
    }
}

