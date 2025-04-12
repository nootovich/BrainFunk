package editor;

import nootovich.nglib.NGMain;

public class Editor extends NGMain {

    public static final int SIZE_FACTOR = 100;

    public static String code   = "";
    public static int    cursor = -1;
    public static int    line   = -1;
    public static int    bol    = -1;
    public static int    eol    = -1;

    // public static final int TAPE_LEN = 8;
    // private static      Token[] parsed = new Token[0];
    //                     token_id         pointer
    // private static HashMap<Integer, HashMap<Integer, CellPossibilities>> state = new HashMap<>();
    // private static final ArrayList<ArrayList<Byte>> inputMem  = new ArrayList<>();
    // private static final ArrayList<ArrayList<Byte>> outputMem = new ArrayList<>();
    // private static       int[][]                    offsets   = new int[0][0];

    public void main() {
        code = "[->]\n[->]\n[->]\r\n[->]\n[->]\r\n\n\n\n\r\n\n\r\n[->]\n";

        code   = code.replace("\r", "");
        cursor = code.length() - 1;
        updateCursor();
        createWindow(16 * SIZE_FACTOR, 9 * SIZE_FACTOR, new EditorRenderer());
        start();
        // state.put(0, new HashMap());
        // state.get(0).put(0, new CellPossibilities(new byte[]{0, 1}));
        // state.get(0).put(1, new CellPossibilities(new byte[]{4}));
        // code = "++++++++[>++++[>++>+++>+++>+<<<<-]>+>+>->>+[<]<-]>>.>";
        // code = "->-<" + "+>+[<->[->>>>+<<<<]]>>>>[-<<<<+>>>>]<<<<<[>>+[<<->>[->>>+<<<]]>>>[-<<<+>>>]<<<<<[>>>+[<<<->>>[->>+<<]]>>[-<<+>>]<<<<<[->>>>+<<<<]]]";
        // updateTokens();
        // inoutCaseAdd(inputMem, 0);
        // EditorWindow window = new EditorWindow(16 * factor, 9 * factor);
        // while (true) {
        //     window.repaint();
        //     Thread.sleep(20);
        // }
    }

    public static void updateCursor() {
        final String[] codeLines = getLines();
        line = 0;
        for (int n = 0; line < codeLines.length && n + codeLines[line].length() <= cursor; line++) {
            n += codeLines[line].length() + 1;
        }
        if (cursor >= bol && cursor < eol && bol != -1 && eol != -1) { } else {
            if (cursor == 0 || code.charAt(cursor) == '\n') bol = cursor;
            else for (int i = cursor - 1; i >= 0; i--) {
                if (i == 0 || code.charAt(i) == '\n') {
                    bol = i;
                    break;
                }
            }
            if (cursor == code.length() - 1 || code.charAt(cursor) == '\n') eol = cursor;
            else for (int i = cursor; i < code.length(); i++) {
                if (i == code.length() - 1 || code.charAt(i) == '\n') {
                    eol = i;
                    break;
                }
            }
        }
        System.out.printf("cursor: %d, line: %d, bol: %d, eol: %d\n", cursor, line, bol, eol);
    }

    public static String[] getLines() {
        return code.split("\\r?\\n", -1);
    }

    @Override
    public void onLeftPress() {
        if (cursor == 0) return;
        cursor--;
        updateCursor();
    }

    @Override
    public void onRightPress() {
        if (cursor == code.length() - 1) return;
        cursor++;
        updateCursor();
    }

    @Override
    public void onUpPress() {

    }

    @Override
    public void onDownPress() {

    }

