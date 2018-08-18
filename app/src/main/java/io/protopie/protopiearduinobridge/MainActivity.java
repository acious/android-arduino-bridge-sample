package io.protopie.protopiearduinobridge;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private static final String ACTION_CONSOLE = MainActivity.class.getCanonicalName() + ".CONSOLE";
    private static final String CONSOLE_STRING_KEY = "message";
    private static final int MAX_CONSOLE_LINES = 10;
    private static final int REQUEST_ENABLE_BT = 100;
    private static final String TAG = "MainActivity";

    private TextView console;
    private ProgressBar progressBar;
    private List<String> consoleLines = new LinkedList<>();
    private StringBuilder consoleText = new StringBuilder();


    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mJogControllerDevice;
    private BluetoothSocket mSocket;
    private OutputStream mBLEOutputStream;
    private InputStream mBLEInputStream;
    private byte b;

    public static void sendToConsole(Context context, String message) {
        Intent intent = new Intent(ACTION_CONSOLE);
        intent.putExtra(CONSOLE_STRING_KEY, message);
        context.sendBroadcast(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        console = (TextView) findViewById(R.id.console);
        console.setMovementMethod(new ScrollingMovementMethod());

        startService(new Intent(this, BridgeService.class));
        registerReceiver(broadcastReceiver, new IntentFilter(ACTION_CONSOLE));

        progressBar = findViewById(R.id.main_ble_setup_progress);
        initBLE();
    }

    private void initBLE() {
        progressBar.setVisibility(View.VISIBLE);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        checkAndTurnOnBLE();
    }

    private void checkAndTurnOnBLE() {
        if (mBluetoothAdapter == null) {
            //장치가 블루투스를 지원하지 않는 경우.
            finish();
        } else {
            // 장치가 블루투스를 지원하는 경우.
            if (!mBluetoothAdapter.isEnabled()) {
                // 장치의 블루투스가 켜져있지 않은경우.
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            } else {
                // 장치의 블루투스가 켜져있는 경우.
                findBLEDevice();
                connectToBLEDevice();
                progressBar.setVisibility(View.GONE);
                setUpListenDataFromDevice();
            }
        }
    }

    private void setUpListenDataFromDevice() {
        final Handler handler = new Handler();
        // 문자열 수신 쓰레드
        Thread mWorkerThread = new Thread(new Runnable() {
            public void run() {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        int bytesAvailable = mBLEInputStream.available();
                        if (bytesAvailable > 0) {
                            final byte[] packetBytes = new byte[bytesAvailable];
                            mBLEInputStream.read(packetBytes);
                            b = packetBytes[0];
                            handler.post(new Runnable() {
                                public void run() {
                                    packingStringFromBLEData(b);
                                }
                            });
                        }
                    } catch (IOException ex) {
                        finish();
                    }
                }
            }
        });

        mWorkerThread.start();
    }

    private void packingStringFromBLEData(byte data) {
        StringBuilder builder = new StringBuilder();
        builder.append(Character.valueOf((char) (data & 0xFF)));

        writeToConsole(builder.toString());
        ProtoPieUtils.sendToProtoPie(this, builder.toString());
    }

    private void connectToBLEDevice() {
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

        try {
            mSocket = mJogControllerDevice.createRfcommSocketToServiceRecord(uuid);
            mSocket.connect();

            mBLEOutputStream = mSocket.getOutputStream();
            mBLEInputStream = mSocket.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
            finish();
        }
    }

    private void findBLEDevice() {
        Set<BluetoothDevice> mDevices = mBluetoothAdapter.getBondedDevices();
        int mPairedDeviceCount = mDevices.size();

        if (mPairedDeviceCount == 0) {
            finish();
        }

        for (BluetoothDevice device : mDevices) {
            if (device.getName().equals("SS_Design")) {
                mJogControllerDevice = device;
                writeToConsole("connected on :" + device.getName());
            }
        }
    }

    private void writeToConsole(String message) {
        consoleLines.add(message);
        while (consoleLines.size() > MAX_CONSOLE_LINES) {
            consoleLines.remove(0);
        }

        consoleText.delete(0, consoleText.length());
        for (int i = 0; i < consoleLines.size(); i++) {
            if (i > 0) {
                consoleText.append("\n");
            }
            consoleText.append(consoleLines.get(i));
        }
        console.setText(consoleText);
    }

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_CONSOLE.equals(intent.getAction())) {
                String message = intent.getStringExtra(CONSOLE_STRING_KEY);
                if (message != null) {
                    writeToConsole(message);
                }
            }
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                if (resultCode == RESULT_OK) {
                    // 블루투스가 활성 상태로 변경됨
                    initBLE();
                } else if (resultCode == RESULT_CANCELED) {
                    // 블루투스가 비활성 상태임
                    finish();
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onDestroy() {
        try {
            mBLEInputStream.close();
            mBLEOutputStream.close();
            mSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        super.onDestroy();
    }
}
