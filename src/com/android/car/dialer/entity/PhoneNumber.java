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

import android.content.res.Resources;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.telephony.PhoneNumberUtils;

import java.util.Objects;

import javax.annotation.Nullable;

/**
 * Contact phone number and its meta data.
 */
public class PhoneNumber {

    private final String mNumber;
    private final int mType;
    @Nullable
    private final String mLabel;

    public PhoneNumber(String number, int type) {
        this(number, type, null);
    }

    public PhoneNumber(String number, int type, String label) {
        mNumber = number;
        mType = type;
        mLabel = label;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PhoneNumber) {
            PhoneNumber other = (PhoneNumber) obj;
            return mNumber.equals(other.mNumber) && mType == other.mType;

        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mNumber, mType);
    }

    /**
     * Returns a human readable string label. For example, Home, Work, etc.
     */
    public CharSequence getReadableLabel(Resources res) {
        return Phone.getTypeLabel(res, mType, mLabel);
    }

    /**
     * Gets phone number.
     */
    public String getNumber() {
        return mNumber;
    }


    /**
     * Gets the type of phone number, for example Home or Work. Possible values are defined in
     * {@link android.provider.ContactsContract.CommonDataKinds.Phone CommonDataKinds.Phone}.
     */
    public int getType() {
        return mType;
    }

    /**
     * Gets the user defined label for the the contact method.
     */
    @Nullable
    public String getLabel() {
        return mLabel;
    }
}
