import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.net.Socket;

public class TCPClient {
    private Socket clientSocket;
    private BufferedWriter bufferedWriter;

    public TCPClient(String host) throws Exception {
        clientSocket = new Socket(host, 22732);
        bufferedWriter = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
    }

    public void writeLine(String message) throws Exception  {
        bufferedWriter.write(message + "\n");
        bufferedWriter.flush();
    }
}
