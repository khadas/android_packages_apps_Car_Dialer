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

package com.android.car.dialer.livedata;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothHeadsetClient;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.telecom.CallAudioState;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;

import com.android.car.dialer.CarDialerRobolectricTestRunner;
import com.android.car.dialer.LiveDataObserver;
import com.android.car.dialer.telecom.UiCallManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;

@RunWith(CarDialerRobolectricTestRunner.class)
public class AudioRouteLiveDataTest {

    private AudioRouteLiveData mAudioRouteLiveData;
    private Context mSpyContext;
    private LifecycleRegistry mLifecycleRegistry;
    @Mock
    private LifecycleOwner mMockLifecycleOwner;
    @Mock
    private LiveDataObserver<Integer> mMockObserver;
    @Captor
    private ArgumentCaptor<BroadcastReceiver> mReceiverArgumentCaptor;
    @Mock
    private UiCallManager mMockUiCallManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mSpyContext = spy(RuntimeEnvironment.application);
        mAudioRouteLiveData = new AudioRouteLiveData(mSpyContext);
        mLifecycleRegistry = new LifecycleRegistry(mMockLifecycleOwner);
        when(mMockLifecycleOwner.getLifecycle()).thenReturn(mLifecycleRegistry);

        when(mMockUiCallManager.getAudioRoute()).thenReturn(CallAudioState.ROUTE_EARPIECE);
        UiCallManager.set(mMockUiCallManager);
    }

    @After
    public void tearDown() {
        UiCallManager.set(null);
    }

    @Test
    public void testOnActive() {
        mAudioRouteLiveData.observe(mMockLifecycleOwner, (value) -> mMockObserver.onChanged(value));
        verify(mMockObserver, never()).onChanged(any());

        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);
        ArgumentCaptor<IntentFilter> intentFilterArgumentCaptor = ArgumentCaptor.forClass(
                IntentFilter.class);
        verify(mSpyContext).registerReceiver(any(), intentFilterArgumentCaptor.capture());
        assertThat(intentFilterArgumentCaptor.getValue().hasAction(
                BluetoothHeadsetClient.ACTION_AUDIO_STATE_CHANGED)).isTrue();
        verify(mMockObserver).onChanged(any());
    }

    @Test
    public void testOnBluetoothHfpStateChange() {
        ArgumentCaptor<Integer> valueCaptor = ArgumentCaptor.forClass(Integer.class);
        doNothing().when(mMockObserver).onChanged(valueCaptor.capture());

        mAudioRouteLiveData.observe(mMockLifecycleOwner, (value) -> mMockObserver.onChanged(value));
        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);
        verify(mSpyContext).registerReceiver(mReceiverArgumentCaptor.capture(), any());
        assertThat(mAudioRouteLiveData.getValue()).isEqualTo(CallAudioState.ROUTE_EARPIECE);
        assertThat(valueCaptor.getValue()).isEqualTo(CallAudioState.ROUTE_EARPIECE);

        when(mMockUiCallManager.getAudioRoute()).thenReturn(CallAudioState.ROUTE_BLUETOOTH);
        mReceiverArgumentCaptor.getValue().onReceive(mock(Context.class), mock(Intent.class));
        assertThat(mAudioRouteLiveData.getValue()).isEqualTo(CallAudioState.ROUTE_BLUETOOTH);
        assertThat(valueCaptor.getValue()).isEqualTo(CallAudioState.ROUTE_BLUETOOTH);
    }

    @Test
    public void testOnInactiveUnregister() {
        mAudioRouteLiveData.observe(mMockLifecycleOwner, (value) -> mMockObserver.onChanged(value));
        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);
        verify(mSpyContext).registerReceiver(mReceiverArgumentCaptor.capture(), any());

        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY);
        verify(mSpyContext).unregisterReceiver(mReceiverArgumentCaptor.getValue());
    }
}
