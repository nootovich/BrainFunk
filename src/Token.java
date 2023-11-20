public class Token {

    public enum Type {
        ADD, SUB, PTRADD, PTRSUB, WHILE, ENDWHILE, WRITE, READ, // VANILLA
        NUMBER
    }

    public Type type;
    public int  row;
    public int col;
    public int value;

    public Token(Type type, int row, int col) {this.type = type; this.row = row; this.col = col;}

    public String toString() {
        return "%s:%d:%d [%s(%d)]".formatted(Main.filename, row+1, col, type.toString(), value);
    }
}
