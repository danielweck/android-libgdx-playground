package daniel.weck;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.GLCommon;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.glutils.ImmediateModeRenderer;
import com.badlogic.gdx.graphics.glutils.ImmediateModeRenderer10;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;

public class Thumbpad {
	int radiusX;
	int radiusY;
	int radiusX_ORIGINAL;
	int radiusY_ORIGINAL;
	Vector2 centre;
	int screenHeight;

	float zDepthCoord;

	enum Axis {
		XY, X, Y
	}

	Axis axis = Axis.XY;

	ShapeRendererEx shapeRenderer;

	ShaderProgram shader;

	public Thumbpad(ShaderProgram s, ShapeRendererEx renderer, int r, // pixels
			Vector2 c, // relative to left-bottom corner of the display
			int h, float z) {

		shader = s;
		shapeRenderer = renderer;

		radiusX = r;
		radiusY = r;
		radiusX_ORIGINAL = radiusX;
		radiusY_ORIGINAL = radiusY;
		centre = c;
		screenHeight = h;
		zDepthCoord = z;
		createMeshes();
	}

	public void forceYAxis() {
		axis = Axis.Y;
		radiusX = radiusX_ORIGINAL;
		radiusY = radiusY_ORIGINAL;
		radiusX /= INNER_OUTER_RATIO;
	}

	public void forceXAxis() {
		axis = Axis.X;
		radiusX = radiusX_ORIGINAL;
		radiusY = radiusY_ORIGINAL;
		radiusY /= INNER_OUTER_RATIO;
	}

	public void restoreXYAxis() {
		axis = Axis.XY;
		radiusX = radiusX_ORIGINAL;
		radiusY = radiusY_ORIGINAL;
	}

	boolean hitTest(int x, int y) {
		// mouse/touch coordinates: relative to top-left corner of the display
		int xMin = (int) centre.x - radiusX;
		int xMax = (int) centre.x + radiusX;
		int yMin = screenHeight - (int) centre.y - radiusY;
		int yMax = screenHeight - (int) centre.y + radiusY;
		if (x < xMin || x > xMax || y > yMax || y < yMin)
			return false;
		return true;
	}

	public boolean touchDown(int x, int y, int index) {
		if (hitTest(x, y)) {
			touchIndex = index;
			touchDragged(x, y, index);
			return true;
		}
		return false;
	}

	final Vector2 distance = new Vector2(0, 0);

	public boolean touchDragged(int x, int y, int index) {
		if (touchIndex != -1) {
			distance.x = x - centre.x;
			distance.y = (screenHeight - y) - centre.y;

			if (axis == Axis.X) {
				distance.y = 0;
			} else if (axis == Axis.Y) {
				distance.x = 0;
			}

			angle = (float) (Math.atan2(distance.y, distance.x));

			// sin(a) = opposite / hypotenuse
			// cos(a) = adjacent / hypotenuse
			// tan(a) = opposite / adjacent

			// thumbpad.amount == hypotenuse
			// projection on X axis = adjacent
			// projection on Y axis = opposite
			// note: values normalized on [0,1]

			if (radiusX == radiusY) {
				amount = distance.len() / radiusX;
			} else {

				float beta = -0; // for rotated ellipse
				float sinbeta = (float) Math.sin(beta);
				float cosbeta = (float) Math.cos(beta);
				float sinalpha = (float) Math.sin(angle);
				float cosalpha = (float) Math.cos(angle);

				// radiusX = semi-major axis
				// radiusY = semi-minor axis
				float ex = 0 // center
				+ (radiusX * cosalpha * cosbeta - radiusY * sinalpha * sinbeta);
				float ey = 0 // center
				+ (radiusX * cosalpha * sinbeta + radiusY * sinalpha * cosbeta);

				float radius = ey / (float) Math.sin(angle);

				amount = distance.len() / radius;
			}
			if (amount > 1) {
				amount = 1;
			}
			// amount *= amount;
		}
		return hitTest(x, y);
	}

	void reset() {
		amount = 0;
		angle = 0;
	}

	boolean sticky = false;

	void stick() {
		sticky = true;
	}

	void unstick() {
		sticky = false;
		if (touchIndex == -1)
			reset();
	}

	void toggleStick() {
		if (sticky)
			unstick();
		else
			stick();
	}

