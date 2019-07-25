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

package com.android.car.dialer.storage;

import android.bluetooth.BluetoothDevice;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.text.TextUtils;

import androidx.annotation.WorkerThread;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.android.car.dialer.log.L;
import com.android.car.telephony.common.Contact;
import com.android.car.telephony.common.I18nPhoneNumberWrapper;
import com.android.car.telephony.common.PhoneNumber;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Repository for favorite numbers.It supports the operation to convert the favorite entities to
 * {@link Contact}s and add or delete entry.
 */
public class FavoriteNumberRepository {
    private static final String TAG = "CD.FavRepository";
    private static ExecutorService sSerializedExecutor;

    static {
        sSerializedExecutor = Executors.newSingleThreadExecutor();
    }

    private final Context mContext;
    private final FavoriteNumberDao mFavoriteNumberDao;
    private final LiveData<List<FavoriteNumberEntity>> mFavoriteNumbers;
    private Future<?> mConvertAllRunnableFuture;

    public FavoriteNumberRepository(Context context) {
        mContext = context.getApplicationContext();

        FavoriteNumberDatabase db = FavoriteNumberDatabase.getDatabase(mContext);
        mFavoriteNumberDao = db.favoriteNumberDao();
        mFavoriteNumbers = mFavoriteNumberDao.loadAll();
    }

    /** Returns the favorite number list. */
    public LiveData<List<FavoriteNumberEntity>> getFavoriteNumbers() {
        return mFavoriteNumbers;
    }

    /** Add a phone number to favorite. */
    public void addToFavorite(Contact contact, PhoneNumber phoneNumber) {
        FavoriteNumberEntity favoriteNumber = new FavoriteNumberEntity();
        favoriteNumber.setContactId(contact.getId());
        favoriteNumber.setContactLookupKey(contact.getLookupKey());
        favoriteNumber.setPhoneNumber(new CipherWrapper<>(
                phoneNumber.getRawNumber()));
        favoriteNumber.setAccountName(phoneNumber.getAccountName());
        favoriteNumber.setAccountType(phoneNumber.getAccountType());
        sSerializedExecutor.execute(() -> mFavoriteNumberDao.insert(favoriteNumber));
    }

    /** Remove a phone number from favorite. */
    public void removeFromFavorite(Contact contact, PhoneNumber phoneNumber) {
        List<FavoriteNumberEntity> favoriteNumbers = mFavoriteNumbers.getValue();
        if (favoriteNumbers == null) {
            return;
        }
        for (FavoriteNumberEntity favoriteNumberEntity : favoriteNumbers) {
            if (matches(favoriteNumberEntity, contact, phoneNumber)) {
                sSerializedExecutor.execute(() -> mFavoriteNumberDao.delete(favoriteNumberEntity));
            }
        }
    }

    /**
     * Convert the {@link FavoriteNumberEntity}s to {@link Contact}s and update contact id and
     * contact lookup key for all the entities that are out of date.
     */
    public void convertToContacts(Context context, final MutableLiveData<List<Contact>> results) {
        if (mConvertAllRunnableFuture != null) {
            mConvertAllRunnableFuture.cancel(false);
        }

        mConvertAllRunnableFuture = sSerializedExecutor.submit(() -> {
            if (mFavoriteNumbers.getValue() == null) {
                results.postValue(Collections.emptyList());
                return;
            }

            ContentResolver cr = context.getContentResolver();
            List<FavoriteNumberEntity> outOfDateList = new ArrayList<>();
            List<Contact> favoriteContacts = new ArrayList<>();
            List<FavoriteNumberEntity> favoriteNumbers = mFavoriteNumbers.getValue();
            for (FavoriteNumberEntity favoriteNumber : favoriteNumbers) {
                Contact contact = lookupContact(cr, favoriteNumber);
                if (contact != null) {
                    favoriteContacts.add(contact);
                    if (favoriteNumber.getContactId() != contact.getId()
                            || !TextUtils.equals(favoriteNumber.getContactLookupKey(),
                            contact.getLookupKey())) {
                        favoriteNumber.setContactLookupKey(contact.getLookupKey());
                        favoriteNumber.setContactId(contact.getId());
                        outOfDateList.add(favoriteNumber);
                    }
                }
            }
            results.postValue(favoriteContacts);
            if (!outOfDateList.isEmpty()) {
                mFavoriteNumberDao.updateAll(outOfDateList);
            }
        });
    }

    /** Remove favorite entries for devices that has been unpaired. */
    public void cleanup(Set<BluetoothDevice> devices) {
        L.d(TAG, "remove entries for unpaired devices except %s", devices);
        sSerializedExecutor.execute(() -> {
            List<String> deviceAddresses = new ArrayList<>();
            for (BluetoothDevice device : devices) {
                deviceAddresses.add(device.getAddress());
            }
            mFavoriteNumberDao.cleanup(deviceAddresses);
        });
    }

    @WorkerThread
    private Contact lookupContact(ContentResolver cr, FavoriteNumberEntity favoriteNumber) {
        Uri lookupUri = ContactsContract.Contacts.getLookupUri(
                favoriteNumber.getContactId(), favoriteNumber.getContactLookupKey());
        Uri refreshedUri = ContactsContract.Contacts.lookupContact(
                mContext.getContentResolver(), lookupUri);
        if (refreshedUri == null) {
            return null;
        }
        long contactId = ContentUris.parseId(refreshedUri);

        try (Cursor cursor = cr.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                /* projection= */null,
                /* selection= */ ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                new String[]{String.valueOf(contactId)},
                /* orderBy= */null)) {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    Contact contact = Contact.fromCursor(mContext, cursor);
                    if (contact.getNumbers().isEmpty()) {
                        continue;
                    }
                    if (numberMatches(favoriteNumber, contact.getNumbers().get(0))) {
                        return contact;
                    }
                }
            }
        }
        return null;
    }

    private boolean matches(FavoriteNumberEntity favoriteNumber, Contact contact,
            PhoneNumber phoneNumber) {
        if (TextUtils.equals(favoriteNumber.getContactLookupKey(), contact.getLookupKey())) {
            return numberMatches(favoriteNumber, phoneNumber);
        }

        return false;
    }

    private boolean numberMatches(FavoriteNumberEntity favoriteNumber, PhoneNumber phoneNumber) {
        if (favoriteNumber.getPhoneNumber() == null) {
            return false;
        }

        if (!TextUtils.equals(favoriteNumber.getAccountName(), phoneNumber.getAccountName())
                || !TextUtils.equals(favoriteNumber.getAccountType(),
                phoneNumber.getAccountType())) {
            return false;
        }

        I18nPhoneNumberWrapper i18nPhoneNumberWrapper = I18nPhoneNumberWrapper.Factory.INSTANCE.get(
                mContext, favoriteNumber.getPhoneNumber().get());
        return i18nPhoneNumberWrapper.equals(phoneNumber.getI18nPhoneNumberWrapper());
    }
}
