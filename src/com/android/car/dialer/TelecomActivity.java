/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.android.car.dialer;

import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.telephony.PhoneNumberUtils;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.car.drawer.CarDrawerActivity;
import androidx.car.drawer.CarDrawerAdapter;
import androidx.car.drawer.DrawerItemViewHolder;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProviders;

import com.android.car.dialer.log.L;
import com.android.car.dialer.telecom.InMemoryPhoneBook;
import com.android.car.dialer.telecom.UiBluetoothMonitor;
import com.android.car.dialer.telecom.UiCallManager;
import com.android.car.dialer.ui.TelecomActivityViewModel;
import com.android.car.dialer.ui.activecall.InCallFragment;
import com.android.car.dialer.ui.calllog.CallHistoryFragment;
import com.android.car.dialer.ui.common.DialerBaseFragment;
import com.android.car.dialer.ui.contact.ContactListFragment;
import com.android.car.dialer.ui.dialpad.DialpadFragment;
import com.android.car.dialer.ui.favorite.FavoriteFragment;
import com.android.car.dialer.ui.warning.NoHfpFragment;

/**
 * Main activity for the Dialer app. Displays different fragments depending on call and
 * connectivity status:
 * <ul>
 * <li>OngoingCallFragment
 * <li>NoHfpFragment
 * <li>DialpadFragment
 * <li>FavoriteFragment
 * </ul>
 */
