package utils;

/**
 * Author: Ziqi Tan
 */

public interface SlidingWindowQueue<T> {
	
	public T getDatabyIndex(int seqNum);
	public boolean isWindowFull();
	public boolean isWindowEmpty();
	public void add(T t);
	public T getFirst();
	public boolean removeFirst();
	public void slide(int ackNum, int baseNum);
	
}
