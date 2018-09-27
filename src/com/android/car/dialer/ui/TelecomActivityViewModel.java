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

import com.android.car.dialer.log.L;
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
    private static final String TAG = "CD.TelecomActivityViewModel";
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
        private LiveData<Integer> mHfpStateLiveData;
        private LiveData<Set<BluetoothDevice>> mPairedListLiveData;
        private LiveData<Integer> mBluetoothStateLiveData;

        private Context mContext;

        ErrorStringLiveData(Context context,
                BluetoothHfpStateLiveData hfpStateLiveData,
                BluetoothPairListLiveData pairListLiveData,
                BluetoothStateLiveData bluetoothStateLiveData) {
            mContext = context;
            mHfpStateLiveData = hfpStateLiveData;
            mPairedListLiveData = pairListLiveData;
            mBluetoothStateLiveData = bluetoothStateLiveData;
            setValue(NO_BT_ERROR);

            addSource(hfpStateLiveData, this::onHfpStateChanged);
            addSource(pairListLiveData, this::onPairListChanged);
            addSource(bluetoothStateLiveData, this::onBluetoothStateChanged);
        }

        private void onHfpStateChanged(Integer state) {
            update();
        }

        private void onPairListChanged(Set<BluetoothDevice> pairedDevices) {
            update();
        }

        private void onBluetoothStateChanged(Integer state) {
            update();
        }

        @Override
        protected void onActive() {
            super.onActive();
            update();
        }

        private void update() {
            boolean isBluetoothEnabled = isBluetoothEnabled();
            boolean hasPairedDevices = hasPairedDevices();
            boolean isHfpConnected = isHfpConnected();
            L.d(TAG, "Update error string."
                    + " isBluetoothEnabled : " + isBluetoothEnabled
                    + " hasPairedDevices : " + hasPairedDevices
                    + " isHfpConnected : " + isHfpConnected);
            if (!isBluetoothEnabled) {
                setValue(mContext.getString(R.string.bluetooth_disabled));
            } else if (!hasPairedDevices) {
                setValue(mContext.getString(R.string.bluetooth_unpaired));
            } else if (!isHfpConnected) {
                setValue(mContext.getString(R.string.no_hfp));
            } else {
                if (!NO_BT_ERROR.equals(getValue())) {
                    setValue(NO_BT_ERROR);
                }
            }
        }

        private boolean isHfpConnected() {
            Integer hfpState = mHfpStateLiveData.getValue();
            return hfpState == null || hfpState == BluetoothProfile.STATE_CONNECTED;
        }

        private boolean isBluetoothEnabled() {
            Integer bluetoothState = mBluetoothStateLiveData.getValue();
            return bluetoothState == null
                    || bluetoothState != BluetoothStateLiveData.BluetoothState.DISABLED;
        }

        private boolean hasPairedDevices() {
            Set<BluetoothDevice> pairedDevices = mPairedListLiveData.getValue();
            return pairedDevices == null || !pairedDevices.isEmpty();
        }
    }
}