	public boolean touchUp(int x, int y, int index) {
		touchIndex = -1;
		if (!sticky) {
			reset();
		}
		return hitTest(x, y);
	}

	int touchIndex = -1; // the single touch index currently acquired

	public int touchIndex() {
		return touchIndex;
	}

	// in radians: 0 -> 2PI, -0 -> -2PI
	// equivalent degrees: 0 -> 180 and -0 -> -180
	float angle = 0;

	public float getAngle() {
		return angle;
	}

	// along the radius, normalized [0..1]
	// (capped to 1, which is on the circle circumference boundary)
	float amount = 0;

	public float getAmount() {
		return amount;
	}

	private static final int NVERTICES_OUTER = 50;
	private static final int NVERTICES_INNER = 15;
	Mesh meshOuter;
	Mesh meshInner;

	final int INNER_OUTER_RATIO = 3;

	public void createMeshes() {
		meshOuter = new Mesh(true, NVERTICES_OUTER, NVERTICES_OUTER,
				new VertexAttribute(Usage.Position, 3,
						ShaderProgram.POSITION_ATTRIBUTE));
		meshOuter.setVertices(createCircleVertices(NVERTICES_OUTER, radiusX,
				radiusY));
		//
		meshInner = new Mesh(false, NVERTICES_INNER, NVERTICES_INNER,
				new VertexAttribute(Usage.Position, 3,
						ShaderProgram.POSITION_ATTRIBUTE));
		meshInner.setVertices(createCircleVertices(NVERTICES_INNER, radiusX
				/ INNER_OUTER_RATIO, radiusY / INNER_OUTER_RATIO));
	}

	public float[] createCircleVertices(int nVertices, int radiusX, int radiusY) {
		float[] vertices = new float[nVertices * 3];
		for (int i = 0; i < nVertices; i++) {
			float angle = (float) (1 / (float) nVertices * i * Math.PI * 2);
			vertices[i * 3] = (float) Math.sin(angle) * radiusX;
			vertices[i * 3 + 1] = (float) Math.cos(angle) * radiusY;
			vertices[i * 3 + 2] = zDepthCoord;
		}
		return vertices;
	}

	Matrix4 shaderMatrix;

