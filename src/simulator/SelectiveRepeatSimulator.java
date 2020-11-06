package simulator;

import java.util.zip.CRC32;
import java.util.zip.Checksum;

import basic.Message;
import basic.Packet;
import utils.SelectiveRepeatReceiverQueue;
import utils.SelectiveRepeatSenderQueue;


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
	private int lastACKNum;
	private boolean isTimerStarted;
	
	private SelectiveRepeatSenderQueue<Packet> senderBuffer;
	private SelectiveRepeatReceiverQueue<Packet> receiverBuffer;
	
	/** custom statistics */
	private int retransmissionsByA;
	private int originalPacketsTransmittedByA;
	
	private double accumulativeRTT;
	private int totalNumOfPacketsForRTT;
	
	private double accumulativeCommunicationStartTime;
	private double accumulativeCommunicationEndTime;
	
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
		limitSeqNo = winsize * 2;	// set appropriately; assumes Selective Repeat here!
		retransmitInterval = timeout;
	}

	/**
	 * This routine will be called once, before any of your other A-side
	 * routines are called. It can be used to do any required initialization
	 * (e.g. of member variables you add to control the state of entity A).
	 * */
	protected void aInit() {
		senderSequenceNumber = FirstSeqNo;
		lastACKNum = -1;
		senderBuffer = new SelectiveRepeatSenderQueue<Packet>(windowSize);
		isTimerStarted = false;
		
		/** initialize custom statistics */
		retransmissionsByA = 0;
		originalPacketsTransmittedByA = 0;
		accumulativeRTT = 0;
		totalNumOfPacketsForRTT = 0;
		accumulativeCommunicationStartTime = 0;
		accumulativeCommunicationEndTime = 0;

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
		
		Packet packet = new Packet(senderSequenceNumber, 0, 0, message.getData());
		packet.setChecksum(getChecksumOfPacket(packet));
		
		// buffer this packet
		senderBuffer.add(packet);
		if ( traceLevel > 3 ) {
			System.out.println(senderBuffer);
		}
		
		// update sequence number
		senderSequenceNumber = (senderSequenceNumber + 1) % limitSeqNo;
		
		// if the sender still has packet to send, start the timer
		boolean prepareTimer = false;
		if ( senderBuffer.hasNextToSend() ) {
			prepareTimer = true;
		}
		
		// send all available packets
		Packet nextPacket = null;
		while ( senderBuffer.hasNextToSend() ) {
			nextPacket = senderBuffer.getNextToSend();
			nextPacket.setSendTime(getTime());
			accumulativeCommunicationStartTime += getTime();
			toLayer3(0, new Packet(nextPacket));
			originalPacketsTransmittedByA++;	// statistics
			System.out.println("wwwwwwwwwwwwwwwwwwwwwwwwwwwww");
		}
		
		// be careful of the timer
		if ( prepareTimer && !isTimerStarted ) {
			startTimer(0, retransmitInterval);
			isTimerStarted = true;
		}
	}

	/**
	 * This routine will be called whenever a packet sent from the B-side
	 * (i.e., as a result of a tolayer3() being done by a B-side procedure)
	 * arrives at the A-side. packet is the (possibly corrupted) packet sent
	 * from the B-side.
	 * */
	protected void aInput(Packet packet) {
		if ( traceLevel > 0 ) {
			System.out.println("Calling aInput()...");
			// System.out.println(packet);
		}
		
		// if the packet corrupts, discard it
		if ( getChecksumOfPacket(packet) != packet.getChecksum() ) {
			if ( traceLevel > 3 ) {
				System.out.println("ACK from B is corrupted!");
			}
			return ;
		}
		
		if ( traceLevel > 3 ) {
			System.out.println("Sender buffer: ");
			System.out.println(senderBuffer);
		}
		
		// packet that A just received
		int cumulativeACK = packet.getAcknum();		
		// the next packet that A is waiting for ACK
		Packet nextPacket = senderBuffer.getFirst(); 
		
		// be careful if A's buffer is empty
		if ( nextPacket == null ) {
			return ;
		}
		
		int baseNum = nextPacket.getSeqnum();
		
		// duplicate ACK then retransmit 
		if ( cumulativeACK == lastACKNum ) {
			
			System.out.println("duplicate ACK: " + cumulativeACK);
			// retransmit only the next missing unACK'ed packet
			nextPacket.setRetransmitted(true);
			toLayer3(0, new Packet(nextPacket));
			
			retransmissionsByA++;	// statistics
			
			// restart timer
			startTimer(0, retransmitInterval);
			
			return ;
		}
		
		// cumulative ACK
		senderBuffer.slide(cumulativeACK, baseNum);
		if ( cumulativeACK - 1 > baseNum ) {
			System.out.println("CumulativeACK: " + cumulativeACK + " baseNum: " + baseNum);
		}
		
		// statistic for average RTT and communication time
		if ( !packet.isRetransmitted() ) {
			accumulativeRTT += (getTime() - packet.getSendTime());
			totalNumOfPacketsForRTT++;
		}
		
		if ( cumulativeACK >= baseNum ) {
			accumulativeCommunicationEndTime += (getTime() * (cumulativeACK - baseNum + 1));
		}

		// if no more packets to be ACKed
		// then stop the timer
		if ( senderBuffer.isWindowEmpty() ) {
			stopTimer(0);
			isTimerStarted = false;
		}
		
		// update lastACKNum
		lastACKNum = cumulativeACK;
		
		// we need the following code to send the available packets
		// if the sender still has packet to send, start the timer
		boolean prepareTimer = false;
		if ( senderBuffer.hasNextToSend() ) {
			prepareTimer = true;
		}
		System.out.println("Here " + senderBuffer.hasNextToSend());
		
		// send all available packets
		Packet nextAvailablePacket = null;
		while ( senderBuffer.hasNextToSend() ) {
			nextAvailablePacket = senderBuffer.getNextToSend();
			nextAvailablePacket.setSendTime(getTime());
			accumulativeCommunicationStartTime += getTime();
			toLayer3(0, new Packet(nextAvailablePacket));
			originalPacketsTransmittedByA++;	// statistics
		}
		
		// be careful of the timer
		if ( prepareTimer && !isTimerStarted ) {
			startTimer(0, retransmitInterval);
			isTimerStarted = true;
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
		
		Packet nextPacket = senderBuffer.getFirst();
		
		nextPacket.setRetransmitted(true);
		toLayer3(0, new Packet(nextPacket));
		retransmissionsByA++;	// statistics
		
		startTimer(0, retransmitInterval);
	}


	/**
	 * This routine will be called once,
	 * before any of your other B-side routines are called.
	 * It can be used to do any required initialization.
	 * */
	protected void bInit() {
		receiverBuffer = new SelectiveRepeatReceiverQueue<Packet>(windowSize);
	}

	/**
	 * This routine will be called whenever a packet sent from the A-side
	 * (i.e., as a result of a tolayer3()being done by an A-side procedure)
	 * arrives at the B-side. packet is the (possibly corrupted) packet sent
	 * from the A-side.
	 * */
	protected void bInput(Packet packet) {
		if ( traceLevel > 0 ) {
			System.out.println("Calling bInput()...");
		}
		
		// the packet that B just received
		int seqNum = packet.getSeqnum();
		// the base sequence number in the receiver window
		int baseSeqNum = receiverBuffer.getCurrentBaseSeqNum();
		
		if ( getChecksumOfPacket(packet) != packet.getChecksum() ) {
			if ( traceLevel > 3 ) {
				/*System.out.println("bInput sends duplicate ACK because of corruption.");
				Packet ackPacket = new Packet(0, seqNum - 1, 0);
				ackPacket.setChecksum(getChecksumOfPacket(ackPacket));
				ackPacket.setRetransmitted(true);	// statistics
				toLayer3(1, ackPacket);*/
			}
			return ;
		}
		
		// if the sequence number is in [rcv_base, rcv_base + N - 1]
		if ( (seqNum >= baseSeqNum && seqNum < baseSeqNum + windowSize) ||
			 (seqNum < baseSeqNum 
				&& baseSeqNum + windowSize > limitSeqNo 
				&& seqNum < (baseSeqNum + windowSize) % limitSeqNo ) 
		) {
			
			// buffer this packet
			if ( seqNum >= baseSeqNum ) {
				receiverBuffer.insert(new Packet(packet), seqNum - baseSeqNum);
			}
			else {
				receiverBuffer.insert(new Packet(packet), (seqNum + 1 + limitSeqNo - 1 - baseSeqNum));
			}
			
			if ( traceLevel > 3 ) {
				System.out.println("seqNum is in [rcv_base, rcv_base + N - 1]");
				System.out.println("SeqNum: " + seqNum + " " + "baseSeq: " + baseSeqNum);
				System.out.println("Reveive window: ");
				System.out.println(receiverBuffer);
			}
			
			// slide the window and deliver the in-order packets to layer 5
			if ( seqNum ==  baseSeqNum ) {
				int index = 0;
				Packet nextPacket = receiverBuffer.getByIndex(index);
				// statistic for average RTT
				double lastPacketSendTimeForCumulativeACK = 0;		
				
				while ( nextPacket != null ) {
					if (seqNum + index == limitSeqNo - 1) {
						// send cumulative ACK when we hit the limit sequence number
						Packet ackPacket = new Packet(0, seqNum + index, 0);
						ackPacket.setChecksum(getChecksumOfPacket(ackPacket));
						
						// statistic for average RTT
						if ( nextPacket.isRetransmitted() ) {
							ackPacket.setRetransmitted(true);
						}
						else {
							ackPacket.setSendTime(nextPacket.getSendTime());
							ackPacket.setRetransmitted(false);
							lastPacketSendTimeForCumulativeACK = nextPacket.getSendTime();
						}
						
						toLayer3(1, ackPacket);		// udt_send
						
					}
					
					// deliver in-order packets to layer 5
					toLayer5(nextPacket.getPayload());
					
					// statistic for average RTT
					lastPacketSendTimeForCumulativeACK = nextPacket.getSendTime();
					
					index++;
					nextPacket = receiverBuffer.getByIndex(index);
				}

				// delete the packets which have been delivered to layer 5
				receiverBuffer.slide(index);
				// update the receive window base sequence number
				receiverBuffer.setCurrentBaseSeqNum((baseSeqNum + index) % limitSeqNo);
				
				if ( traceLevel > 3 ) {
					System.out.println("Receive window after slide: ");
					System.out.println(receiverBuffer);
				}
				
				// send cumulative ACK
				if ( seqNum + index - 1 != limitSeqNo - 1 ) {
					Packet ackPacket = new Packet(0, (seqNum + index - 1) % limitSeqNo, 0);
					ackPacket.setChecksum(getChecksumOfPacket(ackPacket));
					
					// statistic for average RTT
					if ( lastPacketSendTimeForCumulativeACK == 0 ) {
						ackPacket.setRetransmitted(true);
					}
					else {
						ackPacket.setSendTime(lastPacketSendTimeForCumulativeACK);
						ackPacket.setRetransmitted(false);
					}
					
					toLayer3(1, new Packet(ackPacket));		// udt_send
				}
				
				return ;
			}
		}
		// if the sequence number in [rcv_base - N, rcv_base - 1]
		// then generate the ACK
		else {
			System.out.println("Generate the ACK again. SeqNum: " + seqNum);			
			Packet ackPacket = new Packet(0, seqNum, 0);
			ackPacket.setChecksum(getChecksumOfPacket(ackPacket));
			ackPacket.setRetransmitted(true);	// statistics
			toLayer3(1, ackPacket);
		}
		
		// ignore other packets
	}


	/** Use to print final statistics */
	protected void Simulation_done() {
		
		String lineBreaker = System.lineSeparator();
		int maxMessages = getMaxMessages();
		int totalPacketsTransmittedByA = getPacketsTransmittedByA();
		int nToLayer5 = getNtoLayer5();
		int ACKSentByB = getACKSentByB();
		
		/** total packet loss including ACK, retransmission packet and original packets */
		int nLost = getNLost();
		int nCorrupt = getNCorrupt();
		
		/** ratio of lost packets */
		double lostRatio = (retransmissionsByA - getACorrupt()) / 
				(double)(maxMessages + retransmissionsByA + ACKSentByB);
		lostRatio = Math.round(lostRatio * 100 * 100) * 0.01;
		
		/** Ratio of corrupted packets */
		double corruptedRatio = getACorrupt() / 
				(double)((totalPacketsTransmittedByA + retransmissionsByA) 
						+ ACKSentByB - (retransmissionsByA - getACorrupt()));
		corruptedRatio = Math.round(corruptedRatio * 100 * 100) * 0.01;
		
		/**
		 * Average RTT: 
		 * Average time to send a packet and receive its ACK for a packet 
		 * that has not been retransmitted. 
		 * Note that data packets that are ACKed by the 
		 * ACK of a subsequent packet are not part of this metric
		 * */
		double averageRTT = accumulativeRTT / totalNumOfPacketsForRTT;
		
		/**
		 * Average communication time: 
		 * Average time between sending an original data packet 
		 * and receiving its ACK, even if the data packet is retransmitted.
		 * */
		double averageCommunicationTime = (accumulativeCommunicationEndTime - accumulativeCommunicationStartTime) / maxMessages;
		
		/**
		 * TO PRINT THE STATISTICS, FILL IN THE DETAILS BY PUTTING VARIBALE NAMES. 
		 * DO NOT CHANGE THE FORMAT OF PRINTED OUTPUT
		 * */
		System.out.println(lineBreaker);
		System.out.println("===============STATISTICS=======================");
		System.out.println("Number of original packets transmitted by A: " + maxMessages);
		System.out.println("Number of retransmissions by A: " + retransmissionsByA);
		System.out.println("Number of data packets delivered to layer 5 at B: " + nToLayer5);
		System.out.println("Number of ACK packets sent by B: " + ACKSentByB);
		System.out.println("Number of corrupted packets: " + nCorrupt);
		System.out.println("Number of A corrupted packets: " + getACorrupt());
		System.out.println("Ratio of lost packets: " + String.format("%.2f", lostRatio) + "%");
		System.out.println("Ratio of corrupted packets: " + String.format("%.2f", corruptedRatio) + "%");
		System.out.println("Average RTT: " + String.format("%.3f", averageRTT));
		System.out.println("Average communication time: " + String.format("%.3f", averageCommunicationTime));
		System.out.println("==================================================");

		// PRINT YOUR OWN STATISTIC HERE TO CHECK THE CORRECTNESS OF YOUR PROGRAM
		// EXAMPLE GIVEN BELOW
		// System.out.println("Example statistic you want to check e.g. number of ACK
		// packets received by A :" + "<YourVariableHere>");
		System.out.println(lineBreaker + "EXTRA:");
		System.out.println("===============CUSTOM STATISTICS==================");
		System.out.println("Total packets transmitted by A: " + totalPacketsTransmittedByA);
		System.out.println("Original packets transmitted by A: " + originalPacketsTransmittedByA);
		System.out.println("Total number of packets accounts for RTT: " + totalNumOfPacketsForRTT);
		System.out.println("Number of lost packets: " + nLost);
		System.out.println("A corrupt: " + getACorrupt());
		System.out.println("B corrupt: " + getBCorrupt());
		System.out.println("==================================================");

	}
}
