import messages.ProgressMessage;

public class NodeState {
    private ProgressMessage lastProgressMessage;
    private long lastMessageTimestamp = 0;
    private long lastMessageDuration = 0;
    private boolean offline = false;

    public void setOffline(boolean offline) {
        this.offline = offline;
    }

    public boolean getOffline() {
        return offline;
    }

    public void update(ProgressMessage progressMessage) {
        lastProgressMessage = progressMessage;
        long timestamp = System.currentTimeMillis() / 1000;
        lastMessageDuration = lastMessageTimestamp != 0? timestamp - lastMessageTimestamp: 0;
        lastMessageTimestamp = timestamp;
    }

    protected String durationString(long durationInSeconds) {
        if (durationInSeconds >= 3600) {
            return String.valueOf(durationInSeconds / 3600) + " h";
        } else if (durationInSeconds >= 60) {
            return String.valueOf(durationInSeconds / 60) + " m";
        } else {
            return String.valueOf(durationInSeconds) + " s";
        }
    }

    public String toString() {
        if (lastProgressMessage == null) {
            return "ONLINE";
        } else if (lastProgressMessage.getCaseState() == ProgressMessage.CaseState.RUNNING) {
            return lastProgressMessage.getCurrentCase() + " / " + lastProgressMessage.getTotalCases(); // indicate time? may clutter output though
        } else if (lastProgressMessage.getCaseState() == ProgressMessage.CaseState.DONE) {
            return lastProgressMessage.getCaseOutcome().getName().toUpperCase() + " " + durationString(lastMessageDuration);
        } else {
            return "unknown state " + lastProgressMessage.getCaseState().getName();
        }
    }
}
