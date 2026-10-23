package cc.infrai.edtech.service;

public final class Json {
    private Json() {
    }

    public static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
