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

package com.android.car.dialer.telecom;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import android.telecom.Call;

import com.android.car.dialer.CarDialerRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ServiceController;

/**
 * Tests for {@link InCallServiceImpl}.
 */
@RunWith(CarDialerRobolectricTestRunner.class)
public class InCallServiceImplTest {

    private InCallServiceImpl mInCallServiceImpl;
    @Mock
    private Call mMockTelecomCall;
    @Mock
    private InCallServiceImpl.Callback mCallback;
    @Mock
    private InCallServiceImpl.ActiveCallListChangedCallback mActiveCallListChangedCallback;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ServiceController<InCallServiceImpl> inCallServiceController = Robolectric.buildService(
                InCallServiceImpl.class);
        inCallServiceController.create().bind();
        mInCallServiceImpl = inCallServiceController.get();

        mInCallServiceImpl.registerCallback(mCallback);
        mInCallServiceImpl.addActiveCallListChangedCallback(mActiveCallListChangedCallback);
    }

    @Test
    public void testOnCallAdded() {
        mInCallServiceImpl.onCallAdded(mMockTelecomCall);

        ArgumentCaptor<Call.Callback> callbackListCaptor = ArgumentCaptor.forClass(
                Call.Callback.class);
        verify(mMockTelecomCall).registerCallback(callbackListCaptor.capture());

        ArgumentCaptor<Call> callCaptor = ArgumentCaptor.forClass(Call.class);
        verify(mCallback).onTelecomCallAdded(callCaptor.capture());
        assertThat(callCaptor.getValue()).isEqualTo(mMockTelecomCall);

        verify(mActiveCallListChangedCallback).onTelecomCallAdded(callCaptor.capture());
        assertThat(callCaptor.getValue()).isEqualTo(mMockTelecomCall);
    }

    @Test
    public void testOnCallRemoved() {
        mInCallServiceImpl.onCallRemoved(mMockTelecomCall);

        ArgumentCaptor<Call> callCaptor = ArgumentCaptor.forClass(Call.class);
        verify(mCallback).onTelecomCallRemoved(callCaptor.capture());
        assertThat(callCaptor.getValue()).isEqualTo(mMockTelecomCall);

        verify(mActiveCallListChangedCallback).onTelecomCallRemoved(callCaptor.capture());
        assertThat(callCaptor.getValue()).isEqualTo(mMockTelecomCall);

        ArgumentCaptor<Call.Callback> callbackListCaptor = ArgumentCaptor.forClass(
                Call.Callback.class);
        verify(mMockTelecomCall).unregisterCallback(callbackListCaptor.capture());
    }

    @Test
    public void testUnregisterCallback() {
        mInCallServiceImpl.unregisterCallback(mCallback);

        mInCallServiceImpl.onCallAdded(mMockTelecomCall);
        verify(mCallback, never()).onTelecomCallAdded(any());

        mInCallServiceImpl.onCallRemoved(mMockTelecomCall);
        verify(mCallback, never()).onTelecomCallRemoved(any());
    }

    @Test
    public void testRemoveActiveCallListChangedCallback() {
        mInCallServiceImpl.removeActiveCallListChangedCallback(mActiveCallListChangedCallback);

        mInCallServiceImpl.onCallAdded(mMockTelecomCall);
        verify(mActiveCallListChangedCallback, never()).onTelecomCallAdded(any());

        mInCallServiceImpl.onCallRemoved(mMockTelecomCall);
        verify(mActiveCallListChangedCallback, never()).onTelecomCallRemoved(any());
    }
}
