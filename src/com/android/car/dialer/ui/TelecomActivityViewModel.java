package com.android.car.dialer.ui;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.android.car.dialer.R;
import com.android.car.dialer.TelecomActivity;
import com.android.car.dialer.livedata.ActiveCallListLiveData;
import com.android.car.dialer.livedata.BluetoothHfpStateLiveData;
import com.android.car.dialer.livedata.BluetoothPairListLiveData;
import com.android.car.dialer.livedata.BluetoothStateLiveData;
import com.android.car.dialer.telecom.UiBluetoothMonitor;

import java.util.Set;

/**
 * View model for {@link TelecomActivity}.
 */
public class TelecomActivityViewModel extends AndroidViewModel {
    /** A constant which indicates that there's no Bluetooth error. */
    public static final String NO_BT_ERROR = "NO_ERROR";

    private final Context mApplicationContext;
    private ErrorStringLiveData mErrorStringLiveData;
    private LiveData<Boolean> mHasOngoingCallLiveData;

    public TelecomActivityViewModel(Application application) {
        super(application);
        mApplicationContext = application.getApplicationContext();
        mHasOngoingCallLiveData = Transformations.map(
                new ActiveCallListLiveData(application.getApplicationContext()),
                (calls) -> !calls.isEmpty());
    }

    /**
     * Returns a LiveData which provides the warning string based on Bluetooth states. Returns
     * {@link #NO_BT_ERROR} if there's no error.
     */
    public LiveData<String> getErrorMessage() {
        if (BluetoothAdapter.getDefaultAdapter() == null) {
            MutableLiveData<String> bluetoothUnavailableLiveData = new MutableLiveData<>();
            bluetoothUnavailableLiveData.setValue(
                    mApplicationContext.getString(R.string.bluetooth_unavailable));
            return bluetoothUnavailableLiveData;
        }

        if (mErrorStringLiveData == null) {
            UiBluetoothMonitor uiBluetoothMonitor = UiBluetoothMonitor.get();
            mErrorStringLiveData = new ErrorStringLiveData(mApplicationContext,
                    uiBluetoothMonitor.getHfpStateLiveData(),
                    uiBluetoothMonitor.getPairListLiveData(),
                    uiBluetoothMonitor.getBluetoothStateLiveData());
        }

        return mErrorStringLiveData;
    }

    /**
     * Returns a live data which monitors whether there are any active ongoing calls.
     */
    public LiveData<Boolean> hasOngoingCall() {
        return mHasOngoingCallLiveData;
    }

    private static class ErrorStringLiveData extends MediatorLiveData<String> {
        private boolean mIsHfpConnected;
        private boolean mIsBluetoothEnabled;
        private boolean mHasPairedDevices;

        private Context mContext;

        ErrorStringLiveData(Context context,
                BluetoothHfpStateLiveData hfpStateLiveData,
                BluetoothPairListLiveData pairListLiveData,
                BluetoothStateLiveData bluetoothStateLiveData) {
            mContext = context;
            onBluetoothStateChanged(bluetoothStateLiveData.getValue());
            onPairListChanged(pairListLiveData.getValue());
            onHfpStateChanged(hfpStateLiveData.getValue());

            addSource(hfpStateLiveData, this::onHfpStateChanged);
            addSource(pairListLiveData, this::onPairListChanged);
            addSource(bluetoothStateLiveData, this::onBluetoothStateChanged);
        }

        private void onHfpStateChanged(Integer state) {
            mIsHfpConnected = state == BluetoothProfile.STATE_CONNECTED;
            if (mIsBluetoothEnabled && mHasPairedDevices && !mIsHfpConnected) {
                setValue(mContext.getString(R.string.no_hfp));
            } else {
                setValue(NO_BT_ERROR);
            }
        }

        private void onPairListChanged(Set<BluetoothDevice> pairedDevices) {
            mHasPairedDevices = pairedDevices != null && !pairedDevices.isEmpty();
            if (mIsBluetoothEnabled && !mHasPairedDevices) {
                setValue(mContext.getString(R.string.bluetooth_unpaired));
            }
        }

        private void onBluetoothStateChanged(Integer state) {
            mIsBluetoothEnabled = state == BluetoothStateLiveData.BluetoothState.ENABLED;
            if (!mIsBluetoothEnabled) {
                setValue(mContext.getString(R.string.bluetooth_disabled));
            }
        }
    }
}
