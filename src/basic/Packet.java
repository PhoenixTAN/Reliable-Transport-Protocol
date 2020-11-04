package basic;

import simulator.NetworkSimulator;

import java.util.ArrayList;
import java.util.List;

/**
 * @author CS-655
 * @description The unit of data passed between your routines and the network layer
 */
public class Packet {
	
	private int seqnum;
	private int acknum;
	private long checksum;		/** modified by Ziqi Tan */
	private String payload;
	private boolean flag; // if there are optional field used, like sACK
	private List<Integer> sACK;

	public Packet(Packet p) {
		seqnum = p.getSeqnum();
		acknum = p.getAcknum();
		checksum = p.getChecksum();
		payload = new String(p.getPayload());
		sACK = new ArrayList<>();
	}

	public Packet(int seq, int ack, long check, String newPayload) {
		seqnum = seq;
		acknum = ack;
		checksum = check;
		if (newPayload == null) {
			payload = "";
		} else if (newPayload.length() > NetworkSimulator.MAXDATASIZE) {
			payload = null;
		} else {
			payload = new String(newPayload);
		}
		sACK = new ArrayList<>();
	}

	public Packet(int seq, int ack, long check) {
		seqnum = seq;
		acknum = ack;
		checksum = check;
		payload = "";
		sACK = new ArrayList<>();
	}

	public boolean setSeqnum(int n) {
		seqnum = n;
		return true;
	}

	public boolean setAcknum(int n) {
		acknum = n;
		return true;
	}

	public boolean setChecksum(long n) {
		checksum = n;
		return true;
	}

	public boolean setPayload(String newPayload) {
		if (newPayload == null) {
			payload = "";
			return false;
		} else if (newPayload.length() > NetworkSimulator.MAXDATASIZE) {
			payload = "";
			return false;
		} else {
			payload = new String(newPayload);
			return true;
		}
	}

	public int getSeqnum() {
		return seqnum;
	}

	public int getAcknum() {
		return acknum;
	}

	public long getChecksum() {
		return checksum;
	}

	public String getPayload() {
		return payload;
	}

	public String toString() {
		return ("seqnum: " + seqnum + "  acknum: " + acknum + "  checksum: " + checksum + "  payload: " + payload);
	}

	public boolean isFlag() {
		return flag;
	}

	public void setFlag(boolean flag) {
		this.flag = flag;
	}

	public List<Integer> getsACK() {
		return sACK;
	}

	public void setsACK(List<Integer> sACK) {
		this.sACK = sACK;
	}
}
