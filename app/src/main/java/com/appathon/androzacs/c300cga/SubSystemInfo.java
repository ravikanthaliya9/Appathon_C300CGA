package com.appathon.androzacs.c300cga;

import android.os.Handler;
import android.util.Log;

import java.net.Socket;
import java.util.Map;


/**
 * Created by RKanthaliya116069 on 3/31/2015.
 */
public class SubSystemInfo implements ISocketAction{

    private Socket m_Socket;
    private ToolInfo.SystemState m_rSystemState;
    private String m_SubSystemName;
    private String m_SubSystemType;
    private String m_SubSystemWaferState;
    private String m_SubSystemCurrentRecipe;
    private String m_SubSystemCurrentPressure;

    private boolean m_bIsConfigured = true;

    private ISocketAction m_interface;

    public ToolInfo.SystemState GetSystemState()
    {
        return m_rSystemState;
    }

    public String GetSubsystemName()
    {
        return m_SubSystemName;
    }

    public String GetSubsystemType()
    {
        return m_SubSystemType;
    }

    private String[] m_arQueriesStrings;
    private Map<String , String> m_queryResults;

    private String m_RootName = "AT";
    private String m_QUERY_STR_CHNAME;
    private String m_QUERY_STR_CHState;
    private String m_QUERY_STR_CH_Wafer_State;
    private String m_QUERY_STR_CH_Pressure;
    private String m_QUERY_STR_CH_Current_Recipe;

    private boolean m_IsPartofToolSystemInfo;
    public SubSystemInfo(Socket socket, String name)
    {
        m_IsPartofToolSystemInfo = true;
        m_rSystemState = ToolInfo.SystemState.UNKNOWN;
        m_interface = this;

        m_Socket = socket;

        m_SubSystemName = name;

        m_QUERY_STR_CHNAME = m_RootName + ".@_" + m_SubSystemName;

        m_QUERY_STR_CHState = m_RootName + "/" + m_SubSystemName + ".rState";

        m_QUERY_STR_CH_Wafer_State = m_RootName + "/" + m_SubSystemName + ".@HasWaferInAnySlot";

        m_QUERY_STR_CH_Pressure = m_RootName + "/" + m_SubSystemName + "/VacSys/PressGauge.rPressure";

        m_QUERY_STR_CH_Current_Recipe = m_RootName + "/" + m_SubSystemName + ".@RecipeName01";

        m_arQueriesStrings = new String[] {m_QUERY_STR_CHNAME, m_QUERY_STR_CHState, m_QUERY_STR_CH_Wafer_State,
                m_QUERY_STR_CH_Pressure,  m_QUERY_STR_CH_Current_Recipe};

        timerHandler.postDelayed(timerRunnable , 0);
    }

    public String getSubSystemWaferState()
    {
        return m_SubSystemWaferState;
    }

    public boolean hasWaferPresentInAnySlot()
    {
        return m_SubSystemWaferState == "Present" ? true : false;
    }

    public String getSubSystemChPressure()
    {
        return m_SubSystemCurrentPressure;
    }

    public String getSubSystemCurrentRecipe()
    {
        return m_SubSystemCurrentRecipe;
    }

    public boolean getIsPartOfToolSystemInfo()
    {
        return m_IsPartofToolSystemInfo && m_bIsConfigured;
    }

    public void setIsPartOfToolSystemInfo(boolean value)
    {
        m_IsPartofToolSystemInfo = value;
    }

    public void DeleteSubSystem()
    {
        timerHandler.removeCallbacks(timerRunnable);
    }

    public boolean IsConfigured()
    {
        return m_bIsConfigured;
    }

    //Dummy Timer to update tool connection status and tool state
    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {

        @Override
        public void run() {
            if(m_Socket != null)
            {
                System.out.println("Socket is not null: "+m_SubSystemName+"\n");
                if( !m_Socket.isConnected()) {
                    System.out.println("Socket is not connected: "+m_SubSystemName+"\n");
                    updateSubSytemState();
                }
                else
                {
                    System.out.println("Socket is connected: " + m_SubSystemName + "\n");
                    QuerySocket querySoc = new QuerySocket(m_Socket, m_arQueriesStrings, m_interface);
                    querySoc.execute();
                }
            }
            timerHandler.postDelayed(this , 1500);
        }
    };

