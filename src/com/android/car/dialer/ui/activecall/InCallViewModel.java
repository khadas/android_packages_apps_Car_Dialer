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

package com.android.car.dialer.ui.activecall;

import android.app.Application;
import android.content.Context;
import android.telecom.Call;
import android.text.format.DateUtils;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.Transformations;

import com.android.car.telephony.common.CallDetail;
import com.android.car.dialer.livedata.ActiveCallListLiveData;
import com.android.car.dialer.livedata.CallDetailLiveData;
import com.android.car.dialer.livedata.CallStateLiveData;
import com.android.car.dialer.livedata.HeartBeatLiveData;
import com.android.car.telephony.common.TelecomUtils;

import java.util.List;

/**
 * View model for {@link InCallFragment}.
 */
public class InCallViewModel extends AndroidViewModel {
    private LiveData<List<Call>> mActiveCallLiveData;
    private LiveData<CallDetail> mCallDetailLiveData;
    private LiveData<Integer> mCallStateLiveData;
    private LiveData<String> mCallStateDescriptionLiveData;
    private LiveData<Call> mPrimaryCallLiveData;
    private Context mContext;

    public InCallViewModel(@NonNull Application application) {
        super(application);
        mContext = application.getApplicationContext();
        mActiveCallLiveData = new ActiveCallListLiveData();
        mPrimaryCallLiveData = Transformations.map(mActiveCallLiveData,
                input -> (input != null && !input.isEmpty()) ? input.get(0) : null);
        mCallDetailLiveData = Transformations.switchMap(mPrimaryCallLiveData,
                input -> input != null ? new CallDetailLiveData(input) : null);
        mCallStateLiveData = Transformations.switchMap(mPrimaryCallLiveData,
                input -> input != null ? new CallStateLiveData(input) : null);
        mCallStateDescriptionLiveData = new SelfRefreshDescriptionLiveData(mContext,
                new HeartBeatLiveData(DateUtils.SECOND_IN_MILLIS), mCallDetailLiveData,
                mCallStateLiveData);
    }

    /** Returns the live data which monitors the current active call list. */
    public LiveData<List<Call>> getCallList() {
        return mActiveCallLiveData;
    }

    /**
     * Returns the live data which monitors the primary call details.
     */
    public LiveData<CallDetail> getPrimaryCallDetail() {
        return mCallDetailLiveData;
    }

    /**
     * Returns the live data which monitors the primary call state.
     */
    public LiveData<Integer> getPrimaryCallState() {
        return mCallStateLiveData;
    }

    /**
     * Returns the live data which monitor the primary call.
     */
    public LiveData<Call> getPrimaryCall() {
        return mPrimaryCallLiveData;
    }

    /**
     * Returns the live data which represents a verbose description of the primary call. Example
     * return values include:
     * <ul>
     * <li> "Work · Connecting"
     * <li> "Main · 02:03"
     * <li> "Bluetooth disconnected"
     * </ul>
     */
    public LiveData<String> getCallStateDescription() {
        return mCallStateDescriptionLiveData;
    }

    private static class SelfRefreshDescriptionLiveData extends MediatorLiveData<String> {

        private final LiveData<CallDetail> mCallDetailLiveData;
        private final LiveData<Integer> mCallStateLiveData;
        private final Context mContext;

        SelfRefreshDescriptionLiveData(Context context,
                HeartBeatLiveData heartBeatLiveData,
                LiveData<CallDetail> callDetailLiveData,
                LiveData<Integer> callStateLiveData) {
            mContext = context;
            mCallDetailLiveData = callDetailLiveData;
            mCallStateLiveData = callStateLiveData;

            addSource(mCallStateLiveData, (trigger) -> updateDescription());
            addSource(heartBeatLiveData, (trigger) -> updateDescription());
        }

        private void updateDescription() {
            CallDetail callDetail = mCallDetailLiveData.getValue();
            Integer callState = mCallStateLiveData.getValue();
            if (callDetail != null && callState != null) {
                String newDescription = TelecomUtils.getCallInfoText(mContext,
                        callDetail, callState, callDetail.getNumber());
                String oldDescription = getValue();
                if (!newDescription.equals(oldDescription)) {
                    setValue(newDescription);
                }
            } else {
                setValue("");
            }
        }
    }
}
