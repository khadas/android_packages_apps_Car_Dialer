/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.car.dialer.ui;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.telecom.Call;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.android.car.dialer.bluetooth.UiBluetoothMonitor;
import com.android.car.dialer.log.L;
import com.android.car.dialer.telecom.InCallServiceImpl;
import com.android.car.dialer.ui.common.SingleLiveEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * View model for {@link TelecomActivity}.
 */
public class TelecomActivityViewModel extends AndroidViewModel implements
        InCallServiceImpl.ActiveCallListChangedCallback {
    private static final String TAG = "CD.TelecomActivityViewModel";

    private final Context mApplicationContext;
    private RefreshUiEvent mRefreshTabsLiveData;

    private final ToolbarTitleLiveData mToolbarTitleLiveData;
    private final MutableLiveData<Integer> mToolbarTitleMode;

    private final LiveData<Boolean> mHasHfpDeviceConnectedLiveData;
    private final MutableLiveData<List<Call>> mCallListLiveData;
    private final LiveData<List<Call>> mOngoingCallListLiveData;

    // Reuse the same instance so the callback won't be registered more than once.
    private final Call.Callback mRingingCallStateChangedCallback = new Call.Callback() {
        @Override
        public void onStateChanged(Call call, int state) {
            // Don't show in call activity by declining a ringing call to avoid UI flashing.
            if (state != Call.STATE_DISCONNECTED) {
                updateCallList();
            }
            call.unregisterCallback(this);
        }
    };

    private InCallServiceImpl mInCallService;
    private final ServiceConnection mInCallServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            L.d(TAG, "onServiceConnected: %s, service: %s", name, binder);
            mInCallService = ((InCallServiceImpl.LocalBinder) binder).getService();
            for (Call call : mInCallService.getCalls()) {
                if (call.getState() == Call.STATE_RINGING) {
                    call.registerCallback(mRingingCallStateChangedCallback);
                }
            }
            mInCallService.addActiveCallListChangedCallback(TelecomActivityViewModel.this);
            updateCallList();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            L.d(TAG, "onServiceDisconnected: %s", name);
            mInCallService = null;
        }
    };

    public TelecomActivityViewModel(Application application) {
        super(application);
        mApplicationContext = application.getApplicationContext();

        mToolbarTitleMode = new MediatorLiveData<>();
        mToolbarTitleLiveData = new ToolbarTitleLiveData(mApplicationContext, mToolbarTitleMode);

        if (BluetoothAdapter.getDefaultAdapter() != null) {
            mRefreshTabsLiveData = new RefreshUiEvent(
                    UiBluetoothMonitor.get().getHfpDeviceListLiveData());
        }

        mHasHfpDeviceConnectedLiveData = UiBluetoothMonitor.get().hasHfpDeviceConnected();

        mCallListLiveData = new MutableLiveData<>();
        mOngoingCallListLiveData = Transformations.map(mCallListLiveData,
                calls -> {
                    List ongoingCallList = new ArrayList();
                    for (Call call : calls) {
                        if (call.getState() != Call.STATE_RINGING) {
                            ongoingCallList.add(call);
                        }
                    }
                    return ongoingCallList;
                });
        Intent intent = new Intent(mApplicationContext, InCallServiceImpl.class);
        intent.setAction(InCallServiceImpl.ACTION_LOCAL_BIND);
        mApplicationContext.bindService(intent, mInCallServiceConnection, Context.BIND_AUTO_CREATE);
    }

    /**
     * Returns the {@link LiveData} for the toolbar title, which provides the toolbar title
     * depending on the {@link com.android.car.dialer.R.attr#toolbarTitleMode}.
     */
    public LiveData<String> getToolbarTitle() {
        return mToolbarTitleLiveData;
    }

    /**
     * Returns the {@link MutableLiveData} of the toolbar title mode. The value should be set by the
     * {@link TelecomActivity}.
     */
    public MutableLiveData<Integer> getToolbarTitleMode() {
        return mToolbarTitleMode;
    }

    /**
     * Returns the live data which monitors whether to refresh Dialer.
     */
    public LiveData<Boolean> getRefreshTabsLiveData() {
        return mRefreshTabsLiveData;
    }

    /** Returns a {@link LiveData} which monitors if there are any connected HFP devices. */
    public LiveData<Boolean> hasHfpDeviceConnected() {
        return mHasHfpDeviceConnectedLiveData;
    }

    /** Returns the live data which monitors the ongoing call list. */
    public LiveData<List<Call>> getOngoingCallListLiveData() {
        return mOngoingCallListLiveData;
    }

    @Override
    public boolean onTelecomCallAdded(Call telecomCall) {
        if (telecomCall.getState() == Call.STATE_RINGING) {
            telecomCall.registerCallback(mRingingCallStateChangedCallback);
        }
        updateCallList();
        return false;
    }

    @Override
    public boolean onTelecomCallRemoved(Call telecomCall) {
        telecomCall.unregisterCallback(mRingingCallStateChangedCallback);
        updateCallList();
        return false;
    }

    @Override
    protected void onCleared() {
        mApplicationContext.unbindService(mInCallServiceConnection);
        if (mInCallService != null) {
            for (Call call : mInCallService.getCalls()) {
                call.unregisterCallback(mRingingCallStateChangedCallback);
            }
            mInCallService.removeActiveCallListChangedCallback(this);
        }
        mInCallService = null;
    }

    private void updateCallList() {
        List<Call> callList = new ArrayList<>();
        callList.addAll(mInCallService.getCalls());
        mCallListLiveData.setValue(callList);
    }

    /**
     * This is an event live data to determine if the Ui needs to be refreshed.
     */
    @VisibleForTesting
    static class RefreshUiEvent extends SingleLiveEvent<Boolean> {
        private BluetoothDevice mBluetoothDevice;

        @VisibleForTesting
        RefreshUiEvent(LiveData<List<BluetoothDevice>> hfpDeviceListLiveData) {
            addSource(hfpDeviceListLiveData, v -> update(v));
        }

        private void update(List<BluetoothDevice> hfpDeviceList) {
            L.v(TAG, "HfpDeviceList update");
            if (mBluetoothDevice != null && !listContainsDevice(hfpDeviceList, mBluetoothDevice)) {
                setValue(true);
            }
            mBluetoothDevice = getFirstDevice(hfpDeviceList);
        }

        private boolean deviceListIsEmpty(@Nullable List<BluetoothDevice> hfpDeviceList) {
            return hfpDeviceList == null || hfpDeviceList.isEmpty();
        }

        private boolean listContainsDevice(@Nullable List<BluetoothDevice> hfpDeviceList,
                @NonNull BluetoothDevice device) {
            if (!deviceListIsEmpty(hfpDeviceList) && hfpDeviceList.contains(device)) {
                return true;
            }

            return false;
        }

        @Nullable
        private BluetoothDevice getFirstDevice(@Nullable List<BluetoothDevice> hfpDeviceList) {
            if (deviceListIsEmpty(hfpDeviceList)) {
                return null;
            } else {
                return hfpDeviceList.get(0);
            }
        }
    }
}
