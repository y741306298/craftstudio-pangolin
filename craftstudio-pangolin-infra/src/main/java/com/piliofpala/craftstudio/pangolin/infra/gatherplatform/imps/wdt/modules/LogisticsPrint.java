package com.piliofpala.craftstudio.pangolin.infra.gatherplatform.imps.wdt.modules;


import com.alibaba.fastjson2.JSONObject;
import com.piliofpala.craftstudio.pangolin.domain.logistics.exception.LogisticsLabelPrintException;
import com.piliofpala.craftstudio.pangolin.domain.logistics.vo.LogisticsCloudPrintData;
import com.piliofpala.craftstudio.pangolin.domain.logistics.vo.LogisticsCloudPrintPlatform;
import com.piliofpala.craftstudio.pangolin.domain.logistics.vo.LogisticsLabel;
import com.piliofpala.craftstudio.pangolin.infra.gatherplatform.imps.wdt.client.WdtClient;
import com.piliofpala.craftstudio.shared.domain.geo.consignee.vo.Address;
import com.piliofpala.craftstudio.shared.domain.geo.consignee.vo.Consignee;
import com.piliofpala.craftstudio.shared.domain.logistics.vo.LogisticsCarrierPresetType;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.Map;

public class LogisticsPrint {
    private static final System.Logger log = System.getLogger("craftstudio-pangolin");
    private static String token;
    private static Date tokenLastCreateTime;

    public LogisticsLabel print(WdtClient wdtClient, String tradeNo, String printer, boolean dispatch, LogisticsCarrierPresetType carrierPresetType) {
        if(needFlushLoginToken()) {
            createLoginToken();
        }
        String tradeId = new TradeQuery().queryTradeIdByTradeNo(
                wdtClient,tradeNo
        );
        if(tradeId==null){
            throw new LogisticsLabelPrintException("订单信息获取失败");
        }
        var batchId = ordinary(tradeId, printer, dispatch, carrierPresetType);
        return getPrintData(batchId,"ordinary");
    }

    public LogisticsLabel rePrint(WdtClient wdtClient, String tradeNo, String printer, boolean dispatch, LogisticsCarrierPresetType carrierPresetType, String logisticsOrderId) {
        if(needFlushLoginToken()) {
            createLoginToken();
        }
        String tradeId = new TradeQuery().queryTradeIdByTradeNo(
                wdtClient,tradeNo
        );
        if(tradeId==null){
            throw new LogisticsLabelPrintException("订单信息获取失败");
        }
        var batchId = multiRePrint(tradeId, printer, dispatch, carrierPresetType, logisticsOrderId);
        return getPrintData(batchId,"multiBatchRe");
    }

    private boolean needFlushLoginToken(){
        if(tokenLastCreateTime == null || token==null) {
            return true;
        }
        long diffMs = new Date().getTime() - tokenLastCreateTime.getTime();
        return diffMs > 1000*60*60*1.5;//超过1个半小时
    }

    private void createLoginToken() {
        try {
            // 1. 创建 URL 对象
            URL url = new URL("https://login.huice.com/open/tm/unified-login/v5/login/password");

            // 2. 打开连接
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // 3. 设置请求方法为 POST
            connection.setRequestMethod("POST");

            // 4. 设置请求头
            connection.setRequestProperty("Content-Type", "application/json");

            // 5. 允许输入输出流
            connection.setDoOutput(true);
            connection.setDoInput(true);

            // 6. 构建 JSON 请求体
            String jsonBody = "{"
                    + "\"mobileAccount\": true,"
                    + "\"account\": \"13349138430\","
                    + "\"password\": \"1qaz@WSX\","
                    + "\"clientType\": \"WEB\","
                    + "\"vid\": \"1784382890440_1a013886be5c175e9bf729b530791575\""
                    + "}";

            // 7. 写入请求体
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonBody.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            // 8. 获取响应码
            int responseCode = connection.getResponseCode();
            log.log(System.Logger.Level.INFO, "Response Code: " + responseCode);

            // 9. 读取响应内容
            BufferedReader reader;
            if (responseCode >= 200 && responseCode < 300) {
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"));
            } else {
                reader = new BufferedReader(new InputStreamReader(connection.getErrorStream(), "utf-8"));
            }

            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line.trim());
            }
            reader.close();
            log.log(System.Logger.Level.INFO, "Response Body: " + response);

            // 10. 断开连接
            connection.disconnect();

