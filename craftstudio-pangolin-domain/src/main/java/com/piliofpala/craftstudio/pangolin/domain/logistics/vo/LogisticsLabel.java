package com.piliofpala.craftstudio.pangolin.domain.logistics.vo;

import com.piliofpala.craftstudio.shared.domain.geo.consignee.vo.Consignee;

public class LogisticsLabel {
    private String logisticsOrderId;
    private Consignee consignee;
    private LogisticsCloudPrintData logisticsCloudPrintData;

    public String getLogisticsOrderId() {
        return logisticsOrderId;
    }

    public void setLogisticsOrderId(String logisticsOrderId) {
        this.logisticsOrderId = logisticsOrderId;
    }

    public Consignee getConsignee() {
        return consignee;
    }

    public void setConsignee(Consignee consignee) {
        this.consignee = consignee;
    }

    public LogisticsCloudPrintData getLogisticsCloudPrintData() {
        return logisticsCloudPrintData;
    }

    public void setLogisticsCloudPrintData(LogisticsCloudPrintData logisticsCloudPrintData) {
        this.logisticsCloudPrintData = logisticsCloudPrintData;
    }

    public String dump() {
        StringBuilder dumpStr = new StringBuilder("logisticsOrderId:" + logisticsOrderId + "\n");
        dumpStr.append("logisticsCloudPrintData.cloudPrintPlatform: ").append(logisticsCloudPrintData.getCloudPrintPlatform()).append("\n");
        dumpStr.append("logisticsCloudPrintData.customTemplateUrl: ").append(logisticsCloudPrintData.getCustomTemplateUrl()).append("\n");
        dumpStr.append("logisticsCloudPrintData.standardTemplateUrl: ").append(logisticsCloudPrintData.getStandardTemplateUrl()).append("\n");
        dumpStr.append("logisticsCloudPrintData.printData: ").append(logisticsCloudPrintData.getPrintData()).append("\n");
        dumpStr.append("==============logisticsCloudPrintData.extras: ================\n");
        if(logisticsCloudPrintData.getExtras() != null && !logisticsCloudPrintData.getExtras().isEmpty()) {
            for(String key : logisticsCloudPrintData.getExtras().keySet()) {
                var value = logisticsCloudPrintData.getExtras().get(key);
                dumpStr.append(key).append(": ").append(value).append("\n");
            }
        }else {
            dumpStr.append("无额外参数\n");
        }
        dumpStr.append("============================");
        return dumpStr.toString();
    }
}
