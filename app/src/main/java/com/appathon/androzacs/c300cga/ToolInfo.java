package com.appathon.androzacs.c300cga;

import android.os.Handler;
import android.util.Log;

import java.net.Socket;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by RKanthaliya116069 on 3/31/2015.
 */
public class ToolInfo implements ISocketAction{

    public enum ConnectionStatus {
        UNKNOWN(-1), NOT_CONNECTED(0), CONNECTED(1);
        private int value;

        private ConnectionStatus(int value) {
            this.value = value;
        }

        @Override
        public String toString() {
            switch (this) {
                case UNKNOWN:
                    System.out.println("Unknown");
                    break;
                case NOT_CONNECTED:
                    System.out.println("Not Connected");
                    break;
                case CONNECTED:
                    System.out.println("Connected");
                    break;
            }
            return super.toString();
        }
    };

    public enum SystemState {
        FAULTED(-1), IDLE(0), PROCESSING(1), UNKNOWN(2);
        private int value;

        private SystemState(int value) {
            this.value = value;
        }

        @Override
        public String toString() {
            switch (this) {
                case FAULTED:
                    System.out.println("Faulted");
                    break;
                case IDLE:
                    System.out.println("Idle");
                    break;
                case PROCESSING:
                    System.out.println("Processing");
                    break;
                case UNKNOWN:
                    System.out.println("Unknown");
                    break;
            }
            return super.toString();
        }
    };

    public Socket m_Socket; //Change this to private later
    private ConnectionStatus m_rConnectionStatus;
    private SystemState m_rSystemState;
    public int m_ToolID;
    private String m_ToolName;
    public String m_ToolIP;
    public long m_Port;

    private Map<String , SubSystemInfo> m_SubSystems;

    private ISocketAction m_interface;

    public SystemState GetSystemState()
    {
        return m_rSystemState;
    }

    public ConnectionStatus GetConnectionStatus()
    {
        return m_rConnectionStatus;
    }

    public String GetToolName()
    {
        return m_ToolName;
    }

    public ToolInfo(Socket socket, String toolName, int toolID)
    {
        m_rSystemState = SystemState.FAULTED;
        m_interface = this;
        m_ToolID = toolID;

        m_Socket = socket;
        if(m_Socket!=null) {
            if(m_Socket.isConnected())
            {
                m_rConnectionStatus = ConnectionStatus.CONNECTED;
                m_ToolIP = m_Socket.getInetAddress().getHostAddress();
                m_Port = m_Socket.getPort();
            }
            else
            {
                m_rConnectionStatus = ConnectionStatus.NOT_CONNECTED;
            }
        }
        else
        {
            m_rConnectionStatus = ConnectionStatus.UNKNOWN;
            m_ToolIP = "Unknown";
            m_Port= -1;
        }

        m_ToolName = toolName;

        m_SubSystems = new LinkedHashMap<>();
        m_SubSystems.put("CHA", new SubSystemInfo(m_Socket, "CHA"));
        m_SubSystems.put("CHB", new SubSystemInfo(m_Socket, "CHB"));
        m_SubSystems.put("CHC", new SubSystemInfo(m_Socket, "CHC"));
        m_SubSystems.put("CHD", new SubSystemInfo(m_Socket, "CHD"));
        //m_SubSystems.put("CHE", new SubSystemInfo(m_Socket, "CHE"));
        m_SubSystems.put("LLA", new SubSystemInfo(m_Socket, "LLA"));
        m_SubSystems.put("LLB", new SubSystemInfo(m_Socket, "LLB"));
        m_SubSystems.put("Buffer", new SubSystemInfo(m_Socket, "Buffer"));
        //m_SubSystems.put("Transfer", new SubSystemInfo(m_Socket, "Transfer"));

        timerHandler.postDelayed(timerRunnable , 0);
    }

    public void DeleteTool()
    {
        timerHandler.removeCallbacks(timerRunnable);
        for(Map.Entry<String , SubSystemInfo> entry : m_SubSystems.entrySet()) {
            m_SubSystems.get(entry.getKey()).DeleteSubSystem();
        }
        m_SubSystems.clear();
    }

    public Map<String, SubSystemInfo> GetSubSystemsMap()
    {
        return m_SubSystems;
    }

    public SubSystemInfo GetSubSystem(String key)
    {
        return m_SubSystems.get(key);
    }

