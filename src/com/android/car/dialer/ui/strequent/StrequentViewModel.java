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

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.android.car.dialer.ContactEntry;
import com.android.car.dialer.livedata.StrequentLiveData;

import java.util.List;

/**
 * View model for StrequentFragment.
 */
public class StrequentViewModel extends AndroidViewModel {
    private LiveData<List<ContactEntry>> mStrequentLiveData;

    public StrequentViewModel(Application application) {
        super(application);
        mStrequentLiveData = new StrequentLiveData(application);
    }

    public LiveData<List<ContactEntry>> getStrequents() {
        return mStrequentLiveData;
    }
}
