package com.example.pcon;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private Switch aSwitch;
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private Context context;
    private ListView list_paired_devices, list_available_devices;
    private ArrayAdapter<String> adapter_paired_devices, adapter_available_devices;
    private ProgressBar progressBar;
    private TextView show_paired;
    private TextView show_available;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(availdevs, filter);
        IntentFilter intentFilter1 = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(availdevs, intentFilter1);


        bluetoothManager = getSystemService(BluetoothManager.class);
        bluetoothAdapter = bluetoothManager.getAdapter();

        list_paired_devices = findViewById(R.id.padevlist);
        list_available_devices = findViewById(R.id.avdevlist);
        progressBar = (ProgressBar) findViewById(R.id.progress_rote);

        adapter_paired_devices = new ArrayAdapter<String>(context, R.layout.device_list);
        adapter_available_devices = new ArrayAdapter<String>(context, R.layout.device_list);

        list_paired_devices.setAdapter(adapter_paired_devices);
        list_available_devices.setAdapter(adapter_available_devices);


        aSwitch = findViewById(R.id.switch1);
        show_paired = findViewById(R.id.text_paired_dev);
        show_available=findViewById(R.id.teavaidev);

        implementListeners();


    }


    private void implementListeners() {
         bluetoothAdapter=BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            Toast.makeText(MainActivity.this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(MainActivity.this, "Bluetooth is supported", Toast.LENGTH_SHORT).show();
        }


        list_available_devices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                String info = ((TextView) view).getText().toString();
               // String address=info.substring(info.length()-17);
                Intent intent=new Intent(getApplicationContext(),chatting.class);
                intent.putExtra("deviceAddress",info);
                startActivity(intent);
            }
        });

        list_paired_devices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                String info = ((TextView) view).getText().toString();
               // String address=info.substring(info.length()-17);
                Intent intent=new Intent(getApplicationContext(),chatting.class);
                intent.putExtra("deviceAddress",info);
                startActivity(intent);
            }
        });

        aSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

                if (aSwitch.isChecked()) {

                    if (bluetoothAdapter.isEnabled()) {
                        Toast.makeText(MainActivity.this, "Bluetooth is already on", Toast.LENGTH_SHORT).show();
                    } else {
                        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 100);

                            return;
                        }
                        int REQUEST_ENABLE_BT = 2;
                        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                        bluetoothAdapter.enable();

                        Toast.makeText(MainActivity.this, "Bluetooth is on", Toast.LENGTH_SHORT).show();
                    }




                } else {

                    bluetoothAdapter.disable();
                    adapter_paired_devices.clear();
                    adapter_available_devices.clear();
                    progressBar.setVisibility(View.GONE);
                    bluetoothAdapter.cancelDiscovery();
                    Toast.makeText(MainActivity.this, "Bluetooth is off", Toast.LENGTH_SHORT).show();
                }


            }
        });



        // Toast.makeText(context, "device", Toast.LENGTH_LONG).show();

        show_paired.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 500);
                    return;
                }
                if(adapter_paired_devices.getCount()==0) {
                    Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
                    if (pairedDevices.size() > 0) {

                        // There are paired devices. Get the name and address of each paired device.
                        for (BluetoothDevice device : pairedDevices) {
                            String deviceName = device.getName();
                            String deviceHardwareAddress = device.getAddress(); // MAC address
                            // Toast.makeText(context, deviceName, Toast.LENGTH_LONG).show();

                            adapter_paired_devices.add(deviceName + "\n" + deviceHardwareAddress);

                        }
                    }
                }
            }
        });

        show_available.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                progressBar.setVisibility(View.VISIBLE);

                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 300);
                    return;
                }
                int requestCode = 1;
                Intent discoverableIntent =
                        new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 180);
                startActivityForResult(discoverableIntent, requestCode);



                if(bluetoothAdapter.isDiscovering())
                {
                    bluetoothAdapter.cancelDiscovery();
                }

                if (bluetoothAdapter.startDiscovery()) {
                    Toast.makeText(context, "device has started discovering", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    private final BroadcastReceiver availdevs = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 400);
                    return;
                }
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    String deviceName = device.getName();
                    String deviceHardwareAddress = device.getAddress(); // MAC address
                    adapter_available_devices.add(deviceName + "\n" + deviceHardwareAddress);
                }
                else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                    progressBar.setVisibility(View.GONE);
                    if (adapter_available_devices.getCount() == 0) {
                        Toast.makeText(context, "No new device found", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(context, "Click on the device to start the chat", Toast.LENGTH_LONG).show();
                    }
                }
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unregisterReceiver(availdevs);
    }

}
