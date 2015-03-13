package daniel.weck;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;
import aurelienribon.tweenengine.Tween;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.badlogic.gdx.backends.android.surfaceview.FillResolutionStrategy;

public class LibGDXPlaygroundAndroid extends AndroidApplication {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        // prevent the screen from dimming/sleeping (no permission required)
        getWindow().addFlags( WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON );
        
//		 AlertDialog.Builder builder = new AlertDialog.Builder(this);
//		 AlertDialog dlg = builder.setMessage("Stackoverflow!").create();
//		 dlg.setOwnerActivity(this);
//		 dlg.show();
		
		Tween.setPoolEnabled(true);

		AndroidApplicationConfiguration settings = new AndroidApplicationConfiguration();
		settings.resolutionStrategy = new FillResolutionStrategy();
		settings.touchSleepTime = 0;
		settings.useAccelerometer = true;
		settings.useCompass = true;
		settings.useGL20 = true;
		settings.useWakelock = false;

		initialize(new LibGDXPlayground(), settings);
		

//		View view = initializeForView(new LibGDXPlayground(), settings);
//		view.setKeepScreenOn(true);
//
//		getWindow().clearFlags(
//				WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
//		requestWindowFeature(Window.FEATURE_NO_TITLE);
//		FrameLayout layout = new FrameLayout(this);
//		addContentView(layout, new LayoutParams(LayoutParams.FILL_PARENT,
//				LayoutParams.FILL_PARENT));
//		
//		layout.addView(view, new LayoutParams(LayoutParams.FILL_PARENT,
//				LayoutParams.WRAP_CONTENT));

		//
		// AlertDialog.Builder builder = new AlertDialog.Builder(this);
		// AlertDialog dlg = builder.setMessage("Stackoverflow!").create();
		// dlg.setOwnerActivity(this);
		// dlg.show();

		//
		// final LibGDXPlaygroundAndroid thiz = this;
		//
		//
		// Handler mHandler = new Handler();
		// mHandler.postDelayed(new Runnable() {
		// public void run() {
		//
		// final CharSequence[] items = { "1.0 / 1.1 (for old devices)",
		// "2.0 (vertex/fragment shader)" };
		//
		// AlertDialog.Builder builder = new AlertDialog.Builder(thiz);
		// builder.setTitle("Pick an OpenGL ES version");
		// builder.setSingleChoiceItems(items, -1,
		// new DialogInterface.OnClickListener() {
		// public void onClick(DialogInterface dialog, int item) {
		// Toast.makeText(getApplicationContext(),
		// items[item], Toast.LENGTH_SHORT).show();
		//
		//
		//
		// }
		// });
		// AlertDialog alert = builder.create();
		//
		// alert.setOwnerActivity(thiz);
		//
		// alert.getWindow().setType(
		// WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
		//
		// alert.show();
		// }
		// }, 1000);

		//
		/*
		 * LibGDXPlayground game = new LibGDXPlayground(); View gameView =
		 * initializeForView(game, false, new FillResolutionStrategy(), 0);
		 * gameView.setKeepScreenOn(true); // optional
		 * requestWindowFeature(Window.FEATURE_NO_TITLE); AdView adView =
		 * setupAds(); FrameLayout layout = new FrameLayout(this); // everything
		 * at the top // left of the screen addContentView(layout, new
		 * LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
		 * layout.addView(gameView, new LayoutParams(LayoutParams.FILL_PARENT,
		 * LayoutParams.WRAP_CONTENT)); layout.addView(adView, new
		 * LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		 * adView.requestFreshAd(); adView.bringToFront(); //
		 * AdManager.setTestDevices( new String[] { //
		 * "FC31A6E749CC8FEDFDB707BDE035BEA0" } ); // comment out for testing
		 */
	}
	/*
	 * private AdView setupAds() { AdView adView = new AdView(this); // styling
	 * & advert options adView.setBackgroundColor(0xff000000);
	 * adView.setPrimaryTextColor(0xffffffff);
	 * adView.setSecondaryTextColor(0xffcccccc);
	 * adView.setKeywords("viagra,xtc,etc"); adView.setRequestInterval(60); //
	 * add listener for easier debugging adView.setAdListener(new
	 * SimpleAdListener() { public void
	 * onFailedToReceiveAd(com.admob.android.ads.AdView adView) { //
	 * Gdx.app.log("AdMobSDK", "onFailedToReceiveAd: " + // adView.toString());
	 * super.onFailedToReceiveAd(adView); } public void
	 * onFailedToReceiveRefreshedAd( com.admob.android.ads.AdView adView) { //
	 * Gdx.app.log("AdMobSDK", "onFailedToReceiveRefreshedAd: " + //
	 * adView.toString()); super.onFailedToReceiveRefreshedAd(adView); } public
	 * void onReceiveAd(com.admob.android.ads.AdView adView) { //
	 * Gdx.app.log("AdMobSDK", "onReceiveAd: " + adView.toString());
	 * super.onReceiveAd(adView); } public void
	 * onReceiveRefreshedAd(com.admob.android.ads.AdView adView) { //
	 * Gdx.app.log("AdMobSDK", "onReceiveRefreshedAd: " + // adView.toString());
	 * super.onReceiveRefreshedAd(adView); } }); return adView; }
	 */
}
