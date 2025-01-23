package BrainFunk;

public class Token {

    public enum Type {
        PLUS, MINUS, GREATER, LESS, COMMA, DOT, LBRACKET, RBRACKET, // VANILLA
        NUM, STR, PTR, RET, WRD,
        COL, SCL,
        IMP,
        SYS, ERR
    }

    public Type   type;
    public String str = null;
    public int    num = -1;

    // TODO: remove
    public Token[] macroTokens;
    public Token   origin;

    // loc
    public String file;
    public int    row;
    public int    col;

    public Token(Type type, String file, int row, int col) {
        this.type = type;
        this.file = file.replace('\\', '/');
        this.row  = row;
        this.col  = col;
    }

    public Token(Type type, String str, String file, int row, int col) {
        this.type = type;
        this.str  = str;
        this.file = file.replace('\\', '/');
        this.row  = row;
        this.col  = col;
    }

    public Token(Type type, int num, String file, int row, int col) {
        this.type = type;
        this.num  = num;
        this.file = file.replace('\\', '/');
        this.row  = row;
        this.col  = col;
    }

    public boolean eq(Token t) {
        boolean result = (row == t.row && col == t.col && type == t.type && num == t.num);
        if (file != null && t.file != null) result &= file.equals(t.file);
        else result &= (file == null && t.file == null);
        if (str != null && t.str != null) result &= str.equals(t.str);
        else result &= (str == null && t.str == null);
        return result;
    }

    public String repr() {
        return switch (type) {
            case PLUS -> "+";
            case MINUS -> "-";
            case GREATER -> ">";
            case LESS -> "<";
            case LBRACKET -> "[";
            case RBRACKET -> "]";
            case DOT -> ".";
            case COMMA -> ",";
            case NUM -> String.valueOf(num);
            case STR -> '\"' + str + '\"';
            // case MACRODEF -> strValue + ':';
            case WRD -> str;
            case PTR -> "$";
            case RET -> "#";
            case COL -> ":";
            case SCL -> ";";
            case IMP -> "!";
            case SYS -> "@";
            case ERR -> throw new RuntimeException("Attempted to get a representation of an `ERROR` token... HOW!?");
        };
    }

    public int len() {
        return repr().length();
    }

    public static Token[] deepCopy(Token[] source) {
        Token[] result = new Token[source.length];
        for (int i = 0; i < result.length; i++) {
            result[i]        = new Token(source[i].type, source[i].file, source[i].row, source[i].col);
            result[i].num    = source[i].num;
            result[i].str    = source[i].str;
            result[i].origin = source[i].origin;
            if (source[i].macroTokens != null) {
                result[i].macroTokens = deepCopy(source[i].macroTokens);
            }
        }
        return result;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("%s:%d:%d [%s{%d}".formatted(file, row + 1, col, type, num));
        if (str != null) result.append(":{\"").append(str).append("\"}");
        result.append("]");
        return result.toString();
    }

    public String toStringDetailed() {
        StringBuilder result = new StringBuilder(toString());
        if (origin != null) result.append("\n    From: ").append(origin);
        return result.toString();
    }
}
