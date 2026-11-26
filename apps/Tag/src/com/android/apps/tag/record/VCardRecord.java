/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.apps.tag.record;

import com.android.apps.tag.R;
import com.android.vcard.VCardConfig;
import com.android.vcard.VCardEntry;
import com.android.vcard.VCardEntryConstructor;
import com.android.vcard.VCardEntryHandler;
import com.android.vcard.VCardParser;
import com.android.vcard.VCardParser_V21;
import com.android.vcard.VCardParser_V30;
import com.android.vcard.exception.VCardException;
import com.android.vcard.exception.VCardVersionException;
import com.google.common.base.Preconditions;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.nfc.NdefRecord;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

/**
 * VCard Ndef Record object
 */
public class VCardRecord extends ParsedNdefRecord implements OnClickListener {
    private static final String TAG = VCardRecord.class.getSimpleName();

    public static final String RECORD_TYPE = "vcard";

    private final byte[] mVCard;

    private VCardRecord(byte[] content) {
        mVCard = content;
    }

    @Override
    public View getView(Activity activity, LayoutInflater inflater, ViewGroup parent, int offset) {
        var uri = activity.getIntent().getData();
        uri = UriWithAppended(uri, offset);

        // TODO: parse content and display something nicer.
        var intent = new Intent(Intent.ACTION_VIEW, uri);

        var template = activity.getResources().getText(R.string.import_vcard);
        var description = TextUtils.expandTemplate(template, getDisplayName()).toString();

        return RecordUtils.getViewsForIntent(activity, inflater, parent, this, intent, description);
    }

    private static android.net.Uri UriWithAppended(android.net.Uri uri, int offset) {
        uri = android.net.Uri.withAppendedPath(uri, Integer.toString(offset));
        return android.net.Uri.withAppendedPath(uri, "mime");
    }

    @Override
    public String getSnippet(Context context, Locale locale) {
        var template = context.getResources().getText(R.string.vcard_title);
        return TextUtils.expandTemplate(template, getDisplayName()).toString();
    }

    public String getDisplayName() {
        try {
            var entries = getVCardEntries();
            if (!entries.isEmpty()) {
                return entries.get(0).getDisplayName();
            }
        } catch (Exception ignored) {
        }

        return "vCard";
    }

    private ArrayList<VCardEntry> getVCardEntries() throws IOException, VCardException {
        final var entries = new ArrayList<VCardEntry>();

        final var type = VCardConfig.VCARD_TYPE_UNKNOWN;
        final var constructor = new VCardEntryConstructor(type);
        constructor.addEntryHandler(new VCardEntryHandler() {
            @Override public void onStart() {}
            @Override public void onEnd() {}

            @Override
            public void onEntryCreated(VCardEntry entry) {
                entries.add(entry);
            }
        });

        VCardParser parser = new VCardParser_V21(type);
        try (var stream = new ByteArrayInputStream(mVCard)) {
            parser.parse(stream, constructor);
        } catch (VCardVersionException e) {
            parser = new VCardParser_V30(type);
            try (var stream = new ByteArrayInputStream(mVCard)) {
                parser.parse(stream, constructor);
            }
        }

        return entries;
    }

    private static Intent getPickContactIntent() {
        var intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType(ContactsContract.Contacts.CONTENT_ITEM_TYPE);
        return intent;
    }

    public static VCardRecord parse(NdefRecord record) {
        var underlyingRecord = MimeRecord.parse(record);

        // TODO: Add support for other vcard mime types.
        Preconditions.checkArgument("text/x-vcard".equals(underlyingRecord.getMimeType()));
        return new VCardRecord(underlyingRecord.getContent());
    }

    public static NdefRecord newVCardRecord(byte[] data) {
        return MimeRecord.newMimeRecord("text/x-vcard", data);
    }

    @Override
    public void onClick(View view) {
        var info = (RecordUtils.ClickInfo) view.getTag();
        try {
            info.activity.startActivity(info.intent);
            info.activity.finish();
        } catch (ActivityNotFoundException e) {
            // The activity wasn't found for some reason. Don't crash, but don't do anything.
            Log.e(TAG, "Failed to launch activity for intent " + info.intent, e);
        }
    }

    public static boolean isVCard(NdefRecord record) {
        try {
            parse(record);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
