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
import android.telecom.Call;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.android.car.apps.common.FabDrawable;
import com.android.car.dialer.R;
import com.android.car.dialer.telecom.TelecomUtils;
import com.android.car.dialer.telecom.UiCall;
import com.android.car.dialer.telecom.UiCallManager;

/**
 * Holds dialer information such as dialed number and shows proper action based on current call
 * state such as call/mute.
 */
public class DialerInfoFragment extends Fragment {
    private static final String DIAL_NUMBER_KEY = "DIAL_NUMBER_KEY";
    private static final int MAX_DIAL_NUMBER = 20;

    private TextView mTitleView;
    private TextView mBodyView;

    private ImageButton mCallButton;
    private ImageButton mDeleteButton;

    private ImageButton mEndCallButton;
    private ImageButton mMuteButton;

    private final StringBuffer mNumber = new StringBuffer(MAX_DIAL_NUMBER);

    public static DialerInfoFragment newInstance(@Nullable String dialNumber) {
        DialerInfoFragment fragment = new DialerInfoFragment();

        if (!TextUtils.isEmpty(dialNumber)) {
            Bundle args = new Bundle();
            args.putString(DIAL_NUMBER_KEY, dialNumber);
            fragment.setArguments(args);
        }

        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View fragmentView = inflater.inflate(R.layout.dialer_info_fragment, container, false);
        mTitleView = fragmentView.findViewById(R.id.title);
        mBodyView = fragmentView.findViewById(R.id.body);
        mCallButton = fragmentView.findViewById(R.id.call_button);
        mDeleteButton = fragmentView.findViewById(R.id.delete_button);
        mEndCallButton = fragmentView.findViewById(R.id.end_call_button);
        mMuteButton = fragmentView.findViewById(R.id.mute_button);

        FabDrawable answerCallDrawable = new FabDrawable(getContext());
        answerCallDrawable.setFabAndStrokeColor(getContext().getColor(R.color.phone_call));
        mCallButton.setBackground(answerCallDrawable);
        mCallButton.setOnClickListener((unusedView) -> {
            if (!TextUtils.isEmpty(mNumber.toString())) {
                UiCallManager.get().safePlaceCall(mNumber.toString(), false);
            }
        });
        mDeleteButton.setOnClickListener(v -> {
            removeLastDigit();
        });
        mDeleteButton.setOnLongClickListener(v -> {
            // Clear all on long-press
            clearDialedNumber();
            return true;
        });

        updateView();

        Bundle args = getArguments();
        if (args != null) {
            clearDialedNumber();
            appendDialedNumber(args.getString(DIAL_NUMBER_KEY));
        }

        return fragmentView;
    }

    /**
     * Append more number to the end of dialed number.
     */
    public void appendDialedNumber(String number) {
        mNumber.append(number);
        mTitleView.setText(getFormattedNumber(mNumber.toString()));
    }

    /**
     * Remove last digit of the dialed number. If there's no number left to delete, there's no
     * operation to be take.
     */
    public void removeLastDigit() {
        if (mNumber.length() != 0) {
            mNumber.deleteCharAt(mNumber.length() - 1);
            mTitleView.setText(getFormattedNumber(mNumber.toString()));
        }
    }

    private void updateView() {
        UiCall onGoingCall = UiCallManager.get().getPrimaryCall();
        if (onGoingCall == null) {
            showPreDialUi();
        } else if (onGoingCall.getState() == Call.STATE_CONNECTING) {
            showDialingUi(onGoingCall);
        } else if (onGoingCall.getState() == Call.STATE_ACTIVE) {
            showInCallUi();
        }
    }

    private void showPreDialUi() {
        mCallButton.setVisibility(View.VISIBLE);
        mDeleteButton.setVisibility(View.VISIBLE);

        mEndCallButton.setVisibility(View.GONE);
        mMuteButton.setVisibility(View.GONE);
    }

    private void showDialingUi(UiCall uiCall) {
        FabDrawable answerCallDrawable = new FabDrawable(getContext());
        answerCallDrawable.setFabAndStrokeColor(getContext().getColor(R.color.phone_end_call));
        mEndCallButton.setBackground(answerCallDrawable);
        mEndCallButton.setVisibility(View.VISIBLE);
        mMuteButton.setVisibility(View.VISIBLE);
        mBodyView.setVisibility(View.VISIBLE);

        mDeleteButton.setVisibility(View.GONE);
        mCallButton.setVisibility(View.GONE);
    }

    private void showInCallUi() {
        // TODO: Implement this function.
    }

    private String getFormattedNumber(String number) {
        return TelecomUtils.getFormattedNumber(getContext(), number);
    }

    private void clearDialedNumber() {
        mNumber.setLength(0);
        mTitleView.setText(getFormattedNumber(mNumber.toString()));
    }
}
