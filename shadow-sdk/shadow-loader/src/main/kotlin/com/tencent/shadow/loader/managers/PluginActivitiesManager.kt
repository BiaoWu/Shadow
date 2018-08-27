package com.tencent.shadow.loader.managers

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.tencent.hydevteam.pluginframework.plugincontainer.HostActivityDelegator
import com.tencent.hydevteam.pluginframework.plugincontainer.PluginContainerActivity
import com.tencent.shadow.loader.infos.PluginActivityInfo
import com.tencent.shadow.loader.infos.PluginInfo
import com.tencent.shadow.runtime.ShadowContext

abstract class PluginActivitiesManager : ShadowContext.PluginActivityLauncher {
    companion object {
        val AVOID_CLASS_VERIFY_EXCEPTION = PluginContainerActivity::class
        const val PLUGIN_LOADER_BUNDLE_KEY = "PLUGIN_LOADER_BUNDLE_KEY"
        const val PLUGIN_ACTIVITY_INFO_KEY = "PLUGIN_ACTIVITY_INFO_KEY"
        const val PLUGIN_ACTIVITY_CLASS_NAME_KEY = "PLUGIN_ACTIVITY_CLASS_NAME_KEY"
    }

    /**
     * key:插件ComponentName
     * value:壳子ComponentName
     */
    private val activitiesMap: MutableMap<ComponentName, ComponentName> = HashMap()

    /**
     * key:插件Activity类名
     * value:插件PackageName
     */
    private val packageNameMap: MutableMap<String, String> = HashMap()

    /**
     * key:插件ComponentName
     * value:PluginActivityInfo
     */
    private val activityInfoMap: MutableMap<ComponentName, PluginActivityInfo> = HashMap()


    fun addPluginApkInfo(pluginInfo: PluginInfo) {
        pluginInfo.mActivities.forEach {
            val componentName = ComponentName(pluginInfo.packageName, it.className)
            activitiesMap[componentName] = onBindContainerActivity(componentName)
            packageNameMap[it.className] = pluginInfo.packageName
            activityInfoMap[componentName] = it
        }
    }

    /**
     * @param pluginActivity 插件Activity
     * @return 容器Activity
     */
    abstract fun onBindContainerActivity(pluginActivity: ComponentName): ComponentName

    private fun getContainerActivity(pluginActivity: ComponentName): ComponentName =
            activitiesMap[pluginActivity]!!

    override fun startActivity(shadowContext: ShadowContext, pluginIntent: Intent): Boolean {
        if (pluginIntent.component == null) {
            return false
        }
        val className = pluginIntent.component.className
        val packageName = packageNameMap[className] ?: return false
        pluginIntent.component = ComponentName(packageName, className)
        val containerActivity = getContainerActivity(pluginIntent.component)
        val containerActivityIntent = Intent(pluginIntent)
        containerActivityIntent.component = containerActivity

        val bundleForPluginLoader = Bundle()

        bundleForPluginLoader.putString(PLUGIN_ACTIVITY_CLASS_NAME_KEY, className)
        bundleForPluginLoader.putParcelable(PLUGIN_ACTIVITY_INFO_KEY, activityInfoMap[pluginIntent.component])

        containerActivityIntent.putExtra(PLUGIN_LOADER_BUNDLE_KEY, bundleForPluginLoader)
        shadowContext.superStartActivity(containerActivityIntent)
        return true
    }

    override fun startActivityForResult(delegator: HostActivityDelegator, pluginIntent: Intent, requestCode: Int): Boolean {
        if (pluginIntent.component == null) {
            return false
        }
        val className = pluginIntent.component.className
        val packageName = packageNameMap[className] ?: return false
        pluginIntent.component = ComponentName(packageName, className)
        val containerActivity = getContainerActivity(pluginIntent.component)
        val containerActivityIntent = Intent(pluginIntent)
        containerActivityIntent.component = containerActivity

        val bundleForPluginLoader = Bundle()

        bundleForPluginLoader.putString(PLUGIN_ACTIVITY_CLASS_NAME_KEY, className)
        bundleForPluginLoader.putParcelable(PLUGIN_ACTIVITY_INFO_KEY, activityInfoMap[pluginIntent.component])

        containerActivityIntent.putExtra(PLUGIN_LOADER_BUNDLE_KEY, bundleForPluginLoader)
        delegator.startActivityForResult(containerActivityIntent, requestCode)
        return true
    }

     public fun convertActivityIntent(pluginIntent: Intent): Intent {
        val className = pluginIntent.component.className
        val packageName = packageNameMap[className] ?: return pluginIntent
        pluginIntent.component = ComponentName(packageName, className)
        val containerActivity = getContainerActivity(pluginIntent.component)
        val containerActivityIntent = Intent(pluginIntent)
        containerActivityIntent.component = containerActivity

        val bundleForPluginLoader = Bundle()

        bundleForPluginLoader.putString(PLUGIN_ACTIVITY_CLASS_NAME_KEY, className)
        bundleForPluginLoader.putParcelable(PLUGIN_ACTIVITY_INFO_KEY, activityInfoMap[pluginIntent.component])

        containerActivityIntent.putExtra(PLUGIN_LOADER_BUNDLE_KEY, bundleForPluginLoader)
        return containerActivityIntent;
    }

    abstract val launcherActivity: ComponentName
}