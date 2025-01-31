package BrainFunk;

public class Op {

    public enum Type {
        INC, DEC, RGT, LFT, INP, OUT, JEZ, JNZ, // VANILLA
        PTR, RET, // TODO: replace 'RET' with 'JMP' and figure out a way to push a return address in compile time.
        MACRODEF, MACRO,
        SYSCALL,
        PUSH_STRING,
        DEBUG_MACRO
    }

    public Type  type;
    public Token token; // TODO: Save location instead of token

    public int    num = -1;
    public String str = null;

    public int origin = -1; // NOTE: Only for debug purposes

    Op(Type type, Token token) {
        this.type  = type;
        this.token = token;
    }

    Op(Type type, Token token, int num) {
        this.type  = type;
        this.token = token;
        this.num   = num;
    }

    Op(Type type, Token token, String str) {
        this.type  = type;
        this.token = token;
        this.str   = str;
    }

    Op(Type type, Token token, int num, String str) {
        this.type  = type;
        this.token = token;
        this.num   = num;
        this.str   = str;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("[%s{%d}".formatted(type, num));
        if (str != null) result.append(":{\"%s\"}".formatted(str));
        return result.append("] @ %s".formatted(token)).toString();
    }
}
