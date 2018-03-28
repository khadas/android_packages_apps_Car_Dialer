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
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.car.apps.common.FabDrawable;
import com.android.car.dialer.telecom.TelecomUtils;
import com.android.car.dialer.telecom.UiCallManager;

/**
 * Fragment that controls the dialpad.
 */
public class DialerFragment extends Fragment {
    private static final String TAG = "Em.DialerFragment";
    private static final String INPUT_ACTIVE_KEY = "INPUT_ACTIVE_KEY";
    private static final String DIAL_NUMBER_KEY = "DIAL_NUMBER_KEY";

    private static final int TONE_LENGTH_INFINITE = -1;
    private static final int TONE_RELATIVE_VOLUME = 80;
    private static final int MAX_DIAL_NUMBER = 20;

    private static final SparseIntArray mToneMap = new SparseIntArray();
    private static final SparseArray<String> mDialValueMap = new SparseArray<>();
    private static final SparseArray<Integer> mRIdMap = new SparseArray<>();

    static {
        mToneMap.put(KeyEvent.KEYCODE_1, ToneGenerator.TONE_DTMF_1);
        mToneMap.put(KeyEvent.KEYCODE_2, ToneGenerator.TONE_DTMF_2);
        mToneMap.put(KeyEvent.KEYCODE_3, ToneGenerator.TONE_DTMF_3);
        mToneMap.put(KeyEvent.KEYCODE_4, ToneGenerator.TONE_DTMF_4);
        mToneMap.put(KeyEvent.KEYCODE_5, ToneGenerator.TONE_DTMF_5);
        mToneMap.put(KeyEvent.KEYCODE_6, ToneGenerator.TONE_DTMF_6);
        mToneMap.put(KeyEvent.KEYCODE_7, ToneGenerator.TONE_DTMF_7);
        mToneMap.put(KeyEvent.KEYCODE_8, ToneGenerator.TONE_DTMF_8);
        mToneMap.put(KeyEvent.KEYCODE_9, ToneGenerator.TONE_DTMF_9);
        mToneMap.put(KeyEvent.KEYCODE_0, ToneGenerator.TONE_DTMF_0);
        mToneMap.put(KeyEvent.KEYCODE_STAR, ToneGenerator.TONE_DTMF_S);
        mToneMap.put(KeyEvent.KEYCODE_POUND, ToneGenerator.TONE_DTMF_P);

        mDialValueMap.put(KeyEvent.KEYCODE_1, "1");
        mDialValueMap.put(KeyEvent.KEYCODE_2, "2");
        mDialValueMap.put(KeyEvent.KEYCODE_3, "3");
        mDialValueMap.put(KeyEvent.KEYCODE_4, "4");
        mDialValueMap.put(KeyEvent.KEYCODE_5, "5");
        mDialValueMap.put(KeyEvent.KEYCODE_6, "6");
        mDialValueMap.put(KeyEvent.KEYCODE_7, "7");
        mDialValueMap.put(KeyEvent.KEYCODE_8, "8");
        mDialValueMap.put(KeyEvent.KEYCODE_9, "9");
        mDialValueMap.put(KeyEvent.KEYCODE_0, "0");
        mDialValueMap.put(KeyEvent.KEYCODE_STAR, "*");
        mDialValueMap.put(KeyEvent.KEYCODE_POUND, "#");

        mRIdMap.put(KeyEvent.KEYCODE_1, R.id.one);
        mRIdMap.put(KeyEvent.KEYCODE_2, R.id.two);
        mRIdMap.put(KeyEvent.KEYCODE_3, R.id.three);
        mRIdMap.put(KeyEvent.KEYCODE_4, R.id.four);
        mRIdMap.put(KeyEvent.KEYCODE_5, R.id.five);
        mRIdMap.put(KeyEvent.KEYCODE_6, R.id.six);
        mRIdMap.put(KeyEvent.KEYCODE_7, R.id.seven);
        mRIdMap.put(KeyEvent.KEYCODE_8, R.id.eight);
        mRIdMap.put(KeyEvent.KEYCODE_9, R.id.nine);
        mRIdMap.put(KeyEvent.KEYCODE_0, R.id.zero);
        mRIdMap.put(KeyEvent.KEYCODE_STAR, R.id.star);
        mRIdMap.put(KeyEvent.KEYCODE_POUND, R.id.pound);
    }

