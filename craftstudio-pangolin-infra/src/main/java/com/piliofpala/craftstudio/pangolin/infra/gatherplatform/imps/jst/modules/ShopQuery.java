package com.piliofpala.craftstudio.pangolin.infra.gatherplatform.imps.jst.modules;

import com.alibaba.fastjson2.JSONObject;
import com.piliofpala.craftstudio.pangolin.domain.gatherplatform.shop.entity.Shop;
import com.piliofpala.craftstudio.pangolin.domain.gatherplatform.shop.exception.ShopNotUniqueException;
import com.piliofpala.craftstudio.pangolin.infra.gatherplatform.imps.jst.client.JstClient;

import java.io.IOException;
import java.util.Map;

public class ShopQuery {
    public Shop queryShopByCode(JstClient client, String shopCode) throws ShopNotUniqueException {

        try {
            String response = client.execute("shops/query", JSONObject.toJSONString(
                    Map.of("shop_ids", new String[]{shopCode})
            ));
            System.out.println(response);
            var respObj = JSONObject.parseObject(response);
            var shopList = respObj.getJSONObject("data").getJSONArray("datas");
            if(shopList == null || shopList.isEmpty()){
                return null;
            }
            if(shopList.size()!=1) {
                throw new ShopNotUniqueException("此编码下店铺不唯一，请联系相关业务人员处理");
            }
            var shopObj = shopList.getJSONObject(0);
            Shop shop = new Shop();
            shop.setId(shopObj.getString("shop_id"));
            shop.setCode(shopObj.getString("shop_id"));
            shop.setName(shopObj.getString("shop_name"));
            return shop;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }
}
