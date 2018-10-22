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

package com.android.car.dialer.entity;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;

import androidx.annotation.Nullable;

import com.android.car.dialer.log.L;
import com.android.car.dialer.telecom.TelecomUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Encapsulates data about a phone Contact entry. Typically loaded from the local Contact store.
 */
public class Contact {
    private static final String TAG = "CD.Contact";

    /**
     * An unique primary key for searching an entry.
     */
    private int mId;

    /**
     * Whether this contact entry is starred by user.
     */
    private boolean mIsStarred;

    /**
     * Contact-specific information about whether or not a contact has been pinned by the user at
     * a particular position within the system contact application's user interface.
     */
    private int mPinnedPosition;

    /**
     * All phone numbers of this contact.
     */
    private Set<PhoneNumber> mPhoneNumbers = new HashSet<>();

    /**
     * The display name.
     */
    private String mDisplayName;

    /**
     * A URI that can be used to retrieve a thumbnail of the contact's photo.
     */
    private Uri mAvatarThumbnailUri;

    /**
     * A URI that can be used to retrieve the contact's full-size photo.
     */
    private Uri mAvatarUri;

    /**
     * An opaque value that contains hints on how to find the contact if its row id changed
     * as a result of a sync or aggregation. If a contact has multiple phone numbers, all phone
     * numbers are recorded in a single entry and they all have the same look up key in a single
     * load.
     */
    private String mLookupKey;

    /**
     * Whether this contact represents a voice mail.
     */
    private boolean mIsVoiceMail;

    /**
     * Parses a Contact entry for a Cursor loaded from the Contact Database.
     */
    public static Contact fromCursor(Context context, Cursor cursor) {
        int idColumn = cursor.getColumnIndex(BaseColumns._ID);
        int starredColumn = cursor.getColumnIndex(ContactsContract.Contacts.STARRED);
        int pinnedColumn = cursor.getColumnIndex(ContactsContract.Contacts.PINNED);
        int displayNameColumn = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
        int avatarUriColumn = cursor.getColumnIndex(ContactsContract.Contacts.PHOTO_URI);
        int avatarThumbnailColumn = cursor.getColumnIndex(
                ContactsContract.Contacts.PHOTO_THUMBNAIL_URI);
        int lookupKeyColumn = cursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY);
        int typeColumn = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE);
        int labelColumn = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LABEL);
        int numberColumn = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);

        PhoneNumber number = PhoneNumber.newInstance(context,
                cursor.getString(numberColumn),
                cursor.getInt(typeColumn),
                cursor.getString(labelColumn));

        Contact contact = new Contact();
        contact.mDisplayName = cursor.getString(displayNameColumn);
        contact.mPhoneNumbers.add(number);
        contact.mIsStarred = cursor.getInt(starredColumn) > 0;
        contact.mPinnedPosition = cursor.getInt(pinnedColumn);
        contact.mIsVoiceMail = TelecomUtils.isVoicemailNumber(context, number.getNumber());
        contact.mId = cursor.getInt(idColumn);

        String avatarUriStr = cursor.getString(avatarUriColumn);
        contact.mAvatarUri = avatarUriStr == null ? null : Uri.parse(avatarUriStr);

        String avatarThumbnailStringUri = cursor.getString(avatarThumbnailColumn);
        contact.mAvatarThumbnailUri = avatarThumbnailStringUri == null ? null : Uri.parse(
                avatarThumbnailStringUri);

        String lookUpKey = cursor.getString(lookupKeyColumn);
        if (lookUpKey != null) {
            contact.mLookupKey = lookUpKey;
        } else {
            L.w(TAG, "Look up key is null. Fallback to use display name");
            contact.mLookupKey = contact.mDisplayName;
        }
        return contact;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Contact && mLookupKey.equals(((Contact) obj).mLookupKey);
    }

    @Override
    public int hashCode() {
        return mLookupKey.hashCode();
    }

    @Override
    public String toString() {
        return mDisplayName + mPhoneNumbers;
    }

    public String getDisplayName() {
        return mDisplayName;
    }

    public boolean isVoicemail() {
        return mIsVoiceMail;
    }

    public int getId() {
        return mId;
    }

    @Nullable
    public Uri getAvatarUri() {
        return mAvatarUri;
    }

    public String getLookupKey() {
        return mLookupKey;
    }

    @Nullable
    public Uri getAvatarThumbnailUri() {
        return mAvatarThumbnailUri;
    }

    /**
     * Returns a copy of all phone numbers associated with this contact.
     */
    public List<PhoneNumber> getNumbers() {
        return new ArrayList<>(mPhoneNumbers);
    }

    public boolean isStarred() {
        return mIsStarred;
    }

    public int getPinnedPosition() {
        return mPinnedPosition;
    }

    /**
     * Merges a Contact entry with another if they represent different numbers of the same contact.
     *
     * @return A merged contact.
     */
    public Contact merge(Contact contact) {
        if (equals(contact)) {
            mPhoneNumbers.addAll(contact.getNumbers());
        }
        return this;
    }

    /**
     * Looks up a {@link PhoneNumber} of this contact for the given phone number. Returns {@code
     * null} if this contact doesn't contain the given phone number.
     */
    @Nullable
    public PhoneNumber getPhoneNumber(String number) {
        for (PhoneNumber phoneNumber : mPhoneNumbers) {
            if (PhoneNumberUtils.compare(phoneNumber.getNumber(), number)) {
                return phoneNumber;
            }
        }
        return null;
    }
}
