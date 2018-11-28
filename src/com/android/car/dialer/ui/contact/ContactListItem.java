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
package com.android.car.dialer.ui.contact;

import android.content.Context;

import androidx.car.widget.TextListItem;

import com.android.car.dialer.R;
import com.android.car.dialer.entity.Contact;
import com.android.car.dialer.telecom.TelecomUtils;

/**
 * ListItem for contact.
 */
public class ContactListItem extends TextListItem {
    private Context mContext;
    private Contact mContact;

    public ContactListItem(Context context, Contact contact) {
        super(context);
        mContext = context;
        mContact = contact;
    }

    @Override
    public void onBind(ViewHolder viewHolder) {
        super.onBind(viewHolder);
        String firstPhoneNumberString =
                mContact.getNumbers().isEmpty() ? "" : mContact.getNumbers().get(0).getNumber();
        TelecomUtils.setContactBitmapAsync(mContext, viewHolder.getPrimaryIcon(),
                mContact.getDisplayName(), firstPhoneNumberString);
        viewHolder.getContainerLayout().setBackgroundColor(
                mContext.getColor(R.color.contact_list_item_color));
    }
}
