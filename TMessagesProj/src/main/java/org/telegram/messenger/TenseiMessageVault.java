package org.telegram.messenger;

import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;

public class TenseiMessageVault {

    public static void onMessagesDeleted(int account, long dialogId, ArrayList<Integer> mids) {
        if (!TenseiConfig.vaultSaveDeleted || mids == null || mids.isEmpty()) {
            return;
        }
        long selfId = UserConfig.getInstance(account).getClientUserId();
        if (dialogId == selfId) {
            return;
        }
        MessagesController mc = MessagesController.getInstance(account);
        String chatName = DialogObject.getName(account, dialogId);

        for (Integer mid : mids) {
            if (mid == null || mid <= 0) {
                continue;
            }
            if (TenseiDeletedMessages.isSelfDeleted(dialogId, mid)) {
                continue;
            }
            MessageObject obj = mc.dialogMessagesByIds.get(mid);
            if (obj == null || obj.messageOwner == null) {
                continue;
            }
            if (obj.isOutOwner()) {
                continue;
            }
            if (obj.messageOwner instanceof TLRPC.TL_messageService) {
                continue;
            }

            String sender = obj.messageOwner.from_id instanceof TLRPC.TL_peerUser
                    ? UserObject.getUserName(MessagesController.getInstance(account)
                            .getUser(((TLRPC.TL_peerUser) obj.messageOwner.from_id).user_id))
                    : chatName;

            StringBuilder sb = new StringBuilder();
            sb.append("Deleted in ").append(chatName).append(" by ").append(sender).append(":\n\n");

            String text = obj.messageOwner.message;
            boolean hasMedia = obj.messageOwner.media != null
                    && !(obj.messageOwner.media instanceof TLRPC.TL_messageMediaEmpty);

            if (text != null && !text.isEmpty()) {
                sb.append(text);
                if (hasMedia) {
                    sb.append("\n\n[with media]");
                }
            } else if (hasMedia) {
                sb.append("[")
                        .append(mediaLabel(obj.messageOwner.media))
                        .append("]");
            } else {
                continue;
            }

            sendNote(account, selfId, sb.toString());
        }
    }

    private static String mediaLabel(TLRPC.MessageMedia media) {
        if (media instanceof TLRPC.TL_messageMediaPhoto) return "photo";
        if (media instanceof TLRPC.TL_messageMediaDocument) {
            TLRPC.Document doc = ((TLRPC.TL_messageMediaDocument) media).document;
            if (doc != null && doc.mime_type != null) {
                if (doc.mime_type.startsWith("video")) return "video";
                if (doc.mime_type.startsWith("audio")) return "audio";
                if (doc.mime_type.startsWith("image")) return "photo";
            }
            return "file";
        }
        if (media instanceof TLRPC.TL_messageMediaGeo) return "location";
        if (media instanceof TLRPC.TL_messageMediaContact) return "contact";
        if (media instanceof TLRPC.TL_messageMediaPoll) return "poll";
        if (media instanceof TLRPC.TL_messageMediaWebPage) return "link";
        return "media";
    }

    private static void sendNote(int account, long selfId, String text) {
        try {
            SendMessagesHelper.getInstance(account).sendMessage(
                    SendMessagesHelper.SendMessageParams.of(
                            text, selfId,
                            null, null, null,
                            false, null, null, null,
                            true, 0, 0, null, false));
        } catch (Exception ignored) {
        }
    }
}
