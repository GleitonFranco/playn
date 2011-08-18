/**
 * Copyright 2011 The PlayN Authors
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package playn.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.LinearLayout;

/**
 * TODO: save/restore state
 */
public abstract class GameActivity extends Activity {
  private final int REQUIRED_CONFIG_CHANGES = ActivityInfo.CONFIG_ORIENTATION | ActivityInfo.CONFIG_KEYBOARD_HIDDEN;
  
  private GameViewGL gameView;
  private LinearLayout viewLayout;
  private WakeLock wakeLock;
  private Context context;
  
  /**
   * The entry-point into a PlayN game.  Developers should
   * implement main() to call platform().assetManager().setPathPrefix()
   * and PlayN.run().
   */
  public abstract void main();
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
    context = getApplicationContext();
    
    //Build the AndroidPlatform and register this activity.
    AndroidGL20 gl20;
    if (isHoneycombOrLater()) {
      gl20 = new AndroidGL20();
    }else {
      //Provide our own native bindings for some missing methods.
      gl20 = new AndroidGL20Native();
    }

    //Build a View to hold the surface view and report changes to the screen size.
    viewLayout = new AndroidLayoutView(this);
    gameView = new GameViewGL(gl20, this, context);
    viewLayout.addView((View)gameView);

    //Build the Window and View
    if (isHoneycombOrLater()) {
      // Use the raw constant rather than the flag to avoid blowing up on
      // earlier Android
      int flagHardwareAccelerated = 0x1000000;
      getWindow().setFlags(flagHardwareAccelerated, flagHardwareAccelerated);
    }


    getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
        WindowManager.LayoutParams.FLAG_FULLSCREEN);
    LayoutParams params = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
    getWindow().setContentView(viewLayout, params);
    
    //Default to landscape orientation.
    if (usePortraitOrientation()) {
      setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }else {
      setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    //Check that the Wake Lock permissions are set correctly.
    try {
      PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
      wakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK
          | PowerManager.ON_AFTER_RELEASE, "playn");
      wakeLock.acquire();
    } catch (SecurityException e) {
      // Warn the developer of a missing permission. The other calls to
      // wakeLock.acquire/release will throw.
      new AlertDialog.Builder(this).setMessage(
          "Unable to acquire wake lock. Please add <uses-permission android:name=\"android.permission.WAKE_LOCK\" /> to the manifest.").show();
    }
    
    //Make sure the AndroidManifest.xml is set up correctly.
    try {
      ActivityInfo info = this.getPackageManager().getActivityInfo(new ComponentName(context, this.getPackageName() + "." + this.getLocalClassName()), 0);
        if ((info.configChanges & REQUIRED_CONFIG_CHANGES) != REQUIRED_CONFIG_CHANGES) {
          new AlertDialog.Builder(this).setMessage(
            "Unable to guarantee application will handle configuration changes. " +
            "Please add the following line to the Activity manifest: " +
            "      android:configChanges=\"keyboardHidden|orientation\"").show();
        }
    }catch (NameNotFoundException e) {
        Log.w("GameActivity", "Cannot access game AndroidManifest.xml file.");
    }
  }
  
  /**
   * Determines whether or not a game should run in portrait orientation or not.
   * Defaults to false.  Override this method to return true to use portrait.
   * @return Whether or not the game will run in portrait orientation
   */
  public boolean usePortraitOrientation() {
    return false;
  }
  
  public LinearLayout viewLayout() {
    return viewLayout;
  }
  public GameViewGL gameView() {
    return gameView;
  }

  protected AndroidPlatform platform() {
    return AndroidPlatform.instance;
  }
  
  protected Context context() {
    return context;
  }

  protected boolean isHoneycombOrLater() {
    return android.os.Build.VERSION.SDK_INT >= 11;
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    wakeLock.release();
    platform().audio().destroy();
  }

  @Override
  protected void onPause() {
    Log.i("playn", "onPause");
    gameView.notifyVisibilityChanged(View.INVISIBLE);
    if (platform() != null) platform().audio().pause();
    wakeLock.release();
    super.onPause();

    // TODO: Notify game
  }

  @Override
  protected void onResume() {
    Log.i("playn", "onResume");
    gameView.notifyVisibilityChanged(View.VISIBLE);
    if (platform() != null) platform().audio().resume();
    wakeLock.acquire();
    super.onResume();

    // TODO: Notify game
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    platform().keyboard().onKeyDown(event.getEventTime(), keyCode);
    //Don't prevent volume controls from propagating to the system.
    if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
      return false;
    }
    return true;
  }

  @Override
  public boolean onKeyUp(int keyCode, KeyEvent event) {
    platform().keyboard().onKeyUp(event.getEventTime(), keyCode);
    if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
      return false;
    }
    return true;
  }
  

//  public void onLayout(boolean changed, int left, int top, int right, int bottom) {
//    int displayWidth = right - left;
//    int displayHeight = bottom - top;
//    
//    /*
//     * TODO: Pass the width and height here into AndroidGraphics as the display
//     * width/height (this is the only way to take into account the size of the
//     * Honeycomb bezel). This requires the game activity lifecycle to be
//     * reworked, so it is currently not implemented.
//     */
//  }
}