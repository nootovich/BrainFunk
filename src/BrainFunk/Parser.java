package BrainFunk;

import nootovich.nglib.NGFileSystem;
import nootovich.nglib.NGUtils;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Stack;

import static BrainFunk.Token.Type.*;

public class Parser {

    public static boolean debug = false;

    private static final int RECURSION_LIMIT = 1000;
    private static       int recursionCount  = 0;

    public static  HashMap<String, Token[]> macros   = new HashMap<>();
    private static int                      savedNum = 1;

    public static Token[] parse(Token[] tokens, String filepath) {
        tokens = parseImports(tokens, filepath);
        tokens = parseNums(tokens);
        tokens = parseMacroDef(tokens);
        tokens = parseMacroCall(tokens, null);
        tokens = parseJumps(tokens);
        return tokens;
    }

    private static Token[] parseImports(Token[] tokens, String filepath) {
        Stack<Token> parsed = new Stack<>();
        for (int i = 0; i < tokens.length; i++) {
            switch (tokens[i].type) {
                case IMP -> {
                    if (i == tokens.length - 1 || tokens[i + 1].type != STR) {
                        NGUtils.error("No import file was provided. Please provide a `.bf`, `.bfn` or `.bfnx` file as a string after the '!' token.");
                    }
                    String   importStr       = tokens[++i].str;
                    String   importPath      = Path.of(filepath).getParent().resolve(importStr).normalize().toString();
                    String   importName      = new File(importPath).getName();
                    String[] importNameParts = importName.split("\\.");
                    String   importExtension = importNameParts[importNameParts.length - 1];
                    Main.ProgramType importProgramType = switch (importExtension) {
                        case "bf" -> Main.ProgramType.BF;
                        case "bfn" -> Main.ProgramType.BFN;
                        case "bfnx" -> Main.ProgramType.BFNX;
                        default -> {
                            NGUtils.error("Invalid file type `%s`. Please provide a `.bf`, `.bfn` or `.bfnx` file as a string after the '!' token.");
                            yield Main.ProgramType.ERR;
                        }
                    };
                    String  importCode  = NGFileSystem.loadFile(importPath);
                    Token[] importLexed = Lexer.lex(importCode, importPath, importProgramType);
                    for (Token t: importLexed) parsed.push(t);
                }
                case INC, DEC, RGT, LFT, JEZ, JNZ, INP, OUT, NUM, STR, WRD, PTR, RET, COL, SCL, URS, URE, SYS -> parsed.push(tokens[i]);
                default -> NGUtils.error("Unexpected token in parsing. Probably a bug in `Lexer`.\n" + tokens[i]);
            }
        }
        tokens = new Token[parsed.size()];
        for (int i = tokens.length - 1; i >= 0; i--) tokens[i] = parsed.pop();
        return tokens;
    }

