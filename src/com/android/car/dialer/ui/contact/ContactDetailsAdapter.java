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
import android.provider.ContactsContract;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.car.util.ListItemBackgroundResolver;
import androidx.car.widget.PagedListView;
import androidx.recyclerview.widget.RecyclerView;

import com.android.car.dialer.R;
import com.android.car.dialer.log.L;
import com.android.car.dialer.telecom.TelecomUtils;

import java.util.ArrayList;
import java.util.List;

abstract class ContactDetailsAdapter extends RecyclerView.Adapter<ContactDetailsViewHolder>
        implements PagedListView.ItemCap {

    private static final String TAG = "CD.ContactDetailsAdapter";
    private static final String TELEPHONE_URI_PREFIX = "tel:";

    private static final int ID_HEADER = 1;
    private static final int ID_CONTENT = 2;

    private final Context mContext;
    @ColorInt
    private final int mIconTint;

    private final List<Pair<String, String>> mPhoneNumbers = new ArrayList<>();
    private String mContactName;

    public ContactDetailsAdapter(@NonNull Context context) {
        super();
        mContext = context;
        mIconTint = mContext.getColor(R.color.contact_details_icon_tint);
    }

    void setContactName(String contactName) {
        mContactName = contactName;
    }

    List<Pair<String, String>> getPhoneNumbers() {
        return mPhoneNumbers;
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? ID_HEADER : ID_CONTENT;
    }

    @Override
    public void setMaxItems(int maxItems) {
        // Ignore.
    }

    @Override
    public int getItemCount() {
        return mPhoneNumbers.size() + 1;  // +1 for the header row.
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
                viewHolder.title.setText(mContactName);
                if (!mPhoneNumbers.isEmpty()) {
                    String firstNumber = mPhoneNumbers.get(0).second;
                    TelecomUtils.setContactBitmapAsync(mContext, viewHolder.avatar,
                            mContactName, firstNumber);
                }
                // Just in case a viewholder object gets recycled.
                viewHolder.card.setOnClickListener(null);
                break;
            case ID_CONTENT:
                Pair<String, String> data = mPhoneNumbers.get(position - 1);
                viewHolder.title.setText(data.second);  // Type.
                viewHolder.text.setText(data.first);  // Number.
                viewHolder.leftIcon.setImageResource(R.drawable.ic_phone);
                viewHolder.leftIcon.setColorFilter(mIconTint);
                viewHolder.card.setOnClickListener(v -> {
                    Intent callIntent = new Intent(Intent.ACTION_CALL);
                    callIntent.setData(Uri.parse(TELEPHONE_URI_PREFIX + data.second));
                    mContext.startActivity(callIntent);
                });
                break;
            default:
                Log.e(TAG, "Unknown view type " + viewHolder.getItemViewType());
                return;
        }

        if (position == (getItemCount() - 1)) {
            // hide divider for last item.
            viewHolder.divider.setVisibility(View.GONE);
        } else {
            viewHolder.divider.setVisibility(View.VISIBLE);
        }
        ListItemBackgroundResolver.setBackground(viewHolder.card,
                viewHolder.getAdapterPosition(), getItemCount());
    }

    String getReadablePhoneType(int phoneType) {
        switch (phoneType) {
            case ContactsContract.CommonDataKinds.Phone.TYPE_HOME:
                return mContext.getString(R.string.type_home);
            case ContactsContract.CommonDataKinds.Phone.TYPE_WORK:
                return mContext.getString(R.string.type_work);
            case ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE:
                return mContext.getString(R.string.type_mobile);
            default:
                return mContext.getString(R.string.type_other);
        }
    }
}
