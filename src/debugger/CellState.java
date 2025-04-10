package debugger;

import java.util.Arrays;
import nootovich.nglib.NGUtils;

public class CellState {

    public int       ptr;
    public boolean[] values = new boolean[256];

    CellState(int ptr) {
        this.ptr = ptr;
        // this.values[Interpreter.tape[ptr]] = true;
    }

    public static void checkByteRange(int n) {
        if (n < 0 || n >= 256) NGUtils.error("Value is out of range [0-255]: " + n);
    }

    public void inc() {
        boolean last = values[values.length - 1];
        for (int i = values.length - 1; i > 0; i--) values[i] = values[i - 1];
        values[0] = last;
    }

    public void dec() {
        boolean first = values[0];
        for (int i = 0; i < values.length - 1; i++) values[i] = values[i + 1];
        values[values.length - 1] = first;
    }

    public void addValue(int val) {
        checkByteRange(val);
        values[val] = true;
    }

    public void removeValue(int val) {
        checkByteRange(val);
        values[val] = false;
    }

    public void addValueRange(int valStart, int valEnd) {
        checkByteRange(valStart);
        checkByteRange(valEnd);
        for (int i = valStart; i != valEnd; i++) values[i] = true;
    }

    public void removeValueRange(int valStart, int valEnd) {
        checkByteRange(valStart);
        checkByteRange(valEnd);
        for (int i = valStart; i != valEnd; i++) values[i] = false;
    }

    public void setAll(boolean value) {
        Arrays.fill(values, value);
    }

    public boolean isSingleValue() {
        boolean hasSomeValue = false;
        for (int i = 0; i < values.length; i++) {
            if (!values[i]) continue;
            if (hasSomeValue) return false;
            hasSomeValue = true;
        }
        return hasSomeValue;
    }
}
