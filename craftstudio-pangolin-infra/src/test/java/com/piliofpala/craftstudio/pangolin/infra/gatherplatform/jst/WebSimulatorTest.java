package com.piliofpala.craftstudio.pangolin.infra.gatherplatform.jst;

import com.piliofpala.craftstudio.pangolin.infra.gatherplatform.imps.jst.modules.simulator.CookieHolder;
import com.piliofpala.craftstudio.pangolin.infra.gatherplatform.imps.jst.modules.simulator.WebSimulator;
import org.junit.jupiter.api.Test;


public class WebSimulatorTest {
    @Test
    public void testCookie() {
        var webSimulator = WebSimulator.getLoggedInstance();
        var cookies = new CookieHolder().loadCookies();
        assert cookies!=null && !cookies.isEmpty();
    }

    @Test
    public void testLogout() {
        var webSimulator = WebSimulator.getLoggedInstance();
        webSimulator.logout();
    }

}
