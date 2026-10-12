package com.piliofpala.craftstudio.pangolin.infra.gatherplatform.jst;

import com.piliofpala.craftstudio.pangolin.domain.gatherplatform.platform.vo.GatherPlatformType;
import com.piliofpala.craftstudio.pangolin.infra.gatherplatform.GatherPlatform;
import org.junit.jupiter.api.Test;

public class SupplierTest {
    @Test
    public void supplierQueryTest() {
        GatherPlatform.getInstance(GatherPlatformType.JST).queryShopByCode("11052516");
    }
}
