package messages;

abstract public class Message {
    protected int senderId = 0;

    public Message(int senderId) throws Exception {
        if (senderId < 1) {
            throw new Exception("Invalid sender ID < 1");
        }
        this.senderId = senderId;
    }

    public int getSenderId() { return senderId; }
    public int getZeroBasedSenderId() { return getSenderId() - 1; }

    abstract String serialize();
}
