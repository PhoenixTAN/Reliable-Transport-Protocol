package simulator;

import java.util.zip.CRC32;
import java.util.zip.Checksum;

import basic.Message;
import basic.Packet;
import utils.SlidingWindowQueue;
import utils.StopAndWaitQueue;

/**
 * @author: Ziqi Tan
 * @description:
 * 		1. Your protocol should use only ACK (i.e. no NACK) packets. 
 * 		2. The receiver should be able to buffer out-of-order (OOO) packets, 
 * 		and send cumulative ACKs. 
 * 		3. The sender should retransmit only the next missing (unACK¡¯ed) packet either 
 * 		on a timeout or duplicate ACK. 
 */
public class SelectiveRepeatSimulator extends NetworkSimulator {
	
	
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


	public SelectiveRepeatSimulator(int numMessages, double loss, double corrupt, double avgDelay, int trace, int seed,
			int winsize, double timeout) {
		
		super(numMessages, loss, corrupt, avgDelay, trace, seed);
		
		windowSize = winsize;
		limitSeqNo = winsize * 2;	// set appropriately; assumes Stop and Wait here!
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
		
		
	}


	/**
	 * This routine will be called once,
	 * before any of your other B-side routines are called.
	 * It can be used to do any required initialization.
	 * */
	protected void bInit() {
		
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
