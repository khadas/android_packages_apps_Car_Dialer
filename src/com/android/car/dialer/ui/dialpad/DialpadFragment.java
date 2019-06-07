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

import android.app.ActionBar;
import android.content.Context;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.CallLog;
import android.provider.Settings;
import android.telecom.Call;
import android.text.TextUtils;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.Guideline;
import androidx.lifecycle.ViewModelProviders;

import com.android.car.dialer.R;
import com.android.car.dialer.log.L;
import com.android.car.dialer.telecom.UiCallManager;
import com.android.car.dialer.ui.activecall.InCallViewModel;
import com.android.car.dialer.ui.common.DialerBaseFragment;
import com.android.car.telephony.common.Contact;
import com.android.car.telephony.common.InMemoryPhoneBook;
import com.android.car.telephony.common.TelecomUtils;

import com.google.common.annotations.VisibleForTesting;

/**
 * Fragment that controls the dialpad.
 */
public class DialpadFragment extends DialerBaseFragment implements
        KeypadFragment.KeypadCallback {
    private static final String TAG = "CD.DialpadFragment";
    private static final String DIAL_NUMBER_KEY = "DIAL_NUMBER_KEY";
    private static final String DIALPAD_MODE_KEY = "DIALPAD_MODE_KEY";
    @VisibleForTesting
    static final int MAX_DIAL_NUMBER = 20;

    private static final SparseIntArray sToneMap = new SparseIntArray();
    private static final SparseArray<Character> sDialValueMap = new SparseArray<>();

    private static final int TONE_LENGTH_INFINITE = -1;
    private static final int TONE_RELATIVE_VOLUME = 80;
    private static final int PLAY_DTMF_TONE = 1;

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

    /** Shows dialpad for dialing. */
    private static final int MODE_DIAL = 2;

    private static final int MODE_EMERGENCY = 3;

    private TextView mTitleView;
    private TextView mDisplayName;
    private Chronometer mCallStateView;
    private ImageButton mDeleteButton;
    private int mMode;
    private StringBuffer mNumber = new StringBuffer(MAX_DIAL_NUMBER);
    private ToneGenerator mToneGenerator;
    private boolean mDTMFToneEnabled;

    /** An active call which this DialpadFragment is serving for. */
    @Nullable
    private Call mActiveCall;

    /**
     * Creates a new instance of the {@link DialpadFragment} which is used for dialing a number.
     */
    public static DialpadFragment newPlaceCallDialpad() {
        DialpadFragment fragment = newDialpad(MODE_DIAL);
        return fragment;
    }

    /** Creates a new instance used for emergency dialing. */
    public static DialpadFragment newEmergencyDialpad() {
        return newDialpad(MODE_EMERGENCY);
    }

    /**
     * Returns a new instance of the {@link DialpadFragment} which runs in an active call for
     * dialing extension number, etc.
     */
    public static DialpadFragment newInCallDialpad() {
        return newDialpad(MODE_IN_CALL);
    }

    private static DialpadFragment newDialpad(int mode) {
        DialpadFragment fragment = new DialpadFragment();

        Bundle args = new Bundle();
        args.putInt(DIALPAD_MODE_KEY, mode);
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
        if (savedInstanceState != null) {
            mNumber.append(savedInstanceState.getCharSequence(DIAL_NUMBER_KEY));
        }
        L.d(TAG, "onCreateView mode: %s, number: %s", mMode, mNumber);

        View rootView = inflater.inflate(R.layout.dialpad_fragment, container, false);
        // Offset the dialpad to under the tabs in dial mode.
        rootView.setPadding(0, getTopOffset(), 0, 0);

        mTitleView = rootView.findViewById(R.id.title);
        mTitleView.setTextAppearance(
                mMode == MODE_EMERGENCY ? R.style.EmergencyDialNumber : R.style.DialNumber);
        mTitleView.setGravity(Gravity.CENTER);
        mDisplayName = rootView.findViewById(R.id.display_name);
        mCallStateView = rootView.findViewById(R.id.call_state);

        View callButton = rootView.findViewById(R.id.call_button);
        mDeleteButton = rootView.findViewById(R.id.delete_button);

        if (mMode == MODE_IN_CALL) {
            Guideline guideLine = rootView.findViewById(R.id.dialpad_info_guideline);
            // The guideline doesn't exist on portrait
            if (guideLine != null) {
                ConstraintLayout.LayoutParams params =
                        (ConstraintLayout.LayoutParams) guideLine.getLayoutParams();
                params.guidePercent = getContext().getResources().getFloat(
                        R.dimen.dialpad_info_guideline_in_call);
                guideLine.setLayoutParams(params);
            }
            mDeleteButton.setVisibility(View.GONE);
            callButton.setVisibility(View.GONE);
            mCallStateView.setVisibility(View.VISIBLE);

            InCallViewModel viewModel = ViewModelProviders.of(getActivity()).get(
                    InCallViewModel.class);
            mActiveCall = viewModel.getPrimaryCall().getValue();
            viewModel.getCallStateAndConnectTime().observe(this, (pair) -> {
                if (pair == null) {
                    mCallStateView.stop();
                    mCallStateView.setText("");
                    return;
                }
                if (pair.first == Call.STATE_ACTIVE) {
                    mCallStateView.setBase(pair.second
                            - System.currentTimeMillis() + SystemClock.elapsedRealtime());
                    mCallStateView.start();
                } else {
                    mCallStateView.stop();
                    mCallStateView.setText(TelecomUtils.callStateToUiString(
                            getContext(), pair.first));
                }
            });
        } else {
            callButton.setVisibility(View.VISIBLE);
            mDeleteButton.setVisibility(View.GONE);
            mCallStateView.setVisibility(View.GONE);
            Context context = getContext();
            callButton.setOnClickListener((unusedView) -> {
                if (!TextUtils.isEmpty(mNumber.toString())) {
                    UiCallManager.get().placeCall(mNumber.toString());
                    // Update dialed number UI later in onResume() when in call intent is handled.
                    mNumber.setLength(0);
                } else {
                    setDialedNumber(CallLog.Calls.getLastOutgoingCall(context));
                }
            });
            mDeleteButton.setOnClickListener(v -> removeLastDigit());
            mDeleteButton.setOnLongClickListener(v -> {
                clearDialedNumber();
                return true;
            });
        }

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        mDTMFToneEnabled = Settings.System.getInt(getContext().getContentResolver(),
                Settings.System.DTMF_TONE_WHEN_DIALING, 1) == PLAY_DTMF_TONE;
        L.d(TAG, "DTMF tone enabled = %s", String.valueOf(mDTMFToneEnabled));

        presentDialedNumber();
    }

    @Override
    protected void setupActionBar(ActionBar actionBar) {
        // Only setup the actionbar if we're in dial mode.
        // In all the other modes, there will be another fragment in the activity
        // at the same time, and we don't want to mess up it's action bar.
        if (mMode == MODE_DIAL) {
            super.setupActionBar(actionBar);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mToneGenerator.stopTone();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putCharSequence(DIAL_NUMBER_KEY, mNumber);
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

        if (!mDTMFToneEnabled) {
            return;
        }
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
        if (!mDTMFToneEnabled) {
            return;
        }

        if (mActiveCall != null) {
            L.d(TAG, "stop DTMF tone");
            mActiveCall.stopDtmfTone();
        } else {
            L.d(TAG, "stop key pressed tone");
            mToneGenerator.stopTone();
        }
    }

    /** Set the dialed number to the given number. Must be called after the fragment is added. */
    public void setDialedNumber(String number) {
        mNumber.setLength(0);
        if (!TextUtils.isEmpty(number)) {
            mNumber.append(number);
        }
        presentDialedNumber();
    }

    private void clearDialedNumber() {
        mNumber.setLength(0);
        presentDialedNumber();
    }

    private void removeLastDigit() {
        if (mNumber.length() != 0) {
            mNumber.deleteCharAt(mNumber.length() - 1);
        }
        presentDialedNumber();
    }

    private void appendDialedNumber(String number) {
        mNumber.append(number);
        presentDialedNumber();
    }

    private void presentDialedNumber() {
        if (getActivity() == null) {
            return;
        }

        if (mMode != MODE_IN_CALL) {
            presentContactName();
        }

        if (mNumber.length() == 0) {
            mTitleView.setGravity(Gravity.CENTER);
            mDeleteButton.setVisibility(View.GONE);

            if (mMode == MODE_DIAL) {
                mTitleView.setText(R.string.dial_a_number);
            } else if (mMode == MODE_EMERGENCY) {
                mTitleView.setText(R.string.emergency_call_description);
            } else {
                mTitleView.setText("");
            }
        } else if (mNumber.length() > 0 && mNumber.length() <= MAX_DIAL_NUMBER) {
            mTitleView.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);

            if (mMode == MODE_IN_CALL) {
                mTitleView.setText(mNumber);
                mDeleteButton.setVisibility(View.GONE);
            } else {
                mTitleView.setText(
                        TelecomUtils.getFormattedNumber(getContext(), mNumber.toString()));
                mDeleteButton.setVisibility(View.VISIBLE);
            }
        } else {
            mTitleView.setText(mNumber.substring(mNumber.length() - MAX_DIAL_NUMBER));
            mTitleView.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);

            if (mMode == MODE_IN_CALL) {
                mDeleteButton.setVisibility(View.GONE);
            } else {
                mDeleteButton.setVisibility(View.VISIBLE);
            }
        }
    }

    private void presentContactName() {
        if (mDisplayName == null) {
            // OEM may remove this view from resource file.
            return;
        }

        Contact contact = InMemoryPhoneBook.get().lookupContactEntry(mNumber.toString());
        if (contact == null) {
            mDisplayName.setText("");
            mDisplayName.setVisibility(View.GONE);
            return;
        }
        mDisplayName.setVisibility(View.VISIBLE);
        mDisplayName.setText(contact.getDisplayName());
    }

    private int getTopOffset() {
        if (mMode == MODE_DIAL) {
            return getTopBarHeight();
        }
        return 0;
    }
}
