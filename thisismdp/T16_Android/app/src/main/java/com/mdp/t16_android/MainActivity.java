package com.mdp.t16_android;

import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private static final String TAG = "MainActivity";

    //Declarations for Map, Buttons & Switches... Etc on Main Screen
    GridMap myGridMap;
    Button sendPointBtn;
    Button updateBtn;
    ImageButton upBtn;
    ImageButton downBtn;
    ImageButton leftBtn;
    ImageButton rightBtn;
    ToggleButton explorationBtn;
    ToggleButton fastestPathBtn;
    ToggleButton setWayPointBtn;
    ToggleButton setStartPointBtn;
    TextView robotStatusView;
    TextView stringCmdView;
    TextView sentMessagesView;
    TextView connectionStatusBox;
    StringBuilder sentMessage;
    Switch modeSwitch;                      //For Auto/Manual Mode Switch
    Switch tiltSwitch;                      //Declarations for Tilt Toggling
    private SensorManager sensorManager;    //Declarations for Tilt Toggling
    private Sensor sensor;                  //Declarations for Tilt Toggling
    boolean tiltNavi;                       //Declarations for Tilt Toggling
    boolean modeType;
    boolean connectedState;                 //Declarations for Bluetooth Connection
    boolean currentActivity;                //Declarations for Bluetooth Connection
    static String connectedDevice;          //Declarations for Bluetooth Connection
    BluetoothConnectionService mBluetoothConnection; //Declarations for Bluetooth Connection
    BluetoothDevice mDevice;

    StringBuilder message;


    private static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    ArrayList<String> commandBuffer = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Menu Bar
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        // Bluetooth Connectivity        LocalBroadcastManager.getInstance(this).registerReceiver(btConnectionReceiver, new IntentFilter("btConnectionStatus"));
        // connectedDevice = null;
        connectedState = false;
        currentActivity = true;

        //Broadcasts when bluetooth state changes (connected, disconnected etc)
        IntentFilter filter2 = new IntentFilter();
        filter2.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter2.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        filter2.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(btConnectionReceiver, filter2);

        // Register Broadcast Receiver for incoming bluetooth connection
        LocalBroadcastManager.getInstance(this).registerReceiver(btConnectionReceiver, new IntentFilter("btConnectionStatus"));

        // Register Broadcast Receiver for incoming bluetooth message
        LocalBroadcastManager.getInstance(this).registerReceiver(incomingMessageReceiver, new IntentFilter("incomingMessage"));
        LocalBroadcastManager.getInstance(this).registerReceiver(sentMessageReceiver, new IntentFilter("sentMessage"));

        // Initialization of all Buttons, Text Views... Etc.
        // Getting all the Text Views & Setting their Status
        stringCmdView = (TextView) findViewById(R.id.stringCmdView);
        stringCmdView.setMovementMethod(new ScrollingMovementMethod());
        robotStatusView = findViewById(R.id.robotStatusView);
        robotStatusView.setText("Robot Stopped. Waiting for further commands.\n");
        robotStatusView.setMovementMethod(new ScrollingMovementMethod());
        sentMessagesView = findViewById(R.id.sentMessagesView);
        sentMessagesView.setMovementMethod(new ScrollingMovementMethod());
        sentMessage = new StringBuilder();

        connectionStatusBox = findViewById(R.id.connectionStatusBox);

        // Movement Buttons
        upBtn = findViewById(R.id.upBtn);
        leftBtn = findViewById(R.id.leftBtn);
        rightBtn = findViewById(R.id.rightBtn);
        downBtn = findViewById(R.id.downBtn);

        // Switches & Update Button
        // Tilt Switch Initiation
        tiltNavi = false;
        tiltSwitch = findViewById(R.id.tiltSwitch);
        tiltSwitch.setChecked(false);

        // Manual Mode Switch Initiation
        modeType = false;
        Switch modeTypeSwitch = findViewById(R.id.modeSwitch);

        // Update Button
        updateBtn = findViewById(R.id.updateBtn);
        updateBtn.setEnabled(false);

        // Map Initiation
        myGridMap = findViewById(R.id.gridMap);
        myGridMap.initializeMap();

        // Sensor Manager & Sensor Type
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        // Register Tilt Motion Sensor
        sensorManager.registerListener(this, sensor, 900000);

        //Show connection status
        if (connectedDevice == null) {
            connectionStatusBox.setText("Disconnected");
        } else {
            connectionStatusBox.setText("Connected");
        }

        tiltSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (tiltSwitch.isChecked()) {
                    tiltNavi = true;
                    Toast.makeText(MainActivity.this, "Tilt Switch On!", Toast.LENGTH_SHORT).show();
                } else {
                    tiltNavi = false;
                    Toast.makeText(MainActivity.this, "Tilt Switch Off!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        //Instructions Message List
        /*
        1 - Forward
        2 - Turn Left
        3 - Turn Right
        4 - Backward
        5 - Start Exploration
        6 - Start Fastest Path
        7 - Force End
        8 - Calibrate Sensors
        9 - Send Waypoint Coordinates
        */

        // Forward Button
        upBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // Check BT connection If not connected to any bluetooth device
                if (connectedDevice == null) {
                    Toast.makeText(MainActivity.this, "Please connect to bluetooth device first!", Toast.LENGTH_SHORT).show();
                } else {

                    // If already connected to a bluetooth device
                    // Outgoing message to move forward
                    String navigate = "And|Ard|0|1"; //TODO: Double check this part
                    byte[] bytes = navigate.getBytes(Charset.defaultCharset());
                    BluetoothConnectionService.write(bytes);
                    Log.d(TAG, "Android Controller: Move Forward sent");
                    robotStatusView.append("Moving Forward\n");
                    stringCmdView.append("Android Controller: Move Forward\n");
                    myGridMap.moveForward();
                }
            }
        });

        // Turn Left button
        leftBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // Check BT connectionIf not connected to any bluetooth device
                if (connectedDevice == null) {
                    Toast.makeText(MainActivity.this, "Please connect to bluetooth device first!", Toast.LENGTH_SHORT).show();
                } else {

                    // If already connected to a bluetooth device
                    // Outgoing message to turn left
                    String navigate = "And|Ard|1|1"; //TODO: Double check this part
                    byte[] bytes = navigate.getBytes(Charset.defaultCharset());
                    BluetoothConnectionService.write(bytes);
                    Log.d(TAG, "Android Controller: Turn Left sent");
                    robotStatusView.append("Turned Left\n");
                    stringCmdView.append("Android Controller: Turn Left\n");
                    myGridMap.rotateLeft();
                }
            }
        });

        // Turn right button
        rightBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // Check BT connectionIf not connected to any bluetooth device
                if (connectedDevice == null) {
                    Toast.makeText(MainActivity.this, "Please connect to bluetooth device first!", Toast.LENGTH_SHORT).show();
                } else {

                    // If already connected to a bluetooth device
                    // Outgoing message to turn right
                    String navigate = "And|Ard|2|1"; //TODO: Double check this part
                    byte[] bytes = navigate.getBytes(Charset.defaultCharset());
                    BluetoothConnectionService.write(bytes);
                    Log.d(TAG, "Android Controller: Turn Right sent");
                    robotStatusView.append("Turned Right\n");
                    stringCmdView.append("Android Controller: Turn Right\n");
                    myGridMap.rotateRight();
                }
            }
        });

        // Reverse button
        downBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Check BT connectionIf not connected to any bluetooth device
                if (connectedDevice == null) {
                    Toast.makeText(MainActivity.this, "Please connect to bluetooth device first!", Toast.LENGTH_SHORT).show();
                } else {

                    // If already connected to a bluetooth device
                    // Outgoing message to move backwards
                    String navigate = "And|Ard|3|1"; //TODO: Double check this part
                    byte[] bytes = navigate.getBytes(Charset.defaultCharset());
                    BluetoothConnectionService.write(bytes);
                    Log.d(TAG, "Android Controller: Move Backwards sent");
                    robotStatusView.append("Moving Backwards\n");
                    stringCmdView.append("Android Controller: Move Backwards\n");
                    myGridMap.moveBackwards();
                }
            }
        });

        // Select Waypoint button
        setWayPointBtn = findViewById(R.id.setWayPointBtn);
        setWayPointBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                // Check BT connectionIf not connected to any bluetooth device
                if (connectedDevice == null) {
                    Toast.makeText(MainActivity.this, "Please connect to bluetooth device first!", Toast.LENGTH_SHORT).show();
                } else {

                    if (isChecked) {
                        // The toggle is enabled : To select waypoint on map
                        myGridMap.selectWayPoint();
                        setWayPointBtn.toggle();
                        Toast.makeText(MainActivity.this, "Select a point on the map!", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        // Select Start Point button
        setStartPointBtn = (ToggleButton) findViewById(R.id.setStartPointBtn);
        setStartPointBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                // Check BT connectionIf not connected to any bluetooth device
                if (connectedDevice == null) {
                    Toast.makeText(MainActivity.this, "Please connect to bluetooth device first!", Toast.LENGTH_SHORT).show();
                } else {
                    if (isChecked) {
                        // The toggle is enabled: To select start point on map
                        myGridMap.selectStartPoint();
                        setStartDirection();
                        setStartPointBtn.toggle();
                    }
                }
            }
        });

        // To send start coordinates and waypoint coordinates to Algorithm
        sendPointBtn = (Button) findViewById(R.id.sendPointBtn);
        sendPointBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Check BT connectionIf not connected to any bluetooth device
                if (connectedDevice == null) {
                    Toast.makeText(MainActivity.this, "Please connect to bluetooth device first!", Toast.LENGTH_SHORT).show();
                } else {

                    //If already connected to a bluetooth device
                    // Send both coordinates to Algorithm as one string
                    int convertDirection = myGridMap.getRobotDirection();
                    String sendAlgoCoord = "And|Alg|9|".concat(Integer.toString(myGridMap.getStartCoord()[0])).concat(",").concat(Integer.toString(myGridMap.getStartCoord()[1])).concat(",").concat(Integer.toString(convertDirection)).concat(",").concat(Integer.toString(myGridMap.getWayPoint()[0])).concat(",").concat(Integer.toString(myGridMap.getWayPoint()[1]));
                    byte[] bytes = sendAlgoCoord.getBytes(Charset.defaultCharset());
                    BluetoothConnectionService.write(bytes);
                    Log.d(TAG, "Sent Start and Waypoint Coordinates to Algo");
                    Toast.makeText(MainActivity.this, "Start & Waypoint coordinates sent", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Auto / Manual mode button  d
        modeTypeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (connectedDevice == null) {
                    Toast.makeText(MainActivity.this, "Please connect to bluetooth device first!", Toast.LENGTH_SHORT).show();
                } else {
                    if (isChecked) {
                        // The toggle is enabled; Manual Mode

                        // Direction buttons are disabled
                        // Update button is enabled
                        updateBtn.setEnabled(true);
                        upBtn.setEnabled(false);
                        leftBtn.setEnabled(false);
                        rightBtn.setEnabled(false);
                        downBtn.setEnabled(false);
                        Toast.makeText(MainActivity.this, "Manual Mode enabled", Toast.LENGTH_SHORT).show();
                        myGridMap.setAutoUpdate(false);
                        Log.d(TAG, "Auto updates disabled.");

                    } else {
                        // The toggle is disabled; Auto Mode

                        // Update button is disabled
                        // Direction buttons are enabled
                        myGridMap.refreshMap(true);
                        updateBtn.setEnabled(false);
                        upBtn.setEnabled(true);
                        leftBtn.setEnabled(true);
                        rightBtn.setEnabled(true);
                        downBtn.setEnabled(true);
                        Toast.makeText(MainActivity.this, "Auto Mode enabled", Toast.LENGTH_SHORT).show();
                        myGridMap.setAutoUpdate(true);
                        Log.d(TAG, "Auto updates enabled.");
                    }
                }
            }
        });

        // Start Exploration button
        explorationBtn = findViewById(R.id.explorationBtn);
        explorationBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (connectedDevice == null) {
                    Toast.makeText(MainActivity.this, "Please connect to bluetooth device first!", Toast.LENGTH_SHORT).show();
                } else {
                    if (isChecked) {
                        // The toggle is enabled; Start Exploration Mode
                        startExploration();
                    } else {
                        endExploration();
                    }
                }
            }
        });

        // Start Fastest Path button
        fastestPathBtn = findViewById(R.id.fastestPathBtn);
        fastestPathBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                // Check BT connectionIf not connected to any bluetooth device
                if (connectedDevice == null) {
                    Toast.makeText(MainActivity.this, "Please connect to bluetooth device first!", Toast.LENGTH_SHORT).show();
                } else {
                    if (isChecked) {
                        // The toggle is enabled; Start Fastest Path Mode
                        startFastestPath();
                    } else {
                        // The toggle is off; End Fastest Path Mode
                        endFastestPath();
                    }
                }
            }
        });
    }

    // Delimiter for messages
    private String[] msgDelimiter(String message, String delimiter) {
        return (message.toLowerCase()).split(delimiter);
    }

    // Broadcast Receiver for incoming messages
    BroadcastReceiver incomingMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String allMsg = intent.getStringExtra("receivedMessage");

            Log.d(TAG, "Receiving incoming message: " + allMsg);

            stringCmdView.append(allMsg + "\n");

            // Add incoming commands into a buffer to process
            commandBuffer.add(allMsg);
            while (!commandBuffer.isEmpty()) {
                String incomingMsg = commandBuffer.remove(0);
                // Filter empty and concatenated string from receiving channel
                if (incomingMsg.length() > 8 && incomingMsg.length() < 345) {

                    // Check if string is for android
                    if (incomingMsg.substring(4, 7).equals("And")) {
                        //incomingMsg.replaceAll("\\r|\\n", "");
                        String[] filteredMsg = msgDelimiter(incomingMsg.replaceAll("\\,", "\\|").trim(), "\\|");
                        Log.d(TAG, "Incoming Message filtered: " + filteredMsg[2]);

                        // String commands for Android
                        switch (filteredMsg[2]) {

                            // Command: FORWARD
                            case "0":
                                for (int counter = Integer.parseInt(filteredMsg[3]); counter >= 1; counter--) {
                                    myGridMap.moveForward();
                                    Log.d(TAG, "Move Forward");
                                    stringCmdView.append("Move Forward\n");
                                    robotStatusView.append("Moving\n");
                                }
                                break;
                            // Command: TURN LEFT
                            case "1":
                                for (int counter = Integer.parseInt(filteredMsg[3]); counter >= 1; counter--) {
                                    myGridMap.rotateLeft();
                                    Log.d(TAG, "Turn Left");
                                    stringCmdView.append("Turn Left\n");
                                    robotStatusView.append("Moving\n");
                                }
                                break;

                            // Command: TURN RIGHT
                            case "2":
                                for (int counter = Integer.parseInt(filteredMsg[3]); counter >= 1; counter--) {
                                    myGridMap.rotateRight();
                                    Log.d(TAG, "Turn Right");
                                    stringCmdView.append("Turn Right\n");
                                    robotStatusView.append("Moving\n");
                                }
                                break;

                            // Command: REVERSE
                            case "3":
                                for (int counter = Integer.parseInt(filteredMsg[3]); counter >= 1; counter--) {
                                    myGridMap.moveBackwards();
                                    stringCmdView.append("Move Backwards\n");
                                    robotStatusView.append("Moving\n");
                                }
                                break;

                            // Command: FORCE END PROCESSES
                            case "8":
                            case "END":
                                endExploration();
                                endFastestPath();
                                break;

                            // Command: ARROW DETECTED
                            case "a":
                                myGridMap.setArrowImageCoord(filteredMsg[3], filteredMsg[4], filteredMsg[5]);
                                break;

                            // Command: Arrow String from Algo
                            case "done":
                                Log.d(TAG, "Arrow String Command for Algo");
                                break;

                            // Command: Part 1 of MAP Descriptor
                            case "md1":
                                String mapDes1 = filteredMsg[3];
                                myGridMap.mapDescriptorExplored(mapDes1);
                                break;


                            // Command: Part 2 of Map Descriptor
                            case "md2":
                                String mapDes2 = filteredMsg[3];
                                myGridMap.mapDescriptorObstacle(mapDes2);
                                break;


                            // Command: Robot has stopped moving
                            case "s":
                                robotStatusView.append("Stop\n");
                                stringCmdView.append(" \n");
                                break;

                            // Default case; string not recognised
                            default:
                                Log.d(TAG, "String command not recognised.");
                                break;
                        }


                        // To handle concatenation with the previous string command
                        if (filteredMsg.length >= 5) {

                            // If the concatenated string command is for Android
                            if (filteredMsg[5].equals("and")) {

                                Log.d(TAG, "Incoming Message 2 filtered: " + filteredMsg[6]);

                                // Command for Android
                                switch (filteredMsg[6]) {

                                    // Command: FORWARD
                                    case "0":
                                        for (int counter = Integer.parseInt(filteredMsg[7]); counter >= 1; counter--) {
                                            myGridMap.moveForward();
                                            Log.d(TAG, "Move Forward");
                                            stringCmdView.append("Move Forward\n");
                                            robotStatusView.append("Moving\n");
                                        }
                                        break;


                                    // Command: TURN LEFT
                                    case "1":
                                        for (int counter = Integer.parseInt(filteredMsg[7]); counter >= 1; counter--) {
                                            myGridMap.rotateLeft();
                                            Log.d(TAG, "Turn left");
                                            stringCmdView.append("Turn Left\n");
                                            robotStatusView.append("Moving\n");
                                        }
                                        break;


                                    // Command: TURN RIGHT
                                    case "2":
                                        for (int counter = Integer.parseInt(filteredMsg[7]); counter >= 1; counter--) {
                                            myGridMap.rotateRight();
                                            Log.d(TAG, "Turn right");
                                            stringCmdView.append("Turn Right\n");
                                            robotStatusView.append("Moving\n");
                                        }
                                        break;


                                    // Command: REVERSE
                                    case "3":
                                        for (int counter = Integer.parseInt(filteredMsg[7]); counter >= 1; counter--) {
                                            myGridMap.moveBackwards();
                                            stringCmdView.append("Move Backwards\n");
                                            robotStatusView.append("Moving\n");
                                        }
                                        break;


                                    // Command: FORCE END PROCESSES
                                    case "8":
                                    case "END":
                                        endExploration();
                                        endFastestPath();
                                        break;

                                    // Command: ARROW DETECTED
                                    case "a":
                                        myGridMap.setArrowImageCoord(filteredMsg[7], filteredMsg[8], filteredMsg[9]);
                                        break;

                                    // Command: Arrow String from Algo
                                    case "done":
                                        Log.d(TAG, "Arrow String Command for Algo");
                                        break;

                                    // Command: Part 1 of Map Descriptor
                                    case "md1":
                                        String mapDes1 = filteredMsg[7];
                                        myGridMap.mapDescriptorExplored(mapDes1);
                                        break;


                                    // Command: Part 2 of Map Descriptor
                                    case "md2":
                                        String mapDes2 = filteredMsg[7];
                                        myGridMap.mapDescriptorObstacle(mapDes2);
                                        break;

                                    // Command: Robot has stopped moving
                                    case "s":
                                        robotStatusView.append("Stop\n");
                                        stringCmdView.append(" \n");
                                        break;

                                    // Default case: string not recognised
                                    default:
                                        Log.d(TAG, "String command not recognised.");
                                        break;
                                }

//                                if (filteredMsg[9].equals("and")) {
//
//                                    Log.d(TAG, "Incoming Message 2 filtered: " + filteredMsg[6]);
//
//                                    // Command for Android
//                                    switch (filteredMsg[10]) {
//
//                                        // Command: FORWARD
//                                        case "0":
//                                            for (int counter = Integer.parseInt(filteredMsg[7]); counter >= 1; counter--) {
//                                                myGridMap.moveForward();
//                                                Log.d(TAG, "Move Forward");
//                                                stringCmdView.append("Move Forward\n");
//                                                robotStatusView.append("Moving\n");
//                                            }
//                                            break;
//
//
//                                        // Command: TURN LEFT
//                                        case "1":
//                                            for (int counter = Integer.parseInt(filteredMsg[7]); counter >= 1; counter--) {
//                                                myGridMap.rotateLeft();
//                                                Log.d(TAG, "Turn left");
//                                                stringCmdView.append("Turn Left\n");
//                                                robotStatusView.append("Moving\n");
//                                            }
//                                            break;
//
//
//                                        // Command: TURN RIGHT
//                                        case "2":
//                                            for (int counter = Integer.parseInt(filteredMsg[7]); counter >= 1; counter--) {
//                                                myGridMap.rotateRight();
//                                                Log.d(TAG, "Turn right");
//                                                stringCmdView.append("Turn Right\n");
//                                                robotStatusView.append("Moving\n");
//                                            }
//                                            break;
//
//
//                                        // Command: REVERSE
//                                        case "3":
//                                            for (int counter = Integer.parseInt(filteredMsg[7]); counter >= 1; counter--) {
//                                                myGridMap.moveBackwards();
//                                                stringCmdView.append("Move Backwards\n");
//                                                robotStatusView.append("Moving\n");
//                                            }
//                                            break;
//
//
//                                        // Command: FORCE END PROCESSES
//                                        case "8":
//                                        case "END":
//                                            endExploration();
//                                            endFastestPath();
//                                            break;
//
//                                        // Command: ARROW DETECTED
//                                        case "a":
//                                            myGridMap.setArrowImageCoord(filteredMsg[7], filteredMsg[8], filteredMsg[9]);
//                                            break;
//
//                                        // Command: Arrow String from Algo
//                                        case "done":
//                                            Log.d(TAG, "Arrow String Command for Algo");
//                                            break;
//
//                                        // Command: Part 1 of Map Descriptor
//                                        case "md1":
//                                            String mapDes1 = filteredMsg[7];
//                                            myGridMap.mapDescriptorExplored(mapDes1);
//                                            break;
//
//
//                                        // Command: Part 2 of Map Descriptor
//                                        case "md2":
//                                            String mapDes2 = filteredMsg[7];
//                                            myGridMap.mapDescriptorObstacle(mapDes2);
//                                            break;
//
//                                        // Command: Robot has stopped moving
//                                        case "s":
//                                            robotStatusView.append("Stop\n");
//                                            stringCmdView.append(" \n");
//                                            break;
//
//                                        // Default case: string not recognised
//                                        default:
//                                            Log.d(TAG, "String command not recognised.");
//                                            break;
//                                    }
//                                }
                            }
                        }
                    }
                }
                    // The following is for clearing checklist commands only.

//                // For receiving AMD robotPosition and grid
//                if (incomingMsg.substring(0, 1).equals("{")) {
//                    Log.d(TAG, "Incoming Message from AMD: " + incomingMsg);
//                    String[] filteredMsg = msgDelimiter(incomingMsg.replaceAll(" ", "").replaceAll(",", "\\|").replaceAll("\\{", "").replaceAll("\\}", "").replaceAll("\\:", "\\|").replaceAll("\"", "").replaceAll("\\[", "").replaceAll("\\]", "").trim(), "\\|");
//                    Log.d(TAG, "filteredMsg: " + filteredMsg);
//
//                    // AMD Robot Position
//                    if (filteredMsg[0].equals("robotposition")) {
//                        int robotPosCol = Integer.parseInt(filteredMsg[1]) + 1;
//                        int robotPosRow = 19 - (Integer.parseInt(filteredMsg[2]) + 1);
//                        int robotPosDeg = Integer.parseInt(filteredMsg[3]);
//                        int robotPosDir = 0;
//                        // Up
//                        if (robotPosDeg == 0)
//                            robotPosDir = 0;
//                            //Right
//                        else if (robotPosDeg == 90)
//                            robotPosDir = 3;
//                            //Down
//                        else if (robotPosDeg == 180)
//                            robotPosDir = 2;
//                            // Left
//                        else if (robotPosDeg == 270)
//                            robotPosDir = 1;
//                        // For setting robot start position from AMD
//                        myGridMap.setCurPos(robotPosRow, robotPosCol);
//                        myGridMap.setRobotDirection(robotPosDir);
//                    }
//
//                    // AMD Map Descriptor
//                    else if (filteredMsg[0].equals("grid")) {
//                        String mdAMD = filteredMsg[1];
//                        myGridMap.mapDescriptorChecklist(mdAMD);
//                        myGridMap.refreshMap(myGridMap.getAutoUpdate());
//                        Log.d(TAG, "mdAMD: " + mdAMD);
//
//                        // For setting up map from received AMD MDF String, use mdAMD
//                        Log.d(TAG, "Processing mdAMD...");
//                    }
//
//                    //Arrows coordinates
//                    else if (filteredMsg[0].equals("a")) {
//                        myGridMap.setArrowImageCoord(filteredMsg[1], filteredMsg[2], filteredMsg[3]);
//                        break;
//                    }
//                }
                }
            }
        }

        ;

        //Broadcast Receiver for Sent Message
        BroadcastReceiver sentMessageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                Log.d(TAG, "Getting sent message");

                String msg = intent.getStringExtra("sentMessage");
                sentMessage.append(msg + "\n");
                sentMessagesView.setText(sentMessage);

            }
        };

        // Setting Start Point Direction
        public void setStartDirection() {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("Select Robot Direction")
                    .setItems(R.array.directions_array, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int i) {
                            myGridMap.setRobotDirection(i);
                            dialog.dismiss();
                            Log.d(TAG, "Start Point Direction set");
                        }
                    });
            builder.create();
            builder.create().show();
        }

        // Start Exploration
        public void startExploration() {
            Toast.makeText(MainActivity.this, "Exploration started", Toast.LENGTH_SHORT).show();
            String startExp = "And|Alg|6";
            byte[] bytes = startExp.getBytes(Charset.defaultCharset());
            BluetoothConnectionService.write(bytes);
            Log.d(TAG, "Start Exploration");
            stringCmdView.append("Start Exploration\n");
            robotStatusView.append("Moving\n");
        }

        // End Exploration
        public void endExploration() {
            String endExp = "And|Alg|8";
            byte[] bytes = endExp.getBytes(Charset.defaultCharset());
            BluetoothConnectionService.write(bytes);
            Log.d(TAG, "End Exploration");
            stringCmdView.append("End Exploration\n");
            robotStatusView.append("Stop\n");
            Toast.makeText(MainActivity.this, "Exploration ended", Toast.LENGTH_SHORT).show();
        }

        // Start Fastest Path
        public void startFastestPath() {
            Toast.makeText(MainActivity.this, "Fastest Path started", Toast.LENGTH_SHORT).show();
            String startFP = "And|Alg|7";
            byte[] bytes = startFP.getBytes(Charset.defaultCharset());
            BluetoothConnectionService.write(bytes);
            Log.d(TAG, "Start Fastest Path");
            stringCmdView.append("Start Fastest Path\n");
            robotStatusView.append("Moving\n");
        }

        // End Fastest Path
        public void endFastestPath() {
            Log.d(TAG, "Fastest Path Ended.");
            String endFP = "And|Alg|8";
            byte[] bytes = endFP.getBytes(Charset.defaultCharset());
            BluetoothConnectionService.write(bytes);
            stringCmdView.append("End Fastest Path\n");
            robotStatusView.append("Stop\n");
            Toast.makeText(MainActivity.this, "Fastest Path ended", Toast.LENGTH_SHORT).show();
        }

        // Manual Mode; Update button
        public void onClickUpdate(View view) {
            Log.d(TAG, "Updating....");
            myGridMap.refreshMap(true);
            Log.d(TAG, "Update completed!");
            Toast.makeText(MainActivity.this, "Update completed", Toast.LENGTH_SHORT).show();
        }

        public void onSensorChanged(SensorEvent event) {
            float x = event.values[0];
            float y = event.values[1];

            // Check if Tilt Control activated
            if (tiltNavi == true) {
                if (Math.abs(x) > Math.abs(y)) {

                    // Right Tilt
                    if (x < 0) {
                        Log.d("MainActivity:", "RIGHT TILT!!");
                        robotStatusView.append("Moving\n");
                        stringCmdView.append("Android Controller: Turn Right\n");
                        myGridMap.rotateRight();
                    }

                    // Left Tilt
                    if (x > 0) {
                        Log.d("MainActivity:", "LEFT TILT!!");
                        robotStatusView.append("Moving\n");
                        stringCmdView.append("Android Controller: Turn Left\n");
                        myGridMap.rotateLeft();
                    }
                } else {
                    // Forward Tilt
                    if (y < 0) {
                        Log.d("MainActivity:", "UP TILT!!");
                        robotStatusView.append("Moving\n");
                        stringCmdView.append("Android Controller: Move Forward\n");
                        myGridMap.moveForward();
                    }

                    // Backward Tilt
                    if (y > 0) {
                        Log.d("MainActivity:", "DOWN TILT!!");
                        robotStatusView.append("Moving\n");
                        stringCmdView.append("Android Controller: Move Backwards\n");
                        myGridMap.moveBackwards();
                    }
                }
            }
        }

        public void onAccuracyChanged(Sensor arg0, int arg1) {
        }

        @Override
        protected void onResume() {
            super.onResume();
            sensorManager.registerListener(this, sensor, 900000);
        }

        @Override
        protected void onPause() {
            super.onPause();
            //unregister Sensor listener
            sensorManager.registerListener(this, sensor, 900000);
        }

        //Broadcast Receiver for Bluetooth Connection Status (Robust BT Connection)
        BroadcastReceiver btConnectionReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                final String action = intent.getAction();
                final BluetoothDevice mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if (action.equals(BluetoothDevice.ACTION_ACL_CONNECTED)) {
                    connectedDevice = mDevice.getName();
                    connectedState = true;
                    Log.d(TAG, "btConnectionBroadcastReceiver: Device now connected to " + mDevice.getName());
                    Toast.makeText(MainActivity.this, "Device now connected to " + mDevice.getName(), Toast.LENGTH_SHORT).show();
                    connectionStatusBox.setText("Connected to " + mDevice.getName());
                } else if (action.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)) {
                    connectedDevice = null;
                    connectedState = false;
                    Log.d(TAG, "btConnectionBroadcastReceiver: Disconnected from " + mDevice.getName());
                    Toast.makeText(MainActivity.this, "Disconnected from " + mDevice.getName(), Toast.LENGTH_SHORT).show();
                    connectionStatusBox.setText("Disconnected");

                    //start accept thread and wait on the SAME device again
                    mBluetoothConnection = new BluetoothConnectionService(MainActivity.this);
                    mBluetoothConnection.start();

                }
            }
        };

        //Overflow Menu
        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.mainController:
                    return true;

                case R.id.bluetoothSettings:
                    startActivity(new Intent(MainActivity.this, BluetoothMain.class));
                    return true;

                case R.id.stringMessages:
                    startActivity(new Intent(MainActivity.this, StringConfiguration.class));
                    return true;

                case R.id.updateBtn:
                    return true;

                default:
                    // If we got here, the user's action was not recognized.
                    // Invoke the superclass to handle it.
                    return super.onOptionsItemSelected(item);

            }
        }

        //Menu Bar
        @Override
        public boolean onCreateOptionsMenu(Menu menu) {
            // Inflate the menu; this adds items to the action bar if it is present.
            getMenuInflater().inflate(R.menu.overflowmenu, menu);
            return true;
        }


    }
