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

import static com.google.i18n.phonenumbers.PhoneNumberUtil.MatchType.EXACT_MATCH;
import static com.google.i18n.phonenumbers.PhoneNumberUtil.MatchType.NSN_MATCH;
import static com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL;

import android.content.Context;

import androidx.annotation.Nullable;

import com.android.car.dialer.telecom.TelecomUtils;

import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import java.util.Objects;

import javax.annotation.Nonnull;

/**
 * Wraps the i18n {@link Phonenumber.PhoneNumber} with a raw phone number that creates it. It
 * facilitates the invalid phone number comparison where a raw phone number can't be converted into
 * an i18n phone number.
 */
public class I18nPhoneNumberWrapper {
    private final Phonenumber.PhoneNumber mI18nPhoneNumber;
    private final String mRawNumber;
    private final String mNumber;

    /**
     * Creates a new instance of {@link I18nPhoneNumberWrapper}.
     *
     * @param rawNumber A potential phone number. If it can be parsed as a valid phone number,
     *                  {@link #getNumber()} will return a formatted number.
     */
    public static I18nPhoneNumberWrapper newInstance(@Nonnull Context context,
            @Nonnull String rawNumber) {
        Phonenumber.PhoneNumber i18nPhoneNumber = TelecomUtils.createI18nPhoneNumber(context,
                rawNumber);
        return new I18nPhoneNumberWrapper(rawNumber, i18nPhoneNumber);
    }

    private I18nPhoneNumberWrapper(String rawNumber,
            @Nullable Phonenumber.PhoneNumber i18nPhoneNumber) {
        mI18nPhoneNumber = i18nPhoneNumber;
        mRawNumber = rawNumber;
        mNumber = (i18nPhoneNumber == null)
                ? rawNumber
                : PhoneNumberUtil.getInstance().format(i18nPhoneNumber, INTERNATIONAL);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof I18nPhoneNumberWrapper) {
            I18nPhoneNumberWrapper other = (I18nPhoneNumberWrapper) obj;
            if (mI18nPhoneNumber != null && other.mI18nPhoneNumber != null) {
                PhoneNumberUtil.MatchType matchType = PhoneNumberUtil.getInstance().isNumberMatch(
                        mI18nPhoneNumber, other.mI18nPhoneNumber);
                return matchType == EXACT_MATCH || matchType == NSN_MATCH;
            } else if (mI18nPhoneNumber == null && other.mI18nPhoneNumber == null) {
                // compare the raw number directly.
                return mRawNumber.equals(other.mRawNumber);
            } else {
                return false;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(mI18nPhoneNumber);
    }

    /**
     * Returns the unformatted phone number used to create this class.
     */
    public String getRawNumber() {
        return mRawNumber;
    }

    /**
     * Returns the formatted number if the raw number passed to {@link #newInstance} is a valid
     * phone number. Otherwise, returns the raw number.
     *
     * <P>The number is formatted with {@link PhoneNumberUtil.PhoneNumberFormat#INTERNATIONAL
     * international} format.
     */
    public String getNumber() {
        return mNumber;
    }
}
