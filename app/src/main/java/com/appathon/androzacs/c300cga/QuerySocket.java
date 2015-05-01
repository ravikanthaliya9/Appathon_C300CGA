package com.appathon.androzacs.c300cga;

import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

interface ISocketAction
{
    void OnPostQueryExecute(String s);
    void OnPostQueryExecute(Map<String, String> map);
}

enum QueryType{NONE , SINGLE_QUERY , MUlTIPLE_QUERY};

public class QuerySocket extends AsyncTask<Void, Void, Void> {

    String m_query = "";
    String response = "";
    Map<String , String> m_response;
    Socket m_socket;
    ISocketAction m_owner;
    String[] m_queries;
    QueryType m_queryType =   QueryType.NONE;

    QuerySocket(Socket soc, String query , ISocketAction owner) {
        m_socket = soc;
        m_query = query;
        m_owner = owner;
        m_queryType = QueryType.SINGLE_QUERY;
    }

    QuerySocket(Socket soc, String[] queryMap , ISocketAction owner) {
        m_socket = soc;
        m_queries = queryMap;
        m_owner = owner;
        m_response = new HashMap<String , String>();
        m_queryType = QueryType.MUlTIPLE_QUERY;
    }

    @Override
    protected Void doInBackground(Void... arg0) {
        try
        {
            if (!m_socket.isClosed() && m_socket.isConnected() && !m_socket.isInputShutdown() && !m_socket.isOutputShutdown())
            {
                OutputStream outServer = m_socket.getOutputStream();
                DataOutputStream outStream = new DataOutputStream(outServer);

                InputStream inputStream1 = m_socket.getInputStream();
                BufferedReader r = new BufferedReader(new InputStreamReader(inputStream1));

                if(m_queryType == QueryType.SINGLE_QUERY)
                {
                    response = RunQuery(outStream , r , m_query);
                }
                else if(m_queryType == QueryType.MUlTIPLE_QUERY)
                {
                    for(String query : m_queries)
                    {
                        String soutput = RunQuery(outStream , r , query);
                        m_response.put(query , soutput);
                    }
                }
            }

        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            response = "UnknownHostException: " + e.toString();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            response = "IOException: " + e.toString();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            response = "Exception: " + e.toString();
        }
        return null;
    }

    private String RunQuery(DataOutputStream outStream , BufferedReader r , String query)
    {
        String output = "";
        try
        {
            outStream.write(new String("1\t" + query + "\n").getBytes());
            String slog = "\n Just sent the registration command :1\t" + query + "\n";
            System.out.println(slog);

            String line = r.readLine();

            if (line != null) {
                output = line;
            }
        }
        catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            output = "UnknownHostException: " + e.toString();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
           output = "IOException: " + e.toString();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            output = "Exception: " + e.toString();
        }
        return output;
    }

    @Override
    protected void onPostExecute(Void result) {
        super.onPostExecute(result);

        if(m_queryType == QueryType.SINGLE_QUERY)
            m_owner.OnPostQueryExecute(response);
        else if(m_queryType == QueryType.MUlTIPLE_QUERY)
            m_owner.OnPostQueryExecute(m_response);
    }

}
