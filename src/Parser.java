import java.util.HashMap;
import java.util.List;
import java.util.Stack;

public class Parser {

    private static final int RECURSION_LIMIT = 1000;
    private static       int recursionCount  = 0;

    private static HashMap<String, Token[]> macros = new HashMap<>();

    public static Token[] parseTokens(Token[] tokens) {
        macros.clear();
        return privateParseTokens(tokens);
    }

    private static Token[] privateParseTokens(Token[] tokens) {
        tokens = parseMacros(tokens, null);
        tokens = parsePointers(tokens);
        return tokens;
    }

    private static Token[] parseMacros(Token[] tokens, Token origin) {
        if (++recursionCount >= RECURSION_LIMIT) error("The recursion limit of %d was exceeded by: %s".formatted(RECURSION_LIMIT, origin));
        Stack<Token> parsed = new Stack<>();
        for (Token tk: tokens) {
            if (tk.origin == null) tk.origin = origin;
            if (tk.type == Token.Type.MACRODEF) {
                if (macros.containsKey(tk.strValue)) error("Redefinition of a macro %s.".formatted(tk));
                macros.put(tk.strValue, tk.macroTokens);
            } else if (tk.type == Token.Type.MACRO) {
                int amount     = 1;
                int parsedSize = parsed.size();
                if (parsedSize > 0 && parsed.peek().type == Token.Type.NUMBER
                    && (parsedSize < 2 || parsed.get(parsedSize - 2).type != Token.Type.POINTER)) {
                    amount = parsed.pop().numValue;
                }
                if (!macros.containsKey(tk.strValue)) error("Undefined macro %s.".formatted(tk));

                // manually deep copying current macro tokens to avoid making a copy of references
                Token[]     tokensToPass = Token.deepCopy(macros.get(tk.strValue));
                List<Token> macroTokens  = List.of(parseMacros(tokensToPass, tk));
                for (int j = 0; j < amount; j++) parsed.addAll(macroTokens);
            } else parsed.push(tk);
        }
        recursionCount--;
        return parsed.toArray(new Token[0]);
    }

    private static Token[] parsePointers(Token[] tokens) {
        Stack<Token> parsed = new Stack<>();
        for (int i = 0; i < tokens.length; i++) {
            Token tk = tokens[i];
            if (tk.type == Token.Type.POINTER) {
                if (i == tokens.length - 1 || tokens[i + 1].type != Token.Type.NUMBER)
                    error("Invalid argument for a pointer! Expected a number after: " + tk);
                tk.numValue = tokens[++i].numValue;
            }
            parsed.push(tk);
        }
        return parsed.toArray(new Token[0]);
    }

    private static void error(String message) {
        StackTraceElement errSrc = Thread.currentThread().getStackTrace()[2];
        System.out.printf("%s:%d [ERROR]: %s%n", errSrc.getFileName(), errSrc.getLineNumber(), message);
        System.exit(1);
    }

}

