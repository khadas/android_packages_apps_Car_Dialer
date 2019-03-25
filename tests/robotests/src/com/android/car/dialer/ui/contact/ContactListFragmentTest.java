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

package com.android.car.dialer.ui.contact;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.view.View;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.RecyclerView;

import com.android.car.dialer.CarDialerRobolectricTestRunner;
import com.android.car.dialer.FragmentTestActivity;
import com.android.car.dialer.R;
import com.android.car.dialer.telecom.UiCallManager;
import com.android.car.dialer.testutils.ShadowViewModelProvider;
import com.android.car.telephony.common.Contact;
import com.android.car.telephony.common.PhoneNumber;

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
public class ContactListFragmentTest {
    private static final String RAW_NUMBNER = "6502530000";

    private ContactListFragment mContactListFragment;
    private FragmentTestActivity mFragmentTestActivity;
    private ContactListViewHolder mViewHolder;
    @Mock
    private UiCallManager mMockUiCallManager;
    @Mock
    private ContactListViewModel mMockContactListViewModel;
    @Mock
    private ContactDetailsViewModel mMockContactDetailsViewModel;
    @Mock
    private Contact mMockContact1;
    @Mock
    private Contact mMockContact2;
    @Mock
    private Contact mMockContact3;
    @Mock
    private PhoneNumber mMockPhoneNumber;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        MutableLiveData<List<Contact>> contactList = new MutableLiveData<>();
        contactList.setValue(Arrays.asList(mMockContact1, mMockContact2, mMockContact3));
        ShadowViewModelProvider.add(ContactListViewModel.class, mMockContactListViewModel);
        when(mMockContactListViewModel.getAllContacts()).thenReturn(contactList);

        MutableLiveData<Contact> contactDetail = new MutableLiveData<>();
        contactDetail.setValue(mMockContact1);
        ShadowViewModelProvider.add(ContactDetailsViewModel.class, mMockContactDetailsViewModel);
        when(mMockContactDetailsViewModel.getContactDetails(any())).thenReturn(contactDetail);
    }

    @Test
    public void testClickContact_ContactHasOneNumber_placeCall() {
        UiCallManager.set(mMockUiCallManager);
        when(mMockContact1.getNumbers()).thenReturn(Arrays.asList(mMockPhoneNumber));
        when(mMockPhoneNumber.getRawNumber()).thenReturn(RAW_NUMBNER);
        setUpFragment();

        assertThat(mViewHolder.itemView.hasOnClickListeners()).isTrue();

        mViewHolder.itemView.performClick();

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mMockUiCallManager).placeCall(captor.capture());
        assertThat(captor.getValue()).isEqualTo(RAW_NUMBNER);
    }

    @Test
    public void testClickContact_ContactHasMultipleNumbers_showContactDetail() {
        PhoneNumber otherMockPhoneNumber = mock(PhoneNumber.class);
        when(mMockContact1.getNumbers()).thenReturn(
                Arrays.asList(mMockPhoneNumber, otherMockPhoneNumber));
        setUpFragment();

        mViewHolder.itemView.performClick();

        // verify contact detail is shown.
        verifyShowContactDetail();
    }

    @Test
    public void testClickActionButton_showContactDetail() {
        setUpFragment();

        View actionButton = mViewHolder.itemView.findViewById(R.id.action_button);
        assertThat(actionButton.hasOnClickListeners()).isTrue();

        actionButton.performClick();

        // verify contact detail is shown.
        verifyShowContactDetail();
    }

    private void setUpFragment() {
        mContactListFragment = ContactListFragment.newInstance();
        mFragmentTestActivity = Robolectric.buildActivity(
                FragmentTestActivity.class).create().resume().get();
        mFragmentTestActivity.setFragment(mContactListFragment);

        RecyclerView recyclerView = mContactListFragment.getView().findViewById(R.id.list_view);
        //Force RecyclerView to layout to ensure findViewHolderForLayoutPosition works.
        recyclerView.layout(0, 0, 100, 1000);
        mViewHolder = (ContactListViewHolder) recyclerView.findViewHolderForLayoutPosition(0);
    }

    private void verifyShowContactDetail() {
        FragmentManager manager = mFragmentTestActivity.getSupportFragmentManager();
        String tag = manager.getBackStackEntryAt(manager.getBackStackEntryCount() - 1).getName();
        Fragment fragment = manager.findFragmentByTag(tag);
        assertThat(fragment instanceof ContactDetailsFragment).isTrue();
    }
}
