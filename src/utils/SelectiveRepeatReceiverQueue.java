package utils;

import java.util.ArrayList;

import basic.Packet;


/**
 * Author: Ziqi Tan
 */
public class SelectiveRepeatReceiverQueue<T> {

	private T[] receiveWindow;
	private int windowSize;
	private int currentBaseSeqNum;
	
	public SelectiveRepeatReceiverQueue(int _windowSize) {
		receiveWindow = (T[]) new Object[_windowSize];
		windowSize = _windowSize;
		currentBaseSeqNum = 0;
	}
	
	public boolean isBufferEmpty() {
		
		// for test
		boolean hasSomething = false;
		for ( int i = 1; i < receiveWindow.length; i++ ) {
			if ( receiveWindow[i] != null ) {
				hasSomething = true;
				break;
			}
		}
		if ( hasSomething && !(receiveWindow[0] == null) ) {
			System.out.println("Something wrong in receiver buffer");
		}
		
		// for test
		
		return receiveWindow[0] == null;
	}
	
	public int getCurrentBaseSeqNum() {
		return currentBaseSeqNum;
	}
	
	public void setCurrentBaseSeqNum(int seqNum) {
		currentBaseSeqNum = seqNum;
	}

	public void insert(T t, int index) {
		receiveWindow[index] = t;
	}
	
	public T getFirst() {
		return receiveWindow[0];
	}

	public T getByIndex(int index) {
		return receiveWindow[index];
	}
	
	/**
	 * copy the objects from index ~ windowSize - 1 to the beginning of the receiveWindow
	 * */
	public void slide(int index) {
		int i = 0;
		for ( int j = index; j < receiveWindow.length; i++, j++ ) {
			if (receiveWindow[j] == null) {
				break;
			}
			receiveWindow[i] = receiveWindow[j];
		}
		while ( i < receiveWindow.length ) {
			receiveWindow[i] = null;
			i++;
		}

	}
	
	public String toString() {
		String text = "";
		for ( int i = 0; i < receiveWindow.length; i++ ) {
			text += receiveWindow[i] + "\n";
		}
		text += "currentBaseSeqNum: " + currentBaseSeqNum + "\n";
		return text;
	}
	
	/**
	 * Test for this queue
	 */
	/**
	public static void main(String[] args) {
		SelectiveRepeatReceiverQueue srq = new SelectiveRepeatReceiverQueue(5);
		srq.insert(new Packet(0, 0, 0), 0);
		srq.insert(new Packet(1, 0, 0), 1);
		srq.insert(new Packet(2, 0, 0), 2);
		srq.insert(new Packet(3, 0, 0), 3);
		srq.insert(new Packet(4, 0, 0), 4);
		System.out.println(srq);
		srq.slide(3);
		System.out.println(srq);
	}
	*/
}
