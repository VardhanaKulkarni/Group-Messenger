package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;
import android.widget.EditText;
import android.telephony.TelephonyManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;
import java.util.Hashtable;
import java.net.Socket;
import java.io.IOException;
import java.util.Comparator;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.concurrent.PriorityBlockingQueue;

import android.util.Log;
import android.os.AsyncTask;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 */

public class GroupMessengerActivity extends Activity {

    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    static final String[] REMOTE_PORT = {"11108","11112","11116","11120","11124"};
    static final int SERVER_PORT = 10000;

    static Hashtable<String,Integer[]> map = new Hashtable<String,Integer[]>();
    static String myport="";

    Comparator<myobj> myComparator = new Comparator<myobj>() {
        @Override
        public int compare(myobj lhs, myobj rhs) {
            if(lhs.propNum < rhs.propNum)
                return -1;
            if(rhs.propNum > lhs.propNum)
                return 1;
            return 0;
        }
    };
    PriorityBlockingQueue<myobj> holdQ =
            new PriorityBlockingQueue<myobj>(1000, myComparator);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        myport = myPort;

        try {
            /* Create a server socket as well as a thread (AsyncTask) that listens on the server
            * port.*/
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT,100);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }

        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */

        final TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());

        final EditText editText = (EditText) findViewById(R.id.editText1);

        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */

        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));

        findViewById(R.id.button4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg;
                msg = editText.getText().toString();
                tv.append("\t \t"+msg+"\n");
                editText.setText("");
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

    int msgId = 0;
    int failure = -1;
    private class ClientTask extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... msgs) {
            Socket socket[] = new Socket[5];
            PrintWriter[] out = new PrintWriter[5];
            BufferedReader[] in = new BufferedReader[5];

            String pNumber = "";
            String msgToSend = msgs[0];
            String msgPort = msgs[1];
            String msgkey = msgPort + "-" + Integer.toString(msgId);
            String strb = "M%"+msgToSend+"%"+msgkey;

            map.put(msgkey, new Integer[]{0, 0});
           
            for (int i = 0; i < 5; i++) {
                try {
                    Log.e(TAG, "Entering the client task loop" + Integer.toString(i));
                    socket[i] = new Socket();
                    socket[i].connect(new InetSocketAddress(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(REMOTE_PORT[i])));
                    out[i] = new PrintWriter(socket[i].getOutputStream());
                    in[i] = new BufferedReader(new InputStreamReader(socket[i].getInputStream()));

                    String response = "";
                    if (!(msgToSend == null)) {
                        out[i].println(strb);
                        out[i].flush();
                    }

                    socket[i].setSoTimeout(20000);
                    response = in[i].readLine();

                    if (response == null) {
                        failure = i;
                    }

                    if (response != null) {
                        String[] res = response.split("%");
                        if (map.containsKey(res[0])) {
                            Integer[] val = map.get(res[0]);

                            if (Integer.valueOf(res[1]) > val[1])
                                val[1] = Integer.valueOf(res[1]);
                            map.put(res[0], val);
                        }
                        if (i == 4) {
                            Integer sendnum = map.get(res[0])[1];
                            pNumber = "P%" + msgkey + "%" + Float.toString(sendnum) + myport + "%" + Integer.toString(failure);

                            for (int j = 0; j < 5; j++) {
                                out[j].println(pNumber);
                                out[j].flush();
                            }
                        }
                    } else if (response == null) {

                        
                        failure = i;
                        Integer[] val = map.get(msgkey);
                        val[0]++;
                        map.put(msgkey, val);
                        if (i == 4) {
                            Integer[] keys = map.get(msgkey);
                            Integer sendnum = keys[1];
                            pNumber = "P%" + msgkey + "%" + Float.toString(sendnum) + myport + "%" + Integer.toString(failure);

                            for (int j = 0; j < 5; j++) {
                                out[j].println(pNumber);
                                out[j].flush();
                            }
                        } else {
                            continue;
                        }
                    }
                } catch (org.apache.http.conn.ConnectTimeoutException e) {
                    Log.e("TimeOut Exception", "Socket time our exception");
                } catch (ConnectException e) {
                    Log.e("mycatch", "ClientTask Connect Exception");
                } catch (SocketTimeoutException e) {
                    failure = i;
                    Log.e("Exception", "sockettimeout"+ failure); Integer[] val = map.get(msgkey);
                    val[0]++;
                    map.put(msgkey, val);
                    if (i == 4) {
                        Integer[] keys = map.get(msgkey);
                        Integer sendnum = keys[1];
                        pNumber = "P%" + msgkey + "%" + Float.toString(sendnum) + myport + "%" + Integer.toString(failure);

                        for (int j = 0; j < 5; j++) {
                            out[j].println(pNumber);
                            out[j].flush();
                        }
                    }else {
                        continue;
                    }
                } catch (IOException e) {
                    Log.e("Exception", "ClientTask socket IOException" + e.getMessage());
                } catch (Exception e) {
                    Log.e("Exception", "some exception");
                }
            }
            msgId++;
            return null;
        }
    }

    static int proposalNum = 0;
    static int seqNumber=0;
    static float recievedAggNum = 0f;
    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            try {
                while(true){
                    Socket Csocket = serverSocket.accept();
                    handlemsg(Csocket);
                }
            } catch (IOException e) {
                Log.e(TAG, "serverTask socket IOException");
            }
            return null;
        }

        public Uri buildUri(String scheme, String authority) {
            Uri.Builder uriBuilder = new Uri.Builder();
            uriBuilder.authority(authority);
            uriBuilder.scheme(scheme);
            return uriBuilder.build();
        }

        public void onProgressUpdate(String... strings) {
                 /*
                 * The following code displays what is received in doInBackground().
                 */
            String strReceived = strings[0].trim();
            TextView remoteTextView = (TextView) findViewById(R.id.textView1);
            remoteTextView.append(strReceived + "\n");

            return;
        }

        public void handlemsg(Socket Csocket){
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(Csocket.getInputStream()));
                PrintWriter out = new PrintWriter(Csocket.getOutputStream());
                String msgReceived = "";

                while (true) {
                    Log.e("Server", "1");
                    msgReceived = in.readLine();
                    if(msgReceived == null)
                        break;
                    publishProgress(msgReceived);

                    String[] test = msgReceived.split("%");

                    if (test[0].equals("M")) {
                        String msg[] = msgReceived.split("%");
                        String tag = msg[0];
                        String mssg = msg[1];
                        String mkey = msg[2];

                        String[] iport = mkey.split("-");
                        String mport = iport[0];
                        String mId = iport[1];
                        proposalNum++;
                        myobj mObject = new myobj(mssg, mkey, 0);
                        holdQ.add(mObject);


                        String propStr = mport + "-" + mId + "%" + Integer.toString(proposalNum);

                        out.println(propStr + "\r\n");
                        out.flush();
                    } else if (msgReceived.charAt(0) == 'P') {
                        String[] msgNum = msgReceived.split("%");
                        String hsKey = msgNum[1];
                        String SrecievedAggNum = msgNum[2];
                        String Failednode = msgNum[3];
                        recievedAggNum = Float.valueOf(SrecievedAggNum);
                            int receivedInt = (int)(recievedAggNum);

                            if (proposalNum < receivedInt) {
                                proposalNum = receivedInt;
                            }
                        myobj curObj,Currrobj;
                        Iterator<myobj> iterr = holdQ.iterator();

                        while (iterr.hasNext()) {
                            curObj = iterr.next();
                            if (curObj.msgID.equals(hsKey)) {
                                holdQ.remove(curObj);
                                curObj.propNum = recievedAggNum;
                                curObj.deliverable = true;
                                holdQ.add(curObj);
                                break;
                            }
                        }
                        Iterator<myobj> iterr2 = holdQ.iterator();

                            if(Integer.valueOf(Failednode)!=-1){
                                while(iterr2.hasNext()){
                                    Currrobj = iterr2.next();
                                    String ID = Currrobj.msgID;
                                    String[] ids = ID.split("[-]");
                                    if(ids[0].equals(REMOTE_PORT[Integer.valueOf(Failednode)])){
                                        holdQ.remove(Currrobj);
                                    }
                                }
                            }

                            while (!holdQ.isEmpty() && holdQ.peek().deliverable) {
                                myobj delMsg = holdQ.poll();
                                
                                final ContentResolver mContentResolver = getContentResolver();
                                final Uri mUri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger2.provider");

                                String[] snumb = delMsg.msgID.split("-");
                                ContentValues cv = new ContentValues();
                                cv.put("key", Integer.toString(seqNumber));
                                cv.put("value", delMsg.message);
                                mContentResolver.insert(mUri, cv);
                                seqNumber++;
                            }
                            //The read terminates when the server receives the agreed sequence number from the client
                            break;
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "serverTask socket IOException");
            }
        }
    }

    public class myobj{
        String message;
        String msgID;
        float propNum;
        boolean deliverable;

        public myobj(String msg,String msgID,float prop){
            this.message = msg;
            this.msgID = msgID;
            this.propNum = prop;
            boolean deliverable = false;
        }
    }
}
