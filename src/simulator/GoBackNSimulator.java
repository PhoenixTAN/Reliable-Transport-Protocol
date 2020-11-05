package simulator;

import basic.Message;
import basic.Packet;
import utils.GoBackNReceiverQueue;
import utils.GoBackNSenderQueue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.CRC32;
import java.util.zip.Checksum;


/**
 * Author: Xueyan Xia
 * */
public class GoBackNSimulator extends NetworkSimulator {
    /**
     * Predefined Constants (static member variables):
     *
     *   int MAXDATASIZE : the maximum size of the basic.Message data and
     *                     basic.Packet payload
     *
     *   int A           : a predefined integer that represents entity A
     *   int B           : a predefined integer that represents entity B
     *
     * Predefined Member Methods:
     *
     *  void stopTimer(int entity):
     *       Stops the timer running at "entity" [A or B]
     *  void startTimer(int entity, double increment):
     *       Starts a timer running at "entity" [A or B], which will expire in
     *       "increment" time units, causing the interrupt handler to be
     *       called.  You should only call this with A.
     *  void toLayer3(int callingEntity, basic.Packet p)
     *       Puts the packet "p" into the network from "callingEntity" [A or B]
     *  void toLayer5(String dataSent)
     *       Passes "dataSent" up to layer 5
     *  double getTime()
     *       Returns the current time in the simulator.  Might be useful for
     *       debugging.
     *  int getTraceLevel()
     *       Returns TraceLevel
     *  void printEventList()
     *       Prints the current event list to stdout.  Might be useful for
     *       debugging, but probably not.
     *
     *
     *  Predefined Classes:
     *
     *  basic.Message: Used to encapsulate a message coming from layer 5
     *    Constructor:
     *      basic.Message(String inputData):
     *          creates a new basic.Message containing "inputData"
     *    Methods:
     *      boolean setData(String inputData):
     *          sets an existing basic.Message's data to "inputData"
     *          returns true on success, false otherwise
     *      String getData():
     *          returns the data contained in the message
     *  basic.Packet: Used to encapsulate a packet
     *    Constructors:
     *      basic.Packet (basic.Packet p):
     *          creates a new basic.Packet that is a copy of "p"
     *      basic.Packet (int seq, int ack, int check, String newPayload)
     *          creates a new basic.Packet with a sequence field of "seq", an
     *          ack field of "ack", a checksum field of "check", and a
     *          payload of "newPayload"
     *      basic.Packet (int seq, int ack, int check)
     *          chreate a new basic.Packet with a sequence field of "seq", an
     *          ack field of "ack", a checksum field of "check", and
     *          an empty payload
     *    Methods:
     *      boolean setSeqnum(int n)
     *          sets the basic.Packet's sequence field to "n"
     *          returns true on success, false otherwise
     *      boolean setAcknum(int n)
     *          sets the basic.Packet's ack field to "n"
     *          returns true on success, false otherwise
     *      boolean setChecksum(int n)
     *          sets the basic.Packet's checksum to "n"
     *          returns true on success, false otherwise
     *      boolean setPayload(String newPayload)
     *          sets the basic.Packet's payload to "newPayload"
     *          returns true on success, false otherwise
     *      int getSeqnum()
     *          returns the contents of the basic.Packet's sequence field
     *      int getAcknum()
     *          returns the contents of the basic.Packet's ack field
     *      int getChecksum()
     *          returns the checksum of the basic.Packet
     *      int getPayload()
     *          returns the basic.Packet's payload
     *
     */

    /**   Please use the following variables in your routines.
     *   int WindowSize  : the window size
     *   double RxmtInterval   : the retransmission timeout
     *   int LimitSeqNo  : when sequence number reaches this value, it wraps around
     */

    public static final int FirstSeqNo = 0;
    private int windowSize;
    private double retransmitInterval;
    private int limitSeqNo;

