package messages;

public class ProgressMessage extends Message {
    public static final String MESSAGE_NAME = "progress";

    private int currentCase = 0;
    private int totalCases = 0;
    private CaseState caseState = CaseState.NONE;
    private CaseOutcome caseOutcome = CaseOutcome.NONE;

    public enum CaseState {
        NONE("none"),
        RUNNING("running"),
        DONE("done");

        private String name;

        CaseState(String name) {
            this.name = name;
        }

        public String getName() { return name; }

        static public CaseState fromName(String name) throws Exception {
            for (CaseState v : values()) {
                if (v.name.equals(name)) {
                    return v;
                }
            }
            throw new Exception("Unknown enum name " + name);
        }
    }

    public enum CaseOutcome {
        NONE("none"),
        OK("ok"),
        EXPR_ERROR("bug-candidate-expr"),   // unexpected expression result - bug in solc or soltix or blockchain client
        EVENT_ERROR("bug-candidate-event"), // unexpected event result - bug in solc or soltix or blockchain client
        INVALID_OPCODE("invalid-opcode"),   // invalid opcode - bug in solc/soltix/blockchain client or bad random transaction data for externally supplied contract (not a bug)
        OTHER_ERROR("other-error");

        private String name;

        CaseOutcome(String name) {
            this.name = name;
        }

        public String getName() { return name; }

        static public CaseOutcome fromName(String name) throws Exception {
            for (CaseOutcome v : values()) {
                if (v.name.equals(name)) {
                    return v;
                }
            }
            throw new Exception("Unknown enum name " + name);
        }
    }


    public ProgressMessage(int senderId, int currentCase, int totalCases, CaseState caseState, CaseOutcome caseOutcome) throws Exception {
        super(senderId);
        this.currentCase = currentCase;
        this.totalCases = totalCases;
        this.caseState = caseState;
        this.caseOutcome = caseOutcome;
    }

    public CaseState getCaseState() { return caseState; }
    public CaseOutcome getCaseOutcome() { return caseOutcome; }
    public int getCurrentCase() { return currentCase; }
    public int getTotalCases() { return totalCases; }

    @Override
    public String serialize() {
        String result;
        result = senderId + " " + MESSAGE_NAME + " " + currentCase + " " + totalCases + " " + caseState.name;
        if (caseState == CaseState.DONE) {
            result += " " + caseOutcome.name;
        }
        return result;
    }
}
