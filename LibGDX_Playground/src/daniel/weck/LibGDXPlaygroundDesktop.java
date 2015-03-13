package daniel.weck;

import com.badlogic.gdx.backends.jogl.JoglApplication;

public class LibGDXPlaygroundDesktop {
	public static void main (String[] argv) {
		new JoglApplication(new LibGDXPlayground(), "LibGDX Playground", 1024, 600, true);
	}
}
