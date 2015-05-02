package com.appathon.androzacs.c300cga;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends Activity implements ISocketAction , ISocketConnection{

    static Map<Integer , ToolInfo> m_ToolMap;
    static int m_reqCode;
    private final String TOOL_ID = "tool_instance";
    public final static String INITIAL_TOOL_ID = "INITIAL_TOOL_ID";
    Map<Integer , String> m_savedToolsData;
    int m_savedToolsId;
    private static final int NEW_TOOL_ALERT = 1;
    private static final String TOOL_TILE = "tool_tile_";
    private TextView tv_noTool;

    private static final String PREF_FIRSTLAUNCH_HELP = "helpDisplayed";
    private boolean helpDisplayed = false;

    Handler tileImagetimerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {

        @Override
        public void run() {
            updateToolTileImage();
            tileImagetimerHandler.postDelayed(this , 500);
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        ActionBar actionBar = getActionBar();
        actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#FF2EAFFA")));

        setContentView(R.layout.activity_main);

        m_ToolMap = new LinkedHashMap<>();
        m_savedToolsData = new HashMap<>();
        m_reqCode = 0;

        tv_noTool = (TextView) findViewById(R.id.tv_noTool);
        tv_noTool.setVisibility(View.INVISIBLE);

        showHelpForFirstLaunch();
    }

    private void showHelpForFirstLaunch() {
        if (helpDisplayed)
            return;
        helpDisplayed = getPreferenceValue(PREF_FIRSTLAUNCH_HELP, false);
        if (!helpDisplayed) {
            savePreference(PREF_FIRSTLAUNCH_HELP, true);
            showHelp();
        }
    }

    private boolean getPreferenceValue(String key, boolean defaultValue) {
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        return preferences.getBoolean(key, defaultValue);
    }

    private void savePreference(String key, boolean value) {
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(key, value);
        editor.commit();
    }

    //Summy
    @Override
    public void onStart()
    {
        super.onStart();
        if(m_ToolMap.size() == 0)
        {
            Map<Integer , String> toolsMap = AppHelper.getToolsData(this);
            if (toolsMap.size() != 0)
            {
                m_savedToolsData = toolsMap;
                addStartupTools();
            }
            else
            {
                setNoToolInfo();
                //AppHelper.ShowAppMessage("No tools are present , Add a new tool !!", this);
            }
        }
        tileImagetimerHandler.postDelayed(timerRunnable , 0);
    }

    private void setNoToolInfo()
    {
        tv_noTool.setVisibility(View.VISIBLE);
        tv_noTool.setText("No tools are present, please add a tool.");
    }

    private void addStartupTools()
    {
        int id = m_ToolMap.size() + 1;

        if(id <=  AppHelper.MAX_TOOLS  && m_savedToolsData.containsKey(id))
        {
            m_savedToolsData.get(id);
            String[] toolData = m_savedToolsData.get(id).split("##");
            AppHelper.CreateSocketConnection(toolData[1], Integer.parseInt(toolData[2]), toolData[0] , this);
        }
        else
        {
//            AppHelper.ShowAppMessage("Internal :Show all tools on this screen !!", this);
            CreateToolsTiles();
        }
    }

    private void CreateToolsTiles()
    {

        GridLayout gridLayout = (GridLayout) findViewById(R.id.tile_grid);
        gridLayout.removeAllViews();

        int count = 0;
        for(Map.Entry<Integer , ToolInfo> entry: m_ToolMap.entrySet())
        {
            count++;
            int tool_id = entry.getKey();
            String tool_name = entry.getValue().GetToolName();
            ToolInfo.SystemState tool_state =  entry.getValue().GetSystemState();
            DrawToolTile(tool_name, tool_id , count, tool_state , entry.getValue().m_ToolIP , String.valueOf(entry.getValue().m_Port));
        }
        if(count == 0)
        {
            setNoToolInfo();
        }
    }

    private void DrawToolTile(final String name , int id , final int index, ToolInfo.SystemState state, String ip , String port)
    {
        int row = (index - 1) / 2 ;
        int column = index%2 ;
        column = column ^ 1;
//        int resId = 0;
//        if(state == ToolInfo.SystemState.FAULTED)
//            resId = R.drawable.tile4;
//        else if(state == ToolInfo.SystemState.PROCESSING)
//            resId = R.drawable.tile3;
//        else
//            resId = R.drawable.tile5;

        GridLayout gridLayout = (GridLayout) findViewById(R.id.tile_grid);
        //create new item layout
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.griditem_tool, gridLayout, false);
        TextView tool_name = (TextView) v.findViewById(R.id.tool_name);
        Button btn_tool_img = (Button) v.findViewById(R.id.btn_tool_img);
        //String selectedTime = "some new value";
        tool_name.setText(name);
//        btn_tool_img.setText(name);
//        btn_tool_img.setTextSize(0);

        btn_tool_img.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this , ToolsView.class);
                intent.putExtra(MainActivity.INITIAL_TOOL_ID, index-1);
                startActivity(intent);
                // Do something in response to button click
            }
        });

        //GridLayout gv = (GridLayout) findViewById(R.id.tile_grid);
        //Button tv = new Button(this);
        btn_tool_img.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER);
        btn_tool_img.setId(getResIdByToolId(id));
        setToolTileId(btn_tool_img , state);
        btn_tool_img.setText(ip + "\nPort::" + port);
        //tv.setText("Tool " + String.valueOf(id) + "\n\n" + name);
