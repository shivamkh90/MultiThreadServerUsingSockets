package co.shivam;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This program is a very simple Web server. When it receives a HTTP request it
 * sends the request back as the reply. This can be of interest when you want to
 * see just what a Web client is requesting, or what data is being sent when a
 * form is submitted, for example.
 */
public class Main {

    static final int PORT = 8091;

    public static void main(String args[]) {
        ServerSocket serverSocket = null;
        Socket socket = null;
        Integer threadCount=0;
        Map<String, WaitObject> interruptMap = new ConcurrentHashMap<>();//connId and obj on which thread is waiting
        try {
            serverSocket = new ServerSocket(PORT);
        } catch (IOException e) {
            e.printStackTrace();
        }

        while (true) {
            try {
                socket = serverSocket.accept();
            } catch (IOException e) {
                System.out.println("I/O error: " + e);
            }
            String threadName=String.valueOf(threadCount);
            threadCount++;
            // new thread for a client
            new Thread(new RequestThread(socket, interruptMap), threadName).start();
        }
    }
}

