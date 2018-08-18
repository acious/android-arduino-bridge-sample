package io.protopie.protopiearduinobridge.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import io.protopie.protopiearduinobridge.R;

public class BLESetupActivity extends AppCompatActivity {

    final static int BLE_REQUEST_CODE = 100;
    private static final int REQUEST_BLUETOOTH_SETTING = 0xBB;

    private BluetoothAdapter mBLEAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blesetup);

        Intent bleSettingsIntent = new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
        startActivityForResult(bleSettingsIntent, REQUEST_BLUETOOTH_SETTING);
    }
}
