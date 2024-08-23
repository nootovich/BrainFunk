package BrainFunk;

import java.util.Stack;
import nootovich.nglib.NGUtils;

import static BrainFunk.BrainFunk.ProgramType;

public class Lexer {

    public static Token[] lex(String data, String filepath, ProgramType programType) {
        Stack<Token> lexed = new Stack<>();
        String[]     lines = data.split("\n", -1);
        for (int row = 0; row < lines.length; row++) {
            String line = lines[row];
            for (int col = 0; col < line.length(); col++) {
                char c = line.charAt(col);
                if (c == ' ' || c == '\n' || c == '\r' || c == '\t') continue;

                // BF, BFN, BFNX
                else if (c == '+') lexed.push(new Token(Token.Type.INC, filepath, row, col));
                else if (c == '-') lexed.push(new Token(Token.Type.DEC, filepath, row, col));
                else if (c == '>') lexed.push(new Token(Token.Type.RGT, filepath, row, col));
                else if (c == '<') lexed.push(new Token(Token.Type.LFT, filepath, row, col));
                else if (c == ',') lexed.push(new Token(Token.Type.INP, filepath, row, col));
                else if (c == '.') lexed.push(new Token(Token.Type.OUT, filepath, row, col));
                else if (c == '[') lexed.push(new Token(Token.Type.JEZ, filepath, row, col));
                else if (c == ']') lexed.push(new Token(Token.Type.JNZ, filepath, row, col));
                else if (programType == ProgramType.BF) continue;

                // BFN, BFNX
                else if (c == '/' && col < line.length() - 1 && line.charAt(col + 1) == '/') break;
                else if (c == '$') lexed.push(new Token(Token.Type.PTR, filepath, row, col));
                else if (c == '#') lexed.push(new Token(Token.Type.RET, filepath, row, col));
                else if (c == ':') lexed.push(new Token(Token.Type.COL, filepath, row, col));
                else if (c == ';') lexed.push(new Token(Token.Type.SCL, filepath, row, col));
                else if (c == '{') lexed.push(new Token(Token.Type.URS, filepath, row, col));
                else if (c == '}') lexed.push(new Token(Token.Type.URE, filepath, row, col));
                else if (c == '!') lexed.push(new Token(Token.Type.IMP, filepath, row, col));
                else if (c == '"') {
                    int           scol = col;
                    StringBuilder sb   = new StringBuilder();
                    for (col++; col < line.length(); col++) {
                        c = line.charAt(col);
                        if (c == '"') break;
                        if (col == line.length() - 1) {
                            Token tk = new Token(Token.Type.ERR, filepath, row, scol);
                            NGUtils.error("Unfinished string literal at: " + tk);
                        }
                        sb.append(c);
                    }
                    lexed.push(new Token(Token.Type.STR, sb.toString(), filepath, row, scol));
                } else if (Character.isDigit(c)) {
                    int scol = col;
                    int num  = c - '0';
                    for (col++; col < line.length(); col++) {
                        c = line.charAt(col);
                        if (!Character.isDigit(c)) {
                            col--;
                            break;
                        }
                        num = num * 10 + c - '0';
                    }
                    lexed.push(new Token(Token.Type.NUM, num, filepath, row, scol));
                } else if (Character.isLetter(c)) {
                    int           scol = col;
                    StringBuilder sb   = new StringBuilder().append(c);
                    for (col++; col < line.length(); col++) {
                        c = line.charAt(col);
                        if (!Character.isLetterOrDigit(c) && c != '_') {
                            col--;
                            break;
                        }
                        sb.append(c);
                    }
                    lexed.push(new Token(Token.Type.WRD, sb.toString(), filepath, row, scol));
                } else if (programType == ProgramType.BFN) {
                    NGUtils.error("Undefined token '%c'. This is probably a bug in Lexer. %s".formatted(c, new Token(Token.Type.ERR, filepath, row, col)));
                }

                // BFNX
                else if (c == '@') lexed.push(new Token(Token.Type.SYS, filepath, row, col));

                else NGUtils.error("Undefined token '%c'. This is probably a bug in Lexer. %s".formatted(c, new Token(Token.Type.ERR, filepath, row, col)));
            }
        }

        Token[] result = new Token[lexed.size()];
        for (int i = result.length - 1; i >= 0; i--) result[i] = lexed.pop();

        return result;
    }

}

