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
package com.android.car.dialer.ui;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.car.dialer.R;
import com.android.car.dialer.telecom.UiCall;
import com.android.car.dialer.telecom.UiCallManager;

/**
 * A Fragment of the bar which controls on going call. Its host or parent Fragment is expected to
 * implement {@link OnGoingCallControllerBarCallback}.
 */
public class OnGoingCallControllerBarFragment extends Fragment {

    public static OnGoingCallControllerBarFragment newInstance() {
        return new OnGoingCallControllerBarFragment();
    }

    /**
     * Callback for control bar buttons.
     */
    public interface OnGoingCallControllerBarCallback {
        void onOpenDialpad();

        void onCloseDialpad();
    }

    private OnGoingCallControllerBarCallback mOnGoingCallControllerBarCallback;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getParentFragment() != null
                && getParentFragment() instanceof OnGoingCallControllerBarCallback) {
            mOnGoingCallControllerBarCallback =
                    (OnGoingCallControllerBarCallback) getParentFragment();
        } else if (getHost() instanceof OnGoingCallControllerBarCallback) {
            mOnGoingCallControllerBarCallback = (OnGoingCallControllerBarCallback) getHost();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View fragmentView = inflater.inflate(R.layout.on_going_call_controller_bar_fragment,
                container, false);
        fragmentView.findViewById(R.id.mute_button).setOnClickListener((v) -> {
            if (mOnGoingCallControllerBarCallback == null) {
                return;
            }
            if (v.isActivated()) {
                v.setActivated(false);
                onMuteMic();
            } else {
                v.setActivated(true);
                onUnmuteMic();
            }
        });

        fragmentView.findViewById(R.id.toggle_dialpad_button).setOnClickListener((v) -> {
            if (mOnGoingCallControllerBarCallback == null) {
                return;
            }
            if (v.isActivated()) {
                v.setActivated(false);
                mOnGoingCallControllerBarCallback.onCloseDialpad();
            } else {
                v.setActivated(true);
                mOnGoingCallControllerBarCallback.onOpenDialpad();
            }
        });

        fragmentView.findViewById(R.id.end_call_button).setOnClickListener((v) -> {
            if (mOnGoingCallControllerBarCallback == null) {
                return;
            }
            onEndCall();
        });

        fragmentView.findViewById(R.id.voice_channel_button).setOnClickListener((v) -> {
        });

        fragmentView.findViewById(R.id.pause_button).setOnClickListener((v) -> {
            if (mOnGoingCallControllerBarCallback == null) {
                return;
            }
            if (v.isActivated()) {
                v.setActivated(false);
                onHoldCall();
            } else {
                v.setActivated(true);
                onUnholdCall();
            }
        });
        return fragmentView;
    }

    private void onMuteMic() {
        UiCallManager.get().setMuted(true);
    }

    private void onUnmuteMic() {
        UiCallManager.get().setMuted(false);
    }

    private void onHoldCall() {
        UiCallManager uiCallManager = UiCallManager.get();
        UiCall primaryCall = UiCallManager.get().getPrimaryCall();
        uiCallManager.holdCall(primaryCall);
    }

    private void onUnholdCall() {
        UiCallManager uiCallManager = UiCallManager.get();
        UiCall primaryCall = UiCallManager.get().getPrimaryCall();
        uiCallManager.unholdCall(primaryCall);
    }

    private void onVoiceOutputChannelChanged(int channel) {
        // TODO: implement this function.
    }

    private void onEndCall() {
        UiCallManager uiCallManager = UiCallManager.get();
        UiCall primaryCall = UiCallManager.get().getPrimaryCall();
        uiCallManager.disconnectCall(primaryCall);
    }
}