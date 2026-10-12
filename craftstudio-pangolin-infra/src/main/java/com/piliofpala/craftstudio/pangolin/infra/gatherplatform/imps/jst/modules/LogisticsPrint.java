package com.piliofpala.craftstudio.pangolin.infra.gatherplatform.imps.jst.modules;

import com.alibaba.fastjson2.JSONObject;
import com.piliofpala.craftstudio.pangolin.domain.gatherplatform.auth.AuthException;
import com.piliofpala.craftstudio.pangolin.domain.gatherplatform.auth.SmsCodeRequiredAuthException;
import com.piliofpala.craftstudio.pangolin.domain.logistics.exception.LogisticsLabelPrintException;
import com.piliofpala.craftstudio.pangolin.domain.logistics.vo.LogisticsCloudPrintData;
import com.piliofpala.craftstudio.pangolin.domain.logistics.vo.LogisticsCloudPrintPlatform;
import com.piliofpala.craftstudio.pangolin.domain.logistics.vo.LogisticsLabel;
import com.piliofpala.craftstudio.pangolin.infra.gatherplatform.imps.jst.client.JstClient;
import com.piliofpala.craftstudio.pangolin.infra.gatherplatform.imps.jst.modules.ioorder.IOOrderExecutor;
import com.piliofpala.craftstudio.pangolin.infra.gatherplatform.imps.jst.modules.simulator.AspDataPackageParser;
import com.piliofpala.craftstudio.pangolin.infra.gatherplatform.imps.jst.modules.simulator.CookieHolder;
import com.piliofpala.craftstudio.pangolin.infra.gatherplatform.imps.jst.modules.simulator.HttpDataBuilder;
import com.piliofpala.craftstudio.pangolin.infra.gatherplatform.imps.jst.modules.simulator.WebSimulator;
import com.piliofpala.craftstudio.shared.domain.logistics.vo.LogisticsCarrierPresetType;

import java.io.*;
import java.net.*;
import java.util.*;

public class LogisticsPrint {
    private static final System.Logger log = System.getLogger("craftstudio-pangolin");
    private final WebSimulator webSimulator = WebSimulator.getLoggedInstance();
    public LogisticsLabel print(JstClient jstClient, String orderId, String printer, boolean dispatch, LogisticsCarrierPresetType carrierPresetType) {
        //获取出货单
        var ioOrder = new IOOrderExecutor().forceObtainIoOder(jstClient, orderId, webSimulator);
        //获取ioId
        var ioId = ioOrder.getString("io_id");//getIoId(jstClient, orderId);
        log.log(System.Logger.Level.INFO,"ioId:"+ioId);
        if(ioId == null) {//未出库，需要调用强制审核
            auditOrder(orderId);
            WebSimulator.waitStupid(1);//聚水潭审核后到生成出库单有延迟
            ioId = getIoId(jstClient, orderId);
            if(ioId == null) {//还是获取不到出库id
                throw new LogisticsLabelPrintException("聚水潭服务暂不可用");
            }
        }
        //打印
        var logisticsLabel = getPrintData(orderId, ioId, carrierPresetType);
        log.log(System.Logger.Level.INFO, "========== PRINT DATA ==========");
        log.log(System.Logger.Level.INFO, logisticsLabel.dump());
        log.log(System.Logger.Level.INFO, "================================");
        return logisticsLabel;
    }

    private String getIoId(JstClient client, String oid){
        try {
            String response = client.execute("orders/out/simple/query", JSONObject.toJSONString(
                    Map.of("o_ids",new Object[]{oid})
            ));
            log.log(System.Logger.Level.INFO,response);
            var respObj = JSONObject.parseObject(response);
            int code = respObj.getInteger("code");
            if(code!=0){
                throw new LogisticsLabelPrintException(respObj.getString("msg"));
            }
            var datas = respObj.getJSONObject("data").getJSONArray("datas");
            if(datas.isEmpty()) {
                return null;
            }
            if(datas.size()>1) {
                //找出有效的，忽略其他作废的
                for (int i = 0; i < datas.size(); i++) {
                    var dataObj = datas.getJSONObject(i);
                    var status = dataObj.getString("status");
                }
                throw new LogisticsLabelPrintException("订单存在多个出库记录，无法打印");
            }
            return datas.getJSONObject(0).getString("io_id");
        } catch (IOException e) {
            e.printStackTrace();
            throw new LogisticsLabelPrintException(e.getMessage());
        }
    }

    private static final String WWW_BASE = "https://www.erp321.com";

    public LogisticsPrint() {
    }

