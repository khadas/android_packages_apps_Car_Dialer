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

package com.android.car.dialer.ui.common;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.PhoneLookup;
import android.text.TextUtils;
import android.text.format.DateUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.android.car.dialer.R;
import com.android.car.dialer.livedata.HeartBeatLiveData;
import com.android.car.dialer.log.L;
import com.android.car.dialer.ui.common.entity.UiCallLog;
import com.android.car.telephony.common.Contact;
import com.android.car.telephony.common.InMemoryPhoneBook;
import com.android.car.telephony.common.PhoneCallLog;
import com.android.car.telephony.common.PhoneNumber;
import com.android.car.telephony.common.TelecomUtils;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Represents a list of {@link UiCallLog}s and label {@link String}s for UI representation. This
 * live data gets data source from both call log and contact list. It also refresh itself on the
 * relative time in the body text.
 */
public class UiCallLogLiveData extends MediatorLiveData<List<Object>> {
    private static final String TAG = "CD.UiCallLogLiveData";

    private static final String TYPE_AND_RELATIVE_TIME_JOINER = ", ";
    private final ExecutorService mExecutorService;
    private Future<?> mRunnableFuture;
    private Context mContext;

    public UiCallLogLiveData(Context context,
            HeartBeatLiveData heartBeatLiveData,
            LiveData<List<PhoneCallLog>> callHistoryLiveData,
            LiveData<List<Contact>> contactListLiveData) {
        mContext = context;
        mExecutorService = Executors.newSingleThreadExecutor();

        addSource(callHistoryLiveData, this::onCallHistoryChanged);
        addSource(contactListLiveData,
                (contacts) -> onContactsChanged(callHistoryLiveData.getValue()));
        addSource(heartBeatLiveData, (trigger) -> updateRelativeTime());
    }

    private void onCallHistoryChanged(@Nullable List<PhoneCallLog> callLogs) {
        if (mRunnableFuture != null) {
            mRunnableFuture.cancel(true);
        }
        Runnable runnable = () -> {
            postValue(convert(callLogs));
        };
        mRunnableFuture = mExecutorService.submit(runnable);
    }

    private void onContactsChanged(List<PhoneCallLog> callLogs) {
        // When contacts change, do not set value to trigger an update when there are no
        // call logs loaded yet. An update will switch the loading state to loaded in the ViewModel.
        if (getValue() == null || getValue().isEmpty()) {
            return;
        }
        onCallHistoryChanged(callLogs);
    }

    private void updateRelativeTime() {
        boolean hasChanged = false;
        List<Object> uiCallLogs = getValue();
        if (uiCallLogs == null) {
            return;
        }
        for (Object object : uiCallLogs) {
            if (object instanceof UiCallLog) {
                UiCallLog uiCallLog = (UiCallLog) object;
                String secondaryText = uiCallLog.getText();
                List<String> splittedSecondaryText = Splitter.on(
                        TYPE_AND_RELATIVE_TIME_JOINER).splitToList(secondaryText);

                String oldRelativeTime;
                String type = "";
                if (splittedSecondaryText.size() == 1) {
                    oldRelativeTime = splittedSecondaryText.get(0);
                } else if (splittedSecondaryText.size() == 2) {
                    type = splittedSecondaryText.get(0);
                    oldRelativeTime = splittedSecondaryText.get(1);
                } else {
                    return;
                }

                String newRelativeTime = getRelativeTime(uiCallLog.getMostRecentCallEndTimestamp());
                if (!oldRelativeTime.equals(newRelativeTime)) {
                    String newSecondaryText = getSecondaryText(type, newRelativeTime);
                    uiCallLog.setText(newSecondaryText);
                    hasChanged = true;
                }
            }
        }

        if (hasChanged) {
            setValue(getValue());
        }
    }

