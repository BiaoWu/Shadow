package com.tencent.shadow.test.cases.plugin_main;

import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;

public class ServiceContextSubDirTest extends SubDirContextThemeWrapperTest {
    @Override
    protected Intent getLaunchIntent() {
        Intent pluginIntent = new Intent();
        String packageName = ApplicationProvider.getApplicationContext().getPackageName();
        pluginIntent.setClassName(
                packageName,
                "com.tencent.shadow.demo.usecases.context.ServiceContextSubDirTestActivity"
        );
        return pluginIntent;
    }
}