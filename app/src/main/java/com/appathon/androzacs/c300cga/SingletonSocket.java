package com.appathon.androzacs.c300cga;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.view.Gravity;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by NMakwana119578 on 3/27/2015.
 */

final class ConnectionInfo {
    public String IP_ADDR;
    public long PORT;
    public String RESPONSE;
}

public class SingletonSocket {


    private static Socket socket = null;
    private static String socket_name = "";

    public static void setSocket(Socket socketpass , String name) {
        socket = socketpass;
        socket_name = name;
    }

    public static Socket getSocket() {
        return socket;
        //return socket;
    }

    public static String getSocketName()
    {
        return socket_name;
    }

}

interface ISocketConnection
{
    void OnSocketConnect(ConnectionInfo cnf);
}

class MyClientTask extends AsyncTask<Void, Void, Void> {

    String dstAddress;
    int dstPort;
    String response = "";
    String tool_name = "";
    ISocketConnection m_owner;

    MyClientTask(String addr, int port , String name , ISocketConnection owner){
        dstAddress = addr;
        dstPort = port;
        m_owner = owner;
        tool_name = name;
    }

    @Override
    protected Void doInBackground(Void... arg0) {

        Socket socket = null;

        try {
            SingletonSocket.setSocket(null , "");
            socket = new Socket();
            socket.connect(new InetSocketAddress(dstAddress , dstPort) , 2500);
            socket.setKeepAlive(true);
            System.out.println("just connected to " + socket.getRemoteSocketAddress());

            OutputStream outServer = socket.getOutputStream();
            DataOutputStream outStream = new DataOutputStream(outServer);

            outStream.write(new String("20\t1\n").getBytes());
            String slog = "\n Just sent the registration command :20\t1\n";
            System.out.println(slog);

            InputStream inputStream1 = socket.getInputStream();
            BufferedReader r = new BufferedReader(new InputStreamReader(inputStream1));
            String line =r.readLine();

            if(line != null)
            {
                if(line.compareTo("0\tRegistered") == 0)
                    System.out.println("Registration result :" + line);
            }

        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            response = "UnknownHostException: " + e.toString();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            response = "IOException: " + e.toString();
        }
        catch(Exception e){
            // TODO Auto-generated catch block
            e.printStackTrace();
            response = "Exception: " + e.toString();
        }
        finally{
            //if(AppHelper.IsSocketConnectionAlive(socket))
                SingletonSocket.setSocket(socket , tool_name);
            if(response.isEmpty() && !AppHelper.IsSocketConnectionAlive(socket) )
                response = "Socket was closed or disconnected";
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        ConnectionInfo cnf = new ConnectionInfo();
        cnf.IP_ADDR = dstAddress;
        cnf.PORT =  dstPort;
        cnf.RESPONSE = response;
        m_owner.OnSocketConnect(cnf);
        super.onPostExecute(result);
    }

}

class AppHelper
{
    public static final String TOOLS_DATA = "C300CGA_tools_data";
    public static final String TOOL_NAME = "C300CGA_tool_name";
    public static final String TOOL_ADDRESS = "C300CGA_tool_ip";
    public static final String TOOL_PORT = "C300CGA_tool_port";
    public static final String TOOL_ID = "C300CGA_tool_id";
    public static final String TOOL_PREF = "C300CGA_tool_Pref";
    public static final int MAX_TOOLS = 4;

    public static void CreateSocketConnection(String ipAddress , int port , String name ,ISocketConnection iSocket)
    {
        int corePoolSize = 60;
        int maximumPoolSize = 80;
        int keepAliveTime = 10;

        BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<Runnable>(maximumPoolSize);
        Executor threadPoolExecutor = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, TimeUnit.SECONDS, workQueue);

        MyClientTask myClientTask = new MyClientTask(ipAddress, port ,name , iSocket);
        myClientTask.executeOnExecutor(threadPoolExecutor);
    }

    public static void SaveToolsData(Map<Integer  ,ToolInfo> map , Activity activity)
    {
        //if(map.size() != 0)
        {
            SharedPreferences settings = activity.getSharedPreferences(TOOLS_DATA ,Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = settings.edit();
            // clear all old entries
            editor.clear().commit();

            int count = 1;
            for(Map.Entry<Integer , ToolInfo> entry : map.entrySet())
            {
                ToolInfo tInfo = entry.getValue();
                String name = tInfo.GetToolName();
                String sIp = tInfo.m_ToolIP;
                long iPort = tInfo.m_Port;

                if(sIp.startsWith("/"))
                    sIp = sIp.replace("/", "");

                editor.putInt(TOOL_ID + String.valueOf(count) , entry.getKey());
                editor.putString(TOOL_NAME + String.valueOf(count), name);
                editor.putString(TOOL_ADDRESS + String.valueOf(count) ,sIp);
                editor.putLong(TOOL_PORT + String.valueOf(count) ,iPort);
                editor.putString(TOOL_PREF + String.valueOf(count) , tInfo.getToolPreference());
                count++;
            }
            editor.commit();
        }
    }

    public static Map<Integer , String> getToolsData(Activity activity)
    {
        SharedPreferences setting  = activity.getSharedPreferences(TOOLS_DATA , Context.MODE_PRIVATE);
        Map<Integer , String> toolsMap = new HashMap<Integer , String>();
        int count = 1;
        while(true)
        {
            String tool_name = setting.getString(TOOL_NAME + String.valueOf(count), "");

            if(tool_name.isEmpty())
                break;

            String tool_ip = setting.getString(TOOL_ADDRESS + String.valueOf(count), "");
            long tool_port = setting.getLong(TOOL_PORT + String.valueOf(count), 0);
            int tool_id = setting.getInt(TOOL_ID + String.valueOf(count) , 0);
            String tool_pref = setting.getString(TOOL_PREF + String.valueOf(count) , "");

            if(tool_ip.isEmpty() || tool_port == 0 || tool_id == 0)
                break;

            toolsMap.put(tool_id , tool_name + "##" + tool_ip + "##" + String.valueOf( tool_port));
            toolsMap.put(tool_id + 10 , tool_pref);

            count++ ;
        }
        return toolsMap;
    }

    public static boolean IsSocketConnectionAlive(Socket socket)
    {
        if(socket != null && socket.isConnected())
            return true;
        return false;
    }

    public static void ShowAppMessage(String sText , Activity activity)
    {
        Context context = activity.getApplicationContext();
        CharSequence text = sText;
        int duration = Toast.LENGTH_SHORT;

        Toast toast = Toast.makeText(context, text, duration);
        toast.setGravity(Gravity.CENTER , 0 , 0);
        toast.show();
    }

    public static void ShowErrorMessage(String msg ,Activity activity )
    {
        AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(activity);

        dlgAlert.setMessage(msg);
        dlgAlert.setTitle("Error Message...");
        dlgAlert.setPositiveButton("OK", null);
        dlgAlert.setCancelable(true);
        dlgAlert.create().show();

        dlgAlert.setPositiveButton("Ok",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
    }

    public static boolean isParsableToInt(String input){
        boolean parsable = true;
        try{
            Integer.parseInt(input);
        }catch(NumberFormatException e){
            parsable = false;
        }
        return parsable;
    }
}