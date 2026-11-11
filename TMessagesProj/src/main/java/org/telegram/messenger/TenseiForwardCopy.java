package org.telegram.messenger;

import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;

// TenseiGram: restricted-forward bypass.
// Telegram server rejects TL_messages_forwardMessages for noforwards content
// (shows infinite loading then error icon). When allowForward is ON, resend
// restricted messages as fresh copies (same file reference, no download needed,
// no forward header) so 3GB files can go to Saved Messages instantly.
public class TenseiForwardCopy {

    public static boolean isRestricted(MessageObject mo) {
        try {
            if (mo == null || mo.messageOwner == null) {
                return false;
            }
            if (mo.messageOwner.noforwards) {
                return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    public static boolean isSourceRestricted(int account, MessageObject mo) {
        if (isRestricted(mo)) {
            return true;
        }
        try {
            if (mo != null && MessagesController.getInstance(account).isPeerNoForwards(mo.getDialogId())) {
                return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    /**
     * If allowForward is on and any message is restricted, send restricted ones as copies
     * and return the remaining (non-restricted) list for normal server forward.
     * Returns null when everything was handled via copy (caller should skip forward).
     */
    public static ArrayList<MessageObject> splitRestricted(int account, ArrayList<MessageObject> messages) {
        if (!TenseiConfig.allowForward || messages == null || messages.isEmpty()) {
            return messages;
        }
        ArrayList<MessageObject> restricted = null;
        ArrayList<MessageObject> normal = null;
        for (MessageObject mo : messages) {
            if (mo != null && isSourceRestricted(account, mo)) {
                if (restricted == null) {
                    restricted = new ArrayList<>();
                }
                restricted.add(mo);
            } else {
                if (normal == null) {
                    normal = new ArrayList<>();
                }
                normal.add(mo);
            }
        }
        if (restricted == null || restricted.isEmpty()) {
            return messages;
        }
        // Caller passes peer/flags separately; actual copy is done by copyRestricted().
        // Here we only split. If normal is empty -> return empty list (not null) so caller knows all-copy.
        if (normal == null) {
            normal = new ArrayList<>();
        }
        return normal;
    }

    public static boolean hasRestricted(int account, ArrayList<MessageObject> messages) {
        if (!TenseiConfig.allowForward || messages == null) {
            return false;
        }
        for (MessageObject mo : messages) {
            if (mo != null && isSourceRestricted(account, mo)) {
                return true;
            }
        }
        return false;
    }

    public static void copyRestricted(int account, ArrayList<MessageObject> restricted, long peer,
                                      boolean notify, int scheduleDate) {
        if (restricted == null || restricted.isEmpty()) {
            return;
        }
        SendMessagesHelper helper;
        try {
            helper = SendMessagesHelper.getInstance(account);
        } catch (Throwable t) {
            return;
        }
        for (MessageObject mo : restricted) {
            try {
                if (!copyOne(helper, mo, peer, notify, scheduleDate)) {
                    // Fallback: send caption/text if copy of media failed.
                    fallbackText(helper, mo, peer, notify, scheduleDate);
                }
            } catch (Throwable ignored) {
            }
        }
    }

    private static void fallbackText(SendMessagesHelper helper, MessageObject mo, long peer,
                                     boolean notify, int scheduleDate) {
        try {
            String text = null;
            if (mo.caption != null && mo.caption.length() > 0) {
                text = mo.caption.toString();
            } else if (mo.messageText != null && mo.messageText.length() > 0) {
                text = mo.messageText.toString();
            } else if (mo.messageOwner != null && mo.messageOwner.message != null && !mo.messageOwner.message.isEmpty()) {
                text = mo.messageOwner.message;
            }
            if (text == null || text.isEmpty()) {
                text = "";
            }
            if (!text.isEmpty()) {
                helper.sendMessage(SendMessagesHelper.SendMessageParams.of(
                        text, peer, null, null, null, true,
                        mo.messageOwner != null ? mo.messageOwner.entities : null,
                        null, null, notify, scheduleDate, 0, null, false));
            }
        } catch (Throwable ignored) {
        }
    }

    @SuppressWarnings("unchecked")
    private static boolean copyOne(SendMessagesHelper helper, MessageObject mo, long peer,
                                   boolean notify, int scheduleDate) {
        if (helper == null || mo == null || mo.messageOwner == null) {
            return false;
        }
        try {
            TLRPC.MessageMedia media = MessageObject.getMedia(mo.messageOwner);
            String caption = null;
            try {
                if (mo.caption != null && mo.caption.length() > 0) {
                    caption = mo.caption.toString();
                }
            } catch (Throwable ignored) {
            }
            ArrayList<TLRPC.MessageEntity> entities = null;
            try {
                entities = mo.messageOwner.entities;
            } catch (Throwable ignored) {
            }

            // Document (video, file, audio, 3GB etc.) -> resend same file reference, no download.
            if (media instanceof TLRPC.TL_messageMediaDocument) {
                TLRPC.TL_messageMediaDocument md = (TLRPC.TL_messageMediaDocument) media;
                if (md.document instanceof TLRPC.TL_document) {
                    TLRPC.TL_document doc = (TLRPC.TL_document) md.document;
                    // Skip stickers/games (can't copy cleanly); let normal path handle.
                    try {
                        if (mo.isSticker() || mo.isAnimatedSticker() || mo.isGame()) {
                            return false;
                        }
                    } catch (Throwable ignored) {
                    }
                    helper.sendMessage(SendMessagesHelper.SendMessageParams.of(
                            doc, null, null, peer, null, null,
                            caption, entities, null, null,
                            notify, scheduleDate, 0, 0, null, null, false));
                    return true;
                }
                return false;
            }

            // Photo -> resend same photo reference.
            if (media instanceof TLRPC.TL_messageMediaPhoto) {
                TLRPC.TL_messageMediaPhoto mp = (TLRPC.TL_messageMediaPhoto) media;
                if (mp.photo instanceof TLRPC.TL_photo) {
                    TLRPC.TL_photo photo = (TLRPC.TL_photo) mp.photo;
                    helper.sendMessage(SendMessagesHelper.SendMessageParams.of(
                            photo, null, peer, null, null,
                            caption, entities, null, null,
                            notify, scheduleDate, 0, 0, null, false));
                    return true;
                }
                return false;
            }

            // Plain text / caption-only.
            if (media == null || media instanceof TLRPC.TL_messageMediaEmpty
                    || media instanceof TLRPC.TL_messageMediaWebPage) {
                String text = null;
                try {
                    if (mo.messageText != null && mo.messageText.length() > 0) {
                        text = mo.messageText.toString();
                    } else if (mo.messageOwner.message != null) {
                        text = mo.messageOwner.message;
                    }
                } catch (Throwable ignored) {
                }
                if (text != null && !text.isEmpty()) {
                    TLRPC.WebPage webPage = media instanceof TLRPC.TL_messageMediaWebPage
                            ? ((TLRPC.TL_messageMediaWebPage) media).webpage : null;
                    helper.sendMessage(SendMessagesHelper.SendMessageParams.of(
                            text, peer, null, null, webPage, webPage != null,
                            entities, null, null, notify, scheduleDate, 0, null, false));
                    return true;
                }
                return false;
            }

            return false;
        } catch (Throwable t) {
            return false;
        }
    }
}
