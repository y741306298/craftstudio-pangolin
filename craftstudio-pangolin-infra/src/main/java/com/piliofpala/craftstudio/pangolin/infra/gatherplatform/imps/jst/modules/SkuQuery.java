package com.piliofpala.craftstudio.pangolin.infra.gatherplatform.imps.jst.modules;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.piliofpala.craftstudio.pangolin.domain.gatherplatform.product.entity.ProductSpec;
import com.piliofpala.craftstudio.pangolin.infra.gatherplatform.imps.jst.client.JstClient;

import java.io.IOException;
import java.util.Map;

public class SkuQuery {
    public ProductSpec queryProductSpecByCode(JstClient client, String specCode) {
        try {
            String response = client.execute(
                    "sku/query", JSON.toJSONString(Map.of("sku_ids", specCode))
            );
            System.out.println(response);
            var respObj = JSONObject.parseObject(response);
            var specs = respObj.getJSONObject("data").getJSONArray("datas");
            if(specs == null || specs.size()!=1){
                return null;
            }
            var spec = specs.getJSONObject(0);

            return new ProductSpec(
                    spec.getString("name"), spec.getString("sku_code"),
                    spec.getString("pic"), spec.getString("brand")
            );
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }
}