    @NonNull
    private List<Object> convert(@Nullable List<PhoneCallLog> phoneCallLogs) {
        if (phoneCallLogs == null) {
            return Collections.emptyList();
        }
        List<Object> uiCallLogs = new ArrayList<>();
        String preHeader = null;

        InMemoryPhoneBook inMemoryPhoneBook = InMemoryPhoneBook.get();
        for (PhoneCallLog phoneCallLog : phoneCallLogs) {
            String header = getHeader(phoneCallLog.getLastCallEndTimestamp());
            if (preHeader == null || (!header.equals(preHeader))) {
                uiCallLogs.add(header);
            }
            preHeader = header;

            String number = phoneCallLog.getPhoneNumberString();
            String relativeTime = getRelativeTime(phoneCallLog.getLastCallEndTimestamp());
            if (TelecomUtils.isVoicemailNumber(mContext, number)) {
                String title = mContext.getString(R.string.voicemail);
                UiCallLog uiCallLog = new UiCallLog(title, title, relativeTime, number, null,
                        phoneCallLog.getAllCallRecords());
                uiCallLogs.add(uiCallLog);
                continue;
            }

            String title;
            String altTitle = null;
            CharSequence typeLabel = "";
            Contact contact = null;

            // If InMemoryPhoneBook hasn't finished loading, there is still a chance that this
            // number can be found there later. So query will not be proceeded now.
            // TODO: will move to utils later.
            if (inMemoryPhoneBook.isLoaded()) {
                contact = inMemoryPhoneBook.lookupContactEntry(number);
                if (contact == null && !TextUtils.isEmpty(number)) {
                    ContentResolver cr = mContext.getContentResolver();
                    try (Cursor cursor = cr.query(
                            Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI,
                                    Uri.encode(number)),
                            new String[]{
                                    PhoneLookup.LOOKUP_KEY,
                                    PhoneLookup.TYPE,
                                    PhoneLookup.LABEL,
                            },
                            null, null, null)) {

                        if (cursor != null && cursor.moveToFirst()) {
                            int lookupKeyColIdx = cursor.getColumnIndex(PhoneLookup.LOOKUP_KEY);
                            int typeColumn = cursor.getColumnIndex(PhoneLookup.TYPE);
                            int labelColumn = cursor.getColumnIndex(PhoneLookup.LABEL);

                            contact = inMemoryPhoneBook.lookupContactByKey(
                                    cursor.getString(lookupKeyColIdx),
                                    phoneCallLog.getAccountName());
                            int type = cursor.getInt(typeColumn);
                            String label = cursor.getString(labelColumn);
                            typeLabel = ContactsContract.CommonDataKinds.Phone.getTypeLabel(
                                    mContext.getResources(), type, label);
                        }
                    }
                }
            }

            if (contact != null && contact.getDisplayName() != null) {
                title = contact.getDisplayName();
                altTitle = contact.getDisplayNameAlt();
            } else if (!TextUtils.isEmpty(number)) {
                title = TelecomUtils.getFormattedNumber(mContext, number);
            } else {
                title = mContext.getString(R.string.unknown);
            }
            PhoneNumber phoneNumber = contact != null
                    ? contact.getPhoneNumber(mContext, number) : null;

            UiCallLog uiCallLog = new UiCallLog(
                    title,
                    altTitle == null ? title : altTitle,
                    getSecondaryText(
                            TextUtils.isEmpty(typeLabel) ? getType(phoneNumber) : typeLabel,
                            relativeTime),
                    number,
                    contact,
                    phoneCallLog.getAllCallRecords());

            uiCallLogs.add(uiCallLog);
        }
        L.i(TAG, "phoneCallLog size: %d, uiCallLog size: %d",
                phoneCallLogs.size(), uiCallLogs.size());

        return uiCallLogs;
    }

    private String getRelativeTime(long millis) {
        boolean validTimestamp = millis > 0;

        return validTimestamp ? DateUtils.getRelativeTimeSpanString(
                millis, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE).toString() : "";
    }

    private String getSecondaryText(@Nullable CharSequence type, String relativeTime) {
        if (!TextUtils.isEmpty(type)) {
            return Joiner.on(TYPE_AND_RELATIVE_TIME_JOINER).join(type, relativeTime);
        } else {
            return relativeTime;
        }
    }

    private CharSequence getType(@Nullable PhoneNumber phoneNumber) {
        return phoneNumber != null ? phoneNumber.getReadableLabel(mContext.getResources()) : "";
    }

    private String getHeader(long calllogTime) {
        // Calllog times are acquired before getting currentTime, so calllogTime is always
        // less than currentTime
        if (DateUtils.isToday(calllogTime)) {
            return mContext.getResources().getString(R.string.call_log_header_today);
        }

        Calendar callLogCalender = Calendar.getInstance();
        callLogCalender.setTimeInMillis(calllogTime);
        callLogCalender.add(Calendar.DAY_OF_YEAR, 1);

        if (DateUtils.isToday(callLogCalender.getTimeInMillis())) {
            return mContext.getResources().getString(R.string.call_log_header_yesterday);
        }

        return mContext.getResources().getString(R.string.call_log_header_older);
    }
}
