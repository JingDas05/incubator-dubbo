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
package com.alibaba.dubbo.monitor.support;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.monitor.Monitor;
import com.alibaba.dubbo.monitor.MonitorFactory;
import com.alibaba.dubbo.monitor.MonitorService;
import com.alibaba.dubbo.rpc.Filter;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcContext;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.support.RpcUtils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MonitorFilter. (SPI, Singleton, ThreadSafe)
 * <p>
 * * do invoke filter.
 * * <p>
 * * <code>
 * * // before filter
 * * Result result = invoker.invoke(invocation);
 * * // after filter
 * * return result;
 * * </code>
 * <p>
 * 监控是通过 过滤器实现的，这个是在暴露协议的时候，buildInvokerChain方法中建立的Filter
 */
@Activate(group = {Constants.PROVIDER, Constants.CONSUMER})
public class MonitorFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(MonitorFilter.class);

    private final ConcurrentMap<String, AtomicInteger> concurrents = new ConcurrentHashMap<String, AtomicInteger>();

    // 如果不引用如下依赖，monitorFactory 为空
    //         <dependency>
    //            <groupId>com.alibaba</groupId>
    //            <artifactId>dubbo-monitor-default</artifactId>
    //            <version>2.6.2</version>
    //        </dependency>
    // MonitorFactory 主要用于获取监控服务
    private MonitorFactory monitorFactory;

    public void setMonitorFactory(MonitorFactory monitorFactory) {
        this.monitorFactory = monitorFactory;
    }

    // intercepting invocation
    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        // 开启监控的话，才进行监控
        if (invoker.getUrl().hasParameter(Constants.MONITOR_KEY)) {
            RpcContext context = RpcContext.getContext(); // provider must fetch context before invoke() gets called
            String remoteHost = context.getRemoteHost();
            // record start timestamp
            long start = System.currentTimeMillis();
            // count up, 自增版本号
            getConcurrent(invoker, invocation).incrementAndGet();
            try {
                // proceed invocation chain
                Result result = invoker.invoke(invocation);
                // 收集 正常 监控数据
                collect(invoker, invocation, result, remoteHost, start, false);
                return result;
            } catch (RpcException e) {
                // 收集 异常 监控数据
                collect(invoker, invocation, null, remoteHost, start, true);
                throw e;
            } finally {
                // count down 自减版本号
                getConcurrent(invoker, invocation).decrementAndGet();
            }
        } else {
            return invoker.invoke(invocation);
        }
    }

    // collect info
    // error是否发生错误
    private void collect(Invoker<?> invoker, Invocation invocation, Result result, String remoteHost, long start, boolean error) {
        try {
            // ---- service statistics ----
            // invocation cost
            long elapsed = System.currentTimeMillis() - start;
            // current concurrent count
            int concurrent = getConcurrent(invoker, invocation).get();
            String application = invoker.getUrl().getParameter(Constants.APPLICATION_KEY);
            String service = invoker.getInterface().getName(); // service name
            String method = RpcUtils.getMethodName(invocation); // method name
            String group = invoker.getUrl().getParameter(Constants.GROUP_KEY);
            String version = invoker.getUrl().getParameter(Constants.VERSION_KEY);
            // 这个是配置在配置文件中的配置

            //<!--dubbo-monitor 监控需要的配置-->
            // <dubbo:monitor protocol="registry" />

            //  dubbo://127.0.0.1:2181/com.alibaba.dubbo.registry.RegistryService?application=demoConsumer&dubbo=2.0.0&
            // pid=7416&protocol=registry&qos.port=33335&refer=dubbo%3D2.0.0%26
            // interface%3Dcom.alibaba.dubbo.monitor.MonitorService%26pid%3D7416%26timestamp%3D1533524515896&registry=zookeeper&timestamp=1533524515872
            URL url = invoker.getUrl().getUrlParameter(Constants.MONITOR_KEY);
            // 如果不引用如下依赖，monitorFactory 为空
            //         <dependency>
            //            <groupId>com.alibaba</groupId>
            //            <artifactId>dubbo-monitor-default</artifactId>
            //            <version>2.6.2</version>
            //        </dependency>

            // DubboMonitor
            Monitor monitor = monitorFactory.getMonitor(url);
            if (monitor == null) {
                return;
            }
            int localPort;
            String remoteKey;
            String remoteValue;
            if (Constants.CONSUMER_SIDE.equals(invoker.getUrl().getParameter(Constants.SIDE_KEY))) {
                // ---- for service consumer ----
                // 处理消费端监控
                localPort = 0;
                remoteKey = MonitorService.PROVIDER;
                remoteValue = invoker.getUrl().getAddress();
            } else {
                // ---- for service provider ----
                // 处理服务端监控
                localPort = invoker.getUrl().getPort();
                remoteKey = MonitorService.CONSUMER;
                remoteValue = remoteHost;
            }
            String input = "", output = "";
            if (invocation.getAttachment(Constants.INPUT_KEY) != null) {
                input = invocation.getAttachment(Constants.INPUT_KEY);
            }
            if (result != null && result.getAttachment(Constants.OUTPUT_KEY) != null) {
                output = result.getAttachment(Constants.OUTPUT_KEY);
            }
            // path 是 service + "/" + method
            // count://192.168.73.1:20881/com.alibaba.dubbo.demo.DemoService/sayHello?application=demoProvider&concurrent=1&
            // consumer=192.168.73.1&elapsed=0&group=&input=215&interface=com.alibaba.dubbo.demo.DemoService&method=sayHello&output=&success=1&version=
            monitor.collect(new URL(Constants.COUNT_PROTOCOL,
                    NetUtils.getLocalHost(), localPort,
                    service + "/" + method,
                    MonitorService.APPLICATION, application,
                    MonitorService.INTERFACE, service,
                    MonitorService.METHOD, method,
                    remoteKey, remoteValue,
                    error ? MonitorService.FAILURE : MonitorService.SUCCESS, "1",
                    MonitorService.ELAPSED, String.valueOf(elapsed),
                    MonitorService.CONCURRENT, String.valueOf(concurrent),
                    Constants.INPUT_KEY, input,
                    Constants.OUTPUT_KEY, output,
                    Constants.GROUP_KEY, group,
                    Constants.VERSION_KEY, version));
        } catch (Throwable t) {
            logger.error("Failed to monitor count service " + invoker.getUrl() + ", cause: " + t.getMessage(), t);
        }
    }

    // concurrent counter
    private AtomicInteger getConcurrent(Invoker<?> invoker, Invocation invocation) {
        // 根据接口名字+方法名 返回 key
        String key = invoker.getInterface().getName() + "." + invocation.getMethodName();
        AtomicInteger concurrent = concurrents.get(key);
        if (concurrent == null) {
            concurrents.putIfAbsent(key, new AtomicInteger());
            concurrent = concurrents.get(key);
        }
        return concurrent;
    }

}
