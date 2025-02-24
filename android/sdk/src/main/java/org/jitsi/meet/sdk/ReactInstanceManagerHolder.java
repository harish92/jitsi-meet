/*
 * Copyright @ 2017-present 8x8, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jitsi.meet.sdk;

import android.app.Activity;

import androidx.annotation.Nullable;

import org.brentvatne.react.ReactVideoPackage;
import org.calendarevents.CalendarEventsPackage;
import org.corbt.keepawake.KCKeepAwakePackage;
import com.facebook.react.ReactInstanceManager;
import com.facebook.react.ReactPackage;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.common.LifecycleState;
import com.facebook.react.devsupport.DevInternalSettings;
import com.facebook.react.jscexecutor.JSCExecutorFactory;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.shell.MainReactPackage;
import com.facebook.react.uimanager.ViewManager;
import org.horcrux.svg.SvgPackage;
import org.jitsi.meet.sdk.net.NAT64AddrInfoModule;
import org.kevinresol.react_native_default_preference.RNDefaultPreferencePackage;
import org.learnium.RNDeviceInfo.RNDeviceInfo;
import org.ocetnik.timer.BackgroundTimerPackage;
import org.oney.WebRTCModule.RTCVideoViewManager;
import org.oney.WebRTCModule.WebRTCModule;
import org.reactnative.maskedview.RNCMaskedViewPackage;
import org.reactnativecommunity.asyncstorage.AsyncStoragePackage;
import org.reactnativecommunity.netinfo.NetInfoPackage;
import org.reactnativecommunity.slider.ReactSliderPackage;
import org.reactnativecommunity.webview.RNCWebViewPackage;
import org.rnimmersive.RNImmersivePackage;
import org.swmansion.gesturehandler.react.RNGestureHandlerPackage;
import org.swmansion.reanimated.ReanimatedPackage;
import org.swmansion.rnscreens.RNScreensPackage;
import org.th3rdwave.safeareacontext.SafeAreaContextPackage;
import org.zmxv.RNSound.RNSoundPackage;

import org.devio.rn.splashscreen.SplashScreenModule;

import org.webrtc.SoftwareVideoDecoderFactory;
import org.webrtc.SoftwareVideoEncoderFactory;
import org.webrtc.audio.AudioDeviceModule;
import org.webrtc.audio.JavaAudioDeviceModule;


import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.invertase.firebase.analytics.ReactNativeFirebaseAnalyticsPackage;
import io.invertase.firebase.app.ReactNativeFirebaseAppPackage;
import io.invertase.firebase.crashlytics.ReactNativeFirebaseCrashlyticsPackage;

class ReactInstanceManagerHolder {
    /**
     * FIXME (from linter): Do not place Android context classes in static
     * fields (static reference to ReactInstanceManager which has field
     * mApplicationContext pointing to Context); this is a memory leak (and
     * also breaks Instant Run).
     *
     * React Native bridge. The instance manager allows embedding applications
     * to create multiple root views off the same JavaScript bundle.
     */
    private static ReactInstanceManager reactInstanceManager;

    private static List<NativeModule> createNativeModules(ReactApplicationContext reactContext) {
        List<NativeModule> nativeModules
            = new ArrayList<>(Arrays.<NativeModule>asList(
                new AndroidSettingsModule(reactContext),
                new AppInfoModule(reactContext),
                new AudioModeModule(reactContext),
                new DropboxModule(reactContext),
                new ExternalAPIModule(reactContext),
                new JavaScriptSandboxModule(reactContext),
                new LocaleDetector(reactContext),
                new LogBridgeModule(reactContext),
                new SplashScreenModule(reactContext),
                new PictureInPictureModule(reactContext),
                new ProximityModule(reactContext),
                new WiFiStatsModule(reactContext),
                new NAT64AddrInfoModule(reactContext)));

        if (AudioModeModule.useConnectionService()) {
            nativeModules.add(new RNConnectionService(reactContext));
        }

        // Initialize the WebRTC module by hand, since we want to override some
        // initialization options.
        WebRTCModule.Options options = new WebRTCModule.Options();

        AudioDeviceModule adm = JavaAudioDeviceModule.builder(reactContext)
            .createAudioDeviceModule();
        options.setAudioDeviceModule(adm);

        options.setVideoDecoderFactory(new SoftwareVideoDecoderFactory());
        options.setVideoEncoderFactory(new SoftwareVideoEncoderFactory());

        nativeModules.add(new WebRTCModule(reactContext, options));

        return nativeModules;
    }

    private static List<ViewManager> createViewManagers(ReactApplicationContext reactContext) {
        return Arrays.<ViewManager>asList(
            // WebRTC, see createNativeModules for details.
            new RTCVideoViewManager()
        );
    }

    /**
     * Helper function to send an event to JavaScript.
     *
     * @param eventName {@code String} containing the event name.
     * @param data {@code Object} optional ancillary data for the event.
     */
    static void emitEvent(
            String eventName,
            @Nullable Object data) {
        ReactInstanceManager reactInstanceManager
            = ReactInstanceManagerHolder.getReactInstanceManager();

        if (reactInstanceManager != null) {
            ReactContext reactContext
                = reactInstanceManager.getCurrentReactContext();

            if (reactContext != null) {
                reactContext
                    .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                    .emit(eventName, data);
            }
        }
    }

    /**
     * Finds a native React module for given class.
     *
     * @param nativeModuleClass the native module's class for which an instance
     * is to be retrieved from the {@link #reactInstanceManager}.
     * @param <T> the module's type.
     * @return {@link NativeModule} instance for given interface type or
     * {@code null} if no instance for this interface is available, or if
     * {@link #reactInstanceManager} has not been initialized yet.
     */
    static <T extends NativeModule> T getNativeModule(
            Class<T> nativeModuleClass) {
        ReactContext reactContext
            = reactInstanceManager != null
                ? reactInstanceManager.getCurrentReactContext() : null;

        return reactContext != null
                ? reactContext.getNativeModule(nativeModuleClass) : null;
    }

    /**
     * Gets the current {@link Activity} linked to React Native.
     *
     * @return An activity attached to React Native.
     */
    static Activity getCurrentActivity() {
        ReactContext reactContext
            = reactInstanceManager != null
            ? reactInstanceManager.getCurrentReactContext() : null;
        return reactContext != null ? reactContext.getCurrentActivity() : null;
    }

    static ReactInstanceManager getReactInstanceManager() {
        return reactInstanceManager;
    }

    /**
     * Internal method to initialize the React Native instance manager. We
     * create a single instance in order to load the JavaScript bundle a single
     * time. All {@code ReactRootView} instances will be tied to the one and
     * only {@code ReactInstanceManager}.
     *
     * @param activity {@code Activity} current running Activity.
     */
    static void initReactInstanceManager(Activity activity) {
        if (reactInstanceManager != null) {
            return;
        }

        List<ReactPackage> packages
            = new ArrayList<>(Arrays.asList(
                new AsyncStoragePackage(),
                new BackgroundTimerPackage(),
                new CalendarEventsPackage(),
                new KCKeepAwakePackage(),
                new MainReactPackage(),
                new NetInfoPackage(),
                new ReactSliderPackage(),
                new ReactVideoPackage(),
                new ReanimatedPackage(),
                new RNCMaskedViewPackage(),
                new RNCWebViewPackage(),
                new RNDefaultPreferencePackage(),
                new RNDeviceInfo(),
                new RNGestureHandlerPackage(),
                new RNImmersivePackage(),
                new RNScreensPackage(),
                new RNSoundPackage(),
                new SafeAreaContextPackage(),
                new SvgPackage(),
                new ReactNativeFirebaseAnalyticsPackage(),
                new ReactNativeFirebaseAppPackage(),
                new ReactNativeFirebaseCrashlyticsPackage(),
                new ReactPackageAdapter() {
                    @Override
                    public List<NativeModule> createNativeModules(ReactApplicationContext reactContext) {
                        return ReactInstanceManagerHolder.createNativeModules(reactContext);
                    }
                    @Override
                    public List<ViewManager> createViewManagers(ReactApplicationContext reactContext) {
                        return ReactInstanceManagerHolder.createViewManagers(reactContext);
                    }
                }));

        // AmplitudeReactNativePackage
        try {
            Class<?> amplitudePackageClass = Class.forName("org.amplitude.reactnative.AmplitudeReactNativePackage");
            Constructor constructor = amplitudePackageClass.getConstructor();
            packages.add((ReactPackage)constructor.newInstance());
        } catch (Exception e) {
            // Ignore any error, the module is not compiled when LIBRE_BUILD is enabled.
        }

        // RNGoogleSigninPackage
        // try {
        //     Class<?> googlePackageClass = Class.forName("co.apptailor.googlesignin.RNGoogleSigninPackage");
        //     Constructor constructor = googlePackageClass.getConstructor();
        //     packages.add((ReactPackage)constructor.newInstance());
        // } catch (Exception e) {
        //     // Ignore any error, the module is not compiled when LIBRE_BUILD is enabled.
        // }

        // Keep on using JSC, the jury is out on Hermes.
        JSCExecutorFactory jsFactory
            = new JSCExecutorFactory("", "");

        reactInstanceManager
            = ReactInstanceManager.builder()
                .setApplication(activity.getApplication())
                .setCurrentActivity(activity)
                .setBundleAssetName("index.android.bundle")
                .setJSMainModulePath("index.android")
                .setJavaScriptExecutorFactory(jsFactory)
                .addPackages(packages)
                .setUseDeveloperSupport(BuildConfig.DEBUG)
                .setInitialLifecycleState(LifecycleState.RESUMED)
                .build();

        // Disable delta updates on Android, they have caused trouble.
        DevInternalSettings devSettings
            = (DevInternalSettings)reactInstanceManager.getDevSupportManager().getDevSettings();
        if (devSettings != null) {
            devSettings.setBundleDeltasEnabled(false);
        }

        // Register our uncaught exception handler.
        JitsiMeetUncaughtExceptionHandler.register();
    }
}
