package simulator;

import java.util.zip.CRC32;
import java.util.zip.Checksum;

import basic.Message;
import basic.Packet;
import utils.SlidingWindowQueue;
import utils.StopAndWaitQueue;

/**
 * Author: Ziqi Tan, Xueyan Xia
 * */
public class StopAndWaitSimulator extends NetworkSimulator {
	/**
	 * Predefined Constants (static member variables):
	 *
	 * 		int MAXDATASIZE : the maximum size of the basic.Message data and basic.Packet payload
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
	 * void toLayer3(int callingEntity, basic.Packet p)
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
	 * basic.Message: Used to encapsulate a message coming from layer 5
	 * Constructor:
	 * 		basic.Message(String inputData): creates a new basic.Message containing "inputData"
	 * Methods:
	 * 		boolean setData(String inputData):
	 * 			sets an existing basic.Message's data to "inputData" returns true on success, false otherwise
	 *
	 * 		String getData():
	 * 			returns the data contained in the message
	 *
	 * basic.Packet: Used to encapsulate a packet
	 * Constructors:
	 * 		basic.Packet (basic.Packet p):
	 * 			creates a new basic.Packet that is a copy of "p"
	 * 		basic.Packet (int seq, int ack, int check, String newPayload)
	 * 			creates a new basic.Packet with a sequence field of "seq", an ack field of "ack", a checksum
	 * 			field of "check", and a payload of "newPayload"
	 * 		basic.Packet (int seq, int ack, int check)
	 * 			create a new basic.Packet with a sequence field of "seq", an ack field of
	 * 			"ack", a checksum field of "check", and an empty payload
	 * Methods:
	 * 		boolean setSeqnum(int n)
	 * 			sets the basic.Packet's sequence field to "n" returns true on
	 * 			success, false otherwise
	 * 		boolean setAcknum(int n)
	 * 			sets the basic.Packet's ack field to "n" returns true on success, false otherwise
	 * 		boolean setChecksum(int n)
	 * 			sets the basic.Packet's checksum to "n" returns true on success, false otherwise
	 * 		boolean setPayload(String newPayload)
	 * 			sets the basic.Packet's payload to "newPayload" returns true on success, false otherwise
	 * 		int getSeqnum()
	 * 			returns the contents of the basic.Packet's sequence field
	 * 		int getAcknum()
	 * 			returns the contents of the basic.Packet's ack field
	 * 		int getChecksum()
	 * 			returns the checksum of the basic.Packet
	 * 		int getPayload()
	 * 			returns the basic.Packet's payload
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

	
	/**
	 * Add any necessary class variables here. Remember, you cannot use
	 * these variables to send messages error free!
	 * They can only hold state information for A or B.
	 * */
	private int senderSequenceNumber;
	
	/**
	 * 0: wait for call 0 from above, 1: wait for ACK 0
	 * 2: wait for call 1 from above, 3: wait for ACK 1
	 * */
	private int senderState;

	/** 0: wait for 0 from below, 1: wait for 1 from below */
	private int receiverState;
	
	private SlidingWindowQueue<Packet> senderBuffer;
	
	/** custom statistics */
	private int retransmissionsByA;

	/** Also add any necessary methods (e.g. checksum of a String) */ 
	
	/**
	 * get checksum of a packet by java.util.zip Checksum and CRC32
	 * */
	private long getChecksumOfPacket(Packet packet) {
		
		String text = packet.getSeqnum() + packet.getAcknum() + packet.getPayload();
		byte[] bytes = text.getBytes();
		
		Checksum crc32 = new CRC32();
		crc32.update(bytes, 0, bytes.length);
		
		return crc32.getValue();
	}

	// This is the constructor. Don't touch!
	public StopAndWaitSimulator(int numMessages, double loss, double corrupt, double avgDelay, int trace, int seed,
			int winsize, double timeout) {
		
		super(numMessages, loss, corrupt, avgDelay, trace, seed);
		
		windowSize = winsize;
		limitSeqNo = winsize + 1;	// set appropriately; assumes Stop and Wait here!
		retransmitInterval = timeout;
		
		/** initialize custom statistics */
		retransmissionsByA = 0;
	}


	/**
	 * This routine will be called once, before any of your other A-side
	 * routines are called. It can be used to do any required initialization
	 * (e.g. of member variables you add to control the state of entity A).
	 * */
	protected void aInit() {
		senderSequenceNumber = FirstSeqNo;
		senderState = 0;
		senderBuffer = new StopAndWaitQueue<Packet>(windowSize);
	}