    private void auditOrder(String oId) {
        log.log(System.Logger.Level.INFO,"------------------------------订单审核------------------------------");
        var apiUrl = WWW_BASE + "/app/order/order/list.aspx?_c=jst-epaas&epaas=true&am___=ConfirmCheckOids&ts___="+System.currentTimeMillis();
        var viewState = webSimulator.fetchViewState(apiUrl);
        var form = HttpDataBuilder.buildForm(Map.of(
                "__VIEWSTATE", viewState.getViewState(),
                "__VIEWSTATEGENERATOR", viewState.getViewStateGenerator(),
                "__CALLBACKID", "JTable1",
                "__CALLBACKPARAM", "{\"Method\":\"ConfirmCheckOids\",\"Args\":[\""+oId+"\"],\"CallControl\":\"{page}\"}"
        ));
        var expressSetterResult = webSimulator.postForm(apiUrl, form, apiUrl);
        log.log(System.Logger.Level.INFO,expressSetterResult.body());
    }


    private String fetchKeyData(String oId) {
        log.log(System.Logger.Level.INFO,"------------------------------获取 keyData------------------------------");
        var apiUrl = WWW_BASE + "/app/wms/express/expresssetter.aspx?_c=jst-epaas&epaas=true&am___=LoadDataToJSON&ts___="+System.currentTimeMillis();
        var viewState = webSimulator.fetchViewState(apiUrl);
        var form = HttpDataBuilder.buildForm(Map.of(
                "__VIEWSTATE", viewState.getViewState(),
                "__VIEWSTATEGENERATOR", viewState.getViewStateGenerator(),
                "__CALLBACKID", "JTable1",
                "__CALLBACKPARAM", "{\"Method\":\"LoadDataToJSON\",\"Args\":[\"1\",\"[{\\\"k\\\":\\\"o_id\\\",\\\"v\\\":\\\""+oId+"\\\",\\\"c\\\":\\\"@=\\\"}]\",\"{}\"]}"
        ));
        var expressSetterResult = webSimulator.postForm(apiUrl, form, apiUrl);

        String jsonStr = expressSetterResult.body().replaceFirst("^\\d+\\|", "");
        var value = JSONObject.parse(JSONObject.parse(jsonStr).getString("ReturnValue"));
        var datas = value.getJSONArray("datas");
        if(datas == null) {
            return null;
        }else if(datas.size()>1) {
            throw new LogisticsLabelPrintException("多个记录");
        }
        var data = datas.getJSONObject(0);
        return data.getString("__KeyData");
    }

    private void createLogisticsLabel(String oId) {
        log.log(System.Logger.Level.INFO,"------------------------------生成快递面单------------------------------");
        String keyData = fetchKeyData(oId);
        if(keyData == null) {//某些原因订单需要重新审核
            auditOrder(oId);
            WebSimulator.waitStupid(1);
            keyData = fetchKeyData(oId);
            if(keyData == null) {//还是不行
                throw new LogisticsLabelPrintException("打印异常，无法获取keyData");
            }
        }

        var apiUrl = WWW_BASE + "/app/wms/express/SetELids.aspx?_c=jst-epaas&epaas=true&am___=SetElidsForExpressSetter&ts___="+System.currentTimeMillis();
        var viewState = webSimulator.fetchViewState(apiUrl);
        var form = HttpDataBuilder.buildForm(Map.of(
                "__VIEWSTATE", viewState.getViewState(),
                "__VIEWSTATEGENERATOR", viewState.getViewStateGenerator(),
                "__CALLBACKID", "ACall1",
                "__CALLBACKPARAM", "{\"Method\":\"SetElidsForExpressSetter\",\"Args\":[\""+keyData+"\"],\"CallControl\":\"{page}\"}"
        ));
        var expressSetterResult = webSimulator.postForm(apiUrl, form, apiUrl);
        log.log(System.Logger.Level.INFO, expressSetterResult.body());

        var dataObj = new AspDataPackageParser().parseToJson(expressSetterResult.body());
        var returnValue = dataObj.getJSONObject("ReturnValue");
        if(returnValue!=null) {
            for (Map.Entry<String, Object> entry : returnValue.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                // 正常情况下是{"dNphwEBVu0O0/Vsn5RjuZA==":"79033927895255"}， value直接是一个字符串类型的快递单号，而异常的时候
                // value 变为一个对象，包含Code，Message等错误信息
                if(value instanceof JSONObject valueObj) {
                    throw new LogisticsLabelPrintException(valueObj.getString("Message"));
                }
            }
        }
    }


