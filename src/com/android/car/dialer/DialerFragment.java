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
package com.android.car.dialer;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.car.apps.common.FabDrawable;
import com.android.car.dialer.telecom.TelecomUtils;
import com.android.car.dialer.telecom.UiCallManager;
import com.android.car.dialer.ui.DialpadFragment;

/**
 * Fragment that controls the dialpad.
 */
public class DialerFragment extends Fragment implements DialpadFragment.DialpadCallback {
    private static final String TAG = "Em.DialerFragment";
    private static final String INPUT_ACTIVE_KEY = "INPUT_ACTIVE_KEY";
    private static final String DIAL_NUMBER_KEY = "DIAL_NUMBER_KEY";
    private static final String PLUS_DIGIT = "+";

    private static final int MAX_DIAL_NUMBER = 20;

    private Context mContext;
    private UiCallManager mUiCallManager;
    private final StringBuffer mNumber = new StringBuffer(MAX_DIAL_NUMBER);
    private TextView mNumberView;
    private boolean mShowInput = true;
    private Runnable mPendingRunnable;

    private DialerBackButtonListener mBackListener;

    /**
     * Interface for a class that will be notified when the back button of the dialer has been
     * clicked.
     */
    public interface DialerBackButtonListener {
        /**
         * Called when the back button has been clicked on the dialer. This action should dismiss
         * the dialer fragment.
         */
        void onDialerBackClick();
    }

    /**
     * Creates a new instance of the {@link DialerFragment} and display the given number as the one
     * to dial.
     */
    static DialerFragment newInstance(UiCallManager callManager,
            DialerBackButtonListener listener, @Nullable String dialNumber) {
        DialerFragment fragment = new DialerFragment();
        fragment.mUiCallManager = callManager;
        fragment.mBackListener = listener;

        if (!TextUtils.isEmpty(dialNumber)) {
            Bundle args = new Bundle();
            args.putString(DIAL_NUMBER_KEY, dialNumber);
            fragment.setArguments(args);
        }

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null && savedInstanceState.containsKey(INPUT_ACTIVE_KEY)) {
            mShowInput = savedInstanceState.getBoolean(INPUT_ACTIVE_KEY);
        }

        Bundle args = getArguments();
        if (args != null) {
            setDialNumber(args.getString(DIAL_NUMBER_KEY));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "onCreateView");
        }

        mContext = getContext();
        View view = inflater.inflate(R.layout.dialer_fragment, container, false);

        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "onCreateView: inflated successfully");
        }

        view.findViewById(R.id.exit_dialer_button).setOnClickListener(v -> {
            if (mBackListener != null) {
                mBackListener.onDialerBackClick();
            }
        });

        mNumberView = view.findViewById(R.id.number);

        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "mShowInput: " + mShowInput);
        }

        FabDrawable answerCallDrawable = new FabDrawable(mContext);
        answerCallDrawable.setFabAndStrokeColor(getContext().getColor(R.color.phone_call));

        View callButton = view.findViewById(R.id.call);
        callButton.setBackground(answerCallDrawable);
        callButton.setVisibility(View.VISIBLE);
        callButton.setOnClickListener((unusedView) -> {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Call button clicked, placing a call: " + mNumber.toString());
            }

            if (!TextUtils.isEmpty(mNumber.toString())) {
                mUiCallManager.safePlaceCall(mNumber.toString(), false);
            }
        });

        View deleteButton = view.findViewById(R.id.delete);
        deleteButton.setVisibility(View.VISIBLE);
        deleteButton.setOnClickListener(v -> {
            if (mNumber.length() != 0) {
                mNumber.deleteCharAt(mNumber.length() - 1);
                mNumberView.setText(getFormattedNumber(mNumber.toString()));
            }
        });
        deleteButton.setOnLongClickListener(v -> {
            // Clear all on long-press
            mNumber.delete(0, mNumber.length());
            mNumberView.setText(getFormattedNumber(mNumber.toString()));
            return true;
        });

        Fragment dialpadFragment = DialpadFragment.newInstance();
        getChildFragmentManager().beginTransaction()
                .replace(R.id.dialpad_fragment_container, dialpadFragment)
                .commit();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mPendingRunnable != null) {
            mPendingRunnable.run();
            mPendingRunnable = null;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mContext = null;
        mNumberView = null;
    }

    @Override
    public void onDialVoiceMail() {
        mUiCallManager.callVoicemail();
    }

    @Override
    public void onAppendDigit(String digit) {
        if (PLUS_DIGIT.equals(digit)) {
            mNumber.deleteCharAt(mNumber.length() - 1);
        }
        appendDigitAndUpdate(digit);
    }

    private void setDialNumber(final String number) {
        if (TextUtils.isEmpty(number)) {
            return;
        }

        if (mContext != null && mNumberView != null) {
            setDialNumberInternal(number);
        } else {
            mPendingRunnable = () -> setDialNumberInternal(number);
        }
    }

    private void setDialNumberInternal(final String number) {
        // Clear existing content in mNumber.
        mNumber.setLength(0);
        appendDigitAndUpdate(number);
    }

    private String getFormattedNumber(String number) {
        return TelecomUtils.getFormattedNumber(mContext, number);
    }

    private void appendDigitAndUpdate(String digit) {
        mNumber.append(digit);
        mNumberView.setText(getFormattedNumber(mNumber.toString()));
    }
}
