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

package com.android.car.dialer.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;

import androidx.annotation.IntDef;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.android.car.dialer.ui.calllog.CallHistoryFragment;
import com.android.car.dialer.ui.contact.ContactListFragment;
import com.android.car.dialer.ui.dialpad.DialpadFragment;
import com.android.car.dialer.ui.favorite.FavoriteFragment;
import com.android.car.dialer.R;

/**
 * Adapter containing all fragments used in the view pager
 */
public class TelecomPagerAdapter extends FragmentPagerAdapter {

    @IntDef({
            PAGE.FAVORITES,
            PAGE.CALL_HISTORY,
            PAGE.CONTACTS,
            PAGE.DIAL_PAD
    })
    public @interface PAGE {
        int FAVORITES = 0;
        int CALL_HISTORY = 1;
        int CONTACTS = 2;
        int DIAL_PAD = 3;
    }

    private static final int DEFAULT_PAGE_COUNT = 4;
    private static final int[] TAB_LABELS = new int[]{
            R.string.favorites_title,
            R.string.call_history_title,
            R.string.contacts_title,
            R.string.dialpad_title};
    private static final int[] TAB_ICONS = new int[]{
            R.drawable.ic_favorite,
            R.drawable.ic_history,
            R.drawable.ic_contact,
            R.drawable.ic_dialpad};

    private Context mContext;
    private int mPageCount;

    public TelecomPagerAdapter(Context context, FragmentManager fragmentManager) {
        super(fragmentManager);
        mContext = context;
        mPageCount = DEFAULT_PAGE_COUNT;
    }

    @Override
    public Fragment getItem(int i) {
        switch (i) {
            case PAGE.FAVORITES:
                return FavoriteFragment.newInstance();
            case PAGE.CALL_HISTORY:
                return CallHistoryFragment.newInstance();
            case PAGE.CONTACTS:
                return ContactListFragment.newInstance();
            case PAGE.DIAL_PAD:
                return DialpadFragment.newPlaceCallDialpad();
        }
        return null;
    }

    @Override
    public int getCount() {
        return mPageCount;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return mContext.getString(TAB_LABELS[position]);
    }

    /**
     * Returns resource id of the icon to use for the tab at position
     */
    public Drawable getPageIcon(int position) {
        return mContext.getDrawable(TAB_ICONS[position]);
    }
}
