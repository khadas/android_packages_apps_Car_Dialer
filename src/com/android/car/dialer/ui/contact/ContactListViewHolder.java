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

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.android.car.dialer.R;
import com.android.car.telephony.common.TelecomUtils;
import com.android.car.dialer.telecom.UiCallManager;
import com.android.car.telephony.common.Contact;
import com.android.car.telephony.common.PhoneNumber;

import java.util.List;

/**
 * {@link RecyclerView.ViewHolder} for contact list item, responsible for presenting and resetting
 * the UI on recycle.
 */
public class ContactListViewHolder extends RecyclerView.ViewHolder {
    private final ContactListAdapter.OnShowContactDetailListener mOnShowContactDetailListener;
    private final ImageView mAvatarView;
    private final TextView mTitleView;
    private final View mActionButton;

    public ContactListViewHolder(@NonNull View itemView,
            ContactListAdapter.OnShowContactDetailListener onShowContactDetailListener) {
        super(itemView);
        mOnShowContactDetailListener = onShowContactDetailListener;
        mAvatarView = itemView.findViewById(R.id.icon);
        mTitleView = itemView.findViewById(R.id.title);
        mActionButton = itemView.findViewById(R.id.action_button);
    }

    public void onBind(Contact contact) {
        List<PhoneNumber> phoneNumbers = contact.getNumbers();
        TelecomUtils.setContactBitmapAsync(mAvatarView.getContext(), mAvatarView, contact, null);
        mTitleView.setText(contact.getDisplayName());
        mActionButton.setOnClickListener(
                view -> mOnShowContactDetailListener.onShowContactDetail(contact));
        super.itemView.setOnClickListener(view -> {
            if (phoneNumbers.size() == 1) {
                UiCallManager.get().placeCall(phoneNumbers.get(0).getRawNumber());
            } else {
                mOnShowContactDetailListener.onShowContactDetail(contact);
            }
        });
    }
}
