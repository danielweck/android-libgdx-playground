package daniel.weck;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.GLCommon;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Plane;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.math.collision.Sphere;

public class LibGdxApp implements ApplicationListener, InputProcessor {

	WaterRipples waterRipples;

	Camera camera;

	Texture backgroundTexture_Aqua;

	@Override
	public void create() {

		Gdx.input.setInputProcessor(this);

		FileHandle file = Gdx.files.internal("data/background-aqua.jpg");
		backgroundTexture_Aqua = new Texture(file);
		backgroundTexture_Aqua.setFilter(Texture.TextureFilter.Linear,
				Texture.TextureFilter.Linear);
		backgroundTexture_Aqua.setWrap(Texture.TextureWrap.ClampToEdge,
				Texture.TextureWrap.ClampToEdge);
	}

	float cameraX;
	float cameraXShift = 1.4f;
	Vector3 gridCentre = new Vector3();
	Vector3 vector3 = new Vector3();
	Vector3 vector3_ = new Vector3();

	float elapsedSeconds = 0;

	@Override
	public void render() {
		Gdx.gl.glClearColor(1, 1, 1, 1);
		Gdx.gl.glClear(GL10.GL_COLOR_BUFFER_BIT);

		// 0.033 or 0.016 seconds
		float deltaSeconds = Gdx.graphics.getDeltaTime();
		elapsedSeconds += deltaSeconds;
		if (elapsedSeconds > 0.04f) {
			elapsedSeconds = 0;

			if (waterRipples != null && camera instanceof PerspectiveCamera) {

				boolean animate = true;
				if (animate) {
					cameraX += cameraXShift;
					if (cameraX > waterRipples.width) {
						cameraX = waterRipples.width;
						cameraXShift = -cameraXShift;
					} else if (cameraX < 0) {
						cameraX = 0;
						cameraXShift = -cameraXShift;
					}

					camera.position.set(cameraX, 0, camera.position.z);
					camera.lookAt(gridCentre.x, gridCentre.y, gridCentre.z);
				}

				float aspectRatio = Gdx.graphics.getWidth()
						/ (float) Gdx.graphics.getHeight();

				float fov = ((PerspectiveCamera) camera).fieldOfView;

				final float degToRad = (float) Math.PI / 180f;
				final float radToDeg = 1f / degToRad;

				// fov=Math.min(fovX, fovY)
				float halfFovRad = 0.5f * fov * degToRad;
				float h = 2f * camera.near * (float) Math.tan(halfFovRad);
				float w = h * aspectRatio;

				if (aspectRatio >= 1.0) { // landscape

					halfFovRad = (float) Math.atan(aspectRatio
							* Math.tan(halfFovRad));

					w = 2f * camera.near * (float) Math.tan(halfFovRad);
					h = w / aspectRatio;
				}

				boolean debugValues = false;
				if (debugValues) {
					camera.update();
					Vector3 p1 = camera.frustum.planePoints[0];
					Vector3 p2 = camera.frustum.planePoints[1];
					Vector3 p3 = camera.frustum.planePoints[2];
					Vector3 p4 = camera.frustum.planePoints[3];

					float ww = p1.dst(p2);
					float hh = p2.dst(p3);

					float nearWidth = p2.x - p1.x;
					float nearHeight = p3.y - p2.y;

					float fovHorizontal = radToDeg
							* 2f
							* (float) Math.atan((0.5f * nearWidth)
									/ camera.near);

					float val = 0.5f * nearHeight / camera.near;
					float fovVertical = radToDeg * 2f * (float) Math.atan(val);

					float val_ = (float) Math.tan(0.5f * degToRad
							* fovHorizontal)
							/ aspectRatio;
					float fovVertical_ = radToDeg * 2f
							* (float) Math.atan(val_);
				}

				// Bounding sphere radius
				float dim = (float) Math.sqrt(waterRipples.width
						* waterRipples.width + waterRipples.height
						* waterRipples.height) / 2f;

				boolean debugProjection = false;
				if (debugProjection) {
					float minY = Float.MAX_VALUE;
					float maxY = Float.MIN_VALUE;
					float minX = Float.MAX_VALUE;
					float maxX = Float.MIN_VALUE;

					vector3.set(waterRipples.posx, waterRipples.posy,
							waterRipples.zDepthCoord);
					camera.project(vector3);
					if (vector3.y > maxY)
						maxY = vector3.y;
					if (vector3.y < minY)
						minY = vector3.y;
					if (vector3.x > maxX)
						maxX = vector3.x;
					if (vector3.x < minX)
						minX = vector3.x;

					vector3.set(waterRipples.posx, waterRipples.posy
							+ waterRipples.height, waterRipples.zDepthCoord);
					camera.project(vector3);
					if (vector3.y > maxY)
						maxY = vector3.y;
					if (vector3.y < minY)
						minY = vector3.y;
					if (vector3.x > maxX)
						maxX = vector3.x;
					if (vector3.x < minX)
						minX = vector3.x;

					vector3.set(waterRipples.posx + waterRipples.width,
							waterRipples.posy + waterRipples.height,
							waterRipples.zDepthCoord);
					camera.project(vector3);
					if (vector3.y > maxY)
						maxY = vector3.y;
					if (vector3.y < minY)
						minY = vector3.y;
					if (vector3.x > maxX)
						maxX = vector3.x;
					if (vector3.x < minX)
						minX = vector3.x;

					vector3.set(waterRipples.posx + waterRipples.width,
							waterRipples.posy, waterRipples.zDepthCoord);
					camera.project(vector3);
					if (vector3.y > maxY)
						maxY = vector3.y;
					if (vector3.y < minY)
						minY = vector3.y;
					if (vector3.x > maxX)
						maxX = vector3.x;
					if (vector3.x < minX)
						minX = vector3.x;

					assert gridCentre.z == waterRipples.zDepthCoord;

					Plane plane = new Plane(camera.direction, new Vector3(
							gridCentre.x, gridCentre.y, gridCentre.z));

					// camera.unproject(vector3);

					vector3.set(0, 0, 0);
					Ray ray = camera.getPickRay(minX, minY);
					Intersector.intersectRayPlane(ray, plane, vector3);

					vector3_.set(0, 0, 0);
					ray = camera.getPickRay(maxX, maxY);
					Intersector.intersectRayPlane(ray, plane, vector3_);

					// float maxW = Math.max(vector3.x, vector3_.x)
					// - Math.min(vector3.x, vector3_.x);
					// float maxH = Math.max(vector3.y, vector3_.y)
					// - Math.min(vector3.y, vector3_.y);
					// dim = Math.max(maxW, maxH);

					Gdx.app.log("BBOX", "radius: " + dim);
					dim = vector3.dst(vector3_) / 3f;
					Gdx.app.log("BBOX", "vdst: " + dim + "\n----------");
				}

				// should be tan(), but looks better with sin()
				float distance_to_center = dim / (float) Math.sin(halfFovRad);

				Vector3 eyeToCenter = camera.direction.cpy().nor()
						.mul(distance_to_center);
				Vector3 eye = gridCentre.cpy().sub(eyeToCenter);

				camera.position.set(eye);

				// zfar = distance_to_center + radius;
				//
				// if (zfar < 1.5 * znear)
				// {
				// // Keep zfar always bigger than znear
				// zfar = 1.5 * znear;
				// }

				camera.update();
			}
		}

		if (camera != null && !Gdx.graphics.isGL20Available()) {
			camera.apply(Gdx.gl10);
		}

		GLCommon gl = null;
		if (Gdx.graphics.isGL20Available())
			gl = Gdx.gl20;
		else
			gl = Gdx.gl10;

		if (waterRipples != null) {
			gl.glActiveTexture(GL10.GL_TEXTURE0 + 0);
			gl.glEnable(GL10.GL_TEXTURE_2D);
			backgroundTexture_Aqua.bind(0);

			waterRipples.render(camera, false);
		}
	}

