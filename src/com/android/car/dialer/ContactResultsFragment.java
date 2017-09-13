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

package com.android.car.dialer;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Data;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.car.view.PagedListView;

import java.util.ArrayList;
import java.util.List;

/**
 * A fragment that will take a search query, look up contacts that match and display those
 * results as a list.
 */
public class ContactResultsFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "ContactResultsFragment";

    private static final String KEY_INITIAL_SEARCH_QUERY = "initial_search_query";

    private static final String[] CONTACT_DETAILS_PROJECTION = {
            Data.CONTACT_ID,
            Data.LOOKUP_KEY,
            Data.DISPLAY_NAME,
            Data.PHOTO_URI
    };

    /**
     * A selection criteria to filter contacts based on the query given by {@link #mSearchQuery}.
     * The query is search against partial matches of the contact's name
     * (StructuredName.DISPLAY_NAME) or phone number (Phone.NUMBER)
     */
    private static final String CONTACT_SELECTION =
            "(" + ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME + " LIKE ? "
                    + " AND " + Data.MIMETYPE + " = '" +
                    ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE + "'"
                    + ") OR ("
                    + ContactsContract.CommonDataKinds.Phone.NUMBER + " LIKE ? "
                    + " AND " + Data.MIMETYPE + " = '" +
                    ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE + "'" + ")";

    private final ContactResultsAdapter mAdapter = new ContactResultsAdapter();
    private PagedListView mContactResultList;
    private String mSearchQuery;

    private List<RecyclerView.OnScrollListener> mOnScrollListeners = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            setSearchQuery(args.getString(KEY_INITIAL_SEARCH_QUERY));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.contact_result_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mContactResultList = view.findViewById(R.id.contact_result_list);
        mContactResultList.setLightMode();
        mContactResultList.setAdapter(mAdapter);
        mContactResultList.getLayoutManager().setOffsetRows(false);

        RecyclerView recyclerView = mContactResultList.getRecyclerView();
        for (RecyclerView.OnScrollListener listener : mOnScrollListeners) {
            recyclerView.addOnScrollListener(listener);
        }

        mOnScrollListeners.clear();
    }

    /**
     * Adds a {@link android.support.v7.widget.RecyclerView.OnScrollListener} to be notified when
     * the contact list is scrolled.
     *
     * @see RecyclerView#addOnScrollListener(RecyclerView.OnScrollListener)
     */
    public void addOnScrollListener(RecyclerView.OnScrollListener onScrollListener) {
        // If the view has not been created yet, then queue the setting of the scroll listener.
        if (mContactResultList == null) {
            mOnScrollListeners.add(onScrollListener);
            return;
        }

        mContactResultList.getRecyclerView().addOnScrollListener(onScrollListener);
    }

    /**
     * Clears any results from a previous query and displays an empty list.
     */
    public void clearResults() {
        mSearchQuery = null;
        mAdapter.clear();
    }

    /**
     * Sets the search query that should be used to filter contacts.
     */
    public void setSearchQuery(String query) {
        mSearchQuery = query;

        if (!TextUtils.isEmpty(mSearchQuery)) {
            // Calling restartLoader so that the loader is always re-created with the new
            // search query.
            getLoaderManager().restartLoader(0, null /* args */, this /* callback */);
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "onLoadFinished(); count: " + data.getCount());
        }

        mAdapter.setData(data);
        data.close();
    }

    /**
     * Finds the contacts with names or phone numbers that match the search query
     */
    @Override
    public Loader<Cursor> onCreateLoader(int loaderId, Bundle args) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "onCreateLoader(); loaderId: " + loaderId + " with query: " + mSearchQuery);
        }

        String[] mSelectionArgs =
                new String[] { "%" + mSearchQuery + "%", "%" + mSearchQuery + "%" };

        return new CursorLoader(getContext(), Data.CONTENT_URI,
                CONTACT_DETAILS_PROJECTION, CONTACT_SELECTION,
                mSelectionArgs, null);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {}

    @Override
    public void onDestroy() {
        // Clear all scroll listeners.
        mContactResultList.getRecyclerView().removeOnScrollListener(null);
        super.onDestroy();
    }

    /**
     * Creates a new instance of the {@link ContactResultsFragment}.
     *
     * @param listener A scroll listener that will be notified when the list of search results has
     *                 been scrolled.
     * @param initialSearchQuery An optional search query that will be inputted when the fragment
     *                           starts up.
     */
    public static ContactResultsFragment newInstance(RecyclerView.OnScrollListener listener,
            @Nullable String initialSearchQuery) {
        ContactResultsFragment fragment = new ContactResultsFragment();
        fragment.addOnScrollListener(listener);

        if (!TextUtils.isEmpty(initialSearchQuery)) {
            Bundle args = new Bundle();
            args.putString(KEY_INITIAL_SEARCH_QUERY, initialSearchQuery);
            fragment.setArguments(args);
        }

        return fragment;
    }
}
