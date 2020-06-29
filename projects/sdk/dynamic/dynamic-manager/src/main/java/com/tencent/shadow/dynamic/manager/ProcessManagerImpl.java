/*
 * Tencent is pleased to support the open source community by making Tencent Shadow available.
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * Licensed under the BSD 3-Clause License (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *     https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.tencent.shadow.dynamic.manager;

import android.content.Context;

import com.tencent.shadow.core.common.Logger;
import com.tencent.shadow.core.common.LoggerFactory;
import com.tencent.shadow.dynamic.host.FailedException;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 进程管理
 */
public class ProcessManagerImpl implements ProcessManager {
    private static final Logger mLogger = LoggerFactory.getLogger(ProcessManagerImpl.class);

    private final String[] processNames;

    /**
     * 用来检测Service是否已经被占用
     * key:     PluginProcessService的实际类名
     * value:   {@link ProcessLoader}
     */
    private final HashMap<String, ProcessLoaderInternal> serviceUsedMap = new HashMap<>();

    /**
     * 用于快速加载uuid所使用的loader
     * key:     uuid
     * value:   {@link ProcessLoader}
     */
    private final HashMap<String, ProcessLoader> uuidUsedMap = new HashMap<>();

    /**
     * loader缓存
     */
    private final LinkedList<ProcessLoaderInternal> processLoaderCache = new LinkedList<>();

    private final Context hostContext;

    private final UuidManagerImpl uuidManager;

    public ProcessManagerImpl(Context hostContext,
                              String[] processNames,
                              UuidManagerImpl uuidManager) {
        this.hostContext = hostContext;
        this.processNames = processNames == null ? new String[]{} : processNames;
        this.uuidManager = uuidManager;
        mLogger.info("ProcessManager init");
    }

    @Override
    public ProcessLoader getProcessLoader(String uuid) {
        ProcessLoader processLoader = uuidUsedMap.get(uuid);
        mLogger.info("getProcessLoader uuid={}, ProcessLoader=={}", uuid, processLoader);
        return processLoader;
    }

    @Override
    public void initProcessLoaderSync(String uuid, int timeout, TimeUnit timeUnit) throws FailedException, TimeoutException {
        ProcessLoaderInternal processLoader = findOrCreate();
        String serviceName = findIdleServiceName();
        if (serviceName == null) {
            throw new FailedException(-1, "没有找到空闲的进程");
        }
        ProcessLoaderInternal.Partner partner = new ProcessLoaderInternal.Partner();
        partner.uuid = uuid;
        partner.serviceName = serviceName;

        processLoader.bindPluginProcessService(partner);
        processLoader.waitServiceConnected(timeout, timeUnit);
        mLogger.info("进程已占用 partner = {}", partner);
        serviceUsedMap.put(serviceName, processLoader);
        uuidUsedMap.put(uuid, processLoader);
    }

    private String findIdleServiceName() {
        ProcessLoaderInternal processLoader;
        for (String processName : processNames) {
            processLoader = serviceUsedMap.get(processName);
            if (processLoader == null) {
                mLogger.info("ProcessLoader未创建过，获取到一个空闲进程 serviceName={}", processName);
                return processName;
            } else if (processLoader.isNotAlive()) {
                mLogger.info("ProcessLoader创建过，但是不再活着了 serviceName={}", processName);
                cacheProcessLoader(processLoader);
                return processName;
            }
        }
        return null;
    }

    private ProcessLoaderInternal findOrCreate() {
        if (processLoaderCache.size() > 0) {
            ProcessLoaderInternal processLoader = processLoaderCache.removeFirst();
            mLogger.info("ProcessLoader 从缓存中获取");
            return processLoader;
        }
        mLogger.info("创建了一个全新的ProcessLoader");
        return new ProcessLoaderImpl(hostContext, uuidManager);
    }

    private void cacheProcessLoader(ProcessLoaderInternal processLoader) {
        mLogger.info("ProcessLoader 缓存到获取");
        processLoaderCache.addLast(processLoader);

        ProcessLoaderInternal.Partner partner = processLoader.getPartner();
        if (partner != null) {
            serviceUsedMap.remove(partner.serviceName);
            uuidUsedMap.remove(partner.uuid);
        }
    }
}
