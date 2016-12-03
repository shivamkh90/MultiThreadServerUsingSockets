package co.shivam;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.print.attribute.standard.DateTimeAtCreation;

/**
 * Created by shivam on 3/12/16.
 */
public class RequestThread implements Runnable {

    Socket socket;
    Map<String, WaitObject> interruptMap;
    private ThreadLocal<String> connId = new ThreadLocal<String>();
    WaitObject object;

    public RequestThread(Socket clientSocket, Map<String, WaitObject> map) {
        this.socket = clientSocket;
        this.interruptMap = map;
    }

    @Override
    public void run() {

        InputStream inp = null;
        BufferedReader brinp = null;
        DataOutputStream out = null;
        List<String> requestDetails = new ArrayList<String>();
        try {
            inp = socket.getInputStream();
            brinp = new BufferedReader(new InputStreamReader(inp));
            out = new DataOutputStream(socket.getOutputStream());
            String line;

            while (true) {
                line = brinp.readLine();
                if ((line == null) || line.isEmpty()) {
                    break;
                } else {
                    System.out.println(line);
                    requestDetails.add(line);

                }
            }

            System.out.println("=======================================================");

            Map<String, String> params = getParams(requestDetails.get(0));
            if (requestDetails.get(0).startsWith("GET /sleep")) {
                if (sleepCall(params)) {
                    out.writeBytes("{\"stat\": \"ok\"}" + "\n\r");
                } else {
                    out.writeBytes("{\"stat\": \"killed\"}" + "\n\r");
                }
            } else if (requestDetails.get(0).startsWith("GET /server-status")) {
                String response = getLiveConnections();
                out.writeBytes(response + "\n\r");

            } else if (requestDetails.get(0).startsWith("POST /kill")) {
                if (kill(params)) {
                    out.writeBytes("{\"stat\": \"ok\"}" + "\n\r");
                } else {
                    out.writeBytes("{\"stat\": \"400 bad request\"}" + "\n\r");
                }
            } else {
                out.writeBytes("{\"stat\": \"404 Not found\"}" + "\n\r");
            }
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public boolean sleepCall(Map<String, String> params) {
        Long seconds = Long.parseLong(params.get("timeout"));
        object = new WaitObject();
        object.insertTime = LocalDateTime.now().plusSeconds(seconds);
        connId.set(params.get("connid"));
        interruptMap.put(params.get("connid"), object);

        boolean flag = true;
        try {
            synchronized (object) {
                object.wait(seconds * 1000);
                if(object.notified){
                    flag=false;
                }
            }
            System.out.println("Normal wait over");
        } catch (InterruptedException e) {
            flag = false;
            System.out.println("Notified on object");
        }

        interruptMap.remove(params.get("connid"));

        return flag;
    }

    public boolean kill(Map<String, String> params) {
        String connId = params.get("connid");
        WaitObject obj = interruptMap.get(connId);
        if (obj != null) {
            synchronized (obj) {
                obj.notified=true;
                obj.notifyAll();
            }
            return true;
        } else {
            return false;
        }
    }

    public String getLiveConnections(){
        List<String> records = new ArrayList<>();
        LocalDateTime time = LocalDateTime.now();
        Set<String> keys = interruptMap.keySet();
        StringBuilder response = new StringBuilder("[");
        for(String key: keys){
            WaitObject obj = interruptMap.get(key);
            if(obj != null){
                long secondsLeft = Duration.between(time, obj.insertTime).getSeconds();
                response.append("\""+key+ "\""+":"+secondsLeft+",");
                records.add("\""+key+ "\""+":"+secondsLeft+",");
            }
        }
        //response.replace(response.lastIndexOf(","), response.lastIndexOf(","), "]");
        return response.toString();
    }

    private Map<String, String> getParams(String s) {
        String reqString = s.split(" ")[1];
        String reqParamString = reqString.split("\\?")[1];
        String[] params = reqParamString.split("&");
        Map<String, String> map = new HashMap<>();
        for (String param : params) {
            map.put(param.split("=")[0], param.split("=")[1]);
        }

        return map;

    }
}
