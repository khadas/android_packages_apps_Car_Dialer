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

package com.android.car.dialer.ui.common.entity;

import com.android.car.dialer.entity.PhoneCallLog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Ui representation of a call log.
 */
public class UiCallLog {
    private final String mTitle;
    private final String mNumber;
    private final List<PhoneCallLog.Record> mCallRecords;
    private String mText;

    public UiCallLog(String title, String text, String number,
            List<PhoneCallLog.Record> callRecords) {
        mTitle = title;
        mText = text;
        mNumber = number;
        mCallRecords = new ArrayList<>(callRecords);
    }

    /**
     * Returns the title of a call log item.
     */
    public String getTitle() {
        return mTitle;
    }

    /**
     * Returns the body text of a call log item.
     */
    public String getText() {
        return mText;
    }

    /**
     * Updates the body text.
     */
    public void setText(String text) {
        mText = text;
    }

    /**
     * Returns the number of this call log.
     */
    public String getNumber() {
        return mNumber;
    }

    /**
     * Returns a copy of combined call log records. See {@link PhoneCallLog.Record} for more details
     * of each record. Logs are sorted on call end time in decedent order.
     */
    public List<PhoneCallLog.Record> getCallRecords() {
        return getCallRecords(mCallRecords.size());
    }

    /**
     * Returns a copy of last N phone call records. Ordered by call end timestamp in descending
     * order.
     *
     * <p>If {@code n} is greater than the number of {@link #getCallRecords() records}, return
     * the entire record list.
     */
    public List<PhoneCallLog.Record> getCallRecords(int n) {
        int toIndex = Math.max(0, n);
        toIndex = Math.min(toIndex, mCallRecords.size());
        if (toIndex == 0) {
            return Collections.emptyList();
        } else {
            return new ArrayList<>(mCallRecords.subList(0, toIndex));
        }
    }

    /**
     * Returns the most recent call end timestamp of this log in milliseconds since the epoch.
     */
    public long getMostRecentCallEndTimestamp() {
        return mCallRecords.isEmpty() ? 0
                : mCallRecords.get(0).getCallEndTimestamp();
    }
}
