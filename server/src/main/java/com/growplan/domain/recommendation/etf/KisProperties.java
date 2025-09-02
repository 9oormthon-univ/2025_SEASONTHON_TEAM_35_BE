package com.growplan.domain.recommendation.etf;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "kis")
public class KisProperties {
    private String env;
    private String custtype;   // P/B
    private String appkey;
    private String secretkey;  // yml에서도 appsecret으로 통일 권장
    private List<String> domestic;         // ["277630","272910"]
    private Map<String, String> overseas;  // {SPY:"ARCX.SPY", ...}

    // getters / setters
    public String getEnv() { return env; }
    public void setEnv(String env) { this.env = env; }
    public String getCusttype() { return custtype; }
    public void setCusttype(String custtype) { this.custtype = custtype; }
    public String getAppkey() { return appkey; }
    public void setAppkey(String appkey) { this.appkey = appkey; }
    public String getSecretkey() { return secretkey; }
    public void setSecretkey(String secretkey) { this.secretkey = secretkey; }
    public List<String> getDomestic() { return domestic; }
    public void setDomestic(List<String> domestic) { this.domestic = domestic; }
    public Map<String, String> getOverseas() { return overseas; }
    public void setOverseas(Map<String, String> overseas) { this.overseas = overseas; }
}
