import java.util.HashMap;
import java.util.Stack;

public class Parser {

    private static final int RECURSION_LIMIT = 1000;
    private static       int recursionCount  = 0;

    private static HashMap<String, Token[]> macros   = new HashMap<>();
    private static int                      savedNum = 1;

    public static Token[] parse(Token[] tokens) {
        tokens = parseNums(tokens);
        tokens = parseMacroDef(tokens);
        macros.forEach((name, tks) -> macros.replace(name, parseMacroCall(tks)));
        tokens = parseMacroCall(tokens);
        tokens = parseJumps(tokens);
        return tokens;
    }

    private static Token[] parseNums(Token[] tokens) {
        Stack<Token> parsed = new Stack<>();
        for (int i = 0; i < tokens.length; i++) {
            switch (tokens[i].type) {
                case INC, DEC, RGT, LFT, INP, OUT, STR, RET, WRD -> {
                    tokens[i].num = popNum();
                    parsed.push(tokens[i]);
                }
                case JEZ, JNZ, SCL, UNSAFEJEZ, UNSAFEJNZ -> {
                    if (popNum() > 1) Utils.error("Unexpected `NUM` token.\n" + tokens[i - 1]);
                    parsed.push(tokens[i]);
                }
                case COL -> {
                    if (i > 0 && tokens[i - 1].type == Token.Type.UNSAFEJNZ) {
                        tokens[i].num = tokens[i + 1].num;
                        parsed.push(tokens[i++]);
                    } else {
                        parsed.push(tokens[i]);
                    }
                }
                case NUM -> pushNum(tokens[i]);
                case PTR -> {
                    if (i == tokens.length - 1 || tokens[i + 1].type != Token.Type.NUM) Utils.error("No jump location for pointer.\n" + tokens[i]);
                    tokens[i].num = tokens[i + 1].num;
                    parsed.push(tokens[i++]);
                }
                default -> Utils.error("Unexpected token in parsing. Probably a bug in `Lexer`.\n" + tokens[i]);
            }
        }
        tokens = new Token[parsed.size()];
        for (int i = tokens.length - 1; i >= 0; i--) tokens[i] = parsed.pop();
        return tokens;
    }

    private static Token[] parseMacroDef(Token[] tokens) {
        Stack<Token> parsed = new Stack<>();
        for (int i = 0; i < tokens.length; i++) {
            switch (tokens[i].type) {
                case INC, DEC, RGT, LFT, INP, OUT, JEZ, JNZ, NUM, STR, PTR, RET, COL, SCL, UNSAFEJEZ, UNSAFEJNZ -> parsed.push(tokens[i]);
                case WRD -> {
                    if (i == tokens.length - 1 || tokens[i + 1].type != Token.Type.COL) {
                        parsed.push(tokens[i]);
                        continue;
                    }
                    int          si         = i;
                    String       macroName  = tokens[i].str;
                    Stack<Token> macroStack = new Stack<>();
                    for (i += 2; i < tokens.length; i++) {
                        if (tokens[i].type == Token.Type.SCL) break;
                        if (i == tokens.length - 1 && tokens[i].type != Token.Type.SCL) Utils.error("Unfinished macro definition.\n" + tokens[si]);
                        macroStack.push(tokens[i]);
                    }
                    Token[] macroTokens = new Token[macroStack.size()];
                    for (int j = macroTokens.length - 1; j >= 0; j--) macroTokens[j] = macroStack.pop();
                    macros.put(macroName, macroTokens);
                }
                default -> Utils.error("Unexpected token in parsing. Probably a bug in `Lexer`.\n" + tokens[i]);
            }
        }
        Token[] result = new Token[parsed.size()];
        for (int i = result.length - 1; i >= 0; i--) result[i] = parsed.pop();
        return result;
    }

    private static Token[] parseMacroCall(Token[] tokens) {
        recursionCount++;
        if (recursionCount >= RECURSION_LIMIT) Utils.error("Macro expansion limit exceeded.\n" + tokens[0]);
        Stack<Token> parsed = new Stack<>();
        for (int i = 0; i < tokens.length; i++) {
            switch (tokens[i].type) {
                case INC, DEC, RGT, LFT, INP, OUT, JEZ, JNZ, NUM, STR, PTR, RET, COL, SCL, UNSAFEJEZ, UNSAFEJNZ -> parsed.push(tokens[i]);
                case WRD -> {
                    if (!macros.containsKey(tokens[i].str)) Utils.error("Undefined macro.\n" + tokens[i]);
                    Token[] macroTokens = parseMacroCall(macros.get(tokens[i].str));
                    for (int j = 0; j < tokens[i].num; j++) {
                        macroTokens = Token.deepCopy(macroTokens);
                        for (int k = 0; k < macroTokens.length; k++) {
                            parsed.push(macroTokens[k]);
                        }
                    }
                }
                default -> Utils.error("Unexpected token in parsing. Probably a bug in `Lexer`.\n" + tokens[i]);
            }
        }
        recursionCount--;
        Token[] result = new Token[parsed.size()];
        for (int i = result.length - 1; i >= 0; i--) result[i] = parsed.pop();
        return result;
    }

    private static Token[] parseJumps(Token[] tokens) {
        Stack<Integer> jumps = new Stack<>();
        for (int i = 0; i < tokens.length; i++) {
            switch (tokens[i].type) {
                case INC, DEC, RGT, LFT, INP, OUT, NUM, STR, PTR, RET, WRD, COL, SCL -> {}
                case JEZ, UNSAFEJEZ -> jumps.push(i);
                case JNZ, UNSAFEJNZ -> {
                    if (jumps.isEmpty()) Utils.error("Unmatched brackets.\n" + tokens[i]);
                    int jmp = jumps.pop();
                    tokens[jmp].num = i;
                    tokens[i].num   = jmp;
                }
                default -> Utils.error("Unexpected token in parsing. Probably a bug in `Lexer`.\n" + tokens[i]);
            }
        }
        if (!jumps.isEmpty()) Utils.error("Unmatched brackets.\n" + tokens[jumps.pop()]);
        return tokens;
    }

    private static int popNum() {
        int num = savedNum;
        savedNum = 1;
        return num;
    }

    private static void pushNum(Token numTk) {
        if (savedNum > 1) Utils.error("Two consecutive `NUM` tokens are unsupported.\n" + numTk);
        savedNum = numTk.num;
    }
}

