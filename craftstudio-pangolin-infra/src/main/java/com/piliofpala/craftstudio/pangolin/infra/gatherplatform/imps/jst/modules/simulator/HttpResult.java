package com.piliofpala.craftstudio.pangolin.infra.gatherplatform.imps.jst.modules.simulator;

import java.util.List;
import java.util.Map;

public record HttpResult(int statusCode, String body, Map<String, List<String>> headers) {}
