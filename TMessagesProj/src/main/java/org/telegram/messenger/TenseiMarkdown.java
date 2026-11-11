package org.telegram.messenger;

import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;

public class TenseiMarkdown {

    public static class Result {
        public String text;
        public ArrayList<TLRPC.MessageEntity> entities = new ArrayList<>();
    }

    public static Result formatQa(String question, String markdownAnswer) {
        Result r = new Result();
        StringBuilder sb = new StringBuilder();
        ArrayList<TLRPC.MessageEntity> entities = new ArrayList<>();

        String q = question.trim();
        if (!q.isEmpty()) {
            TLRPC.TL_messageEntityBold b = new TLRPC.TL_messageEntityBold();
            b.offset = 0;
            b.length = q.length();
            entities.add(b);
            sb.append(q).append("\n\n");
        }

        parseInto(markdownAnswer, sb, entities);
        r.text = sb.toString().trim();
        r.entities = entities;
        return r;
    }

    private static void parseInto(String markdown, StringBuilder out, ArrayList<TLRPC.MessageEntity> entities) {
        if (markdown == null) {
            return;
        }
        String[] lines = markdown.split("\n");
        for (int li = 0; li < lines.length; li++) {
            String line = lines[li].trim();
            if (line.isEmpty()) {
                out.append("\n");
                continue;
            }
            if (line.startsWith("#")) {
                int i = 0;
                while (i < line.length() && line.charAt(i) == '#') i++;
                line = line.substring(i).trim();
                int start = out.length();
                appendInline(line, out, entities);
                if (out.length() > start) {
                    TLRPC.TL_messageEntityBold b = new TLRPC.TL_messageEntityBold();
                    b.offset = start;
                    b.length = out.length() - start;
                    entities.add(b);
                }
            } else if (line.startsWith("- ") || line.startsWith("* ")) {
                out.append("- ");
                appendInline(line.substring(2).trim(), out, entities);
            } else {
                appendInline(line, out, entities);
            }
            if (li < lines.length - 1) {
                out.append("\n");
            }
        }
    }

    private static void appendInline(String s, StringBuilder out, ArrayList<TLRPC.MessageEntity> entities) {
        int i = 0;
        int n = s.length();
        while (i < n) {
            if (s.startsWith("**", i)) {
                int end = s.indexOf("**", i + 2);
                if (end != -1) {
                    String inner = s.substring(i + 2, end);
                    if (!inner.isEmpty()) {
                        TLRPC.TL_messageEntityBold b = new TLRPC.TL_messageEntityBold();
                        b.offset = out.length();
                        b.length = inner.length();
                        entities.add(b);
                        out.append(inner);
                    }
                    i = end + 2;
                    continue;
                }
            }
            if ((s.charAt(i) == '*' || s.charAt(i) == '_')) {
                char c = s.charAt(i);
                int end = s.indexOf(c, i + 1);
                if (end != -1 && end > i + 1) {
                    String inner = s.substring(i + 1, end);
                    TLRPC.TL_messageEntityItalic it = new TLRPC.TL_messageEntityItalic();
                    it.offset = out.length();
                    it.length = inner.length();
                    entities.add(it);
                    out.append(inner);
                    i = end + 1;
                    continue;
                }
            }
            if (s.charAt(i) == '`') {
                int end = s.indexOf('`', i + 1);
                if (end != -1) {
                    out.append(s, i + 1, end);
                    i = end + 1;
                    continue;
                }
            }
            out.append(s.charAt(i));
            i++;
        }
    }
}
