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

package com.android.car.dialer.ui.common;

import android.app.Activity;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;

import androidx.fragment.app.Fragment;

import com.android.car.dialer.R;

/**
 * The base class for top level Dialer Fragments.
 */
public class DialerBaseFragment extends Fragment {

    /**
     * Interface for Dialer top level fragment's parent to implement.
     */
    public interface DialerFragmentParent {

        /** Sets the background drawable*/
        void setBackground(Drawable background);

        /**
         * Sets the visibility of action bar.
         */
        void setActionBarVisibility(boolean isVisible);
    }

    @Override
    public void onResume() {
        setFullScreenBackground();
        super.onResume();
    }

    /**
     * Sets a fullscreen background to its parent Activity.
     */
    protected void setFullScreenBackground() {
        Activity parentActivity = getActivity();
        if (parentActivity instanceof DialerFragmentParent) {
            ((DialerFragmentParent) parentActivity).setBackground(getFullScreenBackgroundColor());
        }
    }

    /**
     * Hides the action bar of its parent Activity.
     */
    protected void hideActionBar() {
        Activity parentActivity = getActivity();
        if (parentActivity instanceof DialerFragmentParent) {
            ((DialerFragmentParent) parentActivity).setActionBarVisibility(false);
        }
    }

    /**
     * Shows the action bar of its parent Activity.
     */
    protected void showActionBar() {
        Activity parentActivity = getActivity();
        if (parentActivity instanceof DialerFragmentParent) {
            ((DialerFragmentParent) parentActivity).setActionBarVisibility(true);
        }
    }

    /**
     * Returns the full screen background for its parent Activity. Override this function to
     * change the background.
     */
    protected Drawable getFullScreenBackgroundColor() {
        return new ColorDrawable(getContext().getColor(R.color.phone_theme));
    }
}
