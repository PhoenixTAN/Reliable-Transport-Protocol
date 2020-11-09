package basic;

import basic.Event;

public interface EventList {
	public boolean add(Event e);

	public Event removeNext();

	public String toString();

	public Event removeTimer(int entity);

	public double getLastPacketTime(int entityTo);

	public boolean isEmpty();
}
