package com.piliofpala.craftstudio.pangolin.infra.gatherplatform.jst;

import com.piliofpala.craftstudio.pangolin.domain.gatherplatform.auth.AuthException;
import com.piliofpala.craftstudio.pangolin.infra.gatherplatform.imps.jst.modules.simulator.WebSimulator;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class AuthTest {
    @Test
    @Disabled("影响真实数据，打包不予执行")
    public void smsCodeAuth() {
        try {
            WebSimulator.getLoggedInstance().obtainAuthSid("8203");
        } catch (AuthException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
