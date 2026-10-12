package com.piliofpala.craftstudio.pangolin.infra.gatherplatform.jst;

import com.piliofpala.craftstudio.pangolin.infra.gatherplatform.imps.jst.modules.simulator.AspDataPackageParser;
import org.junit.jupiter.api.Test;

public class AspDataPackageParserTest {
    @Test
    public void testAspDataJsonParse() {
        var jsonObj = new AspDataPackageParser().parseToJson("0|{\"IsSuccess\":true,\"ExceptionMessage\":null,\"ReturnValue\":\"L:{\\\"l7U1wlKiPXwzuQuvunFvCw==\\\":{\\\"Code\\\":\\\"WaybillUnOpenButForceUse\\\",\\\"Message\\\":\\\"拼多多平台面单未配置，拼多多订单必须使用拼多多面单,请到对应发货仓配置并启用，发货仓：POD1688sln_id=6199\\\",\\\"LIdTypeName\\\":null,\\\"SolutionId\\\":6199,\\\"LIdType\\\":\\\"p\\\",\\\"SessionShopId\\\":0}}\",\"ExceptionText\":null,\"GotoLogin\":false,\"ClientScript\":null,\"Message\":null,\"LocationUrl\":null,\"OpenUrl\":null,\"IsReloadPage\":false,\"IsReloadData\":false,\"IsResetData\":false,\"RequestId\":\"2760776817897117326722655e5fa2\"}\n");
        assert "WaybillUnOpenButForceUse".equals(jsonObj.getJSONObject("ReturnValue").getJSONObject("l7U1wlKiPXwzuQuvunFvCw==").getString("Code"));
    }
}
