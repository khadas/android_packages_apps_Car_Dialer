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
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.android.car.dialer.ContactEntry;
import com.android.car.dialer.livedata.LastCallLiveData;
import com.android.car.dialer.livedata.StrequentLiveData;
import com.android.car.dialer.log.L;
import com.android.car.dialer.ui.CallLogListingTask;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * View model for StrequentFragment.
 */
// TODO: unify strequent list and last call to remove duplicate entries.
public class StrequentViewModel extends AndroidViewModel {
    private static final String TAG = "CD.StrequentModelView";

    private LiveData<List<ContactEntry>> mStrequentLiveData;
    private LiveData<CallLogListingTask.CallLogItem> mLastCallLiveData;

    public StrequentViewModel(Application application) {
        super(application);
        mStrequentLiveData = StrequentLiveData.getInstance(
                application.getApplicationContext());
        mLastCallLiveData = new PeriodicUpdateLastCallLiveData(
                new LastCallLiveData(application.getApplicationContext()));
    }

    /** Returns strequent list live data. */
    public LiveData<List<ContactEntry>> getStrequents() {
        return mStrequentLiveData;
    }

    /** Returns last call live data. */
    public LiveData<CallLogListingTask.CallLogItem> getLastCall() {
        return mLastCallLiveData;
    }

    /**
     * Periodic updates the last call to make sure relative call time is correct.
     */
    private static class PeriodicUpdateLastCallLiveData extends
            MediatorLiveData<CallLogListingTask.CallLogItem> {
        private static final long LAST_CALL_REFRESH_INTERVAL_MILLIS = TimeUnit.MINUTES.toMillis(1L);
        private Handler mMainThreadHandler = new Handler(Looper.getMainLooper());
        private Runnable mTickRunnable = new Runnable() {
            @Override
            public void run() {
                if (getValue() != null) {
                    setValue(getValue());
                    mMainThreadHandler.postDelayed(this, LAST_CALL_REFRESH_INTERVAL_MILLIS);
                }
            }
        };

        PeriodicUpdateLastCallLiveData(LastCallLiveData lastCallLiveData) {
            addSource(lastCallLiveData, this::onLastCallUpdate);
        }

        @Override
        protected void onActive() {
            super.onActive();
            startTick();
        }

        @Override
        protected void onInactive() {
            super.onInactive();
            stopTick();
        }

        private void onLastCallUpdate(List<CallLogListingTask.CallLogItem> lastCall) {
            L.d(TAG, "onLastCallUpdate");
            if (!lastCall.isEmpty()) {
                CallLogListingTask.CallLogItem oldValue = getValue();
                setValue(lastCall.get(0));
                if (oldValue == null) {
                    startTick();
                }
            } else {
                setValue(null);
            }
        }

        private void startTick() {
            if (getValue() != null) {
                L.d(TAG, "Relative time updater starts ticking");
                mMainThreadHandler.postDelayed(mTickRunnable, LAST_CALL_REFRESH_INTERVAL_MILLIS);
            }
        }

        private void stopTick() {
            mMainThreadHandler.removeCallbacks(mTickRunnable);
        }
    }
}
