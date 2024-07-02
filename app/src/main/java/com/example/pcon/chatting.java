package com.example.pcon;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static android.view.View.TEXT_DIRECTION_LTR;
import static android.view.View.TEXT_DIRECTION_RTL;
import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;


public class chatting extends AppCompatActivity {


    UUID MY_UUID=UUID.fromString("7af83faa-fbfd-11ed-bdfd-325096b39f47");
    BluetoothDevice device;
    String address,info;
    ImageButton send;
    ListView sender;
    EditText editText;
    TextView status;
    private ArrayAdapter<String> ad;
    int i=0;
    static final int STATE_LISTENING=1;
    static final int STATE_CONNECTING=2;

    static final int STATE_CONNECTED=3;
    static final int STATE_CONNECTION_FAILED=4;
    static final int STATE_MESSAGE_RECEIVED=5;
    Button listen,connect;
    Context context = chatting.this;
    private final String NAME="pCon";
    BluetoothAdapter bluetoothAdapter;
    SendReceive sendReceive;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat_room);

       Bundle extras=getIntent().getExtras();

       info=extras.getString("deviceAddress");
        address=info.substring(info.length()-17);
        Toast.makeText(context, address, Toast.LENGTH_SHORT).show();
        editText = (EditText) findViewById(R.id.entermsg);
        sender=(ListView) findViewById(R.id.message);
       ad= new ArrayAdapter<String>(chatting.this, R.layout.device_list);
       sender.setAdapter(ad);
          send=(ImageButton) findViewById(R.id.send);
        status=(TextView) findViewById(R.id.status);
        listen=(Button) findViewById(R.id.listen);
        connect=(Button) findViewById(R.id.connect);
        bluetoothAdapter=BluetoothAdapter.getDefaultAdapter();


       implement();
    }
    Handler handler=new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message message) {
            switch (message.what)
            {
                case STATE_LISTENING:
                    status.setText("Listening......");
                    break;
                case STATE_CONNECTING:
                    status.setText("Connecting......");
                    break;
                case STATE_CONNECTED:
                    status.setText("Connected");
                    break;
                case STATE_CONNECTION_FAILED:
                    status.setText("Conection Failed!");
                    break;
                case STATE_MESSAGE_RECEIVED:
                    byte[] readBuff=(byte[]) message.obj;
                    String tempmsg=new String(readBuff,0, message.arg1);
                    sender.setTextDirection(TEXT_DIRECTION_LTR);
                    ad.add(tempmsg);
                    break;
            }
            return true;
        }
    });
    private void implement()
    {   ad.add(info);
        listen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AcceptThread acceptThread=new AcceptThread();
                 acceptThread.start();
                Toast.makeText(context, "Accept thread Started", Toast.LENGTH_SHORT).show();
            }
        });
        connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                device=bluetoothAdapter.getRemoteDevice(address);
                ConnectThread connectThread=new ConnectThread(device);
                connectThread.start();
                status.setText("Connecting....");
            }
        });
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String str=String.valueOf(editText.getText());
                sender.setTextDirection(view.TEXT_DIRECTION_RTL);
                ad.add(str);
                try {
                    sendReceive.write(str.getBytes());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                editText.setText(" ");
            }
        });
    }





    private class AcceptThread extends Thread {
        private BluetoothServerSocket mmServerSocket ;

        public AcceptThread() {


            BluetoothServerSocket tmp=null;
            try {

                if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(chatting.this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 100);
                    return;
                }

                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket's listen() method failed", e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;
            while (socket==null) {
                try {
                    Message message=Message.obtain();
                    message.what=STATE_CONNECTING;
                    handler.sendMessage(message);
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "Socket's accept() method failed", e);
                    Message message=Message.obtain();
                    message.what=STATE_CONNECTION_FAILED;
                    handler.sendMessage(message);
                    break;
                }

                if (socket != null) {
                    Message message=Message.obtain();
                    message.what=STATE_CONNECTED;
                    handler.sendMessage(message);
                    sendReceive=new SendReceive(socket);
                    sendReceive.start();
                    try {
                        mmServerSocket.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    break;
                }
            }
        }

        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }


    private class ConnectThread extends Thread {
        private BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            BluetoothSocket tmp = null;
            mmDevice = device;

            try {
                if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(chatting.this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 101);
                    return;
                }
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(chatting.this, new String[]{Manifest.permission.BLUETOOTH_SCAN}, 103);
                return;
            }
            bluetoothAdapter.cancelDiscovery();

            try {
                mmSocket.connect();
                Message message=Message.obtain();
                message.what=STATE_CONNECTED;
                handler.sendMessage(message);
                sendReceive=new SendReceive(mmSocket);
                sendReceive.start();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                Message message=Message.obtain();
                message.what=STATE_CONNECTION_FAILED;
                handler.sendMessage(message);
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }
                return;
            }



        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }
    }





    private class SendReceive extends Thread{
        private final BluetoothSocket bluetoothSocket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public SendReceive(BluetoothSocket socket)
        {
            bluetoothSocket=socket;
            InputStream tempin=null;
            OutputStream tempout=null;

            try{
                tempin=bluetoothSocket.getInputStream();
                tempout=bluetoothSocket.getOutputStream();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            inputStream=tempin;
            outputStream=tempout;
        }
        public void run()
        {
            byte[] buffer=new byte[1024];
            int bytes;
            while (true)
            {   try {
                bytes=inputStream.read(buffer);
                handler.obtainMessage(STATE_MESSAGE_RECEIVED,bytes,-1,buffer).sendToTarget();

            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            }
        }

        public void write(byte[] bytes) throws IOException {
            try {
                outputStream.write(bytes);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }

        }
    }
}

