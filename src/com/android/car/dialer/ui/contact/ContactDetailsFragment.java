/*
 * Copyright (C) 2017 The Android Open Source Project
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

import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.RecyclerView;

import com.android.car.dialer.R;
import com.android.car.dialer.ui.common.DialerListBaseFragment;
import com.android.car.telephony.common.Contact;

import java.util.ArrayList;
import java.util.List;

/**
 * A fragment that shows the name of the contact, the photo and all listed phone numbers. It is
 * primarily used to respond to the results of search queries but supplyig it with the content://
 * uri of a contact should work too.
 */
public class ContactDetailsFragment extends DialerListBaseFragment {
    private static final String TAG = "CD.ContactDetailsFragment";
    public static final String FRAGMENT_TAG = "CONTACT_DETAIL_FRAGMENT_TAG";

    // Key to load and save the contact entity instance.
    private static final String KEY_CONTACT_ENTITY = "ContactEntity";

    // Key to load the contact details by passing in the content provider query uri.
    private static final String KEY_CONTACT_QUERY_URI = "ContactQueryUri";

    private final List<RecyclerView.OnScrollListener> mOnScrollListeners = new ArrayList<>();

    private Contact mContact;
    private Uri mContactLookupUri;
    private LiveData<Contact> mContactDetailsLiveData;

    public static ContactDetailsFragment newInstance(
            Uri uri, @Nullable RecyclerView.OnScrollListener listener) {
        ContactDetailsFragment fragment = new ContactDetailsFragment();
        fragment.addOnScrollListener(listener);
        Bundle args = new Bundle();
        args.putParcelable(KEY_CONTACT_QUERY_URI, uri);
        fragment.setArguments(args);
        return fragment;
    }

    public static ContactDetailsFragment newInstance(
            Contact contact, @Nullable RecyclerView.OnScrollListener listener) {
        ContactDetailsFragment fragment = new ContactDetailsFragment();
        fragment.addOnScrollListener(listener);
        Bundle args = new Bundle();
        args.putParcelable(KEY_CONTACT_ENTITY, contact);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        mContact = getArguments().getParcelable(KEY_CONTACT_ENTITY);
        mContactLookupUri = getArguments().getParcelable(KEY_CONTACT_QUERY_URI);
        if (mContact == null && savedInstanceState != null) {
            mContact = savedInstanceState.getParcelable(KEY_CONTACT_ENTITY);
        }
        if (mContact != null) {
            mContactLookupUri = mContact.getLookupUri();
        }
        ContactDetailsViewModel contactDetailsViewModel = ViewModelProviders.of(this).get(
                ContactDetailsViewModel.class);
        mContactDetailsLiveData = contactDetailsViewModel.getContactDetails(mContactLookupUri);
        mContactDetailsLiveData.observe(this, contact -> getArguments().clear());
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.contact_edit, menu);
        MenuItem defaultNumberMenuItem = menu.findItem(R.id.menu_contact_default_number);
        ContactDefaultNumberActionProvider contactDefaultNumberActionProvider =
                (ContactDefaultNumberActionProvider) defaultNumberMenuItem.getActionProvider();
        contactDefaultNumberActionProvider.setContact(mContact);
        mContactDetailsLiveData.observe(this, contactDefaultNumberActionProvider::setContact);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        for (RecyclerView.OnScrollListener listener : mOnScrollListeners) {
            getRecyclerView().addOnScrollListener(listener);
        }
        mOnScrollListeners.clear();

        ContactDetailsAdapter contactDetailsAdapter = new ContactDetailsAdapter(getContext(),
                mContact);
        getRecyclerView().setAdapter(contactDetailsAdapter);
        mContactDetailsLiveData.observe(this, contactDetailsAdapter::setContact);
    }

    /**
     * Adds a {@link androidx.recyclerview.widget.RecyclerView.OnScrollListener} to be notified when
     * the contact details are scrolled.
     *
     * @see RecyclerView#addOnScrollListener(RecyclerView.OnScrollListener)
     */
    public void addOnScrollListener(@Nullable RecyclerView.OnScrollListener onScrollListener) {
        if (onScrollListener == null) {
            return;
        }
        // If the view has not been created yet, then queue the setting of the scroll listener.
        if (getRecyclerView() == null) {
            mOnScrollListeners.add(onScrollListener);
            return;
        }

        getRecyclerView().addOnScrollListener(onScrollListener);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(KEY_CONTACT_ENTITY, mContactDetailsLiveData.getValue());
    }

    @Override
    public void onDestroyView() {
        // Clear all scroll listeners.
        getRecyclerView().removeOnScrollListener(null);
        super.onDestroyView();
    }

    @Override
    protected CharSequence getActionBarTitle() {
        return getString(R.string.toolbar_title_contact_details);
    }
}
