package simulator;

import basic.*;
import utils.OSIRandom;

import java.io.*;


public abstract class NetworkSimulator {
	/**
	 * This constant controls the maximum size of the buffer in a basic.Message
	 * and in a basic.Packet
	 */
	public static final int MAXDATASIZE = 20;

	/**
	 * These constants are possible events
	 */
	public static final int TIMERINTERRUPT = 0;
	public static final int FROMLAYER5 = 1;
	public static final int FROMLAYER3 = 2;

	/**
	 * These constants represent our sender and receiver
	 */
	public static final int A = 0;
	public static final int B = 1;
	
	/**
	 * parameters you will input manually
	 * */
	private int maxMessages;
	private double lossProb;
	private double corruptProb;
	private double avgMessageDelay;

	/**
	 *  0 will turn this of
	 *  Setting a tracing value of 1 or 2 will print out useful information about what is going on inside the emulation
	 *  A tracing value greater than 2 will display all sorts of odd messages that were used for emulator-debugging purposes
	 */
	protected int traceLevel;
	
	/** basic.Event list for all events */
	private EventList eventList;
	
	/** file to write the data deliver from A to the layer 5 of B */
	private FileWriter outFile;

	/** random number generator */
	private OSIRandom rand;
	
	private int numOfMessages;
	private double time;
	
	/** statistics */
	private int nToLayer3;
	private int nLost;
	private int nCorrupt;
	
	/** custom statistics */
	private int nToLayer5;
	private int packetsTransmittedByA;
	private int ACKSentByB;
	private int ACorrupt;
	private int BCorrupt;
	
	
	protected abstract void aOutput(Message message);

	protected abstract void aInput(Packet packet);

	protected abstract void aTimerInterrupt() throws InterruptedException;

	protected abstract void aInit();

	protected abstract void bInput(Packet packet);

	protected abstract void bInit();

	protected abstract void Simulation_done();

	
	public NetworkSimulator(int numMessages, double loss, double corrupt, double avgDelay, int trace, int seed) {
		
		maxMessages = numMessages;
		lossProb = loss;
		corruptProb = corrupt;
		avgMessageDelay = avgDelay;
		traceLevel = trace;
		
		eventList = new EventListImpl();
		rand = new OSIRandom(seed);
		
		time = 0;
		numOfMessages = 0;
		
		try {
			outFile = new FileWriter("OutputFile");
		} catch (Exception e) {
			e.printStackTrace();
		}

		nToLayer3 = 0;
		nLost = 0;
		nCorrupt = 0;
		
		nToLayer5 = 0;
		packetsTransmittedByA = 0;
		ACKSentByB = 0;
		ACorrupt = 0;
		BCorrupt = 0;
	}

