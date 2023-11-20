public class Token {

    public static enum Type {
        ADD, SUB, PTRADD, PTRSUB, WHILE, ENDWHILE, WRITE, READ
    }

    public Type type;
    // public static int amount;
    // public int row;
    // public int col;
    // etc.

    public Token(Type type) {this.type = type;}

    public String toString() {
        return type.toString();
    }
}
