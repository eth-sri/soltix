import messages.Message;

import java.io.*;
import java.net.*;

public class TCPServer {
    private ServerSocket serverSocket;

    public TCPServer() throws Exception {
        serverSocket = new ServerSocket(22732);
    }

    public Message readMessage() throws Exception {
        Socket connectionSocket = serverSocket.accept();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
        String line = bufferedReader.readLine();

        Message message = MessageDeserializer.decode(line);

        connectionSocket.close();
        return message;
    }
}
