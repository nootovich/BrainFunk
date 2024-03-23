public class Utils {

    public static int clampi(int n, int min, int max) {
        if (n < min) return min;
        if (n > max) return max;
        return n;
    }

    public static void error(String message) {
        StackTraceElement errSrc = Thread.currentThread().getStackTrace()[2];
        System.out.printf("%s:%d [ERROR]: %s%n", errSrc.getFileName(), errSrc.getLineNumber(), message);
        System.exit(1);
    }
}
