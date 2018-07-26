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
package com.alibaba.dubbo.registry;

import com.alibaba.dubbo.common.URL;

import java.util.List;

/**
 * NotifyListener. (API, Prototype, ThreadSafe)
 * 线程安全，如果有新服务注册，调用notify(),入参：已注册且具有相同服务的服务列表
 *
 * @see com.alibaba.dubbo.registry.RegistryService#subscribe(URL, NotifyListener)
 */
public interface NotifyListener {

    /**
     * Triggered when a service change notification is received.
     * <p>
     * Notify needs to support the contract: <br>
     * 1. Always notifications on the service interface and the dimension of the data type. that is,
     * won't notify part of the same type data belonging to one service.
     * Users do not need to compare the results of the previous notification.<br>
     * <p>
     * 2. The first notification at a subscription must be a full notification of all types of data of a service.<br>
     * <p>
     * 3. At the time of change, different types of data are allowed to be notified separately,
     * e.g.: providers, consumers, routers, overrides. It allows only one of these types to be notified,
     * but the data of this type must be full, not incremental.<br>
     * <p>
     * 4. If a data type is empty, need to notify a empty protocol with category parameter identification of url data.<br>
     * <p>
     * 5. The order of notifications to be guaranteed by the notifications(That is, the implementation of the registry).
     * Such as: single thread push, queue serialization, and version comparison.<br>
     *
     * @param urls The list of registered information , is always not empty. The meaning is the same as the return value of {@link com.alibaba.dubbo.registry.RegistryService#lookup(URL)}.
     */
    void notify(List<URL> urls);

}