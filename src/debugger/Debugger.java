package debugger;

import BrainFunk.Op;
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

    // public static NGVec4i BUTTON_TOKEN_LIST;

    public static Token[] lexed = new Token[0];

    public void main() {
        setTickRate(0);
        setFrameRate(60);

        createWindow(WINDOW_WIDTH, WINDOW_HEIGHT, new DebuggerRenderer());
        window.jf.setLocation(GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()[0].getDefaultConfiguration().getBounds().getLocation());

        // BUTTON_TOKEN_LIST = new NGVec4i(areaCode.x(), 5, areaCode.y() - 10, areaCode.y() - 10);

        FontMetrics metrics = window.jf.getFontMetrics(font);
        fontSize            = new NGVec2i(metrics.charWidth('@'), metrics.getHeight());
        cachedLinesToBottom = filedata.length - (areaCode.h() - areaPadding.h()) / fontSize.h();
        if (tokens.length > 0) codeOffsetY = NGUtils.clamp(tokens[0].row * fontSize.h() - areaCode.h() / 2, 0, cachedLinesToBottom * fontSize.h());

        start();
    }

    @Override
    public void afterAnyKeyPress(int keyCode, char keyChar) {
        if (finished || ip >= tokens.length) return;
        int tokenTextPos = tokens[ip].row * fontSize.h();
        if (codeOffsetY < tokenTextPos && tokenTextPos < codeOffsetY + areaCode.h()) return;
        codeOffsetY = NGUtils.clamp(tokenTextPos - areaCode.h() / 2, 0, cachedLinesToBottom * fontSize.h());
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
        } else if (ops[ip].type == Op.Type.DEBUG_MACRO) {
            // TODO: cache parsed macros
            int target = ip + ops[ip].num + 1;
            while (ip < ops.length - 1 && ip < target) execute();
        } else if (ops[ip].type == Op.Type.JNZ) {
            int target = ip + 1;
            while (ip < ops.length && ip < target) execute();
        } else {
            execute();
        }
    }

    @Override
    public void onMouseMoved(NGVec2i pos) {
        findMouseToken(pos);
    }

    @Override
    public void onLMBPress(NGVec2i pos) {
        if (false) {// pos.isInside(BUTTON_TOKEN_LIST)) {
            if (mode == NORMAL) {
                mode                = TOKEN_LIST;
                cachedLinesToBottom = tokens.length;
            } else {
                mode                = NORMAL;
                cachedLinesToBottom = filedata.length - areaCode.h() / fontSize.h();
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
        codeOffsetY = NGUtils.clamp(codeOffsetY + direction * fontSize.h() * 3, 0, cachedLinesToBottom * fontSize.h());
        findMouseToken(pos);
    }
}
