package org.telegram.messenger;

import android.content.SharedPreferences;

public class TenseiConfig {

    public static boolean hideTyping = false;
    public static boolean keepScreenOn = false;
    public static boolean disableLinkPreview = false;
    public static boolean sendWithoutSound = false;
    public static boolean hideSeen = false;

    public static boolean disableAllNotifications = false;
    public static boolean hidePhoneNumber = false;

    public static boolean ghostMode = false;
    public static boolean ghostDontSendRead = true;
    public static boolean ghostDontSendOnline = true;
    public static boolean ghostDontSendTyping = true;
    public static boolean ghostDontSendReadStories = true;

    public static boolean vaultSaveDeleted = false;
    public static boolean vaultSaveMedia = true;
    public static boolean spySaveDeleted = false; // legacy alias
    public static boolean spySaveMedia = true; // legacy alias

    public static boolean commandsEnabled = true;
    public static boolean pingEnabled = true;
    public static boolean aiModeEnabled = true;
    public static boolean ytdlEnabled = true;

    public static boolean expConfirmStickers = false;

    public static boolean autoGrammarCorrect = false;

    public static boolean highSpeedNetworking = false;
    public static boolean hideOnline = false;
    public static boolean vibrationEnabled = true;
    public static int vibrationIntensity = 100;
    public static boolean showDeletedMessages = false;
    public static boolean hidePhoneFromBots = true;
    public static boolean allowScreenshot = false;
    public static boolean allowForward = false;
    public static boolean disableAds = true;
    public static int recentStickersLimit = 0;
    public static boolean localPremium = true;

    private static SharedPreferences getPrefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences("tensei_mods", 0);
    }

    public static void load() {
        SharedPreferences prefs = getPrefs();
        hideTyping = prefs.getBoolean("hideTyping", false);
        keepScreenOn = prefs.getBoolean("keepScreenOn", false);
        disableLinkPreview = prefs.getBoolean("disableLinkPreview", false);
        sendWithoutSound = prefs.getBoolean("sendWithoutSound", false);
        hideSeen = prefs.getBoolean("hideSeen", false);

        disableAllNotifications = prefs.getBoolean("disableAllNotifications", false);
        hidePhoneNumber = prefs.getBoolean("hidePhoneNumber", false);

        ghostMode = prefs.getBoolean("ghostMode", false);
        ghostDontSendRead = prefs.getBoolean("ghostDontSendRead", true);
        ghostDontSendOnline = prefs.getBoolean("ghostDontSendOnline", true);
        ghostDontSendTyping = prefs.getBoolean("ghostDontSendTyping", true);
        ghostDontSendReadStories = prefs.getBoolean("ghostDontSendReadStories", true);

        vaultSaveDeleted = prefs.getBoolean("vaultSaveDeleted", prefs.getBoolean("spySaveDeleted", false));
        vaultSaveMedia = prefs.getBoolean("vaultSaveMedia", prefs.getBoolean("spySaveMedia", true));
        spySaveDeleted = vaultSaveDeleted;
        spySaveMedia = vaultSaveMedia;

        commandsEnabled = prefs.getBoolean("commandsEnabled", true);
        pingEnabled = prefs.getBoolean("pingEnabled", true);
        aiModeEnabled = prefs.getBoolean("aiModeEnabled", true);
        ytdlEnabled = prefs.getBoolean("ytdlEnabled", true);

        expConfirmStickers = prefs.getBoolean("expConfirmStickers", false);
        autoGrammarCorrect = prefs.getBoolean("autoGrammarCorrect", false);
        highSpeedNetworking = prefs.getBoolean("highSpeedNetworking", false);
        hideOnline = prefs.getBoolean("hideOnline", false);
        vibrationEnabled = prefs.getBoolean("vibrationEnabled", true);
        vibrationIntensity = prefs.getInt("vibrationIntensity", 100);
        showDeletedMessages = prefs.getBoolean("showDeletedMessages", false);
        hidePhoneFromBots = prefs.getBoolean("hidePhoneFromBots", true);
        allowScreenshot = prefs.getBoolean("allowScreenshot", false);
        allowForward = prefs.getBoolean("allowForward", false);
        disableAds = prefs.getBoolean("disableAds", true);
        recentStickersLimit = prefs.getInt("recentStickersLimit", 0);
        localPremium = prefs.getBoolean("localPremium", true);
    }

    public static void save() {
        getPrefs().edit()
                .putBoolean("hideTyping", hideTyping)
                .putBoolean("keepScreenOn", keepScreenOn)
                .putBoolean("disableLinkPreview", disableLinkPreview)
                .putBoolean("sendWithoutSound", sendWithoutSound)
                .putBoolean("hideSeen", hideSeen)
                .putBoolean("disableAllNotifications", disableAllNotifications)
                .putBoolean("hidePhoneNumber", hidePhoneNumber)
                .putBoolean("ghostMode", ghostMode)
                .putBoolean("ghostDontSendRead", ghostDontSendRead)
                .putBoolean("ghostDontSendOnline", ghostDontSendOnline)
                .putBoolean("ghostDontSendTyping", ghostDontSendTyping)
                .putBoolean("ghostDontSendReadStories", ghostDontSendReadStories)
                .putBoolean("vaultSaveDeleted", vaultSaveDeleted)
                .putBoolean("vaultSaveMedia", vaultSaveMedia)
                .putBoolean("commandsEnabled", commandsEnabled)
                .putBoolean("pingEnabled", pingEnabled)
                .putBoolean("aiModeEnabled", aiModeEnabled)
                .putBoolean("ytdlEnabled", ytdlEnabled)
                .putBoolean("expConfirmStickers", expConfirmStickers)
                .putBoolean("autoGrammarCorrect", autoGrammarCorrect)
                .putBoolean("highSpeedNetworking", highSpeedNetworking)
                .putBoolean("hideOnline", hideOnline)
                .putBoolean("vibrationEnabled", vibrationEnabled)
                .putInt("vibrationIntensity", vibrationIntensity)
                .putBoolean("showDeletedMessages", showDeletedMessages)
                .putBoolean("hidePhoneFromBots", hidePhoneFromBots)
                .putBoolean("allowScreenshot", allowScreenshot)
                .putBoolean("allowForward", allowForward)
                .putBoolean("disableAds", disableAds)
                .putInt("recentStickersLimit", recentStickersLimit)
                .putBoolean("localPremium", localPremium)
                .apply();
    }

    public static boolean ghostActive() {
        return ghostMode;
    }

    public static boolean blockReadReceipts() {
        return hideSeen || (ghostMode && ghostDontSendRead);
    }

    public static boolean blockStoryRead() {
        return hideSeen || (ghostMode && ghostDontSendReadStories);
    }

    public static boolean blockOnlineStatus() {
        return ghostMode && ghostDontSendOnline;
    }

    public static boolean blockTyping() {
        return hideTyping || (ghostMode && ghostDontSendTyping);
    }

    public static void setDisableAllNotifications(int account, boolean disable) {
        disableAllNotifications = disable;
        save();
        NotificationsController nc = NotificationsController.getInstance(account);
        int time = disable ? Integer.MAX_VALUE : 0;
        nc.setGlobalNotificationsEnabled(NotificationsController.TYPE_PRIVATE, time);
        nc.setGlobalNotificationsEnabled(NotificationsController.TYPE_GROUP, time);
        nc.setGlobalNotificationsEnabled(NotificationsController.TYPE_CHANNEL, time);
    }
}
