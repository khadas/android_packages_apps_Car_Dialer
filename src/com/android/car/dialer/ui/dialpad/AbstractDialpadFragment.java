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

package com.android.car.dialer.ui.dialpad;

import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.KeyEvent;

import androidx.annotation.NonNull;

import com.android.car.dialer.log.L;
import com.android.car.dialer.ui.common.DialerBaseFragment;

/** Fragment that controls the dialpad. */
public abstract class AbstractDialpadFragment extends DialerBaseFragment implements
        KeypadFragment.KeypadCallback {
    private static final String TAG = "CD.AbsDialpadFragment";
    private static final String DIAL_NUMBER_KEY = "DIAL_NUMBER_KEY";
    private static final int PLAY_DTMF_TONE = 1;

    static final SparseArray<Character> sDialValueMap = new SparseArray<>();

    static {
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

    private boolean mDTMFToneEnabled;
    private final StringBuffer mNumber = new StringBuffer();

    /** Defines how the dialed number should be presented. */
    abstract void presentDialedNumber(@NonNull StringBuffer number);

    /** Plays the tone for the pressed keycode when DTMF tone enabled in settings. */
    abstract void playTone(int keycode);

    /** Stops playing the tone for the pressed keycode when DTMF tone enabled in settings. */
    abstract void stopTone();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mNumber.append(savedInstanceState.getCharSequence(DIAL_NUMBER_KEY));
        }
        L.d(TAG, "onCreate, number: %s", mNumber);
    }

    @Override
    public void onResume() {
        super.onResume();
        mDTMFToneEnabled = Settings.System.getInt(getContext().getContentResolver(),
                Settings.System.DTMF_TONE_WHEN_DIALING, 1) == PLAY_DTMF_TONE;
        L.d(TAG, "DTMF tone enabled = %s", String.valueOf(mDTMFToneEnabled));

        presentDialedNumber(mNumber);
    }

    @Override
    public void onPause() {
        super.onPause();
        stopTone();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putCharSequence(DIAL_NUMBER_KEY, mNumber);
    }

    @Override
    public void onKeyDown(@KeypadFragment.DialKeyCode int keycode) {
        String digit = sDialValueMap.get(keycode).toString();
        appendDialedNumber(digit);

        if (!mDTMFToneEnabled) {
            return;
        }

        playTone(keycode);
    }

    @Override
    public void onKeyUp(@KeypadFragment.DialKeyCode int keycode) {
        if (!mDTMFToneEnabled) {
            return;
        }
        stopTone();
    }

    /** Set the dialed number to the given number. Must be called after the fragment is added. */
    public void setDialedNumber(String number) {
        mNumber.setLength(0);
        if (!TextUtils.isEmpty(number)) {
            mNumber.append(number);
        }
        presentDialedNumber(mNumber);
    }

    void clearDialedNumber() {
        mNumber.setLength(0);
        presentDialedNumber(mNumber);
    }

    void removeLastDigit() {
        if (mNumber.length() != 0) {
            mNumber.deleteCharAt(mNumber.length() - 1);
        }
        presentDialedNumber(mNumber);
    }

    void appendDialedNumber(String number) {
        mNumber.append(number);
        presentDialedNumber(mNumber);
    }

    @NonNull
    StringBuffer getNumber() {
        return mNumber;
    }
}
