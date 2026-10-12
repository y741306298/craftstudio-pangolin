package com.piliofpala.craftstudio.pangolin.infra.gatherplatform.imps.jst.modules;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.piliofpala.craftstudio.pangolin.domain.gatherplatform.product.entity.ProductSpec;
import com.piliofpala.craftstudio.pangolin.domain.gatherplatform.supplier.entity.Supplier;
import com.piliofpala.craftstudio.pangolin.infra.gatherplatform.imps.jst.client.JstClient;

import java.io.IOException;
import java.util.Map;

public class SupplierQuery {
    public Supplier queryBySupplierId(JstClient client, String supplierId) {
        try {
            String response = client.execute(
                    "api/drp/inneropen/partner/channel/querymysupplier",
                    JSON.toJSONString(Map.of("supplier_co_id", supplierId)
                    )
            );
            System.out.println(response);
            var respObj = JSONObject.parseObject(response);
            var specs = respObj.getJSONObject("data").getJSONArray("supplier_vos");
            if(specs == null || specs.size()!=1){
                return null;
            }
            var supplierObj = specs.getJSONObject(0);

            var supplier = new Supplier();
            supplier.setId(supplierObj.getString("11052516"));
            supplier.setName(supplierObj.getString("co_name"));
            return supplier;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }
}
