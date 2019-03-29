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

import android.telecom.Call;

import androidx.lifecycle.LiveData;

import com.android.car.dialer.log.L;
import com.android.car.dialer.telecom.InCallServiceImpl;
import com.android.car.dialer.telecom.UiCallManager;

import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Monitors the change of active call list.
 */
public class ActiveCallListLiveData extends LiveData<List<Call>> implements
        InCallServiceImpl.ActiveCallListChangedCallback {
    private static final String TAG = "CD.ActiveCallsLD";

    /**
     * The rank of call state. Used for sorting active calls. Rank is listed from lowest to
     * highest
     */
    private static final List<Integer> CALL_STATE_RANK = Lists.newArrayList(
            Call.STATE_DISCONNECTED,
            Call.STATE_DISCONNECTING,
            Call.STATE_NEW,
            Call.STATE_CONNECTING,
            Call.STATE_SELECT_PHONE_ACCOUNT,
            Call.STATE_HOLDING,
            Call.STATE_ACTIVE,
            Call.STATE_DIALING,
            Call.STATE_RINGING);

    private final UiCallManager mUiCallManager;
    private final List<Call> mCalls = new ArrayList<>();

    public ActiveCallListLiveData() {
        mUiCallManager = UiCallManager.get();
    }

    @Override
    protected void onActive() {
        updateActiveCallList();
        mUiCallManager.registerActiveCallListChangedCallback(this);
    }

    @Override
    protected void onInactive() {
        mUiCallManager.unregisterActiveCallListChangedCallback(this);
    }

    @Override
    public void onTelecomCallAdded(Call telecomCall) {
        L.i(TAG, "onTelecomCallAdded %s", telecomCall);
        updateActiveCallList();
    }

    @Override
    public void onTelecomCallRemoved(Call telecomCall) {
        L.i(TAG, "onTelecomCallRemoved %s", telecomCall);
        updateActiveCallList();
    }

    private void updateActiveCallList() {
        mCalls.clear();
        mCalls.addAll(mUiCallManager.getCallList());
        mCalls.sort(mCallComparator);
        setValue(mCalls);
    }

    private Comparator<Call> mCallComparator = (call, otherCall) -> {
        boolean callHasParent = call.getParent() != null;
        boolean otherCallHasParent = otherCall.getParent() != null;

        if (callHasParent && !otherCallHasParent) {
            return 1;
        } else if (!callHasParent && otherCallHasParent) {
            return -1;
        }
        int carCallRank = CALL_STATE_RANK.indexOf(call.getState());
        int otherCarCallRank = CALL_STATE_RANK.indexOf(otherCall.getState());

        return otherCarCallRank - carCallRank;
    };
}
