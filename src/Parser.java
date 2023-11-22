import java.util.HashMap;
import java.util.List;
import java.util.Stack;

public class Parser {

    // TODO: limit recursive macro definitions

    static HashMap<String, Token[]> macros = new HashMap<>();

    public static Token[] parseTokens(Token[] tokens) {
        macros.clear();
        return privateParseTokens(tokens);
    }

    private static Token[] privateParseTokens(Token[] tokens) {
        Stack<Token> parsed = new Stack<>();
        for (int i = 0; i < tokens.length; i++) {
            Token tk = tokens[i];
            if (tk.type == Token.Type.MACRODEF) {
                if (macros.containsKey(tk.strValue)) error("Redefinition of a macro %s.".formatted(tk));
                macros.put(tk.strValue, tk.macroTokens);
            } else if (tk.type == Token.Type.MACRO) {
                int amount = 1;
                if (!parsed.isEmpty() && parsed.peek().type == Token.Type.NUMBER) amount = parsed.pop().numValue;
                if (!macros.containsKey(tk.strValue)) error("Undefined macro %s.".formatted(tk));
                var macroTokens = List.of(privateParseTokens(macros.get(tk.strValue)));
                for (int j = 0; j < amount; j++) parsed.addAll(macroTokens);
            } else parsed.push(tk);
        }
        return parsed.toArray(new Token[0]);
    }

    private static void error(String message) {
        StackTraceElement errSrc = Thread.currentThread().getStackTrace()[2];
        System.out.printf("%s:%d [ERROR]: %s%n", errSrc.getFileName(), errSrc.getLineNumber(), message);
        System.exit(1);
    }

}

