package com.mdp.t16_android;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.nio.charset.Charset;
import java.util.UUID;

public class StringConfiguration extends AppCompatActivity {

    private static final String TAG = "StringConfiguration";

    //Declarations
    Button retrieveSavedCommandsBtn, f1Btn, f2Btn, saveCommandsBtn, resetCommandsBtn;
    EditText f1Text, f2Text;
    SharedPreferences myPreferences;
    public static final String mypreference="mypref";
    public static final String F1="f1";
    public static final String F2="f2";

    // For bluetooth connection status
    private static final UUID MY_UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    //private UUID deviceUUID;
    BluetoothConnectionService mBluetoothConnection;
    BluetoothDevice mDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.string_configuration);

        //Register Broadcast Receiver for incoming bluetooth message
        LocalBroadcastManager.getInstance(this).registerReceiver(btConnectionReceiver, new IntentFilter("btConnectionStatus"));

        //Broadcasts when bluetooth state changes (connected, disconnected etc)
        IntentFilter filter2 = new IntentFilter();
        filter2.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter2.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        filter2.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(btConnectionReceiver, filter2);

        // GUI Buttons
        saveCommandsBtn = (Button) findViewById(R.id.saveCommandsBtn);
        resetCommandsBtn = (Button) findViewById(R.id.resetCommandsBtn);
        retrieveSavedCommandsBtn = (Button) findViewById(R.id.retrieveSavedCommandsBtn);
        f1Btn = (Button) findViewById(R.id.f1Btn);
        f2Btn = (Button) findViewById(R.id.f2Btn);

        // Use Shared Preferences to save string commands
        myPreferences=getSharedPreferences(mypreference, Context.MODE_PRIVATE);

        init();
        onClickF1();
        onClickF2();

        // Menu Bar
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        if (getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }

    // On initialisation
    private void init() {
        f1Text = (EditText) findViewById(R.id.f1Text);
        f2Text = (EditText) findViewById(R.id.f2Text);
    }

    // Save string commands using Shared Preferences Editor
    public void Save(View view) {
        SharedPreferences.Editor editor = myPreferences.edit();
        editor.putString(F1, f1Text.getText().toString());
        editor.putString(F2, f2Text.getText().toString());
        editor.apply();

        Toast.makeText(this, "F1 & F2 Commands Saved!", Toast.LENGTH_SHORT).show();
    }

    // Reset saved string commands using Shared Preferences Editor
    public void Reset(View view) {
        SharedPreferences.Editor editor = myPreferences.edit();
        f1Text.setText("");
        f2Text.setText("");
        editor.clear();
        editor.apply();

        Toast.makeText(StringConfiguration.this, "Commands reset!", Toast.LENGTH_SHORT).show();
    }

    // Retrieve and display previously saved string commands using Shared Preferences
    public void Retrieve (View view) {
        Log.d(TAG,"Retrieval Start");
        String str_f1 = myPreferences.getString(F1, "");
        f1Text.setText(str_f1);
        String str_f2 = myPreferences.getString(F2, "");
        f2Text.setText(str_f2);
        if (myPreferences.contains(F1)){
            f1Text.setText(myPreferences.getString(F1,"String Not Found"));
        }
        if (myPreferences.contains(F2)){
            f2Text.setText(myPreferences.getString(F2,"String Not Found"));
        }
        Log.d(TAG,"Retrieval End");
        Toast.makeText(StringConfiguration.this, "Commands Retrieved!", Toast.LENGTH_SHORT).show();
    }

    // Outgoing message for string command F1
    public void onClickF1(){

        f1Btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String tempF1 = myPreferences.getString(F1, "");
                byte[] bytes = f1Text.getText().toString().getBytes(Charset.defaultCharset());
                BluetoothConnectionService.write(bytes);

                Log.d(TAG, "Outgoing F1 string command: " + f1Text);

                Toast.makeText(StringConfiguration.this, "F1 string command sent.", Toast.LENGTH_SHORT).show();
            }
        });

    }

    // Outgoing message for string command F2
    public void onClickF2() {

        f2Btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String tempF2 = myPreferences.getString(F2, "");
                byte[] bytes = f2Text.getText().toString().getBytes(Charset.defaultCharset());
                BluetoothConnectionService.write(bytes);

                Log.d(TAG, "Outgoing F2 string command: " + f2Text);

                Toast.makeText(StringConfiguration.this, "F2 string command sent.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    //Overflow Menu
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.mainController:
                startActivity(new Intent(StringConfiguration.this, MainActivity.class));
                return true;

            case R.id.bluetoothSettings:
                startActivity(new Intent(StringConfiguration.this, BluetoothMain.class));
                return true;

            case R.id.stringMessages:
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

    //Broadcast Receiver for Bluetooth Connection Status (Robust BT Connection)
    BroadcastReceiver btConnectionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            final String action = intent.getAction();
            final BluetoothDevice mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            if(action.equals(BluetoothDevice.ACTION_ACL_CONNECTED)){
                Log.d(TAG, "btconnectionBroadcastReceiver: Device now connected to "+mDevice.getName());
                Toast.makeText(StringConfiguration.this, "Device now connected to "+mDevice.getName(), Toast.LENGTH_SHORT).show();
                //tvConnectionStatus.setText("Connection Status: Connected to " +mDevice.getName());
            }
            else
            if(action.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)) {
                Log.d(TAG, "btConnectionBroadcastReceiver: Disconnected from "+mDevice.getName());
                Toast.makeText(StringConfiguration.this, "Disconnected from "+mDevice.getName(), Toast.LENGTH_SHORT).show();
                //tvConnectionStatus.setText("Connection Status: Disconnected");

                //start accept thread and wait on the SAME device again
                mBluetoothConnection = new BluetoothConnectionService(StringConfiguration.this);
                mBluetoothConnection.start();

            }
        }
    };
}
