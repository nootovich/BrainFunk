package analyzer;

import java.awt.FontMetrics;
import nootovich.nglib.NGMain;
import nootovich.nglib.NGVec2i;

import static analyzer.AnalyzerRenderer.fontSize;
import static nootovich.nglib.NGRenderer.font;

public class Analyzer extends NGMain {

    public static int WINDOW_WIDTH  = 1900;
    public static int WINDOW_HEIGHT = 1000;

    public void main() {
        setTickRate(0);
        setFrameRate(60);
        createWindow(WINDOW_WIDTH, WINDOW_HEIGHT, new AnalyzerRenderer());

        FontMetrics metrics = window.jf.getFontMetrics(font);
        fontSize = new NGVec2i(metrics.charWidth('@'), metrics.getHeight());

        start();
    }
}
