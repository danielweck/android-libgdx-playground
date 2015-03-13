package daniel.weck;

import android.os.Bundle;
import android.view.WindowManager;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.badlogic.gdx.backends.android.surfaceview.FillResolutionStrategy;

import daniel.weck.LibGdxApp;

public class MainActivity extends AndroidApplication {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // prevent the screen from dimming/sleeping (no permission required)
        getWindow().addFlags( WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON );

		AndroidApplicationConfiguration settings = new AndroidApplicationConfiguration();
		settings.resolutionStrategy = new FillResolutionStrategy();
		settings.touchSleepTime = 0;
		settings.useAccelerometer = false;
		settings.useCompass = false;
		settings.useGL20 = true;
		settings.useWakelock = false;

        initialize(new LibGdxApp(), settings);
    }
}