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

package com.android.car.dialer.ui.activecall;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.view.View;

import com.android.car.dialer.CarDialerRobolectricTestRunner;
import com.android.car.dialer.FragmentTestActivity;
import com.android.car.dialer.R;
import com.android.car.dialer.telecom.InCallServiceImpl;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;

@RunWith(CarDialerRobolectricTestRunner.class)
public class InCallFragmentTest {

    private Context mContext;
    private InCallFragment mInCallFragment;
    private FragmentTestActivity mFragmentTestActivity;
    private View mUserProfileContainerView;
    private View mDialerFragmentContainer;
    @Mock
    private InCallServiceImpl.LocalBinder mMockBinder;
    @Mock
    private InCallServiceImpl mMockInCallService;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;

        mInCallFragment = InCallFragment.newInstance();
        mFragmentTestActivity = Robolectric.buildActivity(
                FragmentTestActivity.class).create().start().resume().get();
        when(mMockBinder.getService()).thenReturn(mMockInCallService);
        shadowOf((Application) mContext).setComponentNameAndServiceForBindService(
                new ComponentName(mContext, InCallServiceImpl.class), mMockBinder);
        mFragmentTestActivity.setFragment(mInCallFragment);

        mUserProfileContainerView = mInCallFragment.getView().findViewById(
                R.id.user_profile_container);
        mDialerFragmentContainer = mInCallFragment.getView().findViewById(R.id.dialpad_container);
    }

    @Test
    public void testOnCreateView() {
        assertThat(mDialerFragmentContainer.getVisibility()).isEqualTo(View.GONE);
        assertThat(mUserProfileContainerView.getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void testOnOpenDialpad() {
        mInCallFragment.onOpenDialpad();

        assertThat(mDialerFragmentContainer.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(mUserProfileContainerView.getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void testOnCloseDialpad() {
        mInCallFragment.onCloseDialpad();

        assertThat(mDialerFragmentContainer.getVisibility()).isEqualTo(View.GONE);
        assertThat(mUserProfileContainerView.getVisibility()).isEqualTo(View.VISIBLE);
    }
}