	boolean force2D = true;

	private Vector3 resetCamera(int width, int height) {

		camera = null;
		if (width < 200 || height < 200) {
			return null;
		}

		short gridWidth = (short) (width / (float) WaterRipples.CellSuggestedDimension);
		short gridHeight = (short) (height / (float) WaterRipples.CellSuggestedDimension);

		float gridZ = 0;
		if (force2D) {
			camera = new OrthographicCamera(gridWidth, gridHeight);
			((OrthographicCamera) camera).zoom = 1;
		} else {
			camera = new PerspectiveCamera(67, gridWidth, gridHeight);
		}

		cameraX = gridWidth / 2f;
		// cameraX = 0;

		float cameraY = gridHeight / 2f;
		// float cameraY = 0;

		// float cameraZ = -Math.max(gridWidth, gridHeight) / 2f;
		float cameraZ = 0;

		camera.position.set(cameraX, cameraY, cameraZ);

		gridZ = camera.position.z
				- (camera.near + ((camera.far - camera.near) / 5f));
		gridCentre.set(gridWidth / 2f, gridHeight / 2f, gridZ);

		// if (!force2D) {
		//
		// // gridBoundingSphere = new Sphere(new Vector3(gridWidth / 2f,
		// // gridHeight / 2f, gridZ), (float) Math.sqrt(gridWidth
		// // * gridWidth + gridHeight * gridHeight) / 2f);
		//
		// cameraX = 0;
		// camera.position.set(cameraX, 0, camera.position.z);
		// camera.lookAt(gridCentre.x, gridCentre.y, gridCentre.z);
		// }

		camera.update();

		return new Vector3(gridWidth, gridHeight, gridZ);
	}

	@Override
	public void resize(int width, int height) {

		waterRipples = null;

		Vector3 gridDims = resetCamera(width, height);
		if (gridDims == null) {
			return;
		}

		waterRipples = new WaterRipples(gridDims.z, 0, 0, (short) gridDims.x,
				(short) gridDims.y, backgroundTexture_Aqua);
	}

	@Override
	public boolean touchDragged(int x, int y, int pointer) {
		if (waterRipples != null) {
			waterRipples.touchScreen(camera, x, y);
		}
		return false;
	}

	int xTouchDown = 0;
	int yTouchDown = 0;

	@Override
	public boolean touchDown(int x, int y, int pointer, int button) {
		if (waterRipples != null) {
			waterRipples.touchScreen(camera, x, y);
		}
		if (pointer == 0) {
			xTouchDown = x;
			yTouchDown = y;
		}
		return false;
	}

	@Override
	public boolean touchUp(int x, int y, int pointer, int button) {
		if (pointer == 0) {
			if (xTouchDown == x && yTouchDown == y) {
				force2D = !force2D;
				Vector3 gridDims = resetCamera(Gdx.graphics.getWidth(),
						Gdx.graphics.getHeight());
				if (gridDims != null) {
					waterRipples.updateZ(gridDims.z);
				}
			}
		}
		return false;
	}

	@Override
	public void dispose() {
		if (backgroundTexture_Aqua != null) {
			backgroundTexture_Aqua.dispose();
		}
	}

	@Override
	public void pause() {
	}

	@Override
	public void resume() {
	}

	@Override
	public boolean keyDown(int arg0) {
		return false;
	}

	@Override
	public boolean keyTyped(char arg0) {
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

	@Override
	public boolean touchMoved(int x, int y) {
		return false;
	}
}