    private void updateSubSytemState()
    {
        m_rSystemState = ToolInfo.SystemState.UNKNOWN;
    }
    @Override
    public void OnPostQueryExecute(String s) {

    }

    @Override
    public void OnPostQueryExecute(Map<String, String> map) {

        m_queryResults = map;
        String key = "";
        String value = "";

        for(Map.Entry<String , String> entry : m_queryResults.entrySet())
        {
            key = entry.getKey();
            value = entry.getValue();

            if(key == m_QUERY_STR_CHNAME)
            {
                if(value.startsWith("0\t")) {

                    value = value.substring(2);

                    if(value.toLowerCase().equals("not configured"))
                    {
                        m_bIsConfigured = false;
                        m_SubSystemType = "Not Configured";
                        Log.d("MyMessage:", "SubSystemInfo: " + m_SubSystemName + " is not configured.");
                    }
                    else {
                        m_bIsConfigured = true;
                        m_SubSystemType = value;

                        Log.d("MyMessage:", "SubSystemInfo: " + m_SubSystemName + " " + m_SubSystemType);
                    }
                }
                else //Query Failed. (Attribute is not registered)
                {
                    m_bIsConfigured = true; //RK:Check again
                    m_rSystemState = ToolInfo.SystemState.UNKNOWN;
                    m_SubSystemType = "Unknown";
                    Log.d("MyMessage:","Query Failed for " + m_SubSystemName + ": <key = " + key + ", value = " + value + ">");
                }

            }
            else if(key == m_QUERY_STR_CHState)
            {
                //Change State Here
                if(value.startsWith("0\t")) {
                    Log.d("MyMessage: SubSystemInfo " + m_SubSystemName + " ", value);
                    value = value.substring(2);
                    if(value.equals("Fault"))
                        m_rSystemState = ToolInfo.SystemState.FAULTED;
                    else if(value.equals("Busy"))
                        m_rSystemState = ToolInfo.SystemState.PROCESSING;
                    else if(value.equals("Ready"))
                        m_rSystemState = ToolInfo.SystemState.IDLE;
                }
                else
                {
                    //RK:Check Again: Attribute not registered.
                    m_rSystemState = ToolInfo.SystemState.UNKNOWN;

                    Log.d("MyMessage:","Query Failed for " + m_SubSystemName + ": <key = " + key + ", value = " + value + ">");
                }

            }
            else if(key == m_QUERY_STR_CH_Wafer_State)
            {
                //Change State Here
                if(value.startsWith("0\t")) {
                    Log.d("MyMessage: SubSystemInfo " + m_SubSystemName + " ", value);
                    value = value.substring(2);
                    if(value.equals("1"))
                        m_SubSystemWaferState = "Present";
                    else
                        m_SubSystemWaferState = "Absent";
                }
                else
                {
                    //RK:Check Again: Attribute not registered.
                    m_SubSystemWaferState = "Absent";

                    Log.d("MyMessage:","Query Failed for " + m_SubSystemName + ": <key = " + key + ", value = " + value + ">");
                }
            }
            else if(key == m_QUERY_STR_CH_Pressure)
            {
                //Change State Here
                if(value.startsWith("0\t")) {
                    Log.d("MyMessage: SubSystemInfo " + m_SubSystemName + " ", value);
                    value = value.substring(2);

                    m_SubSystemCurrentPressure = value + " Torr";
                }
                else
                {
                    //RK:Check Again: Attribute not registered.
                    m_SubSystemCurrentPressure = "Not Registered";

                    Log.d("MyMessage:","Query Failed for " + m_SubSystemName + ": <key = " + key + ", value = " + value + ">");
                }
            }
            else if(key == m_QUERY_STR_CH_Current_Recipe)
            {
                //Change State Here
                if(value.startsWith("0\t")) {
                    Log.d("MyMessage: SubSystemInfo " + m_SubSystemName + " ", value);
                    value = value.substring(2);

                    m_SubSystemCurrentRecipe = value;
                }
                else
                {
                    //RK:Check Again: Attribute not registered.
                    m_SubSystemCurrentRecipe = "Not registered";

                    Log.d("MyMessage:","Query Failed for " + m_SubSystemName + ": <key = " + key + ", value = " + value + ">");
                }
            }
        }
    }
}














