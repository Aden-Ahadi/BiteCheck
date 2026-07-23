package com.example.bitecheck.ui;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.bitecheck.R;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Bluetooth screen : enable BT + list paired devices. */
public class DevicesActivity extends BaseSecondaryActivity {

    private static final int REQUEST_BT_PERMISSION = 61;

    private BluetoothAdapter adapter;
    private TextView statusText;
    private MaterialButton actionButton;

    @Override
    protected int getContentLayoutId() {
        return R.layout.content_devices;
    }

    @Override
    protected CharSequence getScreenTitle() {
        return getString(R.string.nav_devices);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        statusText = findViewById(R.id.text_bt_status);
        actionButton = findViewById(R.id.btn_bt_action);

        BluetoothManager manager = getSystemService(BluetoothManager.class);
        adapter = manager != null ? manager.getAdapter() : null;

        actionButton.setOnClickListener(v -> {
            if (!hasBtPermission()) {
                requestBtPermission();
            } else if (adapter != null && !adapter.isEnabled()) {
                startActivity(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
            } else {
                refresh();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        if (adapter == null) {
            statusText.setText(R.string.devices_not_supported);
            actionButton.setEnabled(false);
            return;
        }
        if (!hasBtPermission()) {
            statusText.setText(R.string.devices_permission_needed);
            actionButton.setText(R.string.devices_grant_permission);
            return;
        }
        if (!adapter.isEnabled()) {
            statusText.setText(R.string.devices_bt_off);
            actionButton.setText(R.string.devices_enable_bt);
            return;
        }
        statusText.setText(R.string.devices_bt_on);
        actionButton.setText(R.string.devices_refresh);
        listPairedDevices();
    }

    private void listPairedDevices() {
        List<String> names = new ArrayList<>();
        try {
            Set<BluetoothDevice> devices = adapter.getBondedDevices();
            for (BluetoothDevice device : devices) {
                names.add(device.getName() + "\n" + device.getAddress());
            }
        } catch (SecurityException ignored) {
            // Permission revoked mid-flight; the empty state below covers it.
        }
        if (names.isEmpty()) {
            names.add(getString(R.string.devices_none_paired));
        }
        ListView list = findViewById(R.id.list_devices);
        list.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, names));
    }

    private boolean hasBtPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(this,
                    Manifest.permission.BLUETOOTH_CONNECT)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private void requestBtPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                    REQUEST_BT_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BT_PERMISSION) {
            refresh();
        }
    }
}
