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

package com.android.car.dialer.ui.activecall;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.telecom.Call;
import android.telecom.CallAudioState;
import android.telecom.TelecomManager;
import android.view.View;
import android.widget.ImageView;

import androidx.core.util.Pair;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.android.car.dialer.CarDialerRobolectricTestRunner;
import com.android.car.dialer.FragmentTestActivity;
import com.android.car.dialer.R;
import com.android.car.dialer.telecom.InCallServiceImpl;
import com.android.car.dialer.telecom.UiCallManager;
import com.android.car.dialer.testutils.ShadowAndroidViewModelFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowContextImpl;
import org.robolectric.shadows.ShadowLooper;

import java.util.ArrayList;
import java.util.List;

@Config(shadows = {ShadowAndroidViewModelFactory.class})
@RunWith(CarDialerRobolectricTestRunner.class)
public class OnGoingCallControllerBarFragmentTest {
    private static final String CALL_STATE = "CALL_STATE";

    private OnGoingCallControllerBarFragment mOnGoingCallControllerBarFragment;
    private List<Integer> mAudioRouteList = new ArrayList<>();
    @Mock
    private Call mMockCall;
    @Mock
    private InCallServiceImpl.LocalBinder mMockBinder;
    @Mock
    private InCallServiceImpl mMockInCallService;
    @Mock
    private TelecomManager mMockTelecomManager;
    @Mock
    private CallAudioState mMockAudioState;
    @Mock
    private UiCallManager mMockUiCallManager;
    @Mock
    private InCallViewModel mMockInCallViewModel;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        Context context = RuntimeEnvironment.application;

