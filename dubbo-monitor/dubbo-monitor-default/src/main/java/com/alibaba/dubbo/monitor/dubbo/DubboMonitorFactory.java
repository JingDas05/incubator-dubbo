/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.monitor.dubbo;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.monitor.Monitor;
import com.alibaba.dubbo.monitor.MonitorService;
import com.alibaba.dubbo.monitor.support.AbstractMonitorFactory;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Protocol;
import com.alibaba.dubbo.rpc.ProxyFactory;

/**
 * DefaultMonitorFactory
 */
public class DubboMonitorFactory extends AbstractMonitorFactory {

    private Protocol protocol;

    private ProxyFactory proxyFactory;

    public void setProtocol(Protocol protocol) {
        this.protocol = protocol;
    }

    public void setProxyFactory(ProxyFactory proxyFactory) {
        this.proxyFactory = proxyFactory;
    }

    // 核心方法
    @Override
    protected Monitor createMonitor(URL url) {
        url = url.setProtocol(url.getParameter(Constants.PROTOCOL_KEY, "dubbo"));
        if (url.getPath() == null || url.getPath().length() == 0) {
            url = url.setPath(MonitorService.class.getName());
        }
        String filter = url.getParameter(Constants.REFERENCE_FILTER_KEY);
        if (filter == null || filter.length() == 0) {
            filter = "";
        } else {
            filter = filter + ",";
        }
        url = url.addParameters(Constants.CLUSTER_KEY, "failsafe", Constants.CHECK_KEY, String.valueOf(false),
                Constants.REFERENCE_FILTER_KEY, filter + "-monitor");
        // 核心
        // 引用远程服务MonitorService,也即 dubbo-monitor上的服务，将本地的数据传送过去
        // 这个类打包在 dubbo-monitor-default中，消费者和服务者如果需要监控的话，需要依赖此包，进行初始化MonitorService代理

        // url registry://127.0.0.1:2181/com.alibaba.dubbo.monitor.MonitorService?application=demoProvider&check=false&
        // cluster=failsafe&dubbo=2.0.0&interface=com.alibaba.dubbo.monitor.MonitorService&pid=5564&protocol=registry&
        // qos.port=22222&refer=dubbo%3D2.0.0%26interface%3Dcom.alibaba.dubbo.monitor.MonitorService%26pid%3D5564%26
        // timestamp%3D1533548960687&reference.filter=-monitor&registry=zookeeper&timeout=30000&timestamp=1533548960483

        // TODO: 2018/8/6  引用远程服务核心方法
        // refer 注册中心上 监控平台注册上的服务，创建代理
        Invoker<MonitorService> monitorInvoker = protocol.refer(MonitorService.class, url);
        MonitorService monitorService = proxyFactory.getProxy(monitorInvoker);
        return new DubboMonitor(monitorInvoker, monitorService);
    }

}