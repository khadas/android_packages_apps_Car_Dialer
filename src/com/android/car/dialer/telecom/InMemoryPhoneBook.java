package com.android.car.dialer.telecom;

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.android.car.dialer.common.ObservableAsyncQuery;
import com.android.car.dialer.entity.Contact;
import com.android.car.dialer.entity.PhoneNumber;
import com.android.car.dialer.log.L;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A singleton statically accessible helper class which pre-loads contacts list into memory so
 * that they can be accessed more easily and quickly.
 */
public class InMemoryPhoneBook {
    private static final String TAG = "CD.InMemoryPhoneBook";
    private static InMemoryPhoneBook sInMemoryPhoneBook;

    private final Context mContext;

    private boolean mIsLoaded = false;

    private ObservableAsyncQuery mObservableAsyncQuery;
    private List<Contact> mContacts = new ArrayList<>();
    private MutableLiveData<List<Contact>> mContactsLiveData = new MutableLiveData<>();

    private InMemoryPhoneBook(Context context) {
        mContext = context;
    }

    /**
     * Initialize the globally accessible {@link InMemoryPhoneBook}.
     */
    public static InMemoryPhoneBook init(Context context) {
        if (sInMemoryPhoneBook == null) {
            sInMemoryPhoneBook = new InMemoryPhoneBook(context);
            sInMemoryPhoneBook.onInit();
        } else {
            throw new IllegalStateException("Call teardown before reinitialized PhoneBook");
        }
        return get();
    }

    public static InMemoryPhoneBook get() {
        if (sInMemoryPhoneBook != null) {
            return sInMemoryPhoneBook;
        } else {
            throw new IllegalStateException("Call init before get InMemoryPhoneBook");
        }
    }

    /**
     * Tears down the globally accessible {@link InMemoryPhoneBook}.
     */
    public static void tearDown() {
        sInMemoryPhoneBook.mObservableAsyncQuery.stopQuery();
        sInMemoryPhoneBook = null;
    }

    private void onInit() {
        L.v(TAG, "onInit");
        String selection = ContactsContract.Data.MIMETYPE + " = ?";
        String[] selectionArgs = new String[1];
        selectionArgs[0] = ContactsContract.CommonDataKinds.Phone
                .CONTENT_ITEM_TYPE;
        ObservableAsyncQuery.QueryParam contactListQueryParam = new ObservableAsyncQuery.QueryParam(
                ContactsContract.Data.CONTENT_URI,
                null,
                selection,
                selectionArgs,
                ContactsContract.Contacts.DISPLAY_NAME + " ASC ");

        mObservableAsyncQuery = new ObservableAsyncQuery(contactListQueryParam,
                mContext.getContentResolver(), this::onDataLoaded);
        mObservableAsyncQuery.startQuery();
    }

    public boolean isLoaded() {
        return mIsLoaded;
    }

    /**
     * Returns a {@link LiveData} which monitors the contact list changes.
     */
    public LiveData<List<Contact>> getContactsLiveData() {
        return mContactsLiveData;
    }

    /**
     * Looks up a {@link Contact} by the given phone number. Returns null if can't find a Contact or
     * the {@link InMemoryPhoneBook} is still loading.
     */
    @Nullable
    public Contact lookupContactEntry(String phoneNumber) {
        L.v(TAG, "lookupContactEntry: " + phoneNumber);
        if (!isLoaded()) {
            L.w(TAG, "looking up a contact while loading.");
            return null;
        }

        for (Contact contact : mContacts) {
            for (PhoneNumber number : contact.getNumbers()) {
                if (PhoneNumberUtils.compare(mContext, phoneNumber, number.getNumber())) {
                    return contact;
                }
            }
        }
        return null;
    }

    private void onDataLoaded(Cursor cursor) {
        Map<String, Contact> result = new LinkedHashMap<>();
        while (cursor.moveToNext()) {
            Contact contact = Contact.fromCursor(mContext, cursor);
            String lookupKey = contact.getLookupKey();
            if (result.containsKey(lookupKey)) {
                Contact existingContact = result.get(lookupKey);
                existingContact.merge(contact);
            } else {
                result.put(lookupKey, contact);
            }
        }
        mIsLoaded = true;
        mContacts.clear();
        mContacts.addAll(result.values());
        mContactsLiveData.setValue(mContacts);
        L.d(TAG, "onDataLoaded " + mContacts);
        cursor.close();
    }
}