    /**
     * Add any necessary class variables here.  Remember, you cannot use
     *      these variables to send messages error free!  They can only hold
     *      state information for A or B.
     */
    public static final int ReceiverBufferSize = 5;
    private GoBackNSenderQueue<Packet> senderQueue;
    private GoBackNReceiverQueue<Packet> receiverQueue;
    /** custom statistics */
    private int retransmissionsByA;
    private double RTTSumTime;
    private double accumulativeCommunicationStartTime;
    private double accumulativeCommunicationEndTime;
    private int RTTTotalPacketNum = 0;

    /**
     * Also add any necessary methods (e.g. checksum of a String)
     */

    /**
     * get checksum of a packet by java.util.zip Checksum and CRC32
     * */
    private long getChecksumOfPacket(Packet packet) {

        String text = null;
        if(packet.isFlag()){
            text = packet.getSeqnum() + packet.getAcknum() + Arrays.toString(packet.getsACK().toArray()) + packet.getPayload();
        }else{
            text = packet.getSeqnum() + packet.getAcknum() + packet.getPayload();
        }

        byte[] bytes = text.getBytes();

        Checksum crc32 = new CRC32();
        crc32.update(bytes, 0, bytes.length);

        return crc32.getValue();
    }

    // This is the constructor.  Don't touch!
    public GoBackNSimulator(int numMessages,
                                   double loss,
                                   double corrupt,
                                   double avgDelay,
                                   int trace,
                                   int seed,
                                   int winsize,
                                   double delay) {
        super(numMessages, loss, corrupt, avgDelay, trace, seed);
        windowSize = winsize;
        limitSeqNo = winsize * 2; // < this value
        retransmitInterval = delay;

        /**
         *  custom statistics
         */
        retransmissionsByA = 0;
        RTTSumTime = 0;
        RTTTotalPacketNum = 0;
        accumulativeCommunicationStartTime = 0;
        accumulativeCommunicationEndTime = 0;
    }

    /** This routine will be called whenever the upper layer at the sender [A]
     has a message to send.  It is the job of your protocol to insure that
     the data in such a message is delivered in-order, and correctly, to
     the receiving upper layer.
     */
    protected void aOutput(Message message) {
        if (traceLevel > 0) {
            System.out.println("Calling aOutput()...");
        }

        int senderTailSeqNum = 0;
        int packetSeqNum = 0;

        Packet tailPacket = senderQueue.getTailData();
        if(tailPacket != null){
            senderTailSeqNum = senderQueue.getTailData().getSeqnum();
            packetSeqNum = (senderTailSeqNum + 1) < limitSeqNo ? (senderTailSeqNum + 1) : 0;
        }else{
            packetSeqNum = senderQueue.getCurSeqNum();
        }
        // handle new message from layer 5
        Packet newPacket = new Packet(packetSeqNum, 0, 0, new String(message.getData()));
        newPacket.setChecksum(getChecksumOfPacket(newPacket));
        senderQueue.add(newPacket);
        if (traceLevel > 2) {
            System.out.println("[A] Make a new packet, add to queue, seq = " + packetSeqNum);
        }

        // send packet
        int senderCurSeqNum = senderQueue.getCurSeqNum();
        int baseSeqNum = senderQueue.getFirst().getSeqnum();
        if (senderCurSeqNum < baseSeqNum + windowSize) {

            Packet packet = getPacketBySeqNum(senderQueue, senderCurSeqNum);
            if (packet == null){
                System.out.println("aOutput Exception: want to send a packet but there are no packets");
            }
            packet.setSendTime(getTime());
            accumulativeCommunicationStartTime += getTime();
            toLayer3(0, new Packet(packet));
            if (senderCurSeqNum == baseSeqNum){
                startTimer(0, retransmitInterval);
            }
            senderCurSeqNum = (senderCurSeqNum + 1) < limitSeqNo ? (senderCurSeqNum + 1) : 0;
            senderQueue.setCurSeqNum(senderCurSeqNum);
        }
        if (traceLevel > 2) {
            System.out.println("[A] Next packet seq = " + senderCurSeqNum);
        }
    }

