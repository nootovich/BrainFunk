package BrainFunk;

import nootovich.nglib.NGUtils;

public class Op {

    public enum Type {
        // VANILLA
        INC, DEC, RGT, LFT, INP, OUT, JEZ, JNZ,

        // BFN
        PTR, RET,
        MACRO, DEBUG_MACRO,
        PUSH_STRING,
        SYSCALL
    }

    public Type  type;
    public Token token; // TODO: Save location instead of token

    public int    num = -1;
    public String str = null;

    // NOTE: Only for debug purposes
    public int   origin = -1;
    public Token modifierToken;

    Op(Type type, Token token) {
        this.type  = type;
        this.token = token;
    }

    Op(Type type, Token token, String str) {
        this.type  = type;
        this.token = token;
        this.str   = str;
    }

    Op(Type type, Token token, Token modifierToken) {
        this.type  = type;
        this.token = token;
        if (modifierToken == null) {
            num = 1;
        } else if (modifierToken.type == Token.Type.NUMBER) {
            this.modifierToken = modifierToken;
            this.num           = modifierToken.num;
        } else NGUtils.error("Unreachable");
    }

    Op(Type type, Token token, Token modifierToken, String str) {
        this.type  = type;
        this.token = token;
        this.str   = str;
        if (modifierToken == null) {
            num = 1;
        } else if (modifierToken.type == Token.Type.NUMBER) {
            this.modifierToken = modifierToken;
            this.num           = modifierToken.num;
        } else NGUtils.error("Unreachable");
    }

    public static Op[] deepCopy(Op[] source) {
        Op[] result = new Op[source.length];
        for (int i = 0; i < result.length; i++) {
            result[i]        = new Op(source[i].type, source[i].token, source[i].modifierToken);
            result[i].num    = source[i].num;
            result[i].str    = source[i].str;
            result[i].origin = source[i].origin;
        }
        return result;
    }

    public String repr() {
        return switch (type) {
            case INC, DEC, RGT, LFT, INP, OUT -> (num > 1 ? num : "") + token.repr();
            case JEZ -> "[";
            case JNZ -> "]";
            case PTR -> "$" + num;
            case RET -> "#";
            case DEBUG_MACRO -> (num > 1 ? num : "") + str;
            case PUSH_STRING -> "\"%s\"".formatted(str);
            case SYSCALL -> "@";
            default -> NGUtils.error("Unreachable");
        };
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("{%c} [%s{%d}".formatted(token.visited ? 'X' : ' ', type, num));
        if (str != null) result.append(":{\"%s\"}".formatted(str));
        return result.append("] @ %s".formatted(token)).toString();
    }

    public final boolean equals(Op op) {
        return num == op.num && origin == op.origin && type == op.type && str.equals(op.str);
    }
}
