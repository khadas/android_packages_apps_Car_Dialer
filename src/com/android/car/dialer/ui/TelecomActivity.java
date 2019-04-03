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

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.telephony.PhoneNumberUtils;
import android.view.View;
import android.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProviders;
import androidx.viewpager.widget.ViewPager;

import com.android.car.apps.common.widget.CarTabLayout;
import com.android.car.dialer.R;
import com.android.car.dialer.log.L;
import com.android.car.dialer.telecom.UiCallManager;
import com.android.car.dialer.ui.activecall.InCallActivity;
import com.android.car.dialer.ui.calllog.CallHistoryFragment;
import com.android.car.dialer.ui.common.DialerBaseFragment;
import com.android.car.dialer.ui.contact.ContactListFragment;
import com.android.car.dialer.ui.dialpad.DialpadFragment;
import com.android.car.dialer.ui.favorite.FavoriteFragment;
import com.android.car.dialer.ui.warning.NoHfpFragment;

/**
 * Main activity for the Dialer app. It contains two layers:
 * <ul>
 * <li>Overlay layer for {@link NoHfpFragment}
 * <li>Content layer for {@link FavoriteFragment} {@link CallHistoryFragment} {@link
 * ContactListFragment} and {@link DialpadFragment}
 *
 * <p>Start {@link InCallActivity} if there are ongoing calls
 *
 * <p>Based on call and connectivity status, it will choose the right page to display.
 */
public class TelecomActivity extends FragmentActivity implements
        DialerBaseFragment.DialerFragmentParent, FragmentManager.OnBackStackChangedListener {
    private static final String TAG = "CD.TelecomActivity";
    private static final String CONTENT_FRAGMENT_TAG = "CONTENT_FRAGMENT_TAG";

    private LiveData<String> mBluetoothErrorMsgLiveData;
    private LiveData<Integer> mDialerAppStateLiveData;

    // View objects for this activity.
    private CarTabLayout mTabLayout;
    private ViewPager mViewPager;
    private Toolbar mToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        L.d(TAG, "onCreate");
        setContentView(R.layout.telecom_activity);

        mToolbar = findViewById(R.id.car_toolbar);
        setActionBar(mToolbar);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        mTabLayout = findViewById(R.id.tab_layout);
        TelecomPagerAdapter telecomPagerAdapter = new TelecomPagerAdapter(this,
                getSupportFragmentManager());
        mViewPager = findViewById(R.id.view_pager);
        mViewPager.setAdapter(telecomPagerAdapter);
        for (int i = 0; i < telecomPagerAdapter.getCount(); i++) {
            mTabLayout.addCarTab(new CarTabLayout.CarTab(telecomPagerAdapter.getPageIcon(i),
                    telecomPagerAdapter.getPageTitle(i)));
        }
        mTabLayout.addOnCarTabSelectedListener(
                new CarTabLayout.SimpleOnCarTabSelectedListener() {
                    @Override
                    public void onCarTabSelected(CarTabLayout.CarTab carTab) {
                        mViewPager.setCurrentItem(mTabLayout.getCarTabPosition(carTab));
                    }
                });
        mViewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                mTabLayout.selectCarTab(position);
            }
        });

        TelecomActivityViewModel viewModel = ViewModelProviders.of(this).get(
                TelecomActivityViewModel.class);
        mBluetoothErrorMsgLiveData = viewModel.getErrorMessage();
        mDialerAppStateLiveData = viewModel.getDialerAppState();
        mDialerAppStateLiveData.observe(this,
                dialerAppState -> updateCurrentFragment(dialerAppState));

        handleIntent();
    }

    @Override
    public void onStart() {
        getSupportFragmentManager().addOnBackStackChangedListener(this);
        onBackStackChanged();
        super.onStart();
        L.d(TAG, "onStart");

        maybeStartInCallActivity();
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
                    showDialPadFragment(number);
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
        findViewById(R.id.content_container)
                .setVisibility(isOverlayFragmentVisible ? View.GONE : View.VISIBLE);
        findViewById(R.id.overlay_container)
                .setVisibility(isOverlayFragmentVisible ? View.VISIBLE : View.GONE);

        switch (dialerAppState) {
            case TelecomActivityViewModel.DialerAppState.BLUETOOTH_ERROR:
                showNoHfpOverlay(mBluetoothErrorMsgLiveData.getValue());
                break;

            case TelecomActivityViewModel.DialerAppState.EMERGENCY_DIALPAD:
                setOverlayFragment(DialpadFragment.newEmergencyDialpad());
                break;

            case TelecomActivityViewModel.DialerAppState.DEFAULT:
            default:
                clearOverlayFragment();
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

    private void setOverlayFragment(@NonNull Fragment overlayFragment) {
        L.d(TAG, "setOverlayFragment: %s", overlayFragment);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.overlay_container, overlayFragment)
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
        return getSupportFragmentManager().findFragmentById(R.id.overlay_container);
    }

    /** Switch to {@link DialpadFragment} and set the given number as dialed number. */
    private void showDialPadFragment(String number) {
        getSupportFragmentManager().popBackStackImmediate(
                CONTENT_FRAGMENT_TAG,
                FragmentManager.POP_BACK_STACK_INCLUSIVE);
        mTabLayout.selectCarTab(TelecomPagerAdapter.PAGE.DIAL_PAD);
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(
                "android:switcher:" + R.id.view_pager + ":" + mViewPager.getCurrentItem());
        if (fragment instanceof DialpadFragment) {
            ((DialpadFragment) fragment).setDialedNumber(number);
        } else {
            L.w(TAG, "Current tab is not dial pad fragment!");
        }
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
        int backStackCount = getSupportFragmentManager().getBackStackEntryCount();
        mTabLayout.setVisibility(backStackCount > 0 ? View.GONE : View.VISIBLE);
        mViewPager.setVisibility(backStackCount > 0 ? View.GONE : View.VISIBLE);
        mToolbar.getNavigationView().setEnabled(backStackCount > 0);

        if (backStackCount == 0) {
            getActionBar().setTitle(R.string.default_toolbar_title);
        }
    }

    @Override
    public boolean onNavigateUp() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            onBackPressed();
            return true;
        }
        return super.onNavigateUp();
    }

    private void maybeStartInCallActivity() {
        if (UiCallManager.get().getCallList().isEmpty()) {
            return;
        }

        L.d(TAG, "Start InCallActivity");
        Intent launchIntent = new Intent(getApplicationContext(), InCallActivity.class);
        startActivity(launchIntent);
    }
}
