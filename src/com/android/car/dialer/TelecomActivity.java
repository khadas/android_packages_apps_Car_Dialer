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

import android.app.ActionBar;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.car.drawer.CarDrawerAdapter;
import androidx.car.drawer.DrawerItemViewHolder;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProviders;
import com.android.car.apps.common.DrawerActivity;
import com.android.car.dialer.log.L;
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
 * Main activity for the Dialer app. It contains two layers:
 * <ul>
 * <li>Overlay layer for {@link NoHfpFragment} and {@link InCallFragment}
 * <li>Content layer for {@link FavoriteFragment} {@link CallHistoryFragment} {@link
 * ContactListFragment} and {@link DialpadFragment}
 *
 * <p>Based on call and connectivity status, it will choose the right page to display.
 */
public class TelecomActivity extends DrawerActivity implements
        DialerBaseFragment.DialerFragmentParent, FragmentManager.OnBackStackChangedListener {
    private static final String TAG = "CD.TelecomActivity";
    private static final String CONTENT_FRAGMENT_TAG = "CONTENT_FRAGMENT_TAG";

    private LiveData<String> mBluetoothErrorMsgLiveData;
    private LiveData<Integer> mDialerAppStateLiveData;

    private ActionBarDrawerToggle mActionBarDrawerToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        L.d(TAG, "onCreate");
        setContentView(R.layout.telecom_activity);
        getActionBar().setBackgroundDrawable(
                new ColorDrawable(getColor(android.R.color.transparent)));

        getDrawerController().setRootAdapter(new DialerRootAdapter());

        TelecomActivityViewModel viewModel = ViewModelProviders.of(this).get(
                TelecomActivityViewModel.class);
        mBluetoothErrorMsgLiveData = viewModel.getErrorMessage();
        mDialerAppStateLiveData = viewModel.getDialerAppState();
        mDialerAppStateLiveData.observe(this,
                dialerAppState -> updateCurrentFragment(dialerAppState));

        mActionBarDrawerToggle = getActionBarDrawerToggle();
        mActionBarDrawerToggle.setHomeAsUpIndicator(R.drawable.ic_arrow_back);

        handleIntent();
    }

    @Override
    public void onStart() {
        getSupportFragmentManager().addOnBackStackChangedListener(this);
        onBackStackChanged();
        super.onStart();
        L.d(TAG, "onStart");
    }

    @Override
    public void onStop() {
        super.onStop();
        L.d(TAG, "onStop");
        getSupportFragmentManager().removeOnBackStackChangedListener(this);
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
        ActionBar actionBar = getActionBar();
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
                if (TelecomActivityViewModel.DialerAppState.BLUETOOTH_ERROR
                        != mDialerAppStateLiveData.getValue()) {
                    setContentFragment(DialpadFragment.newPlaceCallDialpad(number));
                }
                break;

            case Intent.ACTION_CALL:
                number = PhoneNumberUtils.getNumberFromIntent(intent, this);
                UiCallManager.get().placeCall(number);
                break;

            default:
                // Do nothing.
        }

        setIntent(null);
    }

    /**
     * Update the current visible fragment of this Activity based on the state of the application.
     * <ul>
     * <li> If bluetooth is not connected or there is an active call, show overlay, lock drawer,
     * hide action bar and hide the content layer.
     * <li> Otherwise, show the content layer, show action bar, hide the overlay and reset drawer
     * lock mode.
     */
    private void updateCurrentFragment(
            @TelecomActivityViewModel.DialerAppState int dialerAppState) {
        L.d(TAG, "updateCurrentFragment, dialerAppState: %d", dialerAppState);

        boolean isOverlayFragmentVisible =
                TelecomActivityViewModel.DialerAppState.DEFAULT != dialerAppState;
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        drawerLayout.setDrawerLockMode(
                isOverlayFragmentVisible
                        ? DrawerLayout.LOCK_MODE_LOCKED_CLOSED
                        : DrawerLayout.LOCK_MODE_UNLOCKED);
        findViewById(R.id.content_fragment_container)
                .setVisibility(isOverlayFragmentVisible ? View.GONE : View.VISIBLE);
        findViewById(R.id.overlay_fragment_container)
                .setVisibility(isOverlayFragmentVisible ? View.VISIBLE : View.GONE);
        setActionBarVisibility(!isOverlayFragmentVisible);

        switch (dialerAppState) {
            case TelecomActivityViewModel.DialerAppState.BLUETOOTH_ERROR:
                showNoHfpOverlay(mBluetoothErrorMsgLiveData.getValue());
                break;

            case TelecomActivityViewModel.DialerAppState.HAS_ONGOING_CALL:
                showInCallOverlay();
                break;

            case TelecomActivityViewModel.DialerAppState.EMERGENCY_DAILPAD:
                setOverlayFragment(DialpadFragment.newEmergencyDialpad());
                break;

            case TelecomActivityViewModel.DialerAppState.DEFAULT:
            default:
                clearOverlayFragment();
                Fragment currentContentFragment = getCurrentContentFragment();
                if (currentContentFragment == null) {
                    setContentFragment(FavoriteFragment.newInstance());
                }
                break;
        }
    }

    private void showNoHfpOverlay(String errorMsg) {
        Fragment overlayFragment = getCurrentOverlayFragment();
        if (overlayFragment instanceof NoHfpFragment) {
            ((NoHfpFragment) overlayFragment).setErrorMessage(errorMsg);
        } else {
            setOverlayFragment(NoHfpFragment.newInstance(errorMsg));
        }
    }

    private void showInCallOverlay() {
        Fragment overlayFragment = getCurrentOverlayFragment();
        if (overlayFragment instanceof InCallFragment) {
            return;
        }

        setOverlayFragment(InCallFragment.newInstance());
    }

    private void setOverlayFragment(@NonNull Fragment overlayFragment) {
        L.d(TAG, "setOverlayFragment: %s", overlayFragment);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.overlay_fragment_container, overlayFragment)
                .commitNow();
    }

    private void clearOverlayFragment() {
        L.d(TAG, "clearOverlayFragment");

        Fragment overlayFragment = getCurrentOverlayFragment();
        if (overlayFragment == null) {
            return;
        }

        getSupportFragmentManager()
                .beginTransaction()
                .remove(overlayFragment)
                .commitNow();
    }

    /** Returns the fragment that is currently being displayed as the overlay view on top. */
    @Nullable
    private Fragment getCurrentOverlayFragment() {
        return getSupportFragmentManager().findFragmentById(R.id.overlay_fragment_container);
    }

    /**
     * Sets the fragment that will be shown as the main content of this Activity.
     */
    private void setContentFragment(@NonNull Fragment contentFragment) {
        L.d(TAG, "setContentFragment: %s", contentFragment);

        getDrawerController().closeDrawer();
        getSupportFragmentManager().popBackStackImmediate(
                CONTENT_FRAGMENT_TAG,
                FragmentManager.POP_BACK_STACK_INCLUSIVE);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content_fragment_container, contentFragment)
                .addToBackStack(CONTENT_FRAGMENT_TAG)
                .commit();
    }

    /** Returns the fragment that is currently being displayed as the content view. */
    @Nullable
    private Fragment getCurrentContentFragment() {
        return getSupportFragmentManager().findFragmentById(R.id.content_fragment_container);
    }

    @Override
    public void pushContentFragment(@NonNull Fragment topContentFragment, String fragmentTag) {
        L.d(TAG, "pushContentFragment: %s", topContentFragment);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content_fragment_container, topContentFragment)
                .addToBackStack(fragmentTag)
                .commit();
    }

    @Override
    public void onBackStackChanged() {
        mActionBarDrawerToggle.setDrawerIndicatorEnabled(
                getSupportFragmentManager().getBackStackEntryCount() <= 1);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        boolean hasHandled = super.onOptionsItemSelected(menuItem);
        if (hasHandled) {
            return true;
        }
        if (menuItem.getItemId() == android.R.id.home
                && getSupportFragmentManager().getBackStackEntryCount() > 1) {
            getSupportFragmentManager().popBackStackImmediate();
            return true;
        }
        return false;
    }

    private class DialerRootAdapter extends CarDrawerAdapter {
        private static final int ITEM_FAVORITES = 0;
        private static final int ITEM_CALLLOG_ALL = 1;
        private static final int ITEM_CONTACT = 2;
        private static final int ITEM_DIAL = 3;

        private static final int ITEM_COUNT = 4;

        DialerRootAdapter() {
            super(TelecomActivity.this, false /* showDisabledListOnEmpty */);
        }

        @Override
        protected int getActualItemCount() {
            return ITEM_COUNT;
        }

        @Override
        public void populateViewHolder(DrawerItemViewHolder holder, int position) {
            final int iconColor = getResources().getColor(R.color.dialer_tint);
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