    private Packet getPacketBySeqNum(GoBackNSenderQueue<Packet> senderQueue, int senderCurSeqNum) {
        for(int i = 0; i < senderQueue.getTailIndex(); i++){
            Packet packet = senderQueue.getDatabyIndex(i);
            if(packet.getSeqnum() == senderCurSeqNum){
                return packet;
            }
        }
        return null;
    }

    /**
     *   This routine will be called whenever a packet sent from the B-side
     *      (i.e. as a result of a toLayer3() being done by a B-side procedure)
     *      arrives at the A-side.  "packet" is the (possibly corrupted) packet
     *      sent from the B-side.
     */

    protected void aInput(Packet packet) {
        if (traceLevel > 0) {
            System.out.println("Calling aInput()...");
        }

        int ackSeqNum = packet.getAcknum();
        long checksum = packet.getChecksum();

        // the packet is not corrupted
        if (checksum == getChecksumOfPacket(packet)) {


            // handle SACK
            if(packet.isFlag()){
                if (traceLevel > 2) {
                    System.out.println("[A] SACK received ");
                }
                List<Integer> sACK = packet.getsACK();
                // retransmission
                int baseIndex = 0;
                int tailIndex = senderQueue.getTailIndex();

                for(int i = baseIndex; i < tailIndex; i++){
                    Packet reTransPacket = senderQueue.getDatabyIndex(i);
                    int seqNum = reTransPacket.getSeqnum();
                    if(!findPacketInSACK(seqNum, sACK)){ // baseSeqNum < SACKSeqNum
                        // resend i
                        reTransPacket.setRetransmitted(true);
                        toLayer3(0, new Packet(reTransPacket));
                        retransmissionsByA++;
                    }
                }
            }else{
                if (traceLevel > 2) {
                    System.out.println("[A] Cumulative ACK received, ackSeqNum = " + ackSeqNum);
                }
                // ackSeqNum is in the range of [base, cur)
                if(senderQueue.isWindowEmpty()){
                    return;
                }

                int baseSeqNum = senderQueue.getFirst().getSeqnum();
                int curSeqNum = senderQueue.getCurSeqNum();

                if (traceLevel > 2) {
                    System.out.println("[A] baseSeqNum = " + baseSeqNum + ", ackSeqNum = " + ackSeqNum + ", curSeqNum = " + curSeqNum);
                }

                if(ackSeqNum >= baseSeqNum){
                    senderQueue.slide(ackSeqNum, baseSeqNum);
                    curSeqNum = senderQueue.getCurSeqNum();
                    if (traceLevel > 2) {
                        System.out.println("[A] Sliding Window");
                    }
                    // statistic
                    if(!packet.isRetransmitted()){
                        RTTTotalPacketNum++;
                        RTTSumTime += (getTime() - packet.getSendTime());
                    }
                    accumulativeCommunicationEndTime += getTime() * (ackSeqNum - baseSeqNum + 1);

                    // if base == nextSeqNum, stop the timer
//                    baseSeqNum = ackSeqNum + 1;
//                    if(baseSeqNum == curSeqNum || baseSeqNum == windowSize){
//
//                    }
                    stopTimer(0);
                }
                // else, it may be a duplicate or a out of date ACK, do nothing


                // if there are still additional transmitted but not yet acknowledged packets, the timer is restarted
                if(!senderQueue.isWindowEmpty()){
                    startTimer(0, retransmitInterval);
                }
            }
        }else{
            if (traceLevel > 2) {
                System.out.println("[A] Corrupted ACK received ");
            }
        }
    }

    private boolean findPacketInSACK(int seqNum, List<Integer> sACK) {
        for(int i = 0; i < sACK.size(); i++){
            int deliveredSeqNum = sACK.get(i);
            if(seqNum == deliveredSeqNum){
                return true;
            }
        }
        return false;
    }

