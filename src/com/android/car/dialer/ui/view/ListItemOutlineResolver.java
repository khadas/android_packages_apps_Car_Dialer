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

package com.android.car.dialer.ui.view;

import android.view.View;

import com.android.car.dialer.ui.view.ListItemOutlineProvider;

/**
 * A utility class that will set the outline for a View that represents an card entry in a list. The
 * class will set the outline depending on the position of the card within the list.
 */
public class ListItemOutlineResolver {

    /**
     * Sets the outline on the given view so that the combination of all items looks like a
     * rectangle with rounded corner.
     *
     * <p>The view will be set with rounded corners if it is the only card within the list.
     * Or if it is the first or last view, it will have the top or bottom corners rounded
     * respectively.
     *
     * @param view            The view whose outline to set.
     * @param currentPosition The current position of the View within the list. This value should
     *                        be 0-based.
     * @param totalItems      The total items within the list.
     */
    public static void setOutline(View view, float radius, int currentPosition, int totalItems) {
        if (currentPosition < 0) {
            throw new IllegalArgumentException("currentPosition cannot be less than zero.");
        }

        if (currentPosition >= totalItems) {
            throw new IndexOutOfBoundsException("currentPosition: " + currentPosition + "; "
                    + "totalItems: " + totalItems);
        }

        ListItemOutlineProvider listItemOutlineProvider = new ListItemOutlineProvider(radius);
        if (totalItems == 1) {
            // One card - all corners are rounded.
            listItemOutlineProvider.setCorners(
                    /* hasRoundedTop= */true, /* hasRoundedBottom= */true);
        } else if (currentPosition == 0) {
            // First card gets rounded top.
            listItemOutlineProvider.setCorners(
                    /* hasRoundedTop= */true, /* hasRoundedBottom= */false);
        } else if (currentPosition == totalItems - 1) {
            // Last one has a rounded bottom.
            listItemOutlineProvider.setCorners(
                    /* hasRoundedTop= */false, /* hasRoundedBottom= */true);
        } else {
            // Middle has no rounded corners.
            listItemOutlineProvider.setCorners(
                    /* hasRoundedTop= */false, /* hasRoundedBottom= */false);
        }
        view.setOutlineProvider(listItemOutlineProvider);
        view.setClipToOutline(true);
    }
}
