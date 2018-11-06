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

package com.android.car.dialer.ui.dialpad;

import android.content.Context;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.telecom.Call;
import android.text.TextUtils;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import com.android.car.apps.common.FabDrawable;
import com.android.car.dialer.R;
import com.android.car.dialer.log.L;
import com.android.car.dialer.telecom.TelecomUtils;
import com.android.car.dialer.telecom.UiCallManager;
import com.android.car.dialer.ui.activecall.InCallViewModel;
import com.android.car.dialer.ui.common.DialerBaseFragment;

/**
 * Fragment that controls the dialpad.
 */
public class DialpadFragment extends DialerBaseFragment implements
        KeypadFragment.KeypadCallback {
    private static final String TAG = "CD.DialpadFragment";
    private static final String DIAL_NUMBER_KEY = "DIAL_NUMBER_KEY";
    private static final String DIALPAD_MODE_KEY = "DIALPAD_MODE_KEY";
    private static final int MAX_DIAL_NUMBER = 20;

    private static final SparseIntArray sToneMap = new SparseIntArray();
    private static final SparseArray<Character> sDialValueMap = new SparseArray<>();

    private static final int TONE_LENGTH_INFINITE = -1;
    private static final int TONE_RELATIVE_VOLUME = 80;

    static {
        sToneMap.put(KeyEvent.KEYCODE_1, ToneGenerator.TONE_DTMF_1);
        sToneMap.put(KeyEvent.KEYCODE_2, ToneGenerator.TONE_DTMF_2);
        sToneMap.put(KeyEvent.KEYCODE_3, ToneGenerator.TONE_DTMF_3);
        sToneMap.put(KeyEvent.KEYCODE_4, ToneGenerator.TONE_DTMF_4);
        sToneMap.put(KeyEvent.KEYCODE_5, ToneGenerator.TONE_DTMF_5);
        sToneMap.put(KeyEvent.KEYCODE_6, ToneGenerator.TONE_DTMF_6);
        sToneMap.put(KeyEvent.KEYCODE_7, ToneGenerator.TONE_DTMF_7);
        sToneMap.put(KeyEvent.KEYCODE_8, ToneGenerator.TONE_DTMF_8);
        sToneMap.put(KeyEvent.KEYCODE_9, ToneGenerator.TONE_DTMF_9);
        sToneMap.put(KeyEvent.KEYCODE_0, ToneGenerator.TONE_DTMF_0);
        sToneMap.put(KeyEvent.KEYCODE_STAR, ToneGenerator.TONE_DTMF_S);
        sToneMap.put(KeyEvent.KEYCODE_POUND, ToneGenerator.TONE_DTMF_P);

        sDialValueMap.put(KeyEvent.KEYCODE_1, '1');
        sDialValueMap.put(KeyEvent.KEYCODE_2, '2');
        sDialValueMap.put(KeyEvent.KEYCODE_3, '3');
        sDialValueMap.put(KeyEvent.KEYCODE_4, '4');
        sDialValueMap.put(KeyEvent.KEYCODE_5, '5');
        sDialValueMap.put(KeyEvent.KEYCODE_6, '6');
        sDialValueMap.put(KeyEvent.KEYCODE_7, '7');
        sDialValueMap.put(KeyEvent.KEYCODE_8, '8');
        sDialValueMap.put(KeyEvent.KEYCODE_9, '9');
        sDialValueMap.put(KeyEvent.KEYCODE_0, '0');
        sDialValueMap.put(KeyEvent.KEYCODE_STAR, '*');
        sDialValueMap.put(KeyEvent.KEYCODE_POUND, '#');
    }

    /**
     * Shows the dialpad for an active phone call.
     */
    private static final int MODE_IN_CALL = 1;

    /**
     * Shows dialpad for dialing.
     */
    private static final int MODE_DIAL = 2;

    private TextView mTitleView;
    private int mMode;
    private StringBuffer mNumber = new StringBuffer(MAX_DIAL_NUMBER);
    private ToneGenerator mToneGenerator;
    /**
     * An active call which this DialpadFragment is serving for.
     */
    @Nullable
    private Call mActiveCall;

    /**
     * Creates a new instance of the {@link DialpadFragment} which is used for dialing a number.
     *
     * @param dialNumber The given number as the one to dial.
     */
    public static DialpadFragment newPlaceCallDialpad(@Nullable String dialNumber) {
        DialpadFragment fragment = new DialpadFragment();

        Bundle args = new Bundle();
        args.putInt(DIALPAD_MODE_KEY, MODE_DIAL);
        if (!TextUtils.isEmpty(dialNumber)) {
            args.putString(DIAL_NUMBER_KEY, dialNumber);
        }
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Returns a new instance of the {@link DialpadFragment} which runs in an active call for
     * dialing extension number, etc.
     */
    public static DialpadFragment newInCallDialpad() {
        DialpadFragment fragment = new DialpadFragment();

        Bundle args = new Bundle();
        args.putInt(DIALPAD_MODE_KEY, MODE_IN_CALL);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mToneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, TONE_RELATIVE_VOLUME);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mMode = getArguments().getInt(DIALPAD_MODE_KEY);
        L.d(TAG, "onCreateView mode: %s", mMode);
        View rootView = inflater.inflate(R.layout.dialpad_fragment, container, false);
        Fragment keypadFragment = KeypadFragment.newInstance();
        getChildFragmentManager().beginTransaction()
                .replace(R.id.dialpad_fragment_container, keypadFragment)
                .commit();

        mTitleView = rootView.findViewById(R.id.title);
        ImageButton callButton = rootView.findViewById(R.id.call_button);
        ImageButton deleteButton = rootView.findViewById(R.id.delete_button);

        if (mMode == MODE_IN_CALL) {
            mTitleView.setText("");
            deleteButton.setVisibility(View.GONE);
            callButton.setVisibility(View.GONE);
            mActiveCall = ViewModelProviders.of(getParentFragment()).get(
                    InCallViewModel.class).getPrimaryCall().getValue();
        } else {
            if (getArguments() != null && getArguments().containsKey(DIAL_NUMBER_KEY)) {
                appendDialedNumber(getArguments().getString(DIAL_NUMBER_KEY));
            } else {
                mTitleView.setText(getContext().getString(R.string.dial_a_number));
            }
            callButton.setVisibility(View.VISIBLE);
            deleteButton.setVisibility(View.VISIBLE);
            Context context = getContext();
            FabDrawable callDrawable = new FabDrawable(context);
            callDrawable.setFabAndStrokeColor(context.getColor(R.color.phone_call));
            callButton.setBackground(callDrawable);
            callButton.setOnClickListener((unusedView) -> {
                if (!TextUtils.isEmpty(mNumber.toString()) && mMode == MODE_DIAL) {
                    UiCallManager.get().placeCall(mNumber.toString());
                }
            });
            deleteButton.setOnClickListener(v -> removeLastDigit());
            deleteButton.setOnLongClickListener(v -> {
                clearDialedNumber();
                return true;
            });
        }

        return rootView;
    }

    @Override
    public void onPause() {
        super.onPause();
        mToneGenerator.stopTone();
    }

    @Override
    public void onKeyLongPressed(@KeypadFragment.DialKeyCode int keycode) {
        switch (keycode) {
            case KeyEvent.KEYCODE_0:
                removeLastDigit();
                appendDialedNumber("+");
                break;
            case KeyEvent.KEYCODE_1:
                UiCallManager.get().callVoicemail();
        }
    }

    @Override
    public void onKeyDown(@KeypadFragment.DialKeyCode int keycode) {
        String digit = sDialValueMap.get(keycode).toString();
        appendDialedNumber(digit);

        if (mActiveCall != null) {
            L.d(TAG, "start DTMF tone for %s", keycode);
            mActiveCall.playDtmfTone(sDialValueMap.get(keycode));
        } else {
            L.d(TAG, "start key pressed tone for %s", keycode);
            mToneGenerator.startTone(sToneMap.get(keycode), TONE_LENGTH_INFINITE);
        }
    }

    @Override
    public void onKeyUp(@KeypadFragment.DialKeyCode int keycode) {
        if (mActiveCall != null) {
            L.d(TAG, "stop DTMF tone");
            mActiveCall.stopDtmfTone();
        } else {
            L.d(TAG, "stop key pressed tone");
            mToneGenerator.stopTone();
        }
    }

    @StringRes
    @Override
    protected int getActionBarTitleRes() {
        return R.string.dialpad_title;
    }

    private void clearDialedNumber() {
        mNumber.setLength(0);
        mTitleView.setText(getContext().getString(R.string.dial_a_number));
    }

    private void removeLastDigit() {
        if (mNumber.length() != 0) {
            mNumber.deleteCharAt(mNumber.length() - 1);
            mTitleView.setText(TelecomUtils.getFormattedNumber(getContext(), mNumber.toString()));
        }

        if (mNumber.length() == 0 && mMode == MODE_DIAL) {
            mTitleView.setText(R.string.dial_a_number);
        }
    }

    private void appendDialedNumber(String number) {
        mNumber.append(number);
        if (mMode == MODE_DIAL && mNumber.length() < MAX_DIAL_NUMBER) {
            mTitleView.setText(TelecomUtils.getFormattedNumber(getContext(), mNumber.toString()));
        } else {
            mTitleView.setText(mNumber.toString());
        }
    }
}
