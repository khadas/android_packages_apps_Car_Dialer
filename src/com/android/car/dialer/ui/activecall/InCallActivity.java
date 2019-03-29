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

import android.os.Bundle;
import android.telecom.Call;

import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProviders;

import com.android.car.dialer.R;
import com.android.car.dialer.log.L;

import java.util.List;

/**
 * Activity for ongoing call
 */
public class InCallActivity extends FragmentActivity {
    private static final String TAG = "CD.InCallActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        L.d(TAG, "onCreate");

        setContentView(R.layout.in_call_activity);

        InCallViewModel inCallViewModel = ViewModelProviders.of(this).get(InCallViewModel.class);
        inCallViewModel.getCallList().observe(this, this::updateOnGoingCall);
    }

    private void updateOnGoingCall(List<Call> calls) {
        if (calls == null || calls.isEmpty()) {
            L.d(TAG, "Finish InCallActivity");
            finish();
        }
    }
}
