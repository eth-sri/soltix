import messages.Message;
import messages.ProgressMessage;
import messages.StatusMessage;

public class MessageDeserializer {
    public static Message decode(String contents) throws Exception {
        String [] components = contents.split(" ");
        if (components.length < 2) {
            throw new Exception("Unexpected incomplete message");
        }

        //System.out.println("got " + contents + " components " + components.length);

        int senderId = Integer.parseInt(components[0]);
        if (components[1].equals(StatusMessage.MESSAGE_NAME)) {
            if (components.length < 3) {
                throw new Exception("Unexpected StatusMessage with fewer than 3 fields");
            }
            StatusMessage.Status status = StatusMessage.Status.fromName(components[2]);
            return new StatusMessage(senderId, status);
        } else if (components[1].equals(ProgressMessage.MESSAGE_NAME)) {
            int currentCase = Integer.parseInt(components[2]);
            int totalCases = Integer.parseInt(components[3]);
            ProgressMessage.CaseState caseState = ProgressMessage.CaseState.NONE;
            ProgressMessage.CaseOutcome caseOutcome = ProgressMessage.CaseOutcome.NONE;

            if (components[4].equals("running")) {
                // case <cur> <total> running             later: [...extra-info...]
                caseState = ProgressMessage.CaseState.RUNNING;
                if (components.length != 5) {
                    throw new Exception("Unexpected case running component count " + components.length);
                }
            } else if (components[4].equals("done")) {
                // case <cur> <total> done [ok|expr-error|event-error|other]
                caseState = ProgressMessage.CaseState.DONE;
                if (components.length != 6) {
                    throw new Exception("Unexpected case running component count " + components.length);
                }
                caseOutcome = ProgressMessage.CaseOutcome.fromName(components[5]);
            } else {
                throw new Exception("Unexpected case state '" + components[4] + "'");
            }
            return new ProgressMessage(senderId, currentCase, totalCases, caseState, caseOutcome);
        } else {
            throw new Exception("Received unknown message '" + components[1]);
        }
    }
}
