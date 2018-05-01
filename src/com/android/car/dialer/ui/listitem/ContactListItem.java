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
package com.android.car.dialer.ui.listitem;

import android.content.Context;
import android.content.res.Resources;

import androidx.car.widget.TextListItem;

import com.android.car.apps.common.CircleBitmapDrawable;
import com.android.car.apps.common.LetterTileDrawable;
import com.android.car.dialer.telecom.ContactBitmapWorker;
import com.android.car.dialer.ui.ContactListFragment;

/**
 * ListItem for contact.
 */
public class ContactListItem extends TextListItem {
    private Context mContext;
    private ContactListFragment.ContactItem mContactItem;

    public ContactListItem(Context context, ContactListFragment.ContactItem contactItem) {
        super(context);
        mContext = context;
        mContactItem = contactItem;
    }

    @Override
    public void onBind(ViewHolder viewHolder) {
        super.onBind(viewHolder);
        ContactBitmapWorker.loadBitmap(mContext.getContentResolver(), viewHolder.getPrimaryIcon(),
                mContactItem.mNumber,
                bitmap -> {
                    Resources r = mContext.getResources();
                    if (bitmap != null) {
                        setPrimaryActionIcon(new CircleBitmapDrawable(r, bitmap), true);
                    } else {
                        LetterTileDrawable letterTileDrawable = new LetterTileDrawable(r);
                        letterTileDrawable.setContactDetails(mContactItem.mDisplayName,
                                mContactItem.mNumber);
                        letterTileDrawable.setIsCircular(true);
                        setPrimaryActionIcon(letterTileDrawable, true);
                    }
                    // force rebind the view.
                    super.onBind(viewHolder);
                });
    }
}
