import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Stack;

import static java.lang.Math.log10;

public class Debugger {

    public static String filename;

    public static void main(String[] args) throws InterruptedException {
        if (args.length < 1) error("Please provide a .bf or .bfn file as a command line argument.");
        String filepath = args[0];
        filename = new File(filepath).getName();
        if (!filename.endsWith(".bfn") && !filename.endsWith(".bf")) error("Invalid file format. Please provide a .bf or .bfn file.");
        info("Debugging %s file.".formatted(filename));

        DebugWindow debugWindow = new DebugWindow(1200, 800, filepath);
        while (true) {
            debugWindow.repaint();
            Thread.sleep(30);
        }
    }

    private static void info(String message) {
        StackTraceElement src = Thread.currentThread().getStackTrace()[2];
        System.out.printf("%s:%d [INFO]: %s%n", src.getFileName(), src.getLineNumber(), message);
    }

    private static void error(String message) {
        StackTraceElement errSrc = Thread.currentThread().getStackTrace()[2];
        System.out.printf("%s:%d [ERROR]: %s%n", errSrc.getFileName(), errSrc.getLineNumber(), message);
        System.exit(1);
    }

    private static class DebugWindow extends JFrame {

        public int x, y, w, h;
        public int codeX, codeY, codeW, codeH;
        public int tapeX, tapeY, tapeW, tapeH;

        private       Graphics2D    g2d;
        private final Font          font        = new Font(Font.MONOSPACED, Font.BOLD, 18);
        private final BufferedImage buffer;
        private       String[]      filedata    = {""};
        private       int           cachedFontH = 0;
        private       int           cachedFontW = 0;

        private final Color COLOR_BG   = new Color(0x3D4154);
        private final Color COLOR_DATA = new Color(0x4C5470);

        private static final char[] hexLookup = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

        private static int     ip     = 0;
        private static Token[] tokens = new Token[0];

