package com.tencent.shadow.dynamic.manager;

import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import android.os.IBinder;

import com.tencent.shadow.core.common.Logger;
import com.tencent.shadow.core.common.LoggerFactory;
import com.tencent.shadow.dynamic.host.FailedException;
import com.tencent.shadow.dynamic.host.PluginManagerImpl;
import com.tencent.shadow.dynamic.loader.PluginLoader;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

abstract public class PluginManagerThatUseProcessManager extends BaseDynamicPluginManager implements PluginManagerImpl {
    private static final Logger mLogger = LoggerFactory.getLogger(PluginManagerThatUseProcessManager.class);

    private ProcessManager processManager;

    public PluginManagerThatUseProcessManager(Context context) {
        super(context);
        processManager = new ProcessManagerImpl(mHostContext, getPluginProcessServiceNames(), this);
    }

    public void initProcessLoaderSync(String uuid, int timeout, TimeUnit timeUnit) throws FailedException, TimeoutException {
        processManager.initProcessLoaderSync(uuid, timeout, timeUnit);
    }

    public ProcessLoader getProcessLoader(String uuid) {
        return processManager.getProcessLoader(uuid);
    }

    public PluginLoader getRunningPluginLoader(String uuid) {
        ProcessLoader processLoader = processManager.getProcessLoader(uuid);
        if (processLoader == null) {
            throw new RuntimeException("processLoader 未初始化，先要调用");
        }
        return processLoader.getPluginLoader();
    }


    /**
     * 集成这个PluginManager 就不要调用 {@link #bindPluginProcessService(String)}了
     *
     * @deprecated 建议使用 {@link ProcessManagerImpl}相关实现
     */
    protected final void onPluginServiceConnected(ComponentName name, IBinder service) {
        //空实现 为了兼容保留
    }

    /**
     * 集成这个PluginManager 就不要调用 {@link #bindPluginProcessService(String)}了
     *
     * @deprecated 建议使用 {@link ProcessManagerImpl}相关实现
     */
    protected final void onPluginServiceDisconnected(ComponentName name) {
        //空实现 为了兼容保留
    }

    /**
     * 一个Service对应一个进程
     *
     * @return 插件进程 注册在宿主中的Service全限定名。
     */
    abstract protected String[] getPluginProcessServiceNames();

    /**
     * PluginManager对象创建的时候回调
     *
     * @param bundle 当PluginManager有更新时会回调老的PluginManager对象onSaveInstanceState存储数据，bundle不为null说明发生了更新
     *               为null说明是首次创建
     */
    public void onCreate(Bundle bundle) {
        if (mLogger.isInfoEnabled()) {
            mLogger.info("onCreate bundle:" + bundle);
        }
    }

    /**
     * 当PluginManager有更新时会先回调老的PluginManager对象 onSaveInstanceState存储数据
     *
     * @param bundle 要存储的数据
     */
    public void onSaveInstanceState(Bundle bundle) {
        if (mLogger.isInfoEnabled()) {
            mLogger.info("onSaveInstanceState:" + bundle);
        }
    }

    /**
     * 当PluginManager有更新时先会销毁老的PluginManager对象，回调对应的onDestroy
     */
    public void onDestroy() {
        if (mLogger.isInfoEnabled()) {
            mLogger.info("onDestroy:");
        }
    }
}
