public class Token {

    public enum Type {
        ADD, SUB, PTRADD, PTRSUB, WHILE, ENDWHILE, WRITE, READ, // VANILLA
        NUMBER, STRING, MACRODEF, MACRO,
        ERROR
    }

    public Type    type;
    public int     numValue = -1;
    public String  strValue = null;
    public Token[] macroTokens;

    public String file;
    public int    row;
    public int    col;

    public Token(Type type, String file, int row, int col) {this.type = type; this.file = file; this.row = row; this.col = col;}

    public String toString() {
        if (type == Type.NUMBER) return "%s:%d:%d [%s(%d)]".formatted(file, row+1, col, type.toString(), numValue);
        else if (type == Type.STRING || type == Type.MACRO) return "%s:%d:%d [%s(%s)]".formatted(file, row+1, col, type.toString(), strValue);
        else if (type == Type.MACRODEF) {
            StringBuilder result = new StringBuilder("%s:%d:%d [%s(%s)] {".formatted(file, row+1, col, type.toString(), strValue));
            if (macroTokens != null) for (Token macroToken: macroTokens) result.append("%n%s".formatted(macroToken));
            return result.append("\n}").toString();
        }
        return "%s:%d:%d [%s]".formatted(file, row+1, col, type.toString());
    }
}
