package org.telegram.messenger;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TenseiGrammarHelper {

    private static class ReplacementMatch {
        int offset;
        int length;
        String replacement;

        ReplacementMatch(int offset, int length, String replacement) {
            this.offset = offset;
            this.length = length;
            this.replacement = replacement;
        }
    }

    public static CharSequence correct(CharSequence input) {
        if (input == null || input.length() == 0) {
            return input;
        }
        String text = input.toString().trim();
        if (text.startsWith("/") || text.startsWith("http://") || text.startsWith("https://") || text.length() > 2000) {
            return input;
        }

        String aiResult = correctWithKiloAi(text);
        if (aiResult != null && !aiResult.isEmpty() && !aiResult.equalsIgnoreCase(text)) {
            return aiResult;
        }

        String ltResult = correctWithLanguageTool(text);
        if (ltResult != null && !ltResult.isEmpty()) {
            return ltResult;
        }

        return input;
    }

    private static String correctWithKiloAi(String text) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(TenseiSecurity.getKiloUrl());
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("User-Agent", "TenseiGram/1.0");
            conn.setConnectTimeout(3500);
            conn.setReadTimeout(4500);
            conn.setDoOutput(true);

            String prompt = "Fix all grammar, spelling, punctuation, capitalization, and phrasing in the following text. Output ONLY the corrected text without any explanation, markdown formatting, or notes: " + text;

            JSONObject message = new JSONObject();
            message.put("role", "user");
            message.put("content", prompt);
            JSONArray messages = new JSONArray();
            messages.put(message);

            JSONObject body = new JSONObject();
            body.put("model", TenseiSecurity.getAiModel(1));
            body.put("messages", messages);

            byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
            conn.setFixedLengthStreamingMode(bytes.length);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(bytes);
            }

            if (conn.getResponseCode() != 200) {
                return null;
            }

            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }

            JSONObject json = new JSONObject(sb.toString());
            JSONArray choices = json.optJSONArray("choices");
            if (choices == null || choices.length() == 0) {
                return null;
            }
            JSONObject msg = choices.getJSONObject(0).optJSONObject("message");
            if (msg == null) {
                return null;
            }
            String content = msg.optString("content", null);
            if (content == null || content.trim().isEmpty()) {
                return null;
            }

            String res = content.replaceAll("(?s)<think>.*?</think>", "").trim();
            if ((res.startsWith("\"") && res.endsWith("\"")) || (res.startsWith("'") && res.endsWith("'"))) {
                res = res.substring(1, res.length() - 1).trim();
            }
            if (!res.isEmpty()) {
                return res;
            }
        } catch (Exception e) {
            FileLog.e("TenseiGrammarHelper Kilo error: " + e.getMessage());
        } finally {
            if (conn != null) {
                try {
                    conn.disconnect();
                } catch (Exception ignored) {
                }
            }
        }
        return null;
    }

    private static String correctWithLanguageTool(String text) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(TenseiSecurity.getLanguageToolUrl());
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(2500);
            conn.setReadTimeout(2500);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("User-Agent", "TenseiGram-Client/13.2.1 (Android)");

            String postData = "text=" + URLEncoder.encode(text, "UTF-8") + "&language=auto";
            byte[] postBytes = postData.getBytes(StandardCharsets.UTF_8);
            conn.setFixedLengthStreamingMode(postBytes.length);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(postBytes);
                os.flush();
            }

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                return null;
            }

            StringBuilder sbResponse = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sbResponse.append(line);
                }
            }

            JSONObject json = new JSONObject(sbResponse.toString());
            JSONArray matches = json.optJSONArray("matches");
            if (matches == null || matches.length() == 0) {
                return null;
            }

            List<ReplacementMatch> list = new ArrayList<>();
            for (int i = 0; i < matches.length(); i++) {
                JSONObject m = matches.getJSONObject(i);
                int offset = m.optInt("offset", -1);
                int length = m.optInt("length", -1);
                JSONArray reps = m.optJSONArray("replacements");
                if (offset >= 0 && length > 0 && reps != null && reps.length() > 0) {
                    String repVal = reps.getJSONObject(0).optString("value", null);
                    if (repVal != null) {
                        list.add(new ReplacementMatch(offset, length, repVal));
                    }
                }
            }

            if (list.isEmpty()) {
                return null;
            }

            Collections.sort(list, (a, b) -> Integer.compare(b.offset, a.offset));

            StringBuilder corrected = new StringBuilder(text);
            int lastOffset = Integer.MAX_VALUE;
            for (ReplacementMatch match : list) {
                if (match.offset + match.length <= lastOffset && match.offset + match.length <= corrected.length()) {
                    corrected.replace(match.offset, match.offset + match.length, match.replacement);
                    lastOffset = match.offset;
                }
            }

            return corrected.toString();
        } catch (Exception e) {
            FileLog.e("TenseiGrammarHelper LT error: " + e.getMessage());
            return null;
        } finally {
            if (conn != null) {
                try {
                    conn.disconnect();
                } catch (Exception ignored) {
                }
            }
        }
    }
}
