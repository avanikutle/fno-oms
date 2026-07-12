package com.fnooms.broker.mstock;

import com.fnooms.model.BrokerConfig;
import com.fnooms.util.AppConfig;

/**
 * Holds mStock-specific configuration extracted from BrokerConfig.
 */
public class MStockConfig {

    private final String baseUrl;
    private final String apiVersion;
    private final String apiKey;
    private final String privateKey;
    private final String accessToken;

    public MStockConfig(BrokerConfig config) {
        AppConfig app = AppConfig.getInstance();
        this.baseUrl      = app.getMStockBaseUrl();
        this.apiVersion   = app.getMStockApiVersion();
        this.apiKey       = config.getApiKey();
        this.privateKey   = config.getPrivateKey();
        this.accessToken  = config.getAccessToken();
    }

    /** Authorization header value: token {apiKey}:{accessToken} */
    public String getAuthorizationHeader() {
        return "token " + apiKey + ":" + accessToken;
    }

    public String getBaseUrl()     { return baseUrl; }
    public String getApiVersion()  { return apiVersion; }
    public String getApiKey()      { return apiKey; }
    public String getPrivateKey()  { return privateKey; }
    public String getAccessToken() { return accessToken; }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank()
            && accessToken != null && !accessToken.isBlank();
    }
}
