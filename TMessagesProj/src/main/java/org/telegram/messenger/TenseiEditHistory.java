package org.telegram.messenger;

import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TenseiEditHistory {
    private static final ConcurrentHashMap<String, ArrayList<String>> a = new ConcurrentHashMap<>();
    private static boolean loaded = false;
    private static String k(long d, int m) {
        return d + "_" + m;
    }
    private static SharedPreferences p() {
        try {
            return ApplicationLoader.applicationContext.getSharedPreferences("tensei_edits", 0);
        } catch (Throwable ignore) { return null; }
    }
    private static void ensure() {
        if (loaded) return;
        loaded = true;
        try {
            SharedPreferences sp = p();
            if (sp == null) return;
            Map<String, ?> all = sp.getAll();
            for (Map.Entry<String, ?> e : all.entrySet()) {
                try {
                    String v = (String) e.getValue();
                    if (v == null) continue;
                    String[] parts = v.split("", -1);
                    ArrayList<String> l = new ArrayList<>();
                    for (int i = 0; i < parts.length && i < 8; i++) l.add(parts[i]);
                    if (!l.isEmpty()) a.put(e.getKey(), l);
                    if (a.size() > 400) break;
                } catch (Throwable ignore) {}
            }
        } catch (Throwable ignore) {}
    }
    private static void persist(String key) {
        try {
            SharedPreferences sp = p();
            if (sp == null) return;
            ArrayList<String> l = a.get(key);
            if (l == null) return;
            StringBuilder sb = new StringBuilder();
            synchronized (l) {
                for (int i = 0; i < l.size(); i++) {
                    if (i > 0) sb.append("");
                    String s = l.get(i);
                    if (s == null) s = "";
                    if (s.length() > 2000) s = s.substring(0, 2000);
                    sb.append(s.replace("", " "));
                }
            }
            sp.edit().putString(key, sb.toString()).apply();
        } catch (Throwable ignore) {}
    }
    public static void record(int account, long dialogId, int mid, String oldText, String newText) {
        try {
            ensure();
            if (oldText == null) oldText = "";
            if (newText == null) newText = "";
            if (oldText.equals(newText)) {
                track(account, dialogId, mid, newText);
                return;
            }
            String key = k(dialogId, mid);
            ArrayList<String> l = a.get(key);
            if (l == null) {
                l = new ArrayList<>();
                a.put(key, l);
            }
            synchronized (l) {
                if (l.isEmpty()) {
                    if (!oldText.isEmpty()) l.add(oldText);
                } else if (!l.get(l.size() - 1).equals(oldText) && !oldText.isEmpty()) {
                    l.add(oldText);
                }
                while (l.size() > 8) l.remove(0);
            }
            persist(key);
        } catch (Throwable ignore) {}
    }
    public static void track(int account, long dialogId, int mid, String cur) {
        try {
            ensure();
            if (cur == null) cur = "";
            if (cur.isEmpty()) return;
            String key = k(dialogId, mid);
            if (!a.containsKey(key)) {
                ArrayList<String> l = new ArrayList<>();
                l.add(cur);
                a.put(key, l);
                persist(key);
            }
        } catch (Throwable ignore) {}
    }
    public static List<String> get(long dialogId, int mid, String current) {
        try {
            ensure();
            String key = k(dialogId, mid);
            ArrayList<String> l = a.get(key);
            if (current == null) current = "";
            if (l == null || l.isEmpty()) {
                if (!current.isEmpty()) {
                    ArrayList<String> n = new ArrayList<>();
                    n.add(current);
                    a.put(key, n);
                }
                return Collections.singletonList(current);
            }
            synchronized (l) {
                ArrayList<String> o = new ArrayList<>(l);
                if (!current.isEmpty() && (o.isEmpty() || !o.get(o.size() - 1).equals(current))) o.add(current);
                return o;
            }
        } catch (Throwable ignore) {
            return Collections.singletonList(current == null ? "" : current);
        }
    }
    public static boolean has(long dialogId, int mid) {
        try {
            ensure();
            ArrayList<String> l = a.get(k(dialogId, mid));
            return l != null && l.size() > 0;
        } catch (Throwable ignore) { return false; }
    }
}
