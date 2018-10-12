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

package com.android.car.dialer.ui.contact;

import static androidx.car.widget.PagedListView.UNLIMITED_PAGES;

import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.car.widget.ListItemAdapter;
import androidx.car.widget.PagedListView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import com.android.car.dialer.R;
import com.android.car.dialer.entity.Contact;
import com.android.car.dialer.ui.common.DialerBaseFragment;

import java.util.List;

/**
 * Contact Fragment.
 */
public class ContactListFragment extends DialerBaseFragment implements
        ContactListItemProvider.OnShowContactDetailListener {
    private String CONTACT_DETAIL_FRAGMENT_TAG = "CONTACT_DETAIL_FRAGMENT_TAG";
    private ContactListItemProvider mContactListItemProvider;
    private ListItemAdapter mContactListAdapter;
    private View mContactDetailContainer;

    public static ContactListFragment newInstance() {
        return new ContactListFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View fragmentView = inflater.inflate(R.layout.contact_list_fragment, container, false);

        ContactListViewModel contactListViewModel = ViewModelProviders.of(this).get(
                ContactListViewModel.class);
        contactListViewModel.getAllContacts().observe(this, this::onContactListChanged);
        mContactListItemProvider = new ContactListItemProvider(
                getContext(), /* onShowContactDetailListener = */this);
        mContactListItemProvider.setContacts(contactListViewModel.getAllContacts().getValue());
        mContactListAdapter = new ListItemAdapter(getContext(), mContactListItemProvider);

        PagedListView pagedListView = fragmentView.findViewById(R.id.list_view);
        pagedListView.setAdapter(mContactListAdapter);
        pagedListView.setMaxPages(UNLIMITED_PAGES);
        mContactListAdapter.notifyDataSetChanged();

        mContactDetailContainer = fragmentView.findViewById(R.id.contact_detail_container);
        fragmentView.findViewById(R.id.back_button).setOnClickListener(
                (v) -> hideContactDetailFragment());
        return fragmentView;
    }

    private void onContactListChanged(List<Contact> contacts) {
        mContactListItemProvider.setContacts(contacts);
        mContactListAdapter.notifyDataSetChanged();
    }

    private void hideContactDetailFragment() {
        Fragment contactDetailFragment = getChildFragmentManager().findFragmentByTag(
                CONTACT_DETAIL_FRAGMENT_TAG);
        if (contactDetailFragment != null) {
            getChildFragmentManager().beginTransaction().remove(contactDetailFragment).commit();
        }

        mContactDetailContainer.setVisibility(View.GONE);
    }

    @Override
    public void onShowContactDetail(int contactId, String lookupKey) {
        mContactDetailContainer.setVisibility(View.VISIBLE);

        final Uri uri = ContactsContract.Contacts.getLookupUri(contactId, lookupKey);
        // TODO: pass this Contact entity to ContactDetailFragment instead of having it loaded on
        // its own.
        Fragment contactDetailFragment = ContactDetailsFragment.newInstance(uri, null);
        getChildFragmentManager().beginTransaction().replace(R.id.contact_detail_fragment_container,
                contactDetailFragment, CONTACT_DETAIL_FRAGMENT_TAG).commit();
    }

    @StringRes
    @Override
    protected int getActionBarTitleRes() {
        return R.string.contacts_title;
    }
}
