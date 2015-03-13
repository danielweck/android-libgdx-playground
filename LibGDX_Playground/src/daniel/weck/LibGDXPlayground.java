package daniel.weck;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.metalev.multitouch.controller.MultiTouchController;
import org.metalev.multitouch.controller.MultiTouchController.MotionEvent;
import org.metalev.multitouch.controller.MultiTouchController.MultiTouchObjectCanvas;
import org.metalev.multitouch.controller.MultiTouchController.PointInfo;
import org.metalev.multitouch.controller.MultiTouchController.PositionAndScale;

import aurelienribon.tweenengine.Tween;
import aurelienribon.tweenengine.TweenCallback;
import aurelienribon.tweenengine.TweenGroup;
import aurelienribon.tweenengine.TweenManager;
import aurelienribon.tweenengine.Tweenable;
import aurelienribon.tweenengine.equations.Back;
import aurelienribon.tweenengine.equations.Expo;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Input.Peripheral;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GLCommon;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Blending;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;
import com.badlogic.gdx.graphics.g2d.BitmapFont.TextBounds;
import com.badlogic.gdx.graphics.g2d.BitmapFontCache;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.ParticleEmitter;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ImmediateModeRenderer;
import com.badlogic.gdx.graphics.glutils.ImmediateModeRenderer10;
import com.badlogic.gdx.graphics.glutils.ImmediateModeRenderer20;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.CatmullRomSpline;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Plane;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactFilter;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.DestructionListener;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.JointEdge;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.QueryCallback;
import com.badlogic.gdx.physics.box2d.RayCastCallback;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.physics.box2d.WorldManifold;
import com.badlogic.gdx.physics.box2d.joints.MouseJoint;
import com.badlogic.gdx.physics.box2d.joints.MouseJointDef;
import com.badlogic.gdx.physics.box2d.joints.PrismaticJoint;
import com.badlogic.gdx.physics.box2d.joints.PrismaticJointDef;
import com.badlogic.gdx.physics.box2d.joints.RevoluteJointDef;
import com.badlogic.gdx.physics.box2d.joints.WeldJointDef;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.ScreenUtils;

import daniel.weck.ShapeRendererEx.ShapeType;
import daniel.weck.Thumbpad.Axis;

interface TextureBindTracker {
	void bind(SubTexture subtexture);

	void bind(Texture texture);
}