	/**
	 * This method will be ran by the simulator
	 * */
	public void runNumOfMessageSimulator() throws InterruptedException {
		Event next;

		// Perform any student-required initialization
		aInit();
		bInit();

		// Start the whole thing off by scheduling some data arrival
		// from layer 5
		generateNextArrival();

		// Begin the main loop
		while (true) {
			// Get our next event
			next = eventList.removeNext();
			if (next == null) {
				break;
			}

			if (traceLevel >= 2) {
				System.out.println();
				System.out.print("EVENT time: " + next.getTime());
				System.out.print("  type: " + next.getType());
				System.out.println("  entity: " + next.getEntity());
			}

			// Advance the simulator's time
			time = next.getTime();

			// Perform the appropriate action based on the event
			switch (next.getType()) {
			
				case TIMERINTERRUPT:
					if (next.getEntity() == A) {
						aTimerInterrupt();
					} else {
						System.out.println("INTERNAL PANIC: Timeout for " + "invalid entity");
					}
					break;
	
				case FROMLAYER3:
					if (next.getEntity() == A) {
						aInput(next.getPacket());
					} else if (next.getEntity() == B) {
						bInput(next.getPacket());
					} else {
						System.out.println("INTERNAL PANIC: basic.Packet has " + "arrived for unknown entity");
					}
					break;
	
				case FROMLAYER5:

					// If we've reached the maximum message count, exit the main loop
					if (numOfMessages == maxMessages)
						break;

					// If a message has arrived from layer 5, we need to
					// schedule the arrival of the next message
					generateNextArrival();
	
					char[] nextMessage = new char[MAXDATASIZE];
	
					// Now, let's generate the contents of this message
					char j = (char) ((numOfMessages % 26) + 97);
					for (int i = 0; i < MAXDATASIZE; i++) {
						nextMessage[i] = j;
					}
	
					// Increment the message counter
					numOfMessages++;

					// Let the student handle the new message
					aOutput(new Message(new String(nextMessage)));
					break;
	
				default:
					System.out.println("INTERNAL PANIC: Unknown event type");
			}
			if (eventList.isEmpty())
				break;
		}
		System.out.println("Simulator terminated at time " + getTime());
		Simulation_done();
		try {
			outFile.flush();
			outFile.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/* Generate the next arrival and add it to the event list */
	private void generateNextArrival() {
		if (traceLevel > 2) {
			System.out.println("generateNextArrival(): called");
		}

		// arrival time 'x' is uniform on [0, 2*avgMessageDelay]
		// having mean of avgMessageDelay. 
		// Should this be made into a Gaussian distribution?
		double x = 2 * avgMessageDelay * rand.nextDouble(0);
		Event next = new Event(time + x, FROMLAYER5, A);

		eventList.add(next);
		if (traceLevel > 2) {
			System.out.println("generateNextArrival(): time is " + time);
			System.out.println("generateNextArrival(): future time for " + "event " + next.getType() + " at entity "
					+ next.getEntity() + " will be " + next.getTime());
		}

	}
	
	/*
	 * calling_entityis either 0 (for stopping the A-side timer) 
	 * or 1 (for stopping the B-side timer).
	 * */
	protected void stopTimer(int entity) {
		if (traceLevel > 2) {
			System.out.println("stopTimer: stopping timer at " + time);
		}

		Event timer = eventList.removeTimer(entity);

		// Let the student know they are attempting to cancel a non-existant
		// timer
		if (timer == null) {
			System.out.println("stopTimer: Warning: Unable to cancel your " + "timer");
		}
	}
	
	/*
	 * calling_entityis either 0 (for starting the A-side timer) 
	 * or 1 (for starting the B-side timer), 
	 * and increment is a double value indicating the amount of time 
	 * that will pass before the timer interrupts. 
	 * A's timer should only be started (or stopped) by A-side routines, 
	 * and similarly for the B-side timer. 
	 * Note that since we only care about unidirectional data transfer from A to B, 
	 * only the A-side should maintain such timer. 
	 * To give you an idea of the appropriate increment value to use: 
	 * a packet sent into the network takes an average of 5 time units 
	 * to arrive at the other side when there are no other messages in the medium. 
	 * */
	protected void startTimer(int entity, double increment) {
		if (traceLevel > 2) {
			System.out.println("startTimer: starting timer at " + time);
		}

		Event t = eventList.removeTimer(entity);

		if (t != null) {
			System.out.println("startTimer: Warning: Attempting to start a " + "timer that is already running");
			eventList.add(t);
			return;
		} else {
			Event timer = new Event(time + increment, TIMERINTERRUPT, entity);
			eventList.add(timer);
		}
	}
	
	/*
	 * calling_entityis either 0 (for the A-side send) or 1 (for the B-side send), 
	 * and packet is a structure of type pkt. 
	 * Calling this routine will cause the packet to be sent into the network, 
	 * destined for the other entity.
	 * */
	protected void toLayer3(int callingEntity, Packet p) {
		nToLayer3++;

		int destination;
		double arrivalTime;
		Packet packet = new Packet(p);

		if (traceLevel > 2) {
			System.out.println("toLayer3: " + packet);
		}

		// Set our destination
		if (callingEntity == A) {
			packetsTransmittedByA++;
			destination = B;
		} else if (callingEntity == B) {
			ACKSentByB++;
			destination = A;
		} else {
			System.out.println("toLayer3: Warning: invalid packet sender");
			return;
		}

		// Simulate losses
		if (rand.nextDouble(1) < lossProb) {
			nLost++;

			if (traceLevel > 0) {
				System.out.println("toLayer3: packet being lost");
			}

			return;
		}

		// Decide when the packet will arrive. Since the medium cannot
		// reorder, the packet will arrive 1 to 10 time units after the
		// last packet sent by this sender
		arrivalTime = eventList.getLastPacketTime(destination);

		if (arrivalTime <= 0.0) {
			arrivalTime = time;
		}

		arrivalTime = arrivalTime + 1 + (rand.nextDouble(2) * 9);

		// Simulate corruption
		if (rand.nextDouble(3) < corruptProb) {
			nCorrupt++;
			
			if (callingEntity == A) {
				ACorrupt++;
			} else if (callingEntity == B) {
				BCorrupt++;
			}

			if (traceLevel > 0) {
				System.out.println("toLayer3: packet being corrupted");
			}

			double x = rand.nextDouble(4);
			if (x < 0.75) {
				String payload = packet.getPayload();

				if (payload.length() > 0)
					payload = "?" + payload.substring(1);

				else
					payload = "?";

				packet.setPayload(payload);
			} else if (x < 0.875) {
				packet.setSeqnum(999999);
			} else {
				packet.setAcknum(999999);
			}
		}

		// Finally, create and schedule this event
		if (traceLevel > 2) {
			System.out.println("toLayer3: scheduling arrival on other side");
		}
		Event arrival = new Event(arrivalTime, FROMLAYER3, destination, packet);
		eventList.add(arrival);
	}
	
	/**
	 * message is a structure of type msg to be passed up to layer 5 of the B-side. 
	 * Note that we simplified the interface here 
	 * so you don't need to explicitly specify calling_entity (A-side or B-side) 
	 * since we are only dealing with unidirectional data delivery to the B-side.
	 * */
	protected void toLayer5(String dataSent) {
		nToLayer5++;
		try {
			outFile.write(dataSent, 0, MAXDATASIZE);
			outFile.write('\n');
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected double getTime() {
		return time;
	}

	protected void printEventList() {
		System.out.println(eventList.toString());
	}
	
	/** getters for statistics */
	protected int getMaxMessages() {
		return maxMessages;
	}
	
	protected int getNtoLayer3() {
		return nToLayer3;
	}

	protected int getNLost() {
		return nLost;
	}
	
	protected int getNCorrupt() {
		return nCorrupt;
	}
	
	/** getters for custom statistics */
	protected int getNtoLayer5() {
		return nToLayer5;
	}
	
	protected int getPacketsTransmittedByA() {
		return packetsTransmittedByA;
	}
	
	protected int getACKSentByB() {
		return ACKSentByB;
	}

	public int getACorrupt() {
		return ACorrupt;
	}

	public void setACorrupt(int aCorrupt) {
		ACorrupt = aCorrupt;
	}

	public int getBCorrupt() {
		return BCorrupt;
	}

	public void setBCorrupt(int bCorrupt) {
		BCorrupt = bCorrupt;
	}

}
