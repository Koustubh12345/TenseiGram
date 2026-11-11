package org.telegram.messenger;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.tgnet.TLRPC;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class TenseiWebSearch {

    public interface Callback {
        void onResult(String text, ArrayList<TLRPC.MessageEntity> entities);
        void onError();
    }

    private static class SearchResult {
        final String text;
        final String source;

        SearchResult(String text, String source) {
            this.text = text;
            this.source = source;
        }
    }

    public static void search(String query, Callback callback) {
        new Thread(() -> {
            String q = query != null ? query.trim() : "";
            if (q.isEmpty()) {
                AndroidUtilities.runOnUIThread(callback::onError);
                return;
            }

            SearchResult result = executeMultiEngineSearch(q);

            if (result == null || result.text == null || result.text.trim().isEmpty()) {
                AndroidUtilities.runOnUIThread(callback::onError);
                return;
            }

            ArrayList<TLRPC.MessageEntity> entities = new ArrayList<>();
            StringBuilder sb = new StringBuilder();

            TLRPC.TL_messageEntityBold qb = new TLRPC.TL_messageEntityBold();
            qb.offset = 0;
            qb.length = q.length();
            entities.add(qb);
            sb.append(q).append("\n\n");

            sb.append(result.text.trim());

            if (result.source != null && !result.source.isEmpty()) {
                sb.append("\n\n");
                int srcStart = sb.length();
                sb.append("Source: ").append(result.source);
                TLRPC.TL_messageEntityItalic it = new TLRPC.TL_messageEntityItalic();
                it.offset = srcStart;
                it.length = sb.length() - srcStart;
                entities.add(it);
            }

            final String outText = sb.toString();
            final ArrayList<TLRPC.MessageEntity> outEntities = entities;

            AndroidUtilities.runOnUIThread(() -> callback.onResult(outText, outEntities));
        }, "tensei-search").start();
    }

    private static SearchResult executeMultiEngineSearch(String query) {
        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            List<Callable<SearchResult>> tasks = new ArrayList<>();

            tasks.add(() -> engineWikipedia(query));

            tasks.add(() -> engineDuckDuckGo(query));

            tasks.add(() -> engineSearXNG(query));

            tasks.add(() -> engineGoogle(query));

            List<Future<SearchResult>> futures = pool.invokeAll(tasks, 2800, TimeUnit.MILLISECONDS);
            for (Future<SearchResult> f : futures) {
                try {
                    SearchResult sr = f.get();
                    if (sr != null && sr.text != null && sr.text.length() > 30) {
                        return sr;
                    }
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable ignored) {
        } finally {
            pool.shutdownNow();
        }

        SearchResult r = engineBrave(query);
        if (r != null) return r;

        r = engineBing(query);
        if (r != null) return r;

        r = engineQwant(query);
        if (r != null) return r;

        r = engineYahoo(query);
        if (r != null) return r;

        r = engineStartpage(query);
        if (r != null) return r;

        r = engineMojeek(query);
        if (r != null) return r;

        return null;
    }

    private static SearchResult engineWikipedia(String query) {
        try {
            String encoded = URLEncoder.encode(query, "UTF-8").replace("+", "%20");
            String json = httpGet("https://en.wikipedia.org/api/rest_v1/page/summary/" + encoded, 2500);
            if (json != null) {
                JSONObject obj = new JSONObject(json);
                String extract = obj.optString("extract");
                if (extract != null && extract.length() > 25) {
                    return new SearchResult(extract, "Wikipedia");
                }
            }
            String osJson = httpGet("https://en.wikipedia.org/w/api.php?action=opensearch&search=" + encoded + "&limit=1&format=json", 2500);
            if (osJson != null) {
                JSONArray arr = new JSONArray(osJson);
                if (arr.length() >= 3) {
                    JSONArray desc = arr.optJSONArray(2);
                    if (desc != null && desc.length() > 0) {
                        String txt = desc.optString(0);
                        if (txt != null && txt.length() > 25) {
                            return new SearchResult(txt, "Wikipedia");
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static SearchResult engineDuckDuckGo(String query) {
        try {
            String encoded = URLEncoder.encode(query, "UTF-8").replace("+", "%20");
            String json = httpGet("https://api.duckduckgo.com/?q=" + encoded + "&format=json&no_html=1&skip_disambig=1", 2500);
            if (json != null) {
                JSONObject obj = new JSONObject(json);
                String answer = obj.optString("Answer");
                if (answer != null && !answer.trim().isEmpty()) {
                    return new SearchResult(answer.trim(), "DuckDuckGo Instant");
                }
                String abstractText = obj.optString("AbstractText");
                if (abstractText != null && abstractText.length() > 25) {
                    String src = obj.optString("AbstractSource");
                    return new SearchResult(abstractText.trim(), (src != null && !src.isEmpty()) ? src : "DuckDuckGo");
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static SearchResult engineSearXNG(String query) {
        String[] nodes = new String[]{"https://searx.be", "https://priv.au", "https://search.ononoki.org"};
        for (String node : nodes) {
            try {
                String encoded = URLEncoder.encode(query, "UTF-8");
                String json = httpGet(node + "/search?q=" + encoded + "&format=json&categories=general", 2500);
                if (json != null) {
                    JSONObject obj = new JSONObject(json);
                    JSONArray results = obj.optJSONArray("results");
                    if (results != null && results.length() > 0) {
                        for (int i = 0; i < Math.min(results.length(), 2); i++) {
                            JSONObject item = results.getJSONObject(i);
                            String content = item.optString("content");
                            if (content != null && content.length() > 30) {
                                String engine = item.optString("engine", "SearXNG");
                                return new SearchResult(cleanHtml(content), "SearXNG (" + engine + ")");
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static SearchResult engineGoogle(String query) {
        try {
            String encoded = URLEncoder.encode(query, "UTF-8");
            String json = httpGet("https://suggestqueries.google.com/complete/search?client=firefox&q=" + encoded, 2000);
            if (json != null) {
                JSONArray arr = new JSONArray(json);
                if (arr.length() >= 2) {
                    JSONArray suggestions = arr.optJSONArray(1);
                    if (suggestions != null && suggestions.length() > 0) {
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < Math.min(suggestions.length(), 4); i++) {
                            sb.append("- ").append(suggestions.optString(i)).append("\n");
                        }
                        if (sb.length() > 10) {
                            return new SearchResult(sb.toString().trim(), "Google Instant");
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static SearchResult engineBrave(String query) {
        try {
            String encoded = URLEncoder.encode(query, "UTF-8");
            String json = httpGet("https://search.brave.com/api/suggest?q=" + encoded, 2000);
            if (json != null) {
                JSONArray arr = new JSONArray(json);
                if (arr.length() >= 2) {
                    JSONArray suggs = arr.optJSONArray(1);
                    if (suggs != null && suggs.length() > 0) {
                        return new SearchResult("Result: " + suggs.optString(0), "Brave Search");
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static SearchResult engineBing(String query) {
        try {
            String encoded = URLEncoder.encode(query, "UTF-8");
            String json = httpGet("https://api.bing.com/osjson.aspx?query=" + encoded, 2000);
            if (json != null) {
                JSONArray arr = new JSONArray(json);
                if (arr.length() >= 2) {
                    JSONArray suggs = arr.optJSONArray(1);
                    if (suggs != null && suggs.length() > 0) {
                        return new SearchResult("Result: " + suggs.optString(0), "Bing");
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static SearchResult engineQwant(String query) {
        try {
            String encoded = URLEncoder.encode(query, "UTF-8");
            String json = httpGet("https://api.qwant.com/v3/suggest?q=" + encoded + "&client=opensearch", 2000);
            if (json != null) {
                JSONArray arr = new JSONArray(json);
                if (arr.length() >= 2) {
                    JSONArray suggs = arr.optJSONArray(1);
                    if (suggs != null && suggs.length() > 0) {
                        return new SearchResult("Result: " + suggs.optString(0), "Qwant");
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static SearchResult engineYahoo(String query) {
        try {
            String encoded = URLEncoder.encode(query, "UTF-8");
            String json = httpGet("https://search.yahoo.com/sugg/gossip/gossip-us-ura/?output=sd1&command=" + encoded, 2000);
            if (json != null && json.contains("k")) {
                JSONObject obj = new JSONObject(json);
                JSONArray r = obj.optJSONArray("r");
                if (r != null && r.length() > 0) {
                    JSONObject first = r.getJSONObject(0);
                    String k = first.optString("k");
                    if (k != null && !k.isEmpty()) {
                        return new SearchResult("Result: " + k, "Yahoo");
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static SearchResult engineStartpage(String query) {
        try {
            String encoded = URLEncoder.encode(query, "UTF-8");
            String json = httpGet("https://www.startpage.com/suggestions?q=" + encoded, 2000);
            if (json != null) {
                JSONArray arr = new JSONArray(json);
                if (arr.length() > 1) {
                    JSONArray list = arr.optJSONArray(1);
                    if (list != null && list.length() > 0) {
                        return new SearchResult("Result: " + list.optString(0), "Startpage");
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static SearchResult engineMojeek(String query) {
        try {
            String encoded = URLEncoder.encode(query, "UTF-8");
            String html = httpGet("https://www.mojeek.com/search?q=" + encoded, 2000);
            if (html != null && html.contains("<p class=\"s\">")) {
                int start = html.indexOf("<p class=\"s\">") + 13;
                int end = html.indexOf("</p>", start);
                if (end > start) {
                    String snippet = cleanHtml(html.substring(start, end));
                    if (snippet.length() > 25) {
                        return new SearchResult(snippet, "Mojeek");
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static String httpGet(String urlStr, int timeoutMs) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(timeoutMs);
            conn.setReadTimeout(timeoutMs);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36");
            conn.setRequestProperty("Accept", "*/*");
            int code = conn.getResponseCode();
            if (code == 200) {
                InputStream in = conn.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                char[] buf = new char[2048];
                int n;
                while ((n = reader.read(buf)) != -1) {
                    sb.append(buf, 0, n);
                    if (sb.length() > 100000) break;
                }
                return sb.toString();
            }
        } catch (Throwable ignored) {
        } finally {
            if (conn != null) {
                try {
                    conn.disconnect();
                } catch (Throwable ignored) {
                }
            }
        }
        return null;
    }

    private static String cleanHtml(String text) {
        if (text == null) return "";
        return text.replaceAll("<[^>]*>", "")
                .replace("&quot;", "\"")
                .replace("&amp;", "&")
                .replace("&#39;", "'")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .trim();
    }
}
