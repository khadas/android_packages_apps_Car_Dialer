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

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.car.widget.PagedListView;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.RecyclerView;

import com.android.car.dialer.R;
import com.android.car.dialer.entity.Contact;
import com.android.car.dialer.log.L;
import com.android.car.dialer.ui.view.VerticalListDividerDecoration;
import com.android.car.dialer.ui.common.DialerBaseFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * A fragment that shows the name of the contact, the photo and all listed phone numbers. It is
 * primarily used to respond to the results of search queries but supplyig it with the content://
 * uri of a contact should work too.
 */
public class ContactDetailsFragment extends DialerBaseFragment
        implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "CD.ContactDetailsFrag";

    // Key to load the contact details by passing in the Contact entity.
    private static final String KEY_CONTACT_ENTITY = "ContactEntity";

    // Key to load the contact details by passing in the content provider query uri.
    @Deprecated
    private static final String KEY_CONTACT_QUERY_URI = "ContactQueryUri";

    private static final int DETAILS_LOADER_QUERY_ID = 1;
    private static final int PHONE_LOADER_QUERY_ID = 2;

    private static final String[] CONTACT_DETAILS_PROJECTION = {
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.DISPLAY_NAME,
            ContactsContract.Contacts.PHOTO_URI,
            ContactsContract.Contacts.HAS_PHONE_NUMBER
    };

    private PagedListView mListView;
    private List<RecyclerView.OnScrollListener> mOnScrollListeners = new ArrayList<>();

    @Deprecated
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
    }

    @Override
    public void onStart() {
        super.onStart();
        L.d(TAG, "onStart");

        Uri contactUri = getArguments().getParcelable(KEY_CONTACT_QUERY_URI);
        if (contactUri != null) {
            getLoaderManager().initLoader(DETAILS_LOADER_QUERY_ID, null, this);
        }

        Contact contact = getArguments().getParcelable(KEY_CONTACT_ENTITY);
        if (contact != null) {
            mListView.setAdapter(new ContactDetailsEntityAdapter(getContext(), contact));
        }
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

    @Deprecated
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        L.d(TAG, "onCreateLoader id = %s", id);

        if (id != DETAILS_LOADER_QUERY_ID) {
            return null;
        }

        Uri contactUri = getArguments().getParcelable(KEY_CONTACT_QUERY_URI);
        return new CursorLoader(getContext(), contactUri, CONTACT_DETAILS_PROJECTION,
                null /* selection */, null /* selectionArgs */, null /* sortOrder */);
    }

    @Deprecated
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        L.d(TAG, "onLoadFinished");
        if (cursor.moveToFirst()) {
            mListView.setAdapter(new ContactDetailsCursorAdapter(getContext(), this, cursor));
        }
    }

    @Deprecated
    @Override
    public void onLoaderReset(Loader loader) {
        // No-op
    }

    @StringRes
    @Override
    protected int getActionBarTitleRes() {
        return R.string.contacts_title;
    }

    private static class ContactDetailsEntityAdapter extends ContactDetailsAdapter {
        public ContactDetailsEntityAdapter(@NonNull Context context, @NonNull Contact contact) {
            super(context);
            setContact(contact);
        }
    }

    @Deprecated
    private static class ContactDetailsCursorAdapter extends ContactDetailsAdapter {
        private final Context mContext;

        public ContactDetailsCursorAdapter(@NonNull Context context,
                @NonNull Fragment ownerFragment, Cursor cursor) {
            super(context);
            mContext = context;

            int idColIdx = cursor.getColumnIndex(ContactsContract.Contacts._ID);
            String contactId = cursor.getString(idColIdx);
            int hasPhoneColIdx = cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER);
            boolean hasPhoneNumber = Integer.parseInt(cursor.getString(hasPhoneColIdx)) > 0;

            if (!hasPhoneNumber) {
                return;
            }

            // Fetch the phone number from the contacts db using another loader.
            LoaderManager.getInstance(ownerFragment).initLoader(PHONE_LOADER_QUERY_ID,
                    null,
                    new LoaderManager.LoaderCallbacks<Cursor>() {
                        @Override
                        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
                            return new CursorLoader(
                                    mContext,
                                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                    null, /* All columns **/
                                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                                    new String[]{contactId},
                                    null /* sortOrder */);
                        }

                        public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
                            if (cursor == null) {
                                return;
                            }

                            cursor.moveToFirst();
                            Contact contact = Contact.fromCursor(context, cursor);
                            while (cursor.moveToNext()) {
                                contact.merge(Contact.fromCursor(context, cursor));
                            }
                            setContact(contact);
                        }

                        public void onLoaderReset(Loader loader) {
                        }
                    });
        }
    }
}
