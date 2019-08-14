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

package com.android.car.dialer.ui.search;

import android.app.Application;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.android.car.telephony.common.Contact;
import com.android.car.telephony.common.InMemoryPhoneBook;
import com.android.car.telephony.common.ObservableAsyncQuery;
import com.android.car.telephony.common.QueryParam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** {link AndroidViewModel} used for search functionality. */
public class ContactResultsViewModel extends AndroidViewModel {
    private static final String[] CONTACT_DETAILS_PROJECTION = {
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.LOOKUP_KEY
    };

    private final ContactResultsLiveData mContactSearchResultsLiveData;
    private final MutableLiveData<String> mSearchQueryLiveData;

    public ContactResultsViewModel(@NonNull Application application) {
        super(application);
        mSearchQueryLiveData = new MutableLiveData<>();
        mContactSearchResultsLiveData = new ContactResultsLiveData(application.getContentResolver(),
                mSearchQueryLiveData);
    }

    void setSearchQuery(String searchQuery) {
        if (TextUtils.equals(mSearchQueryLiveData.getValue(), searchQuery)) {
            return;
        }

        mSearchQueryLiveData.setValue(searchQuery);
    }

    LiveData<List<Contact>> getContactSearchResults() {
        return mContactSearchResultsLiveData;
    }

    String getSearchQuery() {
        return mSearchQueryLiveData.getValue();
    }

    private static class ContactResultsLiveData extends MediatorLiveData<List<Contact>> {
        private final SearchQueryParamProvider mSearchQueryParamProvider;
        private final ObservableAsyncQuery mObservableAsyncQuery;

        ContactResultsLiveData(ContentResolver contentResolver,
                LiveData<String> searchQueryLiveData) {
            mSearchQueryParamProvider = new SearchQueryParamProvider(searchQueryLiveData);
            mObservableAsyncQuery = new ObservableAsyncQuery(mSearchQueryParamProvider,
                    contentResolver, this::onQueryFinished);

            addSource(InMemoryPhoneBook.get().getContactsLiveData(), this::onContactsChange);
            addSource(searchQueryLiveData, this::onSearchQueryChanged);
        }

        private void onContactsChange(List<Contact> contactList) {
            if (contactList == null || contactList.isEmpty()) {
                mObservableAsyncQuery.stopQuery();
                setValue(Collections.emptyList());
            } else {
                mObservableAsyncQuery.startQuery();
            }
        }

        private void onSearchQueryChanged(String searchQuery) {
            if (TextUtils.isEmpty(searchQuery)) {
                mObservableAsyncQuery.stopQuery();
                setValue(Collections.emptyList());
            } else {
                mObservableAsyncQuery.startQuery();
            }
        }

        private void onQueryFinished(@Nullable Cursor cursor) {
            if (cursor == null) {
                setValue(Collections.emptyList());
                return;
            }

            List<Contact> contacts = new ArrayList<>();
            while (cursor.moveToNext()) {
                int lookupColIdx = cursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY);
                Contact contact = InMemoryPhoneBook.get().lookupContactByKey(
                        cursor.getString(lookupColIdx));
                if (contact != null) {
                    contacts.add(contact);
                }
            }
            setValue(contacts);
            cursor.close();
        }
    }

    private static class SearchQueryParamProvider implements QueryParam.Provider {
        private final LiveData<String> mSearchQueryLiveData;

        private SearchQueryParamProvider(LiveData<String> searchQueryLiveData) {
            mSearchQueryLiveData = searchQueryLiveData;
        }

        @Nullable
        @Override
        public QueryParam getQueryParam() {
            Uri lookupUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_FILTER_URI,
                    Uri.encode(mSearchQueryLiveData.getValue()));
            return new QueryParam(lookupUri, CONTACT_DETAILS_PROJECTION,
                    ContactsContract.Contacts.HAS_PHONE_NUMBER + "!=0",
                    /* selectionArgs= */null, /* orderBy= */null);
        }
    }
}
