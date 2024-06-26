import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

public class Analyzer {

    private static final int    factor = 50;
    private static       String code   = "";

    public static void main(String[] args) throws InterruptedException {
        AnalyzerWindow window = new AnalyzerWindow(16 * factor, 9 * factor);
        while (true) {
            window.repaint();
            Thread.sleep(20);
        }

    }

    public static void handleKeyPress(int key, char ch, int mod) {
        if (key == KeyEvent.VK_ENTER) {
            Token[] lexed  = Lexer.lex(code, "analyzer", Main.ProgramType.BF);
            Token[] parsed = Parser.parse(lexed, "analyzer");
            Interpreter.loadProgram(parsed, Main.ProgramType.BF);
            Interpreter.restart();
        } else if (key == KeyEvent.VK_SPACE) {
            Interpreter.execute();
            System.out.printf("%d: %s %d[%d]",
                              Interpreter.ip,
                              Interpreter.tokens[Interpreter.ip].repr(),
                              Interpreter.tape[Interpreter.pointer],
                              Interpreter.pointer);
        } else if (key == KeyEvent.VK_BACK_SPACE) {
            code = code.substring(0, code.length() - 1);
        } else if (key != KeyEvent.VK_SHIFT && key != KeyEvent.VK_CONTROL && key != KeyEvent.VK_ALT && key != KeyEvent.VK_META) {
            code += ch;
        }
    }

    public static class AnalyzerWindow extends JFrame {

        private static int wx, wy, ww, wh;
        private final Insets        insets;
        private final BufferedImage buffer;
        private final Graphics2D    g2d;
        private final Font          font = new Font("Roboto Mono", Font.PLAIN, 16);
        private final FontMetrics   metrics;

        private static int fontH;
        private static int fontW;

        AnalyzerWindow(int width, int height) {
            pack();

            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            wx = (screenSize.width - width) / 2;
            wy = (screenSize.height - height) / 2;
            ww = width;
            wh = height;

            insets = getInsets();
            buffer = new BufferedImage(ww, wh, BufferedImage.TYPE_INT_RGB);

            g2d = (Graphics2D) buffer.getGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setFont(font);

            metrics = g2d.getFontMetrics();
            fontH   = metrics.getHeight();
            fontW   = metrics.charWidth('@');

            setSize(ww + insets.left + insets.right, wh + insets.top + insets.bottom);
            setLocation(wx, wy);
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setVisible(true);

            // TODO: point this to my own KeyListener class
            addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    handleKeyPress(e.getKeyCode(), e.getKeyChar(), e.getModifiersEx());
                }
            });
            addMouseMotionListener(new MouseMotionAdapter() {});
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {}
            });
            addMouseWheelListener(new MouseAdapter() {
                @Override
                public void mouseWheelMoved(MouseWheelEvent e) {
                }
            });
        }

        public void paint(Graphics g) {
            g2d.setColor(new Color(0x424269));
            g2d.fillRect(0, 0, ww, wh);

            g2d.setColor(Color.GREEN);
            g2d.drawString(code, 0, wh / 2);

            g.drawImage(buffer, insets.left, insets.top, this);
        }
    }

}
