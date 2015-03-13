package daniel.weck;

import com.badlogic.gdx.backends.jogl.JoglApplication;

public class Main {
	public static void main (String[] argv) {
		new JoglApplication(new LibGdxApp(), "LibGDX Water Ripples", 1024, 600, true);
	}
}
	