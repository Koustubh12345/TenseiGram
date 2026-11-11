package org.telegram.ui;

import android.content.Context;
import android.content.res.Configuration;

import org.telegram.ui.ActionBar.Theme;

public class TenseiTheme {

    public static boolean isDark(Context context) {
        if (Theme.getCurrentTheme() != null && Theme.getCurrentTheme().isDark()) {
            return true;
        }
        if (context != null) {
            try {
                int nightMode = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
                return nightMode == Configuration.UI_MODE_NIGHT_YES;
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    public static int getBgColor(Context context) {
        return isDark(context) ? 0xFF0E1117 : 0xFFF7F7F9;
    }

    public static int getCardColor(Context context) {
        return isDark(context) ? 0xFF181D26 : 0xFFFFFFFF;
    }

    public static int getTitleColor(Context context) {
        return isDark(context) ? 0xFFFFFFFF : 0xFF1C1C1E;
    }

    public static int getSubtitleColor(Context context) {
        return isDark(context) ? 0xFF9098A9 : 0xFF8E8E93;
    }

    public static int getIconBgColor(Context context) {
        return isDark(context) ? 0xFF222936 : 0xFFF1F3F7;
    }

    public static int getIconTintColor(Context context) {
        return isDark(context) ? 0xFFF2F4F8 : 0xFF222428;
    }

    public static int getDividerColor(Context context) {
        return isDark(context) ? 0xFF222936 : 0xFFECEEF2;
    }

    public static int getChevronColor(Context context) {
        return isDark(context) ? 0xFF6C7485 : 0xFFC7C7CC;
    }

    public static int getSwitchTrackUnchecked(Context context) {
        return isDark(context) ? 0xFF2C3240 : 0xFFE5E5EA;
    }

    public static int getSwitchTrackChecked(Context context) {
        return isDark(context) ? 0xFF2979FF : 0xFF3372E6;
    }
}
