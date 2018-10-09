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
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;

import androidx.car.widget.ListItem;
import androidx.car.widget.ListItemProvider;
import androidx.car.widget.TextListItem;

import com.android.car.dialer.R;
import com.android.car.dialer.entity.Contact;
import com.android.car.dialer.telecom.UiCallManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides ListItem for contact list.
 */
public class ContactListItemProvider extends ListItemProvider {
    private final List<TextListItem> mListItems = new ArrayList<>();
    private final OnShowContactDetailListener mOnShowContactDetailListener;
    private final Context mContext;

    public interface OnShowContactDetailListener {
        void onShowContactDetail(int contactId, String lookupKey);
    }

    public ContactListItemProvider(Context context,
            OnShowContactDetailListener onShowContactDetailListener) {
        mContext = context;
        mOnShowContactDetailListener = onShowContactDetailListener;
    }

    public void setContacts(List<Contact> contacts) {
        mListItems.clear();
        for (Contact contact : contacts) {
            ContactListItem textListItem = new ContactListItem(mContext, contact);
            // set a primary icon place holder drawable.
            textListItem.setPrimaryActionIcon((Icon) null,
                    TextListItem.PRIMARY_ACTION_ICON_SIZE_MEDIUM);
            textListItem.setTitle(contact.getDisplayName());
            textListItem.setOnClickListener(
                    (v) -> {
                        // TODO: create visual different between these two case.
                        if (contact.getNumbers().size() == 1) {
                            UiCallManager.get().placeCall(
                                    contact.getNumbers().get(0).getNumber());
                        } else {
                            mOnShowContactDetailListener.onShowContactDetail(contact.getId(),
                                    contact.getLookupKey());
                        }

                    });
            Drawable supplementalIconDrawable = mContext.getDrawable(R.drawable.ic_contact);
            supplementalIconDrawable.setTint(mContext.getColor(R.color.car_tint));
            int iconSize = mContext.getResources().getDimensionPixelSize(
                    R.dimen.car_primary_icon_size);
            supplementalIconDrawable.setBounds(0, 0, iconSize, iconSize);
            textListItem.setSupplementalIcon(supplementalIconDrawable, true,
                    (v) -> mOnShowContactDetailListener.onShowContactDetail(contact.getId(),
                            contact.getLookupKey()));
            mListItems.add(textListItem);
        }
    }

    @Override
    public ListItem get(int position) {
        return mListItems.get(position);
    }

    @Override
    public int size() {
        return mListItems.size();
    }
}