public class TelecomActivity extends CarDrawerActivity implements
        DialerBaseFragment.DialerFragmentParent {
    private static final String TAG = "CD.TelecomActivity";

    private static final String CONTENT_FRAGMENT_TAG = "CONTENT_FRAGMENT_TAG";

    private UiCallManager mUiCallManager;
    private UiBluetoothMonitor mUiBluetoothMonitor;

    private LiveData<String> mBluetoothErrorMsgLiveData;
    private LiveData<Boolean> mHasOngoingCallLiveData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        L.d(TAG, "onCreate");

        setToolbarElevation(0f);
        setMainContent(R.layout.telecom_activity);
        getSupportActionBar().setBackgroundDrawable(
                new ColorDrawable(getColor(android.R.color.transparent)));
        mUiCallManager = UiCallManager.init(getApplicationContext());
        mUiBluetoothMonitor = UiBluetoothMonitor.init(getApplicationContext());

        InMemoryPhoneBook.init(getApplicationContext());

        TelecomActivityViewModel viewModel = ViewModelProviders.of(this).get(
                TelecomActivityViewModel.class);
        mBluetoothErrorMsgLiveData = viewModel.getErrorMessage();
        mBluetoothErrorMsgLiveData.observe(this, errorMsg -> updateCurrentFragment());

        mHasOngoingCallLiveData = viewModel.hasOngoingCall();
        mHasOngoingCallLiveData.observe(this, hasOngoingCall -> updateCurrentFragment());

        getDrawerController().setRootAdapter(new DialerRootAdapter(mBluetoothErrorMsgLiveData));

        updateCurrentFragment();
        handleIntent();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        L.d(TAG, "onDestroy");
        mUiBluetoothMonitor.tearDown();
        InMemoryPhoneBook.tearDown();
        mUiCallManager.tearDown();
        mUiCallManager = null;
    }

    @Override
    protected void onNewIntent(Intent i) {
        super.onNewIntent(i);
        setIntent(i);
        handleIntent();
    }

    @Override
    public void setBackground(Drawable background) {
        findViewById(android.R.id.content).setBackground(background);
    }

    @Override
    public void setActionBarVisibility(boolean isVisible) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            if (isVisible) {
                actionBar.show();
            } else {
                actionBar.hide();
            }
        }
    }

    @Override
    public void setActionBarTitle(@StringRes int titleRes) {
        setTitle(titleRes);
    }

    private void handleIntent() {
        Intent intent = getIntent();
        String action = intent != null ? intent.getAction() : null;
        L.d(TAG, "handleIntent, intent: %s, action: %s", intent, action);
        if (action == null || action.length() == 0) {
            return;
        }

        String number;
        switch (action) {
            case Intent.ACTION_DIAL:
                number = PhoneNumberUtils.getNumberFromIntent(intent, this);
                if (!(getCurrentFragment() instanceof NoHfpFragment)) {
                    getDrawerController().closeDrawer();
                    setContentFragment(DialpadFragment.newPlaceCallDialpad(number));
                }
                break;

            case Intent.ACTION_CALL:
                number = PhoneNumberUtils.getNumberFromIntent(intent, this);
                mUiCallManager.placeCall(number);
                break;

            default:
                // Do nothing.
        }

        setIntent(null);
    }

    /**
     * Updates the content fragment of this Activity based on the state of the application.
     */
    private void updateCurrentFragment() {
        L.d(TAG, "updateCurrentFragment()");

        boolean hasOngoingCall = mHasOngoingCallLiveData.getValue() != null
                ? mHasOngoingCallLiveData.getValue()
                : false;

        if (!TelecomActivityViewModel.NO_BT_ERROR.equals(mBluetoothErrorMsgLiveData.getValue())) {
            showNoHfpFragment(mBluetoothErrorMsgLiveData.getValue());
        } else if (hasOngoingCall) {
            getDrawerController().closeDrawer();
            setContentFragment(InCallFragment.newInstance());
        } else {
            Fragment currentFragment = getCurrentFragment();
            if (currentFragment == null
                    || currentFragment instanceof InCallFragment
                    || currentFragment instanceof NoHfpFragment) {
                setContentFragment(FavoriteFragment.newInstance());
            }
        }
    }

    private void showNoHfpFragment(String errorMsg) {
        if (getCurrentFragment() instanceof NoHfpFragment) {
            ((NoHfpFragment) getCurrentFragment()).setErrorMessage(errorMsg);
        } else {
            setContentFragment(NoHfpFragment.newInstance(errorMsg));
        }
    }

    /**
     * Sets the fragment that will be shown as the main content of this Activity.
     */
    private void setContentFragment(Fragment fragment) {
        L.d(TAG, "setContentFragment: %s", fragment);
        if (fragment == null) {
            return;
        }

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.content_fragment_container, fragment, CONTENT_FRAGMENT_TAG)
                .commitNow();
    }

    /**
     * Returns the fragment that is currently being displayed as the content view.
     */
    @Nullable
    private Fragment getCurrentFragment() {
        return getSupportFragmentManager().findFragmentByTag(CONTENT_FRAGMENT_TAG);
    }

    private class DialerRootAdapter extends CarDrawerAdapter {
        private static final int ITEM_FAVORITES = 0;
        private static final int ITEM_CALLLOG_ALL = 1;
        private static final int ITEM_CONTACT = 2;
        private static final int ITEM_DIAL = 3;

        private static final int ITEM_COUNT = 4;
        private LiveData<String> mBluetoothError;

        DialerRootAdapter(LiveData<String> bluetoothErrorMsg) {
            super(TelecomActivity.this, false /* showDisabledListOnEmpty */);
            mBluetoothError = bluetoothErrorMsg;
            bluetoothErrorMsg.observe(TelecomActivity.this, errorMsg -> notifyDataSetChanged());
        }

        @Override
        protected int getActualItemCount() {
            if (TelecomActivityViewModel.NO_BT_ERROR.equals(mBluetoothError.getValue())) {
                return ITEM_COUNT;
            } else {
                return 0;
            }
        }

        @Override
        public void populateViewHolder(DrawerItemViewHolder holder, int position) {
            final int iconColor = getResources().getColor(R.color.car_tint);
            int textResId, iconResId;
            switch (position) {
                case ITEM_DIAL:
                    textResId = R.string.calllog_dial_number;
                    iconResId = R.drawable.ic_dialpad;
                    break;
                case ITEM_CALLLOG_ALL:
                    textResId = R.string.calllog_all;
                    iconResId = R.drawable.ic_history;
                    break;
                case ITEM_FAVORITES:
                    textResId = R.string.calllog_favorites;
                    iconResId = R.drawable.ic_favorite;
                    break;
                case ITEM_CONTACT:
                    textResId = R.string.contact_menu_label;
                    iconResId = R.drawable.ic_contact;
                    break;
                default:
                    Log.wtf(TAG, "Unexpected position: " + position);
                    return;
            }
            holder.getTitleView().setText(textResId);
            Drawable drawable = getDrawable(iconResId);
            drawable.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN);
            holder.getIconView().setImageDrawable(drawable);
            holder.itemView.setOnClickListener(v -> onItemClick(holder.getAdapterPosition()));
        }

        private void onItemClick(int position) {
            getDrawerController().closeDrawer();
            switch (position) {
                case ITEM_DIAL:
                    setContentFragment(DialpadFragment.newPlaceCallDialpad(/* dialNumber= */ null));
                    break;
                case ITEM_CALLLOG_ALL:
                    setContentFragment(CallHistoryFragment.newInstance());
                    break;
                case ITEM_FAVORITES:
                    setContentFragment(FavoriteFragment.newInstance());
                    break;
                case ITEM_CONTACT:
                    setContentFragment(ContactListFragment.newInstance());
                    break;
                default:
                    Log.w(TAG, "Invalid position in ROOT menu! " + position);
            }
        }
    }
}
