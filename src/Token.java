public class Token {

    public enum Type {
        INC, DEC, RGT, LFT, INP, OUT, JEZ, JNZ, // VANILLA
        NUM, STR, PTR, RET, WRD,
        COL, SCL, UNSAFEJEZ, UNSAFEJNZ,
        ERR
    }

    public Type    type;
    public int     numValue = -1;
    public String  strValue = null;
    public Token[] macroTokens;

    public Token  origin;
    public String file;
    public int    row;
    public int    col;

    public Token(Type type, String file, int row, int col) {this.type = type; this.file = file; this.row = row; this.col = col;}

    public boolean eq(Token t) {
        boolean result = (row == t.row && col == t.col && type == t.type && numValue == t.numValue);
        if (file != null && t.file != null) result &= file.equals(t.file);
        else result &= (file == null && t.file == null);
        if (strValue != null && t.strValue != null) result &= strValue.equals(t.strValue);
        else result &= (strValue == null && t.strValue == null);
        return result;
    }

    public String repr() {
        return switch (type) {
            case INC -> "+";
            case DEC -> "-";
            case RGT -> ">";
            case LFT -> "<";
            case JEZ -> "[";
            case JNZ -> "]";
            case OUT -> ".";
            case INP -> ",";
            case NUM -> String.valueOf(numValue);
            case STR -> '\"' + strValue + '\"';
            // case MACRODEF -> strValue + ':';
            case WRD -> strValue;
            case PTR -> "$";
            case RET -> "#";
            case COL -> ":";
            case SCL -> ";";
            case UNSAFEJEZ -> "{";
            case UNSAFEJNZ -> "}";
            case ERR -> throw new RuntimeException("Attempted to get a representation of an `ERROR` token... HOW!?");
        };
    }

    public int len() {
        return repr().length();
    }

    public static Token[] deepCopy(Token[] source) {
        Token[] result = new Token[source.length];
        for (int i = 0; i < result.length; i++) {
            result[i]          = new Token(source[i].type, source[i].file, source[i].row, source[i].col);
            result[i].numValue = source[i].numValue;
            result[i].strValue = source[i].strValue;
            result[i].origin   = source[i].origin;
            if (source[i].macroTokens != null) {
                result[i].macroTokens = deepCopy(source[i].macroTokens);
            }
        }
        return result;
    }

    public String toString() {
        StringBuilder result = new StringBuilder("%s:%d:%d [%s".formatted(file, row + 1, col, type));

        if (type == Type.NUM || type == Type.PTR) result.append("(").append(numValue).append(")]");
        else if (type == Type.STR || type == Type.WRD) result.append("(").append(strValue).append(")]");
        // else if (type == Type.MACRODEF) {
        //     result.append("(").append(strValue).append(")] {");
        //     if (macroTokens != null) for (Token macroToken: macroTokens) result.append("%n    %s".formatted(macroToken));
        //     result.append("\n}");
        // }
        else result.append("]");
        if (origin != null) result.append("\n    [INFO]: Expanded from: ").append(origin);
        return result.toString();
    }
}
