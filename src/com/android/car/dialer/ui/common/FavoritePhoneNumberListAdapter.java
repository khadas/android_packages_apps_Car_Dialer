/*
 * Copyright (C) 2019 The Android Open Source Project
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

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.car.dialer.R;
import com.android.car.telephony.common.PhoneNumber;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link ArrayAdapter} that presents the {@link PhoneNumber} and its type as two line list
 * item with stars to indicate selection.
 */
public class FavoritePhoneNumberListAdapter extends ArrayAdapter<PhoneNumber> {
    private final Context mContext;

    public FavoritePhoneNumberListAdapter(Context context) {
        super(context, R.layout.phone_number_favorite_list_item,
                R.id.phone_number, new ArrayList<>());
        mContext = context;
    }

    /** Sets the phone numbers to display */
    public void setPhoneNumbers(List<PhoneNumber> phoneNumbers) {
        clear();
        addAll(phoneNumbers);
    }

    @Override
    public View getView(int position, @Nullable View convertView,
            @NonNull ViewGroup parent) {
        View view = super.getView(position, convertView, parent);
        PhoneNumber phoneNumber = getItem(position);
        if (phoneNumber == null) {
            return view;
        }
        TextView phoneNumberView = view.findViewById(R.id.phone_number);
        phoneNumberView.setText(phoneNumber.getRawNumber());
        TextView phoneNumberDescriptionView = view.findViewById(R.id.phone_number_description);
        CharSequence readableLabel = phoneNumber.getReadableLabel(mContext.getResources());
        if (phoneNumber.isPrimary()) {
            phoneNumberDescriptionView.setText(
                    mContext.getString(R.string.primary_number_description, readableLabel));
        } else {
            phoneNumberDescriptionView.setText(readableLabel);
        }
        return view;
    }
}
