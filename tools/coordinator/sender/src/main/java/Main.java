import messages.ProgressMessage;
import messages.StatusMessage;

public class Main {
    protected static void usage() {
        System.out.println("Usage: sender [coordinator-host] [own-id] status on");
        System.out.println("       sender [coordinator-host] [own-id] status off");
        System.out.println("       sender [coordinator-host] [own-id] progress generating [cur-case] [total-cases] [case-id]");
        System.out.println("       sender [coordinator-host] [own-id] progress running [cur-case] [total-cases] [case-id]");
        System.out.println("       sender [coordinator-host] [own-id] progress done [cur-case] [total-cases] [case-id] [ok|expr_error|event_error|other_error]");
        System.exit(1);
    }

    public static void main(String [] args) throws Exception {
        if (args.length < 4) {
            System.out.println("Error: Invalid argument count " + args.length);
            usage();
        }

        String host = args[0];
        int senderId = Integer.parseInt(args[1]);
        String messageType = args[2];

        String serializedMessage = null;
        if (messageType.equals(StatusMessage.MESSAGE_NAME)) {
            StatusMessage.Status status = StatusMessage.Status.fromName(args[3]);
            StatusMessage message = new StatusMessage(senderId, status);
            serializedMessage = message.serialize();
        } else if (messageType.equals(ProgressMessage.MESSAGE_NAME)) {
            // running/done
            ProgressMessage.CaseState state = ProgressMessage.CaseState.fromName(args[3]);

            // pertinent to case x of y
            int curCase = Integer.parseInt(args[4]);
            int totalCases = Integer.parseInt(args[5]);
            String caseId = args[6];

            // elaborate on result if done
            ProgressMessage.CaseOutcome outcome = ProgressMessage.CaseOutcome.NONE;
            if (state == ProgressMessage.CaseState.DONE) {
                outcome = ProgressMessage.CaseOutcome.fromName(args[7]);
            }

            ProgressMessage message = new ProgressMessage(senderId, curCase, totalCases, caseId, state, outcome);
            serializedMessage = message.serialize();
        } else {
            System.out.println("Error: Unknown message type '" + messageType + "'");
            usage();
        }


        TCPClient client = new TCPClient(host);

        client.writeLine(serializedMessage);
    }
}
