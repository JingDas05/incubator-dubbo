package com.alibaba.dubbo.demo.mock;

import com.alibaba.dubbo.demo.DemoService;

/**
 * 本地伪装通常用于服务降级，比如某验权服务，当服务提供方全部挂掉后，客户端不抛出异常，而是通过 Mock 数据返回授权失败。
 *
 * @author wusi
 * @version 2018/8/1.
 */
public class DemoServiceMock implements DemoService {
    @Override
    public String sayHello(String name) {
        return "容错数据";
    }
}
