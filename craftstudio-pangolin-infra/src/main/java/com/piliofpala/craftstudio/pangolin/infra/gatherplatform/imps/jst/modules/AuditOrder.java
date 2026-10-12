package com.piliofpala.craftstudio.pangolin.infra.gatherplatform.imps.jst.modules;

import com.alibaba.fastjson2.JSONObject;
import com.piliofpala.craftstudio.pangolin.domain.logistics.exception.LogisticsWarehouseConfigException;
import com.piliofpala.craftstudio.pangolin.infra.gatherplatform.imps.jst.client.JstClient;

import java.io.IOException;
import java.util.Map;

public class AuditOrder {
    public void audit(JstClient client, String oid) throws LogisticsWarehouseConfigException {
        try {
//            String response = client.execute("webapi/wmsapi/checkoutopen/singleorderconfirms", JSONObject.toJSONString(
//                    new Object[]{Map.of("o_id", oid, "wms_co_id", warehouseId)}
//            ));
//            System.out.println(response);
//            var respObj = JSONObject.parseObject(response);
//            int code = respObj.getInteger("code");
//            if(code!=0){
//                throw new LogisticsWarehouseConfigException(respObj.getString("msg"));
//            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new LogisticsWarehouseConfigException(e.getMessage());
        }
    }
}
