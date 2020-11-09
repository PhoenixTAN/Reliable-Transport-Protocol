package utils;

import java.util.ArrayList;

/**
 * Author: Ziqi Tan
 */
public class SelectiveRepeatSenderQueue<T> implements SlidingWindowQueue<T> {
	
	private ArrayList<T> buffer;
	
	private int windowSize;
	
	private int nextSeqNumIndex;
	private int tail;
	
	public SelectiveRepeatSenderQueue(int _windowSize) {
		buffer = new ArrayList<T>();
		windowSize = _windowSize;
		nextSeqNumIndex = 0;
		tail = 0;
	}
	
	public boolean hasNextToSend() {
		if ( nextSeqNumIndex < tail ) {
			return true;
		}
		return false;
	}
	
	public T getNextToSend() {
		if ( nextSeqNumIndex >= tail ) {
			return null;
		}
		T nextToSend = buffer.get(nextSeqNumIndex);
		nextSeqNumIndex++;
		return nextToSend;
	}

	@Override
	public boolean isWindowFull() {
		// be careful the index tail
		if ( (tail + 1) % windowSize == 0 ) {
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
			// nextSeqNumIndex = Math.max(0, nextSeqNumIndex - 1);	// be careful 
			nextSeqNumIndex = tail;
		}	
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for ( int i = 0; i < buffer.size(); i++ ) {
			if ( i == tail ) {
				sb.append("================ tail: " + tail + "==============" + "\n");
			}
			sb.append(buffer.get(i) + "\n");
			
		}
		return sb.toString();
	}

}
