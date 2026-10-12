package com.piliofpala.craftstudio.pangolin.infra.gatherplatform.imps.jst.modules.ioorder;

import com.alibaba.fastjson2.JSONObject;
import com.piliofpala.craftstudio.pangolin.domain.logistics.exception.LogisticsLabelPrintException;
import com.piliofpala.craftstudio.pangolin.infra.gatherplatform.imps.jst.client.JstClient;
import com.piliofpala.craftstudio.pangolin.infra.gatherplatform.imps.jst.modules.simulator.HttpDataBuilder;
import com.piliofpala.craftstudio.pangolin.infra.gatherplatform.imps.jst.modules.simulator.WebSimulator;

import java.io.IOException;
import java.util.Map;

/**
 * 出库单执行器
 */
public class IOOrderExecutor {
    private static final System.Logger log = System.getLogger("craftstudio-pangolin");

    public JSONObject forceObtainIoOder(JstClient client, String oid, WebSimulator webSimulator) {
        var validIoOrder = obtainValidIoOrder(client, oid);
        if(validIoOrder == null) {
            log.log(System.Logger.Level.INFO, "无有效出货单，尝试审核");
            auditOrder(oid, webSimulator);
            WebSimulator.waitStupid(1);
            validIoOrder = obtainValidIoOrder(client, oid);
            if(validIoOrder == null) {//还是没有已出库的出库单
                throw new LogisticsLabelPrintException("无法获取有效出货单");
            }
        }
        var wmsId = validIoOrder.getString("wms_co_id");
        if(wmsId == null || wmsId.isEmpty()) {
            throw new LogisticsLabelPrintException("请先执行换仓！");
        }

        var logisticsId = validIoOrder.getString("lc_id");
        if(logisticsId == null || logisticsId.isEmpty() || "DUMMY".equals(logisticsId)) {
            throw new LogisticsLabelPrintException("请先指定物流！");
        }
        return validIoOrder;

    }

    private JSONObject obtainValidIoOrder(JstClient client, String oid){
        try {
            String response = client.execute("orders/out/simple/query", JSONObject.toJSONString(
                    Map.of("o_ids",new Object[]{oid})
            ));
            System.out.println(response);
            var respObj = JSONObject.parseObject(response);
            int code = respObj.getInteger("code");
            if(code!=0){
                throw new LogisticsLabelPrintException(respObj.getString("msg"));
            }
            var datas = respObj.getJSONObject("data").getJSONArray("datas");
            if(datas == null || datas.isEmpty()) {
                return null;
            }

            for (int i = 0; i < datas.size(); i++) {
                var ioOrder = datas.getJSONObject(i);
                var status = ioOrder.getString("status");
                // 找到已出库的出库单，聚水潭的这个状态设计其实挺迷惑的，他们的“出库单”包含：WaitConfirm=待出库;Confirmed=已出库;
                // Delete=作废(发货前取消订单);Cancelled=取消（发货后撤销发货）;OuterConfirming外部发货中；Archive已结算
                if("WaitConfirm".equals(status) || "Confirmed".equals(status)) {
                    //找出有效的，忽略其他作废的
                    log.log(System.Logger.Level.INFO, "发现有效出货单， status:"+status);
                    return ioOrder;
                }
            }
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            throw new LogisticsLabelPrintException(e.getMessage());
        }
    }

    private void auditOrder(String oId, WebSimulator webSimulator) {
        log.log(System.Logger.Level.INFO,"---------------------------订单审核------------------------------");
        var apiUrl = "https://www.erp321.com/app/order/order/list.aspx?_c=jst-epaas&epaas=true&am___=ConfirmCheckOids&ts___="+System.currentTimeMillis();
        var viewState = webSimulator.fetchViewState(apiUrl);

        var form = HttpDataBuilder.buildForm(Map.of(
                "__VIEWSTATE", viewState.getViewState(),
                "__VIEWSTATEGENERATOR", viewState.getViewStateGenerator(),
                "__CALLBACKID", "JTable1",
                "__CALLBACKPARAM", "{\"Method\":\"ConfirmCheckOids\",\"Args\":[\""+oId+"\"],\"CallControl\":\"{page}\"}"
        ));
        var expressSetterResult = webSimulator.postForm(apiUrl, form, apiUrl);
        log.log(System.Logger.Level.INFO, expressSetterResult.body());
    }
}