    /**
     *  This routine will be called when A's timer expires (thus generating a
     *      timer interrupt). You'll probably want to use this routine to control
     *      the retransmission of packets. See startTimer() and stopTimer(), above,
     *      for how the timer is started and stopped.
     */
    protected void aTimerInterrupt() throws InterruptedException {
        // Thread.sleep(1000);
        if ( traceLevel > 0 ) {
            System.out.println("Calling aTimerInterrupt()...");
        }

        startTimer(0, retransmitInterval);

        int senderTailSeqNum = 0;
        if(!senderQueue.isWindowEmpty()){
            senderTailSeqNum = senderQueue.getTailData().getSeqnum();
        }
        int senderBaseSeqNum = senderQueue.getFirst().getSeqnum();
        int i = 0;
        if ( traceLevel > 2 ) {
            System.out.println("senderBaseSeqNum = " + senderBaseSeqNum + ", senderTailSeqNum = " + senderTailSeqNum);
        }
        while(i < senderQueue.getTailIndex()){
            Packet packet = senderQueue.getDatabyIndex(i);  // a packet may be lost
            packet.setRetransmitted(true);
            retransmissionsByA++;
            toLayer3(0, new Packet(packet));
            if ( traceLevel > 2 ) {
                System.out.println("[A] Resend packet: " + packet);
            }
            i++;
        }
    }


    /**
     * This routine will be called once, before any of your other A-side
     *      routines are called. It can be used to do any required
     *      initialization (e.g. of member variables you add to control the state
     *      of entity A).
     */
    protected void aInit() {
        senderQueue = new GoBackNSenderQueue<Packet>(windowSize);
    }




    /**
     *    This routine will be called whenever a packet sent from the B-side
     *      (i.e. as a result of a toLayer3() being done by an A-side procedure)
     *      arrives at the B-side.  "packet" is the (possibly corrupted) packet
     *      sent from the A-side.
     */
    protected void bInput(Packet packet) {
        if ( traceLevel > 0 ) {
            System.out.println("Calling bInput()...");
        }
        // if not corrupted and in order
        long checkSum = packet.getChecksum();
        int expectedSeqNum = receiverQueue.getExpectedSeqNum();
        int pktSeqNum = packet.getSeqnum();


        if(pktSeqNum == expectedSeqNum && checkSum == getChecksumOfPacket(packet)){
            if ( traceLevel > 2 ) {
                System.out.println("Packet received successfully, send ACK, Seq = " + expectedSeqNum);
            }
            toLayer5(packet.getPayload());
            receiverQueue.updateExpectedSeqArray();
            int i = 0; pktSeqNum++;

            if(!receiverQueue.isWindowEmpty()){
                Packet pkt = receiverQueue.getDatabyIndex(i);
                System.out.println("Find buffered packet: " + pkt);
                while(pkt != null && pkt.getSeqnum() == pktSeqNum){
                    toLayer5(pkt.getPayload());
                    receiverQueue.removeFirst();
                    i++;
                    pktSeqNum++;expectedSeqNum++;
                    pkt = receiverQueue.getDatabyIndex(i);
                    receiverQueue.updateExpectedSeqArray();
                    receiverQueue.updateTail();
                }
            }
            // send ACK
            Packet newPacket = new Packet(0, expectedSeqNum, 0);
            newPacket.setChecksum(getChecksumOfPacket(newPacket));
            newPacket.setSendTime(packet.getSendTime());
            newPacket.setRetransmitted(packet.isRetransmitted());
            toLayer3(1, new Packet(newPacket));
            expectedSeqNum = pktSeqNum == limitSeqNo ? 0 : pktSeqNum;
            receiverQueue.setExpectedSeqNum(expectedSeqNum);
           // receiverQueue.updateExpectedSeqArray();
            if ( traceLevel > 2 ) {
                System.out.println("Update expected array, " + receiverQueue.getExpectedSeqNumArray().toString());
            }
        }
        else if(!receiverQueue.isExpected(pktSeqNum) && checkSum == getChecksumOfPacket(packet)){
            if ( traceLevel > 2 ) {
                System.out.println("Packet received, but duplicated, send ACK, Seq = " + pktSeqNum);
            }
            // send ACK
            Packet newPacket = new Packet(0, pktSeqNum, 0);
            newPacket.setChecksum(getChecksumOfPacket(newPacket));
            newPacket.setSendTime(packet.getSendTime());
            newPacket.setRetransmitted(packet.isRetransmitted());
            toLayer3(1, newPacket);
        }
        else if(checkSum == getChecksumOfPacket(packet)){
            if ( traceLevel > 2 ) {
                System.out.println("Packet received, but out of order, buffed packet, expectedSeq = " + expectedSeqNum + ", but packetSeq = " + pktSeqNum );
            }
            // out of order, buffered
            receiverQueue.add(packet);
            // if SACK array is FULL
            if(receiverQueue.isBufferFull()){
                // send a SACK packet
                Packet newPacket = new Packet(0, 0, 0);
                int newSeqNum = (expectedSeqNum == 0) ? (limitSeqNo - 1): (expectedSeqNum - 1);
                newPacket.setSeqnum(newSeqNum);
                newPacket.setFlag(true);
                List<Integer> SACK = new ArrayList<>();
                for(int i = 0; i < ReceiverBufferSize; i++){
                    int seqNum = receiverQueue.getDatabyIndex(i).getSeqnum();
                    SACK.add(seqNum);
                }
                newPacket.setsACK(SACK);
                newPacket.setChecksum(getChecksumOfPacket(newPacket));
                newPacket.setSendTime(packet.getSendTime());
                newPacket.setRetransmitted(true);
                toLayer3(1, newPacket);
            }
        }else{
            // corrupted packet, do nothing
            if ( traceLevel > 2 ) {
                System.out.println("Packet corrupted");
            }
        }
    }

