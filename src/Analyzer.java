import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class Analyzer {

    public static void main(String[] args) {
        if (args.length < 1)
            Utils.error("No file was provided. Please provide a `.bf`, `.bfn` or `.bfnx` file as a command line argument.");
        String filepath = args[0];
        String filename = new File(filepath).getName();
        String[] filenameParts = filename.split("\\.");
        String extension = filenameParts[filenameParts.length - 1];
        Main.ProgramType programType = switch (extension) {
            case "bf" -> Main.ProgramType.BF;
            case "bfn" -> Main.ProgramType.BFN;
            case "bfnx" -> Main.ProgramType.BFNX;
            default -> {
                Utils.error("Invalid file type `%s`. Please provide a `.bf`, `.bfn` or `.bfnx` file as a command line argument.");
                yield Main.ProgramType.ERR;
            }
        };

        String code = FileSystem.loadFile(filepath);

        Token[] lexed = Lexer.lex(code, filepath, programType);
        Token[] parsed = Parser.parse(Token.deepCopy(lexed), filepath);
        Interpreter.loadProgram(parsed, programType);

        Window window = new Window(1900, 1000);
        while (true) {
            window.repaint();
        }
    }

    private static class Window extends JFrame {

        private final int w, h;
        private int cachedFontH, cachedFontW;

        private final Graphics2D g2d;
        private final BufferedImage buffer;
        private final Font font = new Font("Monospaced", Font.PLAIN, 16);

        public enum colorEnum {
            COLOR_BG, COLOR_DATA, COLOR_TEXT, COLOR_TEXT_FADED, COLOR_HIGHLIGHT, COLOR_SELECT, COLOR_CONNECTION
        }

        public Color[] colors = new Color[]{
                new Color(0x153243),
                new Color(0x284B63),
                new Color(0xFFFFFF),
                new Color(0x7EA8BE),
                new Color(0x5EF38C),
                new Color(0xFFC69B),
                new Color(0xD9E985),
        };

        Window(int width, int height) {

            buffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            w = width;
            h = height;
            int x = (screenSize.width - w) / 2;
            int y = (screenSize.height - h) / 2;

            g2d = (Graphics2D) buffer.getGraphics();
//            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
//            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setFont(font);

            FontMetrics metrics = g2d.getFontMetrics();
            cachedFontH = metrics.getHeight();
            cachedFontW = metrics.charWidth('@');

            pack();
            Insets insets = getInsets();
            setSize(width + insets.left + insets.right, height + insets.top + insets.bottom);
            setLocation(0, 0);
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setVisible(true);
        }

        public void paint(Graphics g) {
//            g2d.setColor(colors[colorEnum.COLOR_BG.ordinal()]);
            g2d.setColor(new Color(colors[colorEnum.COLOR_BG.ordinal()].getRed(), colors[colorEnum.COLOR_BG.ordinal()].getGreen(), colors[colorEnum.COLOR_BG.ordinal()].getBlue(), 5));
            g2d.fillRect(0, 0, w, h);

            g2d.setColor(Color.WHITE);
            if (!Interpreter.finished && Interpreter.ip < Interpreter.tokens.length) {
                int px = (Interpreter.ip * cachedFontW);
                int x = px % w;
                int y = (int) (h * 0.75f - Interpreter.pointer * 5);
                g2d.drawRect(x, y, 1, 1);
                g2d.drawRect(x + 2, (255 - ((int) Interpreter.tape[Interpreter.pointer] & 0xFF)) * 2 + 10, 1, 1);
                g2d.drawString(Interpreter.tokens[Interpreter.ip].repr(), x, h - px / w * cachedFontH - 20 );
                Interpreter.execute();
            } else if (Interpreter.finished) {
                Interpreter.restart();
            }

            Insets insets = getInsets();
            g.drawImage(buffer, insets.left, insets.top, this);
        }

    }
}
