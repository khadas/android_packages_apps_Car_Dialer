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
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.car.widget.PagedListView;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.RecyclerView;

import com.android.car.dialer.R;
import com.android.car.dialer.entity.Contact;
import com.android.car.dialer.ui.view.VerticalListDividerDecoration;
import com.android.car.dialer.ui.common.DialerBaseFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * A fragment that shows the name of the contact, the photo and all listed phone numbers. It is
 * primarily used to respond to the results of search queries but supplyig it with the content://
 * uri of a contact should work too.
 */
public class ContactDetailsFragment extends DialerBaseFragment {
    private static final String TAG = "CD.ContactDetailsFrag";

    // Key to load the contact details by passing in the Contact entity.
    private static final String KEY_CONTACT_ENTITY = "ContactEntity";

    // Key to load the contact details by passing in the content provider query uri.
    private static final String KEY_CONTACT_QUERY_URI = "ContactQueryUri";

    private PagedListView mListView;
    private List<RecyclerView.OnScrollListener> mOnScrollListeners = new ArrayList<>();

    public static ContactDetailsFragment newInstance(
            Uri uri, @Nullable RecyclerView.OnScrollListener listener) {
        ContactDetailsFragment fragment = new ContactDetailsFragment();
        if (listener != null) {
            fragment.addOnScrollListener(listener);
        }

        Bundle args = new Bundle();
        args.putParcelable(KEY_CONTACT_QUERY_URI, uri);
        fragment.setArguments(args);

        return fragment;
    }

    public static ContactDetailsFragment newInstance(
            Contact contact, @Nullable RecyclerView.OnScrollListener listener) {
        ContactDetailsFragment fragment = new ContactDetailsFragment();
        if (listener != null) {
            fragment.addOnScrollListener(listener);
        }

        Bundle args = new Bundle();
        args.putParcelable(KEY_CONTACT_ENTITY, contact);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.contact_details_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mListView = view.findViewById(R.id.list_view);

        RecyclerView recyclerView = mListView.getRecyclerView();
        PagedListView.LayoutParams layoutParams =
                (PagedListView.LayoutParams) recyclerView.getLayoutParams();
        layoutParams.gravity = Gravity.CENTER_HORIZONTAL;
        layoutParams.width = PagedListView.LayoutParams.WRAP_CONTENT;
        recyclerView.addItemDecoration(new VerticalListDividerDecoration(getContext(), true));
        for (RecyclerView.OnScrollListener listener : mOnScrollListeners) {
            recyclerView.addOnScrollListener(listener);
        }

        mOnScrollListeners.clear();

        Contact contact = getArguments().getParcelable(KEY_CONTACT_ENTITY);
        ContactDetailsAdapter contactDetailsAdapter = new ContactDetailsAdapter(getContext(),
                contact);
        mListView.setAdapter(contactDetailsAdapter);

        Uri contactLookupUri;
        if (contact != null) {
            contactLookupUri = contact.getLookupUri();
        } else {
            contactLookupUri = getArguments().getParcelable(KEY_CONTACT_QUERY_URI);
        }
        ContactDetailsViewModel contactDetailsViewModel = ViewModelProviders.of(this).get(
                ContactDetailsViewModel.class);
        LiveData<Contact> contactDetailsLiveData =
                contactDetailsViewModel.getContactDetailsLiveData(contactLookupUri);
        contactDetailsLiveData.observe(this, contactDetailsAdapter::setContact);
    }

    /**
     * Adds a {@link androidx.recyclerview.widget.RecyclerView.OnScrollListener} to be notified when
     * the contact details are scrolled.
     *
     * @see RecyclerView#addOnScrollListener(RecyclerView.OnScrollListener)
     */
    public void addOnScrollListener(RecyclerView.OnScrollListener onScrollListener) {
        // If the view has not been created yet, then queue the setting of the scroll listener.
        if (mListView == null) {
            mOnScrollListeners.add(onScrollListener);
            return;
        }

        mListView.getRecyclerView().addOnScrollListener(onScrollListener);
    }

    @Override
    public void onDestroy() {
        // Clear all scroll listeners.
        mListView.getRecyclerView().removeOnScrollListener(null);
        super.onDestroy();
    }

    @StringRes
    @Override
    protected int getActionBarTitleRes() {
        return R.string.contacts_title;
    }
}
