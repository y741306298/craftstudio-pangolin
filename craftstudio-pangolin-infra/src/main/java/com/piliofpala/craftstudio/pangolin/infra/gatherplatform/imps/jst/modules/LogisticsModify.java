package com.piliofpala.craftstudio.pangolin.infra.gatherplatform.imps.jst.modules;

import com.alibaba.fastjson2.JSONObject;
import com.piliofpala.craftstudio.pangolin.domain.logistics.exception.LogisticsWarehouseConfigException;
import com.piliofpala.craftstudio.pangolin.infra.gatherplatform.imps.jst.client.JstClient;

import java.io.IOException;
import java.util.Map;

public class LogisticsModify {
    public void modify(JstClient client, String logisticsId, String oid) throws LogisticsWarehouseConfigException{
        toWaitAudit(client, oid);//迷惑的聚水潭改快递前还需要将订单转为待审核状态
        try {
            String response = client.execute("webapi/orderapi/modifyorder/modifyorderlc", JSONObject.toJSONString(
                    Map.of("order_lcs",new Object[]{Map.of("o_id", oid, "lc_id", logisticsId)})

            ));
            System.out.println(response);
            var respObj = JSONObject.parseObject(response);
            int code = respObj.getInteger("code");
            if(code!=0){
                throw new LogisticsWarehouseConfigException(respObj.getString("msg"));
            }

            // 改完以后再审核回来
            new OrderAudit().auditOrder(oid);
        } catch (IOException e) {
            e.printStackTrace();
            throw new LogisticsWarehouseConfigException(e.getMessage());
        }
    }

    private void toWaitAudit(JstClient client, String oid) throws LogisticsWarehouseConfigException{
        try {
            String response = client.execute("webapi/orderapi/questionorder/questions", JSONObject.toJSONString(
                    Map.of(
                            "o_ids",new Object[]{oid},
                            "question_type", "转到待审核"
                    )

            ));
            System.out.println(response);
            var respObj = JSONObject.parseObject(response);
            int code = respObj.getInteger("code");
            if(code!=0){
                throw new LogisticsWarehouseConfigException(respObj.getString("msg"));
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new LogisticsWarehouseConfigException(e.getMessage());
        }
    }
}
