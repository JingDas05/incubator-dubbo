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
package com.alibaba.dubbo.rpc.proxy.javassist;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.bytecode.Proxy;
import com.alibaba.dubbo.common.bytecode.Wrapper;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.proxy.AbstractProxyFactory;
import com.alibaba.dubbo.rpc.proxy.AbstractProxyInvoker;
import com.alibaba.dubbo.rpc.proxy.InvokerInvocationHandler;

/**
 * JavaassistRpcProxyFactory
 */
public class JavassistProxyFactory extends AbstractProxyFactory {

    // 重写 AbstractProxyFactory 中的 getProxy(Invoker<T> invoker, Class<?>[] interfaces)
    // interfaces的一部分是从invoker的url 中的interfaces获取的
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getProxy(Invoker<T> invoker, Class<?>[] interfaces) {
        return (T) Proxy.getProxy(interfaces).newInstance(new InvokerInvocationHandler(invoker));
    }

    /**
     * create invoker.
     *
     * @param <T> 服务的具体实现类，dubbo-demo中就是 DemoServiceImpl
     * @param proxy 服务的具体实现类的实例，dubbo-demo中就是 demoServiceImpl
     * @param type 服务接口，dubbo-demo中就是 DemoService
     * @param url 是注册中心的地址，在dubbo-demo中是 registry://224.5.6.7:1234/com.alibaba.dubbo.registry.RegistryService?
     *            application=demo-provider&dubbo=2.0.0&
     *            export=dubbo%3A%2F%2F192.168.73.1%3A20880%2Fcom.alibaba.dubbo.demo.DemoService%3Fanyhost%3Dtrue%26application%3Ddemo-provider%26bind.ip%3D192.168.73.1%26bind.port%3D20880%26dubbo%3D2.0.0%26generic%3Dfalse%26interface%3Dcom.alibaba.dubbo.demo.DemoService%26methods%3DsayHello%26pid%3D10348%26qos.port%3D22222%26side%3Dprovider%26timestamp%3D1527565064290&pid=10348&qos.port=22222&registry=multicast&timestamp=1527565064275
     * @return invoker
     */
    @Override
    public <T> Invoker<T> getInvoker(T proxy, Class<T> type, URL url) {
        // TODO Wrapper cannot handle this scenario correctly: the classname contains '$'
        // 含有$符号的是内部类 A$B.class,主类名为：A，内部类名为：B
        // Wrapper 是对类的包装，Wrapper封装了大部分类的常用操作 以及 Javassit 动态代理的封装
        // 如果是内部类，使用type， 如果不是内部类，使用 实例的class文件
        final Wrapper wrapper = Wrapper.getWrapper(proxy.getClass().getName().indexOf('$') < 0 ? proxy.getClass() : type);
        // 主要使用的 proxy type。 url 只是AbstractProxyInvoker中的一个字段， 在 export 中使用
        return new AbstractProxyInvoker<T>(proxy, type, url) {
            @Override
            protected Object doInvoke(T proxy, String methodName,
                                      Class<?>[] parameterTypes,
                                      Object[] arguments) throws Throwable {
                return wrapper.invokeMethod(proxy, methodName, parameterTypes, arguments);
            }
        };
    }
}
