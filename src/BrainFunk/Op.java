package BrainFunk;

public class Op {
    public enum Type {
        INC, DEC, RGT, LFT, INP, OUT, JEZ, JNZ, // VANILLA
        MACRODEF, MACRO,
        // NUM, STR, PTR, RET, WRD,
        // COL, SCL,
        // IMP,
        // SYS,
    }

    public Type  type;
    public Token token;

    public int    num = -1;
    public String str = "";


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


    @Override
    public String toString() {
        return "%s, %d, %s".formatted(type, num, str);
    }
}
