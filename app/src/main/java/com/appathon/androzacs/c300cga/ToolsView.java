package com.appathon.androzacs.c300cga;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.net.Socket;
import java.util.Locale;
import java.util.Map;


public class ToolsView extends ActionBarActivity {

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link android.support.v4.app.FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link android.support.v4.view.ViewPager} that will host the section contents.
     */
    ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#FF2EAFFA")));
        setContentView(R.layout.activity_tools_view);

        int initialToolID = getIntent().getIntExtra(MainActivity.INITIAL_TOOL_ID, 0);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setCurrentItem(initialToolID);
        mViewPager.setOnPageChangeListener(
                new ViewPager.SimpleOnPageChangeListener() {
                    @Override
                    public void onPageSelected(int position) {
                        // When swiping between pages, select the
                        // corresponding tab.
                    }
                });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_tools_view, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        //int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        //if (id == R.id.action_about) {
          //  return true;
        //}

        return super.onOptionsItemSelected(item);
    }


    /**
     * A {@link android.support.v4.app.FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            PlaceholderFragment fm = PlaceholderFragment.newInstance(position + 1);
            return fm;
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return MainActivity.m_ToolMap.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();

            int count = 0;
            for(Map.Entry<Integer , ToolInfo> entry:MainActivity.m_ToolMap.entrySet()) {
                count++;
                if(count == position + 1)
                {
                    return entry.getValue().GetToolName();
                }
            }

//            //int count = 0;
//            for(Map.Entry<Integer , ToolInfo> entry : MainActivity.m_ToolMap.entrySet())
//            {
//                if(entry.getValue().m_ToolID == position + 1)
//                {
//                    return entry.getValue().GetToolName();
//                }
//                //count++;
//            }
            return null;
        }
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment implements ISocketAction{
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";
        private String[] m_toolStatus = {"AT.@_CHA" , "AT.@_CHB" ,"AT.@_CHC" ,"AT.@_CHD", "AT.@_Buffer" , "AT.@_LLA" ,"AT.@_LLB"};
        private int m_toolId;
        private EditText txt_query;
        private TextView txt_query_result;
        private Button btn_send_query;
        private Button btnToolStatus;
       // private Button btnToolConnectionStatus;
        private Button btnQueryInfo;
        private Socket m_socket;
        private ISocketAction m_interface;
        //private Map<String , String> m_toolStatusResult;
        private int timercount = 0;
        private Polygon m_poly1;

        private View m_View;

        //runs without a timer by reposting this handler at the end of the runnable
        Handler timerHandler = new Handler();
        Runnable timerRunnable = new Runnable() {

            @Override
            public void run() {
                UpdateToolState();
                timerHandler.postDelayed(this , 500);
            }
        };


        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);

            int count = 0;
            for(Map.Entry<Integer , ToolInfo> entry:MainActivity.m_ToolMap.entrySet()) {
                count++;
                if(count == sectionNumber)
                {
                    fragment.m_toolId = entry.getValue().m_ToolID;
                    break;
                }

            }
            return fragment;
        }

        public PlaceholderFragment()
        {
            m_interface = this;
        }

        private Map.Entry<Integer , ToolInfo> getToolEntryById(int toolId)
        {
            Map.Entry<Integer , ToolInfo> _entry = null;
            //int count = 1;
            for(Map.Entry<Integer , ToolInfo> entry : MainActivity.m_ToolMap.entrySet())
            {
                if(entry.getValue().m_ToolID == toolId)
                {
                    _entry = entry;
                    break;
                }
                //count++;
            }
            return _entry;
        }
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_tools_view, container, false);

            return rootView;
        }

        @Override
        public void onPause() {
            super.onPause();
            timerHandler.removeCallbacks(timerRunnable);
        }

        @Override
        public void onResume() {
            super.onResume();
            timerHandler.postDelayed(timerRunnable , 0);
        }

        @Override
        public void onDestroyView()
        {
            super.onDestroyView();
        }
        // Called once the parent Activity and the Fragment's UI have
        // been created.
        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            // Complete the Fragment initialization Ğ particularly anything
            // that requires the parent Activity to be initialized or the
            // Fragment's view to be fully inflated.

            m_View = getView();
            if(m_View != null)
            {
                m_View.setBackgroundColor(Color.parseColor("#FFD1EAEF"));
                m_poly1 = (Polygon)m_View.findViewById(R.id.poly1);
                if(m_poly1!=null && getActivity()!=null) {
                    m_poly1.setContext(getActivity());
                    m_poly1.SetToolID(m_toolId);
                }
                m_socket = getToolEntryById(m_toolId).getValue().m_Socket;
                timerHandler.postDelayed(timerRunnable , 0);

                btnToolStatus = (Button)m_View.findViewById(R.id.tool_status);
                btnToolStatus.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ShowToolSystemInfo();
                    }
                });

               // btnToolConnectionStatus = (Button)v.findViewById(R.id.btn_tool_connection_status);
                //btnToolConnectionStatus.setOnClickListener(new View.OnClickListener() {
                  //  @Override
                    //public void onClick(View v) {
                      //  ShowToolSystemConnectionInfo();
                   // }
                //});

                btnQueryInfo = (Button)m_View.findViewById(R.id.query_info);
                btnQueryInfo.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ShowQueryPopup();
                    }
                });
            }
        }
        public void ExecuteQuery()
        {
            String sExp = txt_query.getText().toString();
            if(m_socket != null)
            {
                if(!m_socket.isConnected())
                    txt_query_result.setText("Client socket not connected to server.");
                else
                {
                    QuerySocket querySocket = new QuerySocket(m_socket, sExp, this);
                    querySocket.execute();
                }
            }
        }

        private void ShowToolSystemInfo()
        {
            Context ctx = getActivity();
            if(ctx!=null) {

                AlertDialog.Builder popDialog = new AlertDialog.Builder(ctx);

                final LayoutInflater inflater = (LayoutInflater) ctx.getSystemService(ctx.LAYOUT_INFLATER_SERVICE);
                final View Viewlayout = inflater.inflate(R.layout.activity_tool_system_info,
                        (ViewGroup) getView().findViewById(R.id.layout_dialog_tool_system_info));

                popDialog.setIcon(R.drawable.checkboximage);
                popDialog.setTitle("Include / Exclude Subsystem");
                popDialog.setView(Viewlayout);

                CheckBox chkCHA = (CheckBox) Viewlayout.findViewById(R.id.chk_CHA);
                CheckBox chkCHB = (CheckBox) Viewlayout.findViewById(R.id.chk_CHB);
                CheckBox chkCHC = (CheckBox) Viewlayout.findViewById(R.id.chk_CHC);
                CheckBox chkCHD = (CheckBox) Viewlayout.findViewById(R.id.chk_CHD);
                CheckBox chkLLA = (CheckBox) Viewlayout.findViewById(R.id.chk_LLA);
                CheckBox chkLLB = (CheckBox) Viewlayout.findViewById(R.id.chk_LLB);
                CheckBox chkBuffer = (CheckBox) Viewlayout.findViewById(R.id.chk_Buffer);


                chkCHA.setChecked(MainActivity.m_ToolMap.get(m_toolId).GetSubSystem("CHA").getIsPartOfToolSystemInfo());
                chkCHB.setChecked(MainActivity.m_ToolMap.get(m_toolId).GetSubSystem("CHB").getIsPartOfToolSystemInfo());
                chkCHC.setChecked(MainActivity.m_ToolMap.get(m_toolId).GetSubSystem("CHC").getIsPartOfToolSystemInfo());
                chkCHD.setChecked(MainActivity.m_ToolMap.get(m_toolId).GetSubSystem("CHD").getIsPartOfToolSystemInfo());
                chkLLA.setChecked(MainActivity.m_ToolMap.get(m_toolId).GetSubSystem("LLA").getIsPartOfToolSystemInfo());
                chkLLB.setChecked(MainActivity.m_ToolMap.get(m_toolId).GetSubSystem("LLB").getIsPartOfToolSystemInfo());
                chkBuffer.setChecked(MainActivity.m_ToolMap.get(m_toolId).GetSubSystem("Buffer").getIsPartOfToolSystemInfo());

                chkCHA.setEnabled(MainActivity.m_ToolMap.get(m_toolId).GetSubSystem("CHA").IsConfigured());
                chkCHB.setEnabled(MainActivity.m_ToolMap.get(m_toolId).GetSubSystem("CHB").IsConfigured());
                chkCHC.setEnabled(MainActivity.m_ToolMap.get(m_toolId).GetSubSystem("CHC").IsConfigured());
                chkCHD.setEnabled(MainActivity.m_ToolMap.get(m_toolId).GetSubSystem("CHD").IsConfigured());
                chkLLA.setEnabled(MainActivity.m_ToolMap.get(m_toolId).GetSubSystem("LLA").IsConfigured());
                chkLLB.setEnabled(MainActivity.m_ToolMap.get(m_toolId).GetSubSystem("LLB").IsConfigured());
                chkBuffer.setEnabled(MainActivity.m_ToolMap.get(m_toolId).GetSubSystem("Buffer").IsConfigured());

                // Button OK
                popDialog.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();

                        CheckBox chkCHA = (CheckBox) Viewlayout.findViewById(R.id.chk_CHA);
                        CheckBox chkCHB = (CheckBox) Viewlayout.findViewById(R.id.chk_CHB);
                        CheckBox chkCHC = (CheckBox) Viewlayout.findViewById(R.id.chk_CHC);
                        CheckBox chkCHD = (CheckBox) Viewlayout.findViewById(R.id.chk_CHD);
                        CheckBox chkLLA = (CheckBox) Viewlayout.findViewById(R.id.chk_LLA);
                        CheckBox chkLLB = (CheckBox) Viewlayout.findViewById(R.id.chk_LLB);
                        CheckBox chkBuffer = (CheckBox) Viewlayout.findViewById(R.id.chk_Buffer);

                        MainActivity.m_ToolMap.get(m_toolId).GetSubSystem("CHA").setIsPartOfToolSystemInfo(chkCHA.isChecked());
                        MainActivity.m_ToolMap.get(m_toolId).GetSubSystem("CHB").setIsPartOfToolSystemInfo(chkCHB.isChecked());
                        MainActivity.m_ToolMap.get(m_toolId).GetSubSystem("CHC").setIsPartOfToolSystemInfo(chkCHC.isChecked());
                        MainActivity.m_ToolMap.get(m_toolId).GetSubSystem("CHD").setIsPartOfToolSystemInfo(chkCHD.isChecked());
                        MainActivity.m_ToolMap.get(m_toolId).GetSubSystem("LLA").setIsPartOfToolSystemInfo(chkLLA.isChecked());
                        MainActivity.m_ToolMap.get(m_toolId).GetSubSystem("LLB").setIsPartOfToolSystemInfo(chkLLB.isChecked());
                        MainActivity.m_ToolMap.get(m_toolId).GetSubSystem("Buffer").setIsPartOfToolSystemInfo(chkBuffer.isChecked());

						AppHelper.SaveToolsData(MainActivity.m_ToolMap , getActivity());
                        // txtUsername and Password (Dialog)
                        //EditText user = (EditText) Viewlayout.findViewById(R.id.txtUsername);
                        //EditText pass = (EditText) Viewlayout.findViewById(R.id.txtPassword);

                        // txtResult (Main Screen)
                        // TextView result = (TextView) findViewById(R.id.txtResult);
                        //result.setText(" Username : " + user.getText().toString() + " \n" +
                        //      " Password : " + pass.getText().toString() + " ");

                    }


                })

                        // Button Cancel
                        .setNegativeButton("Cancel",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                    }
                                });

                popDialog.create();
                popDialog.show();
            }
        }

        private void ShowQueryPopup()
        {
            Context ctx = getActivity();
            if(ctx!=null) {

                AlertDialog.Builder popDialog = new AlertDialog.Builder(ctx);

                final LayoutInflater inflater = (LayoutInflater) ctx.getSystemService(ctx.LAYOUT_INFLATER_SERVICE);
                final View Viewlayout = inflater.inflate(R.layout.activity_query_dialog,
                        (ViewGroup) getView().findViewById(R.id.layout_query_tool));

                txt_query = (EditText) Viewlayout.findViewById(R.id.txt_query);
                txt_query_result = (TextView) Viewlayout.findViewById(R.id.txt_query_result);
                btn_send_query = (Button)Viewlayout.findViewById(R.id.btn_send_query);
                btn_send_query.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        txt_query_result.setText("Checking...");
                        ExecuteQuery();
                    }
                });

                popDialog.setIcon(R.drawable.querytool);
                popDialog.setTitle("Query Tool Attribute");
                popDialog.setView(Viewlayout);
                popDialog.setCancelable(true);

                popDialog.setPositiveButton("Close", null);

                popDialog.create();
                popDialog.show();
            }
        }
        @Override
        public void OnPostQueryExecute(String sOutput)
        {
            if(txt_query_result!=null)
                txt_query_result.setText(sOutput);

        }
        @Override
        public void OnPostQueryExecute(Map<String , String> map)
        {
        }

        private void UpdateToolState()
        {
            ToolInfo tInfo = MainActivity.m_ToolMap.get(m_toolId);

            if(tInfo!=null)
            {
                ToolInfo.SystemState state = tInfo.GetSystemState();
                btnToolStatus.setText(state.toString());

                if(state == ToolInfo.SystemState.IDLE) {
                    btnToolStatus.setBackgroundColor(Color.BLUE);
                }
                else if(state == ToolInfo.SystemState.FAULTED)
                {
                    btnToolStatus.setBackgroundColor(Color.RED);
                }
                else if(state == ToolInfo.SystemState.PROCESSING)
                {
                    btnToolStatus.setBackgroundColor(Color.GREEN);
                }
                else if(state == ToolInfo.SystemState.UNKNOWN)
                {
                    btnToolStatus.setBackgroundColor(Color.GRAY);
                }
            }
            //View v = getView();
            if(m_View!=null)
                m_View.requestLayout();
            m_poly1.invalidate();
        }
    }

}
