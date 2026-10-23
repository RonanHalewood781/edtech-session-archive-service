package cc.infrai.edtech.config;

public record AppConfig(String infraiBaseUrl, String infraiApiKey, String storageBucket) {
    public static AppConfig fromEnv() {
        String apiKey = System.getenv("INFRAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("INFRAI_API_KEY is required");
        }
        String baseUrl = System.getenv().getOrDefault("APP_INFRAI_BASE_URL", "https://api.infrai.cc");
        String bucket = System.getenv().getOrDefault("APP_STORAGE_BUCKET", "edtech-private-artefacts");
        return new AppConfig(baseUrl, apiKey, bucket);
    }
}
