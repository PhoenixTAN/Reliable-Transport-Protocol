package utils;

import java.util.ArrayList;

public class GoBackNQueue<T> implements SlidingWindowQueue<T>  {
    private ArrayList<T> buffer;

    protected int tail;
    protected int windowSize;


    public GoBackNQueue(int _windowSize) {
        windowSize = _windowSize;
        buffer = new ArrayList<T>(_windowSize);
        tail = 0;
    }


    public T getDatabyIndex(int seqNum) {
        return buffer.get(seqNum);
    }


    public boolean isWindowFull() {
        if ( tail % windowSize == 0 ) {
            return true;
        }
        return false;
    }


    public boolean isWindowEmpty() {
        if ( tail == 0 ) {
            return true;
        }
        return false;
    }


    public void add(T t) {
        buffer.add(t);
        tail++;
    }


    public T getFirst() {
        if ( buffer.size() < 1 ) {
            return null;
        }
        return buffer.get(0);
    }


    public boolean removeFirst() {
        if ( buffer.size() < 1 ) {
            return false;
        }
        buffer.remove(0);
        return true;
    }


    public void slide(int ackNum, int baseNum) {
        while ( baseNum <= ackNum ) {
            removeFirst();
            baseNum++;
            // update tail
            tail = Math.min(windowSize - 1, buffer.size());
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for ( int i = 0; i < tail; i++ ) {
            sb.append(buffer.get(i));
        }
        return sb.toString() + " tail: " + tail;
    }
}
