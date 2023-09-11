// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.util.SparseArray;
import android.os.Bundle;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.bridge.Callback;

import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import javax.annotation.Nullable;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

public class ActivityResultModule extends ReactContextBaseJavaModule implements ActivityEventListener {

  final SparseArray<Promise> mPromises;

  public ActivityResultModule(ReactApplicationContext reactContext) {
      super(reactContext);
      mPromises = new SparseArray<>();
  }

  @Override
  public String getName() {
      return "ActivityResult";
  }

  @Nullable
  @Override
  public Map<String, Object> getConstants() {
      HashMap<String, Object> constants = new HashMap<>();
      constants.put("OK", Activity.RESULT_OK);
      constants.put("CANCELED", Activity.RESULT_CANCELED);
      return constants;
  }

  @Override
  public void initialize() {
      super.initialize();
      getReactApplicationContext().addActivityEventListener(this);
  }

  @Override
  public void onCatalystInstanceDestroy() {
      super.onCatalystInstanceDestroy();
      getReactApplicationContext().removeActivityEventListener(this);
  }

  @ReactMethod
  public void startActivity(String action, ReadableMap data) {
      Activity activity = getReactApplicationContext().getCurrentActivity();
      PackageManager pm = getReactApplicationContext().getPackageManager();
      Intent intent = pm.getLaunchIntentForPackage(action);
      intent.setFlags(0);
      intent.putExtras(Arguments.toBundle(data));
      activity.startActivity(intent);
  }

  @ReactMethod
  public void startActivityForResult(int requestCode, String action, ReadableMap data, Promise promise) {
      Activity activity = getReactApplicationContext().getCurrentActivity();
      PackageManager pm = getReactApplicationContext().getPackageManager();
      Intent intent = pm.getLaunchIntentForPackage(action);
      intent.setFlags(0);
      intent.putExtras(Arguments.toBundle(data));
      activity.startActivityForResult(intent, requestCode);
      mPromises.put(requestCode, promise);
  }

  @ReactMethod
  public void resolveActivity(String action, Promise promise) {
      Activity activity = getReactApplicationContext().getCurrentActivity();
      PackageManager pm = getReactApplicationContext().getPackageManager();
      Intent intent = pm.getLaunchIntentForPackage(action);
      ComponentName componentName = intent.resolveActivity(activity.getPackageManager());
      if (componentName == null) {
          promise.resolve(null);
          return;
      }

      WritableMap map = new WritableNativeMap();
      map.putString("class", componentName.getClassName());
      map.putString("package", componentName.getPackageName());
      promise.resolve(map);
  }

  @ReactMethod
  public void finish(int result, String action, ReadableMap map) {
      Activity activity = getReactApplicationContext().getCurrentActivity();
      Intent intent = new Intent(action);
      intent.putExtras(Arguments.toBundle(map));
      activity.setResult(result, intent);
      activity.finish();
  }

  @Override
  public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
      Promise promise = mPromises.get(requestCode);
      if (promise != null) {
        WritableMap result = new WritableNativeMap();
        if (resultCode != Activity.RESULT_CANCELED) {
            result.putInt("resultCode", resultCode);
            result.putMap("data", Arguments.makeNativeMap(data.getExtras()));
            promise.resolve(result);
        } else {
            result.putInt("resultCode", Activity.RESULT_CANCELED);
            promise.resolve(result);
        }
      }
  }

  @Override
  public void onNewIntent(Intent intent) {
      /* Do nothing */
  }
  
  @ReactMethod
    public void getIntentData(Callback callback) {
        Intent intent = getCurrentActivity().getIntent();
        if (intent != null) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                WritableMap intentData = Arguments.createMap();
                for (String key : extras.keySet()) {
                    Object value = extras.get(key);
                    intentData.putString(key, value != null ? value.toString() : null);
                }
                callback.invoke(intentData);
            }
        } else {
            callback.invoke(null);
        }
    }
}