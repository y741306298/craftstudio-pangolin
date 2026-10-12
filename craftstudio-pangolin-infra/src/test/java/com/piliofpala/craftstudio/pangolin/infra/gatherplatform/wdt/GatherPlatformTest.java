package com.piliofpala.craftstudio.pangolin.infra.gatherplatform.wdt;

import com.piliofpala.craftstudio.pangolin.domain.gatherplatform.platform.vo.GatherPlatformType;
import com.piliofpala.craftstudio.pangolin.infra.gatherplatform.GatherPlatform;
import org.junit.jupiter.api.Test;


public class GatherPlatformTest {
    @Test
    public void envTest() {
        System.out.println(GatherPlatform.getInstance(GatherPlatformType.WDT).dumpInfo());
        System.out.println(GatherPlatform.getTestInstance(GatherPlatformType.WDT).dumpInfo());
    }
}
