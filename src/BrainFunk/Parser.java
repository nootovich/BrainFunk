package BrainFunk;

import BrainFunk.Op.Type;
import debugger.Debugger;
import java.io.File;
import java.nio.file.Path;
import java.util.*;
import nootovich.nglib.NGFileSystem;
import nootovich.nglib.NGUtils;

import static BrainFunk.Token.Type.*;

public class Parser {

    public static boolean debug = false;

    private static final int RECURSION_LIMIT = 1000;
    private static       int recursionCount  = 0;

    public static  HashMap<String, Token[]> macros   = new HashMap<>();
    public static  HashMap<String, Token[]> macros2  = new HashMap<>();
    private static int                      savedNum = 1;

    public static Op[] parse2(Token[] tokens, int ipOffset) {
        Stack<Op>      ops   = new Stack<>();
        Stack<Integer> jumps = new Stack<>();

        for (int i = 0; i < tokens.length; i++) {
            Token t = tokens[i];
            switch (t.type) {
                case PLUS -> ops.push(new Op(Type.INC, t, popNum()));
                case MINUS -> ops.push(new Op(Type.DEC, t, popNum()));
                case GREATER -> ops.push(new Op(Type.RGT, t, popNum()));
                case LESS -> ops.push(new Op(Type.LFT, t, popNum()));
                case COMMA -> ops.push(new Op(Type.INP, t, popNum()));
                case DOT -> ops.push(new Op(Type.OUT, t, popNum()));
                case LBRACKET -> ops.push(new Op(Type.JEZ, t, popNum()));
                case RBRACKET -> ops.push(new Op(Type.JNZ, t, popNum()));

                case COLON, SEMICOLON -> NGUtils.error("Unreachable");
                case COMMENT -> { }

                case EXCLAMATION -> {
                    if (++i >= tokens.length || tokens[i].type != STRING) {
                        NGUtils.error("No import file was provided. Please provide a `.bf`, `.bfn` or `.bfnx` file as a string after the '!' token.");
                    }
                    String filepath = NGFileSystem.findRecursively(tokens[i].str);
                    if (filepath == null) NGUtils.error("Was not able to find import file '%s'".formatted(tokens[i].str));
                    String  fileData     = NGFileSystem.loadFile(Path.of(filepath).toAbsolutePath().toString());
                    Token[] importTokens = Lexer.lex(fileData, tokens[i].str);
                    if (debug) Debugger.tokens.put(tokens[i].str.replace('\\', '/'), importTokens);
                    Op[] importOps = parse2(importTokens, 0);
                    ops.addAll(0, Arrays.asList(importOps));
                }
                case AT -> ops.push(new Op(Type.SYSCALL, t, popNum()));
                case OCTOTHORPE -> ops.push(new Op(Type.RET, t, popNum()));
                case DOLLAR -> {
                    if (i + 1 >= tokens.length) NGUtils.error("The address for jump instruction was not provided");
                    ops.push(new Op(Type.PTR, t, tokens[++i].num));
                }

                case NUMBER -> pushNum(t);
                case STRING -> ops.push(new Op(Type.PUSH_STRING, t, t.str));
                case WORD -> {
                    if (i + 1 < tokens.length && tokens[i + 1].type == COLON) {
                        // MACRODEF
                        int macroStart = i += 2;
                        for (; i <= tokens.length; i++) {
                            if (i == tokens.length) NGUtils.error("Unfinished macro definition at " + t);
                            else if (tokens[i].type == SEMICOLON) break;
                        }
                        if (macros2.containsKey(t.str)) {
                            NGUtils.error("Redefinition of a macro '%s'".formatted(t.str));
                            NGUtils.info("Originally defined here: " + macros2.get(t.str)[0].origin);
                        }
                        Token[] macroTokens = new Token[i - macroStart];
                        System.arraycopy(tokens, macroStart, macroTokens, 0, macroTokens.length);
                        macros2.put(t.str, macroTokens);
                    } else {
                        ops.push(new Op(Type.MACRO, t, popNum(), t.str));
                    }
                }

                default -> NGUtils.error("Encountered unexpected token in parsing: " + t);
            }
        }

        for (int i = 0; i < ops.size(); i++) {
            Op op = ops.get(i);
            if (op.type != Type.MACRO) continue;
            if (!macros2.containsKey(op.str)) NGUtils.error("Undefined macro: " + op.token);
            int repeats = op.num;

            if (debug) op.type = Type.DEBUG_MACRO;
            else ops.remove(i);

            Op[] macroOps = parse2(macros2.get(op.str), i + 1);
            if (debug) {
                for (Op macroOp: macroOps) {
                    if (macroOp.origin < 0) macroOp.origin = i + ipOffset;
                }
                op.num = macroOps.length * repeats;
                i++;
            }

            for (int j = 0; j < repeats; j++) {
                ops.addAll(i, Arrays.asList(macroOps));
            }
            i--;
        }

        for (int i = 0; i < ops.size(); i++) {
            Op op = ops.get(i);
            if (op.type == Type.JEZ) jumps.push(i);
            if (op.type == Type.JNZ) {
                if (jumps.isEmpty()) NGUtils.error("Unmatched brackets at: " + op.token);
                int jmpIp = jumps.pop();
                ops.get(jmpIp).num = i;
                op.num             = jmpIp;
            }
        }

        Op[] result = new Op[ops.size()];
        for (int i = result.length - 1; i >= 0; i--) result[i] = ops.pop();
        return result;
    }

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
            if (tokens[i].type != EXCLAMATION) {
                parsed.push(tokens[i]);
                continue;
            }
            if (i == tokens.length - 1 || tokens[i + 1].type != STRING) {
                NGUtils.error("No import file was provided. Please provide a `.bf`, `.bfn` or `.bfnx` file as a string after the '!' token.");
            }
            String   importStr       = tokens[++i].str;
            String   importPath      = Path.of(filepath).getParent().resolve(importStr).normalize().toString();
            String   importName      = new File(importPath).getName();
            String[] importNameParts = importName.split("\\.");
            String   importExtension = importNameParts[importNameParts.length - 1];
            if (!importExtension.equals("bf") && !importExtension.equals("bfn") && !importExtension.equals("bfnx"))
                NGUtils.error("Invalid file type `%s`. Please provide a `.bf`, `.bfn` or `.bfnx` file as a string after the '!' token.");
            String  importCode  = NGFileSystem.loadFile(importPath);
            Token[] importLexed = Lexer.lex(importCode, importPath);
            for (Token t: importLexed) parsed.push(t);
        }
        tokens = new Token[parsed.size()];
        for (int i = tokens.length - 1; i >= 0; i--) tokens[i] = parsed.pop();
        return tokens;
    }

    private static Token[] parseNums(Token[] tokens) {
        Stack<Token> parsed = new Stack<>();
        for (int i = 0; i < tokens.length; i++) {
            switch (tokens[i].type) {
                case PLUS, MINUS, GREATER, LESS, COMMA, DOT, STRING, WORD, OCTOTHORPE, AT -> {
                    tokens[i].num = popNum();
                    parsed.push(tokens[i]);
                }
                case LBRACKET, RBRACKET, SEMICOLON -> {
                    if (popNum() > 1) NGUtils.error("Unexpected `NUMBER` token at: " + tokens[i - 1]);
                    parsed.push(tokens[i]);
                }
                case COLON, COMMENT -> parsed.push(tokens[i]);
                case NUMBER -> pushNum(tokens[i]);
                case DOLLAR -> {
                    if (i == tokens.length - 1 || tokens[i + 1].type != NUMBER) NGUtils.error("No jump location for pointer.\n" + tokens[i]);
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
            if (tokens[i].type != WORD || i == tokens.length - 1 || tokens[i + 1].type != COLON) {
                parsed.push(tokens[i]);
                continue;
            }
            int          si         = i;
            String       macroName  = tokens[i].str;
            Stack<Token> macroStack = new Stack<>();
            for (i += 2; i < tokens.length; i++) {
                if (tokens[i].type == SEMICOLON) break;
                if (i == tokens.length - 1 && tokens[i].type != SEMICOLON) NGUtils.error("Unfinished macro definition.\n" + tokens[si]);
                tokens[i].origin = tokens[si];
                macroStack.push(tokens[i]);
            }
            Token[] macroTokens = new Token[macroStack.size()];
            for (int j = macroTokens.length - 1; j >= 0; j--) macroTokens[j] = macroStack.pop();
            macros.put(macroName, macroTokens);
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
            if (tokens[i].type != WORD) {
                tokens[i].origin = origin;
                parsed.push(tokens[i]);
                continue;
            }
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
        recursionCount--;
        Token[] result = new Token[parsed.size()];
        for (int i = result.length - 1; i >= 0; i--) result[i] = parsed.pop();
        return result;
    }

    private static Token[] parseJumps(Token[] tokens) {
        Stack<Integer> jumps = new Stack<>();
        for (int i = 0; i < tokens.length; i++) {
            switch (tokens[i].type) {
                case LBRACKET -> jumps.push(i);
                case RBRACKET -> {
                    if (jumps.isEmpty()) NGUtils.error("Unmatched brackets at: " + tokens[i]);
                    int jmp = jumps.pop();
                    tokens[i].num   = jmp;
                    tokens[jmp].num = i;
                }
                default -> { }
            }
        }
        if (!jumps.isEmpty()) NGUtils.error("Unmatched brackets at: " + tokens[jumps.pop()]);
        return tokens;
    }

    private static int popNum() {
        int num = savedNum;
        savedNum = 1;
        return num;
    }

    private static void pushNum(Token numTk) {
        if (savedNum > 1)
            NGUtils.error("Two consecutive `NUMBER` tokens are unsupported: " + numTk);
        savedNum = numTk.num;
    }
}

