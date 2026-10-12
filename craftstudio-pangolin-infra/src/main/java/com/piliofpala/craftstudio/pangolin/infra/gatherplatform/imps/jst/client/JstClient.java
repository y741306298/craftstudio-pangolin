package com.piliofpala.craftstudio.pangolin.infra.gatherplatform.imps.jst.client;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.piliofpala.craftstudio.pangolin.infra.gatherplatform.imps.utils.WebUtils;
import com.piliofpala.craftstudio.shared.domain.base.exception.ServiceException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.util.*;

public class JstClient {
    private String appKey;
    private String appSecret;
    private String baseUrl;

    private String accessToken;
    private Date accessTokenExpiredAt;

    private final int connectTimeout = 3000;//3秒
    private final int readTimeout = 15000;//15秒
    private static final System.Logger log = System.getLogger("craftstudio-pangolin");


    public JstClient(String appKey, String appSecret, String baseUrl)
    {
        this.appKey = appKey;
        this.appSecret = appSecret;
        this.baseUrl = baseUrl;

        if(!this.baseUrl.endsWith("/")){
            this.baseUrl = this.baseUrl+"/";
        }
        //fetchAccessToken();//初始化预加载accessToken
    }

    public String execute(String relativeUrl, String biz) throws IOException {
        Map<String, String> params = new HashMap<>();
        params.put("app_key", this.appKey);
        params.put("access_token", fetchAccessToken());
        params.put("timestamp", Long.toString(System.currentTimeMillis()/1000));
        params.put("charset", "utf-8");
        params.put("version", "2");
        params.put("biz", biz);
        params.put("sign", getSign(this.appSecret, params));
        String apiUrl = this.baseUrl + "open/" + relativeUrl;
        log.log(System.Logger.Level.INFO,apiUrl);
        log.log(System.Logger.Level.INFO, params);
        return WebUtils.doPost(apiUrl, params, "UTF-8", connectTimeout, readTimeout, null);

    }

    public static String getSign(String appSecret, Map<String, String> params) {
        try {
            String sortedStr = getSortedParamStr(params);
            String paraStr = appSecret + sortedStr;

            return createSign(paraStr);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return null;
    }

    public String fetchAccessToken() {
        if (accessToken != null && accessTokenExpiredAt!=null) {
            long diffMillis = accessTokenExpiredAt.getTime() - System.currentTimeMillis();
            long diffHours = diffMillis / (1000 * 60 * 60);
            log.log(System.Logger.Level.INFO,"距离过期还有："+diffHours+"小时");
            if(diffHours>12) {
                return accessToken;
            }
        }
        log.log(System.Logger.Level.INFO,"重新获取accessToken");
        Map<String, String> params = new HashMap<>();
        params.put("app_key", this.appKey);
        params.put("grant_type", "authorization_code");
        params.put("timestamp", Long.toString(System.currentTimeMillis()/1000));
        params.put("charset", "utf-8");
        params.put("code", "4xFIOC");
        params.put("sign", getSign(this.appSecret, params));
        try {
            String result = WebUtils.doPost(
                    getInitTokenApiUrl(),
                    params, "UTF-8", connectTimeout, readTimeout, null
            );
            var resultObject = JSONObject.parse(result);
            if(resultObject.getInteger("code")!=0) {
                throw new ServiceException("聚水潭服务暂不可用:"+resultObject.getString("msg"));
            }
            accessToken = resultObject.getJSONObject("data").getString("access_token");
            var expiresIn = resultObject.getJSONObject("data").getLong("expires_in");
            accessTokenExpiredAt = new Date(System.currentTimeMillis() + expiresIn * 1000);
            return accessToken;
        } catch (IOException e) {
            throw new ServiceException("聚水潭服务暂不可用");
        }
    }

    /**
     * 构造自然排序请求参数
     *
     * @param params 请求
     * @return 字符串
     */
    private static String getSortedParamStr(Map<String, String> params) throws UnsupportedEncodingException {
        Set<String> sortedParams = new TreeSet<>(params.keySet());

        StringBuilder strB = new StringBuilder();
        // 排除sign和空值参数
        for (String key : sortedParams) {
            if ("sign".equalsIgnoreCase(key)) {
                continue;
            }

            String value = params.get(key);

            if (value!=null && !"".equals(value)) {
                strB.append(key).append(value);
            }
        }
        return strB.toString();
    }

    /**
     * 生成新sign
     *
     * @param str 字符串
     * @return String
     */
    private static String createSign(String str) {
        if (str == null || str.length() == 0) {
            return null;
        }
        char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        try {
            MessageDigest mdTemp = MessageDigest.getInstance("MD5");
            mdTemp.update(str.getBytes("UTF-8"));

            byte[] md = mdTemp.digest();
            int j = md.length;
            char[] buf = new char[j * 2];
            int k = 0;
            int i = 0;
            while (i < j) {
                byte byte0 = md[i];
                buf[k++] = hexDigits[byte0 >>> 4 & 0xf];
                buf[k++] = hexDigits[byte0 & 0xf];
                i++;
            }
            return new String(buf);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getInitTokenApiUrl(){
        if(baseUrl.contains("dev-api")){
            return this.baseUrl + "openWebIsv/auth/getInitToken";
        }else {
            return this.baseUrl + "openWeb/auth/getInitToken";
        }
    }
}
