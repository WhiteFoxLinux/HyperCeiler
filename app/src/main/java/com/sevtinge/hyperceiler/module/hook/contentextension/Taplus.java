package com.sevtinge.hyperceiler.module.hook.contentextension;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;

import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class Taplus extends BaseHook {
    public boolean mListening = false;

    @Override
    public void init() throws NoSuchMethodException {
        findAndHookMethod("com.miui.contentextension.utils.TaplusSettingUtils",
            "setTaplusEnableStatus", Context.class, boolean.class,
            new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    Context mContext = (Context) param.args[0];
                    boolean z = (boolean) param.args[1];
                    Settings.System.putInt(
                        mContext.getContentResolver(),
                        "key_enable_taplus", z ? 1 : 0);
                    // logE(TAG, "con: " + param.args[0] + " boo: " + param.args[1]);
                }
            }
        );

        findAndHookMethod("com.miui.contentextension.utils.TaplusSettingUtils",
            "isTaplusEnable", Context.class,
            new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    Context mContext = (Context) param.args[0];
                    SharedPreferences sharedPreferences = mContext.getSharedPreferences("pref_com_miui_contentextension", 0);
                    boolean system = false;
                    boolean prefer = false;
                    try {
                        if (mContext != null) {
                            try {
                                system = Settings.System.getInt(
                                    mContext.getContentResolver(),
                                    "key_enable_taplus", 0) == 1;
                                prefer = sharedPreferences.getBoolean("key_enable_taplus", false);
                            } catch (Throwable e) {
                                system = false;
                                prefer = false;
                                logE(TAG, "key_enable_taplus: " + e);
                            }
                        }
                    } catch (Throwable e) {
                        logE(TAG, "isTaplusEnable: " + e);
                    }
                    if ((system && !prefer) || (!system && prefer)) {
                        try {
                            sharedPreferences.edit().putBoolean("key_enable_taplus", system).apply();
                        } catch (Throwable e) {
                            logE(TAG, "putBoolean: " + e);
                        }
                    }
                    if (!mListening) setListening(param, mContext);
                    param.setResult(system);
                    // logE(TAG, "coo: " + param.args[0]);
                }

                /*@Override
                protected void after(MethodHookParam param) throws Throwable {
                    logE(TAG, "after: " + param.getResult());
                }*/
            }
        );
    }

    public void setListening(XC_MethodHook.MethodHookParam param, Context context) {
        mListening = true;
        ContentObserver contentObserver = new ContentObserver(new Handler(context.getMainLooper())) {
            @Override
            public void onChange(boolean selfChange, @Nullable Uri uri) {
                boolean z = false;
                try {
                    z = Settings.System.getInt(context.getContentResolver(), "key_enable_taplus") == 1;
                } catch (Throwable e) {
                    logE(TAG, "key_enable_taplus: " + e);
                }
                XposedHelpers.callStaticMethod(
                    findClassIfExists("com.miui.contentextension.utils.TaplusSettingUtils"),
                    "setTaplusEnableStatus", context, z);
                /*XposedHelpers.callStaticMethod(
                    findClassIfExists(
                        "com.miui.contentextension.utils.ContentCatcherUtil"),
                    "switchCatcherConfig", context, z);*/
            }
        };
        context.getContentResolver().registerContentObserver(
            Settings.System.getUriFor("key_enable_taplus"),
            false, contentObserver);
        // XposedHelpers.setAdditionalInstanceField(param.thisObject, "taplusListener", contentObserver);
    }
}
