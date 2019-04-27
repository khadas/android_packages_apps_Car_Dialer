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
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.android.car.dialer.R;
import com.android.car.dialer.log.L;
import com.android.car.telephony.common.Contact;
import com.android.car.telephony.common.PhoneNumber;
import com.android.car.telephony.common.TelecomUtils;

import com.google.common.annotations.VisibleForTesting;


class ContactDetailsAdapter extends RecyclerView.Adapter<ContactDetailsViewHolder> {

    private static final String TAG = "CD.ContactDetailsAdapter";
    @VisibleForTesting
    static final String TELEPHONE_URI_PREFIX = "tel:";

    private static final int ID_HEADER = 1;
    private static final int ID_CONTENT = 2;

    private final Context mContext;

    private Contact mContact;

    public ContactDetailsAdapter(@NonNull Context context, @Nullable Contact contact) {
        super();
        mContext = context;
        mContact = contact;
    }

    void setContact(Contact contact) {
        L.d(TAG, "setContact %s", contact);
        mContact = contact;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? ID_HEADER : ID_CONTENT;
    }

    @Override
    public int getItemCount() {
        return mContact == null ? 1 : mContact.getNumbers().size() + 1;  // +1 for the header row.
    }

    @Override
    public ContactDetailsViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        int layoutResId;
        switch (viewType) {
            case ID_HEADER:
                layoutResId = R.layout.contact_detail_name_image;
                break;
            case ID_CONTENT:
                layoutResId = R.layout.contact_details_number;
                break;
            default:
                L.e(TAG, "Unknown view type: %d", viewType);
                return null;
        }

        View view = LayoutInflater.from(parent.getContext()).inflate(layoutResId, parent,
                false);
        return new ContactDetailsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ContactDetailsViewHolder viewHolder, int position) {
        switch (viewHolder.getItemViewType()) {
            case ID_HEADER:
                viewHolder.title.setText(
                        mContact == null ? mContext.getString(R.string.error_contact_deleted)
                                : mContact.getDisplayName());
                TelecomUtils.setContactBitmapAsync(mContext, viewHolder.avatar, mContact, null);
                // Just in case a viewholder object gets recycled.
                viewHolder.itemView.setOnClickListener(null);
                break;
            case ID_CONTENT:
                PhoneNumber phoneNumber = mContact.getNumbers().get(position - 1);

                viewHolder.title.setText(phoneNumber.getRawNumber());

                // Present the phone number type.
                CharSequence readableLabel = phoneNumber.getReadableLabel(mContext.getResources());
                if (phoneNumber.isPrimary()) {
                    viewHolder.text.setText(
                            mContext.getString(R.string.primary_number_description, readableLabel));
                } else {
                    viewHolder.text.setText(readableLabel);
                }

                viewHolder.itemView.setOnClickListener(v -> {
                    Intent callIntent = new Intent(Intent.ACTION_CALL);
                    callIntent.setData(Uri.parse(TELEPHONE_URI_PREFIX + phoneNumber.getRawNumber()));
                    mContext.startActivity(callIntent);
                });

                viewHolder.sendTextTouchTarget.setOnClickListener(v -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse("smsto:"));
                    intent.setType("vnd.android-dir/mms-sms");
                    intent.putExtra("address", phoneNumber.getRawNumber());
                    mContext.startActivity(intent);
                });

                break;
            default:
                Log.e(TAG, "Unknown view type " + viewHolder.getItemViewType());
                return;
        }
    }
}
