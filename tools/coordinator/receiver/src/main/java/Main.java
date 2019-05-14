import messages.Message;
import messages.ProgressMessage;
import messages.StatusMessage;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.zookeeper.data.Stat;

import java.util.Properties;

public class Main {
    private static NodeState [] nodeStates;

    protected static void usage() {
        System.out.println("Usage: coordinator [instance-count]");
        System.exit(1);
    }

    protected static void error(String message) {
        System.out.println("ERROR: " + message);
    }

    protected static void fatal(String message) {
        System.out.println("FATAL ERROR: " + message);
        System.exit(1);
    }

    public static void main(String [] args) throws Exception {
        if (args.length != 1) {
            System.out.println("Error: Invalid argument count " + args.length);
            usage();
        }

        int instanceCount = Integer.parseInt(args[0]);
        nodeStates = new NodeState[instanceCount];

        TCPServer server = new TCPServer();
        Message message;

        while ((message = server.readMessage()) != null) {
            if (message instanceof StatusMessage) {
                evaluateStatusMessage((StatusMessage)message);
            } else if (message instanceof  ProgressMessage) {
                evaluateProgressMessage((ProgressMessage)message);
            } else {
                System.out.println("Error: Unknown message type received " + message.getClass().toString());
            }
            printState();
            if (allNodesDone()) { // TODO add timeout
                System.out.println("All done - terminating.");
                break;
            }
        }
    }

    protected static void evaluateStatusMessage(StatusMessage message) {
        System.out.println(message.serialize());
        if (message.getStatus() == StatusMessage.Status.ON) {
            if (message.getZeroBasedSenderId() >= nodeStates.length) {
                fatal("Unexpected status sender ID value - too high, " + message.getZeroBasedSenderId());
            } else if (nodeStates[message.getZeroBasedSenderId()] != null) {
                fatal("Unexpected status: repeated 'on' message for sender " + message.getZeroBasedSenderId());
            }
            nodeStates[message.getZeroBasedSenderId()] = new NodeState();
        } else if (message.getStatus() == StatusMessage.Status.OFF) {
            if (message.getZeroBasedSenderId() >= nodeStates.length) {
                fatal("Unexpected status sender ID value - too high, " + message.getZeroBasedSenderId());
            } else if (nodeStates[message.getZeroBasedSenderId()] == null) {
                fatal("Unexpected status: 'off' message for unregistered sender " + message.getZeroBasedSenderId());
            } else if (nodeStates[message.getZeroBasedSenderId()].getOffline()) {
                fatal("Unexpected status: 'off' message for sender that is already offline " + message.getZeroBasedSenderId());
            }
            nodeStates[message.getZeroBasedSenderId()].setOffline(true);
        } else {
            error("Received unknown status message " + message.getStatus());
        }
    }

    protected static void evaluateProgressMessage(ProgressMessage message) {
        System.out.println(message.serialize());
        if (message.getZeroBasedSenderId() >= nodeStates.length) {
            fatal("Unexpected progress sender ID value - too high, " + message.getZeroBasedSenderId());
        } else if (nodeStates[message.getZeroBasedSenderId()] == null) {
            fatal("Unexpected progress message for unregistered sender " + message.getZeroBasedSenderId());
        } else if (nodeStates[message.getZeroBasedSenderId()].getOffline()) {
            fatal("Unexpected progress message for sender that is offline " + message.getZeroBasedSenderId());
        }
        nodeStates[message.getZeroBasedSenderId()].update(message);
    }

    protected static void printState() {
        for (int i = 0; i < nodeStates.length; ++i) {
            String nodeText;
            if (nodeStates[i] == null) {
                nodeText = "OFFLINE";
            } else {
                nodeText = nodeStates[i].toString();
            }

            String fullText = "" + i + ": " + nodeText;
            String paddedText = String.format("%-15s", fullText);
            System.out.print(paddedText);
        }
        System.out.println("");
    }

    protected static boolean allNodesDone() {
        for (int i = 0; i < nodeStates.length; ++i) {
            if (nodeStates[i] == null || !nodeStates[i].getOffline()) {
                return false;
            }
        }
        return true;
    }
}
