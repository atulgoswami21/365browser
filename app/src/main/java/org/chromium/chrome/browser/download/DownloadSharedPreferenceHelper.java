// Copyright 2017 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.chrome.browser.download;

import android.content.SharedPreferences;

import org.chromium.base.ContextUtils;
import org.chromium.base.ObserverList;
import org.chromium.base.VisibleForTesting;
import org.chromium.components.offline_items_collection.ContentId;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Class for maintaining all entries of DownloadSharedPreferenceEntry.
 */
public class DownloadSharedPreferenceHelper {
    /** Observes modifications to the SharedPreferences for {@link DownloadItem}s. */
    public interface Observer {
        /** Called when a {@link DownloadSharedPreferenceEntry} has been updated. */
        void onAddOrReplaceDownloadSharedPreferenceEntry(ContentId id);
    }

    @VisibleForTesting
    static final String KEY_PENDING_DOWNLOAD_NOTIFICATIONS = "PendingDownloadNotifications";
    private final List<DownloadSharedPreferenceEntry> mDownloadSharedPreferenceEntries =
            new ArrayList<DownloadSharedPreferenceEntry>();
    private final ObserverList<Observer> mObservers = new ObserverList<>();

    private SharedPreferences mSharedPrefs;

    // "Initialization on demand holder idiom"
    private static class LazyHolder {
        private static final DownloadSharedPreferenceHelper INSTANCE =
                new DownloadSharedPreferenceHelper();
    }

    /**
     * Creates DownloadSharedPreferenceHelper.
     */
    public static DownloadSharedPreferenceHelper getInstance() {
        return LazyHolder.INSTANCE;
    }

    private DownloadSharedPreferenceHelper() {
        mSharedPrefs = ContextUtils.getAppSharedPreferences();
        parseDownloadSharedPrefs();
    }

    /**
     * Helper method to make querying whether or not an entry exists for {@code id} easier.
     * @param id The {@link ContentId} to query for.
     * @return Whether or not that entry currently has metadata.
     */
    public boolean hasEntry(ContentId id) {
        return getDownloadSharedPreferenceEntry(id) != null;
    }

    /**
     * Adds a DownloadSharedPreferenceEntry to SharedPrefs. If an entry with the same
     * {@link ContentId} already exists in SharedPrefs, update it if it has changed.
     * @param pendingEntry A DownloadSharedPreferenceEntry to be added.
     */
    public void addOrReplaceSharedPreferenceEntry(DownloadSharedPreferenceEntry pendingEntry) {
        Iterator<DownloadSharedPreferenceEntry> iterator =
                mDownloadSharedPreferenceEntries.iterator();
        while (iterator.hasNext()) {
            DownloadSharedPreferenceEntry entry = iterator.next();
            if (entry.id.equals(pendingEntry.id)) {
                if (entry.equals(pendingEntry)) return;
                iterator.remove();
                break;
            }
        }
        mDownloadSharedPreferenceEntries.add(pendingEntry);
        storeDownloadSharedPreferenceEntries();

        for (Observer observer : mObservers) {
            observer.onAddOrReplaceDownloadSharedPreferenceEntry(pendingEntry.id);
        }
    }

    /**
     * Removes a DownloadSharedPreferenceEntry from SharedPrefs given by the {@link ContentId}.
     * @param id The {@link ContentId} to query for.
     */
    public void removeSharedPreferenceEntry(ContentId id) {
        Iterator<DownloadSharedPreferenceEntry> iterator =
                mDownloadSharedPreferenceEntries.iterator();
        boolean found = false;
        while (iterator.hasNext()) {
            DownloadSharedPreferenceEntry entry = iterator.next();
            if (entry.id.equals(id)) {
                iterator.remove();
                found = true;
                break;
            }
        }
        if (found) {
            storeDownloadSharedPreferenceEntries();
        }
    }

    /**
     * Gets a list of stored SharedPreference entries.
     * return A list of DownloadSharedPreferenceEntry stored in SharedPrefs.
     */
    public List<DownloadSharedPreferenceEntry> getEntries() {
        return mDownloadSharedPreferenceEntries;
    }

    /**
     * Parse a list of the DownloadSharedPreferenceEntry from |mSharedPrefs|.
     */
    private void parseDownloadSharedPrefs() {
        if (!mSharedPrefs.contains(KEY_PENDING_DOWNLOAD_NOTIFICATIONS)) return;
        Set<String> entries = DownloadManagerService.getStoredDownloadInfo(
                mSharedPrefs, KEY_PENDING_DOWNLOAD_NOTIFICATIONS);
        for (String entryString : entries) {
            DownloadSharedPreferenceEntry entry =
                    DownloadSharedPreferenceEntry.parseFromString(entryString);
            if (entry.notificationId > 0) {
                mDownloadSharedPreferenceEntries.add(
                        DownloadSharedPreferenceEntry.parseFromString(entryString));
            }
        }
    }

    /**
     * Gets a DownloadSharedPreferenceEntry that has the given {@link ContentId}.
     * @param id The {@link ContentId} to query for.
     * @return a DownloadSharedPreferenceEntry that has the specified {@link ContentId}.
     */
    public DownloadSharedPreferenceEntry getDownloadSharedPreferenceEntry(ContentId id) {
        for (int i = 0; i < mDownloadSharedPreferenceEntries.size(); ++i) {
            if (mDownloadSharedPreferenceEntries.get(i).id.equals(id)) {
                return mDownloadSharedPreferenceEntries.get(i);
            }
        }
        return null;
    }

    /**
     * Adds the given {@link Observer}.
     * @param observer Observer to notify about changes.
     */
    public void addObserver(Observer observer) {
        mObservers.addObserver(observer);
    }

    /**
     * Removes the given {@link Observer}.
     * @param observer Observer to stop notifying about changes.
     */
    public void removeObserver(Observer observer) {
        mObservers.removeObserver(observer);
    }

    /**
     * Helper method to store all the SharedPreferences entries.
     */
    private void storeDownloadSharedPreferenceEntries() {
        Set<String> entries = new HashSet<String>();
        for (int i = 0; i < mDownloadSharedPreferenceEntries.size(); ++i) {
            entries.add(mDownloadSharedPreferenceEntries.get(i).getSharedPreferenceString());
        }
        DownloadManagerService.storeDownloadInfo(
                mSharedPrefs, KEY_PENDING_DOWNLOAD_NOTIFICATIONS, entries);
    }
}
