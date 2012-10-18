package com.android.btonoff;

//import java.lang.Exception;
import android.app.Activity;
import android.os.Message;
import android.os.Handler;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;
import android.widget.TextView;
import android.text.method.ScrollingMovementMethod;
import android.bluetooth.BluetoothAdapter;

public class BtOnOff extends Activity {

    private static final String LOG_TAG = "BtOnOff";//for use as the tag when logging
    private static final int MAX_TEST_CYCLE = 1;
    private TextView showRunMsg;
    private BluetoothAdapter mBtAdapter;
    private boolean mTermOnOffThread = true;
    private boolean mOnOffThreadState = false;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        showRunMsg = (TextView) findViewById(R.id.showRunMsg);
        showRunMsg.setMovementMethod(new ScrollingMovementMethod());

        Button buttonStart = (Button)findViewById(R.id.buttonStart);       
        buttonStart.setOnClickListener(startListener); // Register the onClick listener with the implementation above

        Button buttonStop = (Button)findViewById(R.id.buttonStop);       
        buttonStop.setOnClickListener(stopListener); // Register the onClick listener with the implementation above
        //buttonStart.setEnabled(true);
        //buttonStop.setEnabled(false);

        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBtAdapter == null) {
            showRunMsg.setText("Fail to getDefaultAdapter");
        } else {
            showRunMsg.setText("Initial bluetooth state: " + (mBtAdapter.isEnabled()?"on":"off"));
        }

    }

    //Create an anonymous implementation of OnClickListener
    private OnClickListener startListener = new OnClickListener() {
        public void onClick(View v) {
            //sendLog(".\r\n.\r\n.\r\n.\r\n.\r\n.\r\n.\r\n.\r\n.\r\n.\r\n.\r\n.\r\n.\r\n.\r\n.\r\n.\r\n.\r\n.\r\n.\r\n.\r\n.\r\n.\r\n.\r\n.\r\n.\r\n.\r\n.\r\n.\r\n.\r\n.\r\n.\r\n.\r\n");
            sendLog("======= onClick() called - start button");             
            if(mOnOffThreadState == false) {
                if(mBtAdapter !=  null) {
                    mOnOffThreadState = true;
                    mTermOnOffThread = false;
                    mOnOffThread = new OnOffThread(MAX_TEST_CYCLE);
                    mOnOffThread.start();
                }
            } else {
                sendLog("OnOffThread already started");
            }
        }
    };

    // Create an anonymous implementation of OnClickListener
    private OnClickListener stopListener = new OnClickListener() {
        public void onClick(View v) {
            sendLog("======= onClick() called - stop button");
            if(mOnOffThreadState == true && mTermOnOffThread == false) {
                if(mBtAdapter !=  null) {
                    mTermOnOffThread = true;
                    try {
                        mOnOffThread.join();
                    } catch(InterruptedException e) {
                        //null;
                    }
                    mOnOffThreadState = false;
                }
            } else {
                sendLog("OnOffThread not started or already terminated");
            }
        }
    };


    @Override
    protected void onStart() {//activity is started and visible to the user
        Log.d(LOG_TAG,"onStart() called");
        super.onStart(); 
    }
    @Override
    protected void onResume() {//activity was resumed and is visible again
        Log.d(LOG_TAG,"onResume() called");
        super.onResume();

    }
    @Override
    protected void onPause() { //device goes to sleep or another activity appears
        Log.d(LOG_TAG,"onPause() called");//another activity is currently running (or user has pressed Home)
        super.onPause();

    }
    @Override
    protected void onStop() { //the activity is not visible anymore
        Log.d(LOG_TAG,"onStop() called");
        super.onStop();

    }
    @Override
    protected void onDestroy() {//android has killed this activity
        Log.d(LOG_TAG,"onDestroy() called");
        super.onDestroy();
    }

    final Handler handler = new Handler() {
        private String allMsgs = "";
        public void handleMessage(Message msg) {
            String stateMsg = (String)msg.obj;
            allMsgs += stateMsg + "\r\n";
            showRunMsg.setText(allMsgs);
        }
    };

    protected void sendLog(String strMsg) {
        Message msg = handler.obtainMessage();
        msg.obj = strMsg;
        handler.sendMessage(msg);
        Log.i(LOG_TAG, strMsg);
    }


    private OnOffThread mOnOffThread;

    private class OnOffThread extends Thread {
        private int mMaxCycle;
        private int mCurCycle;
        private boolean mBtPwrState;
        Thread threadToInterrupt;
        OnOffThread(int maxCycle) {
            mMaxCycle = maxCycle;
            mCurCycle = 0;
            mBtPwrState = false;
            threadToInterrupt = Thread.currentThread();
        }

        public void run() {
            boolean btAdapterState = mBtAdapter.isEnabled();
            for( ; 
                    mTermOnOffThread == false &&  mCurCycle < 2*mMaxCycle;
                    ++mCurCycle) 
            {
                mBtPwrState = btAdapterState;
                boolean nextBtPwrState = !mBtPwrState;

                String stateMsg = "Cycle: " + mCurCycle/2 + " of " + mMaxCycle
                    + " , State: -->" + (nextBtPwrState?"on":"off");
                Log.i(LOG_TAG, "++++++++++++++++++++++++++++++++++++++++++++++++++++++");
                sendLog(stateMsg);


                if(nextBtPwrState) mBtAdapter.enable();
                else mBtAdapter.disable();
                mBtPwrState = !mBtPwrState;
                try {
                    Thread.sleep(10000);
                } catch(InterruptedException e) {
                    //null;
                }

                while((btAdapterState = mBtAdapter.isEnabled())
                        != nextBtPwrState ) {
                    String waitStateMsg = "cur state:" 
                        + (btAdapterState?"on":"off")
                        + ", wait for bt state to change to " 
                        + (mBtPwrState?"on":"off");
                    sendLog(waitStateMsg);
                    try {
                        Thread.sleep(1000);
                    } catch(InterruptedException e) {
                        //null;
                    }
                }
            }
            Log.i(LOG_TAG, "++++++++++++++++++++++++++++++++++++++++++++++++++++++");
            sendLog("OnOffThread Terminated");
            //threadToInterrupt.interrupt();
            mOnOffThread = null;
            mOnOffThreadState = false;
        }
    };

};
