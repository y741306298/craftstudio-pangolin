package com.piliofpala.craftstudio.pangolin.infra.gatherplatform.imps.jst.modules;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.piliofpala.craftstudio.pangolin.domain.gatherplatform.product.entity.ProductSpec;
import com.piliofpala.craftstudio.pangolin.domain.gatherplatform.product.exception.BrandNotExistException;
import com.piliofpala.craftstudio.pangolin.domain.gatherplatform.product.exception.ProductSpecPushException;
import com.piliofpala.craftstudio.pangolin.infra.gatherplatform.imps.jst.client.JstClient;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SkuPush {
    public void push(JstClient client, ProductSpec productSpec) throws BrandNotExistException, ProductSpecPushException {

        Map<String, Object>[] specs = new Map[1];

        specs[0] = new HashMap<>();
        specs[0].put("sku_id", productSpec.getCode());
        specs[0].put("sku_code", productSpec.getCode());
        specs[0].put("name", productSpec.getName());
        specs[0].put("pic", productSpec.getCover());
        specs[0].put("brand", productSpec.getBrand());
        specs[0].put("sale_price",0);
        specs[0].put("cost_price", 0);

        try {
            String response = client.execute(
                    "webapi/itemapi/itemsku/itemskubatchupload", JSON.toJSONString(Map.of("items",specs))
            );
            System.out.println(response);
            var respObj = JSONObject.parseObject(response);
            int code = respObj.getInteger("code");
            if(code!=0){
                if(code == 7210){
                    throw new BrandNotExistException(respObj.getString("msg"));
                }else {
                    throw new ProductSpecPushException(respObj.getString("msg"));
                }
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            throw new ProductSpecPushException(e.getMessage());
        }
    }
}
