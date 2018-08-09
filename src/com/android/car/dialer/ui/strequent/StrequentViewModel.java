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

package com.android.car.dialer.ui.strequent;

import android.app.Application;
import android.text.format.DateUtils;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.android.car.dialer.ContactEntry;
import com.android.car.dialer.livedata.CallHistoryLiveData;
import com.android.car.dialer.livedata.HeartBeatLiveData;
import com.android.car.dialer.livedata.StrequentLiveData;
import com.android.car.dialer.telecom.InMemoryPhoneBook;
import com.android.car.dialer.ui.common.UiCallLogLiveData;
import com.android.car.dialer.ui.common.entity.UiCallLog;

import java.util.List;

/**
 * View model for StrequentFragment.
 */
// TODO: unify strequent list and last call to remove duplicate entries.
public class StrequentViewModel extends AndroidViewModel {
    private static final String TAG = "CD.StrequentModelView";

    private LiveData<List<ContactEntry>> mStrequentLiveData;
    private LiveData<UiCallLog> mLastCallLiveData;

    public StrequentViewModel(Application application) {
        super(application);
        mStrequentLiveData = StrequentLiveData.getInstance(
                application.getApplicationContext());
        UiCallLogLiveData lastUiCallLogLiveData = new UiCallLogLiveData(
                application.getApplicationContext(),
                new HeartBeatLiveData(DateUtils.MINUTE_IN_MILLIS),
                CallHistoryLiveData.newLastCallLiveData(application.getApplicationContext()),
                InMemoryPhoneBook.get().getContactsLiveData());
        mLastCallLiveData = Transformations.map(lastUiCallLogLiveData,
                (uiCallLogs) -> uiCallLogs != null && !uiCallLogs.isEmpty() ? uiCallLogs.get(0)
                        : null);
    }

    /** Returns strequent list live data. */
    public LiveData<List<ContactEntry>> getStrequents() {
        return mStrequentLiveData;
    }

    /** Returns last call live data. */
    public LiveData<UiCallLog> getLastCall() {
        return mLastCallLiveData;
    }
}
