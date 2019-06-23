package messages;

public class StatusMessage extends Message {
    public static final String MESSAGE_NAME = "status";

    private Status status;

    public enum Status {
        NONE("none"),
        ON("on"),
        OFF("off");

        private String name;

        Status(String name) {
            this.name = name;
        }

        static public Status fromName(String name) throws Exception {
            for (Status v : values()) {
                if (v.name.equals(name)) {
                    return v;
                }
            }
            throw new Exception("Unknown enum name " + name);
        }
    }

    public StatusMessage(int senderId, Status status) throws Exception {
        super(senderId);
        this.status = status;
    }

    public Status getStatus() { return status; }

    @Override
    public String serialize() {
        return senderId + " " + MESSAGE_NAME + " " + status.name;
    }
}
