package com.mdp.t16_android;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.content.BroadcastReceiver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import java.nio.charset.Charset;


public class BluetoothMain extends AppCompatActivity {
    //Overflow Menu
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.mainController:
                startActivity(new Intent(BluetoothMain.this, MainActivity.class));
                return true;

            case R.id.bluetoothSettings:
                return true;

            case R.id.stringMessages:
                startActivity(new Intent(BluetoothMain.this, StringConfiguration.class));
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.overflowmenu, menu);
        return true;
    }

    private static final String TAG = "BluetoothMain";

    public ArrayList<BluetoothDevice> btDevicesArrayList = new ArrayList<>();
    public ArrayList<BluetoothDevice> pairedDevicesArrayList = new ArrayList<>();

    public DeviceListAdapter mDeviceListAdapter;
    public DeviceListAdapter mPairedDeviceListAdapter;

    BluetoothAdapter mBluetoothAdapter;
    static BluetoothDevice mBTDevice;
    BluetoothDevice btConnectionDevice;

    Button btnSearch;
    Button btnStartConnection;
    TextView tvDeviceSearchStatus;
    ListView lvNewDevices;
    ListView lvPairedDevices;
    Switch switchONOFF;
    TextView tvConnectionStatus;

    Button btnSend;
    EditText editTextMessage;
    TextView incomingMessagesView;
    TextView outgoingMessagesView;
    StringBuilder message;
    StringBuilder sentMessage;


    Intent connectIntent;
    BluetoothConnectionService mBluetoothConnection;

    private static final UUID MY_UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");


    public static BluetoothDevice getBluetoothDevice(){

        return mBTDevice;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bluetooth_settings);

        // Menu Bar
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        if (getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            getSupportActionBar().setDisplayShowHomeEnabled(false);
        }

        btnSearch = findViewById(R.id.btnSearch);

        switchONOFF = findViewById(R.id.switchONOFF);

        btnStartConnection = findViewById(R.id.btnStartConnection);
        lvNewDevices =  findViewById(R.id.lvNewDevices);
        lvPairedDevices = findViewById(R.id.lvPairedDevices);
        tvDeviceSearchStatus = findViewById(R.id.tvDeviceSearchStatus);
        tvConnectionStatus = findViewById(R.id.tvConnectionStatus);
        mBTDevice = null;

        btDevicesArrayList = new ArrayList<>();
        pairedDevicesArrayList = new ArrayList<>();


        //Register receiver for bluetooth connection
        LocalBroadcastManager.getInstance(this).registerReceiver(btConnectionReceiver, new IntentFilter("btConnectionStatus"));


        //Broadcasts when bond state changes (pairing)
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(bondBroadcastReceiver, filter);

        //Broadcasts when discovering devices
        IntentFilter intentFilter = new IntentFilter(mBluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        registerReceiver(discoverabilityBroadcastReceiver, intentFilter);

        //Broadcasts when search started
        IntentFilter discoverStartedIntent = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        registerReceiver(discoveryStartedBroadcastReceiver, discoverStartedIntent);

        //Broadcasts when search ended
        IntentFilter discoverEndedIntent = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(discoveryEndedBroadcastReceiver, discoverEndedIntent);

        //Broadcasts when bluetooth state changes (connected, disconnected etc)
        IntentFilter filter2 = new IntentFilter();
        filter2.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter2.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        filter2.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(btConnectionReceiver, filter2);


        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if(mBluetoothAdapter.isEnabled()){
            switchONOFF.setChecked(true);
        }

        switchONOFF.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (switchONOFF.isChecked()){
                    Log.d(TAG, "onClick: Enabling/disabling bluetooth.");
                    enableDisableBT();
                    Toast.makeText(BluetoothMain.this, "Bluetooth is on", Toast.LENGTH_LONG).show();
                }
                else{
                    Toast.makeText(BluetoothMain.this, "Bluetooth is off", Toast.LENGTH_LONG).show();
                    if(mBluetoothAdapter.isEnabled()){

                        //discoverabilityON();
                        mBluetoothAdapter.disable();
                        switchONOFF.setChecked(false);

                        IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
                        registerReceiver(enableBluetoothBroadcastReceiver, BTIntent);

                    }

                }
            }
        });

        btnStartConnection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mBTDevice == null) {

                    Toast.makeText(BluetoothMain.this, "No Paired Device! Please Search/Select a Device.",
                            Toast.LENGTH_LONG).show();
                } else if(mBluetoothAdapter.getProfileConnectionState(BluetoothHeadset.HEADSET) == BluetoothHeadset.STATE_CONNECTED){
                    Toast.makeText(BluetoothMain.this, "Bluetooth Already Connected",
                            Toast.LENGTH_LONG).show();
                }

                else{
                    Log.d(TAG, "onClick: connect button");

                    //START CONNECTION WITH THE BOUNDED DEVICE
                    startBTConnection(mBTDevice, MY_UUID);
                }
                lvPairedDevices.setAdapter(mPairedDeviceListAdapter);
            }
        });

        //onclick listener for new devices list
        lvNewDevices.setOnItemClickListener(
                new AdapterView.OnItemClickListener(){
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                        //first cancel discovery because its very memory intensive.
                        mBluetoothAdapter.cancelDiscovery();

                        Log.d(TAG, "onItemClick: You Clicked on a device.");
                        String deviceName = btDevicesArrayList.get(i).getName();
                        String deviceAddress = btDevicesArrayList.get(i).getAddress();

                        Log.d(TAG, "onItemClick: deviceName = " + deviceName);
                        Log.d(TAG, "onItemClick: deviceAddress = " + deviceAddress);

                        //create the bond.
                        //NOTE: Requires API 17+? I think this is JellyBean
                        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2){
                            Log.d(TAG, "Trying to pair with " + deviceName);
                            btDevicesArrayList.get(i).createBond();

                            mBluetoothConnection = new BluetoothConnectionService(BluetoothMain.this);
                            mBTDevice = btDevicesArrayList.get(i);
                        }
                    }
                }
        );

        //onclick listener for paired device list
        lvPairedDevices.setOnItemClickListener(
                new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                        //Cancel discovery
                        mBluetoothAdapter.cancelDiscovery();

                        mBTDevice = pairedDevicesArrayList.get(i);

                        //UnSelect Search Device List
                        lvNewDevices.setAdapter(mDeviceListAdapter);

                        Log.d(TAG, "onItemClick: Paired Device = " + pairedDevicesArrayList.get(i).getName());
                        Log.d(TAG, "onItemClick: DeviceAddress = " + pairedDevicesArrayList.get(i).getAddress());

                        mBluetoothConnection = new BluetoothConnectionService(BluetoothMain.this);
                    }
                }
        );

        //onclick listener for search button
        btnSearch.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Log.d(TAG, "onClick: search button");
                enableDisableBT();
                btDevicesArrayList.clear();
            }
        });

        btnSend = findViewById(R.id.btnSend);
        editTextMessage = findViewById(R.id.editTextMessage);
        incomingMessagesView = findViewById(R.id.incomingMessagesView);
        outgoingMessagesView = findViewById(R.id.outgoingMessagesView);
        message = new StringBuilder();
        sentMessage = new StringBuilder();

        incomingMessagesView.setMovementMethod(new ScrollingMovementMethod());
        outgoingMessagesView.setMovementMethod(new ScrollingMovementMethod());

        // Register receiver for incoming message
        LocalBroadcastManager.getInstance(this).registerReceiver(incomingMessageReceiver, new IntentFilter("incomingMessage"));
        LocalBroadcastManager.getInstance(this).registerReceiver(sentMessageReceiver, new IntentFilter("sentMessage"));

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                byte[] bytes = editTextMessage.getText().toString().getBytes(Charset.defaultCharset());
                BluetoothConnectionService.write(bytes);
                editTextMessage.setText("");
            }
        });



    }

    // Create a BroadcastReceiver for ACTION_FOUND (enable bluetooth)
    private final BroadcastReceiver enableBluetoothBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (action.equals(mBluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, mBluetoothAdapter.ERROR);

                switch(state){
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG, "onReceive: STATE OFF");
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(TAG, "enableBluetoothBroadcastReceiver: STATE TURNING OFF");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG, "enableBluetoothBroadcastReceiver: STATE ON");
                        discoverabilityON();
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(TAG, "enableBluetoothBroadcastReceiver: STATE TURNING ON");
                        break;
                }
            }
        }
    };

    /**
     * Broadcast Receiver for changes made to bluetooth states such as:
     * 1) Discoverability mode on/off or expire.
     */
    private final BroadcastReceiver discoverabilityBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)) {

                int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.ERROR);

                switch (mode) {
                    //Device is in Discoverable Mode
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                        Log.d(TAG, "discoverabilityBroadcastReceiver: Discoverability Enabled.");

                        startSearch();

                        //START BLUETOOTH CONNECTION SERVICE WHICH WILL START THE ACCEPTTHREAD TO LISTEN FOR CONNECTION
                        connectIntent = new Intent(BluetoothMain.this, BluetoothConnectionService.class);
                        connectIntent.putExtra("serviceType", "listen");
                        //connectIntent.putExtra("device", device);
                        //connectIntent.putExtra("id", uuid);
                        startService(connectIntent);

                        //CHECK PAIRED DEVICE LIST
                        checkPairedDevice();
                        break;

                    //Device not in discoverable mode
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                        Log.d(TAG, "discoverabilityBroadcastReceiver: Discoverability Disabled. Able to receive connections.");
                        break;
                    case BluetoothAdapter.SCAN_MODE_NONE:
                        Log.d(TAG, "discoverabilityBroadcastReceiver: Discoverability Disabled. Not able to receive connections.");
                        break;
                    case BluetoothAdapter.STATE_CONNECTING:
                        Log.d(TAG, "discoverabilityBroadcastReceiver: Connecting....");
                        break;
                    case BluetoothAdapter.STATE_CONNECTED:
                        Log.d(TAG, "discoverabilityBroadcastReceiver: Connected.");
                        break;
                }

            }
        }
    };

    /**
     * Broadcast Receiver for listing devices that are not yet paired
     * -Executed by startSearch() method.
     */
    private BroadcastReceiver searchBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "onReceive: ACTION FOUND.");

            if (action.equals(BluetoothDevice.ACTION_FOUND)){
                BluetoothDevice device = intent.getParcelableExtra (BluetoothDevice.EXTRA_DEVICE);
                btDevicesArrayList.add(device);
                Log.d(TAG, "onReceive: " + device.getName() + ": " + device.getAddress());
                mDeviceListAdapter = new DeviceListAdapter(context, R.layout.device_adapter_view, btDevicesArrayList);
                lvNewDevices.setAdapter(mDeviceListAdapter);
                //bluetoothDeviceLbl.setText("List of New Devices: ");
            }
        }
    };

    //Broadcast Receiver for starting search
    private final BroadcastReceiver discoveryStartedBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_STARTED)) {

                Log.d(TAG, "Search Started...");

                tvDeviceSearchStatus.setText("Device search status: Searching...");

            }
        }
    };

    //Broadcast Receiver for ending search
    private final BroadcastReceiver discoveryEndedBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {

                Log.d(TAG, "Search done...");

                tvDeviceSearchStatus.setText("Device search status: Search is done");

            }
        }
    };

    /**
     * Broadcast Receiver that detects bond state changes (Pairing status changes)
     */
    private final BroadcastReceiver bondBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if(action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)){
                BluetoothDevice mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //3 cases:
                //case1: bonded already
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDED){
                    Log.d(TAG, "bondBroadcastReceiver: BOND_BONDED with" + mDevice.getName());

                    mBTDevice = mDevice;
                    Toast.makeText(BluetoothMain.this, "Bonded With: " + mDevice.getName(),
                            Toast.LENGTH_LONG).show();
                    checkPairedDevice();
                    lvNewDevices.setAdapter(mDeviceListAdapter);
                }
                //case2: creating a bond
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDING) {
                    Log.d(TAG, "bondBroadcastReceiver: BOND_BONDING.");
                }
                //case3: breaking a bond
                if (mDevice.getBondState() == BluetoothDevice.BOND_NONE) {
                    Log.d(TAG, "bondBroadcastReceiver: BOND_NONE.");

                    //DIALOG MSG POPUP
                    AlertDialog alertDialog = new AlertDialog.Builder(BluetoothMain.this).create();
                    alertDialog.setTitle("Bonding Status");
                    alertDialog.setMessage("Bond Disconnected");
                    alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                    alertDialog.show();

                    //RESET VARIABLE
                    mBTDevice = null;
                }
            }
        }
    };

    public void checkPairedDevice() {

        //check for paired devices
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        pairedDevicesArrayList.clear();

        if (pairedDevices.size() > 0) {

            for (BluetoothDevice device : pairedDevices) {
                Log.d(TAG, "PAIRED DEVICES: " + device.getName() + "," + device.getAddress());
                pairedDevicesArrayList.add(device);

            }
            //pairedDeviceLbl.setText("List of Paired Devices: ");
            mPairedDeviceListAdapter = new DeviceListAdapter(this, R.layout.device_adapter_view, pairedDevicesArrayList);
            lvPairedDevices.setAdapter(mPairedDeviceListAdapter);

        }
        else {

            /*String[] noDevice = {"No Device"};
            ListAdapter emptyListAdapter = new ArrayAdapter<String>(this, R.layout.device_adapter_view,R.id.deviceName, noDevice);
            lvPairedDevices.setAdapter(emptyListAdapter);*/
            //pairedDeviceLbl.setText("No Paired Devices: ");

            Log.d(TAG, "No paired devices");
        }
    }



    //Broadcast Receiver for bluetooth connection status
    BroadcastReceiver btConnectionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            final String action = intent.getAction();
            final BluetoothDevice mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            if(action.equals(BluetoothDevice.ACTION_ACL_CONNECTED)){
                Log.d(TAG, "btConnectionReceiver: Device now connected to "+mDevice.getName());
                Toast.makeText(BluetoothMain.this, "Device now connected to "+mDevice.getName(), Toast.LENGTH_SHORT).show();
                tvConnectionStatus.setText("Connection Status: Connected to " +mDevice.getName());
            }
            else
            if(action.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)) {
                Log.d(TAG, "btConnectionReceiver: Disconnected from "+mDevice.getName());
                Toast.makeText(BluetoothMain.this, "Disconnected from "+mDevice.getName(), Toast.LENGTH_SHORT).show();
                tvConnectionStatus.setText("Connection Status: Disconnected");

                //start accept thread and wait on the SAME device again
                mBluetoothConnection = new BluetoothConnectionService(BluetoothMain.this);
                mBluetoothConnection.start();


                /*//RECONNECT DIALOG MSG
                AlertDialog.Builder mAlert = new AlertDialog.Builder(BluetoothMain.this);
                mAlert.setTitle("Bluetooth Disconnected");
                mAlert.setMessage("Connection with device: '"+mDevice.getName()+"' has ended. Do you want to reconnect?");
                mAlert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //mBluetoothConnection.startClient(mBTDevice, MY_UUID);
                        startBTConnection(mDevice,MY_UUID);
                    }
                });
                mAlert.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                mAlert.show(); */

            }
        }
    };

    private void discoverabilityON() {

        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 900);
        startActivity(discoverableIntent);

    }

    @Override
    protected void onDestroy() {
        Log.d(TAG,"onDestroy called.");
        super.onDestroy();
        unregisterReceiver(enableBluetoothBroadcastReceiver);
        unregisterReceiver(discoverabilityBroadcastReceiver);
        unregisterReceiver(searchBroadcastReceiver);
        unregisterReceiver(bondBroadcastReceiver);
        unregisterReceiver(discoveryEndedBroadcastReceiver);
        unregisterReceiver(discoveryStartedBroadcastReceiver);
        unregisterReceiver(btConnectionReceiver);

        LocalBroadcastManager.getInstance(this).unregisterReceiver(incomingMessageReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(sentMessageReceiver);

    }

    private void enableDisableBT() {
        if(mBluetoothAdapter == null){
            Log.d(TAG, "enableDisableBT: Does not have Bluetooth capabilities.");
            Toast.makeText(BluetoothMain.this, "Device Does Not Support Bluetooth.",
                    Toast.LENGTH_LONG).show();
            switchONOFF.setChecked(false);
        }
        if(!mBluetoothAdapter.isEnabled()){
            Log.d(TAG, "enableDisableBT: Enabling Bluetooth.");
            switchONOFF.setChecked(true);

            // use intent to enable bluetooth
            Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBTIntent);

            //filter that intercepts changes to bluetooth status
            IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            //send pass the bluetooth adapter the ACTION_STATE_CHANGE, get caught by broadcast receiver
            //catch state change of the bluetooth and log it
            registerReceiver(enableBluetoothBroadcastReceiver, BTIntent);
        }
        if(mBluetoothAdapter.isEnabled()){

            discoverabilityON();

            IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(enableBluetoothBroadcastReceiver, BTIntent);

        }

    }

    /**
     * This method is required for all devices running API23+
     * Android must programmatically check the permissions for bluetooth. Putting the proper permissions
     * in the manifest is not enough.
     * NOTE: This will only execute on versions > LOLLIPOP because it is not needed otherwise.
     */

    private void checkBTPermissions() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION);

            permissionCheck += ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION);
            if (permissionCheck != 0) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001);
            }
        } else {
            Log.d(TAG, "checkBTPermissions: No need to check permissions. SDK version < LOLLIPOP.");

        }
    }

    public void startSearch() {
        Log.d(TAG, "btnSearch: Searching for unpaired devices.");

        if(mBluetoothAdapter.isDiscovering()){
            mBluetoothAdapter.cancelDiscovery();
            Log.d(TAG, "btnSearch: Canceling Search.");

            //check BT permissions in manifest
            checkBTPermissions();

            mBluetoothAdapter.startDiscovery();
            IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(searchBroadcastReceiver, discoverDevicesIntent);
            Log.d(TAG, "BTDiscovery: enable discovery");
        }
        if(!mBluetoothAdapter.isDiscovering()){

            //check BT permissions in manifest
            checkBTPermissions();

            mBluetoothAdapter.startDiscovery();
            Log.d(TAG, "BTDiscovery: enable discovery");
            IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(searchBroadcastReceiver, discoverDevicesIntent);
        }
    }

    /**
     * starting chat service method
     */
    public void startBTConnection(BluetoothDevice device, UUID uuid){
        Log.d(TAG, "startBTConnection: Initializing RFCOM Bluetooth Connection.");

        mBluetoothConnection.startClient(device,uuid);

        connectIntent = new Intent(BluetoothMain.this, BluetoothConnectionService.class);
        connectIntent.putExtra("serviceType", "connect");
        connectIntent.putExtra("device", device);
        connectIntent.putExtra("id", uuid);
        Log.d(TAG, "StartBTConnection: Starting Bluetooth Connection Service!");

        startService(connectIntent);
    }


    //Check Incoming Message Type
    public String checkIncomingMsgType(String msg) {

        String msgType = null;
        String[] splitedMsg = msg.split(":");


        switch (splitedMsg[0]) {

            //RobotStatus
            case "status":
                // Statements
                msgType = "robotstatus";
                break; // optional

            //Auto / Manual Refresh Of Map
            case "maprefresh":
                // Statements
                msgType = "maprefresh";
                break; // optional

            default: // Optional
                Log.d(TAG, "Checking Msg Type: Error - " + splitedMsg[0] + ":" + splitedMsg[1]);
                break;
            // Statements
        }
        return msgType;
    }

    //Broadcast Receiver for Incoming Message
    BroadcastReceiver incomingMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Log.d(TAG, "Received Message...");

            String msg = intent.getStringExtra("receivedMessage");
            message.append(msg + "\n");
            incomingMessagesView.setText(message);
        }
    };

    //Broadcast Receiver for Sent Message
    BroadcastReceiver sentMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Log.d(TAG, "Getting sent message");

            String msg = intent.getStringExtra("sentMessage");
            sentMessage.append(msg + "\n");
            outgoingMessagesView.setText(sentMessage);

        }
    };

}
