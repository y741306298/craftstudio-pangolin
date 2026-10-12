package com.piliofpala.craftstudio.pangolin.infra.gatherplatform.imps.jst.modules.simulator;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

import java.io.IOException;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class CookieHolder {
    private static final System.Logger log = System.getLogger("craftstudio-pangolin");

    public void saveCookieFile(CookieManager cookieManager) {
        var savePath = cookieSavePath();
        String jsonStr = cookieJsonStr(cookieManager);
        try {
            if(!Files.exists(savePath.getParent())) {
                Files.createDirectories(savePath.getParent());
            }
            log.log(System.Logger.Level.INFO, "更新保存cookie信息");
            log.log(System.Logger.Level.INFO, jsonStr);
            Files.writeString(savePath, jsonStr);
        } catch (IOException e) {
            e.printStackTrace();
            log.log(System.Logger.Level.ERROR, e.getMessage());
        }
    }

    public void cleanCookieFile() {
        var savePath = cookieSavePath();
        if(!Files.exists(savePath)) {
            return;
        }
        try {
            Files.delete(savePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * {
     *     "discard": false,
     *     "domain": "www.erp321.com",
     *     "httpOnly": true,
     *     "maxAge": 1800,
     *     "name": "acw_tc",
     *     "path": "/",
     *     "secure": false,
     *     "value": "2760775117900593530165852efdbab583fc05df9b0eb213155c402c1ddbf7",
     *     "version": 1
     *   }
     * @return
     */
    public List<HttpCookie> loadCookies() {
        var savePath = cookieSavePath();
        if(!Files.exists(savePath)) {
            return null;
        }
        try {
            String jsonStr = Files.readString(savePath);
            var array = JSONArray.parseArray(jsonStr, JSONObject.class);
            var cookies = new ArrayList<HttpCookie>();
            for (JSONObject jsonObject : array) {
                var cookie = new HttpCookie(jsonObject.getString("name"), jsonObject.getString("value"));
                cookie.setDiscard(jsonObject.getBoolean("discard"));
                cookie.setDomain(jsonObject.getString("domain"));
                cookie.setHttpOnly(jsonObject.getBoolean("httpOnly"));
                cookie.setMaxAge(jsonObject.getLong("maxAge"));
                cookie.setPath(jsonObject.getString("path"));
                cookie.setSecure(jsonObject.getBoolean("secure"));
                cookie.setVersion(jsonObject.getInteger("version"));
                cookies.add(cookie);
            }
            return cookies;
        } catch (IOException e) {
            e.printStackTrace();
            log.log(System.Logger.Level.ERROR, e.getMessage());
            return null;
        }
    }

    private Path cookieSavePath () {
        return Path.of(System.getProperty("user.home"), ".craftstudio-ordering/jst-web-cookie.dat");
    }

    private String cookieJsonStr(CookieManager cookieManager) {
        var jsonStr = JSONArray.toJSONString(cookieManager.getCookieStore().getCookies());
        log.log(System.Logger.Level.INFO, jsonStr);
        return jsonStr;
    }
}
