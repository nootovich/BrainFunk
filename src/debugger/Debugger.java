package debugger;

import BrainFunk.Parser;
import BrainFunk.Token;
import java.awt.FontMetrics;
import java.awt.GraphicsEnvironment;
import nootovich.nglib.*;

import static BrainFunk.Interpreter.*;
import static debugger.DebuggerRenderer.MODE.NORMAL;
import static debugger.DebuggerRenderer.MODE.TOKEN_LIST;
import static debugger.DebuggerRenderer.*;

public class Debugger extends NGMain {

    public static final int WINDOW_WIDTH  = 1600;
    public static final int WINDOW_HEIGHT = 900;

    public static  NGVec4i BUTTON_TOKEN_LIST;
    private static int     savedRow = 0;

    public void main() {
        setTickRate(0);
        setFrameRate(60);

        createWindow(WINDOW_WIDTH, WINDOW_HEIGHT, new DebuggerRenderer());
        window.jf.setLocation(GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()[1].getDefaultConfiguration().getBounds().getLocation());

        BUTTON_TOKEN_LIST = new NGVec4i(areaCode.x(), 5, areaCode.y() - 10, areaCode.y() - 10);

        FontMetrics metrics = window.jf.getFontMetrics(font);
        cachedFontH         = metrics.getHeight();
        cachedFontW         = metrics.charWidth('@');
        cachedLinesToBottom = filedata.length - areaCode.h() / cachedFontH;

        if (tokens.length > 0) {
            codeOffsetY = NGUtils.clamp(tokens[0].row * cachedFontH - (h - w / 20) / 2, 0, cachedLinesToBottom * cachedFontH);
        }

        start();
    }

    @Override
    public void onAnyKeyPress(int keyCode, char keyChar) {
        savedRow = ip < tokens.length ? tokens[ip].row : 0;
    }

    @Override
    public void afterAnyKeyPress(int keyCode, char keyChar) {
        if (!finished) {
            int newOffsetY = codeOffsetY - (savedRow - tokens[ip].row) * cachedFontH;
            codeOffsetY = NGUtils.clamp(newOffsetY, 0, cachedLinesToBottom * cachedFontH);
        }
    }

    @Override
    public void onEscapePress() {
        exit();
    }

    @Override
    public void onEnterPress() {
        if (!finished) execute();
    }

    @Override
    public void onSpacePress() {
        if (finished) {
            restart();
            return;
        }
        if (tokens[ip].type == Token.Type.WRD) {

            // TODO: cache parsed macros
            Token[] macro = new Token[]{tokens[ip]};
            int     n     = ip + Parser.parseMacroCall(macro, null).length;
            while (ip < n) execute();
        } else if (tokens[ip].type == Token.Type.JEZ || tokens[ip].type == Token.Type.URS) {

            int target = tokens[ip].num + 1;
            while (ip < tokens.length - 1 && ip != target) execute();
        } else execute();
    }

    @Override
    public void onMouseMoved(NGVec2i pos) {
        findMouseToken(pos);
    }

    @Override
    public void onLMBPress(NGVec2i pos) {
        if (pos.isInside(BUTTON_TOKEN_LIST)) {
            if (mode == NORMAL) {
                mode                = TOKEN_LIST;
                cachedLinesToBottom = tokens.length;
            } else {
                mode                = NORMAL;
                cachedLinesToBottom = filedata.length - areaCode.h() / cachedFontH;
            }
            return;
        }
        if (finished || mouseToken == null) return;
        while (ip < tokens.length && !tokens[ip].eq(mouseToken)) {
            execute();
        }
    }

    @Override
    public void onMouseWheel(NGVec2i pos, int direction) {
        codeOffsetY = NGUtils.clamp(codeOffsetY + direction * cachedFontH, 0, cachedLinesToBottom * cachedFontH);
        findMouseToken(pos);
    }

}
