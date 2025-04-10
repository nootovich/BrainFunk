package BrainFunk;

import BrainFunk.Token.Type;
import java.util.Stack;
import nootovich.nglib.NGUtils;

public class Lexer {

    public static Token[] lex(String data, String filepath) {
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

                case ':' -> new Token(Type.COLON, filepath, row, col);
                case ';' -> new Token(Type.SEMICOLON, filepath, row, col);
                case '/' -> {
                    if (i >= dataChars.length - 1 || dataChars[i + 1] != '/') yield null; // new Token(Type.ERR, filepath, row, col);
                    for (int commentStart = i += 2; i < dataChars.length; i++) {
                        if (dataChars[i] == '\n') {
                            String comment = data.substring(commentStart, i--).replace("\r", "");
                            yield new Token(Type.COMMENT, comment, filepath, row, col);
                        }
                    }
                    yield NGUtils.error("Unreachable. Caused by: " + new Token(Type.ERROR, filepath, row, col));
                }

                case '!' -> new Token(Type.EXCLAMATION, filepath, row, col);
                case '@' -> new Token(Type.AT, filepath, row, col);
                case '#' -> new Token(Type.OCTOTHORPE, filepath, row, col);
                case '$' -> new Token(Type.DOLLAR, filepath, row, col);

                case '"' -> {
                    for (int stringStart = ++i; i < dataChars.length && dataChars[i] != '\n'; i++) {
                        if (dataChars[i] == '"') yield new Token(Type.STRING, data.substring(stringStart, i), filepath, row, col);
                    }
                    yield NGUtils.error("Unfinished string literal at: " + new Token(Type.ERROR, filepath, row, col));
                }
                case '\'' -> {
                    // TODO: handle escaping
                    i+=2;
                    if (i >= dataChars.length || dataChars[i] != '\'') {
                        NGUtils.error("Invalid char literal at: " + new Token(Type.ERROR, filepath, row, col));
                    }
                    yield new Token(Type.CHAR, (byte) dataChars[i - 1], filepath, row, col);
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
                lexed.push(new Token(Type.NUMBER, num, filepath, row, col));
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
                lexed.push(new Token(Type.WORD, sb.toString(), filepath, row, col));
                continue;
            }
            NGUtils.error("Undefined token '%c': %s".formatted(c, new Token(Type.ERROR, filepath, row, col)));
        }

        Token[] result = new Token[lexed.size()];
        for (int i = result.length - 1; i >= 0; i--) result[i] = lexed.pop();
        lexed = new Stack<>();

        return result;
    }
}

