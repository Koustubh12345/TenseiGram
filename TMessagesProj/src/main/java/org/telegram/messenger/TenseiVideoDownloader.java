package org.telegram.messenger;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.BulletinFactory;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TenseiVideoDownloader {

    private static final Pattern YOUTUBE_PATTERN = Pattern.compile(
            "(?:youtube\\.com/(?:[^/]+/.+/|(?:v|e(?:mbed)?|shorts)/|.*[?&]v=)|youtu\\.be/)([^\"&?/\\s]{11})");
    private static final Pattern TIKTOK_PATTERN = Pattern.compile(
            "https?://(?:www\\.|vm\\.|vt\\.)?tiktok\\.com/[^\\s]+");
    private static final Pattern INSTAGRAM_PATTERN = Pattern.compile(
            "https?://(?:www\\.|[a-zA-Z0-9_.-]+\\.)?(?:instagram\\.com|instagr\\.am)/(?:share/)?(?:p|reel|reels|tv)/([^/?#&\\s]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern REDDIT_PATTERN = Pattern.compile(
            "https?://(?:www\\.|old\\.|m\\.|new\\.)?(?:reddit\\.com/r/[^/\\s]+/comments/[^/\\s]+|redd\\.it/[^/?#&\\s]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern X_PATTERN = Pattern.compile(
            "https?://(?:www\\.|mobile\\.)?(?:twitter\\.com|x\\.com)/[^/\\s]+/status/\\d+", Pattern.CASE_INSENSITIVE);
    private static final Pattern FB_PATTERN = Pattern.compile(
            "https?://(?:www\\.|m\\.|web\\.)?(?:facebook\\.com/(?:watch|reel|share|.*\\/videos\\/)[^\\s]*|fb\\.watch/[^/?#&\\s]+)", Pattern.CASE_INSENSITIVE);

    public static boolean isSupportedVideoUrl(String url) {
        if (url == null || url.isEmpty()) return false;
        String u = url.trim();
        return YOUTUBE_PATTERN.matcher(u).find()
                || INSTAGRAM_PATTERN.matcher(u).find()
                || TIKTOK_PATTERN.matcher(u).find()
                || REDDIT_PATTERN.matcher(u).find()
                || X_PATTERN.matcher(u).find()
                || FB_PATTERN.matcher(u).find()
                || isDirectMediaUrl(u);
    }

    public static void downloadAndSend(ChatActivity chatActivity, String rawUrl) {
        if (chatActivity == null || rawUrl == null || rawUrl.trim().isEmpty()) {
            return;
        }

        final String videoUrl = rawUrl.trim();
        final int account = chatActivity.getCurrentAccount();
        final long dialogId = chatActivity.getDialogId();

        showBulletin(chatActivity, "Downloading video...");

        Utilities.globalQueue.postRunnable(() -> {
            File downloadedFile = null;
            try {
                Matcher ytMatcher = YOUTUBE_PATTERN.matcher(videoUrl);
                if (ytMatcher.find()) {
                    String videoId = ytMatcher.group(1);
                    downloadedFile = downloadYouTubeVideo(videoId);
                }

                if (downloadedFile == null && videoUrl.startsWith("http")) {
                    try {
                        File cobaltFile = cobaltTunnelForPage(videoUrl);
                        if (cobaltFile != null && isValidVideoFile(cobaltFile)) {
                            downloadedFile = cobaltFile;
                        }
                    } catch (Exception ignored) {
                    }
                }

                if (downloadedFile == null) {
                    Matcher igMatcher = INSTAGRAM_PATTERN.matcher(videoUrl);
                    if (igMatcher.find()) {
                        String shortcode = igMatcher.group(1);
                        String streamUrl = resolveInstagramStream(shortcode, videoUrl);
                        if (streamUrl != null && !streamUrl.isEmpty()) {
                            downloadedFile = downloadFile(streamUrl);
                        }
                    }
                }

                if (downloadedFile == null) {
                    Matcher ttMatcher = TIKTOK_PATTERN.matcher(videoUrl);
                    if (ttMatcher.find()) {
                        String streamUrl = resolveTikTokStream(videoUrl);
                        if (streamUrl != null && !streamUrl.isEmpty()) {
                            downloadedFile = downloadFile(streamUrl);
                        }
                    }
                }

                if (downloadedFile == null && isDirectMediaUrl(videoUrl)) {
                    downloadedFile = downloadFile(videoUrl);
                }

                if (downloadedFile == null || !isValidVideoFile(downloadedFile)) {
                    if (downloadedFile != null && downloadedFile.exists()) {
                        downloadedFile.delete();
                    }
                    showBulletin(chatActivity, "Could not extract video stream. Link may be private or restricted.");
                    return;
                }

                final File finalFile = downloadedFile;
                AndroidUtilities.runOnUIThread(() -> {
                    try {
                        AccountInstance accountInstance = AccountInstance.getInstance(account);
                        showBulletin(chatActivity, "Sending video...");
                        try {
                            SendMessagesHelper.prepareSendingVideo(
                                    accountInstance,
                                    finalFile.getAbsolutePath(),
                                    null,
                                    null,
                                    null,
                                    dialogId,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    0,
                                    null,
                                    true,
                                    0,
                                    0,
                                    false,
                                    false,
                                    null,
                                    null,
                                    0L,
                                    0L
                            );
                            showBulletin(chatActivity, "Video sent!");
                        } catch (Throwable t) {
                            SendMessagesHelper.prepareSendingDocument(
                                    accountInstance,
                                    finalFile.getAbsolutePath(),
                                    finalFile.getAbsolutePath(),
                                    null,
                                    null,
                                    "video/mp4",
                                    dialogId,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    true,
                                    0,
                                    null,
                                    null,
                                    false
                            );
                            showBulletin(chatActivity, "Video sent!");
                        }
                    } catch (Exception e) {
                        FileLog.e("TenseiVideoDownloader send error: " + e.getMessage());
                        showBulletin(chatActivity, "Send failed: " + e.getMessage());
                    }
                });
            } catch (Exception e) {
                FileLog.e("TenseiVideoDownloader error: " + e.getMessage());
                if (downloadedFile != null && downloadedFile.exists()) {
                    downloadedFile.delete();
                }
                showBulletin(chatActivity, "Could not download playable video. Check link or try another.");
            }
        });
    }

    private static boolean isDirectMediaUrl(String url) {
        if (url == null) return false;
        String lower = url.toLowerCase();
        return lower.endsWith(".mp4") || lower.endsWith(".webm") || lower.endsWith(".mov")
                || lower.endsWith(".mkv") || lower.endsWith(".avi") || lower.contains(".mp4?");
    }

    private static void showBulletin(ChatActivity chat, String msg) {
        if (chat == null) return;
        AndroidUtilities.runOnUIThread(() -> {
            try {
                BulletinFactory.of(chat)
                        .createSimpleBulletin(org.telegram.messenger.R.raw.star_premium_2, msg)
                        .show();
            } catch (Exception ignored) {}
        });
    }

    private static File downloadYouTubeVideo(String videoId) {
        if (videoId == null || videoId.isEmpty()) return null;

        try {
            File file = downloadCobaltTunnel(videoId);
            if (file != null && isValidVideoFile(file)) {
                return file;
            }
        } catch (Exception e) {
            FileLog.e("TenseiVideoDownloader Cobalt tier error: " + e.getMessage());
        }

        try {
            File file = downloadYouTubeInnerTube(videoId);
            if (file != null && isValidVideoFile(file)) {
                return file;
            }
        } catch (Exception e) {
            FileLog.e("TenseiVideoDownloader InnerTube tier error: " + e.getMessage());
        }

        try {
            File file = downloadYouTubeInvidious(videoId);
            if (file != null && isValidVideoFile(file)) {
                return file;
            }
        } catch (Exception e) {
            FileLog.e("TenseiVideoDownloader Invidious tier error: " + e.getMessage());
        }

        try {
            File file = downloadYouTubePiped(videoId);
            if (file != null && isValidVideoFile(file)) {
                return file;
            }
        } catch (Exception e) {
            FileLog.e("TenseiVideoDownloader Piped tier error: " + e.getMessage());
        }

        return null;
    }

    private static File downloadCobaltTunnel(String videoId) {
        if (videoId == null || videoId.isEmpty()) return null;
        return cobaltTunnelForPage("https://www.youtube.com/watch?v=" + videoId);
    }

    private static File cobaltTunnelForPage(String pageUrl) {
        if (pageUrl == null || pageUrl.isEmpty()) return null;
        HttpURLConnection apiConn = null;
        try {
            URL apiUrl = new URL(TenseiSecurity.getYoutubePlayerUrl());
            apiConn = (HttpURLConnection) apiUrl.openConnection();
            apiConn.setRequestMethod("POST");
            apiConn.setConnectTimeout(10000);
            apiConn.setReadTimeout(20000);
            apiConn.setDoOutput(true);
            apiConn.setRequestProperty("Content-Type", "application/json");
            apiConn.setRequestProperty("Accept", "application/json");
            apiConn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36");

            JSONObject body = new JSONObject();
            body.put("url", pageUrl);
            body.put("downloadMode", "auto");
            body.put("videoQuality", "720");

            try (OutputStream os = apiConn.getOutputStream()) {
                os.write(body.toString().getBytes(StandardCharsets.UTF_8));
            }

            if (apiConn.getResponseCode() == 200) {
                String respStr = readStream(apiConn.getInputStream());
                if (respStr == null || respStr.isEmpty()) {
                    return null;
                }
                JSONObject json = new JSONObject(respStr);
                String status = json.optString("status", "");
                if ("tunnel".equals(status) || "redirect".equals(status)) {
                    String streamUrl = json.optString("url", null);
                    if (streamUrl != null && streamUrl.startsWith("http")) {
                        File file = downloadFile(streamUrl);
                        if (file != null && isValidVideoFile(file)) {
                            return file;
                        }
                    }
                } else if ("picker".equals(status)) {
                    JSONArray picker = json.optJSONArray("picker");
                    if (picker != null) {
                        for (int i = 0; i < picker.length(); i++) {
                            JSONObject item = picker.optJSONObject(i);
                            if (item == null) {
                                continue;
                            }
                            String streamUrl = item.optString("url", null);
                            if (streamUrl != null && streamUrl.startsWith("http")) {
                                File file = downloadFile(streamUrl);
                                if (file != null && isValidVideoFile(file)) {
                                    return file;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            FileLog.e("TenseiVideoDownloader Cobalt tunnel error: " + e.getMessage());
        } finally {
            if (apiConn != null) {
                try { apiConn.disconnect(); } catch (Exception ignored) {}
            }
        }
        return null;
    }

    private static File downloadYouTubeInnerTube(String videoId) {
        File f = queryInnerTubeClient(videoId, "ANDROID_VR", "1.61.48", "Oculus", "Quest 3", "Android", "12",
                "com.google.android.apps.youtube.vr.oculus/1.61.48 (Linux; U; Android 12; Quest 3) gzip");
        if (f != null && isValidVideoFile(f)) {
            return f;
        }

        f = queryInnerTubeClient(videoId, "VISIONOS", "1.02", "Apple", "RealityDevice17,1", "visionOS", "26.5.23O471",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 15_7_3) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/26.0 Safari/605.1.15");
        if (f != null && isValidVideoFile(f)) {
            return f;
        }

        f = queryInnerTubeClient(videoId, "ANDROID", "19.09.37", "Google", "Pixel 7 Pro", "Android", "14",
                "com.google.android.youtube/19.09.37 (Linux; U; Android 14; en_US; Pixel 7 Pro)");
        if (f != null && isValidVideoFile(f)) {
            return f;
        }

        return null;
    }

    private static File queryInnerTubeClient(String videoId, String clientName, String clientVersion,
                                             String deviceMake, String deviceModel, String osName, String osVersion,
                                             String userAgent) {
        HttpURLConnection apiConn = null;
        try {
            URL apiUrl = new URL(TenseiSecurity.getYoutubePlayerUrl());
            apiConn = (HttpURLConnection) apiUrl.openConnection();
            apiConn.setRequestMethod("POST");
            apiConn.setConnectTimeout(7000);
            apiConn.setReadTimeout(12000);
            apiConn.setDoOutput(true);
            apiConn.setRequestProperty("Content-Type", "application/json");
            apiConn.setRequestProperty("User-Agent", userAgent);

            JSONObject client = new JSONObject();
            client.put("clientName", clientName);
            client.put("clientVersion", clientVersion);
            if (deviceMake != null) client.put("deviceMake", deviceMake);
            if (deviceModel != null) client.put("deviceModel", deviceModel);
            if (osName != null) client.put("osName", osName);
            if (osVersion != null) client.put("osVersion", osVersion);
            client.put("hl", "en");
            client.put("gl", "US");

            JSONObject context = new JSONObject();
            context.put("client", client);

            JSONObject body = new JSONObject();
            body.put("videoId", videoId);
            body.put("context", context);

            try (OutputStream os = apiConn.getOutputStream()) {
                os.write(body.toString().getBytes(StandardCharsets.UTF_8));
            }

            if (apiConn.getResponseCode() == 200) {
                String respStr = readStream(apiConn.getInputStream());
                JSONObject json = new JSONObject(respStr);
                JSONObject streamingData = json.optJSONObject("streamingData");
                if (streamingData != null) {
                    JSONArray formats = streamingData.optJSONArray("formats");
                    if (formats != null && formats.length() > 0) {
                        for (int i = 0; i < formats.length(); i++) {
                            JSONObject fmt = formats.getJSONObject(i);
                            String streamUrl = extractStreamUrl(fmt);
                            if (streamUrl != null && streamUrl.startsWith("http")) {
                                File file = downloadFile(streamUrl);
                                if (file != null && isValidVideoFile(file)) {
                                    return file;
                                }
                            }
                        }
                    }

                    JSONArray adaptive = streamingData.optJSONArray("adaptiveFormats");
                    if (adaptive != null && adaptive.length() > 0) {
                        File muxed = extractAndMuxAdaptive(adaptive);
                        if (muxed != null && isValidVideoFile(muxed)) {
                            return muxed;
                        }
                    }
                }
            }
        } catch (Exception e) {
            FileLog.e("TenseiVideoDownloader queryInnerTubeClient (" + clientName + ") error: " + e.getMessage());
        } finally {
            if (apiConn != null) {
                try { apiConn.disconnect(); } catch (Exception ignored) {}
            }
        }
        return null;
    }

    private static File downloadYouTubeInvidious(String videoId) {
        String[] invidiousInstances = {
                "https://invidious.f5.si",
                "https://inv.nadeko.net",
                "https://invidious.nerdvpn.de",
                "https://invidious.tiekoetter.com",
                "https://yt.chocolatemoo53.com",
                "https://invidious.jing.rocks",
                "https://iv.duti.dev",
                "https://invidious.private.coffee",
                "https://iv.melmac.space",
                "https://invidious.adminforge.de",
                "https://inv.tux.pizza",
                "https://invidious.protokolla.fi"
        };

        for (String instance : invidiousInstances) {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(instance + "/api/v1/videos/" + videoId);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(6000);
                conn.setReadTimeout(12000);
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36");
                conn.setRequestProperty("Accept", "application/json");

                if (conn.getResponseCode() == 200) {
                    String respStr = readStream(conn.getInputStream());
                    JSONObject json = new JSONObject(respStr);

                    JSONArray formatStreams = json.optJSONArray("formatStreams");
                    if (formatStreams != null && formatStreams.length() > 0) {
                        for (int i = 0; i < formatStreams.length(); i++) {
                            JSONObject fmt = formatStreams.getJSONObject(i);
                            String streamUrl = fmt.optString("url", null);
                            if (streamUrl != null && streamUrl.startsWith("http")) {
                                File file = downloadFile(streamUrl);
                                if (file != null && isValidVideoFile(file)) {
                                    return file;
                                }
                            }
                        }
                    }

                    JSONArray adaptive = json.optJSONArray("adaptiveFormats");
                    if (adaptive != null && adaptive.length() > 0) {
                        File muxed = extractAndMuxAdaptive(adaptive);
                        if (muxed != null && isValidVideoFile(muxed)) {
                            return muxed;
                        }
                    }
                }
            } catch (Exception e) {
                FileLog.e("TenseiVideoDownloader Invidious instance (" + instance + ") error: " + e.getMessage());
            } finally {
                if (conn != null) {
                    try { conn.disconnect(); } catch (Exception ignored) {}
                }
            }
        }
        return null;
    }

    private static File downloadYouTubePiped(String videoId) {
        String[] pipedInstances = {
                "https://api.piped.private.coffee",
                "https://pipedapi.kavin.rocks",
                "https://pipedapi.tokhmi.xyz",
                "https://pipedapi.adminforge.de",
                "https://pipedapi.reallyaweso.me",
                "https://pipedapi.leptons.xyz"
        };

        for (String instance : pipedInstances) {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(instance + "/streams/" + videoId);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(6000);
                conn.setReadTimeout(12000);
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
                conn.setRequestProperty("Accept", "application/json");

                if (conn.getResponseCode() == 200) {
                    String respStr = readStream(conn.getInputStream());
                    JSONObject json = new JSONObject(respStr);

                    JSONArray videoStreams = json.optJSONArray("videoStreams");
                    JSONArray audioStreams = json.optJSONArray("audioStreams");

                    if (videoStreams != null && videoStreams.length() > 0) {
                        for (int i = 0; i < videoStreams.length(); i++) {
                            JSONObject vs = videoStreams.getJSONObject(i);
                            boolean videoOnly = vs.optBoolean("videoOnly", true);
                            String streamUrl = vs.optString("url", null);
                            if (!videoOnly && streamUrl != null && streamUrl.startsWith("http")) {
                                File file = downloadFile(streamUrl);
                                if (file != null && isValidVideoFile(file)) {
                                    return file;
                                }
                            }
                        }

                        String bestVideoUrl = null;
                        for (int i = 0; i < videoStreams.length(); i++) {
                            JSONObject vs = videoStreams.getJSONObject(i);
                            String format = vs.optString("format", "");
                            String u = vs.optString("url", null);
                            if (u != null && u.startsWith("http") && ("MPEG_4".equalsIgnoreCase(format) || format.contains("MP4"))) {
                                bestVideoUrl = u;
                                break;
                            }
                        }
                        if (bestVideoUrl == null && videoStreams.length() > 0) {
                            bestVideoUrl = videoStreams.getJSONObject(0).optString("url", null);
                        }

                        String bestAudioUrl = null;
                        if (audioStreams != null && audioStreams.length() > 0) {
                            for (int i = 0; i < audioStreams.length(); i++) {
                                JSONObject as = audioStreams.getJSONObject(i);
                                String format = as.optString("format", "");
                                String u = as.optString("url", null);
                                if (u != null && u.startsWith("http") && ("M4A".equalsIgnoreCase(format) || format.contains("AAC") || format.contains("MP4"))) {
                                    bestAudioUrl = u;
                                    break;
                                }
                            }
                            if (bestAudioUrl == null && audioStreams.length() > 0) {
                                bestAudioUrl = audioStreams.getJSONObject(0).optString("url", null);
                            }
                        }

                        if (bestVideoUrl != null) {
                            File vFile = downloadFile(bestVideoUrl);
                            if (vFile != null) {
                                if (bestAudioUrl != null) {
                                    File aFile = downloadFile(bestAudioUrl);
                                    if (aFile != null) {
                                        File muxed = muxVideoAndAudio(vFile, aFile);
                                        vFile.delete();
                                        aFile.delete();
                                        if (muxed != null && muxed.exists() && muxed.length() > 10240) {
                                            return muxed;
                                        }
                                    }
                                }
                                return vFile;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                FileLog.e("TenseiVideoDownloader Piped instance (" + instance + ") error: " + e.getMessage());
            } finally {
                if (conn != null) {
                    try { conn.disconnect(); } catch (Exception ignored) {}
                }
            }
        }
        return null;
    }

    private static File extractAndMuxAdaptive(JSONArray adaptive) {
        try {
            String bestVideoUrl = null;
            String bestAudioUrl = null;

            for (int itagTarget : new int[]{136, 135, 134, 18}) {
                for (int i = 0; i < adaptive.length(); i++) {
                    JSONObject a = adaptive.getJSONObject(i);
                    if (a.optInt("itag") == itagTarget) {
                        String u = extractStreamUrl(a);
                        if (u != null && u.startsWith("http")) {
                            bestVideoUrl = u;
                            break;
                        }
                    }
                }
                if (bestVideoUrl != null) break;
            }

            if (bestVideoUrl == null) {
                for (int i = 0; i < adaptive.length(); i++) {
                    JSONObject a = adaptive.getJSONObject(i);
                    String mime = a.optString("mimeType", a.optString("type", ""));
                    String u = extractStreamUrl(a);
                    if (mime.startsWith("video/mp4") && u != null && u.startsWith("http")) {
                        bestVideoUrl = u;
                        break;
                    }
                }
            }

            for (int i = 0; i < adaptive.length(); i++) {
                JSONObject a = adaptive.getJSONObject(i);
                if (a.optInt("itag") == 140) {
                    String u = extractStreamUrl(a);
                    if (u != null && u.startsWith("http")) {
                        bestAudioUrl = u;
                        break;
                    }
                }
            }

            if (bestAudioUrl == null) {
                for (int i = 0; i < adaptive.length(); i++) {
                    JSONObject a = adaptive.getJSONObject(i);
                    String mime = a.optString("mimeType", a.optString("type", ""));
                    String u = extractStreamUrl(a);
                    if (mime.startsWith("audio/") && u != null && u.startsWith("http")) {
                        bestAudioUrl = u;
                        break;
                    }
                }
            }

            if (bestVideoUrl != null) {
                File vFile = downloadFile(bestVideoUrl);
                if (vFile != null) {
                    if (bestAudioUrl != null) {
                        File aFile = downloadFile(bestAudioUrl);
                        if (aFile != null) {
                            File muxed = muxVideoAndAudio(vFile, aFile);
                            vFile.delete();
                            aFile.delete();
                            if (muxed != null && muxed.exists() && muxed.length() > 10240) {
                                return muxed;
                            }
                        }
                    }
                    return vFile;
                }
            }
        } catch (Exception e) {
            FileLog.e("TenseiVideoDownloader extractAndMuxAdaptive error: " + e.getMessage());
        }
        return null;
    }

    private static String extractStreamUrl(JSONObject fmt) {
        if (fmt == null) return null;
        String streamUrl = fmt.optString("url", null);
        if (streamUrl != null && !streamUrl.isEmpty() && streamUrl.startsWith("http")) {
            return streamUrl;
        }
        String cipher = fmt.optString("signatureCipher", null);
        if (cipher == null || cipher.isEmpty()) {
            cipher = fmt.optString("cipher", null);
        }
        if (cipher != null && !cipher.isEmpty()) {
            try {
                String[] parts = cipher.split("&");
                String urlPart = null;
                String sigPart = null;
                String spPart = "sig";
                for (String p : parts) {
                    if (p.startsWith("url=")) {
                        urlPart = java.net.URLDecoder.decode(p.substring(4), "UTF-8");
                    } else if (p.startsWith("s=")) {
                        sigPart = java.net.URLDecoder.decode(p.substring(2), "UTF-8");
                    } else if (p.startsWith("sp=")) {
                        spPart = java.net.URLDecoder.decode(p.substring(3), "UTF-8");
                    }
                }
                if (urlPart != null && urlPart.startsWith("http")) {
                    if (sigPart != null && !sigPart.isEmpty()) {
                        return urlPart + "&" + spPart + "=" + sigPart;
                    }
                    return urlPart;
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    private static File muxVideoAndAudio(File videoFile, File audioFile) {
        if (videoFile == null || !videoFile.exists() || audioFile == null || !audioFile.exists()) {
            return null;
        }

        File dir = ApplicationLoader.applicationContext.getExternalCacheDir();
        if (dir == null) {
            dir = ApplicationLoader.applicationContext.getCacheDir();
        }
        File outputFile = new File(dir, "tensei_ytdl_muxed_" + System.currentTimeMillis() + ".mp4");

        MediaExtractor videoExtractor = new MediaExtractor();
        MediaExtractor audioExtractor = new MediaExtractor();
        MediaMuxer muxer = null;

        try {
            videoExtractor.setDataSource(videoFile.getAbsolutePath());
            audioExtractor.setDataSource(audioFile.getAbsolutePath());

            int videoTrackIndex = -1;
            for (int i = 0; i < videoExtractor.getTrackCount(); i++) {
                MediaFormat format = videoExtractor.getTrackFormat(i);
                String mime = format.getString(MediaFormat.KEY_MIME);
                if (mime != null && mime.startsWith("video/")) {
                    videoTrackIndex = i;
                    break;
                }
            }

            int audioTrackIndex = -1;
            for (int i = 0; i < audioExtractor.getTrackCount(); i++) {
                MediaFormat format = audioExtractor.getTrackFormat(i);
                String mime = format.getString(MediaFormat.KEY_MIME);
                if (mime != null && mime.startsWith("audio/")) {
                    audioTrackIndex = i;
                    break;
                }
            }

            if (videoTrackIndex == -1) {
                FileLog.e("TenseiVideoDownloader mux: No video track found");
                return null;
            }

            muxer = new MediaMuxer(outputFile.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            videoExtractor.selectTrack(videoTrackIndex);
            int muxerVideoTrack = muxer.addTrack(videoExtractor.getTrackFormat(videoTrackIndex));

            int muxerAudioTrack = -1;
            if (audioTrackIndex != -1) {
                audioExtractor.selectTrack(audioTrackIndex);
                muxerAudioTrack = muxer.addTrack(audioExtractor.getTrackFormat(audioTrackIndex));
            }

            muxer.start();

            ByteBuffer buffer = ByteBuffer.allocate(1024 * 1024);
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

            while (true) {
                bufferInfo.size = videoExtractor.readSampleData(buffer, 0);
                if (bufferInfo.size < 0) {
                    break;
                }
                bufferInfo.presentationTimeUs = videoExtractor.getSampleTime();
                bufferInfo.flags = videoExtractor.getSampleFlags();
                muxer.writeSampleData(muxerVideoTrack, buffer, bufferInfo);
                videoExtractor.advance();
            }

            if (muxerAudioTrack != -1) {
                while (true) {
                    bufferInfo.size = audioExtractor.readSampleData(buffer, 0);
                    if (bufferInfo.size < 0) {
                        break;
                    }
                    bufferInfo.presentationTimeUs = audioExtractor.getSampleTime();
                    bufferInfo.flags = audioExtractor.getSampleFlags();
                    muxer.writeSampleData(muxerAudioTrack, buffer, bufferInfo);
                    audioExtractor.advance();
                }
            }

            muxer.stop();
            return outputFile;
        } catch (Exception e) {
            FileLog.e("TenseiVideoDownloader mux error: " + e.getMessage());
            if (outputFile.exists()) {
                outputFile.delete();
            }
            return null;
        } finally {
            try { videoExtractor.release(); } catch (Exception ignored) {}
            try { audioExtractor.release(); } catch (Exception ignored) {}
            if (muxer != null) {
                try { muxer.release(); } catch (Exception ignored) {}
            }
        }
    }

    private static String resolveInstagramStream(String shortcode, String igUrl) {
        String[] embedUrls = {
                "https://www.instagram.com/p/" + shortcode + "/embed/captioned/",
                "https://www.instagram.com/reel/" + shortcode + "/embed/captioned/",
                "https://www.instagram.com/p/" + shortcode + "/embed/",
                "https://www.instagram.com/reel/" + shortcode + "/embed/"
        };

        for (String embedUrl : embedUrls) {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(embedUrl);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 16_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.6 Mobile/15E148 Safari/604.1");
                conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
                conn.setRequestProperty("Accept-Language", "en-US,en;q=0.9");
                conn.setInstanceFollowRedirects(true);

                if (conn.getResponseCode() == 200) {
                    String html = readStream(conn.getInputStream());
                    String stream = extractInstagramVideoFromHtml(html);
                    if (stream != null && !stream.isEmpty()) {
                        return stream;
                    }
                }
            } catch (Exception e) {
                FileLog.e("TenseiVideoDownloader Instagram embed error: " + e.getMessage());
            } finally {
                if (conn != null) {
                    try { conn.disconnect(); } catch (Exception ignored) {}
                }
            }
        }

        HttpURLConnection directConn = null;
        try {
            URL url = new URL("https://www.instagram.com/reel/" + shortcode + "/");
            directConn = (HttpURLConnection) url.openConnection();
            directConn.setRequestMethod("GET");
            directConn.setConnectTimeout(8000);
            directConn.setReadTimeout(8000);
            directConn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36");
            directConn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            directConn.setRequestProperty("Accept-Language", "en-US,en;q=0.9");
            directConn.setInstanceFollowRedirects(true);

            if (directConn.getResponseCode() == 200) {
                String html = readStream(directConn.getInputStream());
                String stream = extractInstagramVideoFromHtml(html);
                if (stream != null && !stream.isEmpty()) {
                    return stream;
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (directConn != null) {
                try { directConn.disconnect(); } catch (Exception ignored) {}
            }
        }

        return null;
    }

    private static String extractInstagramVideoFromHtml(String html) {
        if (html == null || html.isEmpty()) return null;

        String[] keys = {"video_url", "video_versions", "\"video\":", "contentUrl"};
        for (String key : keys) {
            int idx = html.indexOf(key);
            while (idx != -1) {
                int httpsIdx = html.indexOf("https", idx);
                if (httpsIdx != -1 && httpsIdx - idx < 120) {
                    int endIdx = httpsIdx;
                    while (endIdx < html.length()) {
                        char c = html.charAt(endIdx);
                        if (c == '"' || c == '\'' || c == '<' || c == '>' || c == ' ' || c == '\n' || c == '\r') {
                            break;
                        }
                        if (c == '\\') {
                            if (endIdx + 1 < html.length()) {
                                char next = html.charAt(endIdx + 1);
                                if (next == '"' || next == '\'') {
                                    break;
                                }
                            }
                        }
                        endIdx++;
                    }
                    String rawUrl = html.substring(httpsIdx, endIdx);
                    String cleanUrl = cleanEscapedUrl(rawUrl);
                    if (cleanUrl.contains(".mp4") || cleanUrl.contains("fbcdn.net") || cleanUrl.contains("cdninstagram")) {
                        return cleanUrl;
                    }
                }
                idx = html.indexOf(key, idx + key.length());
            }
        }

        int ogIdx = html.indexOf("og:video");
        if (ogIdx != -1) {
            int contentIdx = html.indexOf("content=\"", ogIdx);
            if (contentIdx != -1 && contentIdx - ogIdx < 50) {
                int start = contentIdx + 9;
                int end = html.indexOf("\"", start);
                if (end > start) {
                    String ogUrl = cleanEscapedUrl(html.substring(start, end));
                    if (ogUrl.startsWith("http")) return ogUrl;
                }
            }
        }

        int idx = 0;
        while (true) {
            int pos = html.indexOf("https", idx);
            if (pos == -1) break;
            int next600 = Math.min(html.length(), pos + 600);
            String snippet = html.substring(pos, next600);
            if (snippet.contains(".mp4") && (snippet.contains("fbcdn.net") || snippet.contains("cdninstagram"))) {
                int endIdx = pos;
                while (endIdx < html.length()) {
                    char c = html.charAt(endIdx);
                    if (c == '"' || c == '\'' || c == '<' || c == '>' || c == ' ' || c == '\n' || c == '\r') {
                        break;
                    }
                    if (c == '\\' && endIdx + 1 < html.length()) {
                        char next = html.charAt(endIdx + 1);
                        if (next == '"' || next == '\'') {
                            break;
                        }
                    }
                    endIdx++;
                }
                String cleanUrl = cleanEscapedUrl(html.substring(pos, endIdx));
                if (cleanUrl.contains(".mp4") && (cleanUrl.contains("fbcdn.net") || cleanUrl.contains("cdninstagram"))) {
                    return cleanUrl;
                }
            }
            idx = pos + 5;
        }

        return null;
    }

    private static String cleanEscapedUrl(String url) {
        if (url == null) return null;
        return url.replace("\\/", "/")
                .replace("\\\\/", "/")
                .replace("\\", "")
                .replace("&amp;", "&")
                .replace("\\u0026", "&");
    }

    private static String resolveTikTokStream(String tiktokUrl) {
        try {
            URL apiUrl = new URL(TenseiSecurity.getTikwmUrl());
            HttpURLConnection conn = (HttpURLConnection) apiUrl.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(6000);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android 14)");

            String postData = "url=" + URLEncoder.encode(tiktokUrl, "UTF-8") + "&count=1&cursor=0&web=1&hd=1";
            try (OutputStream os = conn.getOutputStream()) {
                os.write(postData.getBytes(StandardCharsets.UTF_8));
            }

            if (conn.getResponseCode() == 200) {
                String jsonStr = readStream(conn.getInputStream());
                JSONObject json = new JSONObject(jsonStr);
                JSONObject data = json.optJSONObject("data");
                if (data != null) {
                    String play = data.optString("play", null);
                    if (play != null && !play.isEmpty()) {
                        if (play.startsWith("/")) {
                            String base = TenseiSecurity.getTikwmUrl();
                            try {
                                java.net.URL u = new java.net.URL(base);
                                play = u.getProtocol() + "://" + u.getHost() + play;
                            } catch (Exception e) {
                                int slash = base.indexOf("/", 8);
                                String hostPart = slash > 0 ? base.substring(0, slash) : base;
                                play = hostPart + play;
                            }
                        }
                        if (play.startsWith("http")) {
                            return play;
                        }
                    }
                }
            }
            conn.disconnect();
        } catch (Exception ignored) {}
        return null;
    }

    private static File downloadFile(String fileUrl) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(fileUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(180000);
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 16_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.6 Mobile/15E148 Safari/604.1");
            conn.setRequestProperty("Accept", "*/*");

            int code = conn.getResponseCode();
            int redirectCount = 0;
            while ((code == HttpURLConnection.HTTP_MOVED_PERM
                    || code == HttpURLConnection.HTTP_MOVED_TEMP
                    || code == 307 || code == 308)
                    && redirectCount < 5) {
                String redirectUrl = conn.getHeaderField("Location");
                if (redirectUrl == null) break;
                conn.disconnect();
                url = new URL(redirectUrl);
                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(180000);
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 16_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.6 Mobile/15E148 Safari/604.1");
                code = conn.getResponseCode();
                redirectCount++;
            }

            if (code != 200 && code != 206) {
                FileLog.e("TenseiVideoDownloader: HTTP " + code + " for " + fileUrl);
                return null;
            }

            String contentType = conn.getContentType();
            if (contentType != null) {
                String lower = contentType.toLowerCase();
                if (lower.contains("text/html") || lower.contains("application/json") || lower.contains("text/plain")) {
                    FileLog.e("TenseiVideoDownloader: Server returned non-video content type: " + contentType);
                    return null;
                }
            }

            File dir = ApplicationLoader.applicationContext.getExternalCacheDir();
            if (dir == null) {
                dir = ApplicationLoader.applicationContext.getCacheDir();
            }
            File outFile = new File(dir, "tensei_ytdl_" + System.currentTimeMillis() + ".mp4");

            try (InputStream in = new BufferedInputStream(conn.getInputStream(), 32768);
                 FileOutputStream out = new FileOutputStream(outFile)) {
                byte[] buffer = new byte[32768];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                out.flush();
            }

            if (outFile.length() < 10240) {
                outFile.delete();
                return null;
            }

            return outFile;
        } catch (Exception e) {
            FileLog.e("TenseiVideoDownloader downloadFile error: " + e.getMessage());
            return null;
        } finally {
            if (conn != null) {
                try {
                    conn.disconnect();
                } catch (Exception ignored) {}
            }
        }
    }

    private static boolean isValidVideoFile(File file) {
        if (file == null || !file.exists() || file.length() < 10240) {
            return false;
        }
        try (InputStream is = new BufferedInputStream(new FileInputStream(file))) {
            byte[] header = new byte[32];
            int read = is.read(header);
            if (read < 12) return false;

            if (header[4] == 'f' && header[5] == 't' && header[6] == 'y' && header[7] == 'p') {
                return true;
            }
            if ((header[0] & 0xFF) == 0x1A && (header[1] & 0xFF) == 0x45
                    && (header[2] & 0xFF) == 0xDF && (header[3] & 0xFF) == 0xA3) {
                return true;
            }
            if (header[0] == 'F' && header[1] == 'L' && header[2] == 'V') {
                return true;
            }
            if (header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                    && header[8] == 'A' && header[9] == 'V' && header[10] == 'I') {
                return true;
            }
            if ((header[0] & 0xFF) == 0x47) {
                return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    private static String readStream(InputStream in) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(in, StandardCharsets.UTF_8), 8192)) {
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[8192];
            int read;
            while ((read = reader.read(buf)) != -1) {
                sb.append(buf, 0, read);
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