    // public static void handleKeyPress(int keyCode, char keyChar, int modifiers) {
    //     switch (keyCode) {
    //         case KeyEvent.VK_ESCAPE -> System.exit(0);
    //         case KeyEvent.VK_SHIFT, KeyEvent.VK_CONTROL, KeyEvent.VK_ALT, KeyEvent.VK_META, KeyEvent.VK_ENTER -> {}
    //         case KeyEvent.VK_BACK_SPACE -> {
    //             if ((modifiers & InputEvent.CTRL_DOWN_MASK) > 0) {
    //                 // if (codeLastLine().isEmpty()) {
    //                 //     codeRemoveLine();
    //                 // } else {
    //                 //     String  s              = codeLastLine();
    //                 //     int     i              = s.length() - 1;
    //                 //     boolean skipWhitespace = true;
    //                 //     while (i >= 0 && (skipWhitespace || Character.isLetterOrDigit(s.charAt(i)))) {
    //                 //         if (Character.isLetterOrDigit(s.charAt(i))) skipWhitespace = false;
    //                 //         i--;
    //                 //     }
    //                 //     if (i == -1) {
    //                 //         codeRemoveLine();
    //                 //     } else {
    //                 //         codeSetLastLine(s.substring(0, i));
    //                 //     }
    //                 // }
    //                 // } else if (codeLastLine().isEmpty()) {
    //                 //     codeRemoveLine();
    //                 // } else {
    //                 //     codeSetLastLine(codeLastLine().substring(0, codeLastLine().length() - 1));
    //                 // }
    //                 boolean skipWhitespace = true;
    //                 int     i              = code.length() - 1;
    //                 while (i >= 0 && (skipWhitespace || !Character.isWhitespace(code.charAt(i)))) {
    //                     if (!Character.isWhitespace(code.charAt(i))) skipWhitespace = false;
    //                     i--;
    //                 }
    //                 if (i == -1) {
    //                     code = "";
    //                 } else {
    //                     code = code.substring(0, i);
    //                 }
    //
    //             } else if (!code.isEmpty()) code = code.substring(0, code.length() - 1);
    //         }
    //         default -> code += keyChar;
    //     }
    //     updateTokens();
    // }
    //
    // private static void updateTokens() {
    //     parsed = EditorParser.parse();
    //     for (int i = 0; i < parsed.length; i++) {
    //         i = EditorInterpreter.execute(i);
    //     }
    //     System.out.println(state);
    //     // offsets    = new int[parsed.length + 1][0];
    //     // offsets[0] = new int[]{0};
    //     // possibleStates = new CellPossibilities[]{};
    //     // System.out.println(Arrays.toString(possibleStates));
    // }
    //
    // private static HashMap<Integer, CellPossibilities> hmCopy(HashMap<Integer, CellPossibilities> source) {
    //     Integer[]           k = source.keySet().toArray(new Integer[0]);
    //     CellPossibilities[] v = source.values().toArray(new CellPossibilities[0]);
    //
    //     HashMap<Integer, CellPossibilities> result = new HashMap<>();
    //     for (int i = 0; i < k.length; i++) {
    //         CellPossibilities nv = new CellPossibilities(v[i].values.clone());
    //         result.put(k[i], nv);
    //     }
    //
    //     return result;
    // }

    // private static String codeLastLine() {
    //     return code.get(code.size() - 1);
    // }
    //
    // private static int codeLastLineIndex() {
    //     return code.size() - 1;
    // }
    //
    // private static void codeRemoveLine() {
    //     int i = codeLastLineIndex();
    //     if (i == 0) {
    //         code.set(0, "");
    //     } else {
    //         code.remove(i);
    //     }
    // }
    //
    // private static void codeSetLastLine(String s) {
    //     code.set(codeLastLineIndex(), s);
    // }

