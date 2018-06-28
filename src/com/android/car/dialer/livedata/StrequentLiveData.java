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

package com.android.car.dialer.livedata;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.ContactsContract;

import androidx.lifecycle.LiveData;
import androidx.loader.content.CursorLoader;

import com.android.car.dialer.ContactEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Live data which loads call strequent list.
 */
public class StrequentLiveData extends LiveData<List<ContactEntry>> {
    private final Context mContext;
    private CursorLoader mCursorLoader;

    public StrequentLiveData(Context context) {
        mContext = context;
        mCursorLoader = newStrequentContactLoader(mContext);
        mCursorLoader.registerListener(0 /* loader id */,
                (loader, cursor) -> new StrequentCursorConversionTask().execute(cursor));
    }

    @Override
    protected void onActive() {
        super.onActive();
        mCursorLoader.startLoading();
    }

    @Override
    protected void onInactive() {
        super.onInactive();
        mCursorLoader.stopLoading();
    }

    private CursorLoader newStrequentContactLoader(Context context) {
        Uri uri = ContactsContract.Contacts.CONTENT_STREQUENT_URI.buildUpon()
                .appendQueryParameter(ContactsContract.STREQUENT_PHONE_ONLY, "true")
                .appendQueryParameter(ContactsContract.REMOVE_DUPLICATE_ENTRIES, "true").build();

        return new CursorLoader(context, uri, null, null, null, null);
    }

    private class StrequentCursorConversionTask extends AsyncTask<Cursor, Void, Void> {

        @Override
        protected Void doInBackground(Cursor... cursors) {
            Set<ContactEntry> entrySet = new HashSet<>();

            Cursor cursor = cursors[0];
            while (cursor.moveToNext()) {
                ContactEntry entry = ContactEntry.fromCursor(cursor, mContext);
                entrySet.add(entry);
            }

            List<ContactEntry> strequentContactEntries = new ArrayList<>(entrySet);
            Collections.sort(strequentContactEntries);
            postValue(strequentContactEntries);

            return null;
        }
    }
}
