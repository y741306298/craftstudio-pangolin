package com.piliofpala.craftstudio.pangolin.domain.logistics.vo;

import java.util.Map;

public class LogisticsCloudPrintData {
    private LogisticsCloudPrintPlatform cloudPrintPlatform;
    private String printData;
    private String standardTemplateUrl;
    private String customTemplateUrl;
    private Map<String, Object> extras;

    public LogisticsCloudPrintPlatform getCloudPrintPlatform() {
        return cloudPrintPlatform;
    }

    public void setCloudPrintPlatform(LogisticsCloudPrintPlatform cloudPrintPlatform) {
        this.cloudPrintPlatform = cloudPrintPlatform;
    }

    public Map<String, Object> getExtras() {
        return extras;
    }

    public void setExtras(Map<String, Object> extras) {
        this.extras = extras;
    }

    public String getPrintData() {
        return printData;
    }

    public void setPrintData(String printData) {
        this.printData = printData;
    }

    public String getStandardTemplateUrl() {
        return standardTemplateUrl;
    }

    public void setStandardTemplateUrl(String standardTemplateUrl) {
        this.standardTemplateUrl = standardTemplateUrl;
    }

    public String getCustomTemplateUrl() {
        return customTemplateUrl;
    }

    public void setCustomTemplateUrl(String customTemplateUrl) {
        this.customTemplateUrl = customTemplateUrl;
    }
}
