/**
 * Copyright (c) 2016-2019 人人开源 All rights reserved.
 * <p>
 * https://www.renren.io
 * <p>
 * 版权所有，侵权必究！
 */

package cn.nicenan.mahumall.common.utils;

import cn.nicenan.mahumall.common.exception.BizCodeEnume;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;

/**
 * 返回数据
 * R 继承了 HashMap 则不能继续使用泛型数据了 必须全是hashMap数据
 *
 * @author Mark sunlightcs@gmail.com
 */
public class R<T> extends HashMap<String, Object> {
    private static final long serialVersionUID = 1L;

    /**
     * 复杂类型转换 TypeReference
     */
    public T getData(TypeReference<T> typeReference) {
        // get("data") 默认是map类型 所以再由map转成string再转json
        Object data = get("data");
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            T t = objectMapper.readValue(objectMapper.writeValueAsString(data), typeReference);
            return t;
        } catch (Exception ex) {
            return null;
        }
    }

    public R setData(T data) {
        put("data", data);
        return this;
    }

    public R() {
        put("code", 0);
        put("msg", "success");
    }

    public static R error() {
        return error(HttpStatus.SC_INTERNAL_SERVER_ERROR, "未知异常，请联系管理员");
    }

    public static R error(String msg) {
        return error(HttpStatus.SC_INTERNAL_SERVER_ERROR, msg);
    }

    public static R error(int code, String msg) {
        R r = new R();
        r.put("code", code);
        r.put("msg", msg);
        return r;
    }

    public static R ok(String msg) {
        R r = new R();
        r.put("msg", msg);
        return r;
    }

    public static R ok(Map<String, Object> map) {
        R r = new R();
        r.putAll(map);
        return r;
    }

    public static R ok() {
        return new R();
    }

    @Override
    public R put(String key, Object value) {
        super.put(key, value);
        return this;
    }

    public static R useEnum(BizCodeEnume bizCodeEnume) {
        R r = new R();
        r.put("code", bizCodeEnume.getCode());
        r.put("msg", bizCodeEnume.getMsg());
        return r;
    }

    public Integer getCode() {
        return (Integer) this.get("code");
    }
}
