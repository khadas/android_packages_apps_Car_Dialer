/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.car.dialer.livedata;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.telecom.Call;

import androidx.lifecycle.LiveData;

import com.android.car.dialer.telecom.InCallServiceImpl;
import com.android.car.dialer.telecom.UiCall;

import java.util.ArrayList;
import java.util.List;

/**
 * Monitors the change of active call list.
 */
public class ActiveCallListLiveData extends LiveData<List<UiCall>> implements
        InCallServiceImpl.ActiveCallListChangedCallback {

    private Context mContext;
    private Intent mServiceIntent;
    private InCallServiceImpl mInCallService;
    private List<UiCall> mUiCalls = new ArrayList<>();

    private ServiceConnection mInCallServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            mInCallService = ((InCallServiceImpl.LocalBinder) binder).getService();
            mInCallService.addActiveCallListChangedCallback(ActiveCallListLiveData.this);
            updateActiveCallList();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mInCallService.removeActiveCallListChangedCallback(ActiveCallListLiveData.this);
        }
    };

    public ActiveCallListLiveData(Context context) {
        mContext = context;
        mServiceIntent = new Intent(mContext, InCallServiceImpl.class);
        mServiceIntent.setAction(InCallServiceImpl.ACTION_LOCAL_BIND);
    }

    @Override
    protected void onActive() {
        mContext.bindService(mServiceIntent, mInCallServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onInactive() {
        mInCallService.removeActiveCallListChangedCallback(this);
        mContext.unbindService(mInCallServiceConnection);
    }

    @Override
    public void onTelecomCallAdded(Call telecomCall) {
        updateActiveCallList();
        setValue(mUiCalls);
    }

    @Override
    public void onTelecomCallRemoved(Call telecomCall) {
        updateActiveCallList();
        setValue(mUiCalls);
    }

    private void updateActiveCallList() {
        mUiCalls.clear();
        if (mInCallService != null) {
            for (Call call : mInCallService.getCalls()) {
                mUiCalls.add(UiCall.createFromTelecomCall(call));
            }
        }
    }
}
