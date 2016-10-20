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

import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.car.Car;
import android.support.car.app.menu.CarDrawerActivity;
import android.support.car.app.menu.CarMenu;
import android.support.car.app.menu.CarMenuCallbacks;
import android.support.car.app.menu.RootMenu;
import android.support.car.app.menu.compat.CarMenuConstantsComapt;
import android.support.car.input.CarInputManager;
import android.support.v4.app.Fragment;
import android.telecom.Call;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import com.android.car.dialer.bluetooth.UiBluetoothMonitor;
import com.android.car.dialer.telecom.PhoneLoader;
import com.android.car.dialer.telecom.UiCall;
import com.android.car.dialer.telecom.UiCallManager;
import com.android.car.dialer.telecom.UiCallManager.CallListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Main activity for the Dialer app. Displays different fragments depending on call and
 * connectivity status:
 * <ul>
 * <li>OngoingCallFragment
 * <li>NoHfpFragment
 * <li>DialerFragment
 * <li>StrequentFragment
 * </ul>
 */
public class TelecomActivity extends CarDrawerActivity implements
        DialerFragment.DialerBackButtonListener {
    private static final String TAG = "Em.TelecomActivity";

    public static final String ACTION_ANSWER_CALL =
            "com.android.car.dialer.ANSWER_CALL";
    public static final String ACTION_END_CALL =
            "com.android.car.dialer.END_CALL";

    public static final String CALL_LOG_EMPTY_PLACEHOLDER = "call_log_empty_placeholder";

    private static final String DIALER_BACKSTACK = "DialerBackstack";

    // Delay after the last call disconnects to go back to the speed dial fragment
    private static final int POST_DISCONNECT_DELAY_MS = 3000;
    private static final String FRAGMENT_CLASS_KEY = "FRAGMENT_CLASS_KEY";

    private final UiBluetoothMonitor.Listener mBluetoothListener =
            new UiBluetoothMonitor.Listener() {
        @Override
        public void onStateChanged() {
            updateCurrentFragment();
        }
    };

    private final Runnable mSpeedDialRunnable = new Runnable() {
        @Override
        public void run() {
            showSpeedDialFragment();
        }
    };

    public TelecomActivity(Proxy proxy, Context context, Car car) {
        super(proxy, context, car);
        if (vdebug()) {
            Log.d(TAG, "ctor: proxy: " + proxy + ", context: " + context);
        }
    }

    private UiCallManager mUiCallManager;
    private UiBluetoothMonitor mUiBluetoothMonitor;

    private Fragment mCurrentFragment;
    private String mCurrentFragmentName;

    private int mLastNoHfpMessageId;
    private StrequentsFragment mSpeedDialFragment;
    private Fragment mOngoingCallFragment;

    private DialerFragment mDialerFragment;
    private boolean mDialerFragmentOpened;

    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (vdebug()) {
            Log.d(TAG, "onCreate");
        }
        Context context = getContext();
        setLightMode();
        getWindow().getDecorView().setBackgroundColor(context.getColor(R.color.phone_theme));
        setTitle(context.getString(R.string.phone_app_name));

        mHandler = new Handler();
        setCarMenuCallbacks(new TelecomMenuCallbacks());

        mUiCallManager = UiCallManager.getInstance(context);
        mUiBluetoothMonitor = UiBluetoothMonitor.getInstance();

        if (savedInstanceState != null) {
            mCurrentFragmentName = savedInstanceState.getString(FRAGMENT_CLASS_KEY);
        }

        if (vdebug()) {
            Log.d(TAG, "onCreate done, mCurrentFragmentName:  " + mCurrentFragmentName);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (vdebug()) {
            Log.d(TAG, "onDestroy");
        }
        mUiCallManager = null;
    }

    @Override
    protected void onPause() {
        super.onPause();
        mHandler.removeCallbacks(mSpeedDialRunnable);
        mUiCallManager.removeListener(mCarCallListener);
        mUiBluetoothMonitor.removeListener(mBluetoothListener);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (mCurrentFragment != null) {
            outState.putString(FRAGMENT_CLASS_KEY, mCurrentFragmentName);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onNewIntent(Intent i) {
        super.onNewIntent(i);
        setIntent(i);
    }

    @Override
    protected void onResume() {
        if (vdebug()) {
            Log.d(TAG, "onResume");
        }
        super.onResume();
        handleIntent();
        mUiCallManager.addListener(mCarCallListener);
        mUiBluetoothMonitor.addListener(mBluetoothListener);
        updateCurrentFragment();
    }

    // TODO: move to base class.
    private void setContentFragmentWithAnimations(Fragment fragment, int enter, int exit) {
        if (vdebug()) {
            Log.d(TAG, "setContentFragmentWithAnimations: " + fragment);
        }

        maybeHideDialer();

        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(enter, exit)
                .replace(getFragmentContainerId(), fragment)
                .commitAllowingStateLoss();

        mCurrentFragmentName = fragment.getClass().getSimpleName();
        mCurrentFragment = fragment;

        if (vdebug()) {
            Log.d(TAG, "setContentFragmentWithAnimations, fragmentName:" + mCurrentFragmentName);
        }
    }

    private void handleIntent() {
        Intent intent = getIntent();
        String action = intent != null ? intent.getAction() : null;

        if (vdebug()) {
            Log.d(TAG, "handleIntent, intent: " + intent + ", action: " + action);
        }

        if (action == null || action.length() == 0) {
            return;
        }

        UiCall ringingCall;
        switch (action) {
            case ACTION_ANSWER_CALL:
                ringingCall = mUiCallManager.getCallWithState(Call.STATE_RINGING);
                if (ringingCall == null) {
                    Log.e(TAG, "Unable to answer ringing call. There is none.");
                } else {
                    mUiCallManager.answerCall(ringingCall);
                }
                break;

            case ACTION_END_CALL:
                ringingCall = mUiCallManager.getCallWithState(Call.STATE_RINGING);
                if (ringingCall == null) {
                    Log.e(TAG, "Unable to end ringing call. There is none.");
                } else {
                    mUiCallManager.disconnectCall(ringingCall);
                }
                break;

            case Intent.ACTION_DIAL:
                String number = PhoneNumberUtils.getNumberFromIntent(intent, getContext());
                if (!(mCurrentFragment instanceof NoHfpFragment)) {
                    showDialerWithNumber(number);
                }
                break;

            default:
                // Do nothing.
        }

        setIntent(null);
    }

    /**
     * Will switch to the drawer or no-hfp fragment as necessary.
     */
    private void updateCurrentFragment() {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "updateCurrentFragment");
        }

        // TODO: do nothing when activity isFinishing() == true.

        boolean callEmpty = mUiCallManager.getCalls().isEmpty();
        if (!mUiBluetoothMonitor.isBluetoothEnabled() && callEmpty) {
            showNoHfpFragment(R.string.bluetooth_disabled);
        } else if (!mUiBluetoothMonitor.isBluetoothPaired() && callEmpty) {
            showNoHfpFragment(R.string.bluetooth_unpaired);
        } else if (!mUiBluetoothMonitor.isHfpConnected() && callEmpty) {
            showNoHfpFragment(R.string.no_hfp);
        } else {
            UiCall ongoingCall = mUiCallManager.getPrimaryCall();

            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "ongoingCall: " + ongoingCall + ", mCurrentFragment: " + mCurrentFragment);
            }

            if (ongoingCall == null && mCurrentFragment instanceof OngoingCallFragment) {
                mHandler.postDelayed(mSpeedDialRunnable, POST_DISCONNECT_DELAY_MS);
            } else if (ongoingCall != null) {
                mHandler.removeCallbacks(mSpeedDialRunnable);
                showOngoingCallFragment();
            } else if (DialerFragment.class.getSimpleName().equals(mCurrentFragmentName)) {
                showDialer();
            } else {
                mHandler.removeCallbacks(mSpeedDialRunnable);
                showSpeedDialFragment();
            }
        }

        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "updateCurrentFragment: done");
        }
    }

    public void showSpeedDialFragment() {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "showSpeedDialFragment");
        }

        if (mCurrentFragment instanceof StrequentsFragment) {
            return;
        }

        if (mSpeedDialFragment == null) {
            mSpeedDialFragment = new StrequentsFragment();
            Bundle args = new Bundle();
            mSpeedDialFragment.setArguments(args);
        }

        if (mCurrentFragment instanceof DialerFragment) {
            setContentFragmentWithSlideAndDelayAnimation(mSpeedDialFragment);
        } else {
            setContentFragmentWithFadeAnimation(mSpeedDialFragment);
        }
    }

    private void showOngoingCallFragment() {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "showOngoingCallFragment");
        }
        if (mCurrentFragment instanceof OngoingCallFragment) {
            closeDrawer();
            return;
        }

        if (mOngoingCallFragment == null) {
            mOngoingCallFragment = new OngoingCallFragment();
        }

        setContentFragmentWithFadeAnimation(mOngoingCallFragment);
        closeDrawer();
    }

    /**
     * Displays the {@link DialerFragment} on top of the contents of the TelecomActivity.
     */
    private void showDialer() {
        if (vdebug()) {
            Log.d(TAG, "showDialer");
        }

        if (mDialerFragmentOpened) {
            return;
        }

        if (mDialerFragment == null) {
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "showDialer: creating dialer");
            }

            mDialerFragment = new DialerFragment();
            mDialerFragment.setDialerBackButtonListener(this);
        }

        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "adding dialer to fragment backstack");
        }

        // Add the dialer fragment to the backstack so that it can be popped off to dismiss it.
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.telecom_slide_in, R.anim.telecom_slide_out,
                        R.anim.telecom_slide_in, R.anim.telecom_slide_out)
                .add(getFragmentContainerId(), mDialerFragment)
                .addToBackStack(DIALER_BACKSTACK)
                .commitAllowingStateLoss();

        mDialerFragmentOpened = true;

        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "done adding fragment to backstack");
        }
    }

    /**
     * Checks if the dialpad fragment is opened and hides it if it is.
     */
    private void maybeHideDialer() {
        if (mDialerFragmentOpened) {
            // Dismiss the dialer by removing it from the back stack.
            getSupportFragmentManager().popBackStack();
            mDialerFragmentOpened = false;
        }
    }

    @Override
    public void onDialerBackClick() {
        maybeHideDialer();
    }

    private void showDialerWithNumber(String number) {
        showDialer();
        mDialerFragment.setDialNumber(number);
    }

    private void showNoHfpFragment(int stringResId) {
        if (getInputManager().isInputActive()) {
            getInputManager().stopInput();
        }

        if (mCurrentFragment instanceof NoHfpFragment && stringResId == mLastNoHfpMessageId) {
            return;
        }

        mLastNoHfpMessageId = stringResId;
        String errorMessage = getContext().getString(stringResId);
        NoHfpFragment frag = new NoHfpFragment();
        frag.setErrorMessage(errorMessage);
        setContentFragment(frag);
        mCurrentFragment = frag;
    }

    private void setContentFragmentWithSlideAndDelayAnimation(Fragment fragment) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "setContentFragmentWithSlideAndDelayAnimation, fragment: " + fragment);
        }
        setContentFragmentWithAnimations(fragment,
                R.anim.telecom_slide_in_with_delay, R.anim.telecom_slide_out);
    }

    private void setContentFragmentWithFadeAnimation(Fragment fragment) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "setContentFragmentWithFadeAnimation, fragment: " + fragment);
        }
        setContentFragmentWithAnimations(fragment,
                R.anim.telecom_fade_in, R.anim.telecom_fade_out);
    }

    private final CallListener mCarCallListener = new UiCallManager.CallListener() {
        @Override
        public void onCallAdded(UiCall call) {
            if (vdebug()) {
                Log.d(TAG, "onCallAdded");
            }
            updateCurrentFragment();
        }

        @Override
        public void onCallRemoved(UiCall call) {
            if (vdebug()) {
                Log.d(TAG, "onCallRemoved");
            }
            updateCurrentFragment();
        }

        @Override
        public void onStateChanged(UiCall call, int state) {
            if (vdebug()) {
                Log.d(TAG, "onStateChanged");
            }
            updateCurrentFragment();
        }

        @Override
        public void onCallUpdated(UiCall call) {
            if (vdebug()) {
                Log.d(TAG, "onCallUpdated");
            }
            updateCurrentFragment();
        }
    };

    private static boolean vdebug() {
        return Log.isLoggable(TAG, Log.DEBUG);
    }

    private final class TelecomMenuCallbacks extends CarMenuCallbacks {
        /** Id for the telecom root menu */
        private static final String ROOT = "TELECOM_MENU_ROOT";
        private static final String VOICE_MAIL = "VOICE_MAIL";
        private static final String DIAL_NUMBER = "DIAL_NUMBER";
        private static final String CALL_HISTORY = "CALL_HISTORY";
        private static final String MISSED_CALLS = "MISSED_CALLS";

        @Override
        public RootMenu onGetRoot(Bundle hints) {
            return new RootMenu(ROOT);
        }

        @Override
        public void onLoadChildren(String parentId, CarMenu result) {
            if (vdebug()) {
                Log.d(TAG, "onLoadChildren, parentId: " + parentId + ", result: " + result);
            }

            switch (parentId) {
                case ROOT:
                    result.sendResult(generateTelecomRootMenu());
                    break;

                case CALL_HISTORY:
                    loadCallHistoryAsync(PhoneLoader.CALL_TYPE_ALL, result, parentId);
                    break;

                case MISSED_CALLS:
                    loadCallHistoryAsync(PhoneLoader.CALL_TYPE_MISSED, result, parentId);
                    break;

                default:
                    throw new IllegalStateException("Shouldn't query on parentId: " + parentId);
            }
        }

        @Override
        public void onItemClicked(String id) {
            if (vdebug()) {
                Log.d(TAG, "onItemClicked: " + id);
            }

            switch (id) {
                case VOICE_MAIL:
                    mUiCallManager.callVoicemail();
                    break;

                case DIAL_NUMBER:
                    // The dialpad should not be shown if there is an on-going call.
                    if (!(mCurrentFragment instanceof OngoingCallFragment)) {
                        showDialer();
                    }

                    break;

                case CALL_HISTORY:
                case MISSED_CALLS:
                case CALL_LOG_EMPTY_PLACEHOLDER:
                    // No-op
                    break;

                default:
                    mUiCallManager.safePlaceCall(
                            CallLogListingTask.getNumberFromCarMenuId(id), false);
            }
        }

        @Override
        public void onCarMenuOpened() {
            super.onCarMenuOpened();
            CarInputManager inputManager = getInputManager();
            if (inputManager.isInputActive()) {
                inputManager.stopInput();
            }
        }

        private List<CarMenu.Item> generateTelecomRootMenu() {
            List<CarMenu.Item> items = new ArrayList<>();
            Context context = getContext();
            final int iconColor = getResources().getColor(R.color.car_tint);

            Drawable drawable = context.getDrawable(R.drawable.ic_drawer_dialpad);
            drawable.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN);
            items.add(new CarMenu.Builder(DIAL_NUMBER)
                    .setTitle(getResources().getString(R.string.calllog_dial_number))
                    .setIconFromSnapshot(drawable)
                    .build());

            drawable = context.getDrawable(R.drawable.ic_drawer_history);
            drawable.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN);
            items.add(new CarMenu.Builder(CALL_HISTORY)
                    .setTitle(getResources().getString(R.string.calllog_all))
                    .setIconFromSnapshot(drawable)
                    .setFlags(CarMenuConstantsComapt.MenuItemConstants.FLAG_BROWSABLE)
                    .build());

            drawable = context.getDrawable(R.drawable.ic_drawer_call_missed);
            drawable.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN);
            items.add(new CarMenu.Builder(MISSED_CALLS)
                    .setTitle(getResources().getString(R.string.calllog_missed))
                    .setIconFromSnapshot(drawable)
                    .setFlags(CarMenuConstantsComapt.MenuItemConstants.FLAG_BROWSABLE)
                    .build());

            return items;
        }

        private void loadCallHistoryAsync(int callType, CarMenu result, String parentId) {
            result.detach();

            // Warning: much callbackiness!
            // First load up the call log cursor using the PhoneLoader so that happens in a
            // background thread. TODO: Why isn't PhoneLoader using a LoaderManager?
            PhoneLoader.registerCallObserver(callType, getContext(),
                    new Loader.OnLoadCompleteListener<Cursor>() {
                        @Override
                        public void onLoadComplete(Loader<Cursor> cursorLoader, Cursor cursor) {
                            // This callback runs on the thread that created the loader which is
                            // the ui thread so spin off another async task because we still need
                            // to pull together all the data along with the contact photo.
                            CallLogListingTask query = new CallLogListingTask.Builder()
                                    .setContext(getContext())
                                    .setCursor(cursor)
                                    .setResult(result)
                                    .build();
                            query.execute();
                        }
                    });
        }
    }
}
