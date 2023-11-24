public class Token {

    public enum Type {
        ADD, SUB, PTRADD, PTRSUB, WHILE, ENDWHILE, WRITE, READ, // VANILLA
        NUMBER, STRING, MACRODEF, MACRO, POINTER, RETURN,
        ERROR
    }

    public Type    type;
    public int     numValue = -1;
    public String  strValue = null;
    public Token[] macroTokens;

    public Token origin;
    public String file;
    public int    row;
    public int    col;

    public Token(Type type, String file, int row, int col) {this.type = type; this.file = file; this.row = row; this.col = col;}

    public String toString() {
        StringBuilder result = new StringBuilder("%s:%d:%d [%s".formatted(file, row+1, col, type));

        if (type == Type.NUMBER || type == Type.POINTER) result.append("(").append(numValue).append(")]");
        else if (type == Type.STRING || type == Type.MACRO) result.append("(").append(strValue).append(")]");
        else if (type == Type.MACRODEF) {
            result.append("(").append(strValue).append(")] {");
            if (macroTokens != null) for (Token macroToken: macroTokens) result.append("%n    %s".formatted(macroToken));
            result.append("\n}");
        } else result.append("]");
        if (origin != null) result.append("\n    [INFO]: Expanded from: ").append(origin);
        return result.toString();
    }
}
