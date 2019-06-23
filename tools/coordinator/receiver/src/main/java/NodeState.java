import messages.ProgressMessage;

public class NodeState {
    private ProgressMessage lastProgressMessage;
    private long lastMessageTimestamp = 0;
    private long lastMessageDuration = 0;
    private long totalProcessingDuration = 0;
    private boolean offline = false;
    private int zeroBasedNodeId;
    private int lastCompletedCase = 0;
    private int totalCases = 0;
    private long lastETA = 0;

    public NodeState(int zeroBasedNodeId) {
        this.zeroBasedNodeId = zeroBasedNodeId;
    }

    public void setOffline(boolean offline) {
        this.offline = offline;
    }

    public boolean getOffline() {
        return offline;
    }

    public long getLastETA() {
        return lastETA;
    }

    public int getLastCompletedCase() {
        return lastCompletedCase;
    }

    public int getTotalCases() {
        return totalCases;
    }


    public void update(ProgressMessage progressMessage) {
        lastProgressMessage = progressMessage;
        long timestamp = System.currentTimeMillis() / 1000;
        lastMessageDuration = lastMessageTimestamp != 0? timestamp - lastMessageTimestamp: 0;
        totalProcessingDuration += lastMessageDuration;
        lastMessageTimestamp = timestamp;
        if (progressMessage.getCaseOutcome() != ProgressMessage.CaseOutcome.NONE) { // test case result
            lastCompletedCase = progressMessage.getCurrentCase();
            totalCases = progressMessage.getTotalCases();
        }
    }

    public static String durationString(long durationInSeconds) {
        if (durationInSeconds >= 3600) {
            return String.valueOf(durationInSeconds / 3600) + " h";
        } else if (durationInSeconds >= 60) {
            return String.valueOf(durationInSeconds / 60) + " m";
        } else {
            return String.valueOf(durationInSeconds) + " s";
        }
    }

    protected void updateETA() {
        lastETA = (long)((totalProcessingDuration/lastProgressMessage.getCurrentCase())+0.5)*(lastProgressMessage.getTotalCases()-lastProgressMessage.getCurrentCase());
    }

    public String toStringKeepingState() {
        String baseString = zeroBasedNodeId + ": ";
        String caseString = lastProgressMessage != null? lastProgressMessage.getCurrentCase() + "/" + lastProgressMessage.getTotalCases(): null;

        if (lastProgressMessage != null) {
            caseString += " (" + lastProgressMessage.getCaseIdString() + ")";
        }

        if (lastProgressMessage == null) {
            return baseString + "ONLINE";
        } else if (lastProgressMessage.getCaseState() == ProgressMessage.CaseState.GENERATING) {
            return baseString + caseString +" - GENERATED";
        } else if (lastProgressMessage.getCaseState() == ProgressMessage.CaseState.RUNNING) {
            return baseString + caseString + "..."; // indicate time? may clutter output though
        } else if (lastProgressMessage.getCaseState() == ProgressMessage.CaseState.DONE) {
            updateETA();
            return baseString + caseString + " - "
                    + lastProgressMessage.getCaseOutcome().getName().toUpperCase()
                    + "  (" + durationString(lastMessageDuration)
                    +       ", total " + durationString(totalProcessingDuration)
                    +       ", eta " + durationString(lastETA)
                    + ")";
        } else {
            return baseString + "unknown state " + lastProgressMessage.getCaseState().getName();
        }
    }

    public boolean haveNotableState() {
        // Only final results are considered notable for now
        // TODO exceptions when generating contracts, hang-ups, ...
        return lastProgressMessage != null &&
                (lastProgressMessage.getCaseOutcome() != ProgressMessage.CaseOutcome.NONE       // a test case result
              || lastProgressMessage.getCaseState() == ProgressMessage.CaseState.GENERATING);   // generating test cases - insignificant but just to avoid long-term output silence
    }

    public String toStringRemovingState() {
        String result = toStringKeepingState();
        lastProgressMessage = null;
        return result;
    }
}
