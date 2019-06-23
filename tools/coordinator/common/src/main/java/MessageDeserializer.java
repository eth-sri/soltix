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
            String caseId = components[4];
            ProgressMessage.CaseState caseState = ProgressMessage.CaseState.NONE;
            ProgressMessage.CaseOutcome caseOutcome = ProgressMessage.CaseOutcome.NONE;

            if (components[5].equals("generating")) {
                // case <cur> <total> <caseid> generating             later: [...extra-info...]
                caseState = ProgressMessage.CaseState.GENERATING;
                if (components.length != 6) {
                    throw new Exception("Unexpected case running component count " + components.length);
                }
            } else if (components[5].equals("running")) {
                // case <cur> <total> <caseid> running             later: [...extra-info...]
                caseState = ProgressMessage.CaseState.RUNNING;
                if (components.length != 6) {
                    throw new Exception("Unexpected case running component count " + components.length);
                }
            } else if (components[5].equals("done")) {
                // case <cur> <total> <caseid> done [ok|expr-error|event-error|other]
                caseState = ProgressMessage.CaseState.DONE;
                if (components.length != 7) {
                    throw new Exception("Unexpected case running component count " + components.length);
                }
                caseOutcome = ProgressMessage.CaseOutcome.fromName(components[6]);
            } else {
                throw new Exception("Unexpected case state '" + components[5] + "'");
            }
            return new ProgressMessage(senderId, currentCase, totalCases, caseId, caseState, caseOutcome);
        } else {
            throw new Exception("Received unknown message '" + components[1]);
        }
    }
}
