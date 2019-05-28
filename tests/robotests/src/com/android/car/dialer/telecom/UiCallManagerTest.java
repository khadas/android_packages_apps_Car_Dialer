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

package com.android.car.dialer.telecom;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.app.Application;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.telecom.TelecomManager;

import com.android.car.dialer.CarDialerRobolectricTestRunner;
import com.android.car.dialer.R;
import com.android.car.dialer.TestDialerApplication;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowContextImpl;
import org.robolectric.shadows.ShadowToast;

@RunWith(CarDialerRobolectricTestRunner.class)
public class UiCallManagerTest {

    private static final String TEL_SCHEME = "tel";

    private Context mContext;
    @Mock
    private TelecomManager mMockTelecomManager;

    private UiCallManager mUiCallManager;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;

        ShadowContextImpl shadowContext = Shadow.extract(((Application) mContext).getBaseContext());
        shadowContext.setSystemService(Context.TELECOM_SERVICE, mMockTelecomManager);
        ((TestDialerApplication) RuntimeEnvironment.application).initUiCallManager();

        mUiCallManager = UiCallManager.get();
    }

    @Test
    public void testInit_initTwice_ThrowException() {
        assertNotNull(mUiCallManager);

        try {
            UiCallManager.init(mContext);
            fail();
        } catch (IllegalStateException e) {
            // This is expected.
        }
    }

    @Test
    public void testPlaceCall() {
        String[] phoneNumbers = {
                "6505551234", // US Number
                "511", // Special number
                "911", // Emergency number
                "122", // Emergency number
                "#77" // Emergency number
        };

        for (int i = 0; i < phoneNumbers.length; i++) {
            checkPlaceCall(phoneNumbers[i], i + 1);
        }
    }

    private void checkPlaceCall(String phoneNumber, int timesCalled) {
        ArgumentCaptor<Uri> uriCaptor = ArgumentCaptor.forClass(Uri.class);

        assertThat(mUiCallManager.placeCall(phoneNumber)).isTrue();
        verify(mMockTelecomManager, times(timesCalled)).placeCall(uriCaptor.capture(),
                (Bundle) isNull());
        assertThat(uriCaptor.getValue().getScheme()).isEqualTo(TEL_SCHEME);
        assertThat(uriCaptor.getValue().getSchemeSpecificPart()).isEqualTo(phoneNumber);
        assertThat(uriCaptor.getValue().getFragment()).isNull();
    }

    @Test
    public void testPlaceCall_invalidNumber() {
        String[] phoneNumbers = {
                "xxxxx",
                "51f"
        };

        for (String phoneNumber : phoneNumbers) {
            checkPlaceCallForInvalidNumber(phoneNumber);
        }
    }

    private void checkPlaceCallForInvalidNumber(String phoneNumber) {
        ArgumentCaptor<Uri> uriCaptor = ArgumentCaptor.forClass(Uri.class);

        assertThat(mUiCallManager.placeCall(phoneNumber)).isFalse();
        verify(mMockTelecomManager, never()).placeCall(uriCaptor.capture(), isNull());

        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo(
                mContext.getString(R.string.error_invalid_phone_number));
    }

    @After
    public void tearDown() {
        mUiCallManager.tearDown();
    }
}
