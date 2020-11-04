package utils;

import java.util.ArrayList;

/**
 * Author: Ziqi Tan
 */
public class SelectiveRepeatSenderQueue<T> implements SlidingWindowQueue<T> {
	
	private ArrayList<T> buffer;
	
	private int windowSize;
	
	private int nextSeqNum;
	private int tail;
	
	public SelectiveRepeatSenderQueue(int _windowSize) {
		buffer = new ArrayList<T>();
		windowSize = _windowSize;
		nextSeqNum = 0;
		tail = 0;
	}
	
	
	public boolean hasNextToSend() {
		if ( nextSeqNum < tail ) {
			return true;
		}
		return false;
	}
	
	public T getNextToSend() {
		if (nextSeqNum >= tail) {
			return null;
		}
		T nextToSend = buffer.get(nextSeqNum);
		nextSeqNum++;
		return nextToSend;
	}

	@Override
	public boolean isWindowFull() {
		if ( tail % windowSize == 0 ) {
			return true;
		}
		return false;
	}

	@Override
	public boolean isWindowEmpty() {
		if ( tail == 0 ) {
			return true;
		}
		return false;
	}

	@Override
	public void add(T t) {
		buffer.add(t);
		tail = Math.min(windowSize, buffer.size());
	}

	@Override
	public T getFirst() {
		if ( buffer.isEmpty() ) {
			return null;
		}
		return buffer.get(0);
	}

	@Override
	public T removeFirst() {
		if(buffer.isEmpty() ){
			return null;
		}
		return buffer.remove(0);
	}

	@Override
	public void slide(int ackNum, int baseNum) {
		while ( baseNum <= ackNum ) {
			removeFirst();
			baseNum++;
			// update tail
			tail = Math.min(windowSize, buffer.size());
			nextSeqNum = tail;	// be careful 
		}	
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for ( int i = 0; i < tail; i++ ) {
			sb.append(buffer.get(i) + "\n");
		}
		return sb.toString() + "tail: " + tail;
	}

}
