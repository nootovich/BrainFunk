public class Utils {
    public static int clampi(int n, int min, int max) {
        if (n < min) return min;
        if (n > max) return max;
        return n;
    }
}
