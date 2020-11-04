package utils;

import java.util.ArrayList;

public class GoBackNQueue<T> implements SlidingWindowQueue<T>  {
    private ArrayList<T> buffer;

    protected int tail;
    protected int windowSize;

    private int curSeqNum;


    public GoBackNQueue(int _windowSize) {
        windowSize = _windowSize;
        buffer = new ArrayList<T>(_windowSize);
        tail = 0;
        curSeqNum = 0;
    }


    public T getDatabyIndex(int index) {
        return buffer.get(index);
    }


    public boolean isWindowFull() {
        if ( (tail) % windowSize == 0 ) {
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
        tail = isWindowFull() ? tail : ++tail;
    }


    public T getFirst() {
        if ( buffer.size() < 1 ) {
            return null;
        }
        return buffer.get(0);
    }


    public T removeFirst() {
        if(buffer.isEmpty()){
            return null;
        }
        return buffer.remove(0);
    }


    public void slide(int ackSeqNum, int baseSeqNum) {
        while ( baseSeqNum <= ackSeqNum ) {
            removeFirst();
            baseSeqNum++;
            // update tail
            tail = Math.min(windowSize, buffer.size());
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for ( int i = 0; i < tail; i++ ) {
            sb.append(buffer.get(i));
        }
        return sb.toString() + " tail: " + tail;
    }

    public T getTail(){
        if(tail == 0){
            return null;
        }
        return buffer.get(tail - 1);
    }

    public int getCurSeqNum() {
        return curSeqNum;
    }

    public void setCurSeqNum(int curSeqNum) {
        this.curSeqNum = curSeqNum;
    }

    public boolean isBufferFull() {
        return buffer.size() >= 5;
    }
}