//        btn_tool_img.setBackgroundResource(resId);
        GridLayout.LayoutParams params = new GridLayout.LayoutParams(GridLayout.spec(row) , GridLayout.spec(column));
        v.setLayoutParams(params);
        gridLayout.addView(v);
        registerForContextMenu(btn_tool_img);
    }

    private void updateToolTileImage()
    {
        System.out.println("MyLog: UpdateToolImage called\n");
        for(Map.Entry<Integer , ToolInfo> entry : m_ToolMap.entrySet())
        {
            System.out.println("MyLog: Tool Entry Found\n");
            Button tool_tile = (Button)findViewById(getResIdByToolId(entry.getKey()));
            if(tool_tile != null) {
                System.out.println("MyLog: "+entry.getValue().GetToolName() + " , State :"+entry.getValue().GetSystemState().toString() + " \n");
                setToolTileId(tool_tile, entry.getValue().GetSystemState());
            }
        }
    }

    private void setToolTileId(Button btn , ToolInfo.SystemState state)
    {
        int resIdFaulted = R.drawable.tile4;
        int resIdProcessing = R.drawable.tile7;
        int resIdIdle = R.drawable.tile5;
        int resIdUnknown = R.drawable.tile6;

        if(state == ToolInfo.SystemState.FAULTED)
            btn.setBackgroundResource(resIdFaulted);
        else if(state == ToolInfo.SystemState.PROCESSING)
            btn.setBackgroundResource(resIdProcessing);
        else if(state == ToolInfo.SystemState.IDLE)
            btn.setBackgroundResource(resIdIdle);
        else
            btn.setBackgroundResource(resIdUnknown);
    }

    private int getResIdByToolId(int toolId)
    {
        int resId = 0;
        switch(toolId)
        {
            case 1: resId = R.id.tool_tile_1; break;
            case 2: resId = R.id.tool_tile_2; break;
            case 3: resId = R.id.tool_tile_3; break;
            case 4: resId = R.id.tool_tile_4; break;
        }
        return resId;
    }

    private int getToolIdByResId(int resId)
    {
        int toolId = 0;
        switch(resId)
        {
            case R.id.tool_tile_1: toolId = 1; break;
            case R.id.tool_tile_2: toolId = 2; break;
            case R.id.tool_tile_3: toolId = 3; break;
            case R.id.tool_tile_4: toolId = 4; break;
        }
        return toolId;
    }

    // here you create de conext menu
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {



        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        //View view = info.targetView;
        Button txtView = (Button)v;
        menu.setHeaderTitle(txtView.getText());

        menu.add(Menu.NONE, txtView.getId(), Menu.NONE, "Delete");

    }

    // This is executed when the user select an option
    @Override
    public boolean onContextItemSelected(MenuItem item) {

        int i = item.getItemId();
        int toolId = getToolIdByResId(i);

        if(m_ToolMap.containsKey(toolId))
        {
            m_ToolMap.get(toolId).DeleteTool();
            m_ToolMap.remove(toolId);

            Map<Integer, ToolInfo> map_temp = new LinkedHashMap<>(m_ToolMap);


            //Update all tool id's again
            int count = 0;
            for(Map.Entry<Integer , ToolInfo> entry: map_temp.entrySet())
            {
                count++;
                if(count < toolId)
                    continue;
                else
                {
                    m_ToolMap.put(count, m_ToolMap.get(entry.getKey()));
                    m_ToolMap.get(count).m_ToolID = count;
                    Button tool_tile = (Button)findViewById(getResIdByToolId(entry.getKey()));
                    tool_tile.setId(getResIdByToolId(count));
                    m_ToolMap.remove(entry.getKey());
                }
            }

            AppHelper.SaveToolsData(m_ToolMap, this);
            CreateToolsTiles();

        }

        //AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        return super.onContextItemSelected(item);

    }

    @Override
    public void OnSocketConnect(ConnectionInfo cnf)
    {
        Socket socket = SingletonSocket.getSocket();
        m_ToolMap.put(m_ToolMap.size() + 1, new ToolInfo(socket, SingletonSocket.getSocketName(), m_ToolMap.size() + 1));
        String toolPref =  m_savedToolsData.get(m_ToolMap.size() + 10);
        m_ToolMap.get(m_ToolMap.size()).setToolPreference(toolPref);
        if(!cnf.RESPONSE.isEmpty())
        {
            ToolInfo tlInfo = m_ToolMap.get(m_ToolMap.size());
            if(tlInfo!=null)
            {
                tlInfo.m_Port = cnf.PORT;
                tlInfo.m_ToolIP = cnf.IP_ADDR;
            }
        }
        addStartupTools();
    }

    @Override
    public void onRestart()
    {
        super.onRestart();
    }

    @Override
    public void onResume()
    {
        super.onResume();
       // tileImagetimerHandler.postDelayed(timerRunnable , 0);
        //addStartupTools();
    }
    @Override
    public void onPause()
    {
        super.onPause();
        //tileImagetimerHandler.removeCallbacks(timerRunnable);
    }
    @Override
    public void onStop()
    {
        super.onStop();
    }
    @Override
    public void onDestroy()
    {
        super.onDestroy();
    }
    public void buttonAddToolListner(View v)
    {
        if(m_ToolMap.size() == AppHelper.MAX_TOOLS)
        {
            showDialog(NEW_TOOL_ALERT);
        }
        else
        {
            Intent intent = new Intent(MainActivity.this, ClientSocket.class);
            startActivityForResult(intent, m_reqCode);
        }
    }

    public void buttonViewToolsListner(View v)
    {
        if(m_ToolMap.size() == 0)
        {
            AppHelper.ShowAppMessage("Tools are offline or no tools added yet :(" , this);
            return;
        }
        Intent intent = new Intent(MainActivity.this , ToolsView.class);
        intent.putExtra(MainActivity.INITIAL_TOOL_ID, 0); //RK:Change here
        startActivity(intent);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        AlertDialog dialog = null;
        switch (id)
        {
            case NEW_TOOL_ALERT:
            {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("Maximum of 4 Tools can be added. Please delete any existing tool and try again.");
                //builder.setCancelable(true);
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
                //builder.setNegativeButton("No, no", new CancelOnClickListener());
                dialog = builder.create();
                return dialog;
            }
        }
        return super.onCreateDialog(id);
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        intent.putExtra("REQUEST_CODE", requestCode);
        m_reqCode += 1;
        super.startActivityForResult(intent, requestCode);
    }

    // Call Back method  to get the Message form other Activity
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        // check if the request code is same as what is passed  here it is 2
        if(requestCode == m_reqCode - 1 && data != null)
        {
            Socket socket = SingletonSocket.getSocket();
            if(AppHelper.IsSocketConnectionAlive( socket))
            {
                AppHelper.ShowAppMessage("New tool added successfully !!" , this);
                m_ToolMap.put(m_ToolMap.size() + 1, new ToolInfo(socket, SingletonSocket.getSocketName(), m_ToolMap.size() + 1));
                DrawToolTile(m_ToolMap.get(m_ToolMap.size()).GetToolName() , m_ToolMap.size() , m_ToolMap.size(), m_ToolMap.get(m_ToolMap.size()).GetSystemState()
                        , m_ToolMap.get(m_ToolMap.size()).m_ToolIP , String.valueOf(m_ToolMap.get(m_ToolMap.size()).m_Port));
                AppHelper.SaveToolsData(m_ToolMap, this);
                tv_noTool.setVisibility(View.INVISIBLE);
            }
        }
    }

    @Override
    public void OnPostQueryExecute(String sOutput)
    {
        //ShowAppMessage(sOutput);
    }

    @Override
    public void OnPostQueryExecute(Map<String , String> map)
    {
        //ShowAppMessage(sOutput);
    }

    private void CloseSocket(Socket socket)
    {
        try {
            socket.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_new_tool, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_about) {
            Intent intent = new Intent(MainActivity.this , AboutActivity.class);
            startActivity(intent);
            return true;
        }
        else if (id == R.id.action_help) {
            showHelp();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
	public void saveToolsPreferences()
    {
        AppHelper.SaveToolsData(m_ToolMap , this);
    }

    private void showHelp() {
        final View instructionsContainer = findViewById(R.id.container_help);
        instructionsContainer.setVisibility(View.VISIBLE);
        instructionsContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                instructionsContainer.setVisibility(View.INVISIBLE);
            }
        });
    }
}


