package analyzer;

import BrainFunk.Interpreter;
import BrainFunk.Op;
import java.awt.Color;
import java.awt.Font;
import nootovich.nglib.*;

import static nootovich.nglib.NGMain.h;
import static nootovich.nglib.NGMain.w;

public class AnalyzerRenderer extends NGRenderer {

    private static final Color COLOR_BG        = new Color(0x05153243, true);
    private static final Color COLOR_BG_OPAQUE = new Color(0x153243);

    public static NGVec2i fontSize;

    static {
        font = new Font(Font.MONOSPACED, Font.PLAIN, 17);
    }

    @Override
    public void render(NGGraphics g) {
        g.drawRect(0, 0, w, h, COLOR_BG);

        String progress  = "%d/%d".formatted(Interpreter.ip, Interpreter.ops.length);
        int    progressX = w - fontSize.w() * progress.length();
        g.drawRect(progressX, 0, fontSize.w() * progress.length(), fontSize.h(), COLOR_BG_OPAQUE);
        g.drawText(progress, progressX, fontSize.h(), Color.ORANGE);

        if (true) {
            if (Interpreter.finished) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                Interpreter.restart();
            }

            for (int i = 0; i < 1000 && Interpreter.ip < Interpreter.ops.length; i++) {
                Op op = Interpreter.ops[Interpreter.ip];
                g.drawText(op.repr(), fontSize.scale(op.token.col, op.token.row).addY(fontSize.y()), op.token.visited ? Color.WHITE : Color.DARK_GRAY);
                Interpreter.execute();
            }
        } else for (int i = 0; i < 1000; i++) {
            if (!Interpreter.finished && Interpreter.ip < Interpreter.ops.length) {
                int px = Interpreter.ip * fontSize.w();
                int x  = px % w;
                int y  = (int) (h * 0.55f - Interpreter.pointer * 3);

                g.drawRectBorder(x, y, 1, 1, Color.WHITE);
                g.drawRectBorder(Interpreter.pointer * fontSize.w() + 3, (256 - ((int) Interpreter.tape[Interpreter.pointer] & 0xFF)), 1, 1, Color.WHITE);
                g.drawText(Interpreter.ops[Interpreter.ip].token.repr(), x, h - px / w * fontSize.h() - 2, Color.WHITE);

                Interpreter.execute();
            } else if (Interpreter.finished) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                Interpreter.restart();
            }
        }
    }

    @Override
    public void reset() { }
}
