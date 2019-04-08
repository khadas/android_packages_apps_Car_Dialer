/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.android.car.dialer.telecom;

import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Process;
import android.telecom.Call;
import android.telecom.CallAudioState;
import android.telecom.InCallService;

import com.android.car.dialer.log.L;
import com.android.car.dialer.notification.InCallNotificationController;
import com.android.car.dialer.ui.activecall.InCallActivity;

import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * An implementation of {@link InCallService}. This service is bounded by android telecom and
 * {@link UiCallManager}. For incoming calls it will launch Dialer app.
 */
public class InCallServiceImpl extends InCallService {
    private static final String TAG = "CD.InCallService";

    /** An action which indicates a bind is from local component. */
    public static final String ACTION_LOCAL_BIND = "local_bind";

    private CopyOnWriteArrayList<Callback> mCallbacks = new CopyOnWriteArrayList<>();

    private ArrayList<ActiveCallListChangedCallback> mActiveCallListChangedCallbacks =
            new ArrayList<>();

    private InCallNotificationController mInCallNotificationController;

    private Handler mMainHanlder = new Handler(Looper.getMainLooper());

    /**
     * Listens on active call list changes. Callbacks will be called on main thread.
     */
    public interface ActiveCallListChangedCallback {

        /**
         * Called when a new call is added.
         */
        void onTelecomCallAdded(Call telecomCall);

        /**
         * Called when an existing call is removed.
         */
        void onTelecomCallRemoved(Call telecomCall);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mInCallNotificationController = InCallNotificationController.get();
    }

    @Override
    public void onCallAdded(Call telecomCall) {
        super.onCallAdded(telecomCall);
        L.d(TAG, "onCallAdded: %s", telecomCall);

        telecomCall.registerCallback(mCallListener);
        mCallListener.onStateChanged(telecomCall, telecomCall.getState());

        for (Callback callback : mCallbacks) {
            callback.onTelecomCallAdded(telecomCall);
        }

        for (ActiveCallListChangedCallback activeCallListChangedCallback :
                mActiveCallListChangedCallbacks) {
            activeCallListChangedCallback.onTelecomCallAdded(telecomCall);
        }
    }

    @Override
    public void onCallRemoved(Call telecomCall) {
        L.d(TAG, "onCallRemoved: %s", telecomCall);
        for (Callback callback : mCallbacks) {
            callback.onTelecomCallRemoved(telecomCall);
        }

        for (ActiveCallListChangedCallback activeCallListChangedCallback :
                mActiveCallListChangedCallbacks) {
            activeCallListChangedCallback.onTelecomCallRemoved(telecomCall);
        }
        telecomCall.unregisterCallback(mCallListener);
        super.onCallRemoved(telecomCall);
    }

    @Override
    public IBinder onBind(Intent intent) {
        L.d(TAG, "onBind: %s", intent);
        return ACTION_LOCAL_BIND.equals(intent.getAction())
                ? new LocalBinder()
                : super.onBind(intent);
    }

    private final Call.Callback mCallListener = new Call.Callback() {
        @Override
        public void onStateChanged(Call call, int state) {
            L.d(TAG, "onStateChanged call: %s, state: %s", call, state);

            if (state == Call.STATE_RINGING) {
                mInCallNotificationController.showInCallNotification(call);
            } else {
              if (state != Call.STATE_DISCONNECTED) {
                // Launch the InCallActivity for on going phone calls.
                Intent launchIntent = new Intent(getApplicationContext(), InCallActivity.class);
                startActivity(launchIntent);
              }
              mInCallNotificationController.cancelInCallNotification(call);
            }
        }
    };

    @Override
    public boolean onUnbind(Intent intent) {
        L.d(TAG, "onUnbind, intent: %s", intent);
        if (ACTION_LOCAL_BIND.equals(intent.getAction())) {
            return false;
        }
        return super.onUnbind(intent);
    }

    @Override
    public void onCallAudioStateChanged(CallAudioState audioState) {
        for (Callback callback : mCallbacks) {
            callback.onCallAudioStateChanged(audioState);
        }
    }

    public void registerCallback(Callback callback) {
        mCallbacks.add(callback);
    }

    public void unregisterCallback(Callback callback) {
        mCallbacks.remove(callback);
    }

    public void addActiveCallListChangedCallback(ActiveCallListChangedCallback callback) {
        mMainHanlder.post(() -> mActiveCallListChangedCallbacks.add(callback));
    }

    public void removeActiveCallListChangedCallback(ActiveCallListChangedCallback callback) {
        mMainHanlder.post(() -> mActiveCallListChangedCallbacks.remove(callback));
    }

    @Deprecated
    interface Callback {
        void onTelecomCallAdded(Call telecomCall);

        void onTelecomCallRemoved(Call telecomCall);

        void onCallAudioStateChanged(CallAudioState audioState);
    }

    /**
     * Local binder only available for Car Dialer package.
     */
    public class LocalBinder extends Binder {

        /**
         * Returns a reference to {@link InCallServiceImpl}. Any process other than Dialer
         * process won't be able to get a reference.
         */
        public InCallServiceImpl getService() {
            if (getCallingPid() == Process.myPid()) {
                return InCallServiceImpl.this;
            }
            return null;
        }
    }
}