    // private static ArrayList<Byte> inoutMemLast(ArrayList<ArrayList<Byte>> inout) {
    //     return inout.get(inout.size() - 1);
    // }
    //
    // private static void inoutMemSet(ArrayList<ArrayList<Byte>> inout, int i, int j, byte value) {
    //     // if (i < 0 || i > inputMem.size() - 1) {
    //     //     NGUtils.error(("Index %d is out of bounds for inputMem length %d").formatted(i, inputMem.size() - 1));
    //     // }
    //
    //     // ArrayList<Byte> inputCase = inputMem.get(i);
    //     // if (j < 0 || j > inputCase.size() - 1) {
    //     //     NGUtils.error(("Index %d is out of bounds for inputCase length %d").formatted(j, inputCase.size() - 1));
    //     // }
    //
    //     // inputCase.set(j, value);
    // }
    //
    // private static void inoutCaseAdd(ArrayList<ArrayList<Byte>> inout, int... values) {
    //     if (!inout.isEmpty() && values.length != inoutMemLast(inout).size()) {
    //         NGUtils.error("Amount of provided values does not match the length of input memory cases");
    //     }
    //     ArrayList<Byte> a = new ArrayList<>();
    //     for (int b: values) a.add((byte) b);
    //     inout.add(a);
    // }
    //
    // public static class EditorWindow extends JFrame {
    //
    //     private static int wx, wy, ww, wh;
    //     private final Insets        insets;
    //     private final BufferedImage buffer;
    //     private final Graphics2D    g2d;
    //     private final Font          font = new Font("Roboto Mono", Font.PLAIN, 16);
    //     private final FontMetrics   metrics;
    //
    //     private static int fontH;
    //     private static int fontW;
    //
    //     EditorWindow(int width, int height) {
    //         pack();
    //
    //         Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    //         wx = (screenSize.width - width) / 2;
    //         wy = (screenSize.height - height) / 2;
    //         ww = width;
    //         wh = height;
    //
    //         insets = getInsets();
    //         buffer = new BufferedImage(ww, wh, BufferedImage.TYPE_INT_RGB);
    //
    //         g2d = (Graphics2D) buffer.getGraphics();
    //         g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
    //         g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    //         g2d.setFont(font);
    //
    //         metrics = g2d.getFontMetrics();
    //         fontH   = metrics.getHeight();
    //         fontW   = metrics.charWidth('@');
    //
    //         setSize(ww + insets.left + insets.right, wh + insets.top + insets.bottom);
    //         setLocation(wx, wy);
    //         setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    //         setVisible(true);
    //
    //         // TODO: point this to my own KeyListener class
    //         addKeyListener(new KeyAdapter() {
    //             @Override
    //             public void keyPressed(KeyEvent e) {
    //                 handleKeyPress(e.getKeyCode(), e.getKeyChar(), e.getModifiersEx());
    //             }
    //         });
    //         addMouseMotionListener(new MouseMotionAdapter() {});
    //         addMouseListener(new MouseAdapter() {
    //             @Override
    //             public void mouseClicked(MouseEvent e) {}
    //         });
    //         addMouseWheelListener(new MouseAdapter() {
    //             @Override
    //             public void mouseWheelMoved(MouseWheelEvent e) {
    //             }
    //         });
    //     }
    //
    //     public void paint(Graphics g) {
    //         g2d.setColor(new Color(0x424269));
    //         g2d.fillRect(0, 0, ww, wh);
    //
    //         // code
    //         {
    //             g2d.setColor(Color.WHITE);
    //             int x = (int) (ww * 0.5f - metrics.stringWidth(code) * 0.5f);
    //             int y = wh / 2;
    //             g2d.drawString(code, x, y);
    //         }
    //
    //         // branches
    //         {
    //             g2d.setColor(Color.CYAN);
    //             g2d.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL));
    //
    //             int prevY = (int) (wh * 0.75f - 0 * fontH * 0.5f);
    //             for (Map.Entry<Integer, HashMap<Integer, CellPossibilities>> entry: state.entrySet()) {
    //                 Integer                             token         = entry.getKey();
    //                 HashMap<Integer, CellPossibilities> possibilities = entry.getValue();
    //                 int                                 x             = (int) (ww * 0.5f - metrics.stringWidth(code) * 0.5f + token * fontW);
    //                 for (Map.Entry<Integer, CellPossibilities> e: possibilities.entrySet()) {
    //                     Integer           ptr    = e.getKey();
    //                     CellPossibilities values = e.getValue();
    //                     int               y      = (int) (wh * 0.75f - ptr * fontH * 0.5f);
    //                     g2d.drawLine(x - fontW, prevY, x, y);
    //                     prevY = y;
    //                 }
    //             }
    //
    //             // int prevY = (int) (wh * 0.75f - offsets[0] * fontH * 0.5f);
    //             // for (int i = 1; i < offsets.length; i++) {
    //             //     int x = (int) (ww * 0.5f - metrics.stringWidth(code) * 0.5f + i * fontW);
    //             //     int y = (int) (wh * 0.75f - offsets[i] * fontH * 0.5f);
    //             //     switch (parsed[i - 1].type) {
    //             //         case JEZ -> {
    //             //             int   depth      = 1;
    //             //             Color savedColor = g2d.getColor();
    //             //             g2d.setColor(Color.LIGHT_GRAY);
    //             //             for (int j = i; j < parsed.length && depth > 0; j++) {
    //             //                 if (parsed[j].type == Token.Type.JEZ) depth++;
    //             //                 else if (parsed[j].type == Token.Type.JNZ) depth--;
    //             //                 int zx = (int) (ww * 0.5f - metrics.stringWidth(code) * 0.5f + j * fontW);
    //             //                 g2d.drawLine(zx, y, zx, y);
    //             //             }
    //             //             g2d.setColor(savedColor.darker().darker());
    //             //         }
    //             //         case JNZ -> g2d.setColor(g2d.getColor().brighter().brighter());
    //             //     }
    //             //     g2d.drawLine(x - fontW, prevY, x, y);
    //             //     prevY = y;
    //             // }
    //         }
    //
    //         // input cases
    //         {
    //             g2d.setColor(Color.WHITE);
    //             // for (int i = 0; i < inputMem.size(); i++) {
    //             //     ArrayList<Byte> inputCase = inputMem.get(i);
    //             //     for (int j = 0; j < inputCase.size(); j++) {
    //             //         int x = j * fontW * 3 + fontW;
    //             //         int y = (i + 1) * fontH;
    //             //         g2d.drawString(Debugger.DebugWindow.hex(inputCase.get(j)), x, y);
    //             //     }
    //             // }
    //         }
    //
    //         // output cases
    //         {
    //             g2d.setColor(Color.WHITE);
    //             // for (int i = 0; i < outputMem.size(); i++) {
    //             //     ArrayList<Byte> outputCase = outputMem.get(i);
    //             //     for (int j = 0; j < outputCase.size(); j++) {
    //             //         int x = ww - (outputCase.size() - j) * fontW * 3 - fontW;
    //             //         int y = (i + 1) * fontH;
    //             //         g2d.drawString(Debugger.DebugWindow.hex(outputCase.get(j)), x, y);
    //             //     }
    //             // }
    //         }
    //
    //         g.drawImage(buffer, insets.left, insets.top, this);
    //     }
    // }
    //
    // private static class EditorParser {
    //
    //     public static Token[] lex() {
    //         Stack<Token> lexed = new Stack<>();
    //         for (int col = 0; col < code.length(); col++) {
    //             char c = code.charAt(col);
    //             if (c == '\n' || c == '\r' || c == '\t') NGUtils.error("Invalid whitespace character");
    //
    //             else if (c == '+') lexed.push(new Token(Token.Type.INC, "editor", 0, col));
    //             else if (c == '-') lexed.push(new Token(Token.Type.DEC, "editor", 0, col));
    //             else if (c == '>') lexed.push(new Token(Token.Type.RGT, "editor", 0, col));
    //             else if (c == '<') lexed.push(new Token(Token.Type.LFT, "editor", 0, col));
    //             else if (c == ',') lexed.push(new Token(Token.Type.INP, "editor", 0, col));
    //             else if (c == '.') lexed.push(new Token(Token.Type.OUT, "editor", 0, col));
    //             else if (c == '[') lexed.push(new Token(Token.Type.JEZ, "editor", 0, col));
    //             else if (c == ']') lexed.push(new Token(Token.Type.JNZ, "editor", 0, col));
    //             else lexed.push(new Token(Token.Type.NOP, "editor", 0, col));
    //         }
    //         Token[] result = new Token[lexed.size()];
    //         for (int i = result.length - 1; i >= 0; i--) {
    //             result[i] = lexed.pop();
    //         }
    //         return result;
    //     }
    //
    //     public static Token[] parse() {
    //         Token[]        lexed = lex();
    //         Stack<Integer> jumps = new Stack<>();
    //         for (int i = 0; i < lexed.length; i++) {
    //             switch (lexed[i].type) {
    //                 case INC, DEC, RGT, LFT, INP, OUT -> {}
    //                 case JEZ -> jumps.push(i);
    //                 case JNZ -> {
    //                     if (jumps.isEmpty()) NGUtils.error("Unmatched brackets.\n" + lexed[i]);
    //                     int jmp = jumps.pop();
    //                     lexed[jmp].num = i;
    //                     lexed[i].num   = jmp;
    //                 }
    //                 default -> NGUtils.error("Invalid token %n%s".formatted(lexed[i]));
    //             }
    //         }
    //         // if (!jumps.isEmpty()) NGUtils.error("Unmatched brackets.\n" + lexed[jumps.pop()]);
    //         return lexed;
    //     }
    //
    // }
    //
    // private static class EditorInterpreter {
    //
    //     public static int execute(int i) {
    //         Token t = parsed[i];
    //         switch (t.type) {
    //             case INC -> {
    //                 HashMap<Integer, CellPossibilities> newState = hmCopy(state.get(i));
    //                 newState.forEach((k, v) -> v.inc());
    //                 state.get(i).forEach((k, v) -> v.processed = true);
    //                 state.put(i + 1, newState);
    //             }
    //             case DEC -> {
    //                 HashMap<Integer, CellPossibilities> newState = hmCopy(state.get(i));
    //                 newState.forEach((k, v) -> v.dec());
    //                 state.get(i).forEach((k, v) -> v.processed = true);
    //                 state.put(i + 1, newState);
    //             }
    //             case RGT -> {
    //                 HashMap<Integer, CellPossibilities> newState = new HashMap<>();
    //                 for (Integer k: state.get(i).keySet()) {
    //                     for (int j = i; j >= 0; j--) {
    //                         if (state.get(j).containsKey(k + 1)) {
    //                             newState.merge(k + 1, hmCopy(state.get(j)).get(k + 1), (a, b) -> new CellPossibilities(a.values, b.values));
    //                             break;
    //                         }
    //                     }
    //                     newState.putIfAbsent(k + 1, new CellPossibilities());
    //                 }
    //                 state.get(i).forEach((k, v) -> v.processed = true);
    //                 state.put(i + 1, newState);
    //             }
    //             case LFT -> {
    //                 HashMap<Integer, CellPossibilities> newState = new HashMap<>();
    //                 for (Integer k: state.get(i).keySet()) {
    //                     for (int j = i; j >= 0; j--) {
    //                         if (state.get(j).containsKey(k - 1)) {
    //                             newState.merge(k - 1, hmCopy(state.get(j)).get(k - 1), (a, b) -> new CellPossibilities(a.values, b.values));
    //                         }
    //                     }
    //                 }
    //                 state.get(i).forEach((k, v) -> v.processed = true);
    //                 state.put(i + 1, newState);
    //                 // HashMap<Integer, CellPossibilities> temp     = hmCopy(state.get(i));
    //                 // HashMap<Integer, CellPossibilities> newState = new HashMap<>();
    //                 // temp.forEach((k, v) -> newState.put(k - 1, v));
    //                 // state.put(i + 1, newState);
    //             }
    //             case INP, OUT, NOP -> {
    //                 HashMap<Integer, CellPossibilities> newState = hmCopy(state.get(i));
    //                 state.get(i).forEach((k, v) -> v.processed = true);
    //                 state.put(i + 1, newState);
    //             }
    //             case JEZ -> {
    //                 for (Map.Entry<Integer, CellPossibilities> entry: state.get(i).entrySet()) {
    //                     boolean hasZero  = entry.getValue().hasZero();
    //                     boolean hasOther = !hasZero || entry.getValue().values.length > 1;
    //
    //                     if (hasZero) {
    //                         int depth = 1;
    //                         for (int j = i + 1; j < parsed.length; j++) {
    //                             // TODO: use parsed[i].num for finding end of loop
    //                             if (parsed[j].type == Token.Type.JEZ) depth++;
    //                             if (parsed[j].type == Token.Type.JNZ) depth--;
    //                             if (depth == 0) {
    //                                 HashMap<Integer, CellPossibilities> temp = new HashMap<>();
    //                                 temp.merge(entry.getKey(), new CellPossibilities(), (a, b) -> new CellPossibilities(a.values, b.values));
    //                                 state.put(j + 1, temp);
    //                                 break;
    //                             }
    //                         }
    //                     }
    //                     if (hasOther) {
    //                         HashMap<Integer, CellPossibilities> temp     = hmCopy(state.get(i));
    //                         HashMap<Integer, CellPossibilities> newState = new HashMap<>();
    //                         temp.forEach((k, v) -> {
    //                             byte[] valuesWithoutZero = v.valuesWithoutZero();
    //                             if (valuesWithoutZero.length > 0) {
    //                                 newState.merge(k, new CellPossibilities(valuesWithoutZero), (a, b) -> new CellPossibilities(a.values, b.values));
    //                             }
    //                         });
    //                         state.get(i).forEach((k, v) -> v.processed = true);
    //                         state.put(i + 1, newState);
    //                     }
    //                 }
    //             }
    //             case JNZ -> {
    //                 for (Map.Entry<Integer, CellPossibilities> entry: state.get(i).entrySet()) {
    //                     boolean hasZero  = entry.getValue().hasZero();
    //                     boolean hasOther = !hasZero || entry.getValue().values.length > 1;
    //                     if (hasZero) {
    //                         state.put(i + 1, hmCopy(state.get(i)));
    //                     }
    //                     if (hasOther) {
    //                         int                                 j        = parsed[i].num + 1;
    //                         HashMap<Integer, CellPossibilities> temp     = hmCopy(state.get(i));
    //                         HashMap<Integer, CellPossibilities> newState = hmCopy(state.get(j));
    //                         for (Map.Entry<Integer, CellPossibilities> e: temp.entrySet()) {
    //                             newState.merge(e.getKey(), e.getValue(), (a, b) -> new CellPossibilities(a.values, b.values));
    //                         }
    //                         state.get(i).forEach((k, v) -> v.processed = true);
    //                         state.put(j, newState);
    //                         return j - 1;
    //                     }
    //                 }
    //             }
    //             default -> NGUtils.error("Invalid token %n%s".formatted(t));
    //         }
    //         return i;
    //     }
    //
    //     public static int getOffset(Token t, int pos) {
    //         return switch (t.type) {
    //             case INC, DEC, INP, OUT, JEZ, JNZ, NOP -> pos;
    //             case RGT -> pos + 1;
    //             case LFT -> pos - 1;
    //             case NUM, STR, PTR, RET, WRD, COL, SCL, URS, URE, IMP, SYS, ERR -> {
    //                 NGUtils.error("Not implemented");
    //                 yield Integer.MAX_VALUE;
    //             }
    //         };
    //     }
    // }
    //
    // public static class CellPossibilities {
    //
    //     private byte[]  values;
    //     public  boolean processed = false;
    //
    //     CellPossibilities() {
    //         values = new byte[]{0};
    //     }
    //
    //     CellPossibilities(byte n) {
    //         values = new byte[]{n};
    //     }
    //
    //     CellPossibilities(int n) {
    //         values = new byte[]{(byte) n};
    //     }
    //
    //     CellPossibilities(int start, int end) {
    //         values = new byte[end - start + 1];
    //         for (int i = 0; i <= end; i++) {
    //             values[i] = (byte) (i + start);
    //         }
    //     }
    //
    //     CellPossibilities(byte[] values) {
    //         if (values.length == 0) NGUtils.error("Unreachable");
    //         this.values = values;
    //     }
    //
    //     CellPossibilities(int[] values) {
    //         if (values.length == 0) NGUtils.error("Unreachable");
    //         this.values = new byte[values.length];
    //         for (int i = 0; i < values.length; i++) {
    //             this.values[i] = (byte) values[i];
    //         }
    //     }
    //
    //     CellPossibilities(byte[] valuesA, byte[] valuesB) {
    //         if (valuesA.length == 0 || valuesB.length == 0) NGUtils.error("Unreachable");
    //         byte[] temp = new byte[valuesA.length + valuesB.length];
    //         System.arraycopy(valuesA, 0, temp, 0, valuesA.length);
    //         System.arraycopy(valuesB, 0, temp, valuesA.length, valuesB.length);
    //         Arrays.sort(temp);
    //         ArrayList<Byte> temp2 = new ArrayList<>();
    //         for (int i = 0; i < temp.length; i++) {
    //             if (i > 0 && temp[i - 1] == temp[i]) continue;
    //             temp2.add(temp[i]);
    //         }
    //         var temp3 = temp2.toArray(Byte[]::new);
    //         this.values = new byte[temp3.length];
    //         for (int i = 0; i < temp3.length; i++) {
    //             this.values[i] = temp3[i].byteValue();
    //         }
    //     }
    //
    //
    //     public void add(int val) {
    //         byte[] temp = values;
    //         values = new byte[values.length + 1];
    //         System.arraycopy(temp, 0, values, 0, temp.length);
    //         values[values.length - 1] = (byte) val;
    //         Arrays.sort(values);
    //     }
    //
    //     public void remove(int index) {
    //         byte[] temp = values;
    //         values = new byte[values.length - 1];
    //         System.arraycopy(temp, 0, values, 0, index);
    //         System.arraycopy(temp, index, values, index, temp.length - index);
    //     }
    //
    //     public void inc() {
    //         for (int i = 0; i < values.length; i++) {
    //             values[i]++;
    //         }
    //     }
    //
    //     public void dec() {
    //         for (int i = 0; i < values.length; i++) {
    //             values[i]--;
    //         }
    //     }
    //
    //     public boolean hasZero() {
    //         for (int i = 0; i < values.length; i++) {
    //             if (values[i] == 0) return true;
    //         }
    //         return false;
    //     }
    //
    //     public byte[] valuesWithoutZero() {
    //         if (!hasZero()) return values;
    //
    //         byte[] result = new byte[values.length - 1];
    //         for (int i = 0, j = 0; i < values.length; i++) {
    //             if (values[i] == 0) continue;
    //             result[j++] = values[i];
    //         }
    //
    //         return result;
    //     }
    //
    //     @Override
    //     public String toString() {
    //         if (values.length == 0) return "<none>";
    //         StringBuilder sb = new StringBuilder();
    //         sb.append("(%02X".formatted(values[0]));
    //         for (int i = 1; i < values.length; i++) {
    //             sb.append(", %02X".formatted(values[i]));
    //         }
    //         return sb.append(')').toString();
    //         // return "(%02X:%02X)".formatted(rangeStart, rangeEnd);
    //     }
    // }
}
