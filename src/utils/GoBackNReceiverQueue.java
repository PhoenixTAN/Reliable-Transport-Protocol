package utils;

import java.util.ArrayList;
import java.util.List;

public class GoBackNReceiverQueue<T> implements SlidingWindowQueue<T>  {
    private ArrayList<T> buffer;

    protected int tail;
    protected int windowSize;

    private List<Integer> expectedSeqNumArray;
    private int expectedSeqNum;


    public GoBackNReceiverQueue(int _windowSize) {
        windowSize = _windowSize;
        buffer = new ArrayList<T>(_windowSize);
        tail = 0;
        expectedSeqNumArray = new ArrayList<>(_windowSize);
        for(int i = 0; i < _windowSize; i++){
            expectedSeqNumArray.add(i);
        }
    }



    public T getDatabyIndex(int index) {
        if(index >= buffer.size()){
            return null;
        }
        return buffer.get(index);
    }


    public boolean isWindowFull() {
        if ( tail != 0 && (tail) % windowSize == 0 ) {
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

    public T getTailData(){
        if(tail == 0){
            return null;
        }
        return buffer.get(tail - 1);
    }

    public int getTailIndex(){
        return tail;
    }


    public boolean isBufferFull() {
        return buffer.size() >= 5;
    }

    public boolean isExpected(int seqNum){
        for(int i = 0; i < expectedSeqNumArray.size(); i++){
            if(expectedSeqNumArray.get(i) == seqNum){
                return true;
            }
        }
        return false;
    }

    public int getExpectedSeqNum() {
        return expectedSeqNum;
    }

    public void setExpectedSeqNum(int expectedSeqNum) {
        this.expectedSeqNum = expectedSeqNum;
    }

    public void updateExpectedSeqArray() {
        expectedSeqNumArray.remove(0);
        int nextMaxSeqNum = expectedSeqNumArray.get(expectedSeqNumArray.size() - 1);
        nextMaxSeqNum = nextMaxSeqNum + 1 == windowSize * 2 ? 0 : nextMaxSeqNum + 1;
        expectedSeqNumArray.add(nextMaxSeqNum);
    }

    public List<Integer> getExpectedSeqNumArray() {
        return expectedSeqNumArray;
    }

    public void updateTail(){
        tail = Math.min(windowSize, buffer.size());
    }
}

