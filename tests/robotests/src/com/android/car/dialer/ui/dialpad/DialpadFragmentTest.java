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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.android.car.dialer.CarDialerRobolectricTestRunner;
import com.android.car.dialer.FragmentTestActivity;
import com.android.car.dialer.R;
import com.android.car.dialer.telecom.InCallServiceImpl;
import com.android.car.dialer.ui.activecall.InCallFragment;

import com.android.car.telephony.common.TelecomUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;

@RunWith(CarDialerRobolectricTestRunner.class)
public class DialpadFragmentTest {
    private static final String DIAL_NUMBER = "6505551234";
    private static final String DIAL_NUMBER_LONG = "650555123465055512346505551234";
    private static final String SINGLE_DIGIT = "0";
    private static final String SPEC_CHAR = "123=_=%^&";

    private DialpadFragment mDialpadFragment;
    @Mock
    private InCallServiceImpl.LocalBinder mMockBinder;
    @Mock
    private InCallServiceImpl mMockInCallService;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testOnCreateView_modeDialWithNormalDialNumber() {
        mDialpadFragment = DialpadFragment.newPlaceCallDialpad(DIAL_NUMBER);
        startPlaceCallActivity();

        verifyButtonVisibility(View.VISIBLE);
        verifyTitleText(DIAL_NUMBER);
    }

    @Test
    public void testOnCreateView_modeDialWithLongDialNumber() {
        mDialpadFragment = DialpadFragment.newPlaceCallDialpad(DIAL_NUMBER_LONG);
        startPlaceCallActivity();

        verifyButtonVisibility(View.VISIBLE);
        verifyTitleText(DIAL_NUMBER_LONG);
    }

    @Test
    public void testOnCreateView_modeDialWithNullDialNumber() {
        mDialpadFragment = DialpadFragment.newPlaceCallDialpad(null);
        startPlaceCallActivity();

        verifyButtonVisibility(View.VISIBLE);
        verifyTitleText(mDialpadFragment.getContext().getString(R.string.dial_a_number));
    }

    @Test
    public void testOnCreateView_modeDialWithEmptyDialNumber() {
        mDialpadFragment = DialpadFragment.newPlaceCallDialpad("");
        startPlaceCallActivity();

        verifyButtonVisibility(View.VISIBLE);
        verifyTitleText(mDialpadFragment.getContext().getString(R.string.dial_a_number));
    }

    @Test
    public void testOnCreateView_modeDialWithSpecialChar() {
        mDialpadFragment = DialpadFragment.newPlaceCallDialpad(SPEC_CHAR);
        startPlaceCallActivity();

        verifyButtonVisibility(View.VISIBLE);
        verifyTitleText(SPEC_CHAR);
    }

    @Test
    public void testOnCreateView_modeInCall() {
        startInCallActivity();

        verifyButtonVisibility(View.GONE);
        verifyTitleText("");
    }

    @Test
    public void testDeleteButton_normalString() {
        mDialpadFragment = DialpadFragment.newPlaceCallDialpad(DIAL_NUMBER);
        startPlaceCallActivity();

        ImageButton deleteButton = mDialpadFragment.getView().findViewById(R.id.delete_button);
        deleteButton.performClick();

        verifyTitleText(DIAL_NUMBER.substring(0, DIAL_NUMBER.length() - 1));
    }

    @Test
    public void testDeleteButton_oneDigit() {
        mDialpadFragment = DialpadFragment.newPlaceCallDialpad(SINGLE_DIGIT);
        startPlaceCallActivity();

        ImageButton deleteButton = mDialpadFragment.getView().findViewById(R.id.delete_button);
        deleteButton.performClick();
        verifyTitleText(mDialpadFragment.getContext().getString(R.string.dial_a_number));
    }

    @Test
    public void testDeleteButton_emptyString() {
        mDialpadFragment = DialpadFragment.newPlaceCallDialpad("");
        startPlaceCallActivity();

        ImageButton deleteButton = mDialpadFragment.getView().findViewById(R.id.delete_button);
        deleteButton.performClick();
        verifyTitleText(mDialpadFragment.getContext().getString(R.string.dial_a_number));
    }

    @Test
    public void testLongPressDeleteButton() {
        mDialpadFragment = DialpadFragment.newPlaceCallDialpad(DIAL_NUMBER);
        startPlaceCallActivity();

        ImageButton deleteButton = mDialpadFragment.getView().findViewById(R.id.delete_button);

        deleteButton.performLongClick();
        verifyTitleText(mDialpadFragment.getContext().getString(R.string.dial_a_number));
    }

    @Test
    public void testOnKeyLongPressed_KeyCode0() {
        mDialpadFragment = DialpadFragment.newPlaceCallDialpad(DIAL_NUMBER);
        startPlaceCallActivity();

        mDialpadFragment.onKeyLongPressed(KeyEvent.KEYCODE_0);
        verifyTitleText(DIAL_NUMBER.substring(0, DIAL_NUMBER.length() - 1) + "+");
    }

    private void startPlaceCallActivity() {
        FragmentTestActivity fragmentTestActivity;
        fragmentTestActivity = Robolectric.buildActivity(FragmentTestActivity.class)
                .create().start().resume().get();
        fragmentTestActivity.setFragment(mDialpadFragment);
    }

    private void startInCallActivity() {
        Context context;
        context = RuntimeEnvironment.application;

        mDialpadFragment = DialpadFragment.newInCallDialpad();
        InCallFragment inCallFragment = InCallFragment.newInstance();
        FragmentTestActivity fragmentTestActivity = Robolectric.buildActivity(
                FragmentTestActivity.class).create().start().resume().get();
        when(mMockBinder.getService()).thenReturn(mMockInCallService);
        shadowOf((Application) context).setComponentNameAndServiceForBindService(
                new ComponentName(context, InCallServiceImpl.class), mMockBinder);
        fragmentTestActivity.setFragment(inCallFragment);
        inCallFragment.getChildFragmentManager().beginTransaction().replace(R.id.dialpad_container,
                mDialpadFragment).commit();
    }

    private void verifyButtonVisibility(int expectedVisibility) {
        ImageButton callButton = mDialpadFragment.getView().findViewById(R.id.call_button);
        ImageButton deleteButton = mDialpadFragment.getView().findViewById(R.id.delete_button);

        assertThat(callButton.getVisibility()).isEqualTo(expectedVisibility);
        assertThat(deleteButton.getVisibility()).isEqualTo(expectedVisibility);
    }

    private void verifyTitleText(String expectedText) {
        expectedText = TelecomUtils.getFormattedNumber(mDialpadFragment.getContext(), expectedText);
        TextView mTitleView = mDialpadFragment.getView().findViewById(R.id.title);
        TelecomUtils.getFormattedNumber(mDialpadFragment.getContext(), null);
        assertThat(mTitleView.getText()).isEqualTo(expectedText);
    }
}