    private static Token[] parseNums(Token[] tokens) {
        Stack<Token> parsed = new Stack<>();
        for (int i = 0; i < tokens.length; i++) {
            switch (tokens[i].type) {
                case INC, DEC, RGT, LFT, INP, OUT, STR, RET, WRD, IMP, SYS -> {
                    tokens[i].num = popNum();
                    parsed.push(tokens[i]);
                }
                case JEZ, JNZ, SCL, URS, URE -> {
                    if (popNum() > 1) NGUtils.error("Unexpected `NUM` token.\n" + tokens[i - 1]);
                    parsed.push(tokens[i]);
                }
                case COL -> {
                    if (i > 0 && tokens[i - 1].type == URE) {
                        tokens[i].num = tokens[i + 1].num;
                        parsed.push(tokens[i++]);
                    } else {
                        parsed.push(tokens[i]);
                    }
                }
                case NUM -> pushNum(tokens[i]);
                case PTR -> {
                    if (i == tokens.length - 1 || tokens[i + 1].type != NUM) NGUtils.error("No jump location for pointer.\n" + tokens[i]);
                    tokens[i].num = tokens[i + 1].num;
                    parsed.push(tokens[i++]);
                }
                default -> NGUtils.error("Unexpected token in parsing. Probably a bug in `Lexer`.\n" + tokens[i]);
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
                case INC, DEC, RGT, LFT, INP, OUT, JEZ, JNZ, NUM, STR, PTR, RET, COL, SCL, URS, URE, SYS -> parsed.push(tokens[i]);
                case WRD -> {
                    if (i == tokens.length - 1 || tokens[i + 1].type != COL) {
                        parsed.push(tokens[i]);
                        continue;
                    }
                    int          si         = i;
                    String       macroName  = tokens[i].str;
                    Stack<Token> macroStack = new Stack<>();
                    for (i += 2; i < tokens.length; i++) {
                        if (tokens[i].type == SCL) break;
                        if (i == tokens.length - 1 && tokens[i].type != SCL) NGUtils.error("Unfinished macro definition.\n" + tokens[si]);
                        tokens[i].origin = tokens[si];
                        macroStack.push(tokens[i]);
                    }
                    Token[] macroTokens = new Token[macroStack.size()];
                    for (int j = macroTokens.length - 1; j >= 0; j--) macroTokens[j] = macroStack.pop();
                    macros.put(macroName, macroTokens);
                }
                default -> NGUtils.error("Unexpected token in parsing. Probably a bug in `Lexer`.\n" + tokens[i]);
            }
        }
        Token[] result = new Token[parsed.size()];
        for (int i = result.length - 1; i >= 0; i--) result[i] = parsed.pop();
        return result;
    }

    public static Token[] parseMacroCall(Token[] tokens, Token origin) {
        recursionCount++;
        if (recursionCount >= RECURSION_LIMIT) NGUtils.error("Macro expansion limit exceeded.\n" + tokens[0]);
        Stack<Token> parsed = new Stack<>();
        for (int i = 0; i < tokens.length; i++) {
            switch (tokens[i].type) {
                case INC, DEC, RGT, LFT, INP, OUT, JEZ, JNZ, NUM, STR, PTR, RET, COL, SCL, URS, URE, SYS -> {
                    tokens[i].origin = origin;
                    parsed.push(tokens[i]);
                }
                case WRD -> {
                    if (!macros.containsKey(tokens[i].str)) NGUtils.error("Undefined macro.\n" + tokens[i]);
                    tokens[i].origin = origin;
                    if (debug) parsed.push(tokens[i]);
                    Token[] macroTokens = parseMacroCall(macros.get(tokens[i].str), tokens[i]);
                    for (int j = 0; j < tokens[i].num; j++) {
                        macroTokens = Token.deepCopy(macroTokens);
                        for (int k = 0; k < macroTokens.length; k++) {
                            parsed.push(macroTokens[k]);
                        }
                    }
                }
                default -> NGUtils.error("Unexpected token in parsing. Probably a bug in `Lexer`.\n" + tokens[i]);
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
                case INC, DEC, RGT, LFT, INP, OUT, NUM, STR, PTR, RET, WRD, COL, SCL, SYS -> {}
                case JEZ, URS -> jumps.push(i);
                case JNZ, URE -> {
                    if (jumps.isEmpty()) NGUtils.error("Unmatched brackets.\n" + tokens[i]);
                    int jmp = jumps.pop();
                    tokens[jmp].num = i;
                    tokens[i].num   = jmp;
                }
                default -> NGUtils.error("Unexpected token in parsing. Probably a bug in `Lexer`.\n" + tokens[i]);
            }
        }
        if (!jumps.isEmpty()) NGUtils.error("Unmatched brackets.\n" + tokens[jumps.pop()]);
        return tokens;
    }

    private static int popNum() {
        int num = savedNum;
        savedNum = 1;
        return num;
    }

    private static void pushNum(Token numTk) {
        if (savedNum > 1) NGUtils.error("Two consecutive `NUM` tokens are unsupported.\n" + numTk);
        savedNum = numTk.num;
    }
}

