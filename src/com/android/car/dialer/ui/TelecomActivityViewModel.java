package com.android.car.dialer.ui;

import android.annotation.IntDef;
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
import com.android.car.dialer.livedata.ActiveCallListLiveData;
import com.android.car.dialer.livedata.BluetoothHfpStateLiveData;
import com.android.car.dialer.livedata.BluetoothPairListLiveData;
import com.android.car.dialer.livedata.BluetoothStateLiveData;
import com.android.car.dialer.telecom.UiBluetoothMonitor;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Set;

/**
 * View model for {@link TelecomActivity}.
 */
public class TelecomActivityViewModel extends AndroidViewModel {
    private static final String TAG = "CD.TelecomActivityViewModel";
    /** A constant which indicates that there's no Bluetooth error. */
    public static final String NO_BT_ERROR = "NO_ERROR";

    private final Context mApplicationContext;
    private final LiveData<String> mErrorStringLiveData;
    private final LiveData<Boolean> mHasOngoingCallLiveData;
    private final MutableLiveData<Integer> mDialerAppStateLiveData;

    /**
     * App state indicates if bluetooth is connected, there is ongoing call or it should just show
     * the content fragments.
     */
    @IntDef({DialerAppState.DEFAULT, DialerAppState.BLUETOOTH_ERROR,
            DialerAppState.EMERGENCY_DAILPAD})
    @Retention(RetentionPolicy.SOURCE)
    public @interface DialerAppState {
        int DEFAULT = 0;
        int BLUETOOTH_ERROR = 1;
        int EMERGENCY_DAILPAD = 2;
    }

    public TelecomActivityViewModel(Application application) {
        super(application);
        mApplicationContext = application.getApplicationContext();

        if (BluetoothAdapter.getDefaultAdapter() == null) {
            MutableLiveData<String> bluetoothUnavailableLiveData = new MutableLiveData<>();
            bluetoothUnavailableLiveData.setValue(
                    mApplicationContext.getString(R.string.bluetooth_unavailable));
            mErrorStringLiveData = bluetoothUnavailableLiveData;
        } else {
            UiBluetoothMonitor uiBluetoothMonitor = UiBluetoothMonitor.get();
            mErrorStringLiveData = new ErrorStringLiveData(
                    mApplicationContext,
                    uiBluetoothMonitor.getHfpStateLiveData(),
                    uiBluetoothMonitor.getPairListLiveData(),
                    uiBluetoothMonitor.getBluetoothStateLiveData());
        }

        mHasOngoingCallLiveData = Transformations.map(new ActiveCallListLiveData(application),
                (calls) -> !calls.isEmpty());

        mDialerAppStateLiveData = new DialerAppStateLiveData(mErrorStringLiveData,
                mHasOngoingCallLiveData);
    }

    public MutableLiveData<Integer> getDialerAppState() {
        return mDialerAppStateLiveData;
    }

    /**
     * Returns a LiveData which provides the warning string based on Bluetooth states. Returns
     * {@link #NO_BT_ERROR} if there's no error.
     */
    public LiveData<String> getErrorMessage() {
        return mErrorStringLiveData;
    }

    private static class DialerAppStateLiveData extends MediatorLiveData<Integer> {
        private final LiveData<String> mErrorStringLiveData;
        private final LiveData<Boolean> mHasOngoingCallLiveData;

        private DialerAppStateLiveData(LiveData<String> errorStringLiveData,
                LiveData<Boolean> hasOngoingCallLiveData) {
            this.mErrorStringLiveData = errorStringLiveData;
            this.mHasOngoingCallLiveData = hasOngoingCallLiveData;
            setValue(DialerAppState.DEFAULT);

            addSource(mErrorStringLiveData, errorMsg -> updateDialerAppState());
            addSource(mHasOngoingCallLiveData, hasOngoingCall -> updateDialerAppState());
        }

        private void updateDialerAppState() {
            L.d(TAG, "updateDialerAppState, error: %s, hasOngoingCall: %s",
                    mErrorStringLiveData.getValue(), mHasOngoingCallLiveData.getValue());

            // If bluetooth is not connected, user can make an emergency call. So show the in
            // call fragment no matter if bluetooth is connected or not.
            if (mHasOngoingCallLiveData.getValue() == Boolean.TRUE) {
                return;
            }

            // Bluetooth error
            if (!NO_BT_ERROR.equals(mErrorStringLiveData.getValue())) {
                // Currently bluetooth is not connected, stay on the emergency dial pad page.
                if (getValue() == DialerAppState.EMERGENCY_DAILPAD) {
                    return;
                }
                setValue(DialerAppState.BLUETOOTH_ERROR);
                return;
            }

            // Bluetooth connected and there is no ongoing call.
            setValue(DialerAppState.DEFAULT);
        }

        @Override
        public void setValue(@DialerAppState Integer newValue) {
            // Only set value and notify observers when the value changes.
            if (getValue() != newValue) {
                super.setValue(newValue);
            }
        }
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

        private void update() {
            boolean isBluetoothEnabled = isBluetoothEnabled();
            boolean hasPairedDevices = hasPairedDevices();
            boolean isHfpConnected = isHfpConnected();
            L.d(TAG, "Update error string."
                            + " isBluetoothEnabled: %s"
                            + " hasPairedDevices: %s"
                            + " isHfpConnected: %s",
                    isBluetoothEnabled,
                    hasPairedDevices,
                    isHfpConnected);
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