    /**
     *   This routine will be called once, before any of your other B-side
     *      routines are called. It can be used to do any required
     *      initialization (e.g. of member variables you add to control the state
     *      of entity B).
     */
    protected void bInit() {
        receiverQueue = new GoBackNReceiverQueue<Packet>(windowSize);
    }

    // Use to print final statistics
    protected void Simulation_done() {
        String lineBreaker = System.lineSeparator();
        int originPacketsTransmittedByA = getMaxMessages();
        int totalPacketsTransmittedByA = getPacketsTransmittedByA();
        int nToLayer5 = getNtoLayer5();
        int ACKSentByB = getACKSentByB();

        /** total packet loss including ACK, retransmission packet and original packets */
        int nLost = getNLost();
        int nCorrupt = getNCorrupt();

        /** ratio of lost packets */
        double lostRatio = (retransmissionsByA - getACorrupt()) /
                (double)(originPacketsTransmittedByA + retransmissionsByA + ACKSentByB);
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
        double averageRTT = RTTSumTime / RTTTotalPacketNum;

        /**
         * Average communication time:
         * Average time between sending an original data packet
         * and receiving its ACK, even if the data packet is retransmitted.
         * */
        double averageCommunicationTime = (accumulativeCommunicationEndTime - accumulativeCommunicationStartTime) / originPacketsTransmittedByA;


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
        System.out.println("Number of A corrupted packets: " + getACorrupt());
        System.out.println("Ratio of lost packets: " + String.format("%.2f", lostRatio) + "%");
        System.out.println("Ratio of corrupted packets: " + String.format("%.2f", corruptedRatio) + "%");
        System.out.println("Average RTT: " + String.format("%.3f", averageRTT));
        System.out.println("Average communication time: " + String.format("%.3f", averageCommunicationTime));
        System.out.println("==================================================");

        // PRINT YOUR OWN STATISTIC HERE TO CHECK THE CORRECTNESS OF YOUR PROGRAM
        System.out.println(lineBreaker + "EXTRA:");
        System.out.println("===============CUSTOM STATISTICS==================");
        System.out.println("Total packets transmitted by A: " + totalPacketsTransmittedByA);
        System.out.println("Total number of packets accounts for RTT: " + RTTTotalPacketNum);
        System.out.println("Number of lost packets: " + nLost);
        System.out.println("A corrupt: " + getACorrupt());
        System.out.println("B corrupt: " + getBCorrupt());
        System.out.println("==================================================");


        //System.out.println("Example statistic you want to check e.g. number of ACK packets received by A :" + "<YourVariableHere>");
    }

}
