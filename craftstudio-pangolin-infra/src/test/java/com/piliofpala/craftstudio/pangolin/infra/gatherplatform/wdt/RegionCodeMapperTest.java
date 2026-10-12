package com.piliofpala.craftstudio.pangolin.infra.gatherplatform.wdt;

import com.piliofpala.craftstudio.pangolin.infra.gatherplatform.imps.wdt.modules.RegionCodeMapper;
import org.junit.jupiter.api.Test;

public class RegionCodeMapperTest {
    @Test
    public void testMap(){
        assert "5205".equals(RegionCodeMapper.map("5224"));
        assert "110102".equals(RegionCodeMapper.map("110104"));
        assert "130284".equals(RegionCodeMapper.map("130285"));
        assert "659010".equals(RegionCodeMapper.map("659078"));
    }
}