    private Context mContext;
    private UiCallManager mUiCallManager;
    private final StringBuffer mNumber = new StringBuffer(MAX_DIAL_NUMBER);
    private ToneGenerator mToneGenerator;
    private final Object mToneGeneratorLock = new Object();
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

        setupKeypadClickListeners(view);

        return view;
    }

    /**
     * The click listener for all dialpad buttons.  Reacts to touch-down and touch-up events, as
     * well as long-press for certain keys.  Mimics the behavior of the phone dialer app.
     */
    private class DialpadClickListener implements View.OnTouchListener, View.OnLongClickListener {
        private final int mTone;
        private final String mValue;

        DialpadClickListener(int keyCode) {
            mTone = mToneMap.get(keyCode);
            mValue = mDialValueMap.get(keyCode);
        }

        @Override
        public boolean onLongClick(View v) {
            switch (mValue) {
                case "0":
                    mNumber.deleteCharAt(mNumber.length() - 1);
                    appendDigitAndUpdate("+");
                    stopTone();
                    return true;
                case "1":
                    // TODO: this currently does not work (at least over bluetooth HFP), because
                    // the framework is unable to get the voicemail number. Revisit later...
                    mUiCallManager.callVoicemail();
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                appendDigitAndUpdate(mValue);
                playTone(mTone);
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                stopTone();
            }

            // Continue propagating the touch event
            return false;
        }
    }

    private void setupKeypadClickListeners(View parent) {
        final int[] keys = {
            KeyEvent.KEYCODE_1,
            KeyEvent.KEYCODE_2,
            KeyEvent.KEYCODE_3,
            KeyEvent.KEYCODE_4,
            KeyEvent.KEYCODE_5,
            KeyEvent.KEYCODE_6,
            KeyEvent.KEYCODE_7,
            KeyEvent.KEYCODE_8,
            KeyEvent.KEYCODE_9,
            KeyEvent.KEYCODE_0,
            KeyEvent.KEYCODE_STAR,
            KeyEvent.KEYCODE_POUND,
        };
        for (int key : keys) {
            DialpadClickListener clickListener = new DialpadClickListener(key);
            View v = parent.findViewById(mRIdMap.get(key));
            v.setOnTouchListener(clickListener);
            v.setOnLongClickListener(clickListener);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        synchronized (mToneGeneratorLock) {
            if (mToneGenerator == null) {
                mToneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, TONE_RELATIVE_VOLUME);
            }
        }

        if (mPendingRunnable != null) {
            mPendingRunnable.run();
            mPendingRunnable = null;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        stopTone();
        synchronized (mToneGeneratorLock) {
            if (mToneGenerator != null) {
                mToneGenerator.release();
                mToneGenerator = null;
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mContext = null;
        mNumberView = null;
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

    private void playTone(int tone) {
        synchronized (mToneGeneratorLock) {
            if (mToneGenerator == null) {
                Log.w(TAG, "playTone: mToneGenerator == null, tone: " + tone);
                return;
            }

            // Start the new tone
            mToneGenerator.startTone(tone, TONE_LENGTH_INFINITE);
        }
    }

    private void stopTone() {
        synchronized (mToneGeneratorLock) {
            if (mToneGenerator == null) {
                Log.w(TAG, "stopTone: mToneGenerator == null");
                return;
            }
            mToneGenerator.stopTone();
        }
    }

    private String getFormattedNumber(String number) {
        return TelecomUtils.getFormattedNumber(mContext, number);
    }

    private void appendDigitAndUpdate(String digit) {
        mNumber.append(digit);
        mNumberView.setText(getFormattedNumber(mNumber.toString()));
    }
}

