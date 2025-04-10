package analyzer;

import BrainFunk.*;
import debugger.DebuggerRenderer;
import java.awt.*;
import nootovich.nglib.*;

import static BrainFunk.Interpreter.*;
import static analyzer.Analyzer.h;
import static analyzer.Analyzer.w;
import static debugger.DebuggerRenderer.*;

public class AnalyzerRenderer extends NGRenderer {

    public static NGVec2i fontSize;

    static {
        font = new Font(Font.MONOSPACED, Font.PLAIN, 17);
    }

    @Override
    public void render(NGGraphics g) {

        g.drawRect(0, 0, w, h, COLOR_BG);

        String progress  = "%d/%d".formatted(ip, ops.length);
        int    progressX = w - fontSize.w() * progress.length();
        g.drawText(progress, progressX, fontSize.h(), Color.ORANGE);

        if (true) {
            for (int i = 0; i < ops.length; i++) {
                Op op = ops[i];
                // if (op.token.visited)
                g.drawText(op.repr(), fontSize.scale(op.token.col, op.token.row).addY(fontSize.y()), op.token.visited ? Color.WHITE : Color.DARK_GRAY);
            }
                execute();
        } else for (int i = 0; i < 1000; i++) {
            if (!finished && ip < ops.length) {
                int px = ip * fontSize.w();
                int x  = px % w;
                int y  = (int) (h * 0.55f - pointer * 3);

                g.drawRectBorder(x, y, 1, 1, Color.WHITE);
                g.drawRectBorder(pointer * fontSize.w() + 3, (256 - ((int) tape[pointer] & 0xFF)), 1, 1, Color.WHITE);
                g.drawText(ops[ip].token.repr(), x, h - px / w * fontSize.h() - 2, Color.WHITE);

                execute();
            } else if (finished) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                restart();
            }
        }
    }

    @Override
    public void reset() { }
}