        when(mMockBinder.getService()).thenReturn(mMockInCallService);
        when(mMockInCallService.getCallAudioState()).thenReturn(mMockAudioState);
        shadowOf((Application) context).setComponentNameAndServiceForBindService(
                new ComponentName(context, InCallServiceImpl.class), mMockBinder);
        ShadowContextImpl shadowContext = Shadow.extract(((Application) context).getBaseContext());
        shadowContext.setSystemService(Context.TELECOM_SERVICE, mMockTelecomManager);
    }

    @Test
    public void testNewInstance_withCallState() {
        mOnGoingCallControllerBarFragment = OnGoingCallControllerBarFragment.newInstance(
                Call.STATE_DIALING);
        assertThat(mOnGoingCallControllerBarFragment).isNotNull();
        assertThat(mOnGoingCallControllerBarFragment.getArguments().getInt(CALL_STATE)).isEqualTo(
                Call.STATE_DIALING);
    }

    @Test
    public void testMuteButton() {
        addFragment(Call.STATE_ACTIVE);

        View muteButton = mOnGoingCallControllerBarFragment.getView().findViewById(
                R.id.mute_button);
        // Test initial state
        assertThat(muteButton.hasOnClickListeners()).isTrue();
        assertThat(muteButton.isActivated()).isFalse();
        // Mute
        muteButton.performClick();
        assertThat(muteButton.isActivated()).isTrue();
        verify(mMockUiCallManager).setMuted(true);
        // Unmute
        muteButton.performClick();
        assertThat(muteButton.isActivated()).isFalse();
        verify(mMockUiCallManager).setMuted(false);
    }

    @Test
    public void testDialpadButton() {
        addFragment(Call.STATE_ACTIVE);

        View dialpadButton = mOnGoingCallControllerBarFragment.getView().findViewById(
                R.id.toggle_dialpad_button);
        View dialerFragmentContainer =
                mOnGoingCallControllerBarFragment.getParentFragment().getView().findViewById(
                        R.id.dialpad_container);
        // Test initial state
        assertThat(dialpadButton.hasOnClickListeners()).isTrue();
        assertThat(dialpadButton.isActivated()).isFalse();
        assertThat(dialerFragmentContainer.getVisibility()).isEqualTo(View.GONE);
        // On open dialpad
        dialpadButton.performClick();
        assertThat(dialpadButton.isActivated()).isTrue();
        assertThat(dialerFragmentContainer.getVisibility()).isEqualTo(View.VISIBLE);
        // On close dialpad
        dialpadButton.performClick();
        assertThat(dialpadButton.isActivated()).isFalse();
        assertThat(dialerFragmentContainer.getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void testEndCallButton() {
        addFragment(Call.STATE_ACTIVE);

        View endCallButton = mOnGoingCallControllerBarFragment.getView().findViewById(
                R.id.end_call_button);
        assertThat(endCallButton.hasOnClickListeners()).isTrue();
        // onEndCall
        endCallButton.performClick();
        verify(mMockCall).disconnect();
    }

    @Test
    public void testAudioRouteButton_withOneAudioRoute() {
        addFragment(Call.STATE_ACTIVE);

        View fragmentView = mOnGoingCallControllerBarFragment.getView();
        assertThat(fragmentView.findViewById(
                R.id.voice_channel_button).hasOnClickListeners()).isFalse();
    }

    @Test
    public void testAudioRouteButtonView_withMultipleAudioRoutes() {
        mAudioRouteList.add(CallAudioState.ROUTE_EARPIECE);
        mAudioRouteList.add(CallAudioState.ROUTE_BLUETOOTH);
        addFragment(Call.STATE_ACTIVE);

        View fragmentView = mOnGoingCallControllerBarFragment.getView();
        ImageView audioRouteButton = fragmentView.findViewById(R.id.voice_channel_button);
        assertThat(audioRouteButton.hasOnClickListeners()).isTrue();
    }

    @Test
    public void testClickPauseButton_activeCall() {
        addFragment(Call.STATE_ACTIVE);

        ImageView pauseButton = mOnGoingCallControllerBarFragment.getView().findViewById(
                R.id.pause_button);
        assertThat(pauseButton.hasOnClickListeners()).isTrue();
        assertThat(pauseButton.isActivated()).isFalse();

        // onHoldCall
        pauseButton.performClick();
        verify(mMockCall).hold();
        // onUnHoldCall
        mOnGoingCallControllerBarFragment.setCallState(Call.STATE_HOLDING);
        pauseButton.performClick();
        verify(mMockCall).unhold();
    }

    @Test
    public void testClickPauseButton_connectingCall() {
        addFragment(Call.STATE_DIALING);
        ImageView pauseButton = mOnGoingCallControllerBarFragment.getView().findViewById(
                R.id.pause_button);
        pauseButton.performClick();
        verify(mMockCall, never()).hold();
        verify(mMockCall, never()).unhold();
    }

    private void addFragment(int CallState) {
        mAudioRouteList.add(CallAudioState.ROUTE_SPEAKER);
        when(mMockUiCallManager.getSupportedAudioRoute()).thenReturn(mAudioRouteList);
        UiCallManager.set(mMockUiCallManager);

        LiveData<Call> callLiveData = mock(LiveData.class);
        when(callLiveData.getValue()).thenReturn(mMockCall);
        when(mMockInCallViewModel.getPrimaryCall()).thenReturn(callLiveData);
        when(mMockInCallViewModel.getPrimaryCallDetail()).thenReturn(mock(LiveData.class));
        when(mMockInCallViewModel.getPrimaryCallState()).thenReturn(mock(LiveData.class));

        MutableLiveData<Integer> audioRouteLiveData = new MutableLiveData<>();
        audioRouteLiveData.setValue(CallAudioState.ROUTE_BLUETOOTH);
        when(mMockInCallViewModel.getAudioRoute()).thenReturn(audioRouteLiveData);

        LiveData<Pair<Integer, Long>> stateAndConnectTimeLiveData = mock(LiveData.class);
        when(mMockInCallViewModel.getCallStateAndConnectTime())
                .thenReturn(stateAndConnectTimeLiveData);

        ShadowAndroidViewModelFactory.add(InCallViewModel.class, mMockInCallViewModel);

        FragmentTestActivity fragmentTestActivity = Robolectric.buildActivity(
                FragmentTestActivity.class).create().start().resume().get();
        InCallFragment inCallFragment = InCallFragment.newInstance();
        fragmentTestActivity.setFragment(inCallFragment);

        mOnGoingCallControllerBarFragment = OnGoingCallControllerBarFragment.newInstance(CallState);
        ShadowLooper.pauseMainLooper();
        inCallFragment.getChildFragmentManager().beginTransaction().replace(
                R.id.controller_bar_container, mOnGoingCallControllerBarFragment).commitNow();
        assertThat(inCallFragment).isEqualTo(mOnGoingCallControllerBarFragment.getParentFragment());
    }
}
