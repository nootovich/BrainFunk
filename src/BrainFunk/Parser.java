package BrainFunk;

import BrainFunk.Op.Type;
import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Stack;
import nootovich.nglib.NGFileSystem;
import nootovich.nglib.NGUtils;

import static BrainFunk.BrainFunk.ProgramType;
import static BrainFunk.Token.Type.*;

public class Parser {

    public static boolean debug = false;

    private static final int RECURSION_LIMIT = 1000;
    private static       int recursionCount  = 0;

    public static  HashMap<String, Token[]> macros   = new HashMap<>();
    public static  HashMap<String, Op[]>    macros2  = new HashMap<>();
    private static int                      savedNum = 1;

    public static Op[] parse2(Token[] tokens) {
        /*
        for (int i = 0; i < tokens.length; i++) {
            switch (tokens[i].type) {

                default -> { }
            }
        }
        if (!jumps.isEmpty()) NGUtils.error("Unmatched brackets at: " + tokens[jumps.pop()]);
        return tokens;*/
        Stack<Op>      ops   = new Stack<>();
        Stack<Integer> jumps = new Stack<>();
        Stack<Integer> pointers = new Stack<>();
        for (int i = 0; i < tokens.length; i++) {
            Token t = tokens[i];
            switch (t.type) {
                case PLUS, MINUS, GREATER, LESS, COMMA, DOT -> ops.push(new Op(Type.values()[t.type.ordinal()], t, popNum()));
                case LBRACKET -> {
                    jumps.push(i);
                    ops.push(new Op(Type.JEZ, t));
                }
                case RBRACKET -> {
                    if (jumps.isEmpty()) NGUtils.error("Unmatched brackets at: " + tokens[i]);
                    int jmp = jumps.pop();
                    ops.get(jmp).num = i;
                    ops.push(new Op(Type.JNZ, t, jmp));
                }

                case COLON, SEMICOLON -> NGUtils.error("Unreachable");
                case COMMENT -> { }

                case OCTOTHORPE -> ops.push(new Op(Type.JMP, t, pointers.pop()));
                case DOLLAR -> {
                    pointers.push(-1); // TODO: Figure out a way to push a return address in compile time if possible.
                    ops.push(new Op(Type.JMP, t, tokens[i + 1].num));
                }

                case NUMBER -> pushNum(t);
                case STRING -> NGUtils.error("TODO");
                case WORD -> {
                    if (i + 1 < tokens.length && tokens[i + 1].type == COLON) { // MACRODEF
                        int macroStart = i += 2;
                        for (; i <= tokens.length; i++) {
                            if (i == tokens.length) NGUtils.error("Unfinished macro definition at " + t);
                            else if (tokens[i].type == SEMICOLON) break;
                        }
                        if (macros2.containsKey(t.str)) {
                            NGUtils.error("Redefinition of a macro '%s'".formatted(t.str));
                            NGUtils.info("Originally defined here: " + macros2.get(t.str)[0].token.origin);
                        }
                        Token[] macroTokens = new Token[i - macroStart];
                        System.arraycopy(tokens, macroStart, macroTokens, 0, macroTokens.length);
                        Op[] macroOps = parse2(macroTokens);
                        macros2.put(t.str, macroOps);
                    } else { // MACRO
                        if (!macros2.containsKey(t.str)) NGUtils.error("Undefined macro: " + t);
                        for (int j = 0, repeats = popNum(); j < repeats; j++)
                            for (Op op: macros2.get(t.str)) ops.push(op);
                    }
                }

                default -> NGUtils.error("Encountered unexpected token in parsing: " + t);
            }
        }

        Op[] result = new Op[ops.size()];
        for (
            int i = result.length - 1;
            i >= 0; i--)
            result[i] = ops.pop();
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
            ProgramType importProgramType = switch (importExtension) {
                case "bf" -> ProgramType.BF;
                case "bfn" -> ProgramType.BFN;
                case "bfnx" -> ProgramType.BFNX;
                default -> {
                    NGUtils.error("Invalid file type `%s`. Please provide a `.bf`, `.bfn` or `.bfnx` file as a string after the '!' token.");
                    yield ProgramType.ERR;
                }
            };
            String  importCode  = NGFileSystem.loadFile(importPath);
            Token[] importLexed = Lexer.lex(importCode, importPath, importProgramType);
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
        if (savedNum > 1) NGUtils.error("Two consecutive `NUMBER` tokens are unsupported: " + numTk);
        savedNum = numTk.num;
    }
}