    private LogisticsLabel getPrintData(String oId, String ioId, LogisticsCarrierPresetType logisticsCarrierPresetType) {

        var printDataObj = fetchPrintDataObject(ioId);
        if(printDataObj == null) {
            //还未生成取快递面单，需要生成
            createLogisticsLabel(oId);
            WebSimulator.waitStupid(1);//面单生成后可能有延迟
            printDataObj = fetchPrintDataObject(ioId);
            if(printDataObj == null) {//还是获取不到
                throw new LogisticsLabelPrintException("无法获取聚水潭面单数据");
            }
        }

        var logisticsOrderId = printDataObj.getString("LId");
        if(logisticsOrderId == null || logisticsOrderId.isBlank()){
            throw new LogisticsLabelPrintException("打印出错");
        }
        LogisticsLabel logisticsLabel = new LogisticsLabel();
        logisticsLabel.setLogisticsOrderId(logisticsOrderId);

        var task = printDataObj.getJSONObject("task");
        var documents = task.getJSONArray("documents");
        if(documents == null || documents.size()!=1) {
            throw new LogisticsLabelPrintException("打印出错");
        }
        var document = documents.getJSONObject(0);
        var contents = document.getJSONArray("contents");
        if(contents == null || contents.isEmpty()) {
            throw new LogisticsLabelPrintException("打印出错");
        }
        var content = contents.getJSONObject(0);
        var printData = content.getString("encryptedData");

        if(printData!=null){
            LogisticsCloudPrintData logisticsCloudPrintData = new LogisticsCloudPrintData();
            logisticsCloudPrintData.setPrintData(printData);
            logisticsCloudPrintData.setCustomTemplateUrl(content.getString("templateURL"));
            var templateUrl = content.getString("templateURL");
            logisticsCloudPrintData.setCustomTemplateUrl(templateUrl);
//                if(successResult.containsKey("params") && content.getString("params")!=null){
//                    logisticsCloudPrintData.setExtras(Map.of("params", successResult.getString("params")));
//                }
//                String standardTemplateUrl = successResult.getString("standardTemplateUrl");
//                logisticsCloudPrintData.setStandardTemplateUrl(standardTemplateUrl);
            if(templateUrl==null) {
                logisticsCloudPrintData.setCloudPrintPlatform(LogisticsCloudPrintPlatform.UNKNOWN);
            }else if(templateUrl.contains("cainiao.com")) {
                logisticsCloudPrintData.setCloudPrintPlatform(LogisticsCloudPrintPlatform.CAINIAO);
            }else if(templateUrl.contains("ecombdstatic.com")) {
                logisticsCloudPrintData.setCloudPrintPlatform(LogisticsCloudPrintPlatform.DY);
            }else {
                logisticsCloudPrintData.setCloudPrintPlatform(LogisticsCloudPrintPlatform.UNKNOWN);
            }
            logisticsLabel.setLogisticsCloudPrintData(logisticsCloudPrintData);
        }

        return logisticsLabel;
    }

    private JSONObject fetchPrintDataObject(String ioId) {
        log.log(System.Logger.Level.INFO, "-------------------------------获取打印面单-----------------------------");
        var apiUrl = WWW_BASE + "/app/print/print/Printer.aspx?async=true";
        var form = HttpDataBuilder.buildForm(Map.of(
                "type", "hybrid_print_print_service",
                "ids", ioId,
                "moudle", "DDJH"

        ));
        var printResult = webSimulator.postForm(apiUrl, form, apiUrl);
        webSimulator.dumpCookies();

        if (printResult.statusCode() < 200 || printResult.statusCode() >= 300) {
            throw new LogisticsLabelPrintException(
                    "获取打印数据失败 HTTP " + printResult.statusCode() + "\n" + printResult.body()
            );
        }
        var printResultBody = printResult.body();
        log.log(System.Logger.Level.INFO, printResultBody);
        var dataObj = JSONObject.parse(printResultBody);
        var requestBodys = dataObj.getJSONArray("requestBodys");
        log.log(System.Logger.Level.INFO, "requestBodys: " + requestBodys);
        log.log(System.Logger.Level.INFO, "-----------------------------------------------------------------------");

        if(requestBodys == null){
            throw new LogisticsLabelPrintException("快递打印异常：聚水潭服务暂不可用");
        }
        if(requestBodys.isEmpty()) {
            if(dataObj.containsKey("apiResponse")){
                var apiResponse = dataObj.getJSONObject("apiResponse");
                if(apiResponse!=null && apiResponse.getIntValue("code",-1)==910001) {
                    var sid = apiResponse.getJSONObject("data").getString("sid");
                    log.log(System.Logger.Level.INFO,"sid:"+sid);
                    //已经测试确定登出账号再登录无法解除这个安全校验，只能真的用验证码了
                    throw new SmsCodeRequiredAuthException("需要验证码校验身份");
                }
            }
            return null;
        }
        if(requestBodys.size()>1){
            throw new LogisticsLabelPrintException("订单存在多个面单");
        }
        return requestBodys.getJSONObject(0);
    }

}
