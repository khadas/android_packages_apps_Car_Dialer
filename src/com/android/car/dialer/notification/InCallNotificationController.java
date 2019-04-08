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

package com.android.car.dialer.notification;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.telecom.Call;
import android.util.Pair;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;

import com.android.car.apps.common.LetterTileDrawable;
import com.android.car.dialer.R;
import com.android.car.dialer.ui.activecall.InCallActivity;
import com.android.car.telephony.common.CallDetail;
import com.android.car.telephony.common.TelecomUtils;

import java.io.FileNotFoundException;
import java.io.InputStream;

/** Controller that manages the heads up notification for incoming calls. */
public class InCallNotificationController {
    // Channel id that uses the package name.
    private static final String CHANNEL_ID = "com.android.car.dialer";
    // A random number that is used for notification id.
    private static final int NOTIFICATION_ID = 20181105;

    private static InCallNotificationController sInCallNotificationController;
    private final Context mContext;
    private final NotificationManager mNotificationManager;

    /**
     * Initialized a globally accessible {@link InCallNotificationController} which can be retrieved
     * by {@link #get}. If this function is called a second time before calling {@link #tearDown()},
     * an {@link IllegalStateException} will be thrown.
     *
     * @param applicationContext Application context.
     */
    public static void init(Context applicationContext) {
        if (sInCallNotificationController == null) {
            sInCallNotificationController = new InCallNotificationController(applicationContext);
        } else {
            throw new IllegalStateException("InCallNotificationController has been initialized.");
        }
    }

    /**
     * Gets the global {@link InCallNotificationController} instance. Make sure
     * {@link #init(Context)} is called before calling this method.
     */
    public static InCallNotificationController get() {
        if (sInCallNotificationController == null) {
            throw new IllegalStateException(
                    "Call InCallNotificationController.init(Context) before calling this function");
        }
        return sInCallNotificationController;
    }

    public static void tearDown() {
        sInCallNotificationController = null;
    }

    @TargetApi(26)
    private InCallNotificationController(Context context) {
        mContext = context;

        CharSequence name = mContext.getString(R.string.in_call_notification_channel_name);
        String description = mContext.getString(R.string.in_call_notification_channel_description);
        NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID, name,
                NotificationManager.IMPORTANCE_HIGH);
        notificationChannel.setDescription(description);

        mNotificationManager = mContext.getSystemService(NotificationManager.class);
        mNotificationManager.createNotificationChannel(notificationChannel);
    }


    /** Show a new incoming call notification or update the existing incoming call notification. */
    @TargetApi(26)
    public void showInCallNotification(Call call) {
        CallDetail callDetail = CallDetail.fromTelecomCallDetail(call.getDetails());
        String number = callDetail.getNumber();
        Pair<String, Uri> displayNameAndAvatarUri = TelecomUtils.getDisplayNameAndAvatarUri(mContext,
                number);

        int avatarSize = mContext.getResources().getDimensionPixelSize(R.dimen.avatar_icon_size);
        Icon largeIcon = loadRoundedContactAvatar(displayNameAndAvatarUri.second, avatarSize);
        if (largeIcon == null) {
            largeIcon = createLetterTile(displayNameAndAvatarUri.first, avatarSize);
        }

        Intent intent = new Intent(mContext, InCallActivity.class);
        PendingIntent fullscreenIntent = PendingIntent.getActivity(mContext, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder builder = new Notification.Builder(mContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_phone)
                .setLargeIcon(largeIcon)
                .setContentTitle(displayNameAndAvatarUri.first)
                .setContentText(mContext.getString(R.string.notification_incoming_call))
                .setFullScreenIntent(fullscreenIntent, /* highPriority= */true)
                .setCategory(Notification.CATEGORY_CALL)
                .addAction(getAction(call, R.string.answer_call,
                        NotificationReceiver.ACTION_ANSWER_CALL))
                .addAction(getAction(call, R.string.decline_call,
                        NotificationReceiver.ACTION_DECLINE_CALL))
                .setOngoing(true)
                .setAutoCancel(false);

        mNotificationManager.notify(
                call.getDetails().getTelecomCallId(),
                NOTIFICATION_ID,
                builder.build());
    }

    /** Cancel the incoming call notification for the given call. */
    public void cancelInCallNotification(Call call) {
        mNotificationManager.cancel(call.getDetails().getTelecomCallId(), NOTIFICATION_ID);
    }

    private Notification.Action getAction(Call call, @StringRes int actionText,
            String intentAction) {
        CharSequence text = mContext.getString(actionText);
        PendingIntent intent = PendingIntent.getBroadcast(mContext, 0,
                getIntent(mContext, intentAction, call),
                PendingIntent.FLAG_UPDATE_CURRENT);
        return new Notification.Action.Builder(null, text, intent).build();
    }

    private Intent getIntent(Context context, String action, Call call) {
        Intent intent = new Intent(action, null, context, NotificationReceiver.class);
        intent.putExtra(NotificationReceiver.EXTRA_CALL_ID, call.getDetails().getTelecomCallId());
        return intent;
    }

    private Icon loadRoundedContactAvatar(@Nullable Uri avatarUri, int avatarSize) {
        if (avatarUri == null) {
            return null;
        }

        try {
            InputStream input = mContext.getContentResolver().openInputStream(avatarUri);
            if (input == null) {
                return null;
            }
            RoundedBitmapDrawable roundedBitmapDrawable = RoundedBitmapDrawableFactory.create(
                    mContext.getResources(), input);
            roundedBitmapDrawable.setCircular(true);

            final Bitmap result = Bitmap.createBitmap(avatarSize, avatarSize,
                    Bitmap.Config.ARGB_8888);
            final Canvas canvas = new Canvas(result);
            roundedBitmapDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            roundedBitmapDrawable.draw(canvas);
            roundedBitmapDrawable.getBitmap().recycle();
            return Icon.createWithBitmap(result);
        } catch (FileNotFoundException e) {
            // No-op
        }
        return null;
    }

    private Icon createLetterTile(String displayName, int avatarSize) {
        LetterTileDrawable letterTileDrawable = TelecomUtils.createLetterTile(mContext,
                displayName);
        return Icon.createWithBitmap(letterTileDrawable.toBitmap(avatarSize));
    }
}
