package cc.infrai.edtech.infra;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record Envelope(boolean ok, String errorCode, String errorMessage) {
    public static Envelope parse(String json) {
        boolean ok = extractBoolean(json, "ok");
        String code = extractString(json, "code");
        String message = extractString(json, "message");
        return new Envelope(ok, code, message);
    }

    private static boolean extractBoolean(String json, String field) {
        Pattern pattern = Pattern.compile("\\\"" + Pattern.quote(field) + "\\\"\\s*:\\s*(true|false)");
        Matcher matcher = pattern.matcher(json);
        return matcher.find() && Boolean.parseBoolean(matcher.group(1));
    }

    public static String extractString(String json, String field) {
        Pattern pattern = Pattern.compile("\\\"" + Pattern.quote(field) + "\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"");
        Matcher matcher = pattern.matcher(json);
        return matcher.find() ? matcher.group(1) : null;
    }

    public static Integer extractInt(String json, String field) {
        Pattern pattern = Pattern.compile("\\\"" + Pattern.quote(field) + "\\\"\\s*:\\s*(\\d+)");
        Matcher matcher = pattern.matcher(json);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : null;
    }
}