    public String getToolPreference()
    {
        String sTool = "";
        String connector = "##";

        sTool = sTool + getSubSytemPreference("Buffer") + connector;
        sTool = sTool + getSubSytemPreference("CHA") + connector;
        sTool = sTool + getSubSytemPreference("CHB") + connector;
        sTool = sTool + getSubSytemPreference("CHC") + connector;
        sTool = sTool + getSubSytemPreference("CHD") + connector;
        sTool = sTool + getSubSytemPreference("LLA") + connector;
        sTool = sTool + getSubSytemPreference("LLB") ;

        return sTool;
    }

    private String getSubSytemPreference(String subSys)
    {
        if(GetSubSystem(subSys).getIsPartOfToolSystemInfo())
            return "true";
        return "false";
    }

    public void setToolPreference(String toolPref)
    {
        String[] sPref = toolPref.split("##");
        int i = 0;
        setSubSytemPreference("Buffer" , sPref[i++]);
        setSubSytemPreference("CHA" , sPref[i++]);
        setSubSytemPreference("CHB" , sPref[i++]);
        setSubSytemPreference("CHC" , sPref[i++]);
        setSubSytemPreference("CHD" , sPref[i++]);
        setSubSytemPreference("LLA" , sPref[i++]);
        setSubSytemPreference("LLB" , sPref[i++]);
    }

    private void setSubSytemPreference(String subSys , String sPref)
    {
        boolean bset = false;
        if(sPref.equals("true"))
            bset = true;
        GetSubSystem(subSys).setIsPartOfToolSystemInfo(bset);
    }
    //Dummy Timer to update tool connection status and tool state
    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {

        @Override
        public void run() {
//            QuerySocket querySoc = new QuerySocket(m_Socket , "" , m_interface);
//            querySoc.execute();
            System.out.println("In ToolInfo Timer: "+m_ToolName+"\n");
            updateToolStatus();
            timerHandler.postDelayed(this , 500);
        }
    };

    private void updateToolStatus()
    {
        Log.d("MyMessage: ToolInfo"+m_ToolName, "In ToolInfo Post Query");
        //Update Connection status here
        if(m_Socket!=null)
        {
            if(m_Socket.isConnected())
            {
                m_rConnectionStatus = ConnectionStatus.CONNECTED;
            }
            else
            {
                m_rConnectionStatus = ConnectionStatus.NOT_CONNECTED;
            }
        }
        else
        {
            m_rConnectionStatus = ConnectionStatus.UNKNOWN;
        }

        Log.d("MyMessage: ToolInfo "+m_ToolName+" ", m_rConnectionStatus.toString());
        //Check Subsystem States and update tool state here.
        if(m_SubSystems!=null && !m_SubSystems.isEmpty())
        {
            SubSystemInfo value;
            boolean isAnySubSystemStateFaulted = false;
            boolean isAnySubSystemStateProcessing = false;
            boolean isAnySubSystemStateUnknown = false;
            boolean isAtleastOneStateEvaluated = false;
            for(Map.Entry<String , SubSystemInfo> entry : m_SubSystems.entrySet())
            {
                if(entry!=null)
                {
                    value = entry.getValue();
                    if(value!=null && value.getIsPartOfToolSystemInfo())
                    {
                        isAtleastOneStateEvaluated = true;
                        SystemState state = value.GetSystemState();
                        Log.d("MyMessage:","ToolInfo: "+m_ToolName+" :: SubSystem -> " + value.GetSubsystemName() + " is " + state.toString());
                        if(state == SystemState.FAULTED) {
                            isAnySubSystemStateFaulted = true;
                            m_rSystemState = SystemState.FAULTED;
                        }
                        else if(state == SystemState.PROCESSING) {
                            isAnySubSystemStateProcessing = true;
                        }
                        else if(state == SystemState.UNKNOWN) {
                            isAnySubSystemStateUnknown = true;
                        }
                    }
                }
            }
            if(isAnySubSystemStateUnknown)
                m_rSystemState = SystemState.UNKNOWN;
            else if(isAnySubSystemStateFaulted)
                m_rSystemState = SystemState.FAULTED;
            else if(isAnySubSystemStateProcessing && !isAnySubSystemStateFaulted)
                m_rSystemState = SystemState.PROCESSING;
            else if(!isAnySubSystemStateFaulted && !isAnySubSystemStateProcessing && !isAnySubSystemStateUnknown)
                m_rSystemState = SystemState.IDLE;

            if(!isAtleastOneStateEvaluated)
                m_rSystemState = SystemState.UNKNOWN;
        }
        Log.d("MyMessage: ToolInfo state is"+m_ToolName+" ", m_rSystemState.toString());

    }

    @Override
    public void OnPostQueryExecute(String s) {

    }

    @Override
    public void OnPostQueryExecute(Map<String, String> map) {
        return;
    }
}
