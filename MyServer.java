import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;


public class MyServer {

    private static JFrame frame = new JFrame("Server");
    private static JTextArea messageArea = new JTextArea(15, 40);
    private static JTextArea userArea = new JTextArea(1, 10);

    /**
     * The port that the server listens on.
     */
    private static final int PORT = 9001;

    /**
     * The set of all names of clients in the chat room.  Maintained
     * so that we can check that new clients are not registering name
     * already in use.
     */
    private static HashSet<String> names = new HashSet<String>();

    /**
     * The set of all the print writers for all the clients.  This
     * set is kept so we can easily broadcast messages.
     */
    private static HashSet<PrintWriter> writers = new HashSet<PrintWriter>();

    static String input;


    /**
     * Create GUI for server's view of chatroom
     */
    public MyServer() {

        // Layout GUI
        messageArea.setEditable(false);
        frame.getContentPane().add(new JScrollPane(messageArea), "Center");
        userArea.setEditable(false);
        frame.getContentPane().add(new JScrollPane(userArea), "East");
        frame.pack();
    }


    public static void viewUsers() {
        userArea.setText("");
        for (String users : names) {
            userArea.append(users + "\n");
        }
    }

    /**
     * The appplication main method, which listens on a port, displays server info, and
     * spawns handler threads.
     */
    public static void main(String[] args) throws Exception {
        messageArea.append("Server IP Address: " + InetAddress.getLocalHost().getHostAddress() + "\n");
        messageArea.append("Port: " + PORT + "\n\n");
        messageArea.append("The chat server is running." + "\n\n");
        MyServer server = new MyServer();
        server.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        server.frame.setVisible(true);
        ServerSocket listener = new ServerSocket(PORT);
        try {
            while (true) {
                new Handler(listener.accept()).start();
            }
        } finally {
            listener.close();
        }
    }

    /**
     * A handler thread class.  Handlers are spawned from the listening
     * loop and are responsible for a dealing with a single client
     * and broadcasting its messages.
     */
    private static class Handler extends Thread {
        private String name;
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;

        /**
         * Constructs a handler thread, squirreling away the socket.
        */
        public Handler(Socket socket) {
            this.socket = socket;
        }

        /**
         * Services this thread's client by repeatedly requesting a
         * screen name until a unique one has been submitted, then
         * acknowledges the name, displays the user has joined chat,
         * and registers the output stream for the client in a global
         * set, then repeatedly gets inputs and broadcasts them.
         */
        public void run() {
            try {

                // Create character streams for the socket.
                in = new BufferedReader(new InputStreamReader(
                    socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // Request a name from this client.  Keep requesting until
                // a name is submitted that is not already used.  Note that
                // checking for the existence of a name and adding the name
                // must be done while locking the set of names.
                while (true) {
                    out.println("SUBMITNAME");
                    name = in.readLine();
                	messageArea.append(name + " joined the chat\n");
                    if (name == null) {
                        return;
                    }
                    synchronized (names) {
                        if (!names.contains(name)) {
                            names.add(name);
                            break;
                        }
                    }
                }

                // Now that a successful name has been chosen, add the
                // socket's print writer to the set of all writers so
                // this client can receive broadcast messages.
                out.println("NAMEACCEPTED");
                writers.add(out);

                // Accept messages from this client and broadcast them.
                // Server views the chat from all clients
                // Ignore other clients that cannot be broadcasted to.
                while (true) {
                    viewUsers();
                    input = in.readLine();
                    if (input == null) {
                        return;
                    }
                    for (PrintWriter writer : writers) {
                        writer.println("MESSAGE " + name + ": " + input);
                    }

                    messageArea.append(name + ": " + input + "\n");
                }
            } catch (IOException e) {
                System.out.println(e);
            } finally {
                // This client is going down!  Remove its name and its print
                // writer from the sets, and close its socket.
                // Server is notified which client leaves the chat
                if (name != null) {
                    names.remove(name);
                    viewUsers();
                    messageArea.append(name + " has left the chat\n");
                }
                if (out != null) {
                    writers.remove(out);
                }
                try {
                    socket.close();
                } catch (IOException e) {
                }
            }
        }
    }
}