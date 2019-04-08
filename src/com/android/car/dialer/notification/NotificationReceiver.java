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

package com.android.car.dialer.notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telecom.Call;
import android.text.TextUtils;

import com.android.car.dialer.telecom.UiCallManager;

import java.util.List;

/**
 * A {@link BroadcastReceiver} that is used to handle actions from notifications to answer or
 * inject an incoming call.
 */
public class NotificationReceiver extends BroadcastReceiver {
     static final String ACTION_ANSWER_CALL = "CD.ACTION_ANSWER_CALL";
     static final String ACTION_DECLINE_CALL = "CD.ACTION_DECLINE_CALL";
     static final String EXTRA_CALL_ID = "CD.EXTRA_CALL_ID";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        String callId = intent.getStringExtra(EXTRA_CALL_ID);
        switch (action) {
            case ACTION_ANSWER_CALL:
                answerCall(callId);
                break;
            case ACTION_DECLINE_CALL:
                declineCall(callId);
                break;
            default:
                break;
        }
    }

    private void answerCall(String callId) {
        List<Call> callList = UiCallManager.get().getCallList();
        for (Call call : callList) {
            if (call.getDetails() != null && TextUtils.equals(call.getDetails().getTelecomCallId(),
                    callId)) {
                call.answer(/* videoState= */0);
                return;
            }
        }
    }

    private void declineCall(String callId) {
        List<Call> callList = UiCallManager.get().getCallList();
        for (Call call : callList) {
            if (call.getDetails() != null && TextUtils.equals(call.getDetails().getTelecomCallId(),
                    callId)) {
                call.reject(false, /* textMessage= */"");
                return;
            }
        }
    }
}
