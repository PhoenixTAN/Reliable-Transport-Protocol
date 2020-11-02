package basic;

public class PacketSACK extends Packet{

    private boolean flag; // if there are optional field used, like sACK
    private int[] sACK;

    public PacketSACK(Packet p) {
        super(p);
        sACK = new int[5];
    }

    public PacketSACK(int seq, int ack, long check, String newPayload) {
        super(seq, ack, check, newPayload);
        sACK = new int[5];
    }

    public PacketSACK(int seq, int ack, long check) {
        super(seq, ack, check);
        sACK = new int[5];
    }

    public boolean isFlag() {
        return flag;
    }

    public void setFlag(boolean flag) {
        this.flag = flag;
    }
}