        DebugWindow(int w, int h, String filepath) throws InterruptedException {
            this.filedata = FileSystem.loadFile(filepath).split("\n");

            Token[] lexedTokens = DebugLexer.lexFile(filepath);
            // tokens = DebugLexer.lexFile(filepath);
            // while (tokens[ip].type == Token.Type.MACRODEF) ip++;
            info("Lexer OK.");

            this.tokens = filename.endsWith(".bfn") ? Parser.parseTokens(lexedTokens) : lexedTokens;
            // DebugParser.parseMacros(tokens);
            info("Parser OK.");

            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            this.w = w;
            this.h = h;
            this.x = (screenSize.width-w)/2;
            this.y = (screenSize.height-h)/2;

            this.codeX = this.w/14;
            this.codeY = codeX;
            this.codeW = this.w-codeX*7;
            this.codeH = this.h-codeY*2;

            this.tapeY = codeY;
            this.tapeH = codeH;
            this.tapeW = this.w-codeX*3-codeW;
            this.tapeX = this.w-codeX-tapeW;

            buffer = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            g2d    = (Graphics2D) buffer.getGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setBackground(COLOR_BG);
            g2d.setFont(font);
            FontMetrics metrics = g2d.getFontMetrics();
            cachedFontH = metrics.getAscent();
            cachedFontW = (int) metrics.getStringBounds("@", null).getWidth();

            addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_SPACE && ip < tokens.length-1) {
                        while (tokens[ip].type == Token.Type.MACRODEF) ip++;
                        DebugInterpreter.debugExecuteBrainFunk(tokens[ip++]);
                    }
                }
            });

            pack();
            Insets insets = getInsets();
            setSize(w+insets.left+insets.right, h+insets.top+insets.bottom);
            setLocation(x, y);
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setVisible(true);
        }

        public void paint(Graphics g) {
            g2d.clearRect(0, 0, w, h);

            g2d.setColor(COLOR_DATA);
            g2d.fillRect(codeX, codeY, codeW, codeH);
            g2d.fillRect(tapeX, tapeY, tapeW, tapeH);


            g2d.setColor(Color.LIGHT_GRAY);
            int y = codeY+cachedFontH;
            for (int i = 0; i < filedata.length; i++) {
                g2d.drawString(filedata[i], codeX+8, y);
                y += cachedFontH;
            }

            x = tapeX;
            y = tapeY+cachedFontH;
            int valW = (int) g2d.getFontMetrics().getStringBounds("---", null).getWidth();
            for (int i = 0; i < DebugInterpreter.tape.length; i++) {
                String val = hex(DebugInterpreter.tape[i]);
                g2d.drawString(val, x+8, y);
                x += valW;
                if (x >= w-codeX-valW) {
                    x = tapeX;
                    y += cachedFontH;
                }
            }
            {
                Token tk  = tokens[ip];
                int   ipX = codeX+tk.col*cachedFontW+8;
                int   ipY = codeY+tk.row*cachedFontH+5;
                int   ipW = cachedFontW;
                int   ipH = cachedFontH;
                switch (tk.type) {
                    case NUMBER -> ipW = (int) (Math.floor(log10(tk.numValue))*cachedFontW+cachedFontW);
                    case MACRO -> ipW = tk.strValue.length()*cachedFontW;
                }
                g2d.setColor(Color.ORANGE);
                g2d.drawRect(ipX, ipY, ipW, ipH);
            }
            Insets insets = getInsets();
            g.drawImage(buffer, insets.left, insets.top, this);
        }

        private static String hex(int n) {
            return String.valueOf(hexLookup[n>>4])+hexLookup[n%16];
        }
    }

    private static class DebugLexer extends Lexer {

        public static Token[] lexFile(String filepath) {
            ArrayList<Token> tokens = new ArrayList<>();
            filename = new File(filepath).getName();
            String rawData = FileSystem.loadFile(filepath);
            if (rawData == null) error("Could not get file data.");
            String[] lines = rawData.split("\n");
            for (int row = 0; row < lines.length; row++) {
                tokens.addAll(lexLine(lines[row], row));
                // tokens.add(new Token(Token.Type.DEBUG_NEWLINE, filename, row, lines.length));
            }
            return tokens.toArray(new Token[0]);
        }
    }

    private static class DebugParser {

        private static HashMap<String, Token[]> macros = new HashMap<>();

        private static void parseMacros(Token[] tokens) {
            for (Token tk: tokens) {
                if (tk.type == Token.Type.MACRODEF) {
                    if (macros.containsKey(tk.strValue)) error("Redefinition of a macro %s.".formatted(tk));
                    macros.put(tk.strValue, tk.macroTokens);
                } else if (tk.type == Token.Type.MACRO && !macros.containsKey(tk.strValue)) error("Undefined macro %s.".formatted(tk));
            }
        }
    }

    private static class DebugInterpreter {

        private static final int TAPE_LEN       = 256;
        private static final int REPETITION_CAP = 1000;

        private static byte[] tape    = new byte[TAPE_LEN];
        private static int    pointer = 0;

        private static int savedVal = -1;

        private static Scanner         input       = new Scanner(System.in);
        private static ArrayList<Byte> inputBuffer = new ArrayList<>();
        private static Stack<Integer>  ptrHistory  = new Stack<>();

        private static void debugExecuteBrainFunk(Token tk) {
            switch (tk.type) {
                case ADD -> tape[pointer] += (byte) getVal();
                case SUB -> tape[pointer] -= (byte) getVal();
                case PTRADD -> ptradd(getVal());
                case PTRSUB -> ptradd(-getVal());
                case WHILE -> {
                    // if (i == tokens.length-1) error("Unmatched brackets at: "+tokens[i]);
                    // int start = i;
                    // int depth = getVal();
                    // int len   = 0;
                    // while (depth > 0) {
                    //     len++;
                    //     tk = tokens[++i];
                    //     if (tk.type == Token.Type.WHILE) depth += getVal();
                    //     else if (tk.type == Token.Type.ENDWHILE) depth -= getVal();
                    //     if (depth != 0 && i == tokens.length-1) error("Unmatched brackets at: "+tokens[start]);
                    // }
                    // Token[] innerTokens = new Token[len-1];
                    // System.arraycopy(tokens, start+1, innerTokens, 0, innerTokens.length);
                    // while (tape[pointer] != 0) output.append(privateExecuteBrainFunk(innerTokens));
                }
                case ENDWHILE -> {} // error("Unmatched brackets at: "+tokens[i]);
                case WRITE -> write(getVal());
                case READ -> read(getVal());
                case NUMBER -> saveVal(tk);
                case STRING -> {
                    int val = getVal();
                    for (int j = 0; j < val; j++) {
                        for (int k = 0; k < tk.strValue.length(); k++) {
                            tape[pointer] = (byte) tk.strValue.charAt(k);
                            ptradd(1);
                        }
                    }
                }
                case POINTER -> {
                    int val = getVal();
                    for (int j = 0; j < val; j++) {
                        ptrHistory.push(pointer);
                        pointer = tk.numValue;
                    }
                }
                case RETURN -> {
                    int val = getVal();
                    for (int j = 0; j < val; j++) {
                        if (ptrHistory.isEmpty()) error("Not enough pointer history for: "+tk);
                        pointer = ptrHistory.pop();
                    }
                }
                case MACRO -> {
                    if (!DebugParser.macros.containsKey(tk.strValue)) error("Undefined macro %s.".formatted(tk));
                    for (Token t: DebugParser.macros.get(tk.strValue))
                        debugExecuteBrainFunk(t);
                }
                default -> error("Unknown token type `"+tk.type+"`");
            }
        }

        private static void ptradd(int n) {
            pointer = ((pointer+n)%TAPE_LEN+TAPE_LEN)%TAPE_LEN;
        }

        private static void saveVal(Token tk) {
            if (savedVal >= 0) error("Two consecutive numbers after one another are not supported: "+tk);
            savedVal = tk.numValue;
        }

        private static int getVal() {
            if (savedVal < 0) return 1;
            int temp = savedVal;
            savedVal = -1;
            return temp;
        }

        private static void read(int amount) {
            for (int repetitions = 0; inputBuffer.size() < amount && repetitions < REPETITION_CAP; repetitions++) {
                System.out.print("Awaiting input: ");
                char[] in = input.nextLine().toCharArray();
                for (char inChar: in) inputBuffer.add((byte) inChar);
            }
            if (inputBuffer.size() < amount) error("Not enough data for `READ` token. This should be unreachable unless there is a bug in processing user input.");
            for (int i = 0; i < amount; i++) {
                tape[pointer] = inputBuffer.get(0);
                inputBuffer.remove(0);
            }
        }

        private static void write(int amount) {
            for (int i = 0; i < amount; i++) System.out.print((char) tape[pointer]);
        }
    }
}
