package org.telegram.messenger;

import android.content.SharedPreferences;

import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class TenseiDeletedMessages {

    private static final Set<String> deletedKeys = Collections.synchronizedSet(new HashSet<>());
    private static volatile boolean loaded = false;

    // Messages the user deleted themselves (local action). These must really
    // disappear instead of being kept with the red bin icon, and the server
    // echo of our own delete must not re-keep them either.
    private static final Set<String> selfDeletedKeys = Collections.synchronizedSet(new HashSet<>());

    private static SharedPreferences getPrefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences("tensei_deleted_msgs", 0);
    }

    private static void ensureLoaded() {
        if (!loaded) {
            synchronized (deletedKeys) {
                if (!loaded) {
                    try {
                        Set<String> allKeys = getPrefs().getAll().keySet();
                        SharedPreferences.Editor ed = null;
                        for (String kk : allKeys) {
                            if (kk != null && kk.contains("_")) {
                                deletedKeys.add(kk);
                            } else {
                                try {
                                    if (ed == null) ed = getPrefs().edit();
                                    ed.remove(kk);
                                } catch (Throwable ignore) {}
                            }
                        }
                        try {
                            if (ed != null) ed.apply();
                        } catch (Throwable ignore) {}
                    } catch (Exception e) {
                        FileLog.e(e);
                    }
                    loaded = true;
                }
            }
        }
    }

    private static String key(long dialogId, int mid) {
        return dialogId + "_" + mid;
    }

    public static void markDeleted(int account, long dialogId, int mid) {
        if (mid <= 0) {
            return;
        }
        ensureLoaded();
        String k = key(dialogId, mid);
        deletedKeys.add(k);

        try {
            getPrefs().edit().putBoolean(k, true).apply();
        } catch (Exception e) {
            FileLog.e(e);
        }

        try {
            MessagesController mc = MessagesController.getInstance(account);
            if (mc != null) {
                MessageObject obj = mc.dialogMessagesByIds.get(mid);
                if (obj != null) {
                    obj.isDeletedByPeer = true;
                }
            }
        } catch (Exception ignored) {
        }

        AndroidUtilities.runOnUIThread(() -> {
            try {
                NotificationCenter.getInstance(account).postNotificationName(
                        NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_CHAT);
            } catch (Exception ignored) {
            }
        });
    }

    public static boolean isDeleted(long dialogId, int mid) {
        ensureLoaded();
        if (mid <= 0) return false;
        return deletedKeys.contains(key(dialogId, mid));
    }

    public static void markSelfDeleted(long dialogId, ArrayList<Integer> mids) {
        if (mids == null || mids.isEmpty()) {
            return;
        }
        ensureLoaded();
        for (Integer mid : mids) {
            if (mid == null || mid <= 0) {
                continue;
            }
            String k = key(dialogId, mid);
            selfDeletedKeys.add(k);
            deletedKeys.remove(k);
        }
        try {
            SharedPreferences.Editor ed = getPrefs().edit();
            for (Integer mid : mids) {
                if (mid == null || mid <= 0) {
                    continue;
                }
                ed.remove(key(dialogId, mid));
            }
            ed.apply();
        } catch (Exception ignored) {
        }
    }

    public static boolean isSelfDeleted(long dialogId, int mid) {
        if (mid <= 0) return false;
        return selfDeletedKeys.contains(key(dialogId, mid));
    }

    // Returns true if this delete was initiated by the user themselves.
    // Consumes the flag so a later genuine peer delete of the same id can be kept again.
    public static boolean consumeSelfDeleted(long dialogId, int mid) {
        if (mid <= 0) return false;
        return selfDeletedKeys.remove(key(dialogId, mid));
    }

    public static boolean isDeleted(MessageObject obj) {
        if (obj == null) return false;
        if (obj.isDeletedByPeer) return true;
        return isDeleted(obj.getDialogId(), obj.getId());
    }

    public static boolean keepViewOnce(MessageObject obj) {
        try {
            if (!TenseiConfig.showDeletedMessages) return false;
            if (obj == null || obj.messageOwner == null || obj.isOut()) return false;
            TLRPC.MessageMedia media = MessageObject.getMedia(obj.messageOwner);
            return media != null && media.ttl_seconds == 0x7FFFFFFF;
        } catch (Throwable ignore) {
            return false;
        }
    }

    public static CharSequence attachDeletedIcon(CharSequence timeString) {
        if (timeString == null) return null;
        try {
            android.text.SpannableStringBuilder ssb = new android.text.SpannableStringBuilder();
            android.text.SpannableString iconSpanStr = new android.text.SpannableString("d ");
            org.telegram.ui.Components.ColoredImageSpan span = new org.telegram.ui.Components.ColoredImageSpan(
                    R.drawable.ic_tensei_dustbin, org.telegram.ui.Components.ColoredImageSpan.ALIGN_CENTER);
            span.setSize(AndroidUtilities.dp(11));
            span.setWidth(AndroidUtilities.dp(12));
            span.recolorDrawable = false;
            span.translate(0, AndroidUtilities.dp(-0.5f));
            iconSpanStr.setSpan(span, 0, 1, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            ssb.append(iconSpanStr);
            ssb.append(timeString);
            return ssb;
        } catch (Throwable t) {
            return timeString;
        }
    }
}
