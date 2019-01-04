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

import android.app.Application;
import android.net.Uri;
import android.provider.ContactsContract;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.android.car.dialer.entity.Contact;
import com.android.car.dialer.livedata.ContactDetailsLiveData;

/** View model for the contact details page. */
public class ContactDetailsViewModel extends AndroidViewModel {
    public ContactDetailsViewModel(@NonNull Application application) {
        super(application);
    }

    /**
     * Builds the {@link LiveData} for the contact entity described by the given look up uri.
     *
     * @param contactLookupUri An {@link ContactsContract.Contacts#CONTENT_LOOKUP_URI} describing
     *                         the contact entry. It might have been out of date and should attempt
     *                         to refresh first.
     */
    public LiveData<Contact> getContactDetailsLiveData(Uri contactLookupUri) {
        Uri refreshedContactLookupUri = ContactsContract.Contacts.getLookupUri(
                getApplication().getContentResolver(), contactLookupUri);
        if (refreshedContactLookupUri == null) {
            MutableLiveData<Contact> deletedContactDetailsLiveData = new MutableLiveData<>();
            deletedContactDetailsLiveData.setValue(null);
            return deletedContactDetailsLiveData;
        }
        return new ContactDetailsLiveData(getApplication(), refreshedContactLookupUri);
    }
}
