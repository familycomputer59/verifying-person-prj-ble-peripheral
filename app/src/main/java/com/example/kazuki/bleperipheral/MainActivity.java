package com.example.kazuki.bleperipheral;

import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.util.UUID;


public class MainActivity extends Activity {

    private final static int REQUEST_ENABLE_BT = 123456;

    private BluetoothAdapter mBleAdapter = null;
    private BluetoothManager mBleManager = null;
    private BluetoothLeAdvertiser mBtAdvertiser;

    private BluetoothGattCharacteristic mBtCharacteristic;

    private BluetoothGattServer mBtGattServer = null;

    private final static int MESSAGE_NEW_RECEIVEDNUM = 0;

    private TextView mTxtUpdatedNum;

    private String mStrReceivedNum;
    private SimpleAdvertiseCallback mAdvertiseCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTxtUpdatedNum = (TextView) findViewById(R.id.peripheral_updated_num);

        // prepare using Bluetooth.
        mBleManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBleAdapter = mBleManager.getAdapter();

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            // BluetoothがOffならインテントを表示する.
            if ((mBleAdapter == null)
                    || (! mBleAdapter.isEnabled())) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                // Intentでボタンを押すとonActivityResultが実行されるので、第二引数の番号を元に処理を行う.
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
            else
            {
                this.prepareBle();
            }
        }
        else
        {
            Toast.makeText(this, R.string.advertising_not_supported, Toast.LENGTH_SHORT).show();
        }
    }
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void prepareBle()
    {
        mBtAdvertiser = mBleAdapter.getBluetoothLeAdvertiser();
        //mBtAdvertiserの確認
        if(mBtAdvertiser != null){

//            AdvertiseData.Builder dataBuilder=new AdvertiseData.Builder();
//            AdvertiseSettings.Builder settingsBuilder=new AdvertiseSettings.Builder();
//            dataBuilder.setIncludeTxPowerLevel(false);
//            dataBuilder.addServiceUuid(ParcelUuid.fromString(getResources().getString(R.string.uuid_service)));
//            settingsBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED);
//            settingsBuilder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH);
//            BluetoothLeAdvertiser bluetoothLeAdvertiser = mBleAdapter.getBluetoothLeAdvertiser();
//            bluetoothLeAdvertiser.startAdvertising(settingsBuilder.build(),dataBuilder.build()
//                    , new AdvertiseCallback(){
//                        @Override
//                        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
//                        }
//                        @Override
//                        public void onStartFailure(int errorCode) {
//
//                        }
//                    });
            startAdvertising();


        }else {
            Toast.makeText(this, R.string.advertising_not_supported, Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // execute when the user push button on the Intent.
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                if ((mBleAdapter != null)
                        || (mBleAdapter.isEnabled())) {
                    // if BLE is enabled, start advertising.
                    this.prepareBle();
                }
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAdvertising();
    }

    private BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("Peripheral", "service added " + service.getUuid().toString());
            } else {
                Log.d("Peripheral", "couldn't add service");
            }
        }
        @Override
        public void onConnectionStateChange(android.bluetooth.BluetoothDevice device, int status,
                                            int newState) {
            if(newState == BluetoothProfile.STATE_CONNECTED){
                // set connected device.
            }
        }
        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device,
                                                 int requestId,
                                                 BluetoothGattCharacteristic characteristic,
                                                 boolean preparedWrite,
                                                 boolean responseNeeded,
                                                 int offset,
                                                 byte[] value) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);

            // set written value to characteristic.
            characteristic.setValue(value);

            // update TextView.
            mStrReceivedNum = characteristic.getStringValue(offset);
            mHndBleHandler.sendEmptyMessage(MESSAGE_NEW_RECEIVEDNUM);

            if(responseNeeded){
                mBtGattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        value);
            }
        }
    };
    private Handler mHndBleHandler = new Handler()
    {
        public void handleMessage(Message msg)
        {
            // execute on UI thread.
            switch (msg.what)
            {
                case MESSAGE_NEW_RECEIVEDNUM:
                    mTxtUpdatedNum.setText(mStrReceivedNum);
                    break;
            }
        }
    };

    private void setServices() {
        //serviceUUIDを設定
        BluetoothGattService btGattService = new BluetoothGattService(UUID.fromString(getResources().getString(R.string.uuid_service)), BluetoothGattService.SERVICE_TYPE_PRIMARY);

        mBtCharacteristic = new BluetoothGattCharacteristic(
                UUID.fromString(getResources().getString(R.string.uuid_characteristic)),
                BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_WRITE);

        btGattService.addCharacteristic(mBtCharacteristic);
        mBtGattServer = mBleManager.openGattServer(this, mGattServerCallback);
        mBtGattServer.addService(btGattService);

    }
    //AdvertiseSettingsの設定
    private AdvertiseSettings buildAdvertiseSettings() {
        AdvertiseSettings.Builder settingsBuilder = new AdvertiseSettings.Builder();
        settingsBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED);
        settingsBuilder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH);
        return settingsBuilder.build();
    }

    //AdvertiseDataの設定
    private AdvertiseData buildAdvertiseData() {
        AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
        dataBuilder.setIncludeTxPowerLevel(false);
        dataBuilder.addServiceUuid(ParcelUuid.fromString(getResources().getString(R.string.uuid_service)));
        return dataBuilder.build();
    }

    //Advertiseの開始
    private void startAdvertising() {
        setServices();
        AdvertiseSettings settings = buildAdvertiseSettings();
        AdvertiseData data = buildAdvertiseData();
        mAdvertiseCallback = new SimpleAdvertiseCallback();
        mBtAdvertiser.startAdvertising(settings, data, mAdvertiseCallback);
    }

    //Advertiseの成功可否
    private class SimpleAdvertiseCallback extends AdvertiseCallback {
        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
        }

        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
        }
    }

    private void stopAdvertising() {
        if (mBtGattServer != null) {
            mBtGattServer.clearServices();
            mBtGattServer.close();
            mBtGattServer = null;
        }
        if (mBtAdvertiser != null) {
            mBtAdvertiser.stopAdvertising(mAdvertiseCallback);
            mAdvertiseCallback = null;
        }
    }
}
