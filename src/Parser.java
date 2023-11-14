public class Parser {

    public static boolean EXTENDED = false;

    public static String parsePureBF(String data) {
        StringBuilder parsed = new StringBuilder();
        for (char c: data.toCharArray()) if (isAllowedPureBF(c)) parsed.append(c);
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
                    if (j == dataLen-1) Main.exit("Unmatched double-quotes!\nFrom: "+i);

                    c = data.charAt(j);
                    if (c != '"') {
                        parsed.append(c);
                        continue;
                    }

                    parsed.append('"');
                    i = j+1;
                    break;
                }

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

    private static boolean isAllowedPureBF(char c) {
        char[] allowedList = new char[]{'+', '-', '>', '<', '[', ']', '.', ','};
        return isAllowed(c, allowedList);
    }

    private static boolean isAllowedBrainFunk(char c) {
        char[] allowedList;
        if (EXTENDED) allowedList = new char[]{'+', '-', '>', '<', '[', ']', '.', ',', ':', ';', '"', '@'};
        else allowedList = new char[]{'+', '-', '>', '<', '[', ']', '.', ',', ':', ';', '"'};
        return isAllowed(c, allowedList);
    }

    private static boolean isAllowed(char c, char[] allowedList) {
        for (char a: allowedList) if (c == a) return true;
        return false;
    }
}

