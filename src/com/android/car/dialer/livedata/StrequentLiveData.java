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
import android.provider.ContactsContract;

import com.android.car.dialer.ContactEntry;
import com.android.car.dialer.common.ObservableAsyncQuery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Live data which loads call strequent list.
 */
public class StrequentLiveData extends AsyncQueryLiveData<List<ContactEntry>> {
    private final static Uri STREQUENT_URI =
            ContactsContract.Contacts.CONTENT_STREQUENT_URI.buildUpon()
                    .appendQueryParameter(ContactsContract.STREQUENT_PHONE_ONLY, "true")
                    .appendQueryParameter(ContactsContract.REMOVE_DUPLICATE_ENTRIES,
                            "true").build();
    private static StrequentLiveData sStrequentLiveData;
    private final Context mContext;

    public static StrequentLiveData getInstance(Context applicationContext) {
        if (sStrequentLiveData == null) {
            sStrequentLiveData = new StrequentLiveData(applicationContext,
                    new ObservableAsyncQuery.QueryParam(STREQUENT_URI, null, null, null, null));
        }
        return sStrequentLiveData;
    }

    private StrequentLiveData(Context context, ObservableAsyncQuery.QueryParam queryParam) {
        super(context, queryParam);
        mContext = context;
    }

    @Override
    protected List<ContactEntry> convertToEntity(Cursor cursor) {
        Set<ContactEntry> entrySet = new HashSet<>();
        while (cursor.moveToNext()) {
            ContactEntry entry = ContactEntry.fromCursor(cursor, mContext);
            entrySet.add(entry);
        }

        List<ContactEntry> strequentContactEntries = new ArrayList<>(entrySet);
        Collections.sort(strequentContactEntries);
        return strequentContactEntries;
    }
}
