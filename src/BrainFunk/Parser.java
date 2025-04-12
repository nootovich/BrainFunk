package BrainFunk;

import BrainFunk.Op.Type;
import debugger.Debugger;
import java.nio.file.Path;
import java.util.*;
import nootovich.nglib.NGFileSystem;
import nootovich.nglib.NGUtils;

import static BrainFunk.Token.Type.*;

public class Parser {

    public static boolean debug = false;

    // TODO: make macros local to current file (for proper imports)
    public static  HashMap<String, Token[]> macros = new HashMap<>();
    private static Token                    savedNumModifier;

    public static Op[] parse(Token[] tokens, int ipOffset) {
        Stack<Op> ops = new Stack<>();

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
                    Op[] importOps = parse(importTokens, 0);
                    ops.addAll(0, Arrays.asList(importOps));
                }
                case AT -> ops.push(new Op(Type.SYSCALL, t, popNum()));
                case OCTOTHORPE -> ops.push(new Op(Type.RET, t, popNum()));
                case DOLLAR -> {
                    if (i + 1 >= tokens.length) NGUtils.error("The address for jump instruction was not provided");
                    ops.push(new Op(Type.PTR, t, tokens[++i]));
                }

                case NUMBER -> pushNum(t);
                case CHAR -> {
                    Token mt = popNum();
                    final int n = mt == null ? 1 : mt.num;
                    for (int j = 0; j < n; j++) ops.push(new Op(Type.INC, t, t));
                }
                case STRING -> ops.push(new Op(Type.PUSH_STRING, t, popNum(), t.str));
                case WORD -> {
                    if (i + 1 < tokens.length && tokens[i + 1].type == COLON) {
                        // MACRODEF
                        int macroStart = i += 2;
                        for (; i <= tokens.length; i++) {
                            if (i == tokens.length) NGUtils.error("Unfinished macro definition at " + t);
                            else if (tokens[i].type == SEMICOLON) break;
                        }
                        if (macros.containsKey(t.str)) {
                            if (!macros.get(t.str)[0].file.equals(t.file)) {
                                NGUtils.error("Redefinition of a macro '%s'".formatted(t.str));
                                NGUtils.info("Originally defined here: " + macros.get(t.str)[0].origin);
                            }
                        } else {
                            Token[] macroTokens = new Token[i - macroStart];
                            System.arraycopy(tokens, macroStart, macroTokens, 0, macroTokens.length);
                            macros.put(t.str, macroTokens);
                            t.visited                      = true;
                            tokens[macroStart - 1].visited = true;
                            tokens[i].visited              = true;
                        }
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
            if (!macros.containsKey(op.str)) NGUtils.error("Undefined macro: " + op.token);
            int repeats = op.num;

            if (debug) op.type = Type.DEBUG_MACRO;
            else ops.remove(i);


            for (int j = 0; j < repeats; j++) {
                Op[] macroOps = parse(macros.get(op.str), i + ipOffset + 1);
                if (debug) {
                    for (Op macroOp: macroOps) {
                        if (macroOp.origin < 0) macroOp.origin = i + ipOffset;
                    }
                    op.num = macroOps.length * repeats;
                    i++;
                }
                ops.addAll(i, Arrays.asList(macroOps));
                // i += macroOps.length;
            }
            i--;
        }

        Stack<Integer> jumps = new Stack<>();
        if (ipOffset == 0) { // NOTE: jumps are parsed only at a top level
            for (int i = 0; i < ops.size(); i++) {
                Op op = ops.get(i);
                if (op.type == Type.JEZ) {
                    op.num = i;
                    jumps.push(i);
                }
                if (op.type == Type.JNZ) {
                    if (jumps.isEmpty()) NGUtils.error("Unmatched brackets at: " + op.token);
                    op.num       = i;
                    op.link      = ops.get(jumps.pop());
                    op.link.link = op;
                }
            }
        }

        Op[] result = new Op[ops.size()];
        for (int i = result.length - 1; i >= 0; i--) result[i] = ops.pop();
        return result;
    }

    public static void reset() {
        debug  = false;
        macros = new HashMap<>();
        popNum();
        System.gc();
    }

    private static Token popNum() {
        Token numModifier = savedNumModifier;
        savedNumModifier = null;
        return numModifier;
    }

    private static void pushNum(Token numModifier) {
        if (savedNumModifier != null) NGUtils.error("Two consecutive `NUMBER` tokens are unsupported: " + numModifier);
        savedNumModifier = numModifier;
    }
}

