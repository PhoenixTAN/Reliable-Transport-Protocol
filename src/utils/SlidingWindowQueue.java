package utils;

/**
 * Author: Ziqi Tan
 */

public interface SlidingWindowQueue<T> {
	
	public boolean isWindowFull();
	public boolean isWindowEmpty();
	public void add(T t);
	public T getFirst();
	public T removeFirst();
	public void slide(int ackNum, int baseNum);
	
}
