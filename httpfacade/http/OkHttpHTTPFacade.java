
import com.google.common.collect.Maps;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by stonenice on 2017/7/12.
 *
 * @author stonenice
 */

@Component("okhttp")
public class OkHttpHTTPFacade extends HTTPFacade {
    private Map<String, Object> headers;

    @Override
    public void sessionStart() {

    }

    @Override
    public void sessionDestory() {

    }

    @Override
    public Object getCookie(String key) {
        return null;
    }

    @Override
    public Object getSession(String key) {
        return null;
    }

    @Override
    public void setHeaders(Map<String, Object> headers) {
        this.headers = headers;
    }

    @Override
    public HTTPResult get(String url) {
        return get(url, null);
    }

    @Override
    public HTTPResult get(String url, Map<String, Object> params) {
        OkHttpClient client = new OkHttpClient();
        Request.Builder builder = new Request.Builder().get();
        if (headers != null && headers.size() > 0) {
            for (Map.Entry<String, Object> header : headers.entrySet()) {
                String key = header.getKey();
                if (StringUtils.isBlank(key)) continue;
                key = key.trim();
                Object value = header.getValue();
                String val = value != null ? value.toString() : null;
                builder.addHeader(key, val);
            }
        }
        String paramLine = null;
        JString jstr = JString.valueOf(url);
        List<String> holders = jstr.placeholders();

        if (params != null && params.size() > 0) {
            Map<String, Object> newParams = Maps.newHashMap();
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                String key = entry.getKey();
                key = key.trim();
                Object value = entry.getValue();
                String val = value != null ? value.toString() : null;
                if (holders != null && holders.size() > 0) {
                    if (!CollectionUtils.contains(holders.iterator(), key)) {
                        sb.append(key);
                        sb.append("=");
                        sb.append(val);
                        sb.append("&");
                    }
                } else {
                    sb.append(key);
                    sb.append("=");
                    sb.append(val);
                    sb.append("&");
                }
            }
            paramLine = StringUtils.stripEnd(sb.toString(), "&");
        }
        String urlline = jstr.toString(params);
        urlline = StringUtils.stripEnd(urlline, "?");
        urlline = StringUtils.isNotBlank(paramLine) ? urlline + "?" + paramLine : urlline;
        builder.url(urlline);
        try {
            HTTPResult result;
            Response response = client.newCall(builder.build()).execute();
            result = response.isSuccessful() ? HTTPResult.newSuccessResult() : HTTPResult.newErrorResult();
            result.setStatusCode(response.code());
            result.setBody(response.body().string());
            Headers respHeaders = response.headers();
            Map<String, Object> hmap = Maps.newHashMap();
            for (String key : respHeaders.names()) {
                hmap.put(key, respHeaders.get(key));
            }
            result.setHeaders(hmap);
            return result;
        } catch (Exception e) {
            return HTTPResult.newErrorResult(e.getMessage());
        }
    }

    @Override
    public HTTPResult post(String url, String rawRes) {
        OkHttpClient client = new OkHttpClient();
        Request.Builder builder = new Request.Builder();
        String contentType = null;
        if (headers != null && headers.size() > 0) {
            for (Map.Entry<String, Object> header : headers.entrySet()) {
                String key = header.getKey();
                if (StringUtils.isBlank(key)) continue;
                key = key.trim();
                Object value = header.getValue();
                String val = value != null ? value.toString() : null;
                builder.addHeader(key, val);
                if ("content-type".equals(key.toLowerCase().trim())) {
                    contentType = val;
                }
            }
        }

        if (StringUtils.isBlank(contentType)) {
            contentType = "text/html; charset=utf-8";
        }

        builder.url(url);
        RequestBody body = RequestBody.create(MediaType.parse(contentType), rawRes);
        builder.post(body);
        try {
            HTTPResult result;
            Response response = client.newCall(builder.build()).execute();
            result = response.isSuccessful() ? HTTPResult.newSuccessResult() : HTTPResult.newErrorResult();
            result.setStatusCode(response.code());
            result.setBody(response.body().string());
            Headers respHeaders = response.headers();
            Map<String, Object> hmap = Maps.newHashMap();
            for (String key : respHeaders.names()) {
                hmap.put(key, respHeaders.get(key));
            }
            result.setHeaders(hmap);
            return result;
        } catch (Exception e) {
            return HTTPResult.newErrorResult(e.getMessage());
        }
    }

    @Override
    public HTTPResult post(String url, Map<String, Object> params) {
        OkHttpClient client = new OkHttpClient();
        Request.Builder builder = new Request.Builder();
        if (headers != null && headers.size() > 0) {
            for (Map.Entry<String, Object> header : headers.entrySet()) {
                String key = header.getKey();
                if (StringUtils.isBlank(key)) continue;
                key = key.trim();
                Object value = header.getValue();
                String val = value != null ? value.toString() : null;
                builder.addHeader(key, val);
            }
        }
        JString jstr = JString.valueOf(url);
        List<String> holders = jstr.placeholders();
        builder.url(jstr.toString(params));
        FormBody.Builder form = new FormBody.Builder();
        if (params != null && params.size() > 0) {
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                String key = entry.getKey();
                key = key.trim();
                Object value = entry.getValue();
                String val = value != null ? value.toString() : null;
                if (holders != null && holders.size() > 0) {
                    if (!CollectionUtils.contains(holders.iterator(), key)) {
                        form.add(key, val);
                    }
                } else {
                    form.add(key, val);
                }
            }
        }
        builder.post(form.build());
        try {
            HTTPResult result;
            Response response = client.newCall(builder.build()).execute();
            result = response.isSuccessful() ? HTTPResult.newSuccessResult() : HTTPResult.newErrorResult();
            result.setStatusCode(response.code());
            result.setBody(response.body().string());
            Headers respHeaders = response.headers();
            Map<String, Object> hmap = Maps.newHashMap();
            for (String key : respHeaders.names()) {
                hmap.put(key, respHeaders.get(key));
            }
            result.setHeaders(hmap);
            return result;
        } catch (Exception e) {
            return HTTPResult.newErrorResult(e.getMessage());
        }
    }

    @Override
    public HTTPResult put(String url, String rawRes) {
        OkHttpClient client = new OkHttpClient();
        Request.Builder builder = new Request.Builder();
        String contentType = null;
        if (headers != null && headers.size() > 0) {
            for (Map.Entry<String, Object> header : headers.entrySet()) {
                String key = header.getKey();
                if (StringUtils.isBlank(key)) continue;
                key = key.trim();
                Object value = header.getValue();
                String val = value != null ? value.toString() : null;
                builder.addHeader(key, val);
                if ("content-type".equals(key.toLowerCase().trim())) {
                    contentType = val;
                }
            }
        }

        if (StringUtils.isBlank(contentType)) {
            contentType = "text/html; charset=utf-8";
        }

        builder.url(url);
        RequestBody body = RequestBody.create(MediaType.parse(contentType), rawRes);
        builder.put(body);
        try {
            HTTPResult result;
            Response response = client.newCall(builder.build()).execute();
            result = response.isSuccessful() ? HTTPResult.newSuccessResult() : HTTPResult.newErrorResult();
            result.setStatusCode(response.code());
            result.setBody(response.body().string());
            Headers respHeaders = response.headers();
            Map<String, Object> hmap = Maps.newHashMap();
            for (String key : respHeaders.names()) {
                hmap.put(key, respHeaders.get(key));
            }
            result.setHeaders(hmap);
            return result;
        } catch (Exception e) {
            return HTTPResult.newErrorResult(e.getMessage());
        }
    }

    @Override
    public HTTPResult put(String url, Map<String, Object> params) {
        OkHttpClient client = new OkHttpClient();
        Request.Builder builder = new Request.Builder();
        if (headers != null && headers.size() > 0) {
            for (Map.Entry<String, Object> header : headers.entrySet()) {
                String key = header.getKey();
                if (StringUtils.isBlank(key)) continue;
                key = key.trim();
                Object value = header.getValue();
                String val = value != null ? value.toString() : null;
                builder.addHeader(key, val);
            }
        }
        JString jstr = JString.valueOf(url);
        List<String> holders = jstr.placeholders();
        builder.url(jstr.toString(params));
        FormBody.Builder form = new FormBody.Builder();
        if (params != null && params.size() > 0) {
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                String key = entry.getKey();
                key = key.trim();
                Object value = entry.getValue();
                String val = value != null ? value.toString() : null;
                if (holders != null && holders.size() > 0) {
                    if (!CollectionUtils.contains(holders.iterator(), key)) {
                        form.add(key, val);
                    }
                } else {
                    form.add(key, val);
                }
            }
        }
        builder.put(form.build());
        try {
            HTTPResult result;
            Response response = client.newCall(builder.build()).execute();
            result = response.isSuccessful() ? HTTPResult.newSuccessResult() : HTTPResult.newErrorResult();
            result.setStatusCode(response.code());
            result.setBody(response.body().string());
            Headers respHeaders = response.headers();
            Map<String, Object> hmap = Maps.newHashMap();
            for (String key : respHeaders.names()) {
                hmap.put(key, respHeaders.get(key));
            }
            result.setHeaders(hmap);
            return result;
        } catch (Exception e) {
            return HTTPResult.newErrorResult(e.getMessage());
        }
    }

    @Override
    public HTTPResult delete(String url) {
        return delete(url, new HashMap<String, Object>());
    }

    @Override
    public HTTPResult delete(String url, String rawRes) {
        OkHttpClient client = new OkHttpClient();
        Request.Builder builder = new Request.Builder();
        String contentType = null;
        if (headers != null && headers.size() > 0) {
            for (Map.Entry<String, Object> header : headers.entrySet()) {
                String key = header.getKey();
                if (StringUtils.isBlank(key)) continue;
                key = key.trim();
                Object value = header.getValue();
                String val = value != null ? value.toString() : null;
                builder.addHeader(key, val);
                if ("content-type".equals(key.toLowerCase().trim())) {
                    contentType = val;
                }
            }
        }

        if (StringUtils.isBlank(contentType)) {
            contentType = "text/html; charset=utf-8";
        }

        builder.url(url);
        RequestBody body = RequestBody.create(MediaType.parse(contentType), rawRes);
        builder.delete(body);
        try {
            HTTPResult result;
            Response response = client.newCall(builder.build()).execute();
            result = response.isSuccessful() ? HTTPResult.newSuccessResult() : HTTPResult.newErrorResult();
            result.setStatusCode(response.code());
            result.setBody(response.body().string());
            Headers respHeaders = response.headers();
            Map<String, Object> hmap = Maps.newHashMap();
            for (String key : respHeaders.names()) {
                hmap.put(key, respHeaders.get(key));
            }
            result.setHeaders(hmap);
            return result;
        } catch (Exception e) {
            return HTTPResult.newErrorResult(e.getMessage());
        }
    }

    @Override
    public HTTPResult delete(String url, Map<String, Object> params) {
        OkHttpClient client = new OkHttpClient();
        Request.Builder builder = new Request.Builder();
        if (headers != null && headers.size() > 0) {
            for (Map.Entry<String, Object> header : headers.entrySet()) {
                String key = header.getKey();
                if (StringUtils.isBlank(key)) continue;
                key = key.trim();
                Object value = header.getValue();
                String val = value != null ? value.toString() : null;
                builder.addHeader(key, val);
            }
        }
        JString jstr = JString.valueOf(url);
        List<String> holders = jstr.placeholders();
        builder.url(jstr.toString(params));
        FormBody.Builder form = new FormBody.Builder();
        if (params != null && params.size() > 0) {
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                String key = entry.getKey();
                key = key.trim();
                Object value = entry.getValue();
                String val = value != null ? value.toString() : null;
                if (holders != null && holders.size() > 0) {
                    if (!CollectionUtils.contains(holders.iterator(), key)) {
                        form.add(key, val);
                    }
                } else {
                    form.add(key, val);
                }
            }
        }
        builder.delete(form.build());
        try {
            HTTPResult result;
            Response response = client.newCall(builder.build()).execute();
            result = response.isSuccessful() ? HTTPResult.newSuccessResult() : HTTPResult.newErrorResult();
            result.setStatusCode(response.code());
            result.setBody(response.body().string());
            Headers respHeaders = response.headers();
            Map<String, Object> hmap = Maps.newHashMap();
            for (String key : respHeaders.names()) {
                hmap.put(key, respHeaders.get(key));
            }
            result.setHeaders(hmap);
            return result;
        } catch (Exception e) {
            return HTTPResult.newErrorResult(e.getMessage());
        }
    }


}
