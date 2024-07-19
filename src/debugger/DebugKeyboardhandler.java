package debugger;

import BrainFunk.Interpreter;
import BrainFunk.Parser;
import BrainFunk.Token;
import nootovich.nglib.NGKeyHandler;
import nootovich.nglib.NGKeys;
import nootovich.nglib.NGUtils;

import static debugger.DebuggerRenderer.*;

public class DebugKeyboardhandler extends NGKeyHandler {
    @Override
    public void onKeyDn(int key, char chr) {
        int savedRow = Interpreter.ip < Interpreter.tokens.length ? Interpreter.tokens[Interpreter.ip].row : 0;

        switch (key) {
            case NGKeys.ESCAPE -> {
                NGUtils.info("debugger.Debugger terminated by user.");
                System.exit(0);
            }
            case NGKeys.SPACEBAR -> {
                if (Interpreter.finished) {
                    Interpreter.restart();
                    return;
                }
                if (Interpreter.tokens[Interpreter.ip].type == Token.Type.WRD) {

                    // TODO: cache parsed macros
                    Token[] macro = new Token[]{Interpreter.tokens[Interpreter.ip]};
                    int     n     = Interpreter.ip + Parser.parseMacroCall(macro, null).length;
                    while (Interpreter.ip < n) Interpreter.execute();

                } else if (Interpreter.tokens[Interpreter.ip].type == Token.Type.JEZ || Interpreter.tokens[Interpreter.ip].type == Token.Type.URS) {

                    int target = Interpreter.tokens[Interpreter.ip].num + 1;
                    while (Interpreter.ip < Interpreter.tokens.length - 1 && Interpreter.ip != target) Interpreter.execute();

                } else Interpreter.execute();
            }
            case NGKeys.ENTER -> {
                if (!Interpreter.finished) Interpreter.execute();
            }
            default -> {
            }
        }

        if (!Interpreter.finished) {
            int newOffsetY = codeOffsetY - (savedRow - Interpreter.tokens[Interpreter.ip].row) * cachedFontH;
            codeOffsetY = NGUtils.clamp(newOffsetY, 0, cachedLinesToBottom * cachedFontH);
        }
    }
}