public class LibGDXPlayground implements ApplicationListener, InputProcessor,
		TextureBindTracker, MultiTouchObjectCanvas<LibGDXPlayground> {

	Texture lastBound = null;

	public void bind(SubTexture subtexture) {
		bind(subtexture.texture);
	}

	public void bind(Texture texture) {
		if (texture == lastBound)
			return;
		lastBound = texture;
		texture.bind(0);
	}

	class BodyUserData {
		public BodyUserData(int i, float z) {
			textureIndex = i;
			zCoord = z;
		}

		int textureIndex = -1;
		float zCoord = -1;
	}

	final int MAX_TOUCH_POINTERS = 10;

	final long[] touchDownTimes = new long[MAX_TOUCH_POINTERS];
	final int touchDownTimeout = 600;

	public LibGDXPlayground() {
		for (int pointer = 0; pointer < MAX_TOUCH_POINTERS; pointer++) {
			touchDownTimes[pointer] = -1;
		}

		for (int pointer = 0; pointer < MAX_TOUCH_POINTERS; pointer++) {
			// slashPoints.set(pointer, new Array<Vector2>(15));
			slashPoints.add(new Array<Vector2>(15));
		}

		for (int pointer = 0; pointer < MAX_TOUCH_POINTERS; pointer++) {

			Vector2[] array = new Vector2[15];
			for (int i = 0; i < 15; i++) {
				array[i] = new Vector2();
			}

			// slashPointCache.set(pointer, array);
			slashPointCache.add(array);
		}

		for (int pointer = 0; pointer < MAX_TOUCH_POINTERS; pointer++) {
			// slashPointsSpline.set(pointer, new Array<Vector2>(30));
			slashPointsSpline.add(new Array<Vector2>(30));
		}

		for (int pointer = 0; pointer < MAX_TOUCH_POINTERS; pointer++) {
			slashPointsSplineFadeoutTime[pointer] = -1;
		}
	}

	// public static void reverse(Array<Vector2> data) {
	// // data.reverse(); :)
	// int left = 0;
	// int right = data.size - 1;
	//
	// while (left < right) {
	// Vector2 temp = data.get(left);
	// data.set(left, data.get(right));
	// data.set(right, temp);
	//
	// left++;
	// right--;
	// }
	// }

	// class SpritePixels
	// {
	// public int w;
	// public int h;
	// public int[] pixels;
	// }
	enum DeviceOrientation {
		PORTRAIT_NORMAL, PORTRAIT_UPSIDEDOWN, LANDSCAPE_NORMAL, LANDSCAPE_UPSIDEDOWN,
	};

	DeviceOrientation orientation = DeviceOrientation.PORTRAIT_NORMAL;

	//
	enum DebugRender {
		None, Medium, Full
	};

	DebugRender renderDebug = DebugRender.None;
	//
	ParticleEffect effect;
	int emitterIndex;
	Array<ParticleEmitter> emitters;
	int particleCount = 10;
	//
	OrthographicCamera cameraSCREEN;
	//
	Camera cameraSCENE;
	OrthographicCamera cameraSCENEOrthographic;
	PerspectiveCamera cameraSCENEPerspective;
	//
	float zCoord_box2D;
	float zCoord_box2D_;
	Plane zPlane_box2D;
	Plane zPlane_box2D_;

	float zCoord_Background;
	//
	WaterRipples waterRipples;

	boolean usePageTurn = false;
	PageTurn pageTurn;
	//
	ShapeRendererEx shapeRenderer;
	//
	SpriteBatch spriteBatch;
	int currentFont = 0;
	Array<BitmapFont> fonts;
	Array<BitmapFontCache> fontCaches;
	//
	SubTexture backgroundTexture_Aqua;
	SubTexture backgroundTexture_Paper;
	SubTexture backgroundTexture_Parchemin;
	SubTexture backgroundTexture_Repeat;
	//
	Array<String> sprite_ImagePaths;
	Array<SubTexture> sprite_Textures;
	Array<Mesh> sprite_TexturesMeshes;
	// IntMap<Mesh> sprite_ChunkMeshes = new IntMap<Mesh>();
	Array<Array<Vector2>> sprite_OutlineVertices; // must be
													// CounterClockWise
													// (decomposer
													// requirement,
	// although it will reverse internally ...)
	Array<Array<Array<Vector2>>> sprite_BodyPolygons;

	// Array<Mesh> sprite_BodyPolygonsMeshes = new
	// Array<Mesh>(N_SPRITES);
	// Array<Body> spriteBodies = new Array<Body>();
	final float SPRITES_SCALE_FACTOR = 0.3f;
	//
	// Vector2 bouncingText_Position = new Vector2(100, 100);
	// Vector2 bouncingText_Direction = new Vector2(1, 1);
	// TextBounds bouncingText_Bounds;
	// String bouncingText_String;
	//
	Vector2 gravityVerticalDown = new Vector2(0, 10);
	//
	World world;
	Vector2 worldSize = new Vector2(0, 0); // dynamic (based on pixel resolution
											// and factor below)
	final int PIXEL_TO_WORLD_RATIO = 20;
	//
	final int FINGER_TOUCH_TOLERANCE = 20;
	final int SPLINE_INTERMEDIATE_POINTS = 2;
	//
	Body groundBody;
	Mesh groundMesh;
	// Mesh groundBorderMesh;
	Mesh compassMesh;
	Mesh worldBackgroundRepeatMesh;
	Texture groundTexture;
	final Vector2 GROUND_SIZE_FRACTION = new Vector2(0.5f, 14f);
	//
	Array<MouseJoint> mouseJoints = new Array<MouseJoint>(MAX_TOUCH_POINTERS);

	//
	enum RenderState {
		Loading, Normal, Disposed, Error
	};

	RenderState renderState;
	//
	// Thread loadSpritesThread;
	// int loadSpritesCurrent = -1;
	// AtomicQueue<SpritePixels> loadSpritesPixelsQueue = new
	// AtomicQueue<SpritePixels>(N_SPRITES);
	//
	long lastTimeGravityCheck = -1;
	long lastTimePageAnimation = -1;
	long lastTimeBodiesDestroyed = -1;

	//
	MultiTouchController<LibGDXPlayground> multiTouchController;
	//
	Thumbpad thumbpad1;
	Thumbpad thumbpad2;
	//
	Sound soundCrush1;
	Sound soundPop1;
	Sound soundPop2;
	Sound soundShatter1;
	// Sound soundShatter2;
	Sound soundSword1;
	Sound soundSword2;
	Sound soundWhip1;

	//
	TweenManager groundTweenManager;
	Tweenable groundTweenable = new Tweenable() {

		Vector2 groundBodyTransform = new Vector2(0, 0);

		@Override
		public int getTweenValues(int tweenType, float[] returnValues) {
			returnValues[0] = groundBodyTransform.x;
			returnValues[1] = groundBodyTransform.y;
			return 2;
		}

		@Override
		public void onTweenUpdated(int tweenType, float[] newValues) {
			groundBodyTransform.x = newValues[0] / 2;
			groundBodyTransform.y = 0;

			if (preferences.getBoolean(PREF_ANIMATE_GROUND))
				if (true || groundBody.getType() == BodyType.StaticBody) {
					groundBody.setTransform(groundBodyTransform,
							(float) Math.toRadians(newValues[1]));
				} else {
					groundBody.setAngularVelocity((float) Math
							.toRadians(newValues[1]));
					groundBody.setLinearVelocity(groundBodyTransform);
				}

			groundBodyTransform.x = newValues[0];
			groundBodyTransform.y = newValues[1];
		}
	};
	TweenManager cameraTweenManager;
	Tweenable cameraTweenable = new Tweenable() {

		@Override
		public int getTweenValues(int tweenType, float[] returnValues) {
			returnValues[0] = MT_scale;
			return 1;
		}

		@Override
		public void onTweenUpdated(int tweenType, float[] newValues) {
			resetSCENECamera(worldSize.x, worldSize.y, newValues[0], MT_angle,
					MT_offset.x, MT_offset.y, MT_offset.z);
		}
	};

	Preferences preferences;
	final String PREF_ANIMATE_GROUND = "Animate-Ground";

	final String PREF_USE_3D_CAM = "Use-3D-Camera";
	final String PREF_USE_BOUNCING_OBJECTS = "Use-Bouncing-Ground";
	final String PREF_SHOW_PARTICLE_EFFECTS = "Show-Particle-Effects";

	Box2DDebugRendererEx box2d_renderer;

	final String BASE_FILENAME = "daniel.weck.LibGDX-Playground";
	final String APP_NAME = BASE_FILENAME;

	//
	// int indexOfBody(Body body) {
	// int index = 0;
	// for (Iterator<Body> iter = world.getBodies(); iter.hasNext();) {
	// Body b = iter.next();
	// if (b == body)
	// return index;
	// index++;
	// }
	// return -1;
	// }

	void create_preferences() {
		preferences = Gdx.app.getPreferences(BASE_FILENAME + ".prefs");
		preferences.clear();

		// prefs.putBoolean("bool", true);
		// prefs.putInteger("int", 1234);
		// prefs.putLong("long", Long.MAX_VALUE);
		// prefs.putFloat("float", 1.2345f);
		// prefs.putString("string", "test!");

		if (!preferences.contains(PREF_ANIMATE_GROUND))
			preferences.putBoolean(PREF_ANIMATE_GROUND, false);
		if (!preferences.contains(PREF_USE_3D_CAM))
			preferences.putBoolean(PREF_USE_3D_CAM, true);
		if (!preferences.contains(PREF_USE_BOUNCING_OBJECTS))
			preferences.putBoolean(PREF_USE_BOUNCING_OBJECTS, true);
		if (!preferences.contains(PREF_SHOW_PARTICLE_EFFECTS))
			preferences.putBoolean(PREF_SHOW_PARTICLE_EFFECTS, true);
		
		preferences.flush();
	}

	void updateZ(float z) {
		zCoord_box2D = z;

		zPlane_box2D = new Plane(new Vector3(0, 0, zCoord_box2D), new Vector3(
				0, 1, zCoord_box2D), new Vector3(1, 0, zCoord_box2D));

		zCoord_box2D_ = zCoord_box2D + zCoord_box2D / 6;

		zPlane_box2D_ = new Plane(new Vector3(0, 0, zCoord_box2D_),
				new Vector3(0, 1, zCoord_box2D_), new Vector3(1, 0,
						zCoord_box2D_));

		zCoord_Background = zCoord_box2D + zCoord_box2D / 4;
	}

	boolean created = false;

	PageCurler pageCurler;
	PageCurler pageCurlerLeft;
	PageCurler pageCurlerRight;

	SubTexture pagesTexture;
	SubTexture page1Texture;
	SubTexture page2Texture;

	//
	// Texture tex1;
	// Texture tex2;
	// Mesh mesh;

	public static final String ShaderProgram_PROJECTION_VIEW_MATRIX_UNIFORM = "u_projectionViewMatrix";
	public static final String ShaderProgram_TEXTURE_SAMPLER_UNIFORM = "u_texture";

	public static final String ShaderProgram_VERTEX_COLOR_UNIFORM = "u_color";
	public static final String ShaderProgram_FLIP_TEXTURE_X_UNIFORM = "u_flipTexU";
	public static final String ShaderProgram_FLIP_TEXTURE_Y_UNIFORM = "u_flipTexV";

	public static String createVertexShader(boolean hasNormals,
			boolean hasColors, int numTexCoords) {

		String shader = "#ifdef GL_ES\n"
		+ "#define LOWP lowp\n"
		+ "precision mediump float;\n"
		+ "#else\n"  
		+ "#define LOWP \n"
		//+ "precision highp float;\n"
		+ "#endif\n";
		
		shader += "attribute vec4 "
				+ ShaderProgram.POSITION_ATTRIBUTE
				+ ";\n"
				+ (hasNormals ? "attribute vec3 "
						+ ShaderProgram.NORMAL_ATTRIBUTE + ";\n" : "")
				+ (hasColors ? "attribute vec4 "
						+ ShaderProgram.COLOR_ATTRIBUTE + ";\n" : "");

		for (int i = 0; i < numTexCoords; i++) {
			shader += "attribute vec2 " + ShaderProgram.TEXCOORD_ATTRIBUTE + i
					+ ";\n";
		}

		shader += "uniform mat4 "
				+ ShaderProgram_PROJECTION_VIEW_MATRIX_UNIFORM + ";\n";

		shader += (hasColors ? "varying LOWP vec4 v_col;\n" : "");

		for (int i = 0; i < numTexCoords; i++) {
			shader += "varying vec2 v_tex" + i + ";\n";
		}

		shader += "void main() {\n"
				+ "   gl_Position = "
				+ ShaderProgram_PROJECTION_VIEW_MATRIX_UNIFORM
				+ " * "
				+ ShaderProgram.POSITION_ATTRIBUTE
				+ ";\n"
				+ (hasColors ? "   v_col = " + ShaderProgram.COLOR_ATTRIBUTE
						+ ";\n" : "");

		for (int i = 0; i < numTexCoords; i++) {
			shader += "   v_tex" + i + " = " + ShaderProgram.TEXCOORD_ATTRIBUTE
					+ i + ";\n";
		}

		shader += "}\n";

		return shader;
	}

	public static String createFragmentShader(boolean hasNormals,
			boolean hasColors, int numTexCoords) {

		String shader = "#ifdef GL_ES\n"
		+ "#define LOWP lowp\n"
		+ "precision mediump float;\n"
		+ "#else\n"  
		+ "#define LOWP \n"
		//+ "precision highp float;\n"
		+ "#endif\n";
		
		if (hasColors)
			shader += "varying LOWP vec4 v_col;\n";

		for (int i = 0; i < numTexCoords; i++) {
			shader += "varying vec2 v_tex" + i + ";\n";
			shader += "uniform sampler2D "
					+ ShaderProgram_TEXTURE_SAMPLER_UNIFORM + i + ";\n";
		}

		if (numTexCoords == 2) {
			shader += "uniform int " + ShaderProgram_FLIP_TEXTURE_X_UNIFORM + 0
					+ ";\n";
			shader += "uniform int " + ShaderProgram_FLIP_TEXTURE_Y_UNIFORM + 0
					+ ";\n";
			shader += "uniform int " + ShaderProgram_FLIP_TEXTURE_X_UNIFORM + 1
					+ ";\n";
			shader += "uniform int " + ShaderProgram_FLIP_TEXTURE_Y_UNIFORM + 1
					+ ";\n";
		}

		shader += "uniform vec4 " + ShaderProgram_VERTEX_COLOR_UNIFORM + ";\n";

		shader += "void main() {\n";

		if (numTexCoords == 2) {
			shader += "vec2 texCoord" + 0 + " = v_tex" + 0 + ";" + "\n if ("
					+ ShaderProgram_FLIP_TEXTURE_X_UNIFORM + 0 + " == 1 && "
					+ ShaderProgram_FLIP_TEXTURE_Y_UNIFORM + 0
					+ " == 0) { texCoord" + 0 + " = vec2(1.0 - v_tex" + 0
					+ ".s, v_tex" + 0 + ".t);}"

					+ "\n if (" + ShaderProgram_FLIP_TEXTURE_X_UNIFORM + 0
					+ " == 1 && " + ShaderProgram_FLIP_TEXTURE_Y_UNIFORM + 0
					+ " == 1) { texCoord" + 0 + " = vec2(1.0 - v_tex" + 0
					+ ".s, 1.0 - v_tex" + 0 + ".t);}"

					+ "\n if (" + ShaderProgram_FLIP_TEXTURE_X_UNIFORM + 0
					+ " == 0 && " + ShaderProgram_FLIP_TEXTURE_Y_UNIFORM + 0
					+ " == 1) { texCoord" + 0 + " = vec2(v_tex" + 0
					+ ".s, 1.0 - v_tex" + 0 + ".t);}"

					+ "\n vec4 texture" + 0 + " = texture2D("
					+ ShaderProgram_TEXTURE_SAMPLER_UNIFORM + 0 + ",  texCoord"
					+ 0 + ");";

			shader += "vec2 texCoord" + 1 + " = v_tex" + 1 + ";" + "\n if ("
					+ ShaderProgram_FLIP_TEXTURE_X_UNIFORM + 1 + " == 1 && "
					+ ShaderProgram_FLIP_TEXTURE_Y_UNIFORM + 1
					+ " == 0) { texCoord" + 1 + " = vec2(1.0 - v_tex" + 1
					+ ".s, v_tex" + 1 + ".t);}"

					+ "\n if (" + ShaderProgram_FLIP_TEXTURE_X_UNIFORM + 1
					+ " == 1 && " + ShaderProgram_FLIP_TEXTURE_Y_UNIFORM + 1
					+ " == 1) { texCoord" + 1 + " = vec2(1.0 - v_tex" + 1
					+ ".s, 1.0 - v_tex" + 1 + ".t);}"

					+ "\n if (" + ShaderProgram_FLIP_TEXTURE_X_UNIFORM + 1
					+ " == 0 && " + ShaderProgram_FLIP_TEXTURE_Y_UNIFORM + 1
					+ " == 1) { texCoord" + 1 + " = vec2(v_tex" + 1
					+ ".s, 1.0 - v_tex" + 1 + ".t);}"

					+ "\n vec4 texture" + 1 + " = texture2D("
					+ ShaderProgram_TEXTURE_SAMPLER_UNIFORM + 1 + ",  texCoord"
					+ 1 + ");";

			shader += " if (" + ShaderProgram_VERTEX_COLOR_UNIFORM
					+ ".r == 0.0 && " + ShaderProgram_VERTEX_COLOR_UNIFORM
					+ ".g == 0.0 && " + ShaderProgram_VERTEX_COLOR_UNIFORM
					+ ".b == 0.0 && " + ShaderProgram_VERTEX_COLOR_UNIFORM
					+ ".a == 0.0 ) { \n gl_FragColor = "
					+ (hasColors ? "v_col" : "vec4(1, 0, 0, 0.5)")
					+ ";\n} else { \n gl_FragColor = "
					+ ShaderProgram_VERTEX_COLOR_UNIFORM + "; \n}";

			shader += "\n vec3 col = mix(texture0.rgb, texture1.rgb, texture1.a);";
			shader += "\n gl_FragColor = gl_FragColor * vec4(col, texture0.a);";

		} else {
			shader += " if (" + ShaderProgram_VERTEX_COLOR_UNIFORM
					+ ".r == 0.0 && " + ShaderProgram_VERTEX_COLOR_UNIFORM
					+ ".g == 0.0 && " + ShaderProgram_VERTEX_COLOR_UNIFORM
					+ ".b == 0.0 && " + ShaderProgram_VERTEX_COLOR_UNIFORM
					+ ".a == 0.0 ) { \n gl_FragColor = "
					+ (hasColors ? "v_col" : "vec4(1, 0, 0, 0.5)")
					+ ";\n} else { \n gl_FragColor = "
					+ ShaderProgram_VERTEX_COLOR_UNIFORM + "; \n}";

			if (numTexCoords > 0)
				shader += " gl_FragColor = gl_FragColor * ";

			for (int i = 0; i < numTexCoords; i++) {

				shader += " texture2D(" + ShaderProgram_TEXTURE_SAMPLER_UNIFORM
						+ i + ",  v_tex" + i + ")";

				if (i != numTexCoords - 1) {
					shader += "*";
				}
			}
		}

		shader += ";\n}";

		return shader;
	}

	Matrix4 shaderMatrix = new Matrix4();

	// thumpads
	ShaderProgram shader_POSITION;

	// compass
	ShaderProgram shader_POSITION_COLOR;

	// worldBackgroundRepeatMesh
	// water ripples
	// sprites
	ShaderProgram shader_POSITION_TEX1;

	// ground
	ShaderProgram shader_POSITION_COLOR_TEX1;

	// page curl
	ShaderProgram shader_POSITION_COLOR_TEX2;
	ShaderProgram shader_POSITION_TEX2;

	protected void createShaders() {
		{
			String vertexShader = createVertexShader(false, false, 0);
			String fragmentShader = createFragmentShader(false, false, 0);
			shader_POSITION = new ShaderProgram(vertexShader, fragmentShader);
			if (!shader_POSITION.isCompiled())
				throw new IllegalArgumentException(
						"Couldn't compile shader_POSITION: "
								+ shader_POSITION.getLog());
		}
		{
			String vertexShader = createVertexShader(false, true, 0);
			String fragmentShader = createFragmentShader(false, true, 0);
			shader_POSITION_COLOR = new ShaderProgram(vertexShader,
					fragmentShader);
			if (!shader_POSITION_COLOR.isCompiled())
				throw new IllegalArgumentException(
						"Couldn't compile shader_POSITION_COLOR: "
								+ shader_POSITION_COLOR.getLog());
		}
		{
			String vertexShader = createVertexShader(false, false, 1);
			String fragmentShader = createFragmentShader(false, false, 1);
			shader_POSITION_TEX1 = new ShaderProgram(vertexShader,
					fragmentShader);
			if (!shader_POSITION_TEX1.isCompiled())
				throw new IllegalArgumentException(
						"Couldn't compile shader_POSITION_TEX1: "
								+ shader_POSITION_TEX1.getLog());
		}
		{
			String vertexShader = createVertexShader(false, true, 1);
			String fragmentShader = createFragmentShader(false, true, 1);
			shader_POSITION_COLOR_TEX1 = new ShaderProgram(vertexShader,
					fragmentShader);
			if (!shader_POSITION_COLOR_TEX1.isCompiled())
				throw new IllegalArgumentException(
						"Couldn't compile shader_POSITION_COLOR_TEX1: "
								+ shader_POSITION_COLOR_TEX1.getLog());
		}
		{
			String vertexShader = createVertexShader(false, true, 2);
			String fragmentShader = createFragmentShader(false, true, 2);
			shader_POSITION_COLOR_TEX2 = new ShaderProgram(vertexShader,
					fragmentShader);
			if (!shader_POSITION_COLOR_TEX2.isCompiled())
				throw new IllegalArgumentException(
						"Couldn't compile shader_POSITION_COLOR_TEX2: "
								+ shader_POSITION_COLOR_TEX2.getLog());
		}
		{
			String vertexShader = createVertexShader(false, false, 2);
			String fragmentShader = createFragmentShader(false, false, 2);
			shader_POSITION_TEX2 = new ShaderProgram(vertexShader,
					fragmentShader);
			if (!shader_POSITION_TEX2.isCompiled())
				throw new IllegalArgumentException(
						"Couldn't compile shader_POSITION_TEX2: "
								+ shader_POSITION_TEX2.getLog());
		}
	}

	FrameBuffer frameBuffer;
	SubTexture frameBufferSubTexture;

	@Override
	public void create() {

		squeezeTextures = squeezeTextures
				&& Gdx.files.isExternalStorageAvailable();

		if (Gdx.graphics.isGL20Available()) {
			frameBuffer = new FrameBuffer(Format.RGB565,
					Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
			frameBufferSubTexture = new SubTexture(
					frameBuffer.getColorBufferTexture());
			frameBufferSubTexture.flipY = true;

			// frameBuffer.getColorBufferTexture().getTextureData().prepare();
			// Pixmap pix =
			// frameBuffer.getColorBufferTexture().getTextureData().consumePixmap();
			// pix.setColor(1, 0, 0, 0.3f);
			// pix.drawRectangle(0, 0, frameBuffer.getWidth(),
			// frameBuffer.getHeight());
		}

		if (Gdx.graphics.isGL20Available())
			createShaders();

		// mesh = new Mesh(true, 4, 6, new VertexAttribute(
		// VertexAttributes.Usage.Position, 3,
		// ShaderProgram.POSITION_ATTRIBUTE),
		// new VertexAttribute(VertexAttributes.Usage.TextureCoordinates,
		// 2, ShaderProgram.TEXCOORD_ATTRIBUTE+"0"), new VertexAttribute(
		// VertexAttributes.Usage.TextureCoordinates, 2,
		// ShaderProgram.TEXCOORD_ATTRIBUTE+"1"));
		//
		// mesh.setVertices(new float[] { //
		// -0.5f, -0.5f, 0, //
		// 0, 1, //
		// 0, 1, //
		// 0.5f, -0.5f, 0, //
		// 1, 1, //
		// 1, 1, //
		// -0.5f, 0.5f, 0, //
		// 0, 0, //
		// 0, 0, //
		// 0.5f, 0.5f, 0, //
		// 1, 0, //
		// 1, 0 //
		// });
		// mesh.setIndices(new short[] { 0, 1, 2, 3 });
		//
		// tex1 = new Texture(Gdx.files.internal("data/planet_earth.png"));
		// tex2 = new
		// Texture(Gdx.files.internal("data/planet_heavyclouds.png"));

		create_preferences();
		//
		if (Gdx.files.isExternalStorageAvailable()) {
			String rootPath = Gdx.files.getExternalStoragePath();
			Gdx.app.log(APP_NAME, "External storage: " + rootPath);

			FileHandle dir1 = Gdx.files.external(BASE_FILENAME);
			if (!dir1.exists())
				dir1.mkdirs();
			Gdx.app.log(APP_NAME, "Created/checked directory: " + rootPath
					+ dir1.path());
			//
			FileHandle dir2 = Gdx.files.external(BASE_FILENAME + "/data/");
			if (!dir2.exists())
				dir2.mkdirs();
			Gdx.app.log(APP_NAME, "Created/checked directory: " + rootPath
					+ dir2.path());
			//
			FileHandle file = Gdx.files.external(BASE_FILENAME + "/README.txt");
			OutputStream stream = file.write(false);
			OutputStreamWriter writer = new OutputStreamWriter(stream,
					Charset.forName("US-ASCII"));
			try {
				writer.write("This is a cache folder,");
				writer.write('\n');
				writer.write("if you delete it,");
				writer.write('\n');
				writer.write("it will be re-created at application startup.");
				writer.close();
			} catch (IOException e) {
				// e.printStackTrace();
				Gdx.app.log(APP_NAME, e.getMessage());
			}

			Gdx.app.log(APP_NAME, "Wrote to file: " + rootPath + file.path());
		} else {
			Gdx.app.log(APP_NAME, "No external storage");
		}
		//
		soundCrush1 = Gdx.audio.newSound(Gdx.files.internal("data/crush1.wav"));
		soundPop1 = Gdx.audio.newSound(Gdx.files.internal("data/pop1.wav"));
		soundPop2 = Gdx.audio.newSound(Gdx.files.internal("data/pop2.wav"));
		soundShatter1 = Gdx.audio.newSound(Gdx.files
				.internal("data/shatter1.wav"));
		// soundShatter2 =
		// Gdx.audio.newSound(Gdx.files.internal("data/shatter2.wav"));
		soundWhip1 = Gdx.audio.newSound(Gdx.files.internal("data/whip1.wav"));
		soundSword1 = Gdx.audio.newSound(Gdx.files.internal("data/sword1.wav"));
		//
		sprite_ImagePaths = new Array<String>(20);
		sprite_ImagePaths.add("data/brick1.png");
		sprite_ImagePaths.add("data/brick2.png");
		sprite_ImagePaths.add("data/brick3.png");
		//
		sprite_ImagePaths.add("data/twitter_square.png");
		sprite_ImagePaths.add("data/twitter_square_angry.png");
		sprite_ImagePaths.add("data/twitter_square_happy.png");
		sprite_ImagePaths.add("data/twitter_square_sleeping.png");
		//
		sprite_ImagePaths.add("data/fish.png");
		sprite_ImagePaths.add("data/firebird.png");
		sprite_ImagePaths.add("data/pidgin.png");
		sprite_ImagePaths.add("data/twitter.png");
		sprite_ImagePaths.add("data/guitar.png");
		sprite_ImagePaths.add("data/mario.png");
		sprite_ImagePaths.add("data/mushrom.png");
		sprite_ImagePaths.add("data/snowman.png");
		sprite_ImagePaths.add("data/sonic.png");
		sprite_ImagePaths.add("data/webcam.png");
		sprite_ImagePaths.add("data/donkey.png");
		sprite_ImagePaths.add("data/chinchilla.png");
		sprite_ImagePaths.add("data/penguin.png");
		sprite_ImagePaths.add("data/skunk.png");
		sprite_ImagePaths.add("data/squirrel.png");
		sprite_ImagePaths.add("data/walrus.png");
		//
		sprite_ImagePaths.add("data/shiny-bee.png");
		sprite_ImagePaths.add("data/shiny-parrot.png");
		sprite_ImagePaths.add("data/dog.png");
		sprite_ImagePaths.add("data/lion.png");
		sprite_ImagePaths.add("data/bird-left.png");
		sprite_ImagePaths.add("data/bird-right.png");
		sprite_ImagePaths.add("data/bird-front.png");
		sprite_ImagePaths.add("data/tuqui.png");
		sprite_Textures = new Array<SubTexture>(sprite_ImagePaths.size);
		sprite_TexturesMeshes = new Array<Mesh>(sprite_ImagePaths.size);
		sprite_OutlineVertices = new Array<Array<Vector2>>(
				sprite_ImagePaths.size);
		sprite_BodyPolygons = new Array<Array<Array<Vector2>>>(
				sprite_ImagePaths.size);
		//
		cameraSCREEN = new OrthographicCamera(Gdx.graphics.getWidth(),
				Gdx.graphics.getHeight());
		cameraSCREEN.translate(Gdx.graphics.getWidth() / 2,
				Gdx.graphics.getHeight() / 2, 0);
		//
		updateZ(-50f);
		//
		if (preferences.getBoolean(PREF_USE_3D_CAM)) {
			cameraSCENE = cameraSCENEPerspective = new PerspectiveCamera(67,
					100, 50);
		} else {
			cameraSCENE = cameraSCENEOrthographic = new OrthographicCamera(100,
					50);
		}

		// Gdx.input = new RemoteInput(8190);
		multiTouchController = new MultiTouchController<LibGDXPlayground>(
				Gdx.input.isPeripheralAvailable(Peripheral.MultitouchScreen),
				this, true);
		//
		renderState = RenderState.Loading;
		// Gdx.input.vibrate(500);
		spriteBatch = new SpriteBatch(500); // default is 1000

		shapeRenderer = new ShapeRendererEx(500, false);

		box2d_renderer = new Box2DDebugRendererEx(true, true, false);

		// font = new BitmapFont();
		// font.setScale(2);

		fonts = new Array<BitmapFont>(20);
		fonts.add(new BitmapFont(Gdx.files.internal("data/verdana39.fnt"),
				Gdx.files.internal("data/verdana39.png"), false));
		fonts.add(new BitmapFont(Gdx.files.internal("data/sansation.fnt"),
				Gdx.files.internal("data/sansation.png"), false));
		fonts.add(new BitmapFont(Gdx.files.internal("data/playtime.fnt"),
				Gdx.files.internal("data/playtime.png"), false));
		fonts.add(new BitmapFont(Gdx.files.internal("data/goudy.fnt"),
				Gdx.files.internal("data/goudy.png"), false));
		fonts.add(new BitmapFont(Gdx.files.internal("data/comfortaa.fnt"),
				Gdx.files.internal("data/comfortaa.png"), false));
		fonts.add(new BitmapFont(Gdx.files.internal("data/oregon.fnt"),
				Gdx.files.internal("data/oregon.png"), false));
		fonts.add(new BitmapFont(Gdx.files.internal("data/cartoon.fnt"),
				Gdx.files.internal("data/cartoon.png"), false));
		fonts.add(new BitmapFont(Gdx.files.internal("data/pixie.fnt"),
				Gdx.files.internal("data/pixie.png"), false));

		fontCaches = new Array<BitmapFontCache>(fonts.size);

		for (int i = 0; i < fonts.size; i++) {
			BitmapFont font = fonts.get(i);
			font.setUseIntegerPositions(false);
			font.getRegion()
					.getTexture()
					.setFilter(Texture.TextureFilter.Linear,
							Texture.TextureFilter.Linear);

			BitmapFontCache bitmapFontCache = new BitmapFontCache(font);
			bitmapFontCache.setUseIntegerPositions(false);
			fontCaches.add(bitmapFontCache);
		}

		Gdx.input.setInputProcessor(this);
		//
		mouseJoints.ensureCapacity(MAX_TOUCH_POINTERS);
		for (int i = 0; i < MAX_TOUCH_POINTERS; i++) {
			mouseJoints.add(null);
		}
		world = new World(new Vector2(0, 0), true);
		world.setGravity(new Vector2(0, -10)); // default
												// top-down mass
		world.setAutoClearForces(false);

		world.setWarmStarting(true);
		// world.setContinuousPhysics(true);

		world.setContactFilter(new ContactFilter() {
			@Override
			public boolean shouldCollide(Fixture fixtureA, Fixture fixtureB) {

				Body bodyA = fixtureA.getBody();
				Body bodyB = fixtureB.getBody();

				if (bodyA == groundBody || bodyB == groundBody) {
					return true; // TODO: there should be one ground per z-layer
				}

				float zA = ((BodyUserData) bodyA.getUserData()).zCoord;
				float zB = ((BodyUserData) bodyB.getUserData()).zCoord;

				return zA == zB;

				//
				// Filter filter1 = fixtureA.getFilterData();
				// Filter filter2 = fixtureB.getFilterData();
				//
				// if (filter1.groupIndex == filter2.groupIndex
				// && filter1.groupIndex != 0) {
				// return filter1.groupIndex > 0;
				// }
				//
				// boolean collide = (filter1.maskBits & filter2.categoryBits)
				// != 0
				// && (filter1.categoryBits & filter2.maskBits) != 0;
				//
				// return collide;
			}
		});

		world.setDestructionListener(new DestructionListener() {
			// ????
		});
		world.setContactListener(new ContactListener() {

			@Override
			public void beginContact(Contact contact) {
			}

			@Override
			public void endContact(Contact contact) {
			}

			@Override
			public void preSolve(Contact contact, Manifold oldManifold) {
			}

			@Override
			public void postSolve(Contact contact, ContactImpulse impulse) {
			}
		});
		created = true;
	}

	void create_background(int index) {

		FileHandle file = Gdx.files.internal("data/background-aqua.jpg");
		backgroundTexture_Aqua = new SubTexture(new Texture(file));
		backgroundTexture_Aqua.texture.setFilter(Texture.TextureFilter.Linear,
				Texture.TextureFilter.Linear);
		backgroundTexture_Aqua.texture.setWrap(Texture.TextureWrap.ClampToEdge,
				Texture.TextureWrap.ClampToEdge);
		//
		FileHandle filez = Gdx.files.internal("data/pages.jpg");
		pagesTexture = new SubTexture(new Texture(filez));
		pagesTexture.texture.setFilter(Texture.TextureFilter.Linear,
				Texture.TextureFilter.Linear);
		pagesTexture.texture.setWrap(Texture.TextureWrap.ClampToEdge,
				Texture.TextureWrap.ClampToEdge);
		//
		//
		FileHandle file1 = Gdx.files.internal("data/page1.jpg");
		page1Texture = new SubTexture(new Texture(file1));
		page1Texture.texture.setFilter(Texture.TextureFilter.Linear,
				Texture.TextureFilter.Linear);
		page1Texture.texture.setWrap(Texture.TextureWrap.ClampToEdge,
				Texture.TextureWrap.ClampToEdge);
		//
		FileHandle file2 = Gdx.files.internal("data/page2.jpg");
		page2Texture = new SubTexture(new Texture(file2));
		page2Texture.texture.setFilter(Texture.TextureFilter.Linear,
				Texture.TextureFilter.Linear);
		page2Texture.texture.setWrap(Texture.TextureWrap.ClampToEdge,
				Texture.TextureWrap.ClampToEdge);
		//
		FileHandle file_ = Gdx.files.internal("data/background-paper.jpg");
		backgroundTexture_Paper = new SubTexture(new Texture(file_));
		backgroundTexture_Paper.texture.setFilter(Texture.TextureFilter.Linear,
				Texture.TextureFilter.Linear);
		backgroundTexture_Paper.texture.setWrap(
				Texture.TextureWrap.ClampToEdge,
				Texture.TextureWrap.ClampToEdge);
		//
		FileHandle file__ = Gdx.files.internal("data/background-parchemin.jpg");
		backgroundTexture_Parchemin = new SubTexture(new Texture(file__));
		backgroundTexture_Parchemin.texture.setFilter(
				Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
		backgroundTexture_Parchemin.texture.setWrap(
				Texture.TextureWrap.ClampToEdge,
				Texture.TextureWrap.ClampToEdge);
		//
		FileHandle file___ = Gdx.files.internal("data/background-repeat.jpg");
		backgroundTexture_Repeat = new SubTexture(new Texture(file___));
		backgroundTexture_Repeat.texture.setFilter(
				Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
		backgroundTexture_Repeat.texture.setWrap(Texture.TextureWrap.Repeat,
				Texture.TextureWrap.Repeat);

		// Texture texture = sprite_Textures.get(index);
		// Pixmap pixmap = new Pixmap(texture.getWidth(), texture.getHeight(),
		// Format.RGBA8888);
		// backgroundTexture = new Texture(pixmap.getWidth(),
		// pixmap.getHeight(),
		// pixmap.getFormat());
		// backgroundTexture.setFilter(Texture.TextureFilter.Nearest,
		// Texture.TextureFilter.Linear);
		// backgroundTexture.setWrap(Texture.TextureWrap.ClampToEdge,
		// Texture.TextureWrap.ClampToEdge);
		// Color color = new Color(0, 0, 0, 0);
		// for (int y = 0; y < pixmap.getHeight(); y++) {
		// for (int x = 0; x < pixmap.getWidth(); x++) {
		// // int color = array[x + y * pixmap2.getWidth()];
		// // long mask1 = (long) color & 0xFFFFFFFFL;
		// // float r = (mask1 & 0xFF000000) / 255.0f;
		// // float g = (mask1 & 0x00FF0000) / 255.0f;
		// // float b = (mask1 & 0x0000FF00) / 255.0f;
		// // float a = (mask1 & 0x000000FF);
		// // pixmap2.setColor();
		// pixmap.setColor(color);
		// pixmap.drawPixel(x, y);
		// }
		// }
		// Array<Array<Vector2>> boxPolygons = sprite_BoxPolygons.get(index);
		// Vector2 firstVertice = new Vector2(0, 0);
		// for (int i = 0; i < boxPolygons.size; i++) {
		// Array<Vector2> polygonVertices = boxPolygons.get(i);
		// for (int j = 0; j < polygonVertices.size; j++) {
		// Vector2 vertice1 = polygonVertices.get(j);
		// if (j == 0) {
		// firstVertice.x = vertice1.x;
		// firstVertice.y = vertice1.y;
		// }
		// Vector2 vertice2;
		// if (j == polygonVertices.size - 1)
		// vertice2 = firstVertice;
		// else
		// vertice2 = polygonVertices.get(j + 1);
		// pixmap.setColor(Color.BLUE);
		// pixmap.drawLine((int) vertice1.x, (int) vertice1.y,
		// (int) vertice2.x, (int) vertice2.y);
		// }
		// }
		// Array<Vector2> outline = sprite_OutlineVertices.get(index);
		// for (int i = 0; i < outline.size; i++) {
		// Vector2 vertice1 = outline.get(i);
		// if (i == 0) {
		// firstVertice.x = vertice1.x;
		// firstVertice.y = vertice1.y;
		// }
		// Vector2 vertice2;
		// if (i == outline.size - 1)
		// vertice2 = firstVertice;
		// else
		// vertice2 = outline.get(i + 1);
		// pixmap.setColor(Color.WHITE);
		// pixmap.drawLine((int) vertice1.x, (int) vertice1.y,
		// (int) vertice2.x, (int) vertice2.y);
		// }
		// backgroundTexture.draw(pixmap, 0, 0);
		// pixmap.dispose();
		// pixmap = null;
	}

	void create_effects() {
		effect = new ParticleEffect();
		effect.load(Gdx.files.internal("data/particles.p"),
				Gdx.files.internal("data"));
		// effect.allowCompletion();
		effect.setDuration(1000);
		effect.setPosition(Gdx.graphics.getWidth() / 2,
				Gdx.graphics.getHeight() / 2);
	}

	boolean squeezeTextures = true;
	TextureSqueezer[] textureSqueezers = new TextureSqueezer[10];
	int currentTextureSqueezer = 0;

	void finishCurrentTextureSqueezer() {
		if (textureSqueezers[currentTextureSqueezer].textureFinalized)
			return;

		textureSqueezers[currentTextureSqueezer].save(BASE_FILENAME,
				"squeezed_" + currentTextureSqueezer);
		textureSqueezers[currentTextureSqueezer].textureFinalize();
	}

	void create_sprite(String dataPath) {

		Gdx.app.log(APP_NAME, "Creating sprite [" + dataPath + "]");
		FileHandle file = Gdx.files.internal(dataPath);
		//
		int width = -1;
		int height = -1;
		SubTexture subtexture;
		//
		if (squeezeTextures) {
			if (textureSqueezers[currentTextureSqueezer] == null) {
				textureSqueezers[currentTextureSqueezer] = new TextureSqueezer(
						1024, 1024);
			}
			String path = BASE_FILENAME + "/" + "squeezed_"
					+ currentTextureSqueezer + ".png";
			FileHandle image = Gdx.files.external(path);
			if (image.exists()) {
				if (textureSqueezers[currentTextureSqueezer].pixmap == null) {
					textureSqueezers[currentTextureSqueezer]
							.open(BASE_FILENAME, "squeezed_"
									+ currentTextureSqueezer);
				}
				subtexture = textureSqueezers[currentTextureSqueezer]
						.subTexture(file.name());
				if (subtexture == null) {
					// finishCurrentTextureSqueezer();

					currentTextureSqueezer++;
					create_sprite(dataPath);
					return;
				}
			} else {
				// Pixmap pixmap = new Pixmap(file);
				subtexture = textureSqueezers[currentTextureSqueezer].insert(
						file,
						// pixmap, file.name(),
						true);
				if (subtexture == null) {
					finishCurrentTextureSqueezer();

					currentTextureSqueezer++;
					// pixmap.dispose();
					create_sprite(dataPath);
					return;
				}

				subtexture.texture = new Texture(file); // temporary, until
														// finishCurrentTextureSqueezer()
				// pixmap.dispose(); not until textureFinalize !!
			}

			sprite_Textures.add(subtexture);
			width = subtexture.width;
			height = subtexture.height;

		} else {
			Texture texture_ = create_sprite_TEXTURE(file);
			subtexture = new SubTexture(texture_);
			sprite_Textures.add(subtexture);
			width = texture_.getWidth();
			height = texture_.getHeight();
		}

		//
		Array<Vector2> outlineSquare = new Array<Vector2>();
		float ratio = SPRITES_SCALE_FACTOR / PIXEL_TO_WORLD_RATIO;
		float scaledHalfWidth = ratio * width / 2;
		float scaledHalfHeight = ratio * height / 2;
		outlineSquare.add(new Vector2(-scaledHalfWidth, scaledHalfHeight));
		outlineSquare.add(new Vector2(-scaledHalfWidth, -scaledHalfHeight));
		outlineSquare.add(new Vector2(scaledHalfWidth, -scaledHalfHeight));
		outlineSquare.add(new Vector2(scaledHalfWidth, scaledHalfHeight));
		Mesh mesh = create_TextureMesh(outlineSquare, subtexture);
		sprite_TexturesMeshes.add(mesh);
		//
		boolean debugSquare = false;
		Array<Vector2> outline = null;

		boolean createOutline = true;
		boolean saveOutline = true;
		FileHandle fileOutline = null;
		String rootPath = null;

		if (Gdx.files.isExternalStorageAvailable()) {
			rootPath = Gdx.files.getExternalStoragePath();
			// Gdx.app.log(APP_NAME, "External storage: " + rootPath);
			//
			fileOutline = Gdx.files.external(BASE_FILENAME + "/" + dataPath
					+ ".outline.txt");

			if (fileOutline.exists()) {
				createOutline = false;
				saveOutline = false;

				InputStream stream = fileOutline.read();
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(stream,
								Charset.forName("US-ASCII")));

				try {
					String line = reader.readLine();
					int n = Integer.parseInt(line);
					outline = new Array<Vector2>(n);

					for (int i = 0; i < n; i++) {
						line = reader.readLine();
						float x = Float.parseFloat(line);
						line = reader.readLine();
						float y = Float.parseFloat(line);

						outline.add(new Vector2(x, y));
					}

					reader.close();

					Gdx.app.log(APP_NAME, "Read outline from file: " + rootPath
							+ fileOutline.path());
				} catch (Exception e) {
					// e.printStackTrace();
					Gdx.app.log(APP_NAME, e.getMessage());
					try {
						reader.close();
					} catch (IOException e1) {
						// e1.printStackTrace();
					}

					if (fileOutline.exists()) {
						fileOutline.delete();
					}
					outline = null;
					createOutline = true;
					saveOutline = true;
				}
			} else {
				createOutline = true;
				saveOutline = true;
			}
		} else {
			createOutline = true;
			saveOutline = false;
			Gdx.app.log(APP_NAME, "No external storage");
		}

		if (createOutline) {
			simulateLatencyOnDesktop();
			if (!debugSquare) {
				outline = create_sprite_OUTLINE(file);
			} else {
				outline = new Array<Vector2>(5);
				outline.add(new Vector2(0, 0));
				outline.add(new Vector2(width, 0));
				outline.add(new Vector2(width, height));
				outline.add(new Vector2(0, height));

				if (!BayazitDecomposer.IsCounterClockWise(outline)) {
					Gdx.app.log(APP_NAME,
							"Clockwise outline vertices (in debug SQUARE body), reversing... ["
									+ sprite_Textures.size + "]");
					// LibGDXPlayground.reverse(outline);
					outline.reverse();

					if (!BayazitDecomposer.IsCounterClockWise(outline)) {
						Gdx.app.log(APP_NAME,
								"Clockwise outline vertices ?! WTF ? ["
										+ sprite_Textures.size + "]");
					}
				}
			}
			if (saveOutline) {
				OutputStream stream = fileOutline.write(false);
				OutputStreamWriter writer = new OutputStreamWriter(stream,
						Charset.forName("US-ASCII"));
				try {
					writer.write(Integer.toString(outline.size));
					writer.write('\n');
					for (int i = 0; i < outline.size; i++) {
						Vector2 v = outline.get(i);
						writer.write(Float.toString(v.x));
						writer.write('\n');
						writer.write(Float.toString(v.y));
						writer.write('\n');
					}

					writer.close();

					Gdx.app.log(APP_NAME, "Wrote outline to file: " + rootPath
							+ fileOutline.path());
				} catch (IOException e) {
					// e.printStackTrace();
					Gdx.app.log(APP_NAME, e.getMessage());
					try {
						writer.close();
					} catch (IOException e1) {
						// e1.printStackTrace();
					}
					if (fileOutline.exists()) {
						fileOutline.delete();
					}
				}
			}
		}

		sprite_OutlineVertices.add(outline);
		//
		Array<Array<Vector2>> listPolygons = null;

		boolean createPolygons = true;
		boolean savePolygons = true;
		FileHandle filePolygons = null;

		if (Gdx.files.isExternalStorageAvailable()) {
			rootPath = Gdx.files.getExternalStoragePath();
			// Gdx.app.log(APP_NAME, "External storage: " + rootPath);
			//
			filePolygons = Gdx.files.external(BASE_FILENAME + "/" + dataPath
					+ ".polygons.txt");

			if (filePolygons.exists()) {
				createPolygons = false;
				savePolygons = false;

				InputStream stream = filePolygons.read();
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(stream,
								Charset.forName("US-ASCII")));

				try {
					String line = reader.readLine();
					int n = Integer.parseInt(line);
					listPolygons = new Array<Array<Vector2>>(n);

					for (int i = 0; i < n; i++) {
						line = reader.readLine();
						int nn = Integer.parseInt(line);
						Array<Vector2> polygon = new Array<Vector2>(nn);

						for (int j = 0; j < nn; j++) {

							line = reader.readLine();
							float x = Float.parseFloat(line);
							line = reader.readLine();
							float y = Float.parseFloat(line);

							polygon.add(new Vector2(x, y));
						}

						listPolygons.add(polygon);
					}

					reader.close();

					Gdx.app.log(APP_NAME, "Read polygons from file: "
							+ rootPath + filePolygons.path());
				} catch (Exception e) {
					// e.printStackTrace();
					Gdx.app.log(APP_NAME, e.getMessage());

					try {
						reader.close();
					} catch (IOException e1) {
						// e1.printStackTrace();
					}
					if (filePolygons.exists()) {
						filePolygons.delete();
					}
					listPolygons = null;
					createPolygons = true;
					savePolygons = true;
				}
			} else {
				createPolygons = true;
				savePolygons = true;
			}
		} else {
			createPolygons = true;
			savePolygons = false;
			Gdx.app.log(APP_NAME, "No external storage");
		}

		if (createPolygons) {
			simulateLatencyOnDesktop();
			if (!debugSquare) {
				listPolygons = create_sprite_POLYGONS(outline);
			} else {
				//
				listPolygons = new Array<Array<Vector2>>(1);
				Array<Vector2> vertices = new Array<Vector2>(5);
				vertices.add(new Vector2(0, height));
				vertices.add(new Vector2(width, height));
				vertices.add(new Vector2(width, 0));
				vertices.add(new Vector2(0, 0));

				if (BayazitDecomposer.IsCounterClockWise(vertices)) {
					Gdx.app.log(
							"DEBUG",
							"Counter Clockwise body polygon vertices (in debug SQUARE body), reversing... ["
									+ sprite_Textures.size + "]");
					// LibGDXPlayground.reverse(outline);
					vertices.reverse();

					if (BayazitDecomposer.IsCounterClockWise(vertices)) {
						Gdx.app.log(APP_NAME,
								"Counter Clockwise body polygon vertices ?! WTF ? ["
										+ sprite_Textures.size + "]");
					}
				}
				listPolygons.add(vertices);
			}

			if (savePolygons) {
				OutputStream stream = filePolygons.write(false);
				OutputStreamWriter writer = new OutputStreamWriter(stream,
						Charset.forName("US-ASCII"));
				try {
					writer.write(Integer.toString(listPolygons.size));
					writer.write('\n');

					for (int i = 0; i < listPolygons.size; i++) {
						Array<Vector2> polygon = listPolygons.get(i);

						writer.write(Integer.toString(polygon.size));
						writer.write('\n');

						for (int j = 0; j < polygon.size; j++) {
							Vector2 v = polygon.get(j);

							writer.write(Float.toString(v.x));
							writer.write('\n');
							writer.write(Float.toString(v.y));
							writer.write('\n');
						}
					}

					writer.close();

					Gdx.app.log(APP_NAME, "Wrote polygons to file: " + rootPath
							+ filePolygons.path());
				} catch (IOException e) {
					// e.printStackTrace();
					Gdx.app.log(APP_NAME, e.getMessage());
					try {
						writer.close();
					} catch (IOException e1) {
						// e1.printStackTrace();
					}
					if (filePolygons.exists()) {
						filePolygons.delete();
					}
				}
			}
		}

		short added = checkBodyPolygonsAreaAndAdd(listPolygons);
		if (added == -1) {
			Gdx.app.log(APP_NAME, "SPRITE NOT CREATED, TOO SMALL ["
					+ (sprite_Textures.size + 1) + "]");
		}
	}

	Texture create_sprite_TEXTURE(FileHandle file) {
		// Pixmap pixmap = new Pixmap(file);
		Texture texture = new Texture(file);
		// pixmap.dispose();

		texture.setFilter(Texture.TextureFilter.Linear,
				Texture.TextureFilter.Linear);
		texture.setWrap(Texture.TextureWrap.ClampToEdge,
				Texture.TextureWrap.ClampToEdge);

		return texture;
	}

	Array<Vector2> create_sprite_OUTLINE(FileHandle file) {
		Pixmap.setBlending(Blending.None);
		Pixmap pixmap = new Pixmap(file);
		int size = pixmap.getWidth() * pixmap.getHeight();
		int[] array = new int[size];
		for (int y = 0; y < pixmap.getHeight(); y++) {
			for (int x = 0; x < pixmap.getWidth(); x++) {
				int color = pixmap.getPixel(x, y);
				array[x + y * pixmap.getWidth()] = color;
			}
		}
		int w = pixmap.getWidth();
		int h = pixmap.getHeight();
		// pixmap.getPixels().array()
		Format format = pixmap.getFormat();
		pixmap.dispose();
		pixmap = null;
		Array<Vector2> outline = null;
		try {
			outline = TextureConverter.createPolygon(array, w, h);
		} catch (Exception e) {
			Gdx.app.log(APP_NAME, e.getMessage());
		} finally {
			//
		}
		// if (outline == null) {
		//
		// return;
		// FIXME: should abandon !
		// }

		if (!BayazitDecomposer.IsCounterClockWise(outline)) {
			// Normally, doesn't happen :)
			Gdx.app.log(
					"DEBUG",
					"Clockwise outline vertices (after TextureConverter.createPolygon), reversing... ["
							+ sprite_Textures.size + "]");
			// LibGDXPlayground.reverse(outline);
			outline.reverse();

			if (!BayazitDecomposer.IsCounterClockWise(outline)) {
				Gdx.app.log(APP_NAME, "Clockwise outline vertices ?! WTF ? ["
						+ sprite_Textures.size + "]");
			}
		}

		return outline;
	}

	Array<Array<Vector2>> create_sprite_POLYGONS(Array<Vector2> outline) {

		if (!BayazitDecomposer.IsCounterClockWise(outline)) {
			Gdx.app.log(APP_NAME,
					"Clockwise outline vertices (passed in create_sprite_POLYGONS), reversing... ["
							+ sprite_Textures.size + "]");
			// LibGDXPlayground.reverse(outline);
			outline.reverse();

			if (!BayazitDecomposer.IsCounterClockWise(outline)) {
				Gdx.app.log(APP_NAME, "Clockwise outline vertices ?! WTF ? ["
						+ sprite_Textures.size + "]");
			}
		}

		Array<Array<Vector2>> polygons = BayazitDecomposer
				.ConvexPartition(outline); // outline must be CounterClockWise
											// (although it gets reversed inside
											// function ...)

		for (int j = 0; j < polygons.size; j++) {
			Array<Vector2> polygon = polygons.get(j);
			if (BayazitDecomposer.IsCounterClockWise(polygon)) {
				Gdx.app.log(
						"DEBUG",
						"Counter Clockwise body polygon vertices (after BayazitDecomposer.ConvexPartition), reversing... ["
								+ sprite_Textures.size
								+ "] "
								+ j
								+ "/"
								+ polygons.size);
				// LibGDXPlayground.reverse(list);
				polygon.reverse();
				if (BayazitDecomposer.IsCounterClockWise(polygon)) {
					Gdx.app.log(APP_NAME,
							"Counter Clockwise body polygon vertices ?! WTF ? ["
									+ sprite_Textures.size + "] " + j + "/"
									+ polygons.size);
				}
			} else {
				boolean breakpoint = true;
			}
		}
		return polygons;
		//
		// //
		// int nVertices = 0;
		// for (int z = 0; z < listPolygons.size; z++)
		// {
		// Array<Vector2> vertices = listPolygons.get(z);
		// for (int j = 0; j < vertices.size; j++)
		// {
		// nVertices++;
		// }
		// }
		// // float ratio = SPRITES_SCALE_FACTOR / PIXEL_TO_WORLD_RATIO;
		// int nIndices = nVertices;
		// float[] meshVertices = new float[nVertices * 3];
		// short[] meshIndices = new short[nIndices];
		// int verticeCounter = 0;
		// int indiceCounter = 0;
		// for (int z = 0; z < listPolygons.size; z++)
		// {
		// Array<Vector2> vertices = listPolygons.get(z);
		// for (int j = 0; j < vertices.size; j++)
		// {
		// Vector2 vertex = vertices.get(j);
		// meshVertices[verticeCounter] = vertex.x - w / 2;
		// verticeCounter++;
		// meshVertices[verticeCounter] = h / 2 - vertex.y;
		// verticeCounter++;
		// meshVertices[verticeCounter] = 0;
		// verticeCounter++;
		// meshIndices[indiceCounter] = (short) indiceCounter;
		// indiceCounter++;
		// }
		// }
		// Mesh mesh = new Mesh(true, nVertices, nIndices, new VertexAttribute(
		// Usage.Position, 3, ShaderProgram.POSITION_ATTRIBUTE));
		// mesh.setVertices(meshVertices);
		// mesh.setIndices(meshIndices);
		// sprite_BodyPolygonsMeshes.add(mesh);
		//
		//
		/*
		 * EarClippingTriangulator boss = new EarClippingTriangulator();
		 * Array<Vector2> listTriangles = (Array<Vector2>) boss
		 * .computeTriangles(outline); sprite_BodyTriangles.add(listTriangles);
		 */
		/*
		 * Array<Vector2> listTriangles = new Array<Vector2>(
		 * listTriangles_.size); for (int i = 0; i < listTriangles_.size; i++) {
		 * Array<Vector2> list = listTriangles_.get(i);
		 * listTriangles.addAll(list); }
		 */
		/*
		 * Gdx.app.log(APP_NAME, "-----"); Polygon poly = new
		 * Polygon(verts.toArray(new Vector2[] {})); Vector2[] vertices =
		 * poly.getVertexVecs(); for (int i = 0; i < vertices.length; i++) {
		 * Vector2 vect = vertices[i]; Gdx.app.log(APP_NAME, "x:" + vect.x +
		 * ", y:" + vect.y); } Gdx.app.log(APP_NAME, "====="); if
		 * (!poly.isSimple()) { Polygon tracedPoly = Polygon.traceEdge(poly);
		 * vertices = tracedPoly.getVertexVecs(); for (int i = 0; i <
		 * vertices.length; i++) { Vector2 vect = vertices[i];
		 * Gdx.app.log(APP_NAME, "x:" + vect.x + ", y:" + vect.y); } poly =
		 * tracedPoly; } Gdx.app.log(APP_NAME, "-----"); PolygonShape proto =
		 * new PolygonShape(); Polygon.decomposeConvexAndAddTo(poly, body,
		 * proto); // FixtureDef fixtureDef = new FixtureDef(); //
		 * fixtureDef.shape = proto; // fixtureDef.filter.groupIndex = 0; //
		 * body.createFixture(fixtureDef); Array<Fixture> list =
		 * body.getFixtureList(); for (int j = 0; j < list.size; j++) { Fixture
		 * fix = list.get(j); PolygonShape polyShape = (PolygonShape)
		 * fix.getShape(); for (int i = 0; i < polyShape.getVertexCount(); i++)
		 * { Vector2 vect = new Vector2(); polyShape.getVertex(i, vect);
		 * Gdx.app.log(APP_NAME, "x:" + vect.x + ", y:" + vect.y); } }
		 * Gdx.app.log(APP_NAME, "#####"); proto.dispose();
		 */
		//
	}

	void initWorld_ground() {
		BodyDef groundBodyDef = new BodyDef();
		groundBodyDef.type = BodyType.KinematicBody; // StaticBody;
		groundBodyDef.position.x = 0;
		groundBodyDef.position.y = 0;
		groundBodyDef.angle = 0;
		groundBodyDef.allowSleep = true;
		groundBodyDef.awake = true;
		groundBodyDef.active = true;
		groundBodyDef.fixedRotation = false;
		// groundBodyDef.angularDamping = (float) (Math.PI * 2);
		// groundBodyDef.linearDamping = 0.5f;
		// groundBodyDef.angularVelocity = 0;
		// groundBodyDef.linearVelocity.set(0, 0);
		groundBodyDef.active = true;
		groundBodyDef.bullet = false;
		// groundBodyDef.inertiaScale

		groundBody = world.createBody(groundBodyDef);
		PolygonShape groundPoly = new PolygonShape();

		float halfWidth = (worldSize.x / GROUND_SIZE_FRACTION.x) / 2;
		float halfHeight = (worldSize.y / GROUND_SIZE_FRACTION.y) / 2;

		if (true) {
			groundPoly.setAsBox(halfWidth, halfHeight); // half width and height
														// (meters)
		} else {
			// groundPoly.setAsEdge(new Vector2(-halfWidth, halfHeight), new
			// Vector2(halfWidth, halfHeight));
			// groundBody.createFixture(groundPoly, 0);
		}
		//
		FixtureDef fixtureDef = new FixtureDef();
		fixtureDef.shape = groundPoly;
		fixtureDef.filter.groupIndex = 0; // 0 => no collision
											// group,negative =>
											// never collide, positive =>
											// always
											// collide
		fixtureDef.filter.categoryBits = 0x0000;
		fixtureDef.filter.maskBits = 0x0000;
		fixtureDef.isSensor = false;
		fixtureDef.density = 1;
		fixtureDef.friction = 0.4f;
		fixtureDef.restitution = 0.0f;

		groundBody.createFixture(fixtureDef);
		//
		groundPoly.dispose();
		//
		groundMesh = new Mesh(true, 4, 4, new VertexAttribute(Usage.Position,
				3, ShaderProgram.POSITION_ATTRIBUTE), new VertexAttribute(
				Usage.ColorPacked, 4, ShaderProgram.COLOR_ATTRIBUTE),
				new VertexAttribute(Usage.TextureCoordinates, 2,
						ShaderProgram.TEXCOORD_ATTRIBUTE + "0"));

		// groundMesh.render(shader, GL10.GL_TRIANGLE_STRIP)
		groundMesh.setVertices(new float[] { //
				-halfWidth, -halfHeight, zCoord_box2D, //
						Color.toFloatBits(0, 0, 0, 255), //
						0, 1, //
						halfWidth, -halfHeight, zCoord_box2D, //
						Color.toFloatBits(255, 0, 0, 255), //
						1, 1, //
						-halfWidth, halfHeight, zCoord_box2D, //
						Color.toFloatBits(0, 255, 0, 255), //
						0, 0, //
						halfWidth, halfHeight, zCoord_box2D, //
						Color.toFloatBits(0, 0, 255, 255), //
						1, 0 //
				});
		groundMesh.setIndices(new short[] { 0, 1, 2, 3 });
		//
		//
		// groundBorderMesh = new Mesh(true, 5, 5, new VertexAttribute(
		// Usage.Position, 3, ShaderProgram.POSITION_ATTRIBUTE)
		// // new VertexAttribute(Usage.ColorPacked, 4,
		// ShaderProgram.COLOR_ATTRIBUTE)
		// );
		// groundBorderMesh.setVertices(new float[] { //
		// -halfWidth, -halfHeight, box2D_zCoord,
		// // Color.toFloatBits(255, 255, 255, 255), //
		// -halfWidth, halfHeight, box2D_zCoord,
		// // Color.toFloatBits(255, 255, 255, 255), //
		// halfWidth, halfHeight, box2D_zCoord,
		// // Color.toFloatBits(255, 255, 255, 255), //
		// halfWidth, -halfHeight, box2D_zCoord,
		// // Color.toFloatBits(255, 255, 255, 255), //
		// -halfWidth, -halfHeight, box2D_zCoord,
		// // Color.toFloatBits(255, 255, 255, 255) //
		// });
		// groundBorderMesh.setIndices(new short[] { 0, 1, 2, 3, 4 });
		//
		// groundBody.setUserData(groundMesh);
		//

		float minX = getWorldMinX();
		float maxX = getWorldMaxX();
		float minY = getWorldMinY();
		float maxY = getWorldMaxY();

		waterRipples = new WaterRipples(shader_POSITION_TEX1, cameraSCENE,
				zCoord_Background, minX, minY, (short) Math.abs(maxX - minX),
				(short) Math.abs(maxY - minY), backgroundTexture_Aqua);

		if (usePageTurn) {
			pageTurn = new PageTurn(shader_POSITION_COLOR_TEX2, cameraSCENE,
					zCoord_Background, new Vector2(minX, maxY), new Vector2(
							minX, minY), new Vector2(maxX, maxY), new Vector2(
							maxX, minY), 10, backgroundTexture_Aqua);
		}

		initWorld_ground_Tween();
	}

	void initWorld_ground_Tween() {

		if (groundTweenManager != null) {
			groundTweenManager.kill();
			groundTweenManager = null;
		}
		groundTweenManager = new TweenManager();

		// Tween.update();
		// groundTweenable.tweenUpdated(-1, new float[] { 0 });

		float val = worldSize.x / GROUND_SIZE_FRACTION.x / 4;

		TweenGroup group = new TweenGroup()
				.pack(Tween.to(groundTweenable, -1, 1000, Back.IN).target(-val, // -
																				// Width
						-val / 2 // -halfWidth
						),
						Tween.to(groundTweenable, -1, 1000, Back.OUT).target(0,
								0),
						Tween.to(groundTweenable, -1, 1000, Back.IN).target(
								val, // Width
								val / 2 // halfWidth
								),
						Tween.to(groundTweenable, -1, 1000, Back.OUT).target(0,
								0)).sequence().repeat(Tween.INFINITY, 500)
				.start();
		groundTweenManager.add(group); // The Group is empty after that !
	}

	public static boolean areVerticesClockwise(Array<Vector2> vertices) {
		float area = 0;
		for (int i = 0; i < vertices.size; i++) {
			final Vector2 p1 = vertices.get(i);
			final Vector2 p2 = vertices.get((i + 1) % vertices.size);
			area += p1.x * p2.y - p2.x * p1.y;
		}
		if (area < 0) {
			return true;
		} else {
			return false;
		}
	}

	public static boolean areVerticesClockwise(Vector2[] vertices) {
		float area = 0;
		for (int i = 0; i < vertices.length; i++) {
			final Vector2 p1 = vertices[i];
			final Vector2 p2 = vertices[(i + 1) % vertices.length];
			area += p1.x * p2.y - p2.x * p1.y;
		}
		if (area < 0) {
			return true;
		} else {
			return false;
		}
	}

	// // float color = Color.WHITE.toFloatBits();
	// float getColorFloat(float r, float g, float b, float a) {
	// int intBits = (int) (255 * a) << 24 | (int) (255 * b) << 16
	// | (int) (255 * g) << 8 | (int) (255 * r);
	// return Float.intBitsToFloat(intBits & 0xfeffffff);
	// }
	// public Color getColor () {
	//
	// int intBits = Float.floatToRawIntBits(color);
	// Color color = this.tempColor;
	// color.r = (intBits & 0xff) / 255f;
	// color.g = ((intBits >>> 8) & 0xff) / 255f;
	// color.b = ((intBits >>> 16) & 0xff) / 255f;
	// color.a = ((intBits >>> 24) & 0xff) / 255f;
	// return color;
	// }
	//
	Mesh create_TextureMesh(Array<Vector2> outline, SubTexture subtexture) {
		float ratio = SPRITES_SCALE_FACTOR / PIXEL_TO_WORLD_RATIO;

		Mesh mesh = new Mesh(true, outline.size, outline.size,
				new VertexAttribute(Usage.Position, 3,
						ShaderProgram.POSITION_ATTRIBUTE),
				// new VertexAttribute(Usage.ColorPacked, 4,
				// ShaderProgram.COLOR_ATTRIBUTE),
				new VertexAttribute(Usage.TextureCoordinates, 2,
						ShaderProgram.TEXCOORD_ATTRIBUTE + "0"));

		int stride = 5;
		float[] vertices = new float[outline.size * stride];
		short[] indices = new short[outline.size];

		// float color = Color.WHITE.toFloatBits();

		int j = -1;
		int offset = -1;
		for (int i = 0; i < outline.size; i++) {
			Vector2 vect = outline.get(i);
			j = i * stride;
			offset = -1;
			++offset;
			vertices[j + offset] = vect.x;
			++offset;
			vertices[j + offset] = vect.y;
			++offset;
			vertices[j + offset] = 0;
			//
			// ++offset;
			// vertices[j + offset] = color; // Color.toFloatBits(0, 255, 0,
			// 255);
			//
			float u = (vect.x / ratio + subtexture.width / 2)
					/ subtexture.width;
			float v = (subtexture.height / 2 - vect.y / ratio)
					/ subtexture.height;

			if (squeezeTextures) {
				u = (vect.x / ratio + subtexture.width / 2 + subtexture.left)
						/ subtexture.textureSqueezer.rootNode.subTexture.width;
				// subtexture.texture.getWidth();
				v = (subtexture.height / 2 - vect.y / ratio + subtexture.top)
						/ subtexture.textureSqueezer.rootNode.subTexture.width;
				// subtexture.texture.getHeight();
			}

			++offset;
			vertices[j + offset] = u; // u
			++offset;
			vertices[j + offset] = v; // v
			//
			indices[i] = (short) i;
		}

		// mesh.render(shader, GL10.GL_TRIANGLE_FAN)
		mesh.setVertices(vertices);
		mesh.setIndices(indices);

		return mesh;
	}

	// bodyPolygons can be null ! (built from outline decomposition)
	Body generate_spriteBody_subBody(Array<Vector2> outline,
			Array<Array<Vector2>> bodyPolygons, Body body, float restitution,
			float angularDamping, float linearDamping, float friction,
			float density) {

		// float area = BayazitDecomposer.GetSignedArea(outline);
		// if (Math.abs(area) <= 1.2f) {
		// Gdx.app.log(APP_NAME, "Signed area too small [" + area + "]");
		// return null;
		// }
		BodyUserData userData = (BodyUserData) body.getUserData();
		int bodyIndex = userData.textureIndex;

		float ratio = SPRITES_SCALE_FACTOR / PIXEL_TO_WORLD_RATIO;
		SubTexture subtexture = sprite_Textures.get(bodyIndex);
		// Vector2 scaledBodySize = new Vector2(ratio *
		// texture.getWidth(), ratio * texture.getHeight());
		sprite_Textures.add(subtexture);
		//
		// float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
		// float maxX = Float.MIN_VALUE, maxY = Float.MIN_VALUE;
		// for (int v = 0; v < outline.size; v++) {
		// Vector2 vector = outline.get(v);
		// if (vector.x > maxX)
		// maxX = vector.x;
		// if (vector.y > maxY)
		// maxY = vector.y;
		//
		// if (vector.x < minX)
		// minX = vector.x;
		// if (vector.y < minY)
		// minY = vector.y;
		// }
		// Vector2 center = new Vector2((maxX - minX) / 2, (maxY - minY) / 2);
		//
		Mesh mesh = create_TextureMesh(outline, subtexture);
		sprite_TexturesMeshes.add(mesh);
		//
		for (int v = 0; v < outline.size; v++) {
			Vector2 vector = outline.get(v);
			vector.x += (subtexture.width / 2) * ratio;
			vector.y = (subtexture.height / 2) * ratio - vector.y;
			vector.x /= ratio;
			vector.y /= ratio;
		}
		//
		Array<Vector2> outlinePolygon = new Array<Vector2>(outline.size);
		for (int j = 0; j < outline.size; j++) {
			Vector2 ve = outline.get(j);
			outlinePolygon.add(ve.cpy());
		}
		//
		if (!BayazitDecomposer.IsCounterClockWise(outline)) {
			Gdx.app.log(APP_NAME,
					"Clockwise outline vertices (in generate_spriteBody chunk), reversing... ["
							+ sprite_Textures.size + "]");
			// LibGDXPlayground.reverse(outline);
			outline.reverse();

			if (!BayazitDecomposer.IsCounterClockWise(outline)) {
				Gdx.app.log(APP_NAME, "Clockwise outline vertices ?! WTF ? ["
						+ sprite_Textures.size + "]");
			}
		}
		sprite_OutlineVertices.add(outline);
		//
		Array<Array<Vector2>> listPolygons;
		if (bodyPolygons != null) {
			listPolygons = bodyPolygons;
			for (int w = 0; w < listPolygons.size; w++) {
				Array<Vector2> poly = listPolygons.get(w);

				float area = BayazitDecomposer.GetSignedArea(poly);
				if (area < 0) {
					// Gdx.app.log(APP_NAME, "Signed area negative [" + area +
					// "]");

					int breakpoint = 1;

					// poly.reverse();

					// area = BayazitDecomposer.GetSignedArea(poly);
					// if (area < 0) {
					// // Gdx.app.log(APP_NAME, "Signed area negative [" + area
					// // +
					// // "]");
					// int breakpoint = 1;
					// }
				}
				// if (Math.abs(area) <= 1.2f) {
				// //Gdx.app.log(APP_NAME, "Signed area too small [" + area +
				// "]");
				// int breakpoint = 1;
				// }

				// if (!BoxCutter.SanityCheck(poly)) {
				//
				// // Gdx.app.log(APP_NAME,
				// // "Damn, and polygon not even sane !!");
				//
				// listPolygons.removeIndex(w);
				//
				// if (listPolygons.size == 0) {
				// sprite_OutlineVertices
				// .removeIndex(sprite_OutlineVertices.size - 1);
				// sprite_ChunkMeshes.remove(sprite_Textures.size);
				// sprite_Textures.removeIndex(sprite_Textures.size - 1);
				// return null;
				// }
				//
				// continue;
				// }

				for (int v = 0; v < poly.size; v++) {
					Vector2 vector = poly.get(v);

					vector.x += (subtexture.width / 2) * ratio;
					vector.y = (subtexture.height / 2) * ratio - vector.y;
					vector.x /= ratio;
					vector.y /= ratio;
				}

				// if (BayazitDecomposer.IsCounterClockWise(poly)) {
				// Gdx.app.log(
				// "DEBUG",
				// "Counter Clockwise box polygon vertices (in create_sprite_POLYGONS() body chunk fail), reversing... ["
				// + sprite_Textures.size
				// + "] "
				// + w
				// + "/"
				// + listPolygons.size);
				// // LibGDXPlayground.reverse(list);
				// poly.reverse();
				// if (BayazitDecomposer.IsCounterClockWise(poly)) {
				// Gdx.app.log(APP_NAME,
				// "Counter Clockwise box polygon vertices ?! WTF ? ["
				// + sprite_Textures.size + "] " + w
				// + "/" + listPolygons.size);
				// }
				// }
			}
		} else {
			listPolygons = create_sprite_POLYGONS(outline);
			//
			// try {
			// listPolygons = create_sprite_POLYGONS(outline);
			// } catch (Exception e) {
			// Gdx.app.log(APP_NAME,
			// "Couldn't create_sprite_POLYGONS() in body chunk ?!");
			//
			// if (!BoxCutter.SanityCheck(outlinePolygon)) {
			//
			// Gdx.app.log(APP_NAME, "Damn, and polygon not even sane !!");
			//
			// sprite_OutlineVertices
			// .removeIndex(sprite_OutlineVertices.size - 1);
			// sprite_ChunkMeshes.remove(sprite_Textures.size);
			// sprite_Textures.removeIndex(sprite_Textures.size - 1);
			// return null;
			// }
			//
			// listPolygons = new Array<Array<Vector2>>(1);
			// listPolygons.add(outlinePolygon);
			// for (int j = 0; j < listPolygons.size; j++) {
			// Array<Vector2> list = listPolygons.get(j);
			// if (BayazitDecomposer.IsCounterClockWise(list)) {
			// Gdx.app.log(
			// "DEBUG",
			// "Counter Clockwise box polygon vertices (in create_sprite_POLYGONS() body chunk fail), reversing... ["
			// + sprite_Textures.size
			// + "] "
			// + j
			// + "/"
			// + listPolygons.size);
			// // LibGDXPlayground.reverse(list);
			// list.reverse();
			// if (BayazitDecomposer.IsCounterClockWise(list)) {
			// Gdx.app.log(APP_NAME,
			// "Counter Clockwise box polygon vertices ?! WTF ? ["
			// + sprite_Textures.size + "] " + j
			// + "/" + listPolygons.size);
			// }
			// }
			// }
			// }
		}
		//
		short added = checkBodyPolygonsAreaAndAdd(listPolygons);
		if (added == -1) {
			Gdx.app.log(APP_NAME, "CUT BODY NOT CREATED, TOO SMALL ["
					+ (sprite_Textures.size + 1) + "]");
			return null;
		}
		//
		Gdx.app.log(APP_NAME, "CREATING CUT BODY [" + sprite_Textures.size
				+ "]");
		//
		// Vector2 position = placeRandom(sprite_Textures.size - 1);
		// Vector2 position = body.getWorldCenter()

		Body newBody = generate_spriteBody_fromExistingTextureAndPolygons(
				body.getPosition().x, // + center.x,
				body.getPosition().y, // + center.y,
				userData.zCoord, body.getAngle(), sprite_Textures.size - 1,
				restitution, angularDamping, linearDamping, friction, density);

		if (added == 0) {
			if (!bodiesToDestroy.contains(newBody, true))
				bodiesToDestroy.add(newBody);
		}
		return newBody;
	}

	Array<Body> bodiesToDestroy = new Array<Body>(10);

	void checkBodyPolygonsAreasToDestroyBodies() {

		for (int i = sprite_ImagePaths.size; i < sprite_BodyPolygons.size; i++) {

			Array<Array<Vector2>> listPolygons = sprite_BodyPolygons.get(i);
			float totalArea = 0;
			for (int j = 0; j < listPolygons.size; j++) {
				Array<Vector2> poly = listPolygons.get(j);

				float area = BayazitDecomposer.GetSignedArea(poly);
				area = Math.abs(area);
				totalArea += area;
			}

			if (totalArea <= minArea) {
				for (Iterator<Body> iter = world.getBodies(); iter.hasNext();) {
					Body b = iter.next();
					if (b == groundBody)
						continue;
					BodyUserData userData = (BodyUserData) b.getUserData();
					int index = userData.textureIndex;
					if (index == i) {
						if (!bodiesToDestroy.contains(b, true)) {
							bodiesToDestroy.add(b);

							Gdx.app.log(APP_NAME,
									"Checked area too small (delayed destroy) ["
											+ totalArea + "/" + minArea + " ("
											+ index + ")]");
						}
						break;
					}
				}
			}
		}
	}

	short checkBodyPolygonsAreaAndAdd(Array<Array<Vector2>> listPolygons) {
		float totalArea = 0;
		for (int j = 0; j < listPolygons.size; j++) {
			Array<Vector2> poly = listPolygons.get(j);

			float area = BayazitDecomposer.GetSignedArea(poly);
			area = Math.abs(area);
			totalArea += area;
			if (area <= 1.0f) {
				Gdx.app.log(APP_NAME,
						"Polygon area too small (removing body polygon) ["
								+ area + "]");
				listPolygons.removeIndex(j);

				if (listPolygons.size == 0) {
					sprite_OutlineVertices
							.removeIndex(sprite_OutlineVertices.size - 1);
					sprite_TexturesMeshes
							.removeIndex(sprite_TexturesMeshes.size - 1);
					sprite_Textures.removeIndex(sprite_Textures.size - 1);
					return -1;
				}

				continue;
			}
		}
		sprite_BodyPolygons.add(listPolygons);

		if (totalArea <= minArea) {
			Gdx.app.log(APP_NAME,
					"Total area too small (registering for delayed destroy) ["
							+ totalArea + "/" + minArea + "]");

			return 0;
		}
		return 1;
	}

	final float MIN_AREA_STAGE1 = 1000.0f;
	final float MIN_AREA_STAGE2 = 2000.0f;
	final float MIN_AREA_STAGE3 = 3000.0f;
	final float MIN_AREA_STAGE4 = 4000.0f;
	float minArea = MIN_AREA_STAGE1;

	public void breakBody(Body bodyToBreak) {

		int bodyIndex = ((BodyUserData) bodyToBreak.getUserData()).textureIndex;

		ArrayList<Fixture> fixtures = bodyToBreak.getFixtureList();
		for (Fixture fixture : fixtures) {

			// boolean fixtureHasBeenCut = false;
			// for (int i = 0; i < cut_fixtures.size; i++) {
			// Fixture f = cut_fixtures.get(i);
			// Body body = fixture.getBody();
			//
			// if (f == fixture && body == cutBody) {
			// fixtureHasBeenCut = true;
			// break;
			// }
			// }
			//
			// if (fixtureHasBeenCut)
			// continue;

			PolygonShape shape = (PolygonShape) fixture.getShape();
			Array<Vector2> vertices = new Array<Vector2>(shape.getVertexCount());
			for (int j = 0; j < shape.getVertexCount(); j++) {
				Vector2 vertex = new Vector2(0, 0);
				shape.getVertex(j, vertex);
				vertices.add(vertex);
			}
			vertices.reverse(); // it is now an outline ! (must be
								// CounterClockWise, unlike body polygons which
								// are ClockWise)

			Body body = generate_spriteBody_subBody(vertices, null,
					bodyToBreak, DEFAULT_restitution, DEFAULT_angularDamping,
					DEFAULT_linearDamping, DEFAULT_friction, DEFAULT_density);
			if (body != null) {
				body.setAngularVelocity(20);
			}
		}

		soundShatter1.play();

		Gdx.app.log(APP_NAME, "Deactivating sprite body (break): " + bodyIndex);
		killSprite(bodyToBreak);
	}

	void destroyBody(Body body) {
		ArrayList<JointEdge> joints = body.getJointList();
		while (joints.iterator().hasNext()) {
			world.destroyJoint(joints.iterator().next().joint);
		}
		// the above code is not strictly necessary anymore with the latest
		// version of LibGDX

		world.destroyBody(body);
	}

	void killSprite(Body body) {

		body.setAwake(false);
		body.setActive(false);

		bodiesToDestroy.removeValue(body, true);

		int pointer = 0;
		for (MouseJoint mouseJoint : mouseJoints) {
			if (mouseJoint != null && mouseJoint.getBodyB() == body) {

				world.destroyJoint(mouseJoint);
				mouseJoints.set(pointer, null);
			}
			pointer++;
		}

		int bodyIndex = ((BodyUserData) body.getUserData()).textureIndex;

		destroyBody(body);

		if (bodyIndex >= sprite_ImagePaths.size) {
			SubTexture tex_ = sprite_Textures.removeIndex(bodyIndex);
			if (tex_ != null) {
				// NO !! (textures are shared, even for broken bodies)
				// tex.dispose();
			}
			Mesh mesh = sprite_TexturesMeshes.removeIndex(bodyIndex);
			if (mesh != null) {
				mesh.dispose();
			}
			sprite_OutlineVertices.removeIndex(bodyIndex);
			sprite_BodyPolygons.removeIndex(bodyIndex);

			for (Iterator<Body> iter = world.getBodies(); iter.hasNext();) {
				Body b = iter.next();
				if (b == groundBody)
					continue;
				BodyUserData userData = (BodyUserData) b.getUserData();
				int index = userData.textureIndex;
				if (index > bodyIndex) {
					b.setUserData(new BodyUserData(index - 1, userData.zCoord));
				}
			}
		}
	}

	Array<Body> cut_bodiesHitByStartEndPoints;
	Array<Body> cut_bodiesHitByRay;
	Array<Fixture> cut_fixtures;
	Array<Vector2> cut_entryPoints;
	IntMap<Vector2> cut_exitPoints;
	boolean multiSegmentsReturn = false;

	public boolean cutWorld(Vector2 start, Vector2 end,
			final boolean allowMultiSegments) {

		if (cut_bodiesHitByRay == null)
			cut_bodiesHitByRay = new Array<Body>();
		else
			cut_bodiesHitByRay.clear();
		if (cut_fixtures == null)
			cut_fixtures = new Array<Fixture>();
		else
			cut_fixtures.clear();
		if (cut_entryPoints == null)
			cut_entryPoints = new Array<Vector2>();
		else
			cut_entryPoints.clear();

		if (cut_exitPoints == null)
			cut_exitPoints = new IntMap<Vector2>();
		else
			cut_exitPoints.clear();

		if (cut_bodiesHitByStartEndPoints == null)
			cut_bodiesHitByStartEndPoints = new Array<Body>();
		else
			cut_bodiesHitByStartEndPoints.clear();

		boolean atLeastOneCreated = false;
		soundWhip1.play();

		for (int i = 1; i <= 2; i++) {
			if (i == 1) {
				temporaryPointForProjection.set(start.x, start.y, zCoord_box2D);
			} else {
				temporaryPointForProjection.set(end.x, end.y, zCoord_box2D);
			}

			world.QueryAABB(
					new QueryCallback() {
						@Override
						public boolean reportFixture(Fixture fixture) {
							if (fixture.getBody() == groundBody)
								return true;

							float z = ((BodyUserData) fixture.getBody()
									.getUserData()).zCoord;
							if (z != zCoord_box2D)
								return true;

							if (fixture.testPoint(
									temporaryPointForProjection.x,
									temporaryPointForProjection.y)) {
								if (!cut_bodiesHitByStartEndPoints.contains(
										fixture.getBody(), true)) {
									cut_bodiesHitByStartEndPoints.add(fixture
											.getBody());

									int breakpoint = 1;
								}
								return true;
							} else
								return true;
						}
					}, temporaryPointForProjection.x - 0.1f,
					temporaryPointForProjection.y - 0.1f,
					temporaryPointForProjection.x + 0.1f,
					temporaryPointForProjection.y + 0.1f);
		}

		world.rayCast(new RayCastCallback() {
			@Override
			public float reportRayFixture(Fixture fixture, Vector2 point,
					Vector2 normal, float fraction) {
				if (fixture.getBody() == groundBody)
					return 1;

				float z = ((BodyUserData) fixture.getBody().getUserData()).zCoord;
				if (z != zCoord_box2D)
					return 1;

				if (!fixture.getBody().isActive()
				// || !fixture.getBody().isAwake()
				)
					return 1;
				if (!allowMultiSegments
						&& cut_bodiesHitByStartEndPoints.contains(
								fixture.getBody(), true))
					return 1;

				if (!cut_bodiesHitByRay.contains(fixture.getBody(), true)) {
					cut_bodiesHitByRay.add(fixture.getBody());

					int breakpoint = 1;
				}
				cut_fixtures.add(fixture);
				cut_entryPoints.add(point.cpy());
				return 1;
			}
		}, start, end);

		multiSegmentsReturn = false;

		world.rayCast(new RayCastCallback() {
			@Override
			public float reportRayFixture(Fixture fixture, Vector2 point,
					Vector2 normal, float fraction) {
				if (fixture.getBody() == groundBody)
					return 1;

				float z = ((BodyUserData) fixture.getBody().getUserData()).zCoord;
				if (z != zCoord_box2D)
					return 1;

				if (!fixture.getBody().isActive()
				// || !fixture.getBody().isAwake()
				)
					return 1;
				if (!allowMultiSegments
						&& cut_bodiesHitByStartEndPoints.contains(
								fixture.getBody(), true))
					return 1;
				if (!allowMultiSegments
						&& !cut_bodiesHitByRay
								.contains(fixture.getBody(), true)) {

					return 0; // PROBLEM !!
				}
				int index = cut_fixtures.indexOf(fixture, true);
				if (index == -1) {
					if (!allowMultiSegments)
						return 0; // PROBLEM !!
					else {
						cut_fixtures.add(fixture);
						index = cut_fixtures.indexOf(fixture, true);
						multiSegmentsReturn = true;
					}
				}
				if (cut_exitPoints.get(index + 1) != null) {
					return 0; // PROBLEM !!
				}
				cut_exitPoints.put(index + 1, point.cpy());

				// cut_exitPoints.insert(0, point.cpy());
				// int index = cut_fixtures.size - cut_exitPoints.size;
				// if (index < 0
				// || index >= cut_fixtures.size
				// || cut_fixtures.get(index) != fixture) {
				// return 0; // PROBLEM !!
				// }
				return 1;
			}
		}, end, start);

		if (multiSegmentsReturn)
			return false;

		if (cut_entryPoints.size != cut_exitPoints.size)
			return false; // PROBLEM !!

		if (cut_fixtures.size != cut_exitPoints.size)
			return false; // PROBLEM !!

		for (int i = 0; i < cut_bodiesHitByRay.size; i++) {
			Body body1 = cut_bodiesHitByRay.get(i);

			for (int j = 0; j < cut_bodiesHitByStartEndPoints.size; j++) {
				Body body2 = cut_bodiesHitByStartEndPoints.get(j);

				if (body1 == body2) {
					return false; // allowMultiSegments
				}
			}
		}

		Vector2 cut_entryPoint_InBodyNotRotated = new Vector2();
		Vector2 cut_exitPoint_InBodyNotRotated = new Vector2();

		for (int w = 0; w < cut_bodiesHitByRay.size; w++) {
			Body cutBody = cut_bodiesHitByRay.get(w);
			// if (cutBody.getType() != BodyType.StaticBody) continue;
			int bodyIndex = ((BodyUserData) cutBody.getUserData()).textureIndex;
			float angleRadians = cutBody.getAngle();
			float angleDegrees = (float) Math.toDegrees(angleRadians);
			//
			Vector2 start_InBodyNotRotated = cutBody.getLocalPoint(start).cpy();
			Vector2 end_InBodyNotRotated = cutBody.getLocalPoint(end).cpy();
			//
			Array<Fixture> fixturesOnSideA = new Array<Fixture>();
			Array<Fixture> fixturesOnSideB = new Array<Fixture>();

			ArrayList<Fixture> fixtures = cutBody.getFixtureList();
			for (Fixture fixture : fixtures) {

				boolean fixtureHasBeenCut = false;
				for (int i = 0; i < cut_fixtures.size; i++) {
					Fixture f = cut_fixtures.get(i);
					// Body body = fixture.getBody();
					if (f == fixture) { // && body == cutBody
						fixtureHasBeenCut = true;
						break;
					}
				}

				if (fixtureHasBeenCut)
					continue;

				PolygonShape shape = (PolygonShape) fixture.getShape();
				Vector2 vertex = new Vector2(0, 0);
				for (int j = 0; j < shape.getVertexCount(); j++) {
					shape.getVertex(j, vertex);
					// vertex.rotate_(angleDegrees);
					// (Bx - Ax) * (Cy - Ay) - (By - Ay) * (Cx - Ax)
					float cross = ((end_InBodyNotRotated.x - start_InBodyNotRotated.x)
							* (vertex.y - start_InBodyNotRotated.y) - (end_InBodyNotRotated.y - start_InBodyNotRotated.y)
							* (vertex.x - start_InBodyNotRotated.x));
					if (cross == 0)
						continue;
					if (cross < 0) {
						fixturesOnSideA.add(fixture);
						break;
					}
					// cross > 0
					fixturesOnSideB.add(fixture);
					break;
				}
			}

			Array<Array<Vector2>> polygonsOnSideA = new Array<Array<Vector2>>();
			Array<Array<Vector2>> polygonsOnSideB = new Array<Array<Vector2>>();

			for (int i = 0; i < cut_fixtures.size; i++) {
				Fixture fixture = cut_fixtures.get(i);
				// if (fixture.getShape().getType() != Shape.Type.Polygon)
				// continue;
				Body body = fixture.getBody();
				// if (body == groundBody)
				// continue;

				if (body != cutBody)
					continue;

				if (!fixture.testPoint(cut_entryPoints.get(i).x,
						cut_entryPoints.get(i).y)) {
					// continue; // PROBLEM !!
				}
				if (!fixture.testPoint(cut_exitPoints.get(i + 1).x,
						cut_exitPoints.get(i + 1).y)) {
					// continue; // PROBLEM !!
				}

				cut_entryPoint_InBodyNotRotated.set(cutBody
						.getLocalPoint(cut_entryPoints.get(i)));
				cut_exitPoint_InBodyNotRotated.set(cutBody
						.getLocalPoint(cut_exitPoints.get(i + 1)));

				PolygonShape shape = (PolygonShape) fixture.getShape();

				Array<Vector2> vertices = new Array<Vector2>(
						shape.getVertexCount());

				for (int j = 0; j < shape.getVertexCount(); j++) {
					Vector2 vertex = new Vector2(0, 0);
					shape.getVertex(j, vertex);

					// int x_origin = 0;
					// int y_origin = 0;
					// vertex.x = x_origin
					// + ((vertex.x - x_origin) * (float) Math.cos(angle))
					// - ((vertex.y - y_origin) * (float) Math.sin(angle));
					// vertex.y = y_origin
					// + ((vertex.y - y_origin) * (float) Math.cos(angle))
					// - ((vertex.x - x_origin) * (float) Math.sin(angle));

					// vertex.rotate_(angleDegrees);
					vertices.add(vertex);
				}

				Array<Vector2> first = new Array<Vector2>();
				Array<Vector2> second = new Array<Vector2>();

				BoxCutter.SplitShape(vertices, cut_entryPoint_InBodyNotRotated,
						cut_exitPoint_InBodyNotRotated, 0, first, second);

				for (int n = 1; n <= 2; n++) {
					Array<Vector2> outline = n == 1 ? first : second;

					// outline.reverse();

					// float area = BayazitDecomposer.GetSignedArea(outline);
					// if (Math.abs(area) <= 1.2f) {
					// Gdx.app.log(APP_NAME, "Signed area too small [" + area
					// + "]");
					// continue;
					// }

					Array<Array<Vector2>> newPolygons = null;

					// gdx/jni/Box2D/Collision/Shapes/b2PolygonShape.cpp, line
					// 134
					// Assertion failed: (2 <= count && count <= 8)
					if (outline.size > BayazitDecomposer.MaxPolygonVertices) {
						// Gdx.app.log(APP_NAME, "Polygon too many vertices [" +
						// outline.size + "]");

						// float ratio = SPRITES_SCALE_FACTOR
						// / SCREEN_TO_WORLD_RATIO;
						// Texture texture = sprite_Textures.get(bodyIndex);
						//
						// for (int v = 0; v < outline.size; v++) {
						// Vector2 vector = outline.get(v);
						//
						// vector.x += (texture.getWidth() / 2) * ratio;
						// vector.y = (texture.getHeight() / 2) * ratio
						// - vector.y;
						// vector.x /= ratio;
						// vector.y /= ratio;
						// }
						//
						// outline.reverse();

						newPolygons = create_sprite_POLYGONS(outline);
						//
						// for (int o = 0; o < newPolygons.size; o++) {
						// Array<Vector2> poly = newPolygons.get(o);
						//
						// for (int j = 0; j < poly.size; j++) {
						// Vector2 vertex = poly.get(j);
						//
						// vertex.x *= ratio;
						// vertex.y *= ratio;
						// vertex.x -= (texture.getWidth() / 2);
						// vertex.y = (texture.getHeight() / 2) - vertex.y;
						// }
						//
						// // poly.reverse();//xxx
						// }
						//
						// // newPolygons.reverse();//xxx
					}

					if (newPolygons != null) {
						Gdx.app.log(APP_NAME,
								"create_sprite_POLYGONS skipped ! [" + outline
										+ "]");
						if (false)
							// TODO: fix Assertion failed: (area >
							// 1.19209290e-7F), function ComputeCentroid !!!
							for (int o = 0; o < newPolygons.size; o++) {
								Array<Vector2> poly = newPolygons.get(o);
								// poly.reverse(); //zzz

								for (int j = 0; j < poly.size; j++) {
									Vector2 vertex = poly.get(j);
									// (Bx - Ax) * (Cy - Ay) - (By - Ay) * (Cx -
									// Ax)
									float cross = ((end_InBodyNotRotated.x - start_InBodyNotRotated.x)
											* (vertex.y - start_InBodyNotRotated.y) - (end_InBodyNotRotated.y - start_InBodyNotRotated.y)
											* (vertex.x - start_InBodyNotRotated.x));
									// vertex.rotate_(-angleDegrees);
									if (cross == 0)
										continue;
									if (cross < 0) {
										polygonsOnSideA.add(poly);
										break;
									}
									// cross > 0
									polygonsOnSideB.add(poly);
									break;
								}
							}
					} else
						for (int j = 0; j < outline.size; j++) {
							Vector2 vertex = outline.get(j);
							// (Bx - Ax) * (Cy - Ay) - (By - Ay) * (Cx - Ax)
							float cross = ((end_InBodyNotRotated.x - start_InBodyNotRotated.x)
									* (vertex.y - start_InBodyNotRotated.y) - (end_InBodyNotRotated.y - start_InBodyNotRotated.y)
									* (vertex.x - start_InBodyNotRotated.x));
							// vertex.rotate_(-angleDegrees);
							if (cross == 0)
								continue;
							if (cross < 0) {
								polygonsOnSideA.add(outline);
								break;
							}
							// cross > 0
							polygonsOnSideB.add(outline);
							break;
						}
				}
			}

			for (int n = 0; n < fixturesOnSideA.size; n++) {
				Fixture fixture = fixturesOnSideA.get(n);
				PolygonShape shape = (PolygonShape) fixture.getShape();
				Array<Vector2> poly = new Array<Vector2>(shape.getVertexCount());
				for (int j = 0; j < shape.getVertexCount(); j++) {
					Vector2 vertex = new Vector2(0, 0);
					shape.getVertex(j, vertex);
					poly.add(vertex);
				}
				polygonsOnSideA.add(poly);
			}

			for (int n = 0; n < fixturesOnSideB.size; n++) {
				Fixture fixture = fixturesOnSideB.get(n);
				PolygonShape shape = (PolygonShape) fixture.getShape();
				Array<Vector2> poly = new Array<Vector2>(shape.getVertexCount());
				for (int j = 0; j < shape.getVertexCount(); j++) {
					Vector2 vertex = new Vector2(0, 0);
					shape.getVertex(j, vertex);
					poly.add(vertex);
				}
				polygonsOnSideB.add(poly);
			}

			if (polygonsOnSideA.size > 0) {
				Array<Vector2> polygonsOnSideA_PointsCloud = new Array<Vector2>(
						polygonsOnSideA.size * 3); // init capacity with
													// assumption of 3 vertices
													// per polygon
				for (int j = 0; j < polygonsOnSideA.size; j++) {
					Array<Vector2> polygon = polygonsOnSideA.get(j);

					for (int u = 0; u < polygon.size; u++) {
						Vector2 point = polygon.get(u);

						boolean pointAlreadyInCloud = false;
						for (int k = 0; k < polygonsOnSideA_PointsCloud.size; k++) {
							Vector2 pointInCloud = polygonsOnSideA_PointsCloud
									.get(k);
							if (pointInCloud.x == point.x
									&& pointInCloud.y == point.y) {
								pointAlreadyInCloud = true;
								break;
							}
						}
						if (!pointAlreadyInCloud)
							polygonsOnSideA_PointsCloud.add(point);
					}
				}
				Array<Vector2> outlineA = BoundingPolygon
						.createGiftWrapConvexHull(polygonsOnSideA_PointsCloud);

				Body bodyA = generate_spriteBody_subBody(outlineA,
						polygonsOnSideA, cutBody, DEFAULT_restitution,
						DEFAULT_angularDamping, DEFAULT_linearDamping,
						DEFAULT_friction, DEFAULT_density);
				if (bodyA != null) {
					// body.setAngularVelocity(body.getAngularVelocity());
					bodyA.setAngularVelocity(14);
					bodyA.setLinearVelocity(bodyA.getLinearVelocity());
				}

				atLeastOneCreated = true;
			}

			if (polygonsOnSideB.size > 0) {
				Array<Vector2> polygonsOnSideB_PointsCloud = new Array<Vector2>(
						polygonsOnSideB.size * 3); // init capacity with
													// assumption of 3 vertices
													// per polygon
				for (int j = 0; j < polygonsOnSideB.size; j++) {
					Array<Vector2> polygon = polygonsOnSideB.get(j);

					for (int u = 0; u < polygon.size; u++) {
						Vector2 point = polygon.get(u);

						polygonsOnSideB_PointsCloud.add(point);
					}
				}
				Array<Vector2> outlineB = BoundingPolygon
						.createGiftWrapConvexHull(polygonsOnSideB_PointsCloud);

				Body bodyB = generate_spriteBody_subBody(outlineB,
						polygonsOnSideB, cutBody, DEFAULT_restitution,
						DEFAULT_angularDamping, DEFAULT_linearDamping,
						DEFAULT_friction, DEFAULT_density);
				if (bodyB != null) {
					// body.setAngularVelocity(body.getAngularVelocity());
					bodyB.setAngularVelocity(14);
					bodyB.setLinearVelocity(bodyB.getLinearVelocity());
				}
				atLeastOneCreated = true;
			}

			Gdx.app.log(APP_NAME, "Deactivating sprite body (cut): "
					+ bodyIndex);
			killSprite(cutBody);
		}
		if (atLeastOneCreated) {
			soundSword1.play();
		}

		return atLeastOneCreated;
	}

	Body generate_spriteBody_fromExistingTextureAndPolygons(float x, float y,
			float z, float angle, int index, float restitution,
			float angularDamping, float linearDamping, float friction,
			float density) {
		float ratio = SPRITES_SCALE_FACTOR / PIXEL_TO_WORLD_RATIO;
		SubTexture subtexture = sprite_Textures.get(index);
		Vector2 bodySize = new Vector2(ratio * subtexture.width, ratio
				* subtexture.height);
		if (false) {
			PolygonShape polygonShape = new PolygonShape();
			polygonShape.setAsBox(bodySize.x / 2, bodySize.y / 2);
			Array<PolygonShape> polygonShapes = new Array<PolygonShape>(1);
			polygonShapes.add(polygonShape);
			Body body = generate_spriteBody(x, y, angle, polygonShapes,
					restitution, angularDamping, linearDamping, friction,
					density);
			body.setUserData(new BodyUserData(index, z));
			// spriteBodies.add(body);
			polygonShapes.clear();
			polygonShape.dispose();
			return body;
		} else {
			Array<PolygonShape> polygonShapes;
			if (false) {
				/*
				 * Array<Vector2> triangles__ = sprite_BodyTriangles.get(index);
				 * Array<Vector2> triangles = new Array<Vector2>(
				 * triangles__.size); for (int j = 0; j < triangles__.size; j++)
				 * { Vector2 v = triangles__.get(j).cpy(); v.x = v.x / ratioX;
				 * v.y = v.y / ratioY; v.x -= halfEdge; v.y = halfEdge - v.y;
				 * triangles.add(v); } // Vector2[] triangles =
				 * triangles_.toArray(new Vector2[] {}); bodyPolies = new
				 * Array<PolygonShape>(triangles.size / 3); for (int j = 0; j <
				 * triangles.size;) { Vector2 v1 = triangles.get(j++); Vector2
				 * v2 = triangles.get(j++); Vector2 v3 = triangles.get(j++);
				 * PolygonShape bodyPoly = new PolygonShape(); Vector2[] array =
				 * new Vector2[] { v1, v2, v3 }; if
				 * (LibGDXPlayground.areVerticesClockwise(array)) {
				 * Gdx.app.log(APP_NAME, "Clockwise triangle vertices ? " + j /
				 * 3 + "/" + triangles.size / 3); array = new Vector2[] { v3,
				 * v2, v1 }; if (LibGDXPlayground.areVerticesClockwise(array)) {
				 * Gdx.app.log(APP_NAME, "Clockwise triangle vertices ?! " + j /
				 * 3 + "/" + triangles.size / 3); } } // EarClippingTriangulator
				 * bodyPoly.set(array); bodyPolies.add(bodyPoly); }
				 */
			} else {
				Array<Array<Vector2>> polygons = sprite_BodyPolygons.get(index);
				polygonShapes = new Array<PolygonShape>(polygons.size);
				// Array<Array<Vector2>> polygons_adjusted = new
				// Array<Array<Vector2>>( polygons_original.size);
				for (int j = 0; j < polygons.size; j++) {
					Array<Vector2> vertices_original = polygons.get(j);
					// ClockWise requirement should have been dealt with at
					// create_sprite() stage !
					if (BayazitDecomposer.IsCounterClockWise(vertices_original)) {
						Gdx.app.log(APP_NAME,
								"Counter Clockwise body polygon vertices (ORIGINAL) for Box2D ?! WTF ? ["
										+ index + "] " + j + "/"
										+ polygons.size);
					}
					// Vector2[] vertices_adjusted =
					// vertices_original.toArray(new
					// Vector2[vertices_original.size]);
					Vector2[] vertices_adjusted = new Vector2[vertices_original.size];
					// Array<Vector2> vertices_adjusted = new
					// Array<Vector2>(vertices_original.size);
					for (int t = 0; t < vertices_original.size; t++) {
						Vector2 v = vertices_original.get(t).cpy();
						v.x *= ratio;
						v.y *= ratio;
						v.x -= (bodySize.x / 2); // x relative to center instead
													// of left corner
						v.y = (bodySize.y / 2) - v.y; // flipped vertical axis,
														// and relative to
														// center instead of
														// upper? corner,
														// which results in
														// Counter ClockWise
														// (signed area
														// positive),
														// which is fine in
														// Box2D's
														// world coordinate
														// system
														// (otherwise the
														// ComputeCentroid
														// fails).
						vertices_adjusted[t] = v;
					}
					if (!BayazitDecomposer
							.IsCounterClockWise(vertices_adjusted)) {
						Gdx.app.log(APP_NAME,
								"Clockwise body polygon vertices (ADJUSTED) for Box2D ?! WTF ? ["
										+ index + "] " + j + "/"
										+ polygons.size);
					}
					if (LibGDXPlayground
							.areVerticesClockwise(vertices_adjusted)) {
						Gdx.app.log(APP_NAME,
								"_ Clockwise body polygon vertices (ADJUSTED) for Box2D ?! WTF ? ["
										+ index + "] " + j + "/"
										+ polygons.size);
					}
					PolygonShape polygonShape = new PolygonShape();
					// polygonShape.set(vertices_adjusted.toArray(new
					// Vector2[vertices_adjusted.size]));
					polygonShape.set(vertices_adjusted);
					polygonShapes.add(polygonShape);
				}
			}
			Body body = generate_spriteBody(x, y, angle, polygonShapes,
					restitution, angularDamping, linearDamping, friction,
					density);

			body.setUserData(new BodyUserData(index, z));
			// spriteBodies.add(body);
			for (int j = 0; j < polygonShapes.size; j++) {
				polygonShapes.get(j).dispose();
			}
			polygonShapes.clear();

			return body;
		}
	}

	Vector2 placeRandom(int index) {
		float ratio = SPRITES_SCALE_FACTOR / PIXEL_TO_WORLD_RATIO;
		SubTexture subtexture = sprite_Textures.get(index);
		Vector2 bodySize = new Vector2(ratio * subtexture.width, ratio
				* subtexture.height);
		float randomX = -(worldSize.x / 2)
				+ (float) (Math.random() * worldSize.x);
		float randomY = (worldSize.y / 2)
				+ (worldSize.y / GROUND_SIZE_FRACTION.y / 2) + bodySize.y
				+ (float) (Math.random() * (worldSize.y / 2));
		bodySize.x = randomX;
		bodySize.y = randomY;
		return bodySize;
		// return new Vector2(randomX, randomY);
	}

	void initWorld_sprites() {

		final float ratio = SPRITES_SCALE_FACTOR / PIXEL_TO_WORLD_RATIO;

		final SubTexture brickSubTexture = sprite_Textures.get(0);
		final float width = ratio * brickSubTexture.width;
		final float height = ratio * brickSubTexture.width;

		final float xOffset = 0.5f;
		final float yOffset = 1.0f;

		final int nRows = 2;
		final int nColumns = (int) ((getWorldMaxX() - getWorldMinX()) / (width + xOffset));

		RevoluteJointDef jd = new RevoluteJointDef();

		Body prevBody = null;

		boolean offset = false;
		for (int i = 0; i < nRows; i++) {
			for (int j = 0; j < nColumns; j++) {

				final float x = j * width + j * xOffset;
				final float y = i * height + i * yOffset;

				final int brickIndex = (int) (3 * Math.random());
				assert (brickIndex == 0 && brickIndex == 1 && brickIndex == 2);

				final boolean brickFlip = ((int) (2 * Math.random())) == 0 ? false
						: true;

				float XX = -(worldSize.x / 2) + width / 2 + x
						+ (offset ? width / 2 : 0);

				float YY = (worldSize.y / GROUND_SIZE_FRACTION.y / 2) + height
						/ 2 + y;

				final Body body = generate_spriteBody_fromExistingTextureAndPolygons(
						XX, YY, zCoord_box2D_,
						brickFlip ? (float) Math.toRadians(180) : 0,
						brickIndex, //
						0, // DEFAULT_restitution, (NO BOUNCE)
						(float) (Math.PI * 2), // DEFAULT_angularDamping,
						0.2f, // DEFAULT_linearDamping
						1f, // DEFAULT_friction
						60f // DEFAULT_density
				);
				// body.setBullet(true);
				// body.setAwake(false);
				//
				// Filter filter = new Filter();
				// filter.categoryBits = 0x0000;
				// filter.maskBits = 0x0000;
				// ArrayList<Fixture> fixtures = body.getFixtureList();
				// for (Fixture fixture : fixtures) {
				// fixture.setFilterData(filter);
				// //
				// // assert (fixture.getFilterData().categoryBits == 0x0000);
				// // assert (fixture.getFilterData().maskBits == 0x0000);
				// }

				if (prevBody != null && i == 0) {
					jd.initialize(prevBody, body, new Vector2(XX, YY));
					// jd.bodyA = prevBody;
					// jd.bodyB = body;
					// jd.localAnchorA.set(new Vector2(XX - width, YY));
					// jd.localAnchorB.set(new Vector2(XX, YY));
					jd.collideConnected = true;
					jd.enableLimit = false;
					jd.lowerAngle = 0;
					jd.upperAngle = (float) (2 * Math.PI);
					jd.referenceAngle = 0;
					jd.motorSpeed = 5.0f;
					jd.maxMotorTorque = 10.0f;
					jd.enableMotor = false;
					world.createJoint(jd);
				}
				prevBody = body;
			}
			offset = !offset;
		}
		//
		// float halfWidth = (worldSize.x / GROUND_SIZE_FRACTION.x) / 2;
		// float halfHeight = (worldSize.y / GROUND_SIZE_FRACTION.y) / 2;

		float xbase = -worldSize.x / 2 + width * 2;
		float ybase = worldSize.y / 1.5f + height;

		Body body = generate_spriteBody_fromExistingTextureAndPolygons(xbase,
				ybase, zCoord_box2D_, 0, 0, DEFAULT_restitution,
				DEFAULT_angularDamping, DEFAULT_linearDamping,
				DEFAULT_friction, DEFAULT_density);

		PrismaticJointDef pjd = new PrismaticJointDef();
		pjd.initialize(groundBody, body, new Vector2(xbase, 0), new Vector2(1,
				0).nor());
		// pjd.bodyA = groundBody;
		// pjd.bodyB = body;
		// pjd.localAnchorA.set(new Vector2(xbase, 0));
		// pjd.localAnchorB.set(new Vector2(xbase, ybase));
		// pjd.localAxis1.set(new Vector2(1, 0).nor());
		pjd.collideConnected = true;
		pjd.referenceAngle = 0;
		pjd.motorSpeed = 5.0f;
		pjd.maxMotorForce = 200.0f * body.getMass(); // 10000.0f;
		pjd.enableMotor = true;
		pjd.enableLimit = true;
		pjd.lowerTranslation = 0;
		pjd.upperTranslation = worldSize.x - width * 4;

		PrismaticJoint joint = (PrismaticJoint) world.createJoint(pjd);
		//
		Body body1 = generate_spriteBody_fromExistingTextureAndPolygons(xbase
				- width / 2, ybase + height, zCoord_box2D_, 0, 1,
				DEFAULT_restitution, DEFAULT_angularDamping,
				DEFAULT_linearDamping, DEFAULT_friction, DEFAULT_density);
		Body body2 = generate_spriteBody_fromExistingTextureAndPolygons(xbase
				+ width / 2, ybase + height, zCoord_box2D_, 0, 2,
				DEFAULT_restitution, DEFAULT_angularDamping,
				DEFAULT_linearDamping, DEFAULT_friction, DEFAULT_density);
		WeldJointDef wjd = new WeldJointDef();
		wjd.initialize(body1, body2, new Vector2(xbase, ybase));
		// wjd.bodyA = body1;
		// wjd.bodyB = body2;
		// wjd.localAnchorA.set(new Vector2(xbase - width / 2, ybase));
		// wjd.localAnchorB.set(new Vector2(xbase + width / 2, ybase));
		wjd.collideConnected = true;
		wjd.referenceAngle = 0;
		world.createJoint(wjd);
		//
		Body body3 = generate_spriteBody_fromExistingTextureAndPolygons(xbase
				- width / 2, ybase - 3 * height, zCoord_box2D_, 0, 1,
				DEFAULT_restitution, DEFAULT_angularDamping,
				DEFAULT_linearDamping, DEFAULT_friction, DEFAULT_density);
		// Body body4 = generate_spriteBody_fromExistingTextureAndPolygons(xbase
		// + width / 2, ybase - 2 * height, zCoord_box2D_, 0, 2,
		// DEFAULT_restitution, DEFAULT_angularDamping,
		// DEFAULT_linearDamping, DEFAULT_friction, DEFAULT_density);
		jd.initialize(body, body3, new Vector2(xbase - width / 2, ybase
				- height));
		// jd.bodyA = prevBody;
		// jd.bodyB = body;
		// jd.localAnchorA.set(new Vector2(XX - width, YY));
		// jd.localAnchorB.set(new Vector2(XX, YY));
		jd.collideConnected = false;
		jd.enableLimit = false;
		jd.lowerAngle = 0;
		jd.upperAngle = (float) (2 * Math.PI);
		jd.referenceAngle = 0;
		jd.motorSpeed = 3.5f;
		jd.maxMotorTorque = 100.0f * body.getMass(); // 60000.0f;
		jd.enableMotor = true;
		world.createJoint(jd);
		//
		// DistanceJoint
		//
		// float gravity = 10.0f;
		// float I = body.getInertia();
		// float mass = body.getMass();
		//
		// float radius = (float)Math.sqrt(2 * I / mass);
		//
		// FrictionJointDef jd = new FrictionJointDef();
		// jd.localAnchorA.set(0, 0);
		// jd.localAnchorB.set(0, 0);
		// jd.bodyA = ground;
		// jd.bodyB = body;
		// jd.collideConnected = true;
		// jd.maxForce = mass * gravity;
		// jd.maxTorque = mass * radius * gravity;
		//
		// world.createJoint(jd);
		//

		int j = sprite_Textures.size - 1;
		final Vector2 placeRandom_ = placeRandom(j);
		final float angle_ = (float) Math.toRadians(Math.random() * 360);
		generate_spriteBody_fromExistingTextureAndPolygons(placeRandom_.x,
				placeRandom_.y, zCoord_box2D, angle_, j, DEFAULT_restitution,
				DEFAULT_angularDamping, DEFAULT_linearDamping,
				DEFAULT_friction, DEFAULT_density);

		int n = 4;
		if (renderDebug == DebugRender.None) {
			n = 7; // sprite_ImagePaths.size;
		}
		for (int i = 3; i < n; i++) {
			// final int index = computeModuloSpriteRotationThing(i);
			final Vector2 placeRandom = placeRandom(i);
			final float angle = (float) Math.toRadians(Math.random() * 360);
			generate_spriteBody_fromExistingTextureAndPolygons(placeRandom.x,
					placeRandom.y, zCoord_box2D, angle, i, DEFAULT_restitution,
					DEFAULT_angularDamping, DEFAULT_linearDamping,
					DEFAULT_friction, DEFAULT_density);
			nextSpriteIndex = n;
		}
	}

	final float DEFAULT_restitution = 0.0f;
	final float DEFAULT_angularDamping = (float) (Math.PI / 2);
	final float DEFAULT_linearDamping = 0.3f;
	final float DEFAULT_friction = 0.4f;
	final float DEFAULT_density = 10f;

	public Body generate_spriteBody(float x, float y, float angle,
			Array<PolygonShape> bodyPolys, float restitution,
			float angularDamping, float linearDamping, float friction,
			float density) {
		BodyDef bodyDef = new BodyDef();
		bodyDef.type = BodyType.DynamicBody;
		bodyDef.position.x = x;
		bodyDef.position.y = y;
		bodyDef.angle = angle;
		bodyDef.allowSleep = true;
		bodyDef.awake = true;
		bodyDef.active = true;
		bodyDef.fixedRotation = false;
		bodyDef.angularDamping = angularDamping;
		bodyDef.linearDamping = linearDamping;
		// BodyDef.angularVelocity = 0;
		// BodyDef.linearVelocity.set(0, 0);
		bodyDef.active = true;
		bodyDef.bullet = false;
		// BodyDef.inertiaScale

		Body body = world.createBody(bodyDef);

		for (int j = 0; j < bodyPolys.size; j++) {
			PolygonShape bodyPoly = bodyPolys.get(j);

			FixtureDef fixtureDef = new FixtureDef();
			fixtureDef.shape = bodyPoly;
			fixtureDef.filter.groupIndex = 0; // 0 => no collision
												// group,negative =>
			// never collide, positive => always
			// collide
			fixtureDef.filter.categoryBits = 0x0000;
			fixtureDef.filter.maskBits = 0x0000;
			fixtureDef.isSensor = false;
			if (preferences.getBoolean(PREF_USE_BOUNCING_OBJECTS))
				fixtureDef.restitution = restitution;
			else
				fixtureDef.restitution = 0.0f;
			fixtureDef.density = density;
			fixtureDef.friction = friction;

			body.createFixture(fixtureDef);
			// Body.createFixture(bodyPoly, 1);
		}
		return body;
	}

	final StringBuilder stringBuilder = new StringBuilder();
	final String STR_BODIES = "\nbodies: ";
	final String STR_AZIMUTH = "\nazimuth: ";
	final String STR_PITCH = "\npitch: ";
	final String STR_ROLL = "\nroll: ";
	final String STR_ACCELEROMETER_X = "\naccel X: ";
	final String STR_ACCELEROMETER_Y = "\naccel Y: ";
	final String STR_ACCELEROMETER_Z = "\naccel Z: ";
	final String STR_ORIENTATION = "\norientation: ";
	final String STR_GRAVITY_X = "\ngravity X: ";
	final String STR_GRAVITY_Y = "\ngravity Y: ";

	StringBuilder getDebugDisplayString() {
		stringBuilder.delete(0, stringBuilder.length());
		stringBuilder.append(STR_BODIES);
		// builder.append(spriteBodies != null ? spriteBodies.size : "??");
		stringBuilder.append(world != null ? world.getBodyCount() : "??");
		stringBuilder.append(STR_AZIMUTH);
		stringBuilder.append(Gdx.input.getAzimuth());
		stringBuilder.append(STR_PITCH);
		stringBuilder.append(Gdx.input.getPitch());
		stringBuilder.append(STR_ROLL);
		stringBuilder.append(Gdx.input.getRoll());
		stringBuilder.append(STR_ACCELEROMETER_X);
		stringBuilder.append(Gdx.input.getAccelerometerX());
		stringBuilder.append(STR_ACCELEROMETER_Y);
		stringBuilder.append(Gdx.input.getAccelerometerY());
		stringBuilder.append(STR_ACCELEROMETER_Z);
		stringBuilder.append(Gdx.input.getAccelerometerZ());
		stringBuilder.append(STR_ORIENTATION);
		stringBuilder.append(orientation);
		Vector2 gravity = (world != null ? world.getGravity() : new Vector2(0,
				0));
		stringBuilder.append(STR_GRAVITY_X);
		stringBuilder.append(gravity.x);
		stringBuilder.append(STR_GRAVITY_Y);
		stringBuilder.append(gravity.y);
		return stringBuilder;
	}

	void updateOrientationAndSetGravity() {
		// Roll= Asin(X-axis) in radian * 180/Pi in degree
		// Pitch= Asin (Y/axis) in radian * 180/Pi in degree
		int screenWidth = Gdx.graphics.getWidth();
		int screenHeight = Gdx.graphics.getHeight();
		boolean portrait = screenHeight > screenWidth;
		if (portrait) {
			float accelY = Gdx.input.getAccelerometerY(); // (m/s^2)
			float pitch = Gdx.input.getPitch();
			if (accelY > 0 || pitch < -45.0) {
				orientation = DeviceOrientation.PORTRAIT_NORMAL;
			}
			if (accelY < 0 || pitch > 45.0) {
				orientation = DeviceOrientation.PORTRAIT_UPSIDEDOWN;
			}
			if (accelY == 0 || pitch == 0.0) {
				orientation = DeviceOrientation.PORTRAIT_NORMAL;
			}
		} else {
			float accelX = Gdx.input.getAccelerometerX(); // (m/s^2)
			float roll = Gdx.input.getRoll();
			if (accelX > 0 || roll < -45.0) {
				orientation = DeviceOrientation.LANDSCAPE_NORMAL;
			}
			if (accelX < 0 || roll > 45.0) {
				orientation = DeviceOrientation.LANDSCAPE_UPSIDEDOWN;
			}
			if (accelX == 0 || roll == 0.0) {
				orientation = DeviceOrientation.LANDSCAPE_NORMAL;
			}
		}
		final int FACTOR = (int) (Math.max(worldSize.x, worldSize.y) / 2.5f);
		float x = 0, y = 0;
		switch (orientation) {
		case PORTRAIT_NORMAL: {
			x = (float) Math.sin(Math.toRadians(Gdx.input.getRoll())) * FACTOR;
			y = -(float) Math.cos(Math.toRadians(Gdx.input.getRoll())) * FACTOR;
		}
			break;
		case PORTRAIT_UPSIDEDOWN: {
			x = -(float) Math.sin(Math.toRadians(Gdx.input.getRoll())) * FACTOR;
			y = -(float) Math.cos(Math.toRadians(Gdx.input.getRoll())) * FACTOR;
		}
			break;
		case LANDSCAPE_NORMAL: {
			x = -(float) Math.sin(Math.toRadians(Gdx.input.getPitch()))
					* FACTOR;
			y = -(float) Math.cos(Math.toRadians(Gdx.input.getPitch()))
					* FACTOR;
		}
			break;
		case LANDSCAPE_UPSIDEDOWN: {
			x = (float) Math.sin(Math.toRadians(Gdx.input.getPitch())) * FACTOR;
			y = -(float) Math.cos(Math.toRadians(Gdx.input.getPitch()))
					* FACTOR;
		}
			break;
		}
		Vector2 gravity = world.getGravity();
		if (gravity.x == x && gravity.y == y)
			return;
		// Gdx.app.log(APP_NAME, "Gravity x:" + (int) x + ", y:" + (int) y);
		world.setGravity(new Vector2(x, y));
	}

	// void setSpriteBatchProjectionModelTransformMatrices(boolean ortho)
	// {
	// if (ortho)
	// {
	// spriteBatch.getProjectionMatrix().setToOrtho2D(0, 0,
	// Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
	// }
	// else{
	// spriteBatch.getProjectionMatrix().set(camera.combined);
	// }
	//
	// GL10 gl = Gdx.gl10;
	// gl.glMatrixMode(GL10.GL_PROJECTION);
	// gl.glLoadMatrixf(spriteBatch.getProjectionMatrix().val, 0);
	// gl.glMatrixMode(GL10.GL_MODELVIEW);
	// gl.glLoadMatrixf(spriteBatch.getTransformMatrix().val, 0);
	// }

	void render_SCREEN_spriteBatch_Loading_FontAndTexture(int index, String info) {

		GLCommon gl = null;
		if (Gdx.graphics.isGL20Available())
			gl = Gdx.gl20;
		else
			gl = Gdx.gl10;

		// GL10 gl = Gdx.gl10;
		//
		// gl.glActiveTexture(GL10.GL_TEXTURE0 + 0);
		// gl.glEnable(GL10.GL_TEXTURE_2D);
		//
		// gl.glEnable(GL10.GL_BLEND);
		// gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
		//
		// if (!Gdx.graphics.isGL20Available()) {Gdx.gl10.glColor4f(1.0f, 1.0f,
		// 1.0f, 1.0f);}

		int screenWidth = Gdx.graphics.getWidth();
		int screenHeight = Gdx.graphics.getHeight();
		int centerX = screenWidth / 2;
		int centerY = screenHeight / 2;
		SubTexture subtexture = index == -1 ? null : sprite_Textures.get(index);

		String text = "Loading... (" + info + ")";
		TextBounds textBounds = fonts.get(currentFont).getBounds(text);

		fonts.get(currentFont).setScale(1.0f);
		fonts.get(currentFont).setColor(Color.WHITE);
		fonts.get(currentFont).draw(spriteBatch, text,
				(screenWidth - textBounds.width) / 2, textBounds.height); // font.getLineHeight()
		if (subtexture != null) {
			spriteBatch.setColor(1, 1, 1, 1);
			spriteBatch.draw(subtexture.texture,
					centerX - subtexture.width / 2, centerY - subtexture.height
							/ 2, subtexture.left, subtexture.top,
					subtexture.width, subtexture.height);
		}
	}

	public void simulateLatencyOnDesktop() {
		// if (true) return;

		if (Gdx.app.getType() == ApplicationType.Desktop) {
			try {
				Thread.sleep(30);
			} catch (InterruptedException e) {
			}
		}
	}

	enum BlurType {
		NORMAL, RADIAL, ZOOM

	}

	Texture blurTexture(Texture texture, int passes, BlurType blur,
			int GL_internalformat) {

		ShapeRendererEx shapeRenderer_ = shapeRenderer;
		if (Gdx.graphics.isGL20Available()) {
			shapeRenderer_ = new ShapeRendererEx(500, true);
		}

		int i, x, y;
		int width = texture.getWidth();
		int height = texture.getHeight();
		//
		// boolean useFrameBufferObject = false;
		// if (Gdx.graphics.isGL20Available() && useFrameBufferObject) {
		//
		// // boolean needsRefresh = frameBuffer != null
		// // && (frameBuffer.getColorBufferTexture().getHeight() != height
		// // || frameBuffer.getColorBufferTexture().getWidth() != width ||
		// // !frameBuffer
		// // .getColorBufferTexture().getTextureData()
		// // .getFormat()
		// // .equals(texture.getTextureData().getFormat()));
		// //
		// // if (needsRefresh) {
		// // frameBuffer.dispose();
		// // frameBuffer = null;
		// //
		// // Gdx.app.log(APP_NAME, "Creating GL20 FrameBuffer object: "
		// // + width + "x" + height);
		// // frameBuffer = new FrameBuffer(texture.getTextureData()
		// // .getFormat(), width, height, true);
		// // }
		//
		// frameBuffer.begin();
		// }

		GLCommon gl = null;
		if (Gdx.graphics.isGL20Available())
			gl = Gdx.gl20;
		else
			gl = Gdx.gl10;

		// GL10 gl = Gdx.gl10;

		gl.glClear(GL10.GL_COLOR_BUFFER_BIT);

		gl.glActiveTexture(GL10.GL_TEXTURE0 + 0);
		gl.glEnable(GL10.GL_TEXTURE_2D);

		gl.glEnable(GL10.GL_BLEND);
		gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);

		bind(texture);

		if (!Gdx.graphics.isGL20Available())
			Gdx.gl10.glMatrixMode(GL10.GL_MODELVIEW);

		while (passes > 0) {

			if (false) {
				shapeRenderer_.setColor(1.0f, 0.0f, 0.0f, 0.5f);

				shapeRenderer_.setProjectionMatrix(cameraSCREEN.combined);
				shaderMatrix.idt();
				shapeRenderer_.setTransformMatrix(shaderMatrix);
				shapeRenderer_.begin(GL10.GL_TRIANGLE_STRIP);
				//
				shapeRenderer_.pointTex(0, 0, 0, 0, 0);
				//
				shapeRenderer_.pointTex(0, height, 0, 0, 1);
				//
				shapeRenderer_.pointTex(width, 0, 0, 1, 0);
				//
				shapeRenderer_.pointTex(width, height, 0, 1, 1);
				//
				shapeRenderer_.end();
			} else
				switch (blur) {
				case NORMAL: {

					i = 0;
					for (x = 0; x < 2; x++) {
						for (y = 0; y < 2; y++, i++) {

							shapeRenderer_.setColor(1.0f, 1.0f, 1.0f,
									1.0f / (i + 1));

							shapeRenderer_
									.setProjectionMatrix(cameraSCREEN.combined);
							shaderMatrix.idt();
							shapeRenderer_.setTransformMatrix(shaderMatrix);
							shapeRenderer_.begin(GL10.GL_TRIANGLE_STRIP);
							//
							shapeRenderer_.pointTex(0, 0, 0, 0 + (x - 0.5f)
									/ width, 1 - (1 + (y - 0.5f) / height));
							//
							shapeRenderer_.pointTex(0, height, 0, 0
									+ (x - 0.5f) / width, 1 - (0 + (y - 0.5f)
									/ height));
							//
							shapeRenderer_.pointTex(width, 0, 0, 1 + (x - 0.5f)
									/ width, 1 - (1 + (y - 0.5f) / height));
							//
							shapeRenderer_.pointTex(width, height, 0, 1
									+ (x - 0.5f) / width, 1 - (0 + (y - 0.5f)
									/ height));
							//
							shapeRenderer_.end();
						}
					}
					break;
				}
				case RADIAL: {
					for (i = 0; i < 2; i++) {
						if (Gdx.graphics.isGL20Available()) {
							shaderMatrix.idt();
							shaderMatrix.translate(width / 2, height / 2, 0);
							shaderMatrix.rotate(0, 0, 1, 1);
							shaderMatrix.translate(-width / 2, -height / 2, 0);
							shapeRenderer_.setTransformMatrix(shaderMatrix);
						} else {
							Gdx.gl10.glPushMatrix();
							Gdx.gl10.glLoadIdentity();
							if (i == 1) {
								Gdx.gl10.glTranslatef(width / 2, height / 2, 0);
								Gdx.gl10.glRotatef(1, 0, 0, 1);
								Gdx.gl10.glTranslatef(-width / 2, -height / 2,
										0);
							}
						}

						shapeRenderer_.setColor(1.0f, 1.0f, 1.0f,
								1.0f / (i + 1));

						shapeRenderer_
								.setProjectionMatrix(cameraSCREEN.combined);

						shapeRenderer_.begin(GL10.GL_TRIANGLE_STRIP);
						//
						shapeRenderer_.pointTex(0, 0, 0, 0.0f, 1 - 1.0f);
						//
						shapeRenderer_.pointTex(0, height, 0, 0.0f, 1 - 0.0f);
						//
						shapeRenderer_.pointTex(width, 0, 0, 1.0f, 1 - 1.0f);
						//
						shapeRenderer_.pointTex(width, height, 0, 1.0f,
								1 - 0.0f);
						//
						shapeRenderer_.end();

						if (Gdx.graphics.isGL20Available()) {
						} else {
							Gdx.gl10.glPopMatrix();
						}
					}
					break;
				}
				case ZOOM: {
					for (i = 0; i < 2; i++) {

						shapeRenderer_.setColor(1.0f, 1.0f, 1.0f,
								1.0f / (i + 1));

						shapeRenderer_
								.setProjectionMatrix(cameraSCREEN.combined);
						shaderMatrix.idt();
						shapeRenderer_.setTransformMatrix(shaderMatrix);
						shapeRenderer_.begin(GL10.GL_TRIANGLE_STRIP);
						//
						shapeRenderer_.pointTex(0, 0, 0,
								0 - (i * 0.5f) / width, 1 - (1 + (i * 0.5f)
										/ height));
						//
						shapeRenderer_.pointTex(0, height, 0, 0 - (i * 0.5f)
								/ width, 1 - (0 - (i * 0.5f) / height));
						//
						shapeRenderer_.pointTex(width, 0, 0, 1 + (i * 0.5f)
								/ width, 1 - (1 + (i * 0.5f) / height));
						//
						shapeRenderer_.pointTex(width, height, 0, 1
								+ (i * 0.5f) / width, 1 - (0 - (i * 0.5f)
								/ height));
						//
						shapeRenderer_.end();
					}
					break;
				}
				}
			//
			// if (Gdx.graphics.isGL20Available() && useFrameBufferObject) {
			// // texture.load(frameBuffer.getColorBufferTexture()
			// // .getTextureData());
			// } else {

			// glCopyTexImage2D doesn't work on Android device in landscape
			// mode
			// (only portrait) ! :(
			if (Gdx.app.getType() == ApplicationType.Desktop
					|| Gdx.graphics.getWidth() < Gdx.graphics.getHeight()) {
				// Power Of Two !
				Gdx.app.log(APP_NAME, "glCopyTexImage2D: " + width + "x"
						+ height);
				// GL10.GL_RGB/A
				gl.glCopyTexImage2D(GL10.GL_TEXTURE_2D, 0, GL_internalformat,
						0, 0, width, height, 0);
				// gl.glCopyTexSubImage2D(GL10.GL_TEXTURE_2D, 0, 0, 0, 0, 0,
				// width, height);
			} else {
				Gdx.app.log(APP_NAME, "updateScreenCapture HACK: " + width
						+ "x" + height);
				gl.glFlush();
				gl.glFinish();
				GL_internalformat = createOrUpdateScreenCapture();
			}

			passes--;
		}
		//
		// if (Gdx.graphics.isGL20Available() && useFrameBufferObject) {
		// frameBuffer.end();
		// }

		if (Gdx.graphics.isGL20Available()) {
			shapeRenderer_.dispose();
		}
		//
		// if (Gdx.graphics.isGL20Available() && useFrameBufferObject) {
		//
		// return frameBuffer.getColorBufferTexture();
		// }
		return null;
	}

	final int FINGER_TOUCH_CORNER_SIZE = FINGER_TOUCH_TOLERANCE * 3;

	boolean isBottomRightCornerTouch() {
		if (!Gdx.input.isTouched())
			return false;
		return isBottomRightCornerTouch(Gdx.input.getX(), Gdx.input.getY());
	}

	boolean isBottomRightCornerTouch(int x, int y) {
		int width = Gdx.graphics.getWidth();
		int height = Gdx.graphics.getHeight();

		return x >= (width - FINGER_TOUCH_CORNER_SIZE)
				&& y >= (height - FINGER_TOUCH_CORNER_SIZE);
	}

	boolean isUpperLeftCornerTouch() {
		if (!Gdx.input.isTouched())
			return false;
		return isUpperLeftCornerTouch(Gdx.input.getX(), Gdx.input.getY());
	}

	boolean isUpperLeftCornerTouch(int x, int y) {
		return x <= FINGER_TOUCH_CORNER_SIZE && y <= FINGER_TOUCH_CORNER_SIZE;
	}

	boolean isBottomLeftCornerTouch() {
		if (!Gdx.input.isTouched())
			return false;
		return isBottomLeftCornerTouch(Gdx.input.getX(), Gdx.input.getY());
	}

	boolean isBottomLeftCornerTouch(int x, int y) {
		int height = Gdx.graphics.getHeight();
		return x <= FINGER_TOUCH_CORNER_SIZE
				&& y >= (height - FINGER_TOUCH_CORNER_SIZE);
	}

	boolean isUpperRightCornerTouch() {
		if (!Gdx.input.isTouched())
			return false;
		return isUpperRightCornerTouch(Gdx.input.getX(), Gdx.input.getY());
	}

	boolean isUpperRightCornerTouch(int x, int y) {
		int width = Gdx.graphics.getWidth();
		return x >= (width - FINGER_TOUCH_CORNER_SIZE)
				&& y <= FINGER_TOUCH_CORNER_SIZE;
	}

	TextureRegion screenCapture;
	Pixmap screenCapturePixmap;

	void destroyScreenCapture() {

		if (screenCapture != null) {
			Gdx.app.log(APP_NAME, "Destroying Screen Capture Region: "
					+ screenCapture.getTexture().getWidth() + "x"
					+ screenCapture.getTexture().getHeight());
			screenCapture.getTexture().dispose();
		}
		screenCapture = null;

		if (screenCapturePixmap != null) {
			Gdx.app.log(APP_NAME, "Destroying Screen Capture Pixmap: "
					+ screenCapturePixmap.getWidth() + "x"
					+ screenCapturePixmap.getHeight());
			screenCapturePixmap.dispose();
		}
		screenCapturePixmap = null;
	}

	@Override
	public void render() {
		if (!created)
			return;

		long start = System.nanoTime();

		GLCommon gl = null;
		if (Gdx.graphics.isGL20Available())
			gl = Gdx.gl20;
		else
			gl = Gdx.gl10;

		// GL10 gl = Gdx.gl10;

		gl.glClearColor(0, 0, 0, 1);
		gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);

		gl.glDisable(GL10.GL_DITHER);

		gl.glDepthMask(true);
		gl.glDisable(GL10.GL_DEPTH_TEST);
		gl.glDisable(GL10.GL_CULL_FACE);

		// gl.glShadeModel(GL10.GL_SMOOTH);
		gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_NICEST);
		gl.glHint(GL10.GL_LINE_SMOOTH_HINT, GL10.GL_NICEST);
		gl.glHint(GL10.GL_POLYGON_SMOOTH_HINT, GL10.GL_NICEST);
		gl.glEnable(GL10.GL_LINE_SMOOTH);

		// Gdx.gl11.glTexEnvf(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE,
		// GL10.GL_REPLACE);
		// Gdx.gl11.glTexEnvx(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE,
		// GL10.GL_MODULATE);

		gl.glActiveTexture(GL10.GL_TEXTURE0 + 0);
		gl.glDisable(GL10.GL_TEXTURE_2D);

		gl.glDisable(GL10.GL_BLEND);
		gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);

		if (!Gdx.graphics.isGL20Available()) {
			Gdx.gl10.glColor4f(1, 1, 1, 1);
		}

		if (Gdx.graphics.isGL11Available() || Gdx.graphics.isGL20Available()) {

			gl.glActiveTexture(GL10.GL_TEXTURE0 + 1);
			gl.glDisable(GL10.GL_TEXTURE_2D);

			gl.glDisable(GL10.GL_BLEND);
			gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
		}

		// spriteBatch.enableBlending();
		// spriteBatch.setBlendFunction(GL10.GL_SRC_ALPHA,
		// GL10.GL_ONE_MINUS_SRC_ALPHA);

		int screenWidth = Gdx.graphics.getWidth();
		int screenHeight = Gdx.graphics.getHeight();

		switch (renderState) {
		case Loading: {

			// NOT NEEDED (spriteBatch does it below) cameraSCREEN.apply(gl);
			// equivalent to:
			// gl.glMatrixMode(GL10.GL_PROJECTION);
			// gl.glLoadMatrixf(camera.projection.val, 0);
			// gl.glMatrixMode(GL10.GL_MODELVIEW);
			// gl.glLoadMatrixf(camera.view.val, 0);
			//
			// spriteBatch.getProjectionMatrix().setToOrtho2D(0, 0,
			// Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
			// spriteBatch.getProjectionMatrix().setToOrtho(0,
			// Gdx.graphics.getWidth(), 0, Gdx.graphics.getHeight(), 0, 1);
			spriteBatch.setProjectionMatrix(cameraSCREEN.combined);
			// spriteBatch.getProjectionMatrix().set(cameraSCREEN.combined);

			gl.glActiveTexture(GL10.GL_TEXTURE0 + 0);

			spriteBatch.enableBlending();
			spriteBatch.setBlendFunction(GL10.GL_SRC_ALPHA,
					GL10.GL_ONE_MINUS_SRC_ALPHA);
			spriteBatch.begin();
			if (groundTexture == null) {
				render_SCREEN_spriteBatch_Loading_FontAndTexture(-1,
						"ground texture");
				groundTexture = new Texture(
						Gdx.files.internal("data/planet_heavyclouds.png"));
				simulateLatencyOnDesktop();
			} else if (effect == null) {
				render_SCREEN_spriteBatch_Loading_FontAndTexture(-1,
						"particle effects");
				create_effects();
				simulateLatencyOnDesktop();
			} else {
				int size = sprite_Textures.size;
				if (size < sprite_ImagePaths.size) {
					String dataPath = sprite_ImagePaths.get(size);
					render_SCREEN_spriteBatch_Loading_FontAndTexture(-1,
							dataPath);
					create_sprite(dataPath);

					render_SCREEN_spriteBatch_Loading_FontAndTexture(size,
							dataPath);
				} else {
					if (squeezeTextures)
						finishCurrentTextureSqueezer();

					create_background(2);
					simulateLatencyOnDesktop();

					Gdx.app.postRunnable(new Runnable() {
						@Override
						public void run() {
							int width = Gdx.graphics.getWidth();
							int height = Gdx.graphics.getHeight();
							renderState = RenderState.Normal;
							resize(width, height);
						}
					});
				}
			}
			int fps = Gdx.graphics.getFramesPerSecond();
			render_SCREEN_spriteBatch_FPS_Font(fps);
			spriteBatch.end();

			// for (int i = 0; i < sprite_Textures.size; i++) {
			// Texture texture = sprite_Textures.get(i);
			// blurTexture(texture, 4);
			// }
			break;
		}
		case Normal: {

			if (Gdx.input.isKeyPressed(Input.Keys.SPACE)
					|| isUpperRightCornerTouch()) {

				if (screenCapture != null) {
					spriteBatch.setProjectionMatrix(cameraSCREEN.combined);

					gl.glActiveTexture(GL10.GL_TEXTURE0 + 0);
					spriteBatch.enableBlending();
					spriteBatch.setBlendFunction(GL10.GL_SRC_ALPHA,
							GL10.GL_ONE_MINUS_SRC_ALPHA);
					spriteBatch.begin();
					spriteBatch.draw(screenCapture, 0, 0);
					spriteBatch.end();
					break;
				}
			} else {
				destroyScreenCapture();
			}

			float ellapsed_ = (start - lastTimeBodiesDestroyed) / 1000000.0f; // ms
			if (lastTimeBodiesDestroyed == -1 || ellapsed_ > 3000f) // 3s
			// polling
			// interval
			{
				while (bodiesToDestroy.size > 0) {
					Body body = bodiesToDestroy.pop();

					Gdx.app.log(APP_NAME, "Delayed destroy: ["
							+ ((BodyUserData) body.getUserData()).textureIndex
							+ "]");

					killSprite(body);
				}

				lastTimeBodiesDestroyed = System.nanoTime();
			}

			int bodyCount = world.getBodyCount();
			if (bodyCount > 50) {
				minArea = MIN_AREA_STAGE2;
				checkBodyPolygonsAreasToDestroyBodies();
			} else if (bodyCount > 100) {
				minArea = MIN_AREA_STAGE3;
				checkBodyPolygonsAreasToDestroyBodies();
			} else if (bodyCount > 160) {
				minArea = MIN_AREA_STAGE4;
				checkBodyPolygonsAreasToDestroyBodies();
			} else {
				minArea = MIN_AREA_STAGE1;
			}

			// boolean desktop = Gdx.app.getType() == ApplicationType.Desktop;
			float delta = Gdx.graphics.getDeltaTime();
			// Gdx.app.log(APP_NAME, ""+delta); 0.033 or 0.016 seconds
			accum += delta;
			while (accum > step) {
				world.step(step, 8, 3);
				accum -= step;
			}
			// world.step(delta, 10, 10);

			for (int pointer = 0; pointer < MAX_TOUCH_POINTERS; pointer++) {
				if (performSlash[pointer]) {

					Gdx.app.log(APP_NAME, "==============");

					if (slashPoints.get(pointer).size == 2) {
						boolean cut = cutWorld(slashPoints.get(pointer).get(0),
								slashPoints.get(pointer).get(1), false);
					} else {
						Array<Vector2> entryPoints = new Array<Vector2>();
						Array<Vector2> exitPoints = new Array<Vector2>();
						Array<Body> bodies = new Array<Body>();

						for (int i = 0; i < slashPoints.get(pointer).size; i++) {
							Vector2 v1 = slashPoints.get(pointer).get(i);
							if (i == (slashPoints.get(pointer).size - 1))
								break;
							Vector2 v2 = slashPoints.get(pointer).get(i + 1);

							boolean cut = cutWorld(v1, v2, true);

							Gdx.app.log(APP_NAME, "cut_bodiesHitByRay: "
									+ cut_bodiesHitByRay.size);
							for (int u = 0; u < cut_bodiesHitByRay.size; u++) {
								Body body = cut_bodiesHitByRay.get(u);

								BodyUserData data = (BodyUserData) body
										.getUserData();
								if (data != null) {
									int bodyIndex = data.textureIndex;

									Gdx.app.log(APP_NAME,
											"cut_bodiesHitByRay BODY: "
													+ bodyIndex);
								} else {
									boolean breakpoint = true;
								}
							}

							Gdx.app.log(APP_NAME, "cut_fixtures: "
									+ cut_fixtures.size);
							for (int u = 0; u < cut_fixtures.size; u++) {
								Body body = cut_fixtures.get(u).getBody();

								BodyUserData data = (BodyUserData) body
										.getUserData();
								if (data != null) {
									int bodyIndex = data.textureIndex;

									Gdx.app.log(APP_NAME, "cut_fixtures BODY: "
											+ bodyIndex);
								} else {
									boolean breakpoint = true;
								}
							}

							Gdx.app.log(APP_NAME, "cut_entryPoints: "
									+ cut_entryPoints.size);

							Gdx.app.log(APP_NAME, "cut_exitPoints: "
									+ cut_exitPoints.size);

							Gdx.app.log(APP_NAME, "----------");

							if (cut) {
								Gdx.app.log(APP_NAME, "CUT 1 !");

								if (entryPoints.size > exitPoints.size
										&& bodies.size > 0) {
									bodies.removeIndex(bodies.size - 1);
									entryPoints
											.removeIndex(entryPoints.size - 1);
								}
							} else if (
							// cut_bodiesHitByRay.size > 0 &&
							// cut_bodiesHitByRay.get(0) ==
							// cut_fixtures.get(0).getBody() &&
							cut_fixtures.size > 0) {

								if (cut_entryPoints.size > 0
										&& (bodies.size == 0 || bodies
												.get(bodies.size - 1) != cut_fixtures
												.get(0).getBody())) {
									if (entryPoints.size > exitPoints.size
											&& bodies.size > 0) {
										bodies.removeIndex(bodies.size - 1);
										entryPoints
												.removeIndex(entryPoints.size - 1);
									}

									bodies.add(cut_fixtures.get(0).getBody());
									entryPoints.add(v1); // cut_entryPoints.get(0).cpy()
								}

								if (cut_exitPoints.size > 0) {

									for (int u = 0; u < cut_fixtures.size; u++) {
										Body body = cut_fixtures.get(u)
												.getBody();
										if (bodies.size > 0
												&& body == bodies
														.get(bodies.size - 1)
												&& cut_exitPoints
														.containsKey(u + 1)) {

											if (entryPoints.size == exitPoints.size) {

												exitPoints.get(
														exitPoints.size - 1)
														.set(v2);
											} else {

												exitPoints.add(v2); // cut_exitPoints.get(u
																	// +
																	// 1).cpy();
											}
										}
									}
								}
							}
						}

						if (entryPoints.size > exitPoints.size
								&& bodies.size > 0) {
							bodies.removeIndex(bodies.size - 1);
							entryPoints.removeIndex(entryPoints.size - 1);
						}

						for (int u = 0; u < bodies.size; u++) {
							boolean cut = cutWorld(entryPoints.get(u),
									exitPoints.get(u), false);
							if (cut) {
								Gdx.app.log(APP_NAME, "CUT 2 !");
							} else {
								Gdx.app.log(APP_NAME, "NOT CUT 2 ?");
							}
						}
					}

					slashPoints.get(pointer).clear();
					performSlash[pointer] = false;
				}
			}
			for (int pointer = 0; pointer < MAX_TOUCH_POINTERS; pointer++) {
				if (performBodyBreak[pointer] != null) {
					breakBody(performBodyBreak[pointer]);
					performBodyBreak[pointer] = null;
				}
			}

			float ellapsed = (start - lastTimeGravityCheck) / 1000000.0f; // ms
			if (lastTimeGravityCheck == -1 || ellapsed > 70.0f) // 70ms
																// polling
																// interval
			{
				updateOrientationAndSetGravity();

				// if (groundTween == null) {
				// launchGroundTween();
				// }
				if (groundTweenManager != null)
					groundTweenManager.update();

				if (cameraTweenManager != null)
					cameraTweenManager.update();

				lastTimeGravityCheck = System.nanoTime();
			}

			if (false) // see ground fixture restitution
				for (int i = 0; i < world.getContactCount(); i++) {
					Contact contact = world.getContactList().get(i);
					if (!contact.isTouching() || !contact.isEnabled()
							|| !contact.getFixtureA().getBody().isActive()
							|| !contact.getFixtureB().getBody().isActive())
						continue;
					boolean groundIsA = contact.getFixtureA().getBody() == groundBody;
					boolean groundIsB = contact.getFixtureB().getBody() == groundBody;
					if (!groundIsA && !groundIsB)
						continue;
					WorldManifold manifold = contact.getWorldManifold();
					int numContactPoints = manifold.getNumberOfContactPoints();

					Body bodySprite = groundIsA ? contact.getFixtureB()
							.getBody() : contact.getFixtureA().getBody();
					Body bodyGround = groundIsA ? contact.getFixtureA()
							.getBody() : contact.getFixtureB().getBody();

					// Vector2 normal = manifold.getNormal().cpy();
					// body.setLinearVelocity(normal.mul(body.getLinearVelocity().len()));
					// body.setLinearVelocity(normal.mul(body.getInertia()));
					// body.applyLinearImpulse(normal.mul(700),
					// body.getWorldCenter());
					// body.applyForce(normal.mul(-100), body.getWorldCenter());

					Vector2[] points = manifold.getPoints();
					for (int j = 0; j < numContactPoints; j++) {
						Vector2 point = points[j];

						Vector2 vSprite = bodySprite
								.getLinearVelocityFromWorldPoint(point);
						Vector2 vGround = bodyGround
								.getLinearVelocityFromWorldPoint(point);
						Vector2 vSub = vGround.cpy().sub(vSprite);

						// Dot producting the contact normal with gravity
						// indicates how floor-like the
						// contact is. If the dot product = 1, it is a flat
						// foor. If it is -1, it is
						// a ceiling. If it's 0.5, it's a sloped floor. Save the
						// contact manifold
						// that is the most floor like.
						float approachVelocity = manifold.getNormal().dot(vSub);
						if (approachVelocity > 5) {
							// Gdx.app.log(APP_NAME, "approachVelocity: " +
							// approachVelocity);
							bodySprite.applyLinearImpulse(manifold.getNormal()
									.cpy().mul(-2
									// approachVelocity
									// * bodySprite.getMass()
									// * 1.9f
									), bodySprite.getWorldCenter()
							// point
									);
							// break;
						}
						//
						// bodySprite.applyLinearImpulse(new Vector2(12.0f,
						// 0.0f), new Vector2(
						// bodySprite.getWidth() / (2 * PIXELS_PER_METER),
						// bodySprite.getHeight() / (2 * PIXELS_PER_METER)));
						// body.setLinearVelocity(manifold.getNormal().cpy().mul(approachVelocity));
						// bodySprite.applyForce(manifold.getNormal().cpy().mul(approachVelocity),
						// point);
						// bodySprite.setLinearVelocity(manifold.getNormal().cpy().mul(-approachVelocity
						// * 50));
						// break;
					}
				}
			//
			if (renderDebug != DebugRender.Medium) {

				if (!Gdx.graphics.isGL20Available()) {
					cameraSCREEN.apply(Gdx.gl10);
				}

				//
				// ==== SCREEN LAYER: WORLD REPEAT BACKGROUND
				render_SCREEN_mesh_worldRepeatBackground();
			}
			//
			// ==== SCENE LAYER: WORLD BACKGROUND
			if (renderDebug == DebugRender.None
					|| renderDebug == DebugRender.Medium) {
				if (!Gdx.graphics.isGL20Available()) {
					cameraSCENE.apply(Gdx.gl10);
				}

				render_SCENE_mesh_worldBackground(); // water ripples

			} else {
				spriteBatch.setProjectionMatrix(cameraSCENE.combined);
				//
				gl.glActiveTexture(GL10.GL_TEXTURE0 + 0);
				spriteBatch.disableBlending();
				spriteBatch.setBlendFunction(GL10.GL_SRC_ALPHA,
						GL10.GL_ONE_MINUS_SRC_ALPHA);
				// ---------- SPRITE BATCH BEGIN
				spriteBatch.begin();
				//
				render_SCENE_spriteBatch_worldBackground();
				//
				// ---------- SPRITE BATCH END
				spriteBatch.end();
				if (Gdx.graphics.isGL20Available()) {
					shaderMatrix.idt();
					spriteBatch.setTransformMatrix(shaderMatrix);
				} else {
					Gdx.gl10.glPopMatrix(); // IMPORTANT ! See PushMatrix in
					// render_SCENE_spriteBatch_worldBackground()
				}
			}
			//
			// ==== SCENE LAYER: WORLD BACKGROUND TEXT OVERLAY
			spriteBatch.setProjectionMatrix(cameraSCENE.combined);
			//

			gl.glActiveTexture(GL10.GL_TEXTURE0 + 0);
			spriteBatch.enableBlending();
			spriteBatch.setBlendFunction(GL10.GL_SRC_ALPHA,
					GL10.GL_ONE_MINUS_SRC_ALPHA);
			// ---------- SPRITE BATCH BEGIN
			spriteBatch.begin();
			//
			render_SCENE_spriteBatch_worldBackgroundTextOverlay();
			//
			// ---------- SPRITE BATCH END
			spriteBatch.end();
			if (Gdx.graphics.isGL20Available()) {
				shaderMatrix.idt();
				spriteBatch.setTransformMatrix(shaderMatrix);
			} else {
				Gdx.gl10.glPopMatrix(); // IMPORTANT ! See PushMatrix in
				// render_SCENE_spriteBatch_worldBackgroundTextOverlay()
			}
			//
			//
			spriteBatch.setProjectionMatrix(cameraSCREEN.combined);
			//
			// // ---------- SPRITE BATCH BEGIN
			// spriteBatch.disableBlending();
			// spriteBatch.begin();
			//
			// // ==== SCREEN LAYER (CENTER BACKGROUND)
			// if (renderDebug == DebugRender.None || renderDebug ==
			// DebugRender.Full) {
			// render_SCREEN_spriteBatch_background();
			// }
			//
			// // ---------- SPRITE BATCH END
			// spriteBatch.end();

			// ---------- SPRITE BATCH BEGIN

			gl.glActiveTexture(GL10.GL_TEXTURE0 + 0);
			spriteBatch.enableBlending();
			spriteBatch.setBlendFunction(GL10.GL_SRC_ALPHA,
					GL10.GL_ONE_MINUS_SRC_ALPHA);
			spriteBatch.begin();

			if (preferences.getBoolean(PREF_SHOW_PARTICLE_EFFECTS)) {

				// ==== SCREEN LAYER (PARTICLE EFFECTS)
				render_SCREEN_spriteBatch_particleEffects();

			}
			//
			// fontCache1.translate(0, Gdx.graphics.getHeight()/2);
			// fontCache1.draw(spriteBatch);

			// ---------- SPRITE BATCH END
			spriteBatch.end();
			// fontCache1.translate(-0, -Gdx.graphics.getHeight()/2);
			//

			// The code below is a workaround for particle effect spriteBatch
			// not closing its GL_BLEND texture mapping correctly...
			if (false && preferences.getBoolean(PREF_SHOW_PARTICLE_EFFECTS)) {

				gl.glActiveTexture(GL10.GL_TEXTURE0 + 0);
				gl.glDisable(GL10.GL_TEXTURE_2D);

				gl.glDisable(GL10.GL_BLEND);
				gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);

				if (!Gdx.graphics.isGL20Available()) {
					Gdx.gl10.glColor4f(1, 1, 1, 1);
				}

				gl.glActiveTexture(GL10.GL_TEXTURE0 + 1);
				gl.glDisable(GL10.GL_TEXTURE_2D);

				gl.glDisable(GL10.GL_BLEND);
				gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);

				if (!Gdx.graphics.isGL20Available()) {
					Gdx.gl10.glPointSize(1);
				}
				gl.glLineWidth(1);

				shapeRenderer.setProjectionMatrix(cameraSCREEN.combined);
				shaderMatrix.idt();
				shapeRenderer.setTransformMatrix(shaderMatrix);
				shapeRenderer.begin(ShapeType.Point);

				shapeRenderer.setColor(1, 0, 0, 0);
				shapeRenderer.point(0, 0, zCoord_box2D);

				shapeRenderer.end();
			}

			// Gdx.gl10.glEnable(GL10.GL_BLEND);
			// Gdx.gl10.glBlendFunc(GL10.GL_SRC_ALPHA,
			// GL10.GL_ONE_MINUS_SRC_ALPHA);
			// Gdx.gl10.glDisable(GL10.GL_BLEND);

			// spriteBatch.setProjectionMatrix(cameraSCENE.combined);
			// spriteBatch.getProjectionMatrix().set(cameraSCENE.combined);
			//
			// --- spriteBatch.BEGIN or camera.APPLY:
			// gl.glMatrixMode(GL10.GL_PROJECTION);
			// gl.glLoadMatrixf(spriteBatch.getProjectionMatrix().val, 0);
			// gl.glMatrixMode(GL10.GL_MODELVIEW);
			// gl.glLoadMatrixf(spriteBatch.getTransformMatrix().val, 0);
			//
			//
			// // ---------- SPRITE BATCH BEGIN
			// spriteBatch.enableBlending();
			// spriteBatch.setBlendFunction(GL10.GL_SRC_ALPHA,
			// GL10.GL_ONE_MINUS_SRC_ALPHA);
			//
			// spriteBatch.begin();

			// cameraSCENE.update();
			//
			// spriteBatch.setProjectionMatrix(cameraSCENE.combined);
			// gl.glMatrixMode(GL10.GL_PROJECTION);
			// gl.glLoadMatrixf(spriteBatch.getProjectionMatrix().val, 0);
			// gl.glMatrixMode(GL10.GL_MODELVIEW);
			// gl.glLoadMatrixf(spriteBatch.getTransformMatrix().val, 0);

			if (!Gdx.graphics.isGL20Available()) {
				cameraSCENE.apply(Gdx.gl10);
			}

			// ==== SCENE LAYER (ACTORS TEXTURES)
			render_SCENE_mesh_spriteBodies_Texture(); // Warning:
														// ImmediateRenderer is
														// called from within
														// (must camera.apply()
														// below !)

			// // ---------- SPRITE BATCH END
			// spriteBatch.end();

			//
			// cameraSCENE.apply(gl);
			// equivalent to:
			// gl.glMatrixMode(GL10.GL_PROJECTION);
			// gl.glLoadMatrixf(camera.projection.val, 0);
			// gl.glMatrixMode(GL10.GL_MODELVIEW);
			// gl.glLoadMatrixf(camera.view.val, 0);

			if (!Gdx.graphics.isGL20Available()) {
				cameraSCENE.apply(Gdx.gl10);
			}

			// ==== SCENE LAYER: SWINGING BLOCK
			render_SCENE_mesh_groundTextureFill();

			// cameraSCENE.apply(gl);

			// // ==== SCENE LAYER (ACTORS WIREFRAMES)
			// if (renderDebug.ordinal() >= DebugRender.Medium.ordinal()) {
			// render_SCENE_immediateRenderer_spriteBodies_Wireframe();
			// }

			// // ==== SCENE LAYER (ACTORS CONTACTS POINTS)
			// if (renderDebug.ordinal() >= DebugRender.Medium.ordinal()) {
			// render_SCENE_immediateRenderer_contactPoints();
			// }

			// ==== SCENE LAYER (ACTORS WIREFRAMES) using Box2D debugger
			// renderer
			if (renderDebug == DebugRender.Full) {
				box2d_renderer.render(world, cameraSCENE.combined, true);
			}

			// ==== SCENE LAYER (ACTORS WIREFRAMES)
			if (renderDebug == DebugRender.Medium) {

				if (Gdx.graphics.isGL20Available()) {
					frameBuffer.begin();
					gl.glClearColor(1, 1, 1, 0.0f);
					// gl.glPushAttrib(GL10.GL_VIEWPORT_BIT |
					// GL10.GL_COLOR_BUFFER_BIT);
					gl.glClear(GL10.GL_COLOR_BUFFER_BIT
							| GL10.GL_DEPTH_BUFFER_BIT);

					// gl.glBlendFuncSeparate(GL10.GL_SRC_ALPHA,
					// GL10.GL_ONE_MINUS_SRC_ALPHA, GL10.GL_ONE,
					// GL10.GL_ONE_MINUS_SRC_COLOR);

					//
					// gl.glEnable(GL10.GL_BLEND);
					// gl.glBlendFunc(GL10.GL_SRC_ALPHA,
					// GL10.GL_ONE_MINUS_SRC_ALPHA);

				}

				render_SCENE_immediateRenderer_spriteBodies_Outline();

				if (Gdx.graphics.isGL20Available()) {
					frameBuffer.end();
				}
			}

			if (!Gdx.graphics.isGL20Available()) {
				cameraSCENE.apply(Gdx.gl10);
			}

			render_SCENE_mesh_bookPages(start);

			// // ==== SCENE LAYER (ACTOR MOUSE JOINTS)
			// if (renderDebug.ordinal() >= DebugRender.Medium.ordinal()) {
			// render_SCENE_immediateRenderer_mouseJoints();
			// }

			// spriteBatch.getProjectionMatrix().setToOrtho2D(0, 0,
			// Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
			// spriteBatch.getProjectionMatrix().setToOrtho(0,
			// Gdx.graphics.getWidth(), 0, Gdx.graphics.getHeight(), 0, 1);
			spriteBatch.setProjectionMatrix(cameraSCREEN.combined);
			// spriteBatch.getProjectionMatrix().set(cameraSCREEN.combined);

			// ---------- SPRITE BATCH BEGIN

			gl.glActiveTexture(GL10.GL_TEXTURE0 + 0);
			spriteBatch.enableBlending();
			spriteBatch.setBlendFunction(GL10.GL_SRC_ALPHA,
					GL10.GL_ONE_MINUS_SRC_ALPHA);
			spriteBatch.begin();

			// ==== SCREEN LAYER (PARTICLE EFFECTS)
			// render_SCREEN_spriteBatch_particleEffects();

			// // ==== SCREEN LAYER (BOUNCING TEXT)
			// if (renderDebug == DebugRender.Full) {
			//
			// bouncingText_String = "(" + screenWidth + "," + screenHeight
			// + ")" + "{X:" + bouncingText_Position.x + "  Y:"
			// + bouncingText_Position.y + "}";
			// bouncingText_Bounds = font.getBounds(bouncingText_String);
			//
			// // render_SCREEN_spriteBatch_bouncingText_Font();
			// }

			// ==== SCREEN LAYER (UPPER-LEFT CORNER DEBUG STRINGS)
			if (renderDebug == DebugRender.Full) {
				render_SCREEN_spriteBatch_debugString_Font();
			}

			// ==== SCREEN LAYER (LOWER-RIGHT CORNER FPS DEBUG STRINGS)
			int fps = Gdx.graphics.getFramesPerSecond();
			render_SCREEN_spriteBatch_FPS_Font(fps);

			// ---------- SPRITE BATCH END
			spriteBatch.end();

			// NOT NEEDED (spriteBatch did it) cameraSCREEN.apply(gl);
			// equivalent to:
			// gl.glMatrixMode(GL10.GL_PROJECTION);
			// gl.glLoadMatrixf(camera.projection.val, 0);
			// gl.glMatrixMode(GL10.GL_MODELVIEW);
			// gl.glLoadMatrixf(camera.view.val, 0);
			//
			// gl.glMatrixMode(GL10.GL_PROJECTION);
			// gl.glLoadMatrixf(spriteBatch.getProjectionMatrix().val, 0);
			// gl.glMatrixMode(GL10.GL_MODELVIEW);
			// gl.glLoadMatrixf(spriteBatch.getTransformMatrix().val, 0);

			// ==== SCREEN LAYER: COMPASS
			if (renderDebug == DebugRender.Full) {

				if (!Gdx.graphics.isGL20Available()) {
					cameraSCREEN.apply(Gdx.gl10);
				}

				render_SCREEN_mesh_compass();
			}

			// ==== SCREEN LAYER (TOUCH DRAG TRACE)
			for (int pointer = 0; pointer < MAX_TOUCH_POINTERS; pointer++) {
				render_SCREEN_immediateRenderer_touchDrag(pointer);
			}

			// ==== SCREEN LAYER (BOUNCING TEXT)
			// if (renderDebug == DebugRender.Full) {
			// render_SCREEN_immediateRenderer_bouncingText_Borders();
			// }

			// Testing multitexturing
			// if (Gdx.graphics.isGL11Available()) {
			// GL11 gl11 = Gdx.graphics.getGL11();
			//
			// gl.glActiveTexture(GL10.GL_TEXTURE0);
			// gl.glEnable(GL10.GL_TEXTURE_2D);
			// gl.glEnable(GL10.GL_BLEND);
			// gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
			// tex1.bind();
			//
			// gl.glActiveTexture(GL10.GL_TEXTURE1);
			// gl.glEnable(GL10.GL_TEXTURE_2D);
			// gl.glDisable(GL10.GL_BLEND);
			// gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
			// tex2.bind();
			//
			// gl11.glTexEnvi(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE,
			// GL11.GL_COMBINE);
			// gl11.glTexEnvi(GL10.GL_TEXTURE_ENV, GL11.GL_COMBINE_RGB,
			// GL11.GL_ADD);
			//
			// gl11.glPushMatrix();
			// gl11.glTranslatef(Gdx.graphics.getWidth()/4,
			// Gdx.graphics.getHeight()/4, 0);
			// gl11.glScalef(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(),
			// 0);
			//
			// mesh.render(shader, GL10.GL_TRIANGLE_STRIP);
			//
			// gl11.glPopMatrix();
			//
			// gl11.glActiveTexture(GL10.GL_TEXTURE0 + 0);
			// gl11.glTexEnvi(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE,
			// GL10.GL_MODULATE);
			//
			// gl11.glActiveTexture(GL10.GL_TEXTURE0 + 1);
			// gl11.glTexEnvi(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE,
			// GL10.GL_MODULATE);
			// }

			// ==== SCREEN LAYER (LONG FINGER TOUCHES)
			// cameraSCREEN.apply(gl);
			gl.glLineWidth(14);

			gl.glActiveTexture(GL10.GL_TEXTURE0 + 1);
			gl.glDisable(GL10.GL_TEXTURE_2D);
			gl.glActiveTexture(GL10.GL_TEXTURE0 + 0);
			gl.glDisable(GL10.GL_TEXTURE_2D);

			gl.glEnable(GL10.GL_BLEND);
			gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);

			if (!Gdx.graphics.isGL20Available()) {
				Gdx.gl10.glColor4f(1, 1, 1, 1);
			}

			for (int pointer = 0; pointer < MAX_TOUCH_POINTERS; pointer++) {
				if (touchDownTimes[pointer] == -1)
					continue;
				ellapsed = (start - touchDownTimes[pointer]) / 1000000.0f; // ms
				if (ellapsed < touchDownTimeout) // 1s
					continue;

				if (thumbpad1.touchIndex == pointer
						|| thumbpad2.touchIndex == pointer) {
					if (!thumbpad1.sticky)
						thumbpad1.stick();
					if (!thumbpad2.sticky)
						thumbpad2.stick();

					continue;
				}

				MouseJoint mouseJoint = mouseJoints.get(pointer);
				if (mouseJoint != null) {
					Body body = mouseJoint.getBodyB();

					// body.setBullet(false);
					world.destroyJoint(mouseJoint);

					mouseJoints.set(pointer, null);
				}

				int x = Gdx.input.getX(pointer);
				int y = Gdx.graphics.getHeight() - Gdx.input.getY(pointer);

				// Gdx.app.log(APP_NAME, "TOUCH " + x + "," + y);

				shapeRenderer.setProjectionMatrix(cameraSCREEN.combined);
				shaderMatrix.idt();
				shapeRenderer.setTransformMatrix(shaderMatrix);
				shapeRenderer.begin(GL10.GL_LINE_STRIP);

				int radius = 60;
				for (int a = 0; a <= 360; a += 10) {
					float xx = x + radius * (float) Math.cos(Math.toRadians(a));
					float yy = y + radius * (float) Math.sin(Math.toRadians(a));
					shapeRenderer.setColor(0.1f, 1f, 0.4f, 0.5f);
					shapeRenderer.point(xx, yy, 0);
				}
				shapeRenderer.end();
			}

			// ==== SCREEN LAYER (ALWAYS-ON-TOP TACTILE CORNERS)
			// cameraSCREEN.apply(gl);

			gl.glLineWidth(1);

			gl.glActiveTexture(GL10.GL_TEXTURE0 + 1);
			gl.glDisable(GL10.GL_TEXTURE_2D);
			gl.glActiveTexture(GL10.GL_TEXTURE0 + 0);
			gl.glDisable(GL10.GL_TEXTURE_2D);

			gl.glEnable(GL10.GL_BLEND);
			gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);

			// if (!Gdx.graphics.isGL20Available()) {
			// Gdx.gl10.glColor4f(1, 1, 1, 0.5f);
			// }
			//
			shapeRenderer.setColor(1, 1, 1, 0.5f);

			int xyOffset = 1;

			shapeRenderer.setProjectionMatrix(cameraSCREEN.combined);
			shaderMatrix.idt();
			shapeRenderer.setTransformMatrix(shaderMatrix);
			shapeRenderer.begin(GL10.GL_LINE_LOOP);

			shapeRenderer.point(0 + xyOffset, FINGER_TOUCH_CORNER_SIZE, 0);
			shapeRenderer.point(0 + xyOffset, Gdx.graphics.getHeight()
					- FINGER_TOUCH_CORNER_SIZE, 0);
			shapeRenderer.point(FINGER_TOUCH_CORNER_SIZE,
					Gdx.graphics.getHeight() - FINGER_TOUCH_CORNER_SIZE, 0);
			shapeRenderer.point(FINGER_TOUCH_CORNER_SIZE,
					Gdx.graphics.getHeight() - xyOffset, 0);
			shapeRenderer.point(Gdx.graphics.getWidth()
					- FINGER_TOUCH_CORNER_SIZE, Gdx.graphics.getHeight()
					- xyOffset, 0);
			shapeRenderer.point(Gdx.graphics.getWidth()
					- FINGER_TOUCH_CORNER_SIZE, Gdx.graphics.getHeight()
					- FINGER_TOUCH_CORNER_SIZE, 0);
			shapeRenderer.point(Gdx.graphics.getWidth(),
					Gdx.graphics.getHeight() - FINGER_TOUCH_CORNER_SIZE, 0);
			shapeRenderer.point(Gdx.graphics.getWidth(),
					FINGER_TOUCH_CORNER_SIZE, 0);
			shapeRenderer.point(Gdx.graphics.getWidth()
					- FINGER_TOUCH_CORNER_SIZE, FINGER_TOUCH_CORNER_SIZE, 0);
			shapeRenderer.point(Gdx.graphics.getWidth()
					- FINGER_TOUCH_CORNER_SIZE, 0 + xyOffset, 0);
			shapeRenderer.point(FINGER_TOUCH_CORNER_SIZE, 0 + xyOffset, 0);
			shapeRenderer.point(FINGER_TOUCH_CORNER_SIZE,
					FINGER_TOUCH_CORNER_SIZE, 0);

			shapeRenderer.end();

			// ==== SCREEN LAYER (ALWAYS-ON-TOP THUMBPAD)
			thumbpad1.render(gl, cameraSCREEN);
			thumbpad2.render(gl, cameraSCREEN);

			//
			world.clearForces();
			break;
		}
		case Disposed: {
			break;
		}
		case Error: {
			break;
		}
		}

		gl.glFlush();
		gl.glFinish();

		float renderTime = (System.nanoTime() - start) / 1000000.0f; // ms
		// 30 fps => 1/30 = 0.03333s = 33.33ms
		// 60 fps => 1/60 = 0.01666s = 16.66ms
		long remainder = (long) (step - renderTime);
		// Gdx.app.log(APP_NAME, remainder + "ms");
		if (remainder > 10)
			try {
				Thread.sleep(remainder);
			} catch (InterruptedException e) {
			}

		if ((Gdx.input.isKeyPressed(Input.Keys.SPACE) || isUpperRightCornerTouch())
				&& screenCapture == null) {

			int GL_internalFormat = createOrUpdateScreenCapture();

			if (GL_internalFormat != -1) {
				Texture tex = blurTexture(screenCapture.getTexture(), 4,
						BlurType.NORMAL, GL_internalFormat);
				if (tex != null) {
					screenCapture.getTexture().dispose();
					screenCapture.setRegion(tex);
				}
				// blurTexture(screenCapture.getTexture(), 10, BlurType.NORMAL);
				// blurTexture(screenCapture.getTexture(), 40, BlurType.ZOOM);
			}
		}
	}

	float accum = 0;
	float step = 1f / 30f;

	int createOrUpdateScreenCapture() {
		int GL_internalFormat = 0;

		if (false) {
			screenCapture = ScreenUtils.getFrameBufferTexture();

			screenCapture.getTexture().setFilter(Texture.TextureFilter.Linear,
					Texture.TextureFilter.Linear);

			Format format = screenCapture.getTexture().getTextureData()
					.getFormat();
			GL_internalFormat = format == Format.RGBA8888
					|| format == Format.RGBA4444 ? GL10.GL_RGBA
					: format == Format.RGB888 || format == Format.RGB565 ? GL10.GL_RGB
							: format == Format.LuminanceAlpha ? GL10.GL_LUMINANCE_ALPHA
									: GL10.GL_ALPHA;
		} else {
			int screenWidth = Gdx.graphics.getWidth();
			int screenHeight = Gdx.graphics.getHeight();

			final int potW = MathUtils.nextPowerOfTwo(screenWidth);
			final int potH = MathUtils.nextPowerOfTwo(screenHeight);

			if (screenCapturePixmap != null
					&& (screenCapturePixmap.getWidth() != potW || screenCapturePixmap
							.getHeight() != potH)) {

				destroyScreenCapture();
			}

			boolean blurPixmap = false; // slow and buggy

			if (screenCapturePixmap == null) {
				Gdx.app.log(APP_NAME, "Creating Screen Capture Pixmap: " + potW
						+ "x" + potH);

				screenCapturePixmap = new Pixmap(potW, potH, Format.RGB565);
				// blurPixmap ? Format.RGBA8888 : Format.RGB565);
			}

			ByteBuffer pixels = screenCapturePixmap.getPixels();
			// pixels.clear();

			// GL_RGB + GL_UNSIGNED_SHORT_5_6_5
			// GL_RGBA + GL_UNSIGNED_BYTE
			Gdx.gl.glReadPixels(0, 0, potW,
					potH, //
					screenCapturePixmap.getGLFormat(),
					screenCapturePixmap.getGLType(), pixels);

			if (blurPixmap) {
				// blurImage(4, screenCapturePixmap, screenWidth, screenHeight,
				// screenWidth / 8, screenHeight / 8, screenWidth
				// - screenWidth / 4, screenHeight - screenHeight
				// / 4);
				// blurImage(4, screenCapturePixmap, screenWidth, screenHeight,
				// 0,
				// 0, screenWidth, screenHeight);

				Blur.blurImage(4, screenCapturePixmap,
						screenCapturePixmap.getWidth(),
						screenCapturePixmap.getHeight(), 0, 0,
						screenCapturePixmap.getWidth(),
						screenCapturePixmap.getHeight());

				GL_internalFormat = -1;
			} else {
				GL_internalFormat = screenCapturePixmap.getGLInternalFormat();
			}

			if (screenCapture == null) {
				Gdx.app.log(APP_NAME, "Creating Screen Capture Texture: "
						+ screenWidth + "x" + screenHeight);

				Texture tex = new Texture(screenCapturePixmap);
				tex.setFilter(Texture.TextureFilter.Linear,
						Texture.TextureFilter.Linear);

				screenCapture = new TextureRegion(tex, 0, screenHeight,
						screenWidth, -screenHeight);
			} else {
				Gdx.app.log(APP_NAME,
						"Drawing Screen Capture Pixmap into Texture: "
								+ screenCapturePixmap.getWidth() + "x"
								+ screenCapturePixmap.getHeight());

				// Uses glTexSubImage2D
				screenCapture.getTexture().draw(screenCapturePixmap, 0, 0);
			}
		}

		return GL_internalFormat;
	}

	void render_SCREEN_spriteBatch_particleEffects() {
		GLCommon gl = null;
		if (Gdx.graphics.isGL20Available())
			gl = Gdx.gl20;
		else
			gl = Gdx.gl10;

		// GL10 gl = Gdx.gl10;
		//
		// gl.glActiveTexture(GL10.GL_TEXTURE0 + 0);
		// gl.glEnable(GL10.GL_TEXTURE_2D);
		//
		// gl.glEnable(GL10.GL_BLEND);
		// gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
		// if (!Gdx.graphics.isGL20Available()) {
		// Gdx.gl10.glColor4f(1, 1, 1, 1);
		// }

		if (effect != null && !effect.isComplete()) {
			float delta = Gdx.graphics.getDeltaTime();
			effect.draw(spriteBatch, delta);
		}
	}

	void render_SCREEN_immediateRenderer_touchDrag(int pointer) {

		GLCommon gl = null;
		if (Gdx.graphics.isGL20Available())
			gl = Gdx.gl20;
		else
			gl = Gdx.gl10;

		// GL10 gl = Gdx.gl10;

		gl.glActiveTexture(GL10.GL_TEXTURE0 + 1);
		gl.glDisable(GL10.GL_TEXTURE_2D);
		gl.glActiveTexture(GL10.GL_TEXTURE0 + 0);
		gl.glDisable(GL10.GL_TEXTURE_2D);

		gl.glDisable(GL10.GL_BLEND);
		gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);

		if (!Gdx.graphics.isGL20Available()) {
			Gdx.gl10.glColor4f(1, 1, 1, 1);
		}

		gl.glLineWidth(8);

		boolean colorDash = true;
		if (slashPoints.get(pointer).size >= 2) {
			for (int i = 0; i < slashPoints.get(pointer).size; i++) {
				Vector2 v1 = slashPoints.get(pointer).get(i);
				if (i == (slashPoints.get(pointer).size - 1))
					break;
				Vector2 v2 = slashPoints.get(pointer).get(i + 1);

				colorDash = !colorDash;

				shapeRenderer.setProjectionMatrix(cameraSCREEN.combined);
				shaderMatrix.idt();
				shapeRenderer.setTransformMatrix(shaderMatrix);
				shapeRenderer.begin(GL10.GL_LINE_STRIP);

				if (colorDash)
					shapeRenderer.setColor(1, 1, 1, 1);
				else
					shapeRenderer.setColor(0, 0, 0, 1);

				shapeRenderer.point(v1.x, Gdx.graphics.getHeight() - v1.y,
						zCoord_box2D);

				if (colorDash)
					shapeRenderer.setColor(1, 1, 1, 1);
				else
					shapeRenderer.setColor(0, 0, 0, 1);

				shapeRenderer.point(v2.x, Gdx.graphics.getHeight() - v2.y,
						zCoord_box2D);

				shapeRenderer.end();
			}
		}

		gl.glLineWidth(4);

		gl.glEnable(GL10.GL_BLEND);
		gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);

		if (slashPointsSpline.get(pointer).size > 0) {

			float alpha = 0;
			float alphaIncrement = 1.0f / slashPointsSpline.get(pointer).size;
			for (int i = 0; i < slashPointsSpline.get(pointer).size; i++) {
				Vector2 v1 = slashPointsSpline.get(pointer).get(i);
				if (i == (slashPointsSpline.get(pointer).size - 1))
					break;
				Vector2 v2 = slashPointsSpline.get(pointer).get(i + 1);

				shapeRenderer.setProjectionMatrix(cameraSCREEN.combined);
				shaderMatrix.idt();
				shapeRenderer.setTransformMatrix(shaderMatrix);
				shapeRenderer.begin(GL10.GL_LINE_STRIP);

				shapeRenderer.setColor(1, 0, 0, alpha);
				shapeRenderer.point(v1.x, Gdx.graphics.getHeight() - v1.y,
						zCoord_box2D);

				alpha += alphaIncrement;
				if (alpha < 0)
					alpha = 0;
				shapeRenderer.setColor(1, 0, 0, alpha);
				shapeRenderer.point(v2.x, Gdx.graphics.getHeight() - v2.y,
						zCoord_box2D);

				shapeRenderer.end();
			}

			if (slashPointsSplineFadeoutTime[pointer] >= 0
					&& slashPointsSpline.get(pointer).size > 0) {
				long now = System.nanoTime();
				float ellapsed = (now - slashPointsSplineFadeoutTime[pointer]) / 1000000.0f; // ms

				if (ellapsed > 70) { // 70ms
					slashPointsSpline.get(pointer).removeIndex(0);
				}
				if (slashPointsSpline.get(pointer).size == 0) {
					slashPointsSplineFadeoutTime[pointer] = -1;
				}
			}
		}
	}

	void render_SCREEN_spriteBatch_debugString_Font() {
		int screenWidth = Gdx.graphics.getWidth();
		int screenHeight = Gdx.graphics.getHeight();
		// TextBounds textBounds = font.getBounds(text);
		// font.getLineHeight()

		GLCommon gl = null;
		if (Gdx.graphics.isGL20Available())
			gl = Gdx.gl20;
		else
			gl = Gdx.gl10;

		// GL10 gl = Gdx.gl10;
		//
		// gl.glActiveTexture(GL10.GL_TEXTURE0 + 0);
		// gl.glEnable(GL10.GL_TEXTURE_2D);
		//
		// gl.glEnable(GL10.GL_BLEND);
		// gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
		//
		// if (!Gdx.graphics.isGL20Available()) {
		// Gdx.gl10.glColor4f(1, 1, 1, 1);
		// }

		fonts.get(currentFont).setColor(Color.WHITE);
		fonts.get(currentFont).setScale(0.6f);
		fonts.get(currentFont).drawMultiLine(spriteBatch,
				getDebugDisplayString(), 0, screenHeight);
	}

	void render_SCREEN_mesh_compass() {
		int screenWidth = Gdx.graphics.getWidth();
		int screenHeight = Gdx.graphics.getHeight();
		int centerX = screenWidth / 2;
		int centerY = screenHeight / 2;

		GLCommon gl = null;
		if (Gdx.graphics.isGL20Available())
			gl = Gdx.gl20;
		else
			gl = Gdx.gl10;

		// GL10 gl = Gdx.gl10;
		//

		gl.glActiveTexture(GL10.GL_TEXTURE0 + 0);
		gl.glDisable(GL10.GL_TEXTURE_2D);

		gl.glDisable(GL10.GL_BLEND);
		gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);

		if (!Gdx.graphics.isGL20Available()) {
			Gdx.gl10.glColor4f(1, 1, 1, 1);
		}
		if (Gdx.graphics.isGL20Available()) {

			shaderMatrix.set(cameraSCREEN.combined);
			shaderMatrix.translate(centerX, centerY, 0);

		} else {
			Gdx.gl10.glPushMatrix();
			// gl.glMatrixMode(GL10.GL_PROJECTION);
			// gl.glLoadIdentity();
			// float aspect = (float) screenHeight / screenWidth;
			// gl.glOrthof(-1, 1, -1 * aspect, 1 * aspect, 0, -1);
			/*
			 * if (aspect == 1) // square { gl.glOrthof(-1, 1, -1, 1, 0, -1); }
			 * if (aspect < 1) // landscape { gl.glOrthof(-1, 1, -1 * aspect, 1
			 * * aspect, 0, -1); } if (aspect > 1) // portrait { gl.glOrthof(-1,
			 * 1, -1 * aspect, 1 * aspect, 0, -1); }
			 */
			// gl.glMatrixMode(GL10.GL_MODELVIEW);
			// gl.glLoadIdentity();
			Gdx.gl10.glTranslatef(centerX, centerY, 0);
		}
		if (false) // poll sensors
		{
			switch (orientation) {
			case PORTRAIT_NORMAL:
				if (Gdx.graphics.isGL20Available()) {
					shaderMatrix.rotate(0, 0, 1, Gdx.input.getRoll());
				} else {
					Gdx.gl10.glRotatef(Gdx.input.getRoll(), 0, 0, 1);
				}
				// Gdx.app.log(APP_NAME, "P NORMAL");
				break;
			case PORTRAIT_UPSIDEDOWN:
				if (Gdx.graphics.isGL20Available()) {
					shaderMatrix.rotate(0, 0, 1, -Gdx.input.getRoll());
				} else {
					Gdx.gl10.glRotatef(-Gdx.input.getRoll(), 0, 0, 1);
				}
				// Gdx.app.log(APP_NAME, "P UPSIDEDOWN");
				break;
			case LANDSCAPE_NORMAL:
				if (Gdx.graphics.isGL20Available()) {
					shaderMatrix.rotate(0, 0, 1, -Gdx.input.getPitch());
				} else {
					Gdx.gl10.glRotatef(-Gdx.input.getPitch(), 0, 0, 1);
				}
				float x = (float) Math.cos(Gdx.input.getPitch());
				float y = (float) Math.sin(Gdx.input.getPitch());
				// world.setGravity(new Vector2(x, y));
				// Gdx.app.log(APP_NAME, "x:" + x + ", y:" + y);
				break;
			case LANDSCAPE_UPSIDEDOWN:
				if (Gdx.graphics.isGL20Available()) {
					shaderMatrix.rotate(0, 0, 1, Gdx.input.getPitch());
				} else {
					Gdx.gl10.glRotatef(Gdx.input.getPitch(), 0, 0, 1);
				}
				float xx = (float) Math.sin(Gdx.input.getPitch());
				float yy = (float) Math.cos(Gdx.input.getPitch());
				// world.setGravity(new Vector2(xx, yy));
				// Gdx.app.log(APP_NAME, "xx:" + xx + ", yy:" + yy);
				break;
			}
		} else {
			Vector2 gravity = world.getGravity();
			// float angle = (float)Math.acos(vertical.dot(gravity));
			float angle = (float) (Math.atan2(gravity.y, gravity.x) - Math
					.atan2(gravityVerticalDown.y, gravityVerticalDown.x));
			if (Gdx.graphics.isGL20Available()) {
				shaderMatrix.rotate(0, 0, 1, (float) Math.toDegrees(angle));
			} else {
				Gdx.gl10.glRotatef((float) Math.toDegrees(angle), 0, 0, 1);
			}
		}

		if (Gdx.graphics.isGL20Available()) {

			shader_POSITION_COLOR.begin();
			shader_POSITION_COLOR.setUniformMatrix(
					ShaderProgram_PROJECTION_VIEW_MATRIX_UNIFORM, shaderMatrix);
			// shader_POSITION_COLOR.setUniformi(ShaderProgram_TEXTURE_SAMPLER_UNIFORM
			// + 0, 0);
			shader_POSITION_COLOR.setUniformf(
					LibGDXPlayground.ShaderProgram_VERTEX_COLOR_UNIFORM, 0, 0,
					0, 0);

			compassMesh.render(shader_POSITION_COLOR, GL10.GL_TRIANGLES);

			shader_POSITION_COLOR.end();
		} else {
			compassMesh.render(GL10.GL_TRIANGLES);
		}
		//
		// renderer.begin(GL10.GL_TRIANGLES);
		// renderer.color(1, 1, 1, 1); // WHITE
		// // renderer.vertex(-0.3f, -0.5f, zPlane);
		// renderer.vertex(-50, -100, zPlane);
		// renderer.color(1, 0, 0, 1); // RED
		// renderer.vertex(50, -100, zPlane);
		// renderer.color(0, 1, 0, 1); // GREEN
		// renderer.vertex(0, 100, zPlane);
		// renderer.end();
		//
		if (!Gdx.graphics.isGL20Available()) {
			Gdx.gl10.glPopMatrix();
		}
	}

	// void render_SCREEN_spriteBatch_background() {
	// if (backgroundTexture == null)
	// return;
	//
	// GL10 gl = Gdx.gl10;
	// gl.glDisable(GL10.GL_BLEND);
	//
	// int screenWidth = Gdx.graphics.getWidth();
	// int screenHeight = Gdx.graphics.getHeight();
	// int centerX = screenWidth / 2;
	// int centerY = screenHeight / 2;
	// font.setColor(Color.WHITE);
	// spriteBatch.setColor(1, 1, 1, 1);
	// spriteBatch.draw(backgroundTexture,
	// centerX - backgroundTexture.getWidth() / 2, centerY
	// - backgroundTexture.getHeight() / 2, 0, 0,
	// backgroundTexture.getWidth(), backgroundTexture.getHeight());
	// }

	String[] fpsStringCache = new String[] { "0", "1", "2", "3", "4", "5", "6",
			"7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17",
			"18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28",
			"29", "30", "31", "32", "33", "34" };

	void render_SCREEN_spriteBatch_FPS_Font(int fps) {
		int screenWidth = Gdx.graphics.getWidth();
		int screenHeight = Gdx.graphics.getHeight();
		int centerX = screenWidth / 2;
		int centerY = screenHeight / 2;

		GLCommon gl = null;
		if (Gdx.graphics.isGL20Available())
			gl = Gdx.gl20;
		else
			gl = Gdx.gl10;

		// GL10 gl = Gdx.gl10;
		//
		// gl.glActiveTexture(GL10.GL_TEXTURE0 + 0);
		// gl.glEnable(GL10.GL_TEXTURE_2D);
		//
		// gl.glEnable(GL10.GL_BLEND);
		// gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
		//
		// if (!Gdx.graphics.isGL20Available()) {
		// Gdx.gl10.glColor4f(1, 1, 1, 1);
		// }

		// String text = "fps: " + fps;

		// text += " SCALE: " + (int) MT_scale + " ANGLE: " + (int)
		// Math.toDegrees(MT_angle) + " OFFSET: " + (int) MT_offset.x + "/" +
		// (int) MT_offset.y;
		if (renderDebug == DebugRender.Full) {
			String text = "fps: " + fps;
			text += " dir: " + cameraSCENE.direction.x + "/"
					+ cameraSCENE.direction.y + "/" + cameraSCENE.direction.z
					+ " up: " + cameraSCENE.up.x + "/" + cameraSCENE.up.y + "/"
					+ cameraSCENE.up.z + " pos: " + cameraSCENE.position.x
					+ "/" + cameraSCENE.position.y + "/"
					+ cameraSCENE.position.z;

			TextBounds textBounds = fonts.get(currentFont).getBounds(text);

			fonts.get(currentFont).setColor(Color.RED);
			fonts.get(currentFont).setScale(0.6f);
			fonts.get(currentFont).draw(spriteBatch, text,
					screenWidth - textBounds.width, textBounds.height // font.getLineHeight()
					);
		} else {
			fonts.get(currentFont).setColor(Color.RED);
			fonts.get(currentFont).setScale(1.0f);
			fonts.get(currentFont).draw(
					spriteBatch,
					fps < fpsStringCache.length ? fpsStringCache[fps]
							: ("FPS: " + fps), 10, 40);
		}
	}

	Vector3 temporaryPointForProjection = new Vector3(0, 0, zCoord_box2D);
	Vector2 temporaryPointForDragging = new Vector2(0, 0);

	// void render_SCENE_immediateRenderer_mouseJoints() {
	// if (cameraSCENE == null)
	// return;
	//
	// boolean first = true;
	// GL10 gl = Gdx.gl10;
	// gl.glDisable(GL10.GL_BLEND);
	//
	// for (int i = 0; i < mouseJoints.size; i++) {
	// MouseJoint mouseJoint = mouseJoints.get(i);
	// if (mouseJoint == null)
	// continue;
	// Vector2 anchorA = mouseJoint.getAnchorA();
	// Vector2 anchorB = mouseJoint.getAnchorB();
	//
	// // gl.glPushMatrix();
	// gl.glPointSize(8 * (cameraSCENE instanceof OrthographicCamera ? MT_scale
	// : 1)); // camera.zoom
	// immediateModeRenderer.begin(GL10.GL_POINTS);
	// immediateModeRenderer.color(1, 0, 0, 1); // RED
	// immediateModeRenderer.vertex(anchorA.x, anchorA.y, box2D_zCoord);
	// immediateModeRenderer.color(0, 0, 1, 1); // BLUE
	// immediateModeRenderer.vertex(anchorB.x, anchorB.y, box2D_zCoord);
	// immediateModeRenderer.end();
	// gl.glPointSize(1);
	// gl.glLineWidth(4 * (cameraSCENE instanceof OrthographicCamera ? MT_scale
	// : 1)); // camera.zoom
	// immediateModeRenderer.begin(GL10.GL_LINE_STRIP);
	// immediateModeRenderer.color(1, 1, 1, 1); // WHITE
	// immediateModeRenderer.vertex(anchorA.x, anchorA.y, box2D_zCoord);
	// immediateModeRenderer.color(1, 1, 1, 1); // WHITE
	// immediateModeRenderer.vertex(anchorB.x, anchorB.y, box2D_zCoord);
	// immediateModeRenderer.end();
	// float touchPrint = Math.min(worldSize.y, worldSize.x) / 7;
	// immediateModeRenderer.begin(GL10.GL_LINE_STRIP);
	// for (int a = 0; a <= 360; a += 20) {
	// float x = anchorA.x + touchPrint
	// * (float) Math.cos(Math.toRadians(a));
	// float y = anchorA.y + touchPrint
	// * (float) Math.sin(Math.toRadians(a));
	// immediateModeRenderer.color(0, 1, 0, 1); // GREEN
	// immediateModeRenderer.vertex(x, y, box2D_zCoord);
	// }
	// immediateModeRenderer.end();
	// gl.glLineWidth(1);
	// // gl.glPopMatrix();
	// if (first && effect != null) {
	// first = false;
	// temporaryPointProject(anchorB.x, anchorB.y, box2D_zCoord);
	//
	// effect.setPosition(temporaryPointForProjection.x,
	// temporaryPointForProjection.y);
	// // effect.setPosition(Gdx.input.getX(i),
	// // Gdx.graphics.getHeight() - Gdx.input.getY(i));
	// }
	// }
	// }

	// void render_SCENE_immediateRenderer_contactPoints() {
	// int fps = Gdx.graphics.getFramesPerSecond();
	// if (world == null || fps < 22)
	// return;
	//
	// GL10 gl = Gdx.gl10;
	// gl.glDisable(GL10.GL_BLEND);
	// // gl.glPushMatrix();
	// gl.glPointSize(8);
	// immediateModeRenderer.begin(GL10.GL_POINTS);
	// for (int i = 0; i < world.getContactCount(); i++) {
	// Contact contact = world.getContactList().get(i);
	// if (!contact.isTouching() || !contact.isEnabled()
	// || !contact.getFixtureA().getBody().isActive()
	// || !contact.getFixtureB().getBody().isActive())
	// continue;
	// WorldManifold manifold = contact.getWorldManifold();
	// int numContactPoints = manifold.getNumberOfContactPoints();
	// Vector2[] points = manifold.getPoints();
	// for (int j = 0; j < numContactPoints; j++) {
	// Vector2 point = points[j];
	// immediateModeRenderer.color(0, 1, 0, 1);
	// immediateModeRenderer.vertex(point.x, point.y, box2D_zCoord);
	// }
	// }
	// immediateModeRenderer.end();
	// gl.glPointSize(1);
	// // gl.glPopMatrix();
	// }

	// int computeModuloSpriteRotationThing(int i) {
	// int index = i;
	// if (index >= sprite_ImagePaths.size) {
	// int parts = (int) ((float) index / sprite_ImagePaths.size);
	// index = index - (parts * sprite_ImagePaths.size);
	// }
	// return index;
	// }

	float getWorldMinX() {
		return -worldSize.x / 2;
	}

	float getWorldMaxX() {
		return worldSize.x / 2;
	}

	float getWorldMinY() {
		return (worldSize.y / GROUND_SIZE_FRACTION.y / 2);
	}

	float getWorldMaxY() {
		return worldSize.y;
	}

	// void render_SCENE_immediateRenderer_spriteBodies_Wireframe() {
	// boolean debugRed = false;
	// GL10 gl = Gdx.gl10;
	// gl.glDisable(GL10.GL_BLEND);
	// float ratio = SPRITES_SCALE_FACTOR / SCREEN_TO_WORLD_RATIO;
	// Vector2 bodySize = new Vector2(0, 0);
	// for (int i = 0; i < spriteBodies.size; i++) {
	// Body body = spriteBodies.get(i);
	// if (!body.isActive())
	// continue; // set in render_spriteBatch_spriteBodies_Texture()
	// // !!!
	// // int index = computeModuloSpriteRotationThing(i);
	// int index = ((Integer) body.getUserData()).intValue();
	// Texture texture = sprite_Textures.get(index);
	// bodySize.x = ratio * texture.getWidth();
	// bodySize.y = ratio * texture.getHeight();
	// float halfWidth = bodySize.x / 2;
	// float halfHeight = bodySize.y / 2;
	// Vector2 position = body.getPosition();
	// float angle = body.getAngle();
	// float minX = getWorldMinX();
	// float maxX = getWorldMaxX();
	// float minY = getWorldMinY();
	// float maxY = getWorldMaxY();
	// if (position.x > maxX) {
	// debugRed = true;
	//
	// // // NOT ACTIVE (already filtered above)
	// // if (position.x > maxX + bodySize.x) {
	// // continue;
	// // }
	// }
	// if (position.x < minX) {
	// debugRed = true;
	//
	// // // NOT ACTIVE (already filtered above)
	// // if (position.x < minX - bodySize.x) {
	// // continue;
	// // }
	// }
	// if (position.y > maxY) {
	// debugRed = true;
	// }
	// if (position.y < minY) {
	// debugRed = true;
	//
	// // // NOT ACTIVE (already filtered above)
	// // if (position.y < -maxY) {
	// // continue;
	// // }
	// }
	// gl.glPushMatrix();
	// gl.glLoadIdentity();
	// gl.glTranslatef(position.x, position.y, 0);
	// gl.glRotatef((float) Math.toDegrees(angle), 0, 0, 1);
	// if (true) {
	// // gl.glRotatef((float) 180, 1, 0, 0);
	// // gl.glScalef(ratio, -ratio, 1);
	// if (renderDebug.ordinal() < DebugRender.Full.ordinal()) {
	// Array<Array<Vector2>> polygons = sprite_BodyPolygons
	// .get(index);
	// Vector2 vertexFirst = new Vector2(0, 0);
	// for (int z = 0; z < polygons.size; z++) {
	//
	// immediateModeRenderer.begin(GL10.GL_LINE_STRIP);
	// vertexFirst.x = 0;
	// vertexFirst.y = 0;
	// Array<Vector2> vertices = polygons.get(z);
	// for (int j = 0; j < vertices.size; j++) {
	// Vector2 vertex = vertices.get(j);
	// if (j == 0) {
	// vertexFirst.x = vertex.x;
	// vertexFirst.y = vertex.y;
	// }
	// immediateModeRenderer.color(1, 1, 1, 1); // WHITE
	// immediateModeRenderer.vertex(ratio * vertex.x
	// - halfWidth, halfHeight - ratio * vertex.y,
	// box2D_zCoord);
	// }
	// immediateModeRenderer.color(1, 1, 1, 1); // WHITE
	// immediateModeRenderer.vertex(ratio * vertexFirst.x
	// - halfWidth,
	// halfHeight - ratio * vertexFirst.y,
	// box2D_zCoord);
	// immediateModeRenderer.end();
	// }
	// } else {
	// Vector2 vertex = new Vector2(0, 0);
	// Vector2 vertexFirst = new Vector2(0, 0);
	// ArrayList<Fixture> list = body.getFixtureList();
	// for (int z = 0; z < list.size(); z++) {
	// PolygonShape poly = (PolygonShape) list.get(z)
	// .getShape();
	//
	// immediateModeRenderer.begin(GL10.GL_LINE_STRIP);
	// vertexFirst.x = 0;
	// vertexFirst.y = 0;
	// for (int j = 0; j < poly.getVertexCount(); j++) {
	// vertex.x = 0;
	// vertex.y = 0;
	// poly.getVertex(j, vertex);
	// if (j == 0) {
	// vertexFirst.x = vertex.x;
	// vertexFirst.y = vertex.y;
	// }
	// immediateModeRenderer.color(1, 1, 1, 1); // WHITE
	// immediateModeRenderer.vertex(vertex.x, vertex.y,
	// box2D_zCoord);
	// }
	// immediateModeRenderer.color(1, 1, 1, 1); // WHITE
	// immediateModeRenderer.vertex(vertexFirst.x,
	// vertexFirst.y, box2D_zCoord);
	// immediateModeRenderer.end();
	// }
	// }
	// } else {
	// // gl.glScalef(ratio, ratio, 1);
	// // Mesh mesh = sprite_BodyPolygonsMeshes.get(index);
	// // mesh.render(shader, GL10.GL_LINE_LOOP);
	// }
	//
	// gl.glPopMatrix();
	// }
	// }

	Vector2 tempRenderVector2 = new Vector2();

	// Vector2 tempRenderVector2_ = new Vector2();

	void render_SCENE_immediateRenderer_spriteBody_Destroying_Outline(Body body) {
		float ratio = SPRITES_SCALE_FACTOR / PIXEL_TO_WORLD_RATIO;
		Vector2 bodySize = new Vector2(0, 0);

		for (int i = 0; i < bodiesToDestroy.size; i++) {
			Body b = bodiesToDestroy.get(i);

			if (b != body)
				continue;

			BodyUserData userData = (BodyUserData) body.getUserData();

			SubTexture subtexture = sprite_Textures.get(userData.textureIndex);

			bodySize.x = ratio * subtexture.width;
			bodySize.y = ratio * subtexture.height;

			float halfWidth = bodySize.x / 2;
			float halfHeight = bodySize.y / 2;

			GLCommon gl = null;
			if (Gdx.graphics.isGL20Available())
				gl = Gdx.gl20;
			else
				gl = Gdx.gl10;

			// GL10 gl = Gdx.gl10;

			gl.glActiveTexture(GL10.GL_TEXTURE0 + 1);
			gl.glDisable(GL10.GL_TEXTURE_2D);
			gl.glActiveTexture(GL10.GL_TEXTURE0 + 0);
			gl.glDisable(GL10.GL_TEXTURE_2D);

			gl.glDisable(GL10.GL_BLEND);
			gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);

			if (!Gdx.graphics.isGL20Available()) {
				Gdx.gl10.glColor4f(1, 1, 1, 1);
			}
			gl.glLineWidth(4);

			// Vector2 position = body.getPosition();
			// float angle = body.getAngle();

			Array<Vector2> listVecs = sprite_OutlineVertices
					.get(userData.textureIndex);

			shapeRenderer.setProjectionMatrix(cameraSCENE.combined);
			shaderMatrix.idt();
			shapeRenderer.setTransformMatrix(shaderMatrix);
			shapeRenderer.begin(GL10.GL_LINE_LOOP);
			shapeRenderer.setColor(0, 0, 1, 1); // blue

			for (int j = 0; j < listVecs.size; j++) {
				Vector2 vv = listVecs.get(j);
				tempRenderVector2.set(vv);
				tempRenderVector2.x = ratio * tempRenderVector2.x - halfWidth;
				tempRenderVector2.y = halfHeight - ratio * tempRenderVector2.y;
				body.getTransform().mul(tempRenderVector2);

				shapeRenderer.point(tempRenderVector2.x, tempRenderVector2.y,
						userData.zCoord);
			}

			shapeRenderer.end();

			// gl.glDisable(GL10.GL_BLEND);
			break;
		}
	}

	void render_SCENE_immediateRenderer_spriteBody_Selected_Outline(Body body) {
		float ratio = SPRITES_SCALE_FACTOR / PIXEL_TO_WORLD_RATIO;
		Vector2 bodySize = new Vector2(0, 0);

		for (MouseJoint mouseJoint : mouseJoints) {
			if (mouseJoint == null)
				continue;

			Body b = mouseJoint.getBodyB();
			if (b != body)
				continue;

			BodyUserData userData = (BodyUserData) body.getUserData();

			SubTexture subtexture = sprite_Textures.get(userData.textureIndex);

			bodySize.x = ratio * subtexture.width;
			bodySize.y = ratio * subtexture.height;

			float halfWidth = bodySize.x / 2;
			float halfHeight = bodySize.y / 2;

			GLCommon gl = null;
			if (Gdx.graphics.isGL20Available())
				gl = Gdx.gl20;
			else
				gl = Gdx.gl10;

			// GL10 gl = Gdx.gl10;

			gl.glActiveTexture(GL10.GL_TEXTURE0 + 1);
			gl.glDisable(GL10.GL_TEXTURE_2D);
			gl.glActiveTexture(GL10.GL_TEXTURE0 + 0);
			gl.glDisable(GL10.GL_TEXTURE_2D);

			gl.glEnable(GL10.GL_BLEND);
			gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);

			if (!Gdx.graphics.isGL20Available()) {
				Gdx.gl10.glColor4f(1, 1, 1, 0.5f);
			}
			gl.glLineWidth(6);

			// Vector2 position = body.getPosition();
			// float angle = body.getAngle();

			Array<Vector2> listVecs = sprite_OutlineVertices
					.get(userData.textureIndex);

			shapeRenderer.setProjectionMatrix(cameraSCENE.combined);
			shaderMatrix.idt();
			shapeRenderer.setTransformMatrix(shaderMatrix);
			shapeRenderer.begin(GL10.GL_LINE_LOOP);
			shapeRenderer.setColor(0, 0, 0, 0.5f); // transparent black

			for (int j = 0; j < listVecs.size; j++) {
				Vector2 vv = listVecs.get(j);
				tempRenderVector2.set(vv);
				tempRenderVector2.x = ratio * tempRenderVector2.x - halfWidth;
				tempRenderVector2.y = halfHeight - ratio * tempRenderVector2.y;
				body.getTransform().mul(tempRenderVector2);
				// if (j == 0) {
				// tempRenderVector2_.x = tempRenderVector2.x;
				// tempRenderVector2_.y = tempRenderVector2.y;
				// }

				shapeRenderer.point(tempRenderVector2.x, tempRenderVector2.y,
						userData.zCoord);
			}
			//
			// immediateModeRenderer.color(0, 0, 0, 0.5f);
			// immediateModeRenderer.vertex(tempRenderVector2_.x,
			// tempRenderVector2_.y, userData.zCoord);
			shapeRenderer.end();

			// gl.glDisable(GL10.GL_BLEND);
			break;
		}
	}

	void render_SCENE_mesh_bookPages(long start) {

		GLCommon gl = null;
		if (Gdx.graphics.isGL20Available())
			gl = Gdx.gl20;
		else
			gl = Gdx.gl10;

		// GL10 gl = Gdx.gl10;

		if (pageCurler.autoAnimated) {
			float ellapsed = (start - lastTimePageAnimation) / 1000000000.0f; // seconds
			if (lastTimePageAnimation == -1 || ellapsed > (1 / 20)) // 20 fps
																	// cap
																	// (polling
																	// interval)
			{
				pageCurler.updateCurlMesh();

				lastTimePageAnimation = System.nanoTime();
			}
		}

		gl.glActiveTexture(GL10.GL_TEXTURE0 + 0);
		gl.glEnable(GL10.GL_TEXTURE_2D);

		gl.glDisable(GL10.GL_BLEND);
		gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);

		if (!Gdx.graphics.isGL20Available()) {
			Gdx.gl10.glColor4f(1, 1, 1, 1);
		}

		if (Gdx.graphics.isGL11Available() || Gdx.graphics.isGL20Available()) {

			gl.glActiveTexture(GL10.GL_TEXTURE0 + 1);
			gl.glEnable(GL10.GL_TEXTURE_2D);

			gl.glEnable(GL10.GL_BLEND);
			gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
		}

		if (Gdx.graphics.isGL20Available()) {
			shaderMatrix.set(cameraSCENE.combined);
			// shaderMatrix.translate(0, 0, pageCurler.zIndex);
			// shaderMatrix.scale(pageCurler.scale, pageCurler.scale,
			// pageCurler.scale);
		} else {
			// Gdx.gl10.glPushMatrix();
			// Gdx.gl10.glTranslatef(0, 0, pageCurler.zIndex);
			// Gdx.gl10.glScalef(pageCurler.scale, pageCurler.scale,
			// pageCurler.scale);
			// Gdx.gl10.glRotatef(pageCurler.rho, 0, 0, 1);
		}

		// final float width =
		// pageCurlerLeft.page.verticesOriginal[pageCurlerLeft.page.verticesOriginal.length
		// - 1].x
		// - pageCurlerLeft.page.verticesOriginal[0].x;
		// gl.glTranslatef(-width, 0, 0);

		if (pageCurlerLeft.currentAnimationTime != 1) {
			pageCurlerLeft.currentAnimationTime = 1f;
			pageCurlerLeft.updateCurlMesh();
		}

		// gl.glDisable(GL10.GL_DEPTH_TEST);
		// gl.glDisable(GL10.GL_CULL_FACE);

		gl.glEnable(GL10.GL_DEPTH_TEST);
		gl.glEnable(GL10.GL_CULL_FACE);

		boolean wireframe = renderDebug == DebugRender.Full;

		if (Math.toDegrees(pageCurler.rho) <= 175) {
			pageCurlerLeft.page.render(
					pagesTexture, // backgroundTexture_Parchemin,
					null, sprite_Textures.get(10), this, shaderMatrix,
					wireframe);
		}
		if (Math.toDegrees(pageCurler.rho) >= 5) {
			pageCurlerRight.page.render(
					pagesTexture, // backgroundTexture_Paper,
					// sprite_Textures.get(4),
					sprite_Textures.get(16), null, this, shaderMatrix,
					wireframe);
		}

		boolean adjustCoplanarOverlap = false; // instead, we remove the
												// left/right page when
												// appropriate.
		// && pageCurler.currentAnimationTime >= 0.45;
		if (adjustCoplanarOverlap) {
			float polyfactor = -1f; // +/- 0.1
			float polyunits = -1f; // +/- 1;

			gl.glEnable(GL10.GL_POLYGON_OFFSET_FILL);
			gl.glPolygonOffset(polyfactor, polyunits);
		}

		if (pageCurler.page.doublePageTexture) {
			pageCurler.page.render(backgroundTexture_Paper, pagesTexture, null,
					this, shaderMatrix, wireframe);
		} else {
			pageCurler.page
					.render(Gdx.graphics.isGL20Available()
							&& renderDebug == DebugRender.Medium ? frameBufferSubTexture
							: pagesTexture, // backgroundTexture_Paper,
					sprite_Textures.get(11), sprite_Textures.get(12)
					// page1Texture,
					// page2Texture
							, this, shaderMatrix, wireframe);
		}

		if (adjustCoplanarOverlap) {
			gl.glDisable(GL10.GL_POLYGON_OFFSET_FILL);
		}

		if (!Gdx.graphics.isGL20Available()) {
			// Gdx.gl10.glPopMatrix();
		}

		gl.glDisable(GL10.GL_CULL_FACE);
		gl.glDisable(GL10.GL_DEPTH_TEST);

		if (true) {
			// if (!Gdx.graphics.isGL20Available()) {
			// Gdx.gl10.glPointSize(3);
			// }
			gl.glLineWidth(3);

			shapeRenderer.setProjectionMatrix(cameraSCENE.combined);

			// POSITION

			shaderMatrix.idt();

			shaderMatrix
					.translate(pageCurler.page.left
							+ pageCurler.page.cylinderPosition.x,
							pageCurler.page.bottom
									+ pageCurler.page.cylinderPosition.y,
							pageCurler.page.cylinderRadius);

			shaderMatrix.rotate(
					0,
					0,
					1,
					(float) Math.toDegrees(pageCurler.page.cylinderAngle
							+ Math.PI / 4));

			shapeRenderer.setTransformMatrix(shaderMatrix);

			shapeRenderer.setColor(1, 1, 1, 1);

			shapeRenderer.begin(ShapeType.Line);

			float offset = (pageCurler.page.right - pageCurler.page.left) / 16;
			offset = pageCurler.page.cylinderRadius;

			shapeRenderer.line(-offset, -offset, pageCurler.zIndex, -offset,
					offset, pageCurler.zIndex);
			shapeRenderer.line(offset, -offset, pageCurler.zIndex, offset,
					offset, pageCurler.zIndex);
			shapeRenderer.line(-offset, offset, pageCurler.zIndex, offset,
					offset, pageCurler.zIndex);
			shapeRenderer.line(-offset, -offset, pageCurler.zIndex, offset,
					-offset, pageCurler.zIndex);
			shapeRenderer.end();

			// RADIUS

			shaderMatrix.idt();

			shaderMatrix
					.translate(pageCurler.page.left
							+ pageCurler.page.cylinderPosition.x,
							pageCurler.page.bottom
									+ pageCurler.page.cylinderPosition.y,
							pageCurler.page.cylinderRadius);

			shaderMatrix.rotate(
					0,
					0,
					1,
					(float) Math.toDegrees(pageCurler.page.cylinderAngle
							+ Math.PI / 4));

			shapeRenderer.setTransformMatrix(shaderMatrix);

			shapeRenderer.setColor(0, 1, 0, 1);

			shapeRenderer.begin(ShapeType.Line);
			shapeRenderer.line(0, 0, pageCurler.zIndex,
					-pageCurler.page.cylinderRadius,
					-pageCurler.page.cylinderRadius, pageCurler.zIndex);
			shapeRenderer.end();

			// DIRECTION

			shaderMatrix.idt();

			// shaderMatrix.translate(pageCurler.page.left,
			// pageCurler.page.bottom, pageCurler.page.cylinderRadius);
			//
			shaderMatrix
					.translate(pageCurler.page.left
							+ pageCurler.page.cylinderPosition.x,
							pageCurler.page.bottom
									+ pageCurler.page.cylinderPosition.y,
							pageCurler.page.cylinderRadius);

			shaderMatrix.rotate(
					0,
					0,
					1,
					(float) Math.toDegrees(pageCurler.page.cylinderAngle
							+ Math.PI / 4));

			shapeRenderer.setTransformMatrix(shaderMatrix);

			shapeRenderer.setColor(0, 0, 1, 1);

			shapeRenderer.begin(ShapeType.Line);
			float factor = 10f;
			shapeRenderer.line(0, 0, pageCurler.zIndex,
					pageCurler.page.cylinderDirection.x * factor,
					pageCurler.page.cylinderDirection.x * factor,
					pageCurler.zIndex);
			shapeRenderer.end();
		}
	}

	void render_SCENE_immediateRenderer_spriteBodies_Outline() {
		boolean debugRed = false;

		GLCommon gl = null;
		if (Gdx.graphics.isGL20Available())
			gl = Gdx.gl20;
		else
			gl = Gdx.gl10;

		// GL10 gl = Gdx.gl10;

		gl.glActiveTexture(GL10.GL_TEXTURE0 + 1);
		gl.glDisable(GL10.GL_TEXTURE_2D);
		gl.glActiveTexture(GL10.GL_TEXTURE0 + 0);
		gl.glDisable(GL10.GL_TEXTURE_2D);

		gl.glDisable(GL10.GL_BLEND);
		gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);

		if (!Gdx.graphics.isGL20Available()) {
			Gdx.gl10.glColor4f(1, 1, 1, 1);
		}

		if (Gdx.graphics.isGL20Available()) {

			gl.glActiveTexture(GL10.GL_TEXTURE0 + 0);
			gl.glEnable(GL10.GL_TEXTURE_2D);

			bind(pagesTexture);

			spriteBatch.begin();
			// spriteBatch.draw(pagesTexture.texture, 0, 0, 0, 0,
			// pagesTexture.texture.getWidth(),
			// pagesTexture.texture.getHeight());
			spriteBatch.draw(pagesTexture.texture,//
					0, // x
					0, // y
					(frameBuffer.getWidth() - 0) / 2, // originX
					(frameBuffer.getHeight() - 0) / 2, // originY
					frameBuffer.getWidth() - 0, // width
					frameBuffer.getHeight() - 0, // height
					1, 1, // scaleX, scaleY
					0, // rotation
					pagesTexture.left, // srcX
					pagesTexture.top, // srcY
					pagesTexture.width, // srcWidth
					pagesTexture.height, // srcHeight
					false, false // flipX, flipY
					);
			spriteBatch.end();

			gl.glActiveTexture(GL10.GL_TEXTURE0 + 0);
			gl.glDisable(GL10.GL_TEXTURE_2D);
		}

		float ratio = SPRITES_SCALE_FACTOR / PIXEL_TO_WORLD_RATIO;
		Vector2 bodySize = new Vector2(0, 0);
		// for (int i = 0; i < spriteBodies.size; i++) {
		// Body body = spriteBodies.get(i);
		for (Iterator<Body> iter = world.getBodies(); iter.hasNext();) {
			Body body = iter.next();
			if (body == groundBody || !body.isActive())
				continue; // set in render_spriteBatch_spriteBodies_Texture()

			debugRed = false;
			// int index = computeModuloSpriteRotationThing(i);
			BodyUserData userData = (BodyUserData) body.getUserData();
			int index = userData.textureIndex;
			SubTexture subtexture = sprite_Textures.get(index);
			bodySize.x = ratio * subtexture.width;
			bodySize.y = ratio * subtexture.height;
			float halfWidth = bodySize.x / 2;
			float halfHeight = bodySize.y / 2;
			Vector2 position = body.getPosition();
			float angle = body.getAngle();
			float minX = getWorldMinX();
			float maxX = getWorldMaxX();
			float minY = getWorldMinY();
			float maxY = getWorldMaxY();
			if (position.x > maxX) {
				debugRed = true;

				// // NOT ACTIVE (already filtered above)
				// if (position.x > maxX + bodySize.x) {
				// continue;
				// }
			}
			if (position.x < minX) {
				debugRed = true;

				// // NOT ACTIVE (already filtered above)
				// if (position.x < minX - bodySize.x) {
				// continue;
				// }
			}
			if (position.y > maxY) {
				debugRed = true;
			}
			if (position.y < minY) {
				debugRed = true;

				// // NOT ACTIVE (already filtered above)
				// if (position.y < -maxY) {
				// continue;
				// }
			}
			// gl.glPushMatrix();
			// gl.glLoadIdentity();
			// gl.glTranslatef(position.x, position.y, 0);
			// gl.glRotatef((float) Math.toDegrees(angle), 0, 0, 1);

			Array<Vector2> listVecs = sprite_OutlineVertices.get(index);

			gl.glLineWidth(3);

			shapeRenderer.setProjectionMatrix(cameraSCENE.combined);
			shaderMatrix.idt();
			shapeRenderer.setTransformMatrix(shaderMatrix);
			shapeRenderer.begin(GL10.GL_LINE_LOOP);

			if (debugRed)
				shapeRenderer.setColor(1, 0, 0, 1); // RED
			else
				shapeRenderer.setColor(0, 1, 0, 1); // GREEN

			for (int j = 0; j < listVecs.size; j++) {
				Vector2 vv = listVecs.get(j);
				tempRenderVector2.set(vv);
				tempRenderVector2.x = ratio * tempRenderVector2.x - halfWidth;
				tempRenderVector2.y = halfHeight - ratio * tempRenderVector2.y;
				body.getTransform().mul(tempRenderVector2);
				// if (j == 0) {
				// tempRenderVector2_.x = tempRenderVector2.x;
				// tempRenderVector2_.y = tempRenderVector2.y;
				// }
				shapeRenderer.point(tempRenderVector2.x, tempRenderVector2.y,
						userData.zCoord);
			}
			// if (debugRed)
			// immediateModeRenderer.color(1, 0, 0, 1); // RED
			// else
			// immediateModeRenderer.color(0, 1, 0, 1); // GREEN
			// immediateModeRenderer.vertex(tempRenderVector2_.x,
			// tempRenderVector2_.y, userData.zCoord);
			shapeRenderer.end();

			// gl.glPopMatrix();
		}
	}

	void render_SCENE_mesh_spriteBodies_Texture() {
		boolean debugRed = false;
		float ratio = SPRITES_SCALE_FACTOR / PIXEL_TO_WORLD_RATIO;
		Vector2 bodySize = new Vector2(0, 0);

		// for (int i = 0; i < spriteBodies.size; i++) {
		// Body body = spriteBodies.get(i);

		for (int layer = 0; layer < 2; layer++) {
			int i = -1;
			for (Iterator<Body> iter = world.getBodies(); iter.hasNext();) {
				Body body = iter.next();
				i++;

				if (body == groundBody || !body.isActive())
					continue;

				BodyUserData userData = (BodyUserData) body.getUserData();
				//
				// short l = 0x0000; // wall = 0x0002 (otherwise 0x0001)
				// ArrayList<Fixture> fixtures = body.getFixtureList();
				// for (Fixture fixture : fixtures) {
				// l = fixture.getFilterData().categoryBits;
				// break;
				// }
				if (layer == 0 && userData.zCoord != zCoord_box2D_) // l !=
																	// 0x0002)
					continue;

				if (layer == 1 && userData.zCoord != zCoord_box2D) // l !=
																	// 0x0001)
					continue;

				debugRed = false;

				// int index = computeModuloSpriteRotationThing(i);
				int index = userData.textureIndex;
				SubTexture subtexture = sprite_Textures.get(index);
				bodySize.x = ratio * subtexture.width;
				bodySize.y = ratio * subtexture.height;
				Vector2 position = body.getPosition();

				float minX = getWorldMinX();
				float maxX = getWorldMaxX();
				float minY = getWorldMinY();
				float maxY = getWorldMaxY();
				if (position.x > maxX) {
					debugRed = true;
					// Gdx.app.log(APP_NAME, "position.x > worldSize.x/2 ==> " +
					// position.x);
					if (position.x > maxX + bodySize.x) {
						Gdx.app.log(APP_NAME,
								"Deactivating sprite body (lost right): " + i);
						killSprite(body);
						continue;
					}
				}
				if (position.x < minX) {
					debugRed = true;
					// /Gdx.app.log(APP_NAME, "position.x < worldSize.x/2 ==> "
					// +
					// position.x);
					if (position.x < minX - bodySize.x) {
						Gdx.app.log(APP_NAME,
								"Deactivating sprite body (lost left): " + i);
						killSprite(body);
						continue;
					}
				}
				if (position.y > maxY) {
					debugRed = true;
					// Gdx.app.log(APP_NAME, "position.y > worldSize.y ==> " +
					// position.y);
				}
				if (position.y < minY) {
					debugRed = true;
					// Gdx.app.log(APP_NAME, "position.y < 0 ==> " +
					// position.y);
					if (position.y < -maxY) {
						Gdx.app.log(APP_NAME,
								"Deactivating sprite body (lost bottom): " + i);
						killSprite(body);
						continue;
					}
				}

				float angle = body.getAngle();

				// float colorOverlay = Color.WHITE.toFloatBits();
				Color colorOverlay = Color.WHITE;

				if (renderDebug.ordinal() >= DebugRender.Medium.ordinal())
					if (debugRed) {
						colorOverlay = Color.RED; // getColorFloat(1, 0, 0,
													// 0.58f);
					} else { // if (body.isBullet()) {
						for (MouseJoint mouseJoint : mouseJoints) {
							if (mouseJoint != null
									&& mouseJoint.getBodyB() == body) {
								colorOverlay = Color.BLUE; // getColorFloat(0,
															// 0, 1,
															// 0.58f);
								break;
							}
						}
					}

				Mesh mesh = sprite_TexturesMeshes.get(index);

				GLCommon gl = null;
				if (Gdx.graphics.isGL20Available())
					gl = Gdx.gl20;
				else
					gl = Gdx.gl10;

				// GL10 gl = Gdx.gl10;

				gl.glActiveTexture(GL10.GL_TEXTURE0 + 0);
				gl.glEnable(GL10.GL_TEXTURE_2D);

				gl.glEnable(GL10.GL_BLEND);
				gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);

				if (!Gdx.graphics.isGL20Available()) {
					Gdx.gl10.glColor4f(1, 1, 1, 1);
				}

				// if (renderDebug.ordinal() >= DebugRender.Medium.ordinal()) {
				// }

				if (Gdx.graphics.isGL20Available()) {
					shaderMatrix.set(cameraSCENE.combined);
					shaderMatrix.translate(position.x, position.y,
							userData.zCoord);
					shaderMatrix.rotate(0, 0, 1, (float) Math.toDegrees(angle));
				} else {
					Gdx.gl10.glPushMatrix();
					// gl.glLoadIdentity();

					Gdx.gl10.glTranslatef(position.x, position.y,
							userData.zCoord);
					Gdx.gl10.glRotatef((float) Math.toDegrees(angle), 0, 0, 1);
				}

				bind(subtexture);

				if (Gdx.graphics.isGL20Available()) {

					shader_POSITION_TEX1.begin();
					shader_POSITION_TEX1.setUniformMatrix(
							ShaderProgram_PROJECTION_VIEW_MATRIX_UNIFORM,
							shaderMatrix);
					shader_POSITION_TEX1.setUniformi(
							ShaderProgram_TEXTURE_SAMPLER_UNIFORM + 0, 0);
					shader_POSITION_TEX1
							.setUniformf(
									LibGDXPlayground.ShaderProgram_VERTEX_COLOR_UNIFORM,
									colorOverlay.r, colorOverlay.g,
									colorOverlay.b, colorOverlay.a);
					//
					// shader_POSITION_TEX1
					// .setUniformf(
					// LibGDXPlayground.ShaderProgram_VERTEX_COLOR_UNIFORM,
					// 0, 0, 0, 0);

					mesh.render(shader_POSITION_TEX1, GL10.GL_TRIANGLE_FAN);

					shader_POSITION_TEX1.end();
				} else {
					if (colorOverlay != Color.WHITE) {
						Gdx.gl10.glColor4f(colorOverlay.r, colorOverlay.g,
								colorOverlay.b, colorOverlay.a);
					}

					mesh.render(GL10.GL_TRIANGLE_FAN);
				}

				if (!Gdx.graphics.isGL20Available()) {
					Gdx.gl10.glPopMatrix();
				}

				//
				// if (renderDebug.ordinal() >= DebugRender.Medium.ordinal())
				// spriteBatch.setColor(colorOverlay);
				// else
				// spriteBatch.setColor(1, 1, 1, 1);
				//
				// spriteBatch.draw(texture,//
				// position.x - bodySize.x / 2, // x
				// position.y - bodySize.y / 2, // y
				// bodySize.x / 2, // originX
				// bodySize.y / 2, // originY
				// bodySize.x, // width
				// bodySize.y, // height
				// 1, 1, // scaleX, scaleY
				// (float) Math.toDegrees(angle), // rotation
				// 0, 0, // srcX, srcY
				// texture.getWidth(), // srcWidth
				// texture.getHeight(), // srcHeight
				// false, false // flipX, flipY
				// );
				//
				// // spriteBatch.draw(textureRegion, position.x - bodySize.x /
				// 2,
				// // position.y - bodySize.y / 2, bodySize.x / 2,
				// // bodySize.y / 2, bodySize.x, bodySize.y, 1, 1,
				// // (float) Math.toDegrees(angle));

				render_SCENE_immediateRenderer_spriteBody_Selected_Outline(body);

				if (renderDebug.ordinal() >= DebugRender.None.ordinal()) {
					render_SCENE_immediateRenderer_spriteBody_Destroying_Outline(body);
				}
			}
		}
	}

	void render_SCENE_spriteBatch_worldBackgroundTextOverlay() {

		float minX = getWorldMinX();
		float maxX = getWorldMaxX();
		float minY = getWorldMinY();
		float maxY = getWorldMaxY();

		float ratio = PIXEL_TO_WORLD_RATIO; // + PIXEL_TO_WORLD_RATIO/2;

		TextBounds bounds = fontCaches.get(currentFont).getBounds();
		float textWorldWidth = bounds.width / ratio;
		float textWorldHeight = bounds.height / ratio;

		GLCommon gl = null;
		if (Gdx.graphics.isGL20Available())
			gl = Gdx.gl20;
		else
			gl = Gdx.gl10;

		// GL10 gl = Gdx.gl10;
		//
		// gl.glActiveTexture(GL10.GL_TEXTURE0 + 0);
		// gl.glEnable(GL10.GL_TEXTURE_2D);
		//
		// gl.glEnable(GL10.GL_BLEND);
		// gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
		//
		// if (!Gdx.graphics.isGL20Available()) {
		// Gdx.gl10.glColor4f(1, 1, 1, 1);
		// }

		if (Gdx.graphics.isGL20Available()) {
			shaderMatrix.idt();
			shaderMatrix.translate(minX + (maxX - minX - textWorldWidth) / 2,//
					maxY - (maxY - minY - textWorldHeight) / 2, //
					zCoord_Background);
			shaderMatrix.scale(1.0f / ratio, 1.0f / ratio, 1f);
			spriteBatch.setTransformMatrix(shaderMatrix);
		} else {
			Gdx.gl10.glPushMatrix();
			Gdx.gl10.glTranslatef(minX + (maxX - minX - textWorldWidth) / 2,//
					maxY - (maxY - minY - textWorldHeight) / 2, //
					zCoord_Background);
			Gdx.gl10.glScalef(1.0f / ratio, 1.0f / ratio, 1f);
		}

		// fontCache1.translate(xAmount, yAmount);
		// fontCaches.get(currentFont).getFont().setColor(Color.BLACK);
		// spriteBatch.setColor(Color.BLACK);
		fontCaches.get(currentFont).draw(spriteBatch);

		// /// spriteBatch.setColor(1, 1, 1, 1);
		// font.setColor(Color.RED);
		// font.scale(1.0f);
		// font.draw(spriteBatch, "TESTING", PIXEL_TO_WORLD_RATIO * minX,
		// PIXEL_TO_WORLD_RATIO * (minY + worldSize.y / 2));
	}

	void render_SCENE_spriteBatch_worldBackground() {

		SubTexture texture;
		if (renderDebug == DebugRender.None)
			texture = backgroundTexture_Aqua;
		else if (renderDebug == DebugRender.Medium)
			texture = backgroundTexture_Parchemin;
		else
			// DebugRender.Full
			texture = backgroundTexture_Paper;

		GLCommon gl = null;
		if (Gdx.graphics.isGL20Available())
			gl = Gdx.gl20;
		else
			gl = Gdx.gl10;

		// GL10 gl = Gdx.gl10;
		//
		// gl.glActiveTexture(GL10.GL_TEXTURE0 + 0);
		// gl.glEnable(GL10.GL_TEXTURE_2D);
		//
		// gl.glDisable(GL10.GL_BLEND);
		// gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
		//
		// if (!Gdx.graphics.isGL20Available()) {
		// Gdx.gl10.glColor4f(1, 1, 1, 1);
		// }

		if (Gdx.graphics.isGL20Available()) {
			shaderMatrix.idt();
			shaderMatrix.translate(0, 0, zCoord_Background);
			spriteBatch.setTransformMatrix(shaderMatrix);
		} else {
			Gdx.gl10.glPushMatrix();
			Gdx.gl10.glTranslatef(0, 0, zCoord_Background);
		}

		float minX = getWorldMinX();
		float maxX = getWorldMaxX();
		float minY = getWorldMinY();
		float maxY = getWorldMaxY();

		// if (cameraSCENEPerspective != null) {
		// minX *= 1.5;
		// maxX *= 1.5;
		// minY *= 1.5;
		// maxY *= 1.5;
		// }
		spriteBatch.setColor(1, 1, 1, 1);
		spriteBatch.draw(texture.texture,//
				minX, // x
				minY, // y
				(maxX - minX) / 2, // originX
				(maxY - minY) / 2, // originY
				maxX - minX, // width
				maxY - minY, // height
				1, 1, // scaleX, scaleY
				0, // rotation
				texture.left, // srcX
				texture.top, // srcY
				texture.width, // srcWidth
				texture.height, // srcHeight
				false, false // flipX, flipY
				);
	}

	void render_SCENE_mesh_worldBackground() {

		// SubTexture texture;
		// if (renderDebug == DebugRender.None)
		// texture = backgroundTexture_Aqua;
		// else if (renderDebug == DebugRender.Medium)
		// texture = backgroundTexture_Parchemin;
		// else
		// // DebugRender.Full
		// texture = backgroundTexture_Paper;

		GLCommon gl = null;
		if (Gdx.graphics.isGL20Available())
			gl = Gdx.gl20;
		else
			gl = Gdx.gl10;

		// GL10 gl = Gdx.gl10;

		gl.glActiveTexture(GL10.GL_TEXTURE0 + 0);
		gl.glEnable(GL10.GL_TEXTURE_2D);

		if (renderDebug == DebugRender.None) {
			if (usePageTurn) {
				bind(pageTurn.subtexture);
				pageTurn.render();
			}
		} else {
			bind(waterRipples.subtexture);
			waterRipples.render(false);
		}
	}

	void render_SCREEN_mesh_worldRepeatBackground() {

		GLCommon gl = null;
		if (Gdx.graphics.isGL20Available())
			gl = Gdx.gl20;
		else
			gl = Gdx.gl10;

		// GL10 gl = Gdx.gl10;

		gl.glActiveTexture(GL10.GL_TEXTURE0 + 0);
		gl.glEnable(GL10.GL_TEXTURE_2D);

		gl.glDisable(GL10.GL_BLEND);
		gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);

		if (!Gdx.graphics.isGL20Available()) {
			Gdx.gl10.glColor4f(1, 1, 1, 1);
		}

		// gl.glPushMatrix();

		// gl.glLineWidth(4 * (cameraSCENE instanceof OrthographicCamera ?
		// MT_scale
		// : 1)); // camera.zoom

		shaderMatrix.set(cameraSCREEN.combined);

		bind(backgroundTexture_Repeat);

		if (Gdx.graphics.isGL20Available()) {

			shader_POSITION_TEX1.begin();
			shader_POSITION_TEX1.setUniformMatrix(
					ShaderProgram_PROJECTION_VIEW_MATRIX_UNIFORM, shaderMatrix);
			shader_POSITION_TEX1.setUniformf(
					LibGDXPlayground.ShaderProgram_VERTEX_COLOR_UNIFORM, 1, 1,
					1, 1);
			shader_POSITION_TEX1.setUniformi(
					ShaderProgram_TEXTURE_SAMPLER_UNIFORM + 0, 0);

			worldBackgroundRepeatMesh.render(shader_POSITION_TEX1,
					GL10.GL_TRIANGLE_STRIP);

			shader_POSITION_TEX1.end();
		} else {
			worldBackgroundRepeatMesh.render(GL10.GL_TRIANGLE_STRIP);
		}

		// //
		// renderer.begin(GL10.GL_LINE_STRIP);
		// renderer.color(1, 0, 0, 1); // RED
		// renderer.vertex(minX, minY, zPlane);
		// renderer.color(1, 0, 0, 1); // RED
		// renderer.vertex(maxX, minY, zPlane);
		// renderer.color(1, 0, 0, 1); // RED
		// renderer.vertex(maxX, maxY, zPlane);
		// renderer.color(1, 0, 0, 1); // RED
		// renderer.vertex(minX, maxY, zPlane);
		// renderer.color(1, 0, 0, 1); // RED
		// renderer.vertex(minX, minY, zPlane);
		// renderer.end();
		// //
		// gl.glLineWidth(1);
		// gl.glPopMatrix();
	}

	void render_SCENE_mesh_groundTextureFill() {
		if (groundBody == null || groundTexture == null || groundMesh == null)
			return;

		Vector2 pos = groundBody.getPosition();
		float angle = groundBody.getAngle();
		// Transform transform = groundBody.getTransform();

		GLCommon gl = null;
		if (Gdx.graphics.isGL20Available())
			gl = Gdx.gl20;
		else
			gl = Gdx.gl10;

		// GL10 gl = Gdx.gl10;

		gl.glActiveTexture(GL10.GL_TEXTURE0 + 0);
		gl.glEnable(GL10.GL_TEXTURE_2D);

		gl.glDisable(GL10.GL_BLEND);
		gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);

		if (!Gdx.graphics.isGL20Available()) {
			Gdx.gl10.glColor4f(1, 1, 1, 1);
		}

		if (Gdx.graphics.isGL20Available()) {
			shaderMatrix.set(cameraSCENE.combined);
			shaderMatrix.translate(pos.x, pos.y, 0);
			shaderMatrix.rotate(0, 0, 1, (float) Math.toDegrees(angle));
		} else {
			Gdx.gl10.glPushMatrix();
			Gdx.gl10.glTranslatef(pos.x, pos.y, 0);
			Gdx.gl10.glRotatef((float) Math.toDegrees(angle), 0, 0, 1);
		}

		bind(groundTexture);

		if (Gdx.graphics.isGL20Available()) {

			shader_POSITION_COLOR_TEX1.begin();
			shader_POSITION_COLOR_TEX1.setUniformMatrix(
					ShaderProgram_PROJECTION_VIEW_MATRIX_UNIFORM, shaderMatrix);
			shader_POSITION_COLOR_TEX1.setUniformf(
					LibGDXPlayground.ShaderProgram_VERTEX_COLOR_UNIFORM, 0, 0,
					0, 0);
			shader_POSITION_COLOR_TEX1.setUniformi(
					ShaderProgram_TEXTURE_SAMPLER_UNIFORM + 0, 0);

			groundMesh.render(shader_POSITION_COLOR_TEX1,
					GL10.GL_TRIANGLE_STRIP);

			shader_POSITION_COLOR_TEX1.end();
		} else {
			groundMesh.render(GL10.GL_TRIANGLE_STRIP);
		}

		if (!Gdx.graphics.isGL20Available()) {
			Gdx.gl10.glPopMatrix();
		}
		//
		// if (renderDebug.ordinal() < DebugRender.Medium.ordinal()) {
		// gl.glPopMatrix();
		// return;
		// }
		// float halfWidth = (worldSize.x / GROUND_SIZE_FRACTION.x) / 2;//
		// (meters)
		// float halfHeight = (worldSize.y / GROUND_SIZE_FRACTION.y) / 2;//
		// (meters)
		// gl.glLineWidth(4 * (cameraSCENE instanceof OrthographicCamera ?
		// MT_scale
		// : 1)); // camera.zoom
		// if (!Gdx.graphics.isGL20Available()) {
		// Gdx.gl10.glColor4f(1, 1, 1, 1);
		// }
		// groundBorderMesh.render(shader, GL10.GL_LINE_STRIP);
		// //
		//
		// renderer.begin(GL10.GL_LINE_STRIP);
		// renderer.color(1, 1, 1, 1);
		// renderer.vertex(-halfWidth, -halfHeight, zPlane);
		// renderer.color(1, 1, 1, 1); // WHITE
		// renderer.vertex(-halfWidth, halfHeight, zPlane);
		// renderer.color(1, 1, 1, 1); // WHITE
		// renderer.vertex(halfWidth, halfHeight, zPlane);
		// renderer.color(1, 1, 1, 1); // WHITE
		// renderer.vertex(halfWidth, -halfHeight, zPlane);
		// renderer.color(1, 1, 1, 1); // WHITE
		// renderer.vertex(-halfWidth, -halfHeight, zPlane);
		// renderer.end();
		// //
		// gl.glLineWidth(1);
		// gl.glPopMatrix();
	}

	// void render_SCREEN_immediateRenderer_bouncingText_Borders() {
	//
	// float halfWidth = bouncingText_Bounds.width / 2;
	// float halfHeight = bouncingText_Bounds.height / 2;
	// GL10 gl = Gdx.gl10;
	// gl.glDisable(GL10.GL_BLEND);
	// gl.glPushMatrix();
	// gl.glTranslatef(bouncingText_Position.x + halfWidth,
	// bouncingText_Position.y - halfHeight, 0);
	// // gl.glRotatef((float) 0, 0, 0, 1);
	// immediateModeRenderer.begin(GL10.GL_LINE_STRIP);
	// immediateModeRenderer.color(1, 1, 1, 1);
	// immediateModeRenderer.vertex(-halfWidth, -halfHeight, box2D_zCoord);
	// immediateModeRenderer.color(1, 1, 1, 1); // WHITE
	// immediateModeRenderer.vertex(-halfWidth, halfHeight, box2D_zCoord);
	// immediateModeRenderer.color(1, 0, 0, 1); // RED
	// immediateModeRenderer.vertex(halfWidth, halfHeight, box2D_zCoord);
	// immediateModeRenderer.color(0, 1, 0, 1); // GREEN
	// immediateModeRenderer.vertex(halfWidth, -halfHeight, box2D_zCoord);
	// immediateModeRenderer.color(0, 0, 1, 1); // BLUE
	// immediateModeRenderer.vertex(-halfWidth, -halfHeight, box2D_zCoord);
	// immediateModeRenderer.end();
	// gl.glPopMatrix();
	// }
	//
	// void render_SCREEN_spriteBatch_bouncingText_Font() {
	// int screenWidth = Gdx.graphics.getWidth();
	// int screenHeight = Gdx.graphics.getHeight();
	//
	// //
	// textPosition.add(textDirection.tmp().mul(Gdx.graphics.getDeltaTime()).mul(60));
	// bouncingText_Position.x += bouncingText_Direction.x
	// * Gdx.graphics.getDeltaTime() * 200;
	// bouncingText_Position.y += bouncingText_Direction.y
	// * Gdx.graphics.getDeltaTime() * 200;
	// boolean collided = false;
	// if (bouncingText_Position.x < 0) {
	// bouncingText_Direction.x = -bouncingText_Direction.x;
	// bouncingText_Position.x = 0;
	// collided = true;
	// }
	// int maxX = screenWidth - bouncingText_Bounds.width;
	// if (bouncingText_Position.x > maxX) {
	// bouncingText_Direction.x = -bouncingText_Direction.x;
	// bouncingText_Position.x = maxX;
	// collided = true;
	// }
	// if (bouncingText_Position.y < bouncingText_Bounds.height) {
	// bouncingText_Direction.y = -bouncingText_Direction.y;
	// bouncingText_Position.y = bouncingText_Bounds.height;
	// collided = true;
	// }
	// if (bouncingText_Position.y > screenHeight) {
	// bouncingText_Direction.y = -bouncingText_Direction.y;
	// bouncingText_Position.y = screenHeight;
	// collided = true;
	// }
	// if (collided) {
	// // Gdx.input.vibrate(500);
	// }
	//
	// font.setColor(Color.WHITE);
	//
	// font.draw(spriteBatch, bouncingText_String,
	// (int) bouncingText_Position.x, (int) bouncingText_Position.y);
	//
	// }

	void initWorld() {

		pageCurler = new PageCurler(shader_POSITION_COLOR_TEX2,
				shader_POSITION_TEX2, cameraSCENE, zCoord_Background, false); // zCoord_box2D);
		pageCurlerLeft = new PageCurler(shader_POSITION_COLOR_TEX2,
				shader_POSITION_TEX2, cameraSCENE, zCoord_Background, true); // zCoord_box2D);
		pageCurlerRight = new PageCurler(shader_POSITION_COLOR_TEX2,
				shader_POSITION_TEX2, cameraSCENE, zCoord_Background, true); // zCoord_box2D);

		compassMesh = new Mesh(true, 3, 3, new VertexAttribute(Usage.Position,
				3, ShaderProgram.POSITION_ATTRIBUTE), new VertexAttribute(
				Usage.ColorPacked, 4, ShaderProgram.COLOR_ATTRIBUTE));

		// compassMesh.render(shader, GL10.GL_TRIANGLES)
		compassMesh.setVertices(new float[] { //
				-50, -100, zCoord_box2D, //
						Color.toFloatBits(255, 255, 255, 100), //
						50, -100, zCoord_box2D, //
						Color.toFloatBits(255, 0, 0, 100), //
						-0, 100, zCoord_box2D, //
						Color.toFloatBits(0, 255, 0, 100) //
				});
		compassMesh.setIndices(new short[] { 0, 1, 2 });
		//
		// float minX = getWorldMinX();
		// float maxX = getWorldMaxX();
		// float minY = getWorldMinY();
		// float maxY = getWorldMaxY();
		//
		// if (cameraSCENEPerspective != null) {
		// minX *= 1.5;
		// maxX *= 1.5;
		// minY *= 1.5;
		// maxY *= 1.5;
		// }
		// float z = box2D_zCoord + box2D_zCoord / 4;
		//
		worldBackgroundRepeatMesh = new Mesh(true, 4, 4, new VertexAttribute(
				Usage.Position, 3, ShaderProgram.POSITION_ATTRIBUTE),
				new VertexAttribute(Usage.TextureCoordinates, 2,
						ShaderProgram.TEXCOORD_ATTRIBUTE + "0"));

		int width = Gdx.graphics.getWidth();
		int height = Gdx.graphics.getHeight();
		int edge = Math.max(width, height);
		int nRepeat = 5;

		// worldBackgroundRepeatMesh.render(shader, GL10.GL_TRIANGLE_STRIP)
		worldBackgroundRepeatMesh.setVertices(new float[] { //
				0, 0, 0, //
						0, nRepeat, //
						edge, 0, 0, //
						nRepeat, nRepeat, //
						0, edge, 0, //
						0, 0, //
						edge, edge, 0, //
						nRepeat, 0 //
				});
		worldBackgroundRepeatMesh.setIndices(new short[] { 0, 1, 2, 3 });
		//
		initWorld_ground();
		initWorld_sprites();
	}

	void clearWorld() {

		bodiesToDestroy.clear();

		for (MouseJoint mouseJoint : mouseJoints)
			if (mouseJoint != null)
				world.destroyJoint(mouseJoint);
		mouseJoints.clear();
		for (int i = 0; i < 10; i++)
			mouseJoints.add(null);
		//
		// for (Body body : spriteBodies)
		// if (body != null)
		for (Iterator<Body> iter = world.getBodies(); iter.hasNext();) {
			Body body = iter.next();
			// iter.remove();
			destroyBody(body);
		}
		// spriteBodies.clear();
		//
		// if (groundBody != null)
		// destroyBody(groundBody);
		groundBody = null;
		//
		while (sprite_BodyPolygons.size > sprite_ImagePaths.size)
			sprite_BodyPolygons.removeIndex(sprite_ImagePaths.size);
		//
		while (sprite_OutlineVertices.size > sprite_ImagePaths.size)
			sprite_OutlineVertices.removeIndex(sprite_ImagePaths.size);
		//
		while (sprite_Textures.size > sprite_ImagePaths.size) {
			SubTexture subtex = sprite_Textures
					.removeIndex(sprite_ImagePaths.size);
			if (subtex != null) {
				// NO !! (textures are shared, even for broken bodies)
				// tex.dispose();
			}
		}
		//
		while (sprite_TexturesMeshes.size > sprite_ImagePaths.size) {
			Mesh mesh = sprite_TexturesMeshes
					.removeIndex(sprite_ImagePaths.size);
			if (mesh != null) {
				mesh.dispose();
			}
		}
	}

	void resetWorldSize(int screenWidth, int screenHeight) {
		worldSize.x = screenWidth / PIXEL_TO_WORLD_RATIO;
		worldSize.y = screenHeight / PIXEL_TO_WORLD_RATIO;
	}

	Vector3 camera_direction_original;
	Vector3 camera_up_original;

	void resetSCENECamera(float width, float height, float factor, float angle,
			float positionX, float positionY, float positionZ) {
		//
		// INIT BACKUP
		if (camera_direction_original == null) {
			camera_direction_original = new Vector3();
			camera_direction_original.x = cameraSCENE.direction.x;
			camera_direction_original.y = cameraSCENE.direction.y;
			camera_direction_original.z = cameraSCENE.direction.z;
		}
		if (camera_up_original == null) {
			camera_up_original = new Vector3();
			camera_up_original.x = cameraSCENE.up.x;
			camera_up_original.y = cameraSCENE.up.y;
			camera_up_original.z = cameraSCENE.up.z;
		}
		//
		// RESET WITH BACKUP
		cameraSCENE.direction.x = camera_direction_original.x;// 0
		cameraSCENE.direction.y = camera_direction_original.y;// 0
		cameraSCENE.direction.z = camera_direction_original.z;// -1
		cameraSCENE.up.x = camera_up_original.x;// 0
		cameraSCENE.up.y = camera_up_original.y;// 1
		cameraSCENE.up.z = camera_up_original.z;// 0
		cameraSCENE.position.set(0, 0, 0);
		//
		// ADJUST
		float viewportWidthUnscaled = width
				+ (Gdx.app.getType() == ApplicationType.Android ? 20 / PIXEL_TO_WORLD_RATIO
						: 0);
		float viewportHeightUnscaled = height;
		//
		// ASSIGN
		MT_scale = factor;
		MT_angle = angle;
		MT_offset.x = positionX;
		MT_offset.y = positionY;
		MT_offset.z = positionZ;
		//
		// TRANSLATE
		if (MT_offset.x == Float.MIN_VALUE || MT_offset.y == Float.MIN_VALUE
				|| MT_offset.z == Float.MIN_VALUE) {
			// remain centered on the horizontal axis (shows negative and
			// positive X coordinates)
			float worldOffsetX = 0; // viewportWidthUnscaled / 2;
			// shift up on the vertical axis, to show only positive Y
			// coordinates
			float worldOffsetY = viewportHeightUnscaled / 2
					- ((viewportHeightUnscaled / GROUND_SIZE_FRACTION.y) / 2);
			MT_offset.x = -worldOffsetX * PIXEL_TO_WORLD_RATIO;
			MT_offset.y = worldOffsetY * PIXEL_TO_WORLD_RATIO;
			MT_offset.z = 0;
			cameraSCENE.position.set(worldOffsetX / MT_scale, worldOffsetY
					/ MT_scale, MT_offset.z);
		} else {
			float thisx = -MT_offset.x / PIXEL_TO_WORLD_RATIO;
			float thisy = MT_offset.y / PIXEL_TO_WORLD_RATIO;
			float thisz = MT_offset.z / PIXEL_TO_WORLD_RATIO;

			if (cameraSCENEPerspective != null) {
				// temporaryPointForProjection.x = -MT_offset.x;
				// temporaryPointForProjection.y = MT_offset.y;
				// temporaryPointForProjection.z = 0; // near plane
				// cameraSCENE.unproject(temporaryPointForProjection);
				//
				// temporaryPointUnproject((int) MT_offset.x, (int)
				// MT_offset.y);
				// thisx = -temporaryPointForProjection.x;
				// thisy = temporaryPointForProjection.y;
			}

			// rotate-a-point-by-an-angle
			// p'x = cos(theta) * (px-originx) - sin(theta) * (py-originy) +
			// originx
			// p'y = sin(theta) * (px-originx) + cos(theta) * (py-originy) +
			// originy

			// ACCOUNT FOR ROTATION
			float rx = (thisx * (float) Math.cos(MT_angle))
					- (thisy * (float) Math.sin(MT_angle));
			float ry = (thisx * (float) Math.sin(MT_angle))
					+ (thisy * (float) Math.cos(MT_angle));
			cameraSCENE.position.set(rx / MT_scale, ry / MT_scale, thisz);
			//
			// Vector3 offset = new Vector3(-MT_offset.x, MT_offset.y, zPlane);
			// camera.unproject(offset);
			// camera.position.set(offset.x/ MT_scale, offset.y/ MT_scale, 0);
		}
		//
		//
		// ROTATE
		if (MT_angle == Float.MIN_VALUE) {
			MT_angle = 0;
		} else {
			// float angleDegrees = (float) Math.toDegrees(MT_angle);
			// float axisX = 0, axisY = 0, axisZ = 1;
			// camera.rotate(angleDegrees, axisX, axisY, axisZ);
			//
			// Matrix4 tmpMat = new Matrix4();
			// Vector3 tmpVec = new Vector3();
			// tmpMat.setToRotation(tmpVec.set(axisX, axisY, axisZ),
			// angleDegrees);
			// camera.direction.mul(tmpMat).nor();
			// camera.up.mul(tmpMat).nor();
			//
			// Vector3 center = new Vector3(thumbpad.centre.x,
			// thumbpad.centre.y, zPlane);
			// camera.unproject(center);
			// camera.up.add(-center.x,-center.y, 0);
			//
			float thisx = cameraSCENE.up.x;
			float thisy = cameraSCENE.up.y;
			float rx = (thisx * (float) Math.cos(MT_angle))
					- (thisy * (float) Math.sin(MT_angle));
			float ry = (thisx * (float) Math.sin(MT_angle))
					+ (thisy * (float) Math.cos(MT_angle));
			cameraSCENE.up.set(rx / MT_scale, ry / MT_scale, 0).nor();
			//
		}
		//
		// ZOOM
		if (cameraSCENEOrthographic != null) {
			cameraSCENE.viewportWidth = viewportWidthUnscaled; // / MT_scale;
			cameraSCENE.viewportHeight = viewportHeightUnscaled; // / MT_scale;
			cameraSCENEOrthographic.zoom = 1 / MT_scale;
		}

		if (cameraSCENEPerspective != null) {
			cameraSCENE.viewportWidth = viewportWidthUnscaled; // / MT_scale;
			cameraSCENE.viewportHeight = viewportHeightUnscaled; // / MT_scale;
			// cameraSCENEPerspective.translate(0, -viewportHeightUnscaled
			// * (1 - MT_scale), 50 * (1 - MT_scale));
		}

		// camera.viewportWidth = width
		// + (Gdx.app.getType() == ApplicationType.Android ? 20 /
		// PIXEL_TO_WORLD_RATIO
		// : 0);
		// camera.viewportHeight = height;
		// camera.zoom = factor;
		//
		cameraSCENE.near = 1.0f; // default = 1.0
		cameraSCENE.far = worldSize.x * 2; // default = 100
		//
		updateZ(-(cameraSCENE.far - cameraSCENE.near) / 5f);
		//
		cameraSCENE.update();
	}

	@Override
	public void resize(int screenWidth, int screenHeight) {

		if (Gdx.graphics.isGL20Available()) {
			if (frameBuffer != null) {
				Gdx.app.log(APP_NAME,
						"Destroying FrameBuffer: " + frameBuffer.getWidth()
								+ "x" + frameBuffer.getHeight());
				frameBuffer.dispose();
			}
			frameBuffer = new FrameBuffer(Format.RGB565,
					Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
			frameBufferSubTexture = new SubTexture(
					frameBuffer.getColorBufferTexture());
			frameBufferSubTexture.flipY = true;
		}

		destroyScreenCapture();

		float radius = Math.min(screenWidth, screenHeight) / 7;
		float offset = radius / 4;
		thumbpad1 = new Thumbpad(shader_POSITION, shapeRenderer, (int) radius,
				new Vector2(radius + offset, radius + offset),
				(int) screenHeight, 0.0f);
		thumbpad2 = new Thumbpad(shader_POSITION, shapeRenderer, (int) radius,
				new Vector2(screenWidth - radius - offset, radius + offset),
				(int) screenHeight, 0.0f);

		cameraSCREEN = new OrthographicCamera(Gdx.graphics.getWidth(),
				Gdx.graphics.getHeight());
		cameraSCREEN.translate(Gdx.graphics.getWidth() / 2,
				Gdx.graphics.getHeight() / 2, 0);
		// cameraSCREEN.viewportWidth = screenWidth;
		// cameraSCREEN.viewportHeight = screenHeight;
		cameraSCREEN.update();

		if (renderState == RenderState.Normal) {
			//
			// String text =
			// "This is a rather long piece of text, with explicit line breaks\nand other bits and bobs to make it wrap automatically.";
			// font.setScale(1.0f);
			// fontCache1 = new BitmapFontCache(font);
			// fontCache1.setColor(Color.BLACK);
			// fontCache1.setWrappedText(text, screenWidth / 4, screenHeight
			// - screenHeight / 6, screenWidth / 2, HAlignment.CENTER);

			resetWorldSize(screenWidth, screenHeight);

			String text = "This text contains an explicit line break (here:\n), but it should wrap automatically. It should be aliased nicely despite being based on a bitmap font, which is itself a texture projected onto a 3D mesh.";

			for (int i = 0; i < fonts.size; i++) {
				// BitmapFont font = fonts.get(i);
				BitmapFontCache fontCache = fontCaches.get(i);

				// fontCache.getFont().setScale(1.0f);
				// fontCache.getFont().setColor(Color.BLACK);

				// font.setScale(1.0f);
				// font.setColor(Color.BLACK);
				//
				fontCache.setColor(Color.BLACK);

				// fontCache1.setWrappedText(text, -worldSize.x / 2 +
				// worldSize.x /
				// 4,
				// worldSize.y - worldSize.y / 6, worldSize.x / 2,
				// HAlignment.CENTER);
				// fontCache1.setWrappedText(text, screenWidth / 4, screenHeight
				// -
				// screenHeight / 6, screenWidth / 2, HAlignment.CENTER);
				fontCache.setWrappedText(text, 0, 0, screenWidth / 2,
						HAlignment.CENTER);
				// fontCache1.translate(xAmount, yAmount);
			}

			resetSCENECamera(worldSize.x, worldSize.y, 1, Float.MIN_VALUE,
					Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE); // reset
																		// to
																		// defaults
			clearWorld();
			// bouncingText_Position.set(0, 0);
			initWorld();
		}
	}

	Array<Array<Vector2>> slashPoints = new Array<Array<Vector2>>(
			MAX_TOUCH_POINTERS);

	Array<Vector2[]> slashPointCache = new Array<Vector2[]>(MAX_TOUCH_POINTERS);

	Array<Array<Vector2>> slashPointsSpline = new Array<Array<Vector2>>(
			MAX_TOUCH_POINTERS);
	long[] slashPointsSplineFadeoutTime = new long[MAX_TOUCH_POINTERS];

	Body touchedBody;
	Vector2[] lastTouchDown = new Vector2[MAX_TOUCH_POINTERS];

	String textInput = null;

	@Override
	public boolean touchDown(int x, int y, int pointer, int newParam) {
		if (renderState != RenderState.Normal)
			return false;

		touchDownTimes[pointer] = System.nanoTime();

		if (thumbpad1.touchDown(x, y, pointer)) {

			if (thumbpad2.touchIndex == -1) {
				// thumbpad1.unstick();
				// thumbpad2.unstick();

				thumbpad1.restoreXYAxis();
				thumbpad2.forceYAxis();
			}
			resetSCENECameraThumbpad1();
			return true;
		}
		if (thumbpad2.touchDown(x, y, pointer)) {

			if (thumbpad1.touchIndex == -1) {
				// thumbpad1.unstick();
				// thumbpad2.unstick();

				thumbpad2.restoreXYAxis();
				thumbpad1.forceYAxis();
			}
			resetSCENECameraThumbpad2();
			return true;
		}

		soundCrush1.play();
		//
		if (lastTouchDown[pointer] == null)
			lastTouchDown[pointer] = new Vector2();
		lastTouchDown[pointer].set(x, y);
		//
		if (isUpperRightCornerTouch(x, y)) { // UPPER RIGHT

			if (pageCurler.autoAnimated) {
				pageCurler.autoAnimated = false;
				pageCurler.currentAnimationTime = 0;
				pageCurler.updateCurlMesh();
			} else {
				pageCurler.autoAnimated = true;
				pageCurler.timeAppLaunch = -1;
				pageCurler.currentAnimationTime = 0;
				pageCurler.updateCurlMesh();
			}

			// Gdx.input.getTextInput(new TextInputListener() {
			// @Override
			// public void input(String text) {
			// textInput = text;
			// }
			//
			// @Override
			// public void cancled() {
			// textInput = APP_NAME;
			// }
			// }, "Switching to font " + (currentFont + 1),
			// "please discard this dialog");
			// if (textInput == APP_NAME) {
			// // cancelled
			// } else {
			// // TODO: display text ?
			// }
		} else if (isUpperLeftCornerTouch(x, y)) { // UPPER LEFT
			DebugRender[] values = DebugRender.values();
			int current = renderDebug.ordinal();
			int next = (current + 1) % values.length;
			renderDebug = values[next];
		} else if (isBottomLeftCornerTouch(x, y)) { // BOTTOM LEFT

			if (cameraTweenManager != null) {
				cameraTweenManager.kill();
				// Tween.update();
				cameraTweenManager = null;
			}

			cameraTweenManager = new TweenManager();

			cameraTweenable.onTweenUpdated(-1, new float[] { 1 });

			Tween cameraTween = Tween.to(cameraTweenable, -1, 1500, Expo.IN)
					.target(1 / 3f).addCompleteCallback(new TweenCallback() {
						@Override
						public void tweenEventOccured(Types eventType,
								Tween tween) {
							if (cameraTweenManager != null) {
								cameraTweenManager.kill();
								// Tween.update();
								cameraTweenManager = null;
							}

						}
					}).start();

			cameraTweenManager.add(cameraTween);

		} else if (isBottomRightCornerTouch(x, y)) { // BOTTOM RIGHT

			currentFont = (currentFont + 1) % fonts.size;

			if (cameraTweenManager != null) {
				cameraTweenManager.kill();
				// Tween.update();
				cameraTweenManager = null;
			}
			// cameraTweenable.tweenUpdated(-1, 1);

			if (thumbpad1.sticky)
				thumbpad1.unstick();

			if (thumbpad1.axis != Axis.XY)
				thumbpad1.restoreXYAxis();

			if (thumbpad2.sticky)
				thumbpad2.unstick();

			if (thumbpad2.axis != Axis.XY)
				thumbpad2.restoreXYAxis();

			resetSCENECamera(worldSize.x, worldSize.y, 1, Float.MIN_VALUE,
					Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE); // reset
																		// to
																		// defaults
		}
		if (renderDebug == DebugRender.Full) {
			boolean multitouch = false;
			if (Gdx.input.isPeripheralAvailable(Peripheral.MultitouchScreen))
				for (int i = 0; i < 10; i++) {
					if (i != pointer && Gdx.input.isTouched(i)) {
						multitouch = true;
						break;
					}
				}
			MotionEvent event;
			if (multitouch) {
				if (MultiTouchController.DEBUG)
					Gdx.app.log(APP_NAME, "touchDown (MOVE): " + x + " / " + y);
				event = MotionEvent.create(MotionEvent.ACTION_MOVE, x, y,
						pointer, newParam,
						(long) (System.nanoTime() / 1000000.0f)); // ms
			} else {
				if (MultiTouchController.DEBUG)
					Gdx.app.log(APP_NAME, "touchDown (DOWN): " + x + " / " + y);
				event = MotionEvent.create(MotionEvent.ACTION_DOWN, x, y,
						pointer, newParam,
						(long) (System.nanoTime() / 1000000.0f)); // ms
			}

			return multiTouchController.onTouchEvent(event);
		}
		effect.setPosition(x, Gdx.graphics.getHeight() - y);
		effect.setDuration(1000);
		effect.start();
		/*
		 * emitters = new Array(effect.getEmitters());
		 * effect.getEmitters().clear();
		 * effect.getEmitters().add(emitters.get(0)); emitterIndex =
		 * (emitterIndex + 1) % emitters.size; // ParticleEmitter emitter =
		 * emitters.get(emitterIndex); particleCount =
		 * (int)(emitter.getEmission().getHighMax() *
		 * emitter.getLife().getHighMax() / 1000f); // particleCount =
		 * Math.max(0, particleCount); if (particleCount >
		 * emitter.getMaxParticleCount())
		 * emitter.setMaxParticleCount(particleCount * 2);
		 * emitter.getEmission().setHigh(particleCount /
		 * emitter.getLife().getHighMax() * 1000); effect.getEmitters().clear();
		 * effect.getEmitters().add(emitter); //
		 */

		touchedBody = null;
		temporaryPointUnproject(x, y, zPlane_box2D);
		world.QueryAABB(
				new QueryCallback() {
					@Override
					public boolean reportFixture(Fixture fixture) {
						if (fixture.getBody() == groundBody)
							return true;

						float z = ((BodyUserData) fixture.getBody()
								.getUserData()).zCoord;
						if (z != zCoord_box2D)
							return true;

						if (fixture.testPoint(temporaryPointForProjection.x,
								temporaryPointForProjection.y)) {
							touchedBody = fixture.getBody();
							return false;
						} else
							return true;
					}
				}, temporaryPointForProjection.x - 0.1f,
				temporaryPointForProjection.y - 0.1f,
				temporaryPointForProjection.x + 0.1f,
				temporaryPointForProjection.y + 0.1f);

		if (touchedBody == null) {
			touchedBody = null;
			temporaryPointUnproject(x, y, zPlane_box2D_);
			world.QueryAABB(
					new QueryCallback() {
						@Override
						public boolean reportFixture(Fixture fixture) {
							if (fixture.getBody() == groundBody)
								return true;

							float z = ((BodyUserData) fixture.getBody()
									.getUserData()).zCoord;
							if (z != zCoord_box2D_)
								return true;

							if (fixture.testPoint(
									temporaryPointForProjection.x,
									temporaryPointForProjection.y)) {
								touchedBody = fixture.getBody();
								return false;
							} else
								return true;
						}
					}, temporaryPointForProjection.x - 0.1f,
					temporaryPointForProjection.y - 0.1f,
					temporaryPointForProjection.x + 0.1f,
					temporaryPointForProjection.y + 0.1f);
		}

		if (touchedBody != null) {
			BodyUserData userData = (BodyUserData) touchedBody.getUserData();
			int bodyIndex = userData.textureIndex;

			MouseJointDef def = new MouseJointDef();
			def.bodyA = groundBody;
			def.bodyB = touchedBody;
			def.collideConnected = true;
			def.dampingRatio = bodyIndex < 3 ? 1f : 0.4f;
			def.frequencyHz = 10;
			def.target.set(temporaryPointForProjection.x,
					temporaryPointForProjection.y);
			def.maxForce = 300.0f * touchedBody.getMass();
			MouseJoint mouseJoint = (MouseJoint) world.createJoint(def);
			mouseJoints.set(pointer, mouseJoint);
			touchedBody.setAwake(true);
			// touchedBody.setBullet(true);
			soundPop2.play();

			// float z = ((BodyUserData) touchedBody.getUserData()).zCoord;
			// if (z == zCoord_box2D_) {
			// waterRipples.touchScreen(x, y);
			// }
		} else {
			boolean enableSlash = true;
			if (renderDebug == DebugRender.None) {
				if (usePageTurn)
					pageTurn.touchScreen(x, y);
				else {
					enableSlash = !pageCurler.page.touchScreen(x, y);
				}
			} else {
				waterRipples.touchScreen(x, y);
			}

			if (enableSlash) {
				slashPoints.get(pointer).clear();
				slashPoints.get(pointer).add(
						slashPointCache.get(pointer)[0].set(x, y));
			}

			// clearWorld();
			// initWorld();
		}
		return false;
	}

	public void temporaryPointProject(float x, float y, float z) {

		if (true || cameraSCENEOrthographic != null) {
			temporaryPointForProjection.x = x;
			temporaryPointForProjection.y = y;
			temporaryPointForProjection.z = z;
			cameraSCENE.project(temporaryPointForProjection);
		}

		if (cameraSCENEPerspective != null) {

		}
	}

	public void temporaryPointUnproject(int x, int y, Plane plane) {

		if (cameraSCENEOrthographic != null) {
			temporaryPointForProjection.x = x;
			temporaryPointForProjection.y = y;
			temporaryPointForProjection.z = 0; // near plane
			cameraSCENE.unproject(temporaryPointForProjection);
		}

		if (cameraSCENEPerspective != null) {

			temporaryPointForProjection.x = 123;
			temporaryPointForProjection.y = 123;
			temporaryPointForProjection.z = 123;
			Ray ray = cameraSCENE.getPickRay(x, y);
			Intersector.intersectRayPlane(ray, plane,
					temporaryPointForProjection);
		}
	}

	void resetSCENECameraThumbpad1() {
		if (cameraSCENEPerspective == null
				|| (thumbpad2.touchIndex != -1 && thumbpad1.axis != Thumbpad.Axis.XY))
			return;

		// float angle = (float) Math.toDegrees(thumbpad.angle);
		// Gdx.app.log(APP_NAME, ""+angle);

		// float thisx = -MT_offset.x / SCREEN_TO_WORLD_RATIO;
		// float thisy = MT_offset.y / SCREEN_TO_WORLD_RATIO;

		// MT_offset.z = 0 + angle * 2;
		// resetSCENECamera(worldSize.x, worldSize.y, MT_scale, MT_angle,
		// MT_offset.x, MT_offset.y, MT_offset.z);

		// sin(a) = opposite / hypotenuse
		// cos(a) = adjacent / hypotenuse
		// tan(a) = opposite / adjacent

		// thumbpad.amount == hypotenuse
		// projection on X axis = adjacent
		// projection on Y axis = opposite
		// note: values normalized on [0,1]

		float x = thumbpad1.amount * (float) Math.cos(thumbpad1.angle);
		float y = thumbpad1.amount * (float) Math.sin(thumbpad1.angle);

		resetSCENECamera(worldSize.x, worldSize.y, 1, Float.MIN_VALUE,
				Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE); // reset
																	// to
																	// defaults
		x *= worldSize.x;
		y *= worldSize.y;

		cameraSCENE.translate(x, y, 0);

		if (thumbpad2.touchIndex != -1 || thumbpad2.sticky) {

			x = thumbpad2.amount * (float) Math.cos(thumbpad2.angle);
			y = thumbpad2.amount * (float) Math.sin(thumbpad2.angle);

			x *= worldSize.x;
			y *= worldSize.y;

			// cameraSCENE.position.z = -y;
			cameraSCENE.translate(0, 0, -y);
		}

		cameraSCENE.lookAt(0, worldSize.y / 2, zCoord_box2D);

		cameraSCENE.update();
	}

	void resetSCENECameraThumbpad2() {
		if (cameraSCENEPerspective == null
				|| (thumbpad1.touchIndex != -1 && thumbpad2.axis != Thumbpad.Axis.XY))
			return;

		resetSCENECamera(worldSize.x, worldSize.y, 1, Float.MIN_VALUE,
				Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE); // reset
																	// to
																	// defaults

		float x = thumbpad2.amount * (float) Math.cos(thumbpad2.angle);
		float y = thumbpad2.amount * (float) Math.sin(thumbpad2.angle);

		x *= worldSize.x;
		y *= worldSize.y;

		cameraSCENE.translate(x, y, 0);

		if (thumbpad1.touchIndex != -1 || thumbpad1.sticky) {

			x = thumbpad1.amount * (float) Math.cos(thumbpad1.angle);
			y = thumbpad1.amount * (float) Math.sin(thumbpad1.angle);

			x *= worldSize.x;
			y *= worldSize.y / 2;

			// cameraSCENE.position.z = -y;
			cameraSCENE.translate(0, 0, -y);
		}

		cameraSCENE.update();
	}

	@Override
	public boolean touchDragged(int x, int y, int pointer) {
		if (renderState != RenderState.Normal)
			return false;

		boolean multitouchActivated = false;
		if (touchDownTimes[pointer] != -1
				&& ((System.nanoTime() - touchDownTimes[pointer]) / 1000000.0f) >= touchDownTimeout) {
			multitouchActivated = true;
		} else {
			touchDownTimes[pointer] = -1;
		}

		if (thumbpad1.touchIndex == pointer) {
			// if (multitouchActivated) {
			// thumbpad1.stick();
			// thumbpad2.stick();
			// }
			thumbpad1.touchDragged(x, y, pointer);
			if (thumbpad1.axis == Axis.XY)
				resetSCENECameraThumbpad1();
			else
				resetSCENECameraThumbpad2();
			return true;
		}
		if (thumbpad2.touchIndex == pointer) {
			// if (multitouchActivated) {
			// thumbpad1.stick();
			// thumbpad2.stick();
			// }
			thumbpad2.touchDragged(x, y, pointer);
			if (thumbpad2.axis == Axis.XY)
				resetSCENECameraThumbpad2();
			else
				resetSCENECameraThumbpad1();
			return true;
		}

		if (multitouchActivated || renderDebug == DebugRender.Full) {
			MotionEvent event = MotionEvent.create(MotionEvent.ACTION_MOVE, x,
					y, pointer, -1, (long) (System.nanoTime() / 1000000.0f)); // ms
			if (MultiTouchController.DEBUG)
				Gdx.app.log(APP_NAME, "touchDragged: " + x + " / " + y);
			return multiTouchController.onTouchEvent(event);
		}
		MouseJoint mouseJoint = mouseJoints.get(pointer);
		if (mouseJoint != null) {

			Body body = mouseJoint.getBodyB();
			float z = ((BodyUserData) body.getUserData()).zCoord;

			temporaryPointUnproject(x, y, z == zCoord_box2D ? zPlane_box2D
					: zPlane_box2D_);

			temporaryPointForDragging.x = temporaryPointForProjection.x;
			temporaryPointForDragging.y = temporaryPointForProjection.y;
			mouseJoint.setTarget(temporaryPointForDragging);
			if (effect.isComplete()) {
				effect.setDuration(1000);
				effect.start();
			}

			// if (z == zCoord_box2D_) {
			// waterRipples.touchScreen(x, y);
			// }
		} else {

			boolean enableSlash = true;
			if (renderDebug == DebugRender.None) {
				if (usePageTurn)
					pageTurn.touchScreen(x, y);
				else
					enableSlash = !pageCurler.page.touchScreen(x, y);
			} else {
				waterRipples.touchScreen(x, y);
			}

			if (enableSlash) {
				boolean enoughDisplacement = slashPoints.get(pointer).size == 0;
				if (slashPoints.get(pointer).size > 0) {
					Vector2 v = slashPoints.get(pointer).get(
							slashPoints.get(pointer).size - 1);

					enoughDisplacement = Math.abs(x - v.x) > FINGER_TOUCH_TOLERANCE
							* SPLINE_INTERMEDIATE_POINTS
							|| Math.abs(y - v.y) > FINGER_TOUCH_TOLERANCE
									* SPLINE_INTERMEDIATE_POINTS;
				}
				if (enoughDisplacement)
					if (slashPoints.get(pointer).size < (slashPointCache
							.get(pointer).length - 1)) {
						slashPoints.get(pointer).add(
								slashPointCache.get(pointer)[slashPoints
										.get(pointer).size].set(x, y));
					} else {
						slashPoints.get(pointer).add(new Vector2(x, y));
					}
			}
			// effect.setDuration(1000);
			// effect.start();
			// effect.setPosition(x, Gdx.graphics.getHeight() - y);
		}
		return false;
	}

	int nextSpriteIndex = 0;

	@Override
	public boolean touchUp(int x, int y, int pointer, int button) {
		if (renderState != RenderState.Normal)
			return false;

		pageCurler.page.untouchScreen(x, y);

		boolean multitouchActivated = false;
		if (touchDownTimes[pointer] != -1
				&& ((System.nanoTime() - touchDownTimes[pointer]) / 1000000.0f) >= touchDownTimeout) {
			multitouchActivated = true;
		}

		touchDownTimes[pointer] = -1;

		if (thumbpad1.touchIndex == pointer) {
			if (!multitouchActivated) {
				// thumbpad1.unstick();
				// thumbpad2.unstick();
				// thumbpad2.restoreXYAxis();
			}
			if (thumbpad2.touchIndex == -1 && !thumbpad1.sticky) {
				thumbpad1.restoreXYAxis();
				thumbpad2.restoreXYAxis();
				// thumbpad2.forceYAxis();
			}
			thumbpad1.touchUp(x, y, pointer);

			if (thumbpad2.touchIndex == -1) {
				if (!thumbpad1.sticky) {
					// resetSCENECameraThumbpad();
					// resetSCENECamera(worldSize.x, worldSize.y, MT_scale,
					// MT_angle,
					// MT_offset.x, MT_offset.y, MT_offset.z);
					resetSCENECamera(worldSize.x, worldSize.y, 1,
							Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE,
							Float.MIN_VALUE); // reset
												// to
												// defaults
				}
			} else {
				if (thumbpad2.axis == Axis.XY)
					resetSCENECameraThumbpad2();
				else
					resetSCENECameraThumbpad1();
			}
		}
		if (thumbpad2.touchIndex == pointer) {
			if (!multitouchActivated) {
				// thumbpad1.unstick();
				// thumbpad2.unstick();
				// thumbpad1.restoreXYAxis();
			}
			if (thumbpad1.touchIndex == -1 && !thumbpad2.sticky) {
				thumbpad1.restoreXYAxis();
				thumbpad2.restoreXYAxis();
				// thumbpad2.forceYAxis();
			}
			thumbpad2.touchUp(x, y, pointer);

			if (thumbpad1.touchIndex == -1) {
				if (!thumbpad2.sticky) {
					resetSCENECamera(worldSize.x, worldSize.y, 1,
							Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE,
							Float.MIN_VALUE); // reset
												// to
												// defaults
				}
			} else {
				if (thumbpad1.axis == Axis.XY)
					resetSCENECameraThumbpad1();
				else
					resetSCENECameraThumbpad2();
			}
		}

		if (multitouchActivated || renderDebug == DebugRender.Full) {
			int multitouch = -1;
			if (Gdx.input.isPeripheralAvailable(Peripheral.MultitouchScreen))
				for (int i = 0; i < 10; i++) {
					if (i != pointer && Gdx.input.isTouched(i)) {
						multitouch = i;
					}
				}
			if (MultiTouchController.DEBUG)
				Gdx.app.log(APP_NAME, "touchUp (UP): " + x + " / " + y);
			MotionEvent event = MotionEvent.create(MotionEvent.ACTION_UP, x, y,
					pointer, button, (long) (System.nanoTime() / 1000000.0f)); // ms
			if (false && multitouch != -1) {
				if (MultiTouchController.DEBUG)
					Gdx.app.log(APP_NAME,
							"touchUp (MOVE): " + Gdx.input.getX(multitouch)
									+ " / " + Gdx.input.getY(multitouch));
				event = MotionEvent.create(MotionEvent.ACTION_MOVE,
						Gdx.input.getX(multitouch), Gdx.input.getY(multitouch),
						multitouch, button,
						(long) (System.nanoTime() / 1000000.0f)); // ms
			}
			return multiTouchController.onTouchEvent(event);
		}
		// effect.setPosition(Gdx.graphics.getWidth() / 2,
		// Gdx.graphics.getHeight() / 2);
		effect.setPosition(x, Gdx.graphics.getHeight() - y);
		effect.setDuration(50);
		effect.start();
		//
		MouseJoint mouseJoint = mouseJoints.get(pointer);
		if (mouseJoint != null) {
			Body body = mouseJoint.getBodyB();

			// body.setBullet(false);
			world.destroyJoint(mouseJoint);

			mouseJoints.set(pointer, null);
		}
		//
		float halfFingerTolerance = FINGER_TOUCH_TOLERANCE / 2;
		//
		performSlash[pointer] = false;

		if (slashPoints.get(pointer).size > 1) {
			slashPointsSpline.get(pointer).clear();
			slashPointsSplineFadeoutTime[pointer] = -1;

			if (slashPoints.get(pointer).size >= 4) {
				CatmullRomSpline spline = new CatmullRomSpline();

				for (int i = 0; i < slashPoints.get(pointer).size; i++) {
					Vector2 v1 = slashPoints.get(pointer).get(i);

					spline.add(new Vector3(v1.x, v1.y, zCoord_box2D));
				}

				slashPointsSpline.get(pointer).add(
						slashPoints.get(pointer).get(0).cpy());

				List<Vector3> list = spline.getPath(SPLINE_INTERMEDIATE_POINTS);
				for (int i = 0; i < list.size(); i++) {
					Vector3 v = list.get(i);

					slashPointsSpline.get(pointer).add(new Vector2(v.x, v.y));
				}

				slashPointsSpline.get(pointer).add(
						slashPoints.get(pointer)
								.get(slashPoints.get(pointer).size - 1).cpy());

				slashPointsSplineFadeoutTime[pointer] = System.nanoTime();
			}

			for (int i = 0; i < slashPoints.get(pointer).size; i++) {
				Vector2 v1 = slashPoints.get(pointer).get(i);

				temporaryPointUnproject((int) v1.x, (int) v1.y, zPlane_box2D);
				v1.set(temporaryPointForProjection.x,
						temporaryPointForProjection.y);
			}

			performSlash[pointer] = true;

		} else if (slashPoints.get(pointer).size == 1) {

			if (!isBottomLeftCornerTouch(x, y)
					&& !isBottomRightCornerTouch(x, y)
					&& !isUpperLeftCornerTouch(x, y)
					&& !isUpperRightCornerTouch(x, y)) {
				nextSpriteIndex = nextSpriteIndex >= (sprite_ImagePaths.size - 1) ? 0
						: nextSpriteIndex + 1;
				// int index = computeModuloSpriteRotationThing(); //
				float angle = (float) Math.toRadians(Math.random() * 360);
				temporaryPointUnproject(x, y, zPlane_box2D);
				generate_spriteBody_fromExistingTextureAndPolygons(
						temporaryPointForProjection.x,
						temporaryPointForProjection.y, zCoord_box2D, angle,
						nextSpriteIndex, DEFAULT_restitution,
						DEFAULT_angularDamping, DEFAULT_linearDamping,
						DEFAULT_friction, DEFAULT_density);
				soundPop1.play();
			}
		} else if (lastTouchDown[pointer] != null
				&& (x >= (lastTouchDown[pointer].x - halfFingerTolerance)
						&& x <= (lastTouchDown[pointer].x + halfFingerTolerance)
						&& y >= (lastTouchDown[pointer].y - halfFingerTolerance) && y <= (lastTouchDown[pointer].y + halfFingerTolerance))) {

			touchedBody = null;
			temporaryPointUnproject(x, y, zPlane_box2D);
			world.QueryAABB(
					new QueryCallback() {
						@Override
						public boolean reportFixture(Fixture fixture) {
							if (fixture.getBody() == groundBody)
								return true;

							float z = ((BodyUserData) fixture.getBody()
									.getUserData()).zCoord;
							if (z != zCoord_box2D)
								return true;

							if (fixture.testPoint(
									temporaryPointForProjection.x,
									temporaryPointForProjection.y)) {
								touchedBody = fixture.getBody();
								return false;
							} else
								return true;
						}
					}, temporaryPointForProjection.x - 0.1f,
					temporaryPointForProjection.y - 0.1f,
					temporaryPointForProjection.x + 0.1f,
					temporaryPointForProjection.y + 0.1f);

			if (touchedBody == null) {
				touchedBody = null;
				temporaryPointUnproject(x, y, zPlane_box2D_);
				world.QueryAABB(new QueryCallback() {
					@Override
					public boolean reportFixture(Fixture fixture) {
						if (fixture.getBody() == groundBody)
							return true;

						float z = ((BodyUserData) fixture.getBody()
								.getUserData()).zCoord;
						if (z != zCoord_box2D_)
							return true;

						if (fixture.testPoint(temporaryPointForProjection.x,
								temporaryPointForProjection.y)) {
							touchedBody = fixture.getBody();
							return false;
						} else
							return true;
					}
				}, temporaryPointForProjection.x - 0.1f,
						temporaryPointForProjection.y - 0.1f,
						temporaryPointForProjection.x + 0.1f,
						temporaryPointForProjection.y + 0.1f);
			}

			if (touchedBody != null) {
				performBodyBreak[pointer] = touchedBody;
			}
		}
		if (!performSlash[pointer]) {
			slashPoints.get(pointer).clear();
		}
		//
		return false;
	}

	@Override
	public void dispose() {
		if (shader_POSITION != null) {
			shader_POSITION.dispose();
		}
		if (shader_POSITION_COLOR != null) {
			shader_POSITION_COLOR.dispose();
		}
		if (shader_POSITION_COLOR_TEX1 != null) {
			shader_POSITION_COLOR_TEX1.dispose();
		}
		if (shader_POSITION_COLOR_TEX2 != null) {
			shader_POSITION_COLOR_TEX2.dispose();
		}
		if (shader_POSITION_TEX1 != null) {
			shader_POSITION_TEX1.dispose();
		}
		if (shader_POSITION_TEX2 != null) {
			shader_POSITION_TEX2.dispose();
		}

		if (Gdx.graphics.isGL20Available() && frameBuffer != null) {
			frameBuffer.dispose();
		}

		renderState = RenderState.Disposed;
		clearWorld();
		while (sprite_TexturesMeshes.size > 0) {
			Mesh mesh = sprite_TexturesMeshes.removeIndex(0);
			if (mesh != null) {
				mesh.dispose();
			}
		}
		if (world != null)
			world.dispose();
		// if (font != null)
		// font.dispose();
		for (int i = 0; i < fonts.size; i++) {
			BitmapFontCache fontCache = fontCaches.get(i);
			fontCache.dispose();
		}
		if (spriteBatch != null)
			spriteBatch.dispose();
		if (box2d_renderer != null)
			box2d_renderer.dispose();
		Tween.dispose();

		shapeRenderer.dispose();

		System.out.println("DISPOSING TEXTURES...");

		while (sprite_Textures.size > 0) {
			SubTexture subtex = sprite_Textures.removeIndex(0);
			if (subtex != null) {
				subtex.texture.dispose();
			}
		}

		System.out.println("DISPOSED.");
	}

	@Override
	public void pause() {
	}

	@Override
	public void resume() {
	}

	@Override
	public boolean touchMoved(int arg0, int arg1) {
		return false;
	}

	@Override
	public boolean keyDown(int arg0) {
		return false;
	}

	boolean[] performSlash = new boolean[MAX_TOUCH_POINTERS];
	Body[] performBodyBreak = new Body[MAX_TOUCH_POINTERS];

	@Override
	public boolean keyTyped(char arg0) {
		// Gdx.input.isKeyPressed(Input.Keys.C)) {
		// if (arg0 == 'c') {
		// return true;
		// }
		return false;
	}

	@Override
	public boolean keyUp(int arg0) {
		return false;
	}

	@Override
	public boolean scrolled(int arg0) {
		return false;
	}

	float MT_scale = 1;
	float MT_angle = 0;
	Vector3 MT_offset = new Vector3(0, 0, 0);

	@Override
	public void getPositionAndScale(LibGDXPlayground obj,
			PositionAndScale objPosAndScaleOut) {
		if (MultiTouchController.DEBUG)
			Gdx.app.log(APP_NAME, "objPosAndScaleOut: " + MT_offset.x + " / "
					+ MT_offset.y);
		objPosAndScaleOut.set(MT_offset.x, // xOff,
				MT_offset.y, // yOff,
				true, // updateScale,
				MT_scale, // scale,
				false, // updateScaleXY,
				1, // scaleX,
				1, // scaleY,
				true, // updateAngle,
				MT_angle // angle
				);
	}

	@Override
	public boolean setPositionAndScale(LibGDXPlayground obj,
			PositionAndScale newObjPosAndScale, PointInfo touchPoint) {
		for (int i = 0; i < touchPoint.getNumTouchPoints(); i++) {
			int pointer = touchPoint.getPointerIds()[i];
			if (!Gdx.input.isTouched(pointer)) {
				return false;
			}
		}
		if (touchPoint.isMultiTouch()) {
			MT_scale = newObjPosAndScale.getScale();
			MT_angle = newObjPosAndScale.getAngle();
			// MT_offset.x = newObjPosAndScale.getXOff() * MT_scale;
			// MT_offset.y = newObjPosAndScale.getYOff() * MT_scale;
			if (MultiTouchController.DEBUG)
				Gdx.app.log(APP_NAME, "setPositionAndScale: " + MT_offset.x
						+ " / " + MT_offset.y);
			// float minX = Float.MAX_VALUE;
			// float maxX = Float.MIN_VALUE;
			// float minY = Float.MAX_VALUE;
			// float maxY = Float.MIN_VALUE;
			// for (int i = 0; i < touchPoint.getNumTouchPoints(); i++) {
			// float x = touchPoint.getXs()[i];
			// float y = touchPoint.getYs()[i];
			// if (MultiTouchController.DEBUG)
			// Gdx.app.log(APP_NAME, "setPositionAndScale_" + i + ": " + x
			// + " / " + y);
			// if (x < minX)
			// minX = x;
			// if (y < minY)
			// minY = y;
			// if (x > maxX)
			// maxX = x;
			// if (y > maxY)
			// maxY = y;
			// }
			// float centerX = minX + (maxX - minX) / 2;
			// float centerY = minY + (maxY - minY) / 2;
			// thumbpad1.centre.x = centerX;
			// thumbpad1.centre.y = Gdx.graphics.getHeight() - centerY;
		} else {
			MT_offset.x = newObjPosAndScale.getXOff();
			MT_offset.y = newObjPosAndScale.getYOff();
		}
		if (MultiTouchController.DEBUG)
			Gdx.app.log(APP_NAME, "setPositionAndScale: " + MT_offset.x + " / "
					+ MT_offset.y);
		resetSCENECamera(worldSize.x, worldSize.y, MT_scale, MT_angle,
				MT_offset.x, MT_offset.y, MT_offset.z);

		return true;
	}

	@Override
	public void selectObject(LibGDXPlayground obj, PointInfo touchPoint) {
		if (MultiTouchController.DEBUG)
			Gdx.app.log(APP_NAME, "selectObject: " + touchPoint.getX() + " / "
					+ touchPoint.getY());
	}

	@Override
	public LibGDXPlayground getDraggableObjectAtPoint(PointInfo touchPoint) {
		if (MultiTouchController.DEBUG)
			Gdx.app.log(APP_NAME,
					"getDraggableObjectAtPoint: " + touchPoint.getX() + " / "
							+ touchPoint.getY());
		return this;
	}
}
