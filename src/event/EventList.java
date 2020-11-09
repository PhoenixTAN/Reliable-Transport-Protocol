package event;


/**
 * @author TA
 * Modified by Phoenix TAN and Lighthousexxx. We add isEmpty() method.
 * */
public interface EventList {
	public boolean add(Event e);

	public Event removeNext();

	public String toString();

	public Event removeTimer(int entity);

	public double getLastPacketTime(int entityTo);

	public boolean isEmpty();	
}
