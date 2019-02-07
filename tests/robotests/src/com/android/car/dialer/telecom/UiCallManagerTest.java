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
import static org.mockito.Mockito.verify;
import static org.robolectric.Shadows.shadowOf;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.telecom.TelecomManager;

import com.android.car.dialer.R;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowContextImpl;
import org.robolectric.shadows.ShadowToast;

@RunWith(RobolectricTestRunner.class)
public class UiCallManagerTest {

    private static final String PHONE_NUMBER = "6055551234";
    private static final String INVALID_PHONE_NUMBER = "#######";
    private static final String TEL_SCHEME = "tel";

    private Context mContext;
    @Mock
    private TelecomManager mMockTelecomManager;
    @Mock
    private InCallServiceImpl.LocalBinder mMockBinder;

    private UiCallManager mUiCallManager;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        shadowOf((Application) mContext).setComponentNameAndServiceForBindService(
                new ComponentName(mContext, InCallServiceImpl.class), mMockBinder);
        ShadowContextImpl shadowContext = Shadow.extract(((Application) mContext).getBaseContext());
        shadowContext.setSystemService(Context.TELECOM_SERVICE, mMockTelecomManager);

        mUiCallManager = UiCallManager.init(mContext);
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
        ArgumentCaptor<Uri> uriCaptor = ArgumentCaptor.forClass(Uri.class);

        assertThat(mUiCallManager.placeCall(PHONE_NUMBER)).isTrue();
        verify(mMockTelecomManager).placeCall(uriCaptor.capture(), (Bundle) isNull());
        assertThat(uriCaptor.getValue().getScheme()).isEqualTo(TEL_SCHEME);
        assertThat(uriCaptor.getValue().getSchemeSpecificPart()).isEqualTo(PHONE_NUMBER);
        assertThat(uriCaptor.getValue().getFragment()).isNull();
    }

    @Test
    public void testPlaceCall_invalidNumber() {
        ArgumentCaptor<Uri> uriCaptor = ArgumentCaptor.forClass(Uri.class);

        assertThat(mUiCallManager.placeCall(INVALID_PHONE_NUMBER)).isFalse();
        verify(mMockTelecomManager, never()).placeCall(uriCaptor.capture(), isNull());

        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo(
                mContext.getString(R.string.error_invalid_phone_number));
    }

    @After
    public void tearDown() {
        mUiCallManager.tearDown();
    }
}
