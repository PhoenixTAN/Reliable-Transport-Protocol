import java.util.*;
import java.io.*;
import utils.LoopQueue;


public class StopAndWaitSimulator extends NetworkSimulator {
	/**
	 * Predefined Constants (static member variables):
	 *
	 * 		int MAXDATASIZE : the maximum size of the Message data and Packet payload
	 * 		int A : a predefined integer that represents entity A
	 * 		int B : a predefined integer that represents entity B
	 *
	 * Predefined Member Methods:
	 * void stopTimer(int entity):
	 * 		Stops the timer running at "entity" [A or B]
	 *
	 * void startTimer(int entity, double increment):
	 * 		Starts a timer running at "entity" [A or B],
	 * 		which will expire in "increment" time units, causing the interrupt
	 * 		handler to be called. You should only call this with A.
	 *
	 * void toLayer3(int callingEntity, Packet p)
	 * 		Puts the packet "p" into the network from "callingEntity" [A or B]
	 *
	 * void toLayer5(String dataSent)
	 * 		Passes "dataSent" up to layer 5
	 *
	 * double getTime()
	 * 		Returns the current time in the simulator. Might be useful for debugging.
	 *
	 * int getTraceLevel()
	 * 		Returns TraceLevel
	 *
	 * void printEventList()
	 * 		Prints the current event list to stdout.
	 * 		Might be useful for debugging, but probably not.
	 *
	 *
	 * Predefined Classes:
	 *
	 * Message: Used to encapsulate a message coming from layer 5
	 * Constructor:
	 * 		Message(String inputData): creates a new Message containing "inputData"
	 * Methods:
	 * 		boolean setData(String inputData):
	 * 			sets an existing Message's data to "inputData" returns true on success, false otherwise
	 *
	 * 		String getData():
	 * 			returns the data contained in the message
	 *
	 * Packet: Used to encapsulate a packet
	 * Constructors:
	 * 		Packet (Packet p):
	 * 			creates a new Packet that is a copy of "p"
	 * 		Packet (int seq, int ack, int check, String newPayload)
	 * 			creates a new Packet with a sequence field of "seq", an ack field of "ack", a checksum
	 * 			field of "check", and a payload of "newPayload"
	 * 		Packet (int seq, int ack, int check)
	 * 			create a new Packet with a sequence field of "seq", an ack field of
	 * 			"ack", a checksum field of "check", and an empty payload
	 * Methods:
	 * 		boolean setSeqnum(int n)
	 * 			sets the Packet's sequence field to "n" returns true on
	 * 			success, false otherwise
	 * 		boolean setAcknum(int n)
	 * 			sets the Packet's ack field to "n" returns true on success, false otherwise
	 * 		boolean setChecksum(int n)
	 * 			sets the Packet's checksum to "n" returns true on success, false otherwise
	 * 		boolean setPayload(String newPayload)
	 * 			sets the Packet's payload to "newPayload" returns true on success, false otherwise
	 * 		int getSeqnum()
	 * 			returns the contents of the Packet's sequence field
	 * 		int getAcknum()
	 * 			returns the contents of the Packet's ack field
	 * 		int getChecksum()
	 * 			returns the checksum of the Packet
	 * 		int getPayload()
	 * 			returns the Packet's payload
	 */

	/**
	 * Please use the following variables in your routines. int WindowSize : the
	 * window size double RxmtInterval : the retransmission timeout int LimitSeqNo :
	 * when sequence number reaches this value, it wraps around
	 */

	public static final int FirstSeqNo = 0;
	private int windowSize;
	private double retransmitInterval;
	private int limitSeqNo;

	// Add any necessary class variables here. Remember, you cannot use
	// these variables to send messages error free!
	// They can only hold state information for A or B.
	private int senderSequenceNumber;
	private int senderState;
	// 0: wait for call 0 from above, 1: wait for ACK 0
	// 2: wait for call 1 from above, 3: wait for ACK 1

	private int receiverState;
	// 0: wait for 0 from below, 1: wait for 1 from below

	private LoopQueue<Packet> senderBuffer;


	// Also add any necessary methods (e.g. checksum of a String)

	// This is the constructor. Don't touch!
	public StopAndWaitSimulator(int numMessages, double loss, double corrupt, double avgDelay, int trace, int seed,
			int winsize, double timeout) {
		super(numMessages, loss, corrupt, avgDelay, trace, seed);
		windowSize = winsize;
		limitSeqNo = winsize;	// set appropriately; assumes Stop and Wait here!
		retransmitInterval = timeout;
	}

	// TODO
	private int checksumOfString(String text) {
		
		
		return 0;
	}

	/**
	 * This routine will be called once, before any of your other A-side
	 * routines are called. It can be used to do any required initialization
	 * (e.g. of member variables you add to control the state of entity A).
	 * */
	protected void aInit() {
		senderSequenceNumber = FirstSeqNo;
		senderState = 0;
		senderBuffer = new LoopQueue<Packet>(windowSize);
	}

