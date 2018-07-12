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

import static com.android.car.dialer.telecom.PhoneLoader.CALL_TYPE_ALL;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CallLog;

import androidx.lifecycle.LiveData;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import com.android.car.dialer.ui.CallLogListingTask;

import java.util.ArrayList;
import java.util.List;

/**
 * Live data which loads call history.
 */
public class CallHistoryLiveData extends LiveData<List<CallLogListingTask.CallLogItem>> {
    /** The default limit of loading call logs */
    public final static int DEFAULT_CALL_LOG_LIMIT = 100;
    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    private final Context mContext;
    private CursorLoader mCursorLoader;

    public CallHistoryLiveData(Context context) {
        mContext = context;
        mCursorLoader = createCallLogCursor(
                (loader, cursor) -> new CallLogListingTask(mContext, cursor,
                        this::setValue).execute());
    }

    @Override
    protected void onActive() {
        super.onActive();
        mCursorLoader.startLoading();
    }

    @Override
    protected void onInactive() {
        super.onInactive();
        mCursorLoader.stopLoading();
    }

    /**
     * Subclass override this function to filter different call log type. By default it filters
     * nothing.
     */
    protected int getCallType() {
        return CALL_TYPE_ALL;
    }

    /**
     * Subclass override this function to limit the number of logs being loaded. By default, the
     * limit is {@link #DEFAULT_CALL_LOG_LIMIT}.
     */
    protected int getLimit() {
        return DEFAULT_CALL_LOG_LIMIT;
    }

    private CursorLoader createCallLogCursor(Loader.OnLoadCompleteListener<Cursor> listener) {
        // We need to check for NULL explicitly otherwise entries with where READ is NULL
        // may not match either the query or its negation.
        // We consider the calls that are not yet consumed (i.e. IS_READ = 0) as "new".
        StringBuilder where = new StringBuilder();
        List<String> selectionArgs = new ArrayList<String>();

        int callType = getCallType();
        if (callType != CALL_TYPE_ALL) {
            // add a filter for call type
            where.append(String.format("(%s = ?)", CallLog.Calls.TYPE));
            selectionArgs.add(Integer.toString(callType));
        }
        String selection = where.length() > 0 ? where.toString() : null;

        Uri uri = CallLog.Calls.CONTENT_URI.buildUpon()
                .appendQueryParameter(CallLog.Calls.LIMIT_PARAM_KEY,
                        Integer.toString(getLimit()))
                .build();
        CursorLoader loader = new CursorLoader(mContext, uri, null, selection,
                selectionArgs.toArray(EMPTY_STRING_ARRAY), CallLog.Calls.DEFAULT_SORT_ORDER);
        loader.registerListener(0, listener);
        return loader;
    }
}
