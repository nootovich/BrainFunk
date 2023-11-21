public class Token {

    public enum Type {
        ADD, SUB, PTRADD, PTRSUB, WHILE, ENDWHILE, WRITE, READ, // VANILLA
        NUMBER, STRING
    }

    public Type   type;
    public int    row;
    public int    col;
    public int    numValue = -1;
    public String strValue = null;

    public Token(Type type, int row, int col) {this.type = type; this.row = row; this.col = col;}

    public String toString() {
        return "%s:%d:%d [%s%s]".formatted(Main.filename, row+1, col, type.toString(),
                                           numValue >= 0 ? "(%d)".formatted(numValue) : strValue != null ? "(%s)".formatted(strValue) : "");
    }
}
