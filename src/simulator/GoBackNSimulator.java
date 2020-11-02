package simulator;

import basic.Message;
import basic.Packet;
import utils.GoBackNQueue;


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
    private int WindowSize;
    private double RxmtInterval;
    private int LimitSeqNo;

    /**
     * Add any necessary class variables here.  Remember, you cannot use
     *      these variables to send messages error free!  They can only hold
     *      state information for A or B.
     */
    private int senderSeqNumBegin;
    private int senderSeqNumEnd;
    private int receiverSeqNumBegin;
    private int receiverSeqNumEnd;

    private int senderState;
    private int receiverState;


    private GoBackNQueue<Packet> senderQueue;
    private GoBackNQueue<Packet> receiverQueue;

    /**
     * Also add any necessary methods (e.g. checksum of a String)
     */

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
        WindowSize = winsize;
        LimitSeqNo = winsize + 1; // set appropriately; assumes SR here!
        RxmtInterval = delay;
    }

    /** This routine will be called whenever the upper layer at the sender [A]
     has a message to send.  It is the job of your protocol to insure that
     the data in such a message is delivered in-order, and correctly, to
     the receiving upper layer.
     */
    protected void aOutput(Message message) {

    }

    /**
     *   This routine will be called whenever a packet sent from the B-side
     *      (i.e. as a result of a toLayer3() being done by a B-side procedure)
     *      arrives at the A-side.  "packet" is the (possibly corrupted) packet
     *      sent from the B-side.
     */

    protected void aInput(Packet packet) {

    }

    /**
     *  This routine will be called when A's timer expires (thus generating a
     *      timer interrupt). You'll probably want to use this routine to control
     *      the retransmission of packets. See startTimer() and stopTimer(), above,
     *      for how the timer is started and stopped.
     */
    protected void aTimerInterrupt() {

    }

    /**
     * This routine will be called once, before any of your other A-side
     *      routines are called. It can be used to do any required
     *      initialization (e.g. of member variables you add to control the state
     *      of entity A).
     */
    protected void aInit() {
        senderSeqNumBegin = FirstSeqNo;
        senderSeqNumEnd = senderSeqNumBegin + WindowSize - 1;
        senderState = 0;
        senderQueue = new GoBackNQueue<Packet>(WindowSize);
    }



    /**
     *    This routine will be called whenever a packet sent from the B-side
     *      (i.e. as a result of a toLayer3() being done by an A-side procedure)
     *      arrives at the B-side.  "packet" is the (possibly corrupted) packet
     *      sent from the A-side.
     */
    protected void bInput(Packet packet) {

    }

    /**
     *   This routine will be called once, before any of your other B-side
     *      routines are called. It can be used to do any required
     *      initialization (e.g. of member variables you add to control the state
     *      of entity B).
     */
    protected void bInit() {
        receiverSeqNumBegin = FirstSeqNo;
        receiverSeqNumEnd = receiverSeqNumBegin + WindowSize - 1;
        receiverState = 0;
        receiverQueue = new GoBackNQueue<Packet>(WindowSize);
    }

    // Use to print final statistics
    protected void Simulation_done() {
        // TO PRINT THE STATISTICS, FILL IN THE DETAILS BY PUTTING VARIBALE NAMES. DO NOT CHANGE THE FORMAT OF PRINTED OUTPUT
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
        //System.out.println("Example statistic you want to check e.g. number of ACK packets received by A :" + "<YourVariableHere>");
    }

}