package com.piliofpala.craftstudio.pangolin.infra.gatherplatform.jst;

import com.piliofpala.craftstudio.pangolin.domain.ecommerceorder.exception.GatherOrderException;
import com.piliofpala.craftstudio.pangolin.domain.ecommerceorder.vo.EcommerceOrder;
import com.piliofpala.craftstudio.pangolin.domain.gatherplatform.platform.vo.GatherPlatformType;
import com.piliofpala.craftstudio.pangolin.infra.gatherplatform.GatherPlatform;
import org.junit.jupiter.api.Test;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class GatherOrderTest {

    @Test
    public void gatherOrders() {
        try {
            var result = GatherPlatform.getInstance(GatherPlatformType.JST).gatherOrdersByTs(1);
            System.out.println("orders size:" + result.getEcommerceOrders().size());
            System.out.println("current max ts:" + result.getCurrentMaxTs());
        }catch (GatherOrderException e) {
            e.printStackTrace();
        }
    }
}
