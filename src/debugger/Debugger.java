package debugger;

import BrainFunk.*;
import java.awt.FontMetrics;
import java.awt.GraphicsEnvironment;
import java.util.HashMap;
import nootovich.nglib.*;

import static BrainFunk.Interpreter.*;
import static debugger.DebuggerRenderer.*;

public class Debugger extends NGMain {

    public static final int WINDOW_WIDTH  = 1600;
    public static final int WINDOW_HEIGHT = 900;
    public static final int SCROLL_SPEED  = 5;

    public enum MODES {
        EXECUTION,
        FLOW_ANALYSIS
    }

    public static MODES mode = MODES.EXECUTION;

    public static HashMap<String, Token[]> tokens     = new HashMap<>();
    public static CellState[]              cellStates = new CellState[TAPE_LEN];

    public void main() {
        setTickRate(0);
        setFrameRate(60);

        createWindow(WINDOW_WIDTH, WINDOW_HEIGHT, new DebuggerRenderer());
        window.jf.setLocation(GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()[1].getDefaultConfiguration().getBounds().getLocation());

        FontMetrics metrics = window.jf.getFontMetrics(font);
        fontSize            = new NGVec2i(metrics.charWidth('@'), metrics.getHeight());
        cachedLinesToBottom = filedata.length - (areaCode.h() - areaPadding.h()) / fontSize.h();
        if (Interpreter.ops.length > 0) codeOffsetY = NGUtils.clamp(Interpreter.ops[0].token.row * fontSize.h() - areaCode.h() / 2, 0, cachedLinesToBottom * fontSize.h());

        start();
    }

    public static void evaluatePossibleStates() {
        CellState cs = cellStates[pointer];
        if (cs == null) {
            cellStates[pointer] = new CellState(pointer);
            cs                  = cellStates[pointer];
        }
        switch (ops[ip].type) {
            case INC -> cs.inc();
            case DEC -> cs.dec();
            case JEZ -> {
                if (tape[pointer] != 0) NGUtils.info("TODO");
                if (cs.isSingleValue() && !cs.values[0]) NGUtils.error("Cell state desync at: " + ops[ip]);
            }
            case JNZ -> {

            }
            case INP -> cs.setAll(true);
            default -> NGUtils.info("TODO");
        }
    }

    @Override
    public void afterAnyKeyPress(int keyCode, char keyChar) {
        int tokenTextPos = ops[finished || ip >= ops.length ? ops.length - 1 : ip].token.row * fontSize.h();
        if (codeOffsetY < tokenTextPos && tokenTextPos < codeOffsetY + areaCode.h()) return;
        codeOffsetY = NGUtils.clamp(tokenTextPos - areaCode.h() / 2, 0, cachedLinesToBottom * fontSize.h());
    }

    @Override
    public void onEPress() {
        mode = MODES.EXECUTION;
    }

    @Override
    public void onFPress() {
        mode = MODES.FLOW_ANALYSIS;
    }

    @Override
    public void onQPress() {
        exit();
    }

    @Override
    public void onRPress() {
        if (finished) restart();
    }

    @Override
    public void onEnterPress() {
        if (!finished) {
            if (mode == MODES.FLOW_ANALYSIS) evaluatePossibleStates();
            execute();
        }
    }

    @Override
    public void onSpacePress() {
        if (finished) return;
        // TODO: cache parsed macros
        if (ops[ip].type == Op.Type.DEBUG_MACRO) {
            int target = ip + ops[ip].num + 1;
            while (ip < ops.length && ip < target) execute();
        } else if (ops[ip].type == Op.Type.JEZ) {
            int target = ops[ip].num + 1;
            while (ip < ops.length && ip < target) execute();
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
        if (finished || mouseToken == null) return;
        while (ip < Interpreter.ops.length && !Interpreter.ops[ip].token.eq(mouseToken)) {
            execute();
        }
    }

    @Override
    public void onMouseWheel(NGVec2i pos, int direction) {
        codeOffsetY = NGUtils.clamp(codeOffsetY + direction * fontSize.h() * SCROLL_SPEED, 0, cachedLinesToBottom * fontSize.h());
        findMouseToken(pos);
    }
}
