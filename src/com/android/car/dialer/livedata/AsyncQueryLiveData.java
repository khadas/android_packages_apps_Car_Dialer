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

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;

import androidx.annotation.MainThread;
import androidx.lifecycle.LiveData;

import com.android.car.dialer.common.ObservableAsyncQuery;

/**
 * Asynchronously queries a {@link ContentResolver} for a given query and observes the loaded data
 * for changes, reloading if necessary.
 */
public abstract class AsyncQueryLiveData<T> extends LiveData<T> {

    private ObservableAsyncQuery mObservableAsyncQuery;

    public AsyncQueryLiveData(Context context, ObservableAsyncQuery.QueryParam queryParam) {
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
     * Override this function to convert the loaded data. This function is called on main thread.
     */
    @MainThread
    protected abstract T convertToEntity(Cursor cursor);

    private void onCursorLoaded(Cursor cursor) {
        setValue(convertToEntity(cursor));
    }
}
