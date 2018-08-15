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
package com.android.car.dialer.ui.calllog;

import android.content.Context;

import androidx.car.widget.TextListItem;

import com.android.car.dialer.R;
import com.android.car.dialer.entity.Contact;
import com.android.car.dialer.telecom.InMemoryPhoneBook;
import com.android.car.dialer.telecom.TelecomUtils;
import com.android.car.dialer.ui.common.entity.UiCallLog;

/**
 * List item which is created by {@link CallHistoryListItemProvider} binds a call list item to a
 * list view item.
 */
public class CallLogListItem extends TextListItem {
    private final UiCallLog mCallLogItem;
    private final Context mContext;

    public CallLogListItem(Context context, UiCallLog callLog) {
        super(context);
        mCallLogItem = callLog;
        mContext = context;
    }

    @Override
    public void onBind(ViewHolder viewHolder) {
        super.onBind(viewHolder);
        Contact contact = InMemoryPhoneBook.get().lookupContactEntry(
                mCallLogItem.getNumber());
        String displayName = contact != null ? contact.getDisplayName() : null;
        TelecomUtils.setContactBitmapAsync(mContext, viewHolder.getPrimaryIcon(),
                displayName, mCallLogItem.getNumber());

        viewHolder.getContainerLayout().setBackgroundColor(
                mContext.getColor(R.color.call_history_list_item_color));
    }
}
