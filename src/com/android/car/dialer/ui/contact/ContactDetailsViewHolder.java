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

import androidx.recyclerview.widget.RecyclerView;

import com.android.car.dialer.R;

/** ViewHolder for {@link ContactDetailsFragment}. */
class ContactDetailsViewHolder extends RecyclerView.ViewHolder {
    View sendTextTouchTarget;
    TextView title;
    TextView text;
    ImageView avatar;

    ContactDetailsViewHolder(View v) {
        super(v);
        sendTextTouchTarget = v.findViewById(R.id.contact_details_text_button_touchtarget);
        title = v.findViewById(R.id.title);
        text = v.findViewById(R.id.text);
        avatar = v.findViewById(R.id.avatar);
    }
}
