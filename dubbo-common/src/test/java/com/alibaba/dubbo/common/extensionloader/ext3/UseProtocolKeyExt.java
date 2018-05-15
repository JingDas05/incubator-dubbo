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
package com.alibaba.dubbo.common.extensionloader.ext3;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.Adaptive;
import com.alibaba.dubbo.common.extension.SPI;

@SPI("impl1")
public interface UseProtocolKeyExt {
    // protocol key is the second
    // key1 protocol 是 url中的key,这个方法会根据具体的value值，选择具体的实现类，并且调用相对应的方法
    // 如果key1=impl2，那么这个方法实际调用的是UseProtocolKeyExtImpl2中的echo()方法
    @Adaptive({"key1", "protocol"})
    String echo(URL url, String s);

    // protocol key is the first
    @Adaptive({"protocol", "key2"})
    String yell(URL url, String s);
}