	/**
	 * This routine will be called
	 * whenever the upper layer at the sending side (A) has a message to send.
	 * It is the job of your protocol to insure that the data in such a message
	 * is delivered in-order, and correctly, to the receiving side upper layer.
	 * */
	protected void aOutput(Message message) {
		if ( traceLevel > 0 ) {
			System.out.println("Calling aOutput()...");
		}
		
		switch(senderState) {
			case 0:		// wait for call 0 from above
			case 2:		// wait for call 1 from above
				Packet packet = null;
				// check the sender buffer first
				if ( !senderBuffer.isWindowEmpty() ) {
					
					// get the message but not delete it
					Packet pkt = senderBuffer.getFirst();	
					
					// System.out.println("Current sender buffer: " + senderBuffer);
					packet = new Packet(pkt);
				}
				else {
					packet = new Packet(senderSequenceNumber, 0, 0, new String(message.getData()));
					long checksum = getChecksumOfPacket(packet);
					packet.setChecksum(checksum);
					// then buffer the new message
					senderBuffer.add(new Packet(packet));
				}

				toLayer3(0, packet);	// udt_send(packet)
				startTimer(0, retransmitInterval);

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
		if ( traceLevel > 0 ) {
			System.out.println("Calling aInput()...");
		}
		
		int ackNum = packet.getAcknum();
		long checksum = packet.getChecksum();
		
		switch(senderState) {
			case 1:
				// sender is waiting for ACK 0
				// if the packet is corrupted or ack is 1
				// then do nothing
				
				// if not corrupted and ack is 0
				// then stop timer and wait for call 1 from above
				if ( ackNum == 0 && checksum == getChecksumOfPacket(packet) ) {
					stopTimer(0);
					senderState++;
					// System.out.println("sender state becomes: " + senderState);
					int baseNum = senderBuffer.getFirst().getSeqnum();
					senderBuffer.slide(ackNum, baseNum);
				}
				break;
			case 3:
				if ( ackNum == 1 && checksum == getChecksumOfPacket(packet) ) {
					stopTimer(0);
					senderState = 0;	// wait for call 0 from above
					// System.out.println("sender state becomes: " + senderState);
					int baseNum = senderBuffer.getFirst().getSeqnum();
					senderBuffer.slide(ackNum, baseNum);
				}
				break;
			case 0:
			case 2:
				if ( traceLevel > 0 ) {
					System.out.println("current sender state: " + senderState);
					System.out.println("When waiting for call from above, receive packet from layer 3. Do nothing.");
				}
				break;
			default:
				System.out.println("WARNING!! Unexpected sender state: " + senderState +  " in aInput().");
				
		}

	}

	/**
	 * This routine will be called when A's timer expires (thus generating a timer interrupt).
	 * You'll probably want to use this routine to control the retransmission of packets.
	 * See starttimer() and stoptimer() below for how the timer is started and stopped.
	 * */
	protected void aTimerInterrupt() {
		if ( traceLevel > 0 ) {
			System.out.println("Calling aTimerInterrupt()...");
		}
		
		switch(senderState) {
			case 1:
			case 3:
				Packet packet = senderBuffer.getFirst();
				toLayer3(0, new Packet(packet));	// udt_send()
				retransmissionsByA++;	// statistics
				startTimer(0, retransmitInterval);
				break;
			default:
				System.out.println("Unexpected sender state in aTimerInterrupt()");
		}
	}


	/**
	 * This routine will be called once,
	 * before any of your other B-side routines are called.
	 * It can be used to do any required initialization.
	 * */
	protected void bInit() {
		receiverState = 0;
	}

	/**
	 * Author: Xueyan Xia
	 * This routine will be called whenever a packet sent from the A-side
	 * (i.e., as a result of a tolayer3()being done by an A-side procedure)
	 * arrives at the B-side. packet is the (possibly corrupted) packet sent
	 * from the A-side.
	 * */
	protected void bInput(Packet packet) {
		
		System.out.println("Calling bInput()...");
		int seqNum = packet.getSeqnum();
		long checksum = packet.getChecksum();
		
		switch(receiverState) {
			case 0:
				// if corrupted or sequence number is 1
				if ( checksum != getChecksumOfPacket(packet) || seqNum == 1) {
					Packet pkt = new Packet(0, 1, 0);
					pkt.setChecksum(getChecksumOfPacket(pkt));
					toLayer3(1, pkt);
				}
				// if sequence number is 0
				else if( checksum == getChecksumOfPacket(packet) && seqNum == 0) {
					toLayer5(packet.getPayload());
					Packet pkt = new Packet(0, 0, 0);
					pkt.setChecksum(getChecksumOfPacket(pkt));
					toLayer3(1, pkt);
					receiverState++;
				}
				break;
			case 1:
				// if corrupted or sequence number is 0
				if ( checksum != getChecksumOfPacket(packet) || seqNum == 0) {
					Packet pkt = new Packet(1, 0, 0);
					pkt.setChecksum(getChecksumOfPacket(pkt));
					toLayer3(1, pkt);
				}
				// if sequence number is 1
				else if( checksum == getChecksumOfPacket(packet) && seqNum == 1) {
					toLayer5(packet.getPayload());
					Packet pkt = new Packet(1, 1, 0);
					pkt.setChecksum(getChecksumOfPacket(pkt));
					toLayer3(1, pkt);
					receiverState = 0;
				}
				break;
			default:
				System.out.println("Unexpected sender state in bInput()");
				
		}
	}


	/** Use to print final statistics */
	protected void Simulation_done() {
		
		String lineBreaker = System.lineSeparator();
		int originPacketsTransmittedByA = getMaxMessages();
		int totalPacketsTransmittedByA = getPacketsTransmittedByA();
		int nToLayer5 = getNtoLayer5();
		int ACKSentByB = getACKSentByB();
		
		/** total packet loss including ACK, retransmission packet and original packets */
		int nLost = getNLost();
		int nCorrupt = getNCorrupt();
		
		/** ratio of lost packets 
		 * */
		double lostRatio = (retransmissionsByA - nCorrupt) / 
				(double)(originPacketsTransmittedByA + retransmissionsByA + ACKSentByB);
		lostRatio = Math.round(lostRatio * 100 * 100) * 0.01;
		
		/**
		 * Ratio of corrupted packets:
		 * Corruption ratio = 
		 * 			(corrupted packets) / 
		 * 			( (original packets by A + retransmissions by A) + ACK packets by B - 
		 * 			(retransmissions by A ¨C corrupted packets) )
		 * */
		double corruptedRatio = nCorrupt / 
				(double)((totalPacketsTransmittedByA + retransmissionsByA) 
						+ ACKSentByB - (retransmissionsByA - nCorrupt));
		corruptedRatio = Math.round(corruptedRatio * 100 * 100) * 0.01;
		
		/**
		 * TO PRINT THE STATISTICS, FILL IN THE DETAILS BY PUTTING VARIBALE NAMES. 
		 * DO NOT CHANGE THE FORMAT OF PRINTED OUTPUT
		 * */
		System.out.println(lineBreaker);
		System.out.println("===============STATISTICS=======================");
		System.out.println("Number of original packets transmitted by A: " + originPacketsTransmittedByA);
		System.out.println("Number of retransmissions by A: " + retransmissionsByA);
		System.out.println("Number of data packets delivered to layer 5 at B: " + nToLayer5);
		System.out.println("Number of ACK packets sent by B: " + ACKSentByB);
		System.out.println("Number of corrupted packets: " + nCorrupt);
		System.out.println("Ratio of lost packets: " + String.format("%.2f", lostRatio) + "%");
		System.out.println("Ratio of corrupted packets: " + String.format("%.2f", corruptedRatio) + "%");
		System.out.println("Average RTT: " + "<YourVariableHere>");
		System.out.println("Average communication time: " + "<YourVariableHere>");
		System.out.println("==================================================");

		// PRINT YOUR OWN STATISTIC HERE TO CHECK THE CORRECTNESS OF YOUR PROGRAM
		System.out.println(lineBreaker + "EXTRA:");
		System.out.println("===============CUSTOM STATISTICS==================");
		System.out.println("Total packets transmitted by A: " + totalPacketsTransmittedByA);
		System.out.println("Number of lost packets: " + nLost);
		System.out.println("==================================================");
		// EXAMPLE GIVEN BELOW
		// System.out.println("Example statistic you want to check e.g. number of ACK
		// packets received by A :" + "<YourVariableHere>");
	}

}
