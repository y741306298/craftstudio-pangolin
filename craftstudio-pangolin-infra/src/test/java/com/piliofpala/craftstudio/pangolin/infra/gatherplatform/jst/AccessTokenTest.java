package com.piliofpala.craftstudio.pangolin.infra.gatherplatform.jst;

import com.piliofpala.craftstudio.pangolin.domain.gatherplatform.platform.vo.GatherPlatformType;
import com.piliofpala.craftstudio.pangolin.infra.gatherplatform.GatherPlatform;
import org.junit.jupiter.api.Test;

public class AccessTokenTest {
    @Test
    public void testAccessToken() throws InterruptedException {
        GatherPlatform.getInstance(GatherPlatformType.JST).dumpInfo();
        Thread.sleep(1000*2);
        GatherPlatform.getInstance(GatherPlatformType.JST).dumpInfo();
        GatherPlatform.getInstance(GatherPlatformType.JST).dumpInfo();

    }
}