            var respObj = JSONObject.parseObject(response.toString());
            var data = respObj.getJSONObject("data");
            if(data == null){
                throw new LogisticsLabelPrintException("快递打印异常：无法模拟登录");
            }
            var newToken = data.getString("token");
            if(newToken == null || newToken.isBlank()){
                throw new LogisticsLabelPrintException("快递打印异常：无法模拟登录");
            }
            token = newToken;
            tokenLastCreateTime = new Date();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String ordinary(String tradeId, String printer, boolean dispatch, LogisticsCarrierPresetType carrierPresetType) {
        try {
            // 1. 目标 URL
            URL url = new URL("https://erp.huice.com/api/main/oms/logistics/print/async/ordinary");

            // 2. 打开连接
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            // 3. 设置请求方法
            conn.setRequestMethod("POST");

            // 4. 设置请求头
            conn.setRequestProperty("Content-Type", "application/json");

            // 5. 设置 Cookie（对应 curl 的 -b 参数）
            String cookie = "X-HC-TOKEN="+token;
            conn.setRequestProperty("Cookie", cookie);

            // 6. 允许输入输出
            conn.setDoOutput(true);
            conn.setDoInput(true);

            // 7. 构建 JSON 请求体
            String jsonBody = "{"
                    + "\"groupId\": \"\","
                    + "\"invoicePrintFlag\": false,"
                    + "\"invoiceTemplateId\": \"\","
                    + "\"pickingListTemplateId\": \"\","
                    + "\"logisticsPrintFlag\": true,"
                    + "\"excludedDataCacheId\": \"\","
                    + "\"mappings\": [{\"configKey\":\""+logisticsPrinterConfigKey(carrierPresetType)+"\",\"printer\":\""+printer+"\"}],"
                    + "\"pickingOrderNum\": 0,"
                    + "\"pickingPrintFlag\": false,"
                    + "\"printPickingUseLogisticsPrinter\": false,"
                    + "\"sendFlag\": "+dispatch+","
                    + "\"source\": \"ordinary\","
                    + "\"tradeIds\": [\""+tradeId+"\"],"
                    + "\"separatorPage\": false,"
                    + "\"forbidMultiWarehouse\": true"
                    + "}";

            // 8. 写入请求体
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonBody.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            // 9. 获取响应码
            int responseCode = conn.getResponseCode();
            log.log(System.Logger.Level.INFO, "Response Code: " + responseCode);

            // 10. 读取响应内容
            BufferedReader reader;
            if (responseCode >= 200 && responseCode < 300) {
                reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
            } else {
                reader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "utf-8"));
            }

            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line.trim());
            }
            reader.close();

            log.log(System.Logger.Level.INFO, "Response Body: " + response);

            var respObj = JSONObject.parseObject(response.toString());
            // 11. 断开连接
            conn.disconnect();
            return respObj.getJSONObject("data").getString("batchId");

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String multiRePrint(String tradeId, String printer, boolean dispatch, LogisticsCarrierPresetType carrierPresetType, String logisticsOrderId) {
        try {
            // 1. 目标 URL
            URL url = new URL("https://erp.huice.com/api/main/oms/logistics/print/async/multiRePrint");

            // 2. 打开连接
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            // 3. 设置请求方法
            conn.setRequestMethod("POST");

            // 4. 设置请求头
            conn.setRequestProperty("Content-Type", "application/json");

            // 5. 设置 Cookie（对应 curl 的 -b 参数）
            String cookie = "X-HC-TOKEN="+token;
            conn.setRequestProperty("Cookie", cookie);

            // 6. 允许输入输出
            conn.setDoOutput(true);
            conn.setDoInput(true);

            // 7. 构建 JSON 请求体
            String jsonBody = "{"
                    + "\"groupId\": \"\","
                    + "\"invoicePrintFlag\": false,"
                    + "\"invoiceTemplateId\": \"\","
                    + "\"pickingListTemplateId\": \"\","
                    + "\"logisticsPrintFlag\": true,"
                    + "\"excludedDataCacheId\": \"\","
                    + "\"mappings\": [{\"configKey\":\""+logisticsPrinterConfigKey(carrierPresetType)+"\",\"printer\":\""+printer+"\"}],"
                    + "\"pickingOrderNum\": 0,"
                    + "\"pickingPrintFlag\": false,"
                    + "\"printPickingUseLogisticsPrinter\": false,"
                    + "\"sendFlag\": "+dispatch+","
                    + "\"source\": \"ordinary\","
                    + "\"tradeIds\": [\""+tradeId+"\"],"
                    + "\"separatorPage\": false,"
                    + "\"forbidMultiWarehouse\": true,"
                    + "\"logisticsNos\": [\""+logisticsOrderId+"\"]"
                    + "}";

            // 8. 写入请求体
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonBody.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            // 9. 获取响应码
            int responseCode = conn.getResponseCode();
            log.log(System.Logger.Level.INFO, "Response Code: " + responseCode);

            // 10. 读取响应内容
            BufferedReader reader;
            if (responseCode >= 200 && responseCode < 300) {
                reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
            } else {
                reader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "utf-8"));
            }

            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line.trim());
            }
            reader.close();

            log.log(System.Logger.Level.INFO, "Response Body: " + response);

            var respObj = JSONObject.parseObject(response.toString());
            // 11. 断开连接
            conn.disconnect();
            return respObj.getJSONObject("data").getString("batchId");

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private LogisticsLabel getPrintData(String batchId, String type) throws LogisticsLabelPrintException{
        try {
            // 1. 创建 URL 对象
            URL url = new URL("https://erp.huice.com/api/main/oms/logistics/print/async/getPrintData");

            // 2. 打开连接
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // 3. 设置请求方法为 POST
            connection.setRequestMethod("POST");

            // 4. 设置请求头
            connection.setRequestProperty("Content-Type", "application/json");
            String cookie = "X-HC-TOKEN="+token;
            connection.setRequestProperty("Cookie", cookie);

            // 5. 允许输入输出流
            connection.setDoOutput(true);
            connection.setDoInput(true);

            // 6. 构建 JSON 请求体
            String jsonBody = "{"
                    + "\"batchId\": \""+batchId+"\","
                    + "\"type\": \""+type+"\""
                    + "}";

            // 7. 写入请求体
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonBody.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            // 8. 获取响应码
            int responseCode = connection.getResponseCode();
            log.log(System.Logger.Level.INFO, "Response Code: " + responseCode);

            // 9. 读取响应内容
            BufferedReader reader;
            if (responseCode >= 200 && responseCode < 300) {
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"));
            } else {
                reader = new BufferedReader(new InputStreamReader(connection.getErrorStream(), "utf-8"));
            }

            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line.trim());
            }
            reader.close();

            log.log(System.Logger.Level.INFO, "Response Body: " + response);

            // 10. 断开连接
            connection.disconnect();

            var respObj = JSONObject.parseObject(response.toString());
            var dataObj = respObj.getJSONObject("data");
            if(dataObj == null){
                throw new LogisticsLabelPrintException("快递打印异常：旺店通服务暂不可用");
            }
            var successResults = dataObj.getJSONArray("successResults");
            if(successResults == null || successResults.size()!=1) {
                throw new LogisticsLabelPrintException(errorReason(batchId));
            }
            var successResult = successResults.getJSONObject(0);
            var logisticsOrderId = successResult.getString("logisticsNo");
            if(logisticsOrderId == null || logisticsOrderId.isBlank()){
                throw new LogisticsLabelPrintException(errorReason(batchId));
            }
            LogisticsLabel logisticsLabel = new LogisticsLabel();
            logisticsLabel.setLogisticsOrderId(logisticsOrderId);
            var customData = successResult.getJSONObject("customData");
            if(customData!=null) {
                Consignee consignee = new Consignee();
                logisticsLabel.setConsignee(consignee);
                consignee.setName(customData.getString("receiver_name"));
                consignee.setPhone(customData.getString("receiver_mobile"));
                Address address = new Address();
                address.setDetailAddress(customData.getString("receiver_address"));
                consignee.setAddress(address);
            }
            var printData = successResult.getString("printData");
            if(printData!=null){
                LogisticsCloudPrintData logisticsCloudPrintData = new LogisticsCloudPrintData();
                logisticsCloudPrintData.setPrintData(printData);
                logisticsCloudPrintData.setCustomTemplateUrl(successResult.getString("customTemplateUrl"));
                if(successResult.containsKey("params") && successResult.getString("params")!=null){
                    logisticsCloudPrintData.setExtras(Map.of("params", successResult.getString("params")));
                }
                String standardTemplateUrl = successResult.getString("standardTemplateUrl");
                logisticsCloudPrintData.setStandardTemplateUrl(standardTemplateUrl);
                if(standardTemplateUrl==null) {
                    logisticsCloudPrintData.setCloudPrintPlatform(LogisticsCloudPrintPlatform.UNKNOWN);
                }else if(standardTemplateUrl.contains("cainiao.com")) {
                    logisticsCloudPrintData.setCloudPrintPlatform(LogisticsCloudPrintPlatform.CAINIAO);
                }else if(standardTemplateUrl.contains("ecombdstatic.com")) {
                    logisticsCloudPrintData.setCloudPrintPlatform(LogisticsCloudPrintPlatform.DY);
                }else if(standardTemplateUrl.contains("pinduoduo.com")) {
                    logisticsCloudPrintData.setCloudPrintPlatform(LogisticsCloudPrintPlatform.PDD);
                }else {
                    logisticsCloudPrintData.setCloudPrintPlatform(LogisticsCloudPrintPlatform.UNKNOWN);
                }

                logisticsLabel.setLogisticsCloudPrintData(logisticsCloudPrintData);
            }

            return logisticsLabel;

        } catch (Exception e) {
            e.printStackTrace();
            throw new LogisticsLabelPrintException(errorReason(batchId));
        }
    }

    //获取错误类型
    private String errorReason(String batchId) {
        try {
            // 1. 创建 URL 对象
            URL url = new URL("https://erp.huice.com/api/main/oms/logistics/print/batchDetail");

            // 2. 打开连接
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // 3. 设置请求方法为 POST
            connection.setRequestMethod("POST");

            // 4. 设置请求头
            connection.setRequestProperty("Content-Type", "application/json");
            String cookie = "X-HC-TOKEN="+token;
            connection.setRequestProperty("Cookie", cookie);

            // 5. 允许输入输出流
            connection.setDoOutput(true);
            connection.setDoInput(true);

            // 6. 构建 JSON 请求体
            String jsonBody = "{"
                    + "\"batchId\": \""+batchId+"\","
                    + "\"type\": \"ordinary\""
                    + "}";

            // 7. 写入请求体
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonBody.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            // 8. 获取响应码
            int responseCode = connection.getResponseCode();
            log.log(System.Logger.Level.INFO, "Response Code: " + responseCode);

            // 9. 读取响应内容
            BufferedReader reader;
            if (responseCode >= 200 && responseCode < 300) {
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"));
            } else {
                reader = new BufferedReader(new InputStreamReader(connection.getErrorStream(), "utf-8"));
            }

            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line.trim());
            }
            reader.close();

            log.log(System.Logger.Level.INFO, "Response Body: " + response);

            // 10. 断开连接
            connection.disconnect();

            var respObj = JSONObject.parseObject(response.toString());
            var dataObj = respObj.getJSONArray("data");
            if(dataObj == null || dataObj.isEmpty()){
                return "未知错误";
            }
            return dataObj.getJSONObject(0).getString("reason");

        } catch (Exception e) {
            e.printStackTrace();
            return "未知错误";
        }
    }

    private String logisticsPrinterConfigKey(LogisticsCarrierPresetType carrierPresetType) {
        if(carrierPresetType == LogisticsCarrierPresetType.ZTO){
            return "54";
        }else if(carrierPresetType == LogisticsCarrierPresetType.SF){
            return "32";
        }else if(carrierPresetType == LogisticsCarrierPresetType.YD){
            return "49";
        }else if(carrierPresetType == LogisticsCarrierPresetType.STO){
            return "30";
        }else if(carrierPresetType == LogisticsCarrierPresetType.YTO){
            return "47";
        }else if(carrierPresetType == LogisticsCarrierPresetType.EMS){
            return "3";
        }else if(carrierPresetType == LogisticsCarrierPresetType.JD){
            return "59";
        }else if(carrierPresetType == LogisticsCarrierPresetType.JTSD){
            return "58";
        }else if(carrierPresetType == LogisticsCarrierPresetType.TT){
            return "39";
        }else if(carrierPresetType == LogisticsCarrierPresetType.HTKY){
            return "8";
        }else if(carrierPresetType == LogisticsCarrierPresetType.DBL){
            return "12";
        }else if(carrierPresetType == LogisticsCarrierPresetType.ANE){
            return "6";
        }else if(carrierPresetType == LogisticsCarrierPresetType.YMDD){
            return "42";
        }
        throw new LogisticsLabelPrintException("暂不支持的物流类型");
    }
}
