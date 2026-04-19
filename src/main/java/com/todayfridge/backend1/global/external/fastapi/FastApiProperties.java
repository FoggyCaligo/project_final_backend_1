package com.todayfridge.backend1.global.external.fastapi;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.fastapi")
public class FastApiProperties {
    private String baseUrl;
    private String serviceKey;
    private String callerService = "spring-backend";

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getServiceKey() { return serviceKey; }
    public void setServiceKey(String serviceKey) { this.serviceKey = serviceKey; }
    public String getCallerService() { return callerService; }
    public void setCallerService(String callerService) { this.callerService = callerService; }
}
