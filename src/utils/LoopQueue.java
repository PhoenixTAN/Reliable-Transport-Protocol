package utils;
/*
Author: Ziqi Tan
*/
public class LoopQueue<T> {
	
	protected T[] buffer;

	protected int head;
	protected int tail;
	
	/**
	 * @param _bufferSize should be the maximum sequence number
	 * */
	public LoopQueue(int _bufferSize) {
		buffer = (T[])new Object[_bufferSize];
		head = 0;
		tail = 0;
	}
	
	public T getDatabyIndex(int seqNum) {
		return buffer[seqNum];
	}
	
	public boolean add(T t) {
		if ( isFull() ) {
			return false;	
		}
		
		buffer[tail] = t;
		tail = (tail + 1) % buffer.length;
		
		return true;
	}
	
	public T poll() {
		if ( isEmpty() ) {
			return null;
		}
		T t = buffer[head];
		head = (head + 1) % buffer.length;
		return t;
	}
	
	public T peak() {
		return buffer[head];
	}
	
	public boolean isFull() {
		if ( (tail + 1) % buffer.length == head ) {
			return true;
		}
		return false;
	}
	
	public boolean isEmpty() {
		if ( head == tail ) {
			return true;
		}
		return false;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for ( int i = head; i < tail; i++ ) {
			sb.append(buffer[i]);
		}
		return sb.toString();
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
