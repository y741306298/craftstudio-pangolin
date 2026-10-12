package com.piliofpala.craftstudio.pangolin.infra.gatherplatform.jst;

import com.piliofpala.craftstudio.pangolin.domain.gatherplatform.platform.vo.GatherPlatformType;
import com.piliofpala.craftstudio.pangolin.infra.gatherplatform.GatherPlatform;
import org.junit.jupiter.api.Test;

public class ShopTest {
    @Test
    public void queryShop(){
        var shop = GatherPlatform.getInstance(GatherPlatformType.JST).queryShopByCode("11052516");
        System.out.println(shop);
    }
}