	public void render(GLCommon gl, Camera camera) {

		if (shaderMatrix == null) {
			shaderMatrix = new Matrix4();
			shaderMatrix.idt();
		}

		// GLCommon gl = null;
		// if (Gdx.graphics.isGL20Available())
		// gl = Gdx.gl20;
		// else
		// gl = Gdx.gl10;
		//
		// // GL10 gl = Gdx.gl10;

		gl.glActiveTexture(GL10.GL_TEXTURE0 + 0);
		gl.glDisable(GL10.GL_TEXTURE_2D);
		gl.glEnable(GL10.GL_BLEND);
		gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
		if (!Gdx.graphics.isGL20Available()) {
			Gdx.gl10.glColor4f(1, 1, 1, 1);
		}
		float alpha = (touchIndex != -1 ? 0.7f : 0.3f);

		if (sticky) {
			if (!Gdx.graphics.isGL20Available()) {
				Gdx.gl10.glColor4f(0, 0, 0, alpha);
			}
			shapeRenderer.setColor(0, 0, 0, alpha);
		} else {
			if (!Gdx.graphics.isGL20Available()) {
				Gdx.gl10.glColor4f(1, 1, 1, alpha);
			}
			shapeRenderer.setColor(1, 1, 1, alpha);
		}

		if (radiusX == radiusY) {

			if (!Gdx.graphics.isGL20Available()) {

				camera.apply(Gdx.gl10);
				Gdx.gl10.glPushMatrix();
				// Gdx.gl10.glMatrixMode(GL10.GL_MODELVIEW);
				// Gdx.gl10.glLoadIdentity();
				Gdx.gl10.glTranslatef(centre.x, centre.y, 0);

				meshOuter.render(GL10.GL_TRIANGLE_FAN, 0, NVERTICES_OUTER); // GL_LINE_LOOP

				Gdx.gl10.glPopMatrix();
			} else {

				shaderMatrix.set(camera.combined);
				shaderMatrix.translate(centre.x, centre.y, 0);

				shader.begin();
				shader.setUniformMatrix(
						LibGDXPlayground.ShaderProgram_PROJECTION_VIEW_MATRIX_UNIFORM,
						shaderMatrix);
				//shader.setUniformi(LibGDXPlayground.ShaderProgram_TEXTURE_SAMPLER_UNIFORM + 0, 0);
				
				if (sticky)
					shader.setUniformf(
							LibGDXPlayground.ShaderProgram_VERTEX_COLOR_UNIFORM,
							0, 0, 0, alpha);
				else
					shader.setUniformf(
							LibGDXPlayground.ShaderProgram_VERTEX_COLOR_UNIFORM,
							1, 1, 1, alpha);

				meshOuter.render(shader, GL10.GL_TRIANGLE_FAN, 0,
						NVERTICES_OUTER); // GL_LINE_LOOP

				shader.end();
			}
		} else {
			shapeRenderer.setProjectionMatrix(camera.combined);
			shaderMatrix.idt();
			shaderMatrix.translate(centre.x, centre.y, 0);
			shapeRenderer.setTransformMatrix(shaderMatrix);
			
			shapeRenderer.begin(GL10.GL_TRIANGLE_FAN);
			for (int a = 0; a <= 360; a += (360 / NVERTICES_OUTER)) {
				float xx = radiusX * (float) Math.cos(Math.toRadians(a));
				float yy = radiusY * (float) Math.sin(Math.toRadians(a));
				// shapeRenderer.setColor(0.1f, 1f, 0.4f, 0.5f);
				shapeRenderer.point(xx, yy, 0);
			}
			shapeRenderer.end();
		}

		if (sticky) {
			if (!Gdx.graphics.isGL20Available()) {
				Gdx.gl10.glColor4f(1, 0, 0, alpha);
			}
			shapeRenderer.setColor(1, 0, 0, alpha);
		} else {
			if (!Gdx.graphics.isGL20Available()) {
				Gdx.gl10.glColor4f(0, 0, 1, alpha);
			}
			shapeRenderer.setColor(0, 0, 1, alpha);
		}

		float centerX = amount * radiusX * (float) Math.cos(angle) + centre.x;
		float centerY = amount * radiusY * (float) Math.sin(angle) + centre.y;

		if (true || radiusX == radiusY) { // center is always round
			if (!Gdx.graphics.isGL20Available()) {

				camera.apply(Gdx.gl10);

				Gdx.gl10.glPushMatrix();
				// Gdx.gl10.glMatrixMode(GL10.GL_MODELVIEW);
				// Gdx.gl10.glLoadIdentity();
				Gdx.gl10.glTranslatef(centerX, centerY, 0);

				meshInner.render(GL10.GL_TRIANGLE_FAN, 0, NVERTICES_INNER);

				Gdx.gl10.glPopMatrix();
			} else {
				shaderMatrix.set(camera.combined);
				shaderMatrix.translate(centerX, centerY, 0);

				shader.begin();
				shader.setUniformMatrix(
						LibGDXPlayground.ShaderProgram_PROJECTION_VIEW_MATRIX_UNIFORM,
						shaderMatrix);
				//shader.setUniformi(LibGDXPlayground.ShaderProgram_TEXTURE_SAMPLER_UNIFORM + 0, 0);
				
				if (sticky)
					shader.setUniformf(
							LibGDXPlayground.ShaderProgram_VERTEX_COLOR_UNIFORM,
							1, 0, 0, alpha);
				else
					shader.setUniformf(
							LibGDXPlayground.ShaderProgram_VERTEX_COLOR_UNIFORM,
							0, 0, 1, alpha);

				meshInner.render(shader, GL10.GL_TRIANGLE_FAN, 0,
						NVERTICES_INNER);

				shader.end();
			}
		} else {
			shapeRenderer.setProjectionMatrix(camera.combined);
			shaderMatrix.idt();
			shaderMatrix.translate(centerX, centerY, 0);
			shapeRenderer.setTransformMatrix(shaderMatrix);
			shapeRenderer.begin(GL10.GL_TRIANGLE_FAN);
			for (int a = 0; a <= 360; a += (360 / NVERTICES_INNER)) {
				float xx = (radiusX / INNER_OUTER_RATIO)
						* (float) Math.cos(Math.toRadians(a));
				float yy = (radiusY / INNER_OUTER_RATIO)
						* (float) Math.sin(Math.toRadians(a));
				// shapeRenderer.setColor(0.1f, 1f, 0.4f, 0.5f);
				shapeRenderer.point(xx, yy, 0);
			}
			shapeRenderer.end();
		}
	}
}