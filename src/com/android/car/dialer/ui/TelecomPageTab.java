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
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.android.car.apps.common.widget.CarTabLayout;
import com.android.car.dialer.R;
import com.android.car.dialer.ui.calllog.CallHistoryFragment;
import com.android.car.dialer.ui.contact.ContactListFragment;
import com.android.car.dialer.ui.dialpad.DialpadFragment;
import com.android.car.dialer.ui.favorite.FavoriteFragment;

/** Tab presenting fragments. */
public class TelecomPageTab extends CarTabLayout.CarTab {
    @IntDef({
            TelecomPageTab.PAGE.FAVORITES,
            TelecomPageTab.PAGE.CALL_HISTORY,
            TelecomPageTab.PAGE.CONTACTS,
            TelecomPageTab.PAGE.DIAL_PAD
    })
    public @interface PAGE {
        int FAVORITES = 0;
        int CALL_HISTORY = 1;
        int CONTACTS = 2;
        int DIAL_PAD = 3;
    }

    private final Factory mFactory;
    private Fragment mFragment;
    private String mFragmentTag;
    private boolean mWasFragmentRestored;

    private TelecomPageTab(@Nullable Drawable icon, @Nullable CharSequence text, Factory factory) {
        super(icon, text);
        mFactory = factory;
    }

    /**
     * Either restore fragment from saved state or create new instance.
     */
    private void initFragment(FragmentManager fragmentManager, @PAGE int page) {
        mFragmentTag = makeFragmentTag(page);
        mFragment = fragmentManager.findFragmentByTag(mFragmentTag);
        if (mFragment == null) {
            mFragment = mFactory.createFragment(page);
            mWasFragmentRestored = false;
            return;
        }
        mWasFragmentRestored = true;
    }

    /** Returns true if the fragment for this tab is restored from a saved state. */
    public boolean wasFragmentRestored() {
        return mWasFragmentRestored;
    }

    /** Returns the fragment for this tab. */
    public Fragment getFragment() {
        return mFragment;
    }

    /** Returns the fragment tag for this tab. */
    public String getFragmentTag() {
        return mFragmentTag;
    }

    private String makeFragmentTag(@PAGE int page) {
        return String.format("%s:%d", getClass().getSimpleName(), page);
    }

    /** Responsible for creating the top tab items and their fragments. */
    public static class Factory {
        private static final int TAB_COUNT = 4;

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


        private final FragmentManager mFragmentManager;

        public Factory(FragmentManager fragmentManager) {
            mFragmentManager = fragmentManager;
        }

        private Fragment createFragment(@PAGE int page) {
            switch (page) {
                case PAGE.FAVORITES:
                    return FavoriteFragment.newInstance();
                case PAGE.CALL_HISTORY:
                    return CallHistoryFragment.newInstance();
                case PAGE.CONTACTS:
                    return ContactListFragment.newInstance();
                case PAGE.DIAL_PAD:
                    return DialpadFragment.newPlaceCallDialpad();
                default:
                    return null;
            }
        }

        public TelecomPageTab createTab(Context context, @PAGE int page) {
            TelecomPageTab telecomPageTab = new TelecomPageTab(context.getDrawable(TAB_ICONS[page]),
                    context.getString(TAB_LABELS[page]), this);
            telecomPageTab.initFragment(mFragmentManager, page);
            return telecomPageTab;
        }

        public int getTabCount() {
            return TAB_COUNT;
        }
    }
}
