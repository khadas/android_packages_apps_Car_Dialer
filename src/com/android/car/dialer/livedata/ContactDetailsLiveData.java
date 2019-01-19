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

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;

import androidx.annotation.NonNull;

import com.android.car.telephony.common.AsyncQueryLiveData;
import com.android.car.telephony.common.Contact;
import com.android.car.telephony.common.ObservableAsyncQuery;

/** {@link androidx.lifecycle.LiveData} for contact details that observes the contact change. */
public class ContactDetailsLiveData extends AsyncQueryLiveData<Contact> {
    private final Context mContext;

    public ContactDetailsLiveData(Context context, @NonNull Uri contactLookupUri) {
        super(context, getQueryParam(contactLookupUri));
        mContext = context;
    }

    @Override
    protected Contact convertToEntity(Cursor cursor) {
        if (cursor == null) {
            return null;
        }

        // Contact is not deleted.
        if (cursor.moveToFirst()) {
            Contact contact = Contact.fromCursor(mContext, cursor);
            while (cursor.moveToNext()) {
                contact.merge(Contact.fromCursor(mContext, cursor));
            }
            return contact;
        }
        return null;
    }

    /** Caller is responsible for passing the up to date and non null contact lookup uri. */
    private static ObservableAsyncQuery.QueryParam getQueryParam(@NonNull Uri contactLookupUri) {
        long contactId = ContentUris.parseId(contactLookupUri);
        return new ObservableAsyncQuery.QueryParam(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                /* projection= */null,
                /* selection= */ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                new String[]{String.valueOf(contactId)},
                /* orderBy= */null);
    }
}
