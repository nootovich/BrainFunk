public class Parser {

    public static boolean EXTENDED = false;

    public static String parseBF(String data) {
        StringBuilder parsed = new StringBuilder();
        for (char c: data.toCharArray()) if (isAllowedBF(c)) parsed.append(c);
        return parsed.toString();
    }

    public static String parseBrainFunk(String data) {
        StringBuilder parsed      = new StringBuilder();
        int           dataLen     = data.length();
        boolean       nameStarted = false;
        for (int i = 0; i < dataLen; i++) {
            char c = data.charAt(i);

            // remove comments
            if (c == '/' && i+1 < dataLen && data.charAt(i+1) == '/') {
                do i++; while (i < dataLen && data.charAt(i) != '\n');
            }

            // parse strings
            else if (c == '"') {
                parsed.append('"');
                for (int j = i+1; j < dataLen; j++) {
                    if (j == dataLen-1) error("Unmatched double-quotes!\nFrom: "+i);
                    c = data.charAt(j);
                    if (c != '"') {
                        parsed.append(c);
                        continue;
                    }
                    parsed.append('"');
                    i = j;
                    break;
                }

            }

            // parse pointers
            else if (c == '$') {
                parsed.append('$');
                int ptr = -1;
                for (int j = i+1; j < dataLen; j++) {
                    // TODO: error reporting (and in a proper way)
                    c = data.charAt(j);
                    if (!Character.isDigit(c)) {
                        i = j;
                        break;
                    }
                    if (ptr == -1) ptr = 0;
                    ptr = ptr*10+c-'0';
                }
                if (ptr == -1) error("[ERROR]: pointer value expected but got nothing\nAt:"+i);
                parsed.append(ptr).append("_");
                if (isAllowedBrainFunk(c)) parsed.append(c);
            }

            // parse data
            else if (!nameStarted) {
                if (Character.isLetter(c)) nameStarted = true;
                if (Character.isLetterOrDigit(c) || isAllowedBrainFunk(c)) parsed.append(c);
            } else {
                if (Character.isLetterOrDigit(c)) parsed.append(c);
                else {
                    nameStarted = false;
                    if (c == ':') parsed.append(':');
                    else if (c == ';') parsed.append(" ;");
                    else parsed.append(' ').append(isAllowedBrainFunk(c) ? c : "");
                }
            }
        }
        return parsed.toString();
    }

    public static String parseBrainFunkExtended(String data) {
        EXTENDED = true;
        String result = parseBrainFunk(data);
        EXTENDED = false;
        return result;
    }

    private static boolean isAllowedBF(char c) {
        char[] allowedList = new char[]{'+', '-', '>', '<', '[', ']', '.', ','};
        return isAllowed(c, allowedList);
    }

    private static boolean isAllowedBrainFunk(char c) {
        char[] allowedList;
        if (EXTENDED) allowedList = new char[]{'+', '-', '>', '<', '[', ']', '.', ',', ':', ';', '"', '$', '#', '@'};
        else allowedList = new char[]{'+', '-', '>', '<', '[', ']', '.', ',', ':', ';', '"', '$', '#'};
        return isAllowed(c, allowedList);
    }

    private static boolean isAllowed(char c, char[] allowedList) {
        for (char a: allowedList) if (c == a) return true;
        return false;
    }

    private static void error(String message) {
        System.out.printf("[PARSER_ERROR]: %s%n", message);
        System.exit(1);
    }
    private static void error(Token tk, String message) {
        System.out.printf("[PARSER_ERROR]: %s: %s%n", tk, message);
        System.exit(1);
    }
}

