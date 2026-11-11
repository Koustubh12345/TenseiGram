package org.telegram.ui;

import org.telegram.messenger.R;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.TenseiAI;
import org.telegram.messenger.TenseiConfig;
import org.telegram.messenger.TenseiMarkdown;
import org.telegram.messenger.TenseiVideoDownloader;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.Components.BulletinFactory;

import java.util.ArrayList;

public class TenseiCommands {

    public static boolean handle(ChatActivity chatActivity, String rawText) {
        if (chatActivity == null || rawText == null) {
            return false;
        }
        if (!TenseiConfig.commandsEnabled) {
            return false;
        }
        String text = rawText.trim();
        int account = chatActivity.getCurrentAccount();
        long dialogId = chatActivity.getDialogId();

        if (text.equals("/ping") && TenseiConfig.pingEnabled) {
            int pingMs = ConnectionsManager.native_getCurrentPingTime(account);
            sendAsUser(account, dialogId, "Pong! " + pingMs + " ms", null);
            return true;
        }

        if ((text.equals("/aimode") || text.startsWith("/aimode ")) && TenseiConfig.aiModeEnabled) {
            String query = "";
            if (text.length() > 7) {
                query = text.substring(7).trim();
            }
            if (query.isEmpty()) {
                bulletin(chatActivity, "Usage: /aimode <your question>");
                return true;
            }
            bulletin(chatActivity, "Asking AI...");
            final String q = query;
            TenseiAI.ask(q, new TenseiAI.Callback() {
                @Override
                public void onResult(String answer) {
                    TenseiMarkdown.Result r = TenseiMarkdown.formatQa(q, answer);
                    sendAsUser(account, dialogId, r.text, r.entities);
                }

                @Override
                public void onError() {
                    bulletin(chatActivity, "AI didn't respond, try again");
                }
            });
            return true;
        }

        if ((text.equals("/ytdl") || text.startsWith("/ytdl ")) && TenseiConfig.ytdlEnabled) {
            String query = "";
            if (text.length() > 5) {
                query = text.substring(5).trim();
            }
            if (query.isEmpty()) {
                bulletin(chatActivity, "Usage: /ytdl <video link>");
                return true;
            }
            TenseiVideoDownloader.downloadAndSend(chatActivity, query);
            return true;
        }

        return false;
    }

    private static void sendAsUser(int account, long dialogId, String text,
                                   ArrayList<TLRPC.MessageEntity> entities) {
        try {
            SendMessagesHelper.getInstance(account).sendMessage(
                    SendMessagesHelper.SendMessageParams.of(
                            text, dialogId,
                            null, null, null,
                            false, entities, null, null,
                            true, 0, 0, null, false));
        } catch (Exception e) {
            try {
                org.telegram.messenger.FileLog.e(e);
            } catch (Throwable ignored) {
            }
        }
    }

    private static void bulletin(ChatActivity chatActivity, String text) {
        boolean shown = false;
        try {
            BulletinFactory.of(chatActivity)
                    .createSimpleBulletin(R.raw.star_premium_2, text)
                    .show();
            shown = true;
        } catch (Exception ignored) {
        }
        if (!shown) {
            // Fallback so /aimode errors are never silent (e.g. activity gone).
            try {
                android.widget.Toast.makeText(
                        org.telegram.messenger.ApplicationLoader.applicationContext,
                        text, android.widget.Toast.LENGTH_SHORT).show();
            } catch (Throwable ignored) {
            }
        }
    }
}
