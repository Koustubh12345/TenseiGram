package org.telegram.messenger;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class TenseiAI {

    public interface Callback {
        void onResult(String text);
        void onError();
    }

    private static final String KILO_URL = TenseiSecurity.getKiloUrl();
    private static final String[] FREE_MODELS = new String[]{
            TenseiSecurity.getAiModel(0),
            TenseiSecurity.getAiModel(1),
            TenseiSecurity.getAiModel(2)
    };
    private static final String POLL_POST_URL = "https://text.pollinations.ai/openai";
    private static final String POLL_GET_BASE = "https://text.pollinations.ai/";
    private static final String[] POLL_GET_MODELS = new String[]{
            "openai"
    };
    private static final String[] DUCK_HOSTS = new String[]{
            "https://duck.ai",
            "https://duckduckgo.com"
    };
    private static final String[] DUCK_MODELS = new String[]{
            "gpt-4o-mini",
            "claude-3-haiku-20240307"
    };

    private static boolean looksBroken(String s) {
        if (s == null) {
            return true;
        }
        String t = s.trim();
        if (t.isEmpty()) {
            return true;
        }
        String low = t.toLowerCase(java.util.Locale.ENGLISH);
        if (t.length() < 400 && (low.contains("rate limit") || low.contains("rate-limit") || low.contains("too many requests") || low.contains("try again later") || low.contains("temporarily unavailable") || low.contains("service unavailable") || low.contains("overloaded"))) {
            return true;
        }
        if (t.startsWith("{") && t.endsWith("}") && low.contains("\"error\"")) {
            return true;
        }
        return false;
    }

    public static void ask(String prompt, Callback callback) {
        new Thread(() -> {
            // Overall deadline so the UI can never hang silently: ~45s max, then onError.
            final long start = System.currentTimeMillis();
            final long deadline = start + 45000L;
            String result = null;
            try {
                // Fast path first: Pollinations GET plain (proven working, no 402).
                try {
                    result = pollinationsAsk(prompt, "openai");
                } catch (Throwable ignored) {
                }
                if (looksBroken(result) && System.currentTimeMillis() < deadline) {
                    for (String model : FREE_MODELS) {
                        if (!looksBroken(result) || System.currentTimeMillis() >= deadline) {
                            break;
                        }
                        try {
                            result = chatCompletions(KILO_URL, model, prompt, false);
                        } catch (Throwable ignored) {
                        }
                        sleepQuiet(250L);
                    }
                }
                if (looksBroken(result) && System.currentTimeMillis() < deadline) {
                    try {
                        result = chatCompletions(POLL_POST_URL, "openai", prompt, false);
                    } catch (Throwable ignored) {
                    }
                }
                if (looksBroken(result) && System.currentTimeMillis() < deadline) {
                    for (String pm : POLL_GET_MODELS) {
                        if (!looksBroken(result) || System.currentTimeMillis() >= deadline) {
                            break;
                        }
                        sleepQuiet(250L);
                        try {
                            result = pollinationsAsk(prompt, pm);
                        } catch (Throwable ignored) {
                        }
                    }
                }
                if (looksBroken(result) && System.currentTimeMillis() < deadline) {
                    for (String model : DUCK_MODELS) {
                        if (!looksBroken(result) || System.currentTimeMillis() >= deadline) {
                            break;
                        }
                        sleepQuiet(250L);
                        try {
                            result = duckAsk(model, prompt);
                        } catch (Throwable ignored) {
                        }
                    }
                }
                // Last-chance retry only if time remains (no long sleep).
                if (looksBroken(result) && deadline - System.currentTimeMillis() > 5000L) {
                    sleepQuiet(800L);
                    try {
                        result = pollinationsAsk(prompt, "openai");
                    } catch (Throwable ignored) {
                    }
                }
                result = stripBranding(clean(result));
            } catch (Throwable t) {
                result = null;
            }
            final String out = result;
            AndroidUtilities.runOnUIThread(() -> {
                try {
                    if (out != null && !out.trim().isEmpty()) {
                        callback.onResult(out.trim());
                    } else {
                        callback.onError();
                    }
                } catch (Throwable t) {
                    try {
                        callback.onError();
                    } catch (Throwable ignored) {
                    }
                }
            });
        }, "tensei-ai").start();
    }

    private static JSONObject systemMessage() {
        JSONObject sys = new JSONObject();
        try {
            java.text.SimpleDateFormat fmt =
                    new java.text.SimpleDateFormat("EEEE, MMMM d, yyyy", java.util.Locale.ENGLISH);
            String today = fmt.format(new java.util.Date());
            sys.put("role", "system");
            sys.put("content", "You are a helpful assistant inside the TenseiGram app. "
                    + "Today is " + today + ". "
                    + "Answer concisely for chat. Use **bold** for key terms sparingly, no # headers.");
        } catch (Exception e) {
            try {
                sys.put("role", "system");
                sys.put("content", "You are a helpful assistant. Answer concisely for chat.");
            } catch (Exception ignored) {
            }
        }
        return sys;
    }

    private static void sleepQuiet(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
        }
    }

    private static String chatCompletions(String endpoint, String model, String prompt, boolean priv) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(endpoint);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("User-Agent", "TenseiGram/1.0");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(30000);
            conn.setDoOutput(true);

            JSONObject message = new JSONObject();
            message.put("role", "user");
            message.put("content", prompt);
            JSONArray messages = new JSONArray();
            messages.put(systemMessage());
            messages.put(message);
            JSONObject body = new JSONObject();
            body.put("model", model);
            body.put("messages", messages);
            body.put("referrer", "TenseiGram");
            if (priv) {
                body.put("private", true);
            }

            byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
            conn.setFixedLengthStreamingMode(bytes.length);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(bytes);
            }

            if (conn.getResponseCode() != 200) {
                return null;
            }
            String response = readAll(conn.getInputStream());
            if (response == null || response.isEmpty()) {
                return null;
            }
            JSONObject json = new JSONObject(response);
            if (json.has("error")) {
                return null;
            }
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
            return content;
        } catch (Exception e) {
            return null;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static String clean(String s) {
        if (s == null) {
            return null;
        }
        try {
            s = s.replaceAll("(?s)<think>.*?</think>", "").trim();
        } catch (Throwable ignored) {
        }
        return s;
    }

    private static String stripBranding(String s) {
        if (s == null) {
            return null;
        }
        try {
            String[] lines = s.split("\n");
            StringBuilder sb = new StringBuilder();
            for (String ln : lines) {
                String t = ln.trim();
                String low = t.toLowerCase(java.util.Locale.ENGLISH);
                if (low.contains("pollinat") || low.contains("kofi") || low.contains("support our mission") || low.contains("powered by") || low.contains("keep ai accessible")) {
                    continue;
                }
                if (t.indexOf(0x1F338) >= 0) {
                    continue;
                }
                if (t.equals("---") || t.equals("***") || t.equals("___") || (t.length() == 1 && t.charAt(0) == 0x25BC)) {
                    continue;
                }
                sb.append(ln).append("\n");
            }
            String o = sb.toString();
            o = o.replaceAll("[\\x{25BC}\\x{1F338}]", "");
            o = o.replaceAll("(?i)\\[support our mission\\]\\([^\\)]*\\)", "");
            o = o.replaceAll("(?i)https?://pollinations\\.ai[^\\s\\)]*", "");
            o = o.replaceAll("\n{3,}", "\n\n").trim();
            o = o.replaceAll("(?m)^---\\s*$", "").trim();
            return o;
        } catch (Throwable ignored) {
            return s;
        }
    }

    private static String headerIgnoreCase(HttpURLConnection conn, String name) {
        try {
            java.util.Map<String, java.util.List<String>> map = conn.getHeaderFields();
            if (map != null) {
                for (java.util.Map.Entry<String, java.util.List<String>> e : map.entrySet()) {
                    if (e.getKey() != null && e.getKey().equalsIgnoreCase(name)) {
                        java.util.List<String> v = e.getValue();
                        if (v != null && !v.isEmpty()) {
                            return v.get(0);
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static String duckToken(String host) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(host + "/duckchat/v1/status");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("x-vqd-accept", "1");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setRequestProperty("Origin", "https://duck.ai");
            conn.setRequestProperty("Referer", "https://duck.ai/");
            conn.setRequestProperty("Accept", "*/*");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);
            conn.connect();
            int code = conn.getResponseCode();
            if (code != 200 && code != 202) {
                return null;
            }
            // Duck rotated header names: x-vqd-4 (old) -> x-vqd-hash-1 (new). Accept either.
            String tok = headerIgnoreCase(conn, "x-vqd-4");
            if (tok == null || tok.isEmpty()) {
                tok = headerIgnoreCase(conn, "x-vqd-hash-1");
            }
            if (tok == null || tok.isEmpty()) {
                tok = headerIgnoreCase(conn, "x-vqd-hash");
            }
            return tok;
        } catch (Exception e) {
            return null;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static String duckAsk(String model, String prompt) {
        for (String host : DUCK_HOSTS) {
            String out = duckChatOnce(host, model, prompt);
            if (out == null) {
                sleepQuiet(800L);
                out = duckChatOnce(host, model, prompt);
            }
            if (out != null && !out.isEmpty()) {
                return out;
            }
        }
        return null;
    }

    private static String duckChatOnce(String host, String model, String prompt) {
        HttpURLConnection conn = null;
        try {
            String token = duckToken(host);
            if (token == null || token.isEmpty()) {
                return null;
            }
            URL url = new URL(host + "/duckchat/v1/chat");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "text/event-stream");
            conn.setRequestProperty("x-vqd-4", token);
            conn.setRequestProperty("x-vqd-hash-1", token);
            conn.setRequestProperty("Origin", "https://duck.ai");
            conn.setRequestProperty("Referer", "https://duck.ai/");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(30000);
            conn.setDoOutput(true);
            JSONObject message = new JSONObject();
            message.put("role", "user");
            message.put("content", prompt);
            JSONArray messages = new JSONArray();
            messages.put(message);
            JSONObject body = new JSONObject();
            body.put("model", model);
            body.put("messages", messages);
            byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
            conn.setFixedLengthStreamingMode(bytes.length);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(bytes);
            }
            if (conn.getResponseCode() != 200) {
                return null;
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("data: ")) {
                    continue;
                }
                String data = line.substring(6).trim();
                if (data.equals("[DONE]") || data.isEmpty()) {
                    continue;
                }
                try {
                    JSONObject chunk = new JSONObject(data);
                    String piece = chunk.optString("message", null);
                    if (piece != null && !piece.isEmpty()) {
                        sb.append(piece);
                    }
                } catch (Throwable ignored) {
                }
                if (sb.length() > 8000) {
                    break;
                }
            }
            String out = sb.toString().trim();
            if (looksBroken(out)) {
                return null;
            }
            return out;
        } catch (Exception e) {
            return null;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static String pollinationsAsk(String prompt, String model) {
        // Pollinations paywalls some combos (e.g. encoded '?' + ?model=openai => 402,
        // and &system=... => 402). Plain GET with no model is most reliable (200 even with '?').
        // Try: 1) plain (no model)  2) ?model=openai  3) ?-stripped + ?model=openai
        String encPrompt;
        String encModel;
        try {
            encPrompt = URLEncoder.encode(prompt, "UTF-8").replace("+", "%20");
            encModel = URLEncoder.encode(model, "UTF-8");
        } catch (Exception e) {
            return null;
        }
        String promptNoQ = prompt.replace("?", "").trim();
        String encPromptNoQ;
        try {
            encPromptNoQ = URLEncoder.encode(promptNoQ.isEmpty() ? prompt : promptNoQ, "UTF-8").replace("+", "%20");
        } catch (Exception e) {
            encPromptNoQ = encPrompt;
        }
        String[] urlVariants = new String[]{
                POLL_GET_BASE + encPrompt,
                POLL_GET_BASE + encPrompt + "?model=" + encModel,
                POLL_GET_BASE + encPromptNoQ + "?model=" + encModel
        };
        for (String urlStr : urlVariants) {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(urlStr);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "text/plain");
                conn.setRequestProperty("User-Agent", "Mozilla/5.0");
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(20000);
                if (conn.getResponseCode() != 200) {
                    continue;
                }
                String response = readAll(conn.getInputStream());
                if (looksBroken(response)) {
                    continue;
                }
                return response.trim();
            } catch (Exception e) {
                continue;
            } finally {
                if (conn != null) {
                    try {
                        conn.disconnect();
                    } catch (Throwable ignored) {
                    }
                }
            }
        }
        return null;
    }

    private static String readAll(InputStream in) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        char[] buf = new char[4096];
        int n;
        while ((n = reader.read(buf)) != -1) {
            sb.append(buf, 0, n);
            if (sb.length() > 8000) {
                break;
            }
        }
        return sb.toString();
    }
}
