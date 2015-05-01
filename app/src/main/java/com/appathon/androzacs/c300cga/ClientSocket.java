package com.appathon.androzacs.c300cga;
//
//import android.support.v7.app.ActionBarActivity;
//import android.os.Bundle;
//import android.view.Menu;
//import android.view.MenuItem;
//
//

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.Map;

import android.app.ActionBar;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class ClientSocket extends Activity implements ISocketConnection{

    TextView textResponse;
    EditText editTextAddress, editTextPort , editToolName;
    Button buttonConnect, buttonClear;
    Socket m_socket = null ;
    ISocketConnection m_iSocket;
    int m_requestCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#FF2EAFFA")));

        setContentView(R.layout.activity_client_socket);

        m_iSocket = this;
        editTextAddress = (EditText)findViewById(R.id.address);
        editTextPort = (EditText)findViewById(R.id.port);
        editToolName = (EditText)findViewById(R.id.toolname);
        buttonConnect = (Button)findViewById(R.id.connect);
        buttonClear = (Button)findViewById(R.id.clear);
        textResponse = (TextView)findViewById(R.id.response);

        buttonConnect.setOnClickListener(buttonConnectOnClickListener);
        buttonClear.setVisibility(View.INVISIBLE);
        buttonClear.setOnClickListener(new OnClickListener(){

            @Override
            public void onClick(View v)
            {
                textResponse.setText("");
                buttonClear.setVisibility(View.INVISIBLE);
            }});
    }

    OnClickListener buttonConnectOnClickListener =
            new OnClickListener(){

                @Override
                public void onClick(View arg0) {
                    String ip = editTextAddress.getText().toString();
                    String port = editTextPort.getText().toString();
                    String name = editToolName.getText().toString();

                    // Error handling
                    if(IsInputDataCorrect())
                    {
                        AppHelper.CreateSocketConnection(ip , Integer.parseInt(port) ,name ,  m_iSocket);
                    }
                }};
    @Override
    public void OnSocketConnect(ConnectionInfo cnf)
    {
        textResponse.setText(cnf.RESPONSE);
        if(cnf.RESPONSE.isEmpty())
        {
            Intent intent = new Intent();
            intent.putExtra("tool_name", editToolName.getText().toString());
            setResult(0, intent);
            finish();
        }
        else
        {
            buttonClear.setVisibility(View.VISIBLE);
        }
    }

    private boolean IsInputDataCorrect()
    {
        String sErr1 = "Invalid IP Address !!";
        String sErr2 = "Please provide correct port no. of tool service !!";
        String sErr3 = "Please provide a unique tool name!!";

        String sErr = "";
        String ip = editTextAddress.getText().toString();
        String port = editTextPort.getText().toString();
        String name = editToolName.getText().toString();


        String[] ipChucnks = ip.split("\\.");
        int length = ipChucnks.length;

        if (length != 4)
            sErr = sErr1;
        else
        {
            for(int i = 0 ; i < length ; i++)
            {
                if(ipChucnks[i].length() > 3 || AppHelper.isParsableToInt(ipChucnks[i]) == false)
                    sErr = sErr1;
            }
        }

        if(sErr.isEmpty())
        {
            if (port.isEmpty() || AppHelper.isParsableToInt(port) == false)
                sErr = sErr2;
            else if (name.isEmpty())
                sErr = sErr3;
        }


        if(sErr.isEmpty())
        {
            boolean bUniqueName = true;
            for(ToolInfo toolinfo : MainActivity.m_ToolMap.values())
            {
                if(name.equals(toolinfo.GetToolName()))
                {
                    bUniqueName = false;
                    break;
                }
            }
            if(!bUniqueName) {
                sErr = sErr3;
            }
        }

        if(!sErr.isEmpty())
        {
            AppHelper.ShowErrorMessage(sErr , this);
            return false;
        }
        return true;
    }
}