	/**
	 * This routine will be called
	 * whenever the upper layer at the sending side (A) has a message to send.
	 * It is the job of your protocol to insure that the data in such a message
	 * is delivered in-order, and correctly, to the receiving side upper layer.
	 * */
	protected void aOutput(Message message) {

		switch(senderState) {
			case 0:		// wait for call 0 from above
			case 2:		// wait for call 1 from above
				Packet packet = null;
				// check the sender buffer first
				if ( !senderBuffer.isEmpty() ) {
					Packet pkt = senderBuffer.peak();	// get the message but not delete it
					packet = new Packet(pkt);
				}
				else {
					packet = new Packet(senderSequenceNumber, 0, 0, new String(message.getData()));
				}

				// then buffer the new message if the buffer is not full
				senderBuffer.add(new Packet(packet));

				toLayer3(0, packet);	// udt_send(packet)
				this.startTimer(0, retransmitInterval);

				// update sequence number
				senderSequenceNumber = (senderSequenceNumber + 1) % limitSeqNo;

				// update sender state
				senderState++;	// state 0 -> state 1 and state 2 -> state 3
				break;

			case 1:		// wait for ACK 0
			case 3:		// wait for ACK 1
				// buffer this message from layer 5
				senderBuffer.add(new Packet(senderSequenceNumber, 0, 0, new String(message.getData())));
				// update sequence number
				senderSequenceNumber = (senderSequenceNumber + 1) % limitSeqNo;
				break;
			default:
				System.out.println("Unexpected sender state in aOutput().");
		}

	}

	/**
	 * This routine will be called whenever a packet sent from the B-side
	 * (i.e., as a result of a tolayer3()being done by a B-side procedure)
	 * arrives at the A-side. packet is the (possibly corrupted) packet sent
	 * from the B-side.
	 * */
	protected void aInput(Packet packet) {
		int ackNum = packet.getAcknum();

		int checksum = packet.getChecksum();
		

		switch(senderState) {
			case 1:
				// sender is waiting for ACK 0
				// if the packet is corrupted or ack is 1
				// then do nothing
				
				// if not corrupted and ack is 0
				// then stop timer and wait for call 1 from above
				if ( ackNum == 0 && checksum == checksumOfString(packet.getPayload()) ) {
					stopTimer(0);
					senderState++;
				}
				break;
			case 3:
				if ( ackNum == 1 && checksum == checksumOfString(packet.getPayload()) ) {
					stopTimer(0);
					senderState = 0;	// wait for call 0 from above
				}
				break;
			default:
				System.out.println("Unexpected sender state in aInput().");
				
		}

	}

	/*
	 * This routine will be called when A's timer expires (thus generating a timer interrupt).
	 * You'll probably want to use this routine to control the retransmission of packets.
	 * See starttimer() and stoptimer() below for how the timer is started and stopped.
	 * */
	protected void aTimerInterrupt() {
		switch(senderState) {
			case 1:
			case 3:
				Packet packet = senderBuffer.peak();
				toLayer3(0, new Packet(packet));	// udt_send()
				startTimer(0, retransmitInterval);
				break;
			default:
				System.out.println("Unexpected sender state in aTimerInterrupt()");
		}
	}


	/*
	 * This routine will be called once,
	 * before any of your other B-side routines are called.
	 * It can be used to do any required initialization.
	 * */
	protected void bInit() {
		receiverState = 0;
	}

	/*
	 * This routine will be called whenever a packet sent from the A-side
	 * (i.e., as a result of a tolayer3()being done by an A-side procedure)
	 * arrives at the B-side. packet is the (possibly corrupted) packet sent
	 * from the A-side.
	 * */
	protected void bInput(Packet packet) {
		int seqNum = packet.getSeqnum();
		int checksum = packet.getChecksum();
		
		switch(receiverState) {
			case 0:
				// if corrupted or sequence number is 1
				if ( checksum != checksumOfString(packet.getPayload()) || seqNum == 1) {
					Packet pkt = new Packet(0, 1, 0);
					
				}
			case 1:
				
			default:
				System.out.println("Unexpected sender state in bInput()");
				
		}
		
	}



	// Use to print final statistics
	protected void Simulation_done() {
		// TO PRINT THE STATISTICS, FILL IN THE DETAILS BY PUTTING VARIBALE NAMES. DO
		// NOT CHANGE THE FORMAT OF PRINTED OUTPUT
		System.out.println("\n\n===============STATISTICS=======================");
		System.out.println("Number of original packets transmitted by A:" + "<YourVariableHere>");
		System.out.println("Number of retransmissions by A:" + "<YourVariableHere>");
		System.out.println("Number of data packets delivered to layer 5 at B:" + "<YourVariableHere>");
		System.out.println("Number of ACK packets sent by B:" + "<YourVariableHere>");
		System.out.println("Number of corrupted packets:" + "<YourVariableHere>");
		System.out.println("Ratio of lost packets:" + "<YourVariableHere>");
		System.out.println("Ratio of corrupted packets:" + "<YourVariableHere>");
		System.out.println("Average RTT:" + "<YourVariableHere>");
		System.out.println("Average communication time:" + "<YourVariableHere>");
		System.out.println("==================================================");

		// PRINT YOUR OWN STATISTIC HERE TO CHECK THE CORRECTNESS OF YOUR PROGRAM
		System.out.println("\nEXTRA:");
		// EXAMPLE GIVEN BELOW
		// System.out.println("Example statistic you want to check e.g. number of ACK
		// packets received by A :" + "<YourVariableHere>");
	}

}
