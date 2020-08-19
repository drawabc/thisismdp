package com.mdp.t16_android;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.UUID;

public class BluetoothConnectionService {
    private static final String TAG ="BluetoothConnectionServ";
    private static BluetoothConnectionService instance;

    private static final String appName = "MYAPP";

    private static final UUID MY_UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private BluetoothAdapter mBluetoothAdapter;
    Context mContext;

    private AcceptThread mAcceptThread;;
    private ConnectThread mConnectThread;
    public BluetoothDevice mmDevice;
    private UUID deviceUUID;

    ProgressDialog mProgressDialog;

    private static ConnectedThread mConnectedThread;

    //CONSTRUCTOR
    public BluetoothConnectionService(Context context) {

        //super("BluetoothConnectionService");
        this.mBluetoothAdapter= mBluetoothAdapter.getDefaultAdapter();
        this.mContext = context;
        start();
    }


    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    private class AcceptThread extends Thread {

        // The local server socket
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread(){
            BluetoothServerSocket tmp = null;

            // Create a new listening server socket
            try{
                tmp = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(appName, MY_UUID);
                Log.d(TAG, "AcceptThread: Setting up Server using: " + MY_UUID);

            }catch (IOException e){
                Log.e(TAG, "AcceptThread: IOException: " + e.getMessage() );
            }

            mmServerSocket = tmp;
        }

        public void run(){
            Log.d(TAG, "run: AcceptThread Running.");

            BluetoothSocket socket = null;

            try{
                // This is a blocking call and will only return on a
                // successful connection or an exception
                Log.d(TAG, "run: RFCOM server socket start.....");

                socket = mmServerSocket.accept();

                Log.d(TAG, "run: RFCOM server socket accepted connection.");
                connected(socket,mmDevice);

            }catch (IOException e){

                //connectionStatusIntent = new Intent("btConnectionStatus");
                // connectionStatusIntent.putExtra("ConnectionStatus", "connectionFail");
                //connectionStatusIntent.putExtra("Device", BluetoothMain.getBluetoothDevice());

                Log.e(TAG, "AcceptThread: IOException: " + e.getMessage() );
            }
            //This is for cases where the other device initiates the request. If the other party initiates, we can skip the ConnectThread
            //and go straight to connectedthread
            if(socket!=null){
                connected(socket, mmDevice);}

            Log.d(TAG, "END mAcceptThread ");
        }

        public void cancel() {
            Log.d(TAG, "cancel: Canceling AcceptThread.");
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "cancel: Close of AcceptThread ServerSocket failed. " + e.getMessage() );
            }
        }

    }

    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private BluetoothSocket mmSocket;

        public ConnectThread(BluetoothDevice device, UUID uuid) {
            Log.d(TAG, "ConnectThread: started.");
            mmDevice = device;
            deviceUUID = uuid;
        }

        public void run(){
            BluetoothSocket tmp = null;
            //Intent connectionStatusIntent;
            Log.i(TAG, "RUN mConnectThread ");

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                Log.d(TAG, "ConnectThread: Trying to create InsecureRfcommSocket using UUID: "
                        +MY_UUID);
                tmp = mmDevice.createRfcommSocketToServiceRecord(deviceUUID);
            } catch (IOException e) {
                Log.e(TAG, "ConnectThread: Could not create InsecureRfcommSocket " + e.getMessage());
            }

            mmSocket = tmp;

            // Always cancel discovery because it will slow down a connection
            mBluetoothAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket

            try {

                Log.d(TAG, "Connecting to Device: " + mmDevice);
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();

                /*//BROADCAST CONNECTION MSG
                connectionStatusIntent = new Intent("btConnectionStatus");
                connectionStatusIntent.putExtra("ConnectionStatus", "connect");
                connectionStatusIntent.putExtra("Device", mmDevice);
                LocalBroadcastManager.getInstance(mContext).sendBroadcast(connectionStatusIntent);*/

                Log.d(TAG, "run: ConnectThread connected.");

                //START BLUETOOTH CHAT
                //connected(mmSocket, mmDevice);

                //CANCEL ACCEPT THREAD FOR LISTENING
                /*if (mAcceptThread != null) {
                    mAcceptThread.cancel();
                    mAcceptThread = null;
                }*/

            } catch (IOException e) {
                // Close the socket
                try {
                    mmSocket.close();
                    //connectionStatusIntent = new Intent("btConnectionStatus");
                    //connectionStatusIntent.putExtra("ConnectionStatus", "connectionFail");
                    // connectionStatusIntent.putExtra("Device", mmDevice);

                    //LocalBroadcastManager.getInstance(mContext).sendBroadcast(connectionStatusIntent);
                    Log.d(TAG, "run: Closed Socket, connection failed");
                } catch (IOException e1) {
                    Log.e(TAG, "mConnectThread: run: Unable to close connection in socket " + e1.getMessage());
                }

            }
            connected(mmSocket,mmDevice);

        }
        public void cancel() {
            try {
                Log.d(TAG, "cancel: Closing Client Socket.");
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "cancel: close() of mmSocket in Connectthread failed. " + e.getMessage());
            }
        }
    }

    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume()
     */
    public synchronized void start() {
        Log.d(TAG, "start");

        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        if (mAcceptThread == null) {
            mAcceptThread = new AcceptThread();
            mAcceptThread.start();
        }
    }

    /**
     AcceptThread starts and sits waiting for a connection.
     Then ConnectThread starts and attempts to make a connection with the other devices AcceptThread.
     **/

    public void startClient(BluetoothDevice device,UUID uuid){
        Log.d(TAG, "startClient: Started.");

        //initprogress dialog
        mProgressDialog = ProgressDialog.show(mContext,"Connecting Bluetooth"
                ,"Please Wait...",true);

        mConnectThread = new ConnectThread(device, uuid);
        mConnectThread.start();
    }

    /**
     * Manages the connection
     */
    public class ConnectedThread extends Thread{
        private final BluetoothSocket mSocket;
        private final InputStream inStream;
        private final OutputStream outStream;

        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "ConnectedThread: Starting.");
            this.mSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            //dismiss the process dialog box when connection is established
            try {
                mProgressDialog.dismiss();
            } catch(NullPointerException e){
                e.printStackTrace();
            }

            try {
                tmpIn = mSocket.getInputStream();
                tmpOut = mSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            inStream = tmpIn;
            outStream = tmpOut;
        }

        public void run(){
            byte[] buffer = new byte[1024]; //byte array object that holds the input from the input stream
            int bytes; //will use this to read from the input stream

            //keep listening to the InputStream until an exception occurs
            while(true){
                try {
                    // read from input stream
                    bytes = inStream.read(buffer); //blocking call
                    String incomingmessage = new String(buffer, 0, bytes);
                    Log.d(TAG, "InputStream: "+ incomingmessage);

                    Intent incomingMessageIntent = new Intent("incomingMessage");
                    incomingMessageIntent.putExtra("receivedMessage", incomingmessage);
                    LocalBroadcastManager.getInstance(mContext).sendBroadcast(incomingMessageIntent);
                } catch (IOException e) {
                    Log.e(TAG, "Error reading input stream. "+e.getMessage());
                    break; //break the while loop if there is a problem with the input stream
                }
            }
        }

        /*
        //CALL THIS FROM MAIN ACTIVITY TO SEND DATA TO REMOTE DEVICE (ROBOT)//
        */
        public void write(byte[] bytes) {
            String text = new String(bytes, Charset.defaultCharset());
            Log.d(TAG, "write: Writing to output stream: "+text);

            Intent sentMessageIntent = new Intent("sentMessage");
            sentMessageIntent.putExtra("sentMessage", text);
            LocalBroadcastManager.getInstance(mContext).sendBroadcast(sentMessageIntent);

            try {
                outStream.write(bytes);
            } catch (IOException e) {
                Log.e(TAG, "Error writing to output stream. "+e.getMessage());
            }
        }

        //CALL THIS TO SHUTDOWN CONNECTION
        public void cancel() {
            try {
                Log.d(TAG, "cancel: Closing Client Socket");
                mSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "cancel: Failed to close ConnectThread mSocket " + e.getMessage());
            }
        }
    }



    //METHOD TO START CHAT SERVICE
    private void connected(BluetoothSocket mySocket, BluetoothDevice myDevice) {
        Log.d(TAG, "Connected: Starting");

        //stop the accept thread from listening
        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }
        //showToast("Connection Established With: "+myDevice.getName());
        //btConnectionDevice = myDevice;
        //mContext = context;
        //Start thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(mySocket);
        mConnectedThread.start();
    }



    public static void write(byte[] out) {

        // Create temporary object
        ConnectedThread temp;

        // Synchronize a copy of the ConnectedThread
        Log.d(TAG, "write: Write Called.");
        //perform the write
        mConnectedThread.write(out);

    }
}
