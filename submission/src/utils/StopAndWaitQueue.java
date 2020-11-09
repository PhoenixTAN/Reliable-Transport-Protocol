package utils;

import java.util.ArrayList;


/**
 * Author: Ziqi Tan, Xueyan Xia
 * Description:
 * 		The buffer can receive messages from layer 5. 
 * 		Even though the window size is full, 
 * 		we still buffer the messages from layer 5.
 * 
 * */
public class StopAndWaitQueue<T> implements SlidingWindowQueue<T> {
	
	private ArrayList<T> buffer;

	protected int tail;
	protected int windowSize;
	
	/**
	 * @param _bufferSize should be the maximum sequence number
	 * */
	public StopAndWaitQueue(int _windowSize) {
		windowSize = _windowSize;
		buffer = new ArrayList<T>(_windowSize);
		tail = 0;
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
		// TODO 
		// tail = Math.min(windowSize, buffer.size());
//		if ( !isWindowFull() ) {
//			tail++;
//		}
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
		}	
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for ( int i = 0; i < tail; i++ ) {
			sb.append(buffer.get(i));
		}
		return sb.toString() + "\ntail: " + tail;
	}

}
