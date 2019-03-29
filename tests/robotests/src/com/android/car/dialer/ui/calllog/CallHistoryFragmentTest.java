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

package com.android.car.dialer.ui.calllog;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.net.Uri;
import android.widget.TextView;

import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.RecyclerView;

import com.android.car.dialer.CarDialerRobolectricTestRunner;
import com.android.car.dialer.FragmentTestActivity;
import com.android.car.dialer.R;
import com.android.car.dialer.livedata.CallHistoryLiveData;
import com.android.car.dialer.telecom.UiCallManager;
import com.android.car.dialer.testutils.ShadowViewModelProvider;
import com.android.car.dialer.ui.common.entity.UiCallLog;
import com.android.car.dialer.widget.CallTypeIconsView;
import com.android.car.telephony.common.PhoneCallLog;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.List;

@Config(shadows = {ShadowViewModelProvider.class})
@RunWith(CarDialerRobolectricTestRunner.class)
public class CallHistoryFragmentTest {
    private static final String PHONE_NUMBER = "6502530000";
    private static final String UI_CALLOG_TITLE = "TITLE";
    private static final String UI_CALLOG_TEXT = "TEXT";
    private static final long TIME_STAMP_1 = 5000;
    private static final long TIME_STAMP_2 = 10000;

    private CallLogViewHolder mViewHolder;
    @Mock
    private UiCallManager mMockUiCallManager;
    @Mock
    private Uri mMockUri;
    @Mock
    private CallHistoryViewModel mMockCallHistoryViewModel;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        UiCallManager.set(mMockUiCallManager);

        PhoneCallLog.Record record1 = new PhoneCallLog.Record(TIME_STAMP_1,
                CallHistoryLiveData.CallType.INCOMING_TYPE);
        PhoneCallLog.Record record2 = new PhoneCallLog.Record(TIME_STAMP_2,
                CallHistoryLiveData.CallType.OUTGOING_TYPE);
        UiCallLog uiCallLog = new UiCallLog(UI_CALLOG_TITLE, UI_CALLOG_TEXT, PHONE_NUMBER, mMockUri,
                Arrays.asList(record1, record2));

        MutableLiveData<List<UiCallLog>> callLog = new MutableLiveData<>();
        callLog.setValue(Arrays.asList(uiCallLog));
        ShadowViewModelProvider.add(CallHistoryViewModel.class, mMockCallHistoryViewModel);
        when(mMockCallHistoryViewModel.getCallHistory()).thenReturn(callLog);

        CallHistoryFragment callHistoryFragment = CallHistoryFragment.newInstance();
        FragmentTestActivity mFragmentTestActivity = Robolectric.buildActivity(
                FragmentTestActivity.class).create().resume().get();
        mFragmentTestActivity.setFragment(callHistoryFragment);

        RecyclerView recyclerView = callHistoryFragment.getView().findViewById(R.id.list_view);
        // set up layout for recyclerView
        recyclerView.layout(0, 0, 100, 1000);
        mViewHolder = (CallLogViewHolder) recyclerView.findViewHolderForLayoutPosition(0);
    }

    @Test
    public void testUI() {
        TextView titleView = mViewHolder.itemView.findViewById(R.id.title);
        TextView textView = mViewHolder.itemView.findViewById(R.id.text);
        CallTypeIconsView callTypeIconsView = mViewHolder.itemView.findViewById(
                R.id.call_type_icons);

        assertThat(titleView.getText()).isEqualTo(UI_CALLOG_TITLE);
        assertThat(textView.getText()).isEqualTo(UI_CALLOG_TEXT);
        assertThat(callTypeIconsView.getCallType(0)).isEqualTo(
                CallHistoryLiveData.CallType.INCOMING_TYPE);
        assertThat(callTypeIconsView.getCallType(1)).isEqualTo(
                CallHistoryLiveData.CallType.OUTGOING_TYPE);
    }

    @Test
    public void testClick_placeCall() {
        assertThat(mViewHolder.itemView.hasOnClickListeners()).isTrue();

        mViewHolder.itemView.performClick();

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mMockUiCallManager).placeCall(captor.capture());
        assertThat(captor.getValue()).isEqualTo(PHONE_NUMBER);
    }
}
