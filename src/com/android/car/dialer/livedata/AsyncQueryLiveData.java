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

import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

/**
 * Asynchronously queries a {@link ContentResolver} for a given query and observes the loaded data
 * for changes, reloading if necessary.
 */
public abstract class AsyncQueryLiveData<T> extends LiveData<T> {

    /**
     * Represents query parameters.
     */
    public static class QueryParam {
        final Uri mUri;
        final String[] mProjection;
        final String mSelection;
        final String[] mSelectionArgs;
        final String mOrderBy;

        public QueryParam(@NonNull Uri uri,
                @Nullable String[] projection,
                @Nullable String selection,
                @Nullable String[] selectionArgs,
                @Nullable String orderBy) {
            mUri = uri;
            mProjection = projection;
            mSelection = selection;
            mSelectionArgs = selectionArgs;
            mOrderBy = orderBy;
        }
    }

    private ObservableAsyncQuery mObservableAsyncQuery;

    public AsyncQueryLiveData(Context context, QueryParam queryParam) {
        mObservableAsyncQuery = new ObservableAsyncQuery(queryParam,
                context.getContentResolver(), this::onCursorLoaded);
    }

    @Override
    protected void onActive() {
        super.onActive();
        mObservableAsyncQuery.startQuery();
    }

    @Override
    protected void onInactive() {
        super.onInactive();
        mObservableAsyncQuery.stopQuery();
    }

    /**
     * Override this function to convert the loaded data.
     */
    protected abstract T convertToEntity(Cursor cursor);

    private void onCursorLoaded(Cursor cursor) {
        setValue(convertToEntity(cursor));
    }

    private static class ObservableAsyncQuery extends AsyncQueryHandler {
        private static final int QUERY_TOKEN = 0;

        /**
         * Called when query is finished.
         */
        interface OnQueryFinishedListener {
            /**
             * Called when the query is finished loading. This callbacks will also be called if data
             * changed.
             *
             * <p>Called on main thread.
             */
            void onQueryFinished(Cursor cursor);
        }

        private QueryParam mQueryParam;
        private Cursor mCurrentCursor;
        private OnQueryFinishedListener mOnQueryFinishedListener;
        private ContentObserver mContentObserver = new ContentObserver(this) {
            @Override
            public void onChange(boolean selfChange) {
                super.onChange(selfChange);
                startQuery();
            }
        };

        /**
         * @param queryParam Query arguments for the current query.
         * @param cr        ContentResolver.
         * @param listener  Listener which will be called when data is available.
         */
        public ObservableAsyncQuery(@NonNull QueryParam queryParam, @NonNull ContentResolver cr,
                @NonNull OnQueryFinishedListener listener) {
            super(cr);
            mQueryParam = queryParam;
            mOnQueryFinishedListener = listener;
        }

        /**
         * Starts the query and stops any pending query.
         */
        void startQuery() {
            cancelOperation(QUERY_TOKEN);
            startQuery(QUERY_TOKEN, null,
                    mQueryParam.mUri,
                    mQueryParam.mProjection,
                    mQueryParam.mSelection,
                    mQueryParam.mSelectionArgs,
                    mQueryParam.mOrderBy);
        }

        /**
         * Stops any pending query and also stops listening on the data set change.
         */
        void stopQuery() {
            if (mCurrentCursor != null) {
                mCurrentCursor.unregisterContentObserver(mContentObserver);
            }
            closeCurrentCursorIfNecessary();
            mCurrentCursor = null;
            cancelOperation(QUERY_TOKEN);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            super.onQueryComplete(token, cookie, cursor);
            cursor.registerContentObserver(mContentObserver);
            closeCurrentCursorIfNecessary();
            mCurrentCursor = cursor;
            if (mOnQueryFinishedListener != null) {
                mOnQueryFinishedListener.onQueryFinished(cursor);
            }
        }

        private void closeCurrentCursorIfNecessary() {
            if (mCurrentCursor != null && !mCurrentCursor.isClosed()) {
                mCurrentCursor.close();
            }
        }
    }
}
