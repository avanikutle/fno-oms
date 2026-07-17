package com.fnooms.broker.mstock;

import com.fnooms.util.AppConfig;
import com.fnooms.util.CredsUtil;

/**
 * Holds mStock-specific configuration.
 */
public class MStockConfig {

    private final String baseUrl;
    private final String apiVersion;
    private final String apiKey;
    private final String accessToken;

    public MStockConfig(String prefix) {
        AppConfig app = AppConfig.getInstance();
        this.baseUrl      = app.getMStockBaseUrl();
        this.apiVersion   = app.getMStockApiVersion();
        this.apiKey       = CredsUtil.getApiKey(prefix);
        this.accessToken  = CredsUtil.getJwtToken(prefix);
    }

    /** Authorization header value for Type B: Bearer {accessToken} */
    public String getAuthorizationHeader() {
        return "Bearer " + accessToken;
    }

    public String getPrivateKey() {
        return apiKey;
    }

    public String getBaseUrl()     { return baseUrl; }
    public String getApiVersion()  { return apiVersion; }
    public String getApiKey()      { return apiKey; }
    public String getAccessToken() { return accessToken; }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank()
            && accessToken != null && !accessToken.isBlank();
    }
}
