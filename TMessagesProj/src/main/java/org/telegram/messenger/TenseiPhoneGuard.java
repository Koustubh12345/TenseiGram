package org.telegram.messenger;

import org.telegram.tgnet.TLRPC;

public class TenseiPhoneGuard {
    public static boolean hideFor(TLRPC.User u) {
        try {
            if (u == null) return false;
            if (!TenseiConfig.hidePhoneFromBots) return false;
            if (u.bot) return true;
            if (u.phone == null || u.phone.isEmpty()) return false;
            long self = UserConfig.getInstance(UserConfig.selectedAccount).getClientUserId();
            if (u.id == self) return false;
            if (u.contact) return false;
            if (u.mutual_contact) return false;
            return true;
        } catch (Throwable ignore) { return false; }
    }
    public static String safePhone(TLRPC.User u) {
        try {
            if (hideFor(u)) return "";
            return u.phone;
        } catch (Throwable ignore) { return ""; }
    }
    public static boolean blockShareToBot(int account, long dialogId) {
        try {
            if (!TenseiConfig.hidePhoneFromBots) return false;
            TLRPC.User u = null;
            if (dialogId > 0) u = MessagesController.getInstance(account).getUser(dialogId);
            return u != null && u.bot;
        } catch (Throwable ignore) { return false; }
    }
}
