public class Utils {

    private static final int FILENAME_LEN = 20;

    public static int clampi(int n, int min, int max) {
        if (max < min) max = min;
        if (n < min) return min;
        if (n > max) return max;
        return n;
    }

    public static void info(String message) {
        StackTraceElement src      = Thread.currentThread().getStackTrace()[2];
        String            filename = src.getFileName();
        String            lineNum  = String.valueOf(src.getLineNumber());
        String            padding  = " ".repeat(FILENAME_LEN - filename.length() - lineNum.length());
        System.out.printf("%s:%s%s [INFO]:  %s%n", filename, lineNum, padding, message);
    }

    public static void error(String message) {
        StackTraceElement src      = Thread.currentThread().getStackTrace()[2];
        String            filename = src.getFileName();
        String            lineNum  = String.valueOf(src.getLineNumber());
        String            padding  = " ".repeat(FILENAME_LEN - filename.length() - lineNum.length());
        System.out.printf("%s:%s%s %s[ERROR]%s: %s%n", filename, lineNum, padding, "\u001B[31m", "\u001B[0m", message);
        System.exit(1);
    }
}
