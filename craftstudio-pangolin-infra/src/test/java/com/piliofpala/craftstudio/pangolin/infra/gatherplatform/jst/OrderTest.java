package com.piliofpala.craftstudio.pangolin.infra.gatherplatform.jst;

import com.piliofpala.craftstudio.pangolin.domain.gatherplatform.platform.vo.GatherPlatformType;
import com.piliofpala.craftstudio.pangolin.infra.gatherplatform.GatherPlatform;
import com.piliofpala.craftstudio.pangolin.infra.gatherplatform.imps.jst.client.JstClient;
import com.piliofpala.craftstudio.pangolin.infra.gatherplatform.imps.jst.modules.OrderQuery;
import org.junit.jupiter.api.Test;

public class OrderTest {
    @Test
    public void queryOrder() {
        var client = new JstClient(
                "3a25371919074892be91a0485a1c5cb1",
                "4e490e53704b4b14b3c063bf0eba4a41",
                "https://openapi.jushuitan.com/"
        );
        new OrderQuery().queryOrder(client,"538261");
    }
}
