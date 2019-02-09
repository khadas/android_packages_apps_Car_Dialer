/**
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.car.dialer.ui.activecall;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.telecom.Call;

import androidx.fragment.app.FragmentActivity;

import com.android.car.dialer.R;
import com.android.car.dialer.log.L;
import com.android.car.dialer.telecom.InCallServiceImpl;

/**
 * Activity for ongoing call
 */
public class InCallActivity extends FragmentActivity implements
        InCallServiceImpl.ActiveCallListChangedCallback {
    private static final String TAG = "CD.InCallActivity";

    private Intent mServiceIntent;
    private InCallServiceImpl mInCallService;

    private ServiceConnection mInCallServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            L.i(TAG, "onServiceConnected %s", name);
            mInCallService = ((InCallServiceImpl.LocalBinder) binder).getService();
            mInCallService.addActiveCallListChangedCallback(InCallActivity.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mInCallService.removeActiveCallListChangedCallback(InCallActivity.this);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        L.d(TAG, "onCreate");

        setContentView(R.layout.in_call_activity);
        mServiceIntent = new Intent(this, InCallServiceImpl.class);
        mServiceIntent.setAction(InCallServiceImpl.ACTION_LOCAL_BIND);
        bindService(mServiceIntent, mInCallServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mInCallService != null) {
            mInCallService.removeActiveCallListChangedCallback(this);
        }
        unbindService(mInCallServiceConnection);
    }

    @Override
    public void onTelecomCallAdded(Call telecomCall) {
        // Do nothing.
    }

    @Override
    public void onTelecomCallRemoved(Call telecomCall) {
        updateOnGoingCall();
    }

    private void updateOnGoingCall() {
        L.d(TAG, "On Going Call Number: " + mInCallService.getCalls().size());
        if (mInCallService.getCalls().size() == 0) {
            finish();
        }
    }
}
