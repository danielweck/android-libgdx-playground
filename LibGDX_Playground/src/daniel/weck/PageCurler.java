package daniel.weck;

import java.nio.IntBuffer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.GL11;
import com.badlogic.gdx.graphics.GLCommon;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Plane;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;

import daniel.weck.SubTexture;

// http://wdnuon.blogspot.com/2010/05/implementing-ibooks-page-curling-using.html
// http://blog.flirble.org/2010/10/08/the-anatomy-of-a-page-curl/
// http://forum.unity3d.com/threads/65639-Page-Flipping-Behaviour-Effect?p=419976&viewfull=1#post419976
// http://nomtek.com/tips-for-developers/page-flip-3d/
// http://nomtek.com/tips-for-developers/page-flip-2d/
// https://github.com/harism/android_page_curl/
// https://github.com/xissburg/XBPageCurl/blob/master/XBPageCurl/Resources/VertexShader.glsl
class PageCurler {

	// public float scale = 1f;

	Plane plane;

	PageMesh page;
	long timeAppLaunch = -1;
	long timeOpenClosePause = -1;

	boolean autoAnimated = false;
	boolean autoComputed = true;
	float kT = 0.5f; // frequency flips per second [0.1,10]
	//
	float currentAnimationTime = 0f; // current time during right to left
										// page curl

	// ("turn to next page"), normalized to [0,1]
	//
	// Vector3 apex = new Vector3(0, 0, -3);
	float apex = -1; // cone apex (z = [-20,0])
	float rho = -1; // page rotation [0,180] radians
	float theta = -1; // cone angle [90,1] radians

	ShaderProgram shaderColor;
	ShaderProgram shaderNoColor;

	Camera camera;

	public float zIndex;

	PageCurler(ShaderProgram sColor, ShaderProgram sNoColor, Camera c, float z,
			boolean singleQuadMesh) {

		shaderColor = sColor;
		shaderNoColor = sNoColor;
		camera = c;

		zIndex = z;

		plane = new Plane(new Vector3(0, 0, z), new Vector3(1, 0, z),
				new Vector3(0, 1, z));

		page = new PageMesh(singleQuadMesh);
		updateCurlMesh();
	}

	float apexFactor = 15;

	void reset() {
		rho = 0;
		theta = (float) Math.toRadians(90);
		apex = -15 * apexFactor;
	}

	void updateDeformParameters(float animationTime) {
		// This method computes rho, theta, and A for time parameter t using
		// pre-defined functions to simulate a natural page turn
		// without finger tracking, i.e., for a quick swipe of the finger to
		// turn to the next page.
		// These functions were constructed empirically by breaking down a
		// page turn into phases and experimenting with trial and error
		// until we got acceptable results. This basic example consists of
		// three distinct phases, but a more elegant solution yielding
		// smoother transitions can be obtained by curve fitting functions
		// to our key data points once satisfied with the behavior.

		float angle1 = (float) Math.toRadians(90);
		float angle2 = (float) Math.toRadians(8);
		float angle3 = (float) Math.toRadians(6);
		float A1 = -15.0f * apexFactor;
		float A2 = -2.5f * apexFactor;
		float A3 = -3.5f * apexFactor;
		float theta1 = 0.05f;
		float theta2 = 0.5f;
		float theta3 = 10.0f;
		float theta4 = 2.0f;

		// Here rho, the angle of the page rotation around the spine, is a
		// linear function of time t. This is the simplest case and looks
		// Good Enough. A side effect is that due to the curling effect, the
		// page appears to accelerate quickly at the beginning
		// of the turn, then slow down toward the end as the page uncurls
		// and returns to its natural form, just like in real life.
		// A non-linear function may be slightly more realistic but is
		// beyond the scope of this example.
		rho = animationTime * (float) Math.toRadians(180);

		if (animationTime == 0) {
			reset();
		} else if (animationTime <= 0.15f) {

			// Start off with a flat page with no deformation at the
			// beginning of a page turn, then begin to curl the page
			// gradually
			// as the hand lifts it off the surface of the book.
			float dt = animationTime / 0.15f;

			float f1 = (float) Math.sin(Math.PI * Math.pow(dt, theta1) / 2.0);
			float f2 = (float) Math.sin(Math.PI * Math.pow(dt, theta2) / 2.0);
			theta = linearInterpolation(f1, angle1, angle2);
			double theta_ = Math.toDegrees(theta);

			apex = linearInterpolation(f2, A1, A2);
			double apex_ = Math.toDegrees(apex);
		} else if (animationTime <= 0.4f) {

			// Produce the most pronounced curling near the middle of the
			// turn. Here small values of theta and A
			// result in a short, fat cone that distinctly show the curl
			// effect.
			float dt = (animationTime - 0.15f) / 0.25f;

			theta = linearInterpolation(dt, angle2, angle3);
			double theta_ = Math.toDegrees(theta);

			apex = linearInterpolation(dt, A2, A3);
			double apex_ = Math.toDegrees(apex);
		} else if (animationTime <= 1f) {

			// Near the middle of the turn, the hand has released the page
			// so it can return to its normal form.
			// Ease out the curl until it returns to a flat page at the
			// completion of the turn. More advanced simulations
			// could apply a slight wobble to the page as it falls down like
			// in real life.

			float dt = (animationTime - 0.4f) / 0.6f;
			float f1 = (float) Math.sin(Math.PI * Math.pow(dt, theta3) / 2.0);
			float f2 = (float) Math.sin(Math.PI * Math.pow(dt, theta4) / 2.0);

			theta = linearInterpolation(f1, angle3, angle1);
			double theta_ = Math.toDegrees(theta);

			apex = linearInterpolation(f2, A3, A1);
			double apex_ = Math.toDegrees(apex);
		}
	}

	void updateCurlMesh() {
		if (timeAppLaunch == -1)
			timeAppLaunch = System.nanoTime();

		if (autoAnimated) {
			float timeS = (System.nanoTime() - timeAppLaunch) / 1000000000.0f; // seconds

			if (currentAnimationTime <= 0.003f || currentAnimationTime >= 0.98f) {
				// wait 1s
				if (timeOpenClosePause == -1) {
					timeOpenClosePause = System.nanoTime();
					// Gdx.app.log(APP_NAME, "PAUSING " +
					// currentAnimationTime);
					return;
				} else {
					timeS = (System.nanoTime() - timeOpenClosePause) / 1000000000.0f; // seconds
					if (timeS >= 1f) {
						timeOpenClosePause = -1;
						timeAppLaunch = System.nanoTime();

						// Gdx.app.log(APP_NAME, "UNPAUSED "
						// + currentAnimationTime);

						if (currentAnimationTime > 0.5f) {
							currentAnimationTime = 0;
						} else {
							timeS = 1 / 30f; // 30fps
							currentAnimationTime = (kT * timeS) % 1f;
						}
						// Gdx.app.log(APP_NAME, "RESUMING "
						// + currentAnimationTime);
					} else {
						// Gdx.app.log(APP_NAME, "PAUSED " +
						// currentAnimationTime);
						return;
					}
				}
			} else {
				currentAnimationTime = (kT * timeS) % 1f;
			}
		}

		if (autoComputed) {
			updateDeformParameters(currentAnimationTime);
		}

		for (int i = 0; i < page.vertices.length; i++) {
			Vector3 verticeOriginal = page.verticesOriginal[i];
			Vector3 vertice = page.vertices[i];

			if (useColorShading)
				page.resetColorShading(i);

			// Radius of the circle circumscribed by vertex (v.x, v.y)
			// around apex on the x-y plane
			float radius_circle = (float) Math.sqrt(verticeOriginal.x
					* verticeOriginal.x
					+ Math.pow((verticeOriginal.y - apex), 2.0f));

			// From radius_circle, calculate the radius of the cone cross
			// section intersected by our vertex in 3D space.
			float radius_cone = radius_circle * (float) Math.sin(theta);

			// Angle SCT, the angle of the cone cross section subtended by
			// the arc |ST|.
			float beta = (float) Math.asin(verticeOriginal.x / radius_circle)
					/ (float) Math.sin(theta);

			float vx = radius_cone * (float) Math.sin(beta);
			float vy = radius_circle + apex - radius_cone
					* (1 - (float) Math.cos(beta)) * (float) Math.sin(theta);
			float vz =
			// verticeOriginal.z +
			// TODO: testing only (page backface preserves
			// z-offset)
			radius_cone * (1 - (float) Math.cos(beta))
					* (float) Math.cos(theta);
			// Apply a basic rotation transform around the y axis to rotate
			// the curled page.
			// These two steps could be combined through simple
			// substitution, but are left separate to keep the math simple
			// for debugging and illustrative purposes.
			vertice.x = // scale *
			((vx * (float) Math.cos(rho) - vz * (float) Math.sin(rho)));
			vertice.y = // scale *
			(vy);
			vertice.z = // scale *
			(verticeOriginal.z + (vx * (float) Math.sin(rho) + vz
					* (float) Math.cos(rho)));

			// page._vertices[i] = vertice;
		}

		page.updateMeshVertices(false);
	}

	float linearInterpolation(float t, float f0, float f1) {
		// Linear interpolation between f0 and f1
		return f0 + (f1 - f0) * t;
	}

	float quadraticInterpolation(float t, float f0, float f1) {
		// Quadratic interpolation between f0 and f1
		return f0 + (f1 - f0) * t * t;
	}

	float exponentialInterpolation(float t, float f0, float f1, float p) {
		// Exponential interpolation between f0 and f1
		return f0 + (f1 - f0) * (float) Math.pow(t, p);
	}

	final static boolean useColorShading = false;

	class PageMesh {

		public float right = 0;
		public float left = 0;
		public float top = 0;
		public float bottom = 0;

		Vector3 point3 = new Vector3();
		Vector2 point2 = new Vector2();
		Vector2 point2_ = new Vector2();

		public Vector2 cylinderPosition = new Vector2();
		public Vector2 cylinderDirection = new Vector2();
		public float cylinderRadius = 0; // / scale;
		public float cylinderAngle = 0;

		public void untouchScreen(int x, int y) {
			startPoint = null;
		}

		public void resetColorShading(int i) {

			if (!useColorShading)
				return;

			colors[i] = Color.WHITE;
			colors[i] = interpolate(colorDark, Color.WHITE, uvs[i].x);
		}

		public boolean touchScreen(int x, int y) {
			Ray ray = camera.getPickRay(x, y);
			Intersector.intersectRayPlane(ray, plane, point3);
			return touchPage(point2_.set(point3.x // / scale
					, point3.y // / scale
					));
		}

		Vector3 startPoint = null;

		boolean touchPage(Vector2 point) {
			float width = right - left;
			float height = top - bottom;

			if (startPoint == null) {

				if (point.x < left || point.x > right || point.y < bottom
						|| point.y > top)
					return false;

				// startPoint = new Vector2(point.x, point.y);
				//

				if (point.x > right - width / 2) {
					startPoint = new Vector3(point.x, point.y, right);
				} else {
					startPoint = new Vector3(point.x, point.y, left);
				}
			}

			point2.set(startPoint.x - point.x, startPoint.y - point.y); // dir

			boolean reset = false;
			// if (startPoint.z == right && point2.x <= 0)
			// reset = true;
			// if (startPoint.z == left && point2.x >= 0)
			// reset = true;

			if (reset) {
				cylinderAngle = (float) Math.PI / 4;
				cylinderDirection.set((float) Math.cos(cylinderAngle),
						(float) Math.sin(cylinderAngle));
				cylinderPosition.set(right - width / 4, bottom + height / 2.2f);
				cylinderRadius = (right - left) / 26; // / scale;
			} else {
				cylinderPosition.set(point);

				// point2.set(-point2.y, point2.x);
				float length = (float) Math.sqrt(point2.x * point2.x + point2.y
						* point2.y);
				// point2.x /= length;
				// point2.y /= length;
				// float angle = (float) Math.atan2(point2.y, point2.x);
				cylinderAngle = (float) Math.atan2(point2.x / length, -point2.y
						/ length);

				cylinderDirection.set((float) Math.cos(cylinderAngle),
						(float) Math.sin(cylinderAngle));

				float maxRadius = (right - left) / 18;
				cylinderRadius = maxRadius;
				if (length != 0)
					cylinderRadius /= length / 10;
				if (cylinderRadius > maxRadius)
					cylinderRadius = maxRadius;

				// System.out.println(length);
			}
			deformMesh();

			return true;
		}

		void deformMesh() {
			for (int i = 0; i < vertices.length; i++) {
				Vector3 verticeOriginal = verticesOriginal[i];
				Vector3 vertice = vertices[i];
				vertice.set(verticeOriginal); // v
				vertice.z = 0; // zIndex

				point2.set(cylinderDirection.y, -cylinderDirection.x); // n

				point2_.set(vertice.x - cylinderPosition.x, vertice.y
						- cylinderPosition.y); // w

				float d = point2_.dot(point2);
				// v_normal = vec3(0.0, 0.0, 1.0);

				float length = (float) Math.PI * cylinderRadius;

				//
				// if (useColorShading) {
				// // float rotY = (float) Math.PI * (v.mPos.x / length);
				// float rotY = vertice.x / cylinderRadius;
				// float color = 0.1f + 0.9f * (float) Math.sqrt(Math
				// .sin(rotY) + 1);
				// colors[i].a = 1;
				// colors[i].r = color;
				// colors[i].g = color;
				// colors[i].b = color;
				//
				// // System.out.println("COLOR: " + color);
				// }

				if (d > 0.0) {
					if (d > length) {
						float factor = 2 * d - length;
						vertice.x -= point2.x * factor;
						vertice.y -= point2.y * factor;
						vertice.z = 2 * cylinderRadius;
						// v_normal = vec3(0.0, 0.0, -1.0);
					} else {
						float dr = d / cylinderRadius;
						float s = (float) Math.sin(dr);
						float c = (float) Math.cos(dr);

						float projx = vertice.x - point2.x * d;
						float projy = vertice.y - point2.y * d;

						vertice.set(s * point2.x * cylinderRadius, s * point2.y
								* cylinderRadius, (1 - c) * cylinderRadius);

						vertice.x += projx;
						vertice.y += projy;

						// vec3 center = vec3(proj, cylinderRadius);
						// v_normal = (center - v.xyz) / u_cylinderRadius;
						/*
						 * if (dr < M_PI/2.0) { //lower part of curl v_normal =
						 * -v_normal; }
						 */
					}
				}
				// vertice.x *= scale;
				// vertice.y *= scale;
				// vertice.z *= scale;

				vertice.z += zIndex;
			}

			updateMeshVertices(false);
		}

		int nVertices;
		Vector3[] vertices;
		Vector3[] verticesOriginal;

		Vector2[] uvs;
		Color[] colors;

		int nIndices;
		short[] indices;

		int meshVerticeStride = useColorShading ? 8 : 7;
		float[] meshVertices;

		final boolean doubleMeshLayer = false;
		final boolean doublePageTexture = false;
		final boolean useTriangleStrips = true;

		Mesh mesh;

		Color colorDark = new Color(Color.WHITE);

		float quadSizeX = 1.4f;
		float quadSizeY = quadSizeX;

		PageMesh(boolean singleQuadMesh) {

			colorDark.mul(0.2f);
			colorDark.a = 1;
			int nx = 12;
			int ny = 16;

			if (singleQuadMesh) {
				quadSizeX = nx * quadSizeX;
				quadSizeY = ny * quadSizeY;
				createMesh(1, 1);
			} else
				createMesh(nx, ny);
		}

		void createMesh(int nColumns, int nRows) {

			int nVerticesX = nColumns + 1;
			int nVerticesY = nRows + 1;

			int nVerticesPerPageSide = nVerticesY * nVerticesX;
			int nQuadsPerPageSide = nRows * nColumns;

			nVertices = nVerticesPerPageSide * (doubleMeshLayer ? 2 : 1);

			vertices = new Vector3[nVertices];
			verticesOriginal = new Vector3[nVertices];
			uvs = new Vector2[nVertices];
			if (useColorShading)
				colors = new Color[nVertices];

			// basic geometry

			// float width = nColumns; // / 10f;
			// float height = nRows; // / 10f;

			final int yOffset = 0;
			int idxVertice = 0;
			for (int idxLayer = 0; idxLayer < (doubleMeshLayer ? 2 : 1); idxLayer++) {

				for (int idxY = 0; idxY < nVerticesY; idxY++) {

					for (int idxX = 0; idxX < nVerticesX; idxX++) {

						vertices[idxVertice] = new Vector3(//
								quadSizeX * idxX, // * width / nColumns, // X
								yOffset // / scale
										+ quadSizeY * idxY, // * height / nRows,
															// // Y
								zIndex // / scale
										+ (idxLayer / 2f * -1)); // Z

						verticesOriginal[idxVertice] = vertices[idxVertice]
								.cpy();

						if (idxX == nVerticesX - 1)
							right = vertices[idxVertice].x;
						if (idxY == nVerticesY - 1)
							top = vertices[idxVertice].y;
						if (idxX == 0)
							left = vertices[idxVertice].x;
						if (idxY == 0)
							bottom = vertices[idxVertice].y;

						float u = idxX
								/ (float) (nColumns * (doubleMeshLayer
										&& doublePageTexture ? 2 : 1));

						float v = 1f - (idxY / (float) nRows);

						// TODO: these UVs are not valid for SubTexture bindings
						// !!
						uvs[idxVertice] = new Vector2( //
								idxLayer == 0 ? u : 1f - u, // back side mesh,
															// x-flip
								v);

						if (useColorShading)
							if (doubleMeshLayer) {
								if (idxLayer != 0) { // back side inverted
														// gradient
									colors[idxVertice] = interpolate(colorDark,
											Color.WHITE, u);
								} else {
									colors[idxVertice] = interpolate(
											Color.WHITE, colorDark, u);
								}
							} else {
								colors[idxVertice] = Color.WHITE;
								colors[idxVertice] = interpolate(colorDark,
										Color.WHITE, u);
							}

						idxVertice++;
					}
				}
			}

			cylinderPosition.set(right, top - (top - bottom) / 2);

			// tesselation

			if (!useTriangleStrips) { // GL_TRIANGLES

				final int triangles_stride = 6; // 2 triangles per face = 6
												// vertices

				nIndices = 2 * (nQuadsPerPageSide * triangles_stride);

				// vertices, page recto + verso
				indices = new short[nIndices];

				for (int idxQuad = 0; idxQuad < nQuadsPerPageSide; idxQuad++) {
					int idxQuadTriangles = idxQuad * triangles_stride;

					int rowNum = idxQuad / nColumns;
					int colNum = idxQuad % nColumns;

					int ll = rowNum * nVerticesX + colNum;
					int lr = ll + 1;
					int ul = (rowNum + 1) * nVerticesX + colNum;
					int ur = ul + 1;

					// Front side of the page is winded counter-clockwise

					// _indices[vi + 0] = (short) ur;
					// _indices[vi + 1] = (short) ll;
					// _indices[vi + 2] = (short) lr;
					//
					// _indices[vi + 3] = (short) ur;
					// _indices[vi + 4] = (short) ul;
					// _indices[vi + 5] = (short) ll;

					indices[idxQuadTriangles + 0] = (short) lr;
					indices[idxQuadTriangles + 1] = (short) ul;
					indices[idxQuadTriangles + 2] = (short) ll;

					indices[idxQuadTriangles + 3] = (short) lr;
					indices[idxQuadTriangles + 4] = (short) ur;
					indices[idxQuadTriangles + 5] = (short) ul;

					int versoOffset = idxQuadTriangles + nIndices / 2;

					if (doubleMeshLayer) {

						ll += nVerticesPerPageSide;
						lr = ll + 1;
						ul += nVerticesPerPageSide;
						ur = ul + 1;

						// Back side of the page is winded clockwise

						indices[versoOffset + 0] = (short) ll;
						indices[versoOffset + 1] = (short) ur;
						indices[versoOffset + 2] = (short) lr;

						indices[versoOffset + 3] = (short) ll;
						indices[versoOffset + 4] = (short) ul;
						indices[versoOffset + 5] = (short) ur;
					} else {
						// Back side of the page is winded clockwise

						indices[versoOffset + 0] = (short) lr;
						indices[versoOffset + 1] = (short) ll;
						indices[versoOffset + 2] = (short) ul;

						indices[versoOffset + 3] = (short) lr;
						indices[versoOffset + 4] = (short) ul;
						indices[versoOffset + 5] = (short) ur;
					}
				}
			} else { // GL_TRIANGLE_STRIP

				// 1 page recto + verso
				nIndices = 2 * ((2 * (nVerticesX * nRows)) + (1 * (nRows - 1)));

				indices = new short[nIndices];

				// For the front side of the page, go right to left for odd
				// rows, left to right for even rows. Weaving back and forth
				// rather than always restarting each row on the same side
				// allows the graphics hardware to reuse cached vertex
				// calculations. Build the back at the same time by scanning in
				// reverse.

				idxVertice = 0;
				for (int idxRow = 0; idxRow < nRows; idxRow++) {

					int val = 0;
					int versoOffset = idxVertice + nIndices / 2;

					int rowOffset = idxRow * nVerticesX;
					boolean isOddRow = (idxRow % 2) != 0;

					for (int idxX = 0; idxX < nVerticesX; idxX++) {

						if (isOddRow) {
							val = rowOffset + nColumns - idxX;
							indices[idxVertice] = (short) (val + nVerticesX);
							idxVertice++;
							indices[idxVertice] = (short) (val);
							idxVertice++;

							val = rowOffset + idxX;
							indices[versoOffset] = (short) (val + nVerticesX);
							versoOffset++;
							indices[versoOffset] = (short) (val);
							versoOffset++;
						} else {
							val = rowOffset + idxX;
							indices[idxVertice] = (short) (val + nVerticesX);
							idxVertice++;
							indices[idxVertice] = (short) (val);
							idxVertice++;

							val = rowOffset + nColumns - idxX;
							indices[versoOffset] = (short) (val + nVerticesX);
							versoOffset++;
							indices[versoOffset] = (short) (val);
							versoOffset++;
						}
					}
					if (idxRow < nRows - 1) { // not last row

						// insert a degenerate vertex to connect to the next
						// adjacent row.
						if (isOddRow) {
							val = rowOffset + nVerticesX;
							indices[idxVertice] = (short) (val);
							idxVertice++;

							indices[versoOffset] = (short) (val + nColumns);
							versoOffset++;
						} else {
							val = rowOffset + nVerticesX;
							indices[idxVertice] = (short) (val + nColumns);
							idxVertice++;

							indices[versoOffset] = (short) (val);
							versoOffset++;
						}
					}
				}
			}

			if (useColorShading)
				mesh = new Mesh(true, nVertices, nIndices, new VertexAttribute(
						Usage.Position, 3, ShaderProgram.POSITION_ATTRIBUTE), // 3
																				// floats
						new VertexAttribute(Usage.ColorPacked, 4,
								ShaderProgram.COLOR_ATTRIBUTE), // 1
						// float
						new VertexAttribute(Usage.TextureCoordinates, 2,
								ShaderProgram.TEXCOORD_ATTRIBUTE + "0"), // 2
																			// floats
						new VertexAttribute(Usage.TextureCoordinates, 2,
								ShaderProgram.TEXCOORD_ATTRIBUTE + "1") // 2
																		// floats
				);
			else
				mesh = new Mesh(true, nVertices, nIndices, new VertexAttribute(
						Usage.Position, 3, ShaderProgram.POSITION_ATTRIBUTE), // 3
																				// floats
						new VertexAttribute(Usage.TextureCoordinates, 2,
								ShaderProgram.TEXCOORD_ATTRIBUTE + "0"), // 2
																			// floats
						new VertexAttribute(Usage.TextureCoordinates, 2,
								ShaderProgram.TEXCOORD_ATTRIBUTE + "1") // 2
																		// floats
				);

			mesh.setIndices(indices);

			meshVertices = new float[nVertices * meshVerticeStride];
			updateMeshVertices(false);
		}

		void updateMeshVertices(boolean onlyXYZ) {

			int j = -1;
			int offset = -1;

			for (int idxVertice = 0; idxVertice < nVertices; idxVertice++) {

				j = idxVertice * meshVerticeStride;
				offset = -1;

				++offset;
				meshVertices[j + offset] = vertices[idxVertice].x;
				++offset;
				meshVertices[j + offset] = vertices[idxVertice].y;
				++offset;
				meshVertices[j + offset] = vertices[idxVertice].z;

				if (!onlyXYZ) {
					if (useColorShading) {
						++offset;
						meshVertices[j + offset] = colors[idxVertice]
								.toFloatBits();
					}

					++offset;
					meshVertices[j + offset] = uvs[idxVertice].x;
					++offset;
					meshVertices[j + offset] = uvs[idxVertice].y;

					++offset;
					meshVertices[j + offset] = uvs[idxVertice].x;
					++offset;
					meshVertices[j + offset] = uvs[idxVertice].y;
				} else {
					++offset;
					++offset;
					++offset;
				}
			}

			mesh.setVertices(meshVertices);
		}

		void render(SubTexture pageBackground, SubTexture contentTexture1,
				SubTexture contentTexture2, TextureBindTracker bindtracker,
				Matrix4 matrix, boolean wireframe) {

			GLCommon gl = null;
			if (Gdx.graphics.isGL20Available())
				gl = Gdx.gl20;
			else
				gl = Gdx.gl10;

			// GL10 gl = Gdx.gl10;

			gl.glLineWidth(1.0f);

			int offset = nIndices / 2;
			int count = nIndices - offset;

			if (!Gdx.graphics.isGL11Available()
					&& !Gdx.graphics.isGL20Available()) {

				if (true)
					throw new RuntimeException("ANCIENT OPENGL DEVICE ??");

				gl.glActiveTexture(GL10.GL_TEXTURE0 + 0);
				gl.glEnable(GL10.GL_TEXTURE_2D);

				if (!Gdx.graphics.isGL20Available()) {
					Gdx.gl10.glColor4f(1, 1, 1, 1);
				}

				gl.glEnable(GL10.GL_BLEND);

				gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA); // usual
																				// alpha
																				// blending
				// gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ZERO); //
				// transparent
				// area
				// becomes
				// black

				// Doesn't seem to do anything ?
				// GL11.GL_CONSTANT_COLOR ==> lacking !
				// final float[] factor = { 1, 1, 1, 1f };
				// gl11.glTexEnvfv(GL10.GL_TEXTURE_ENV,
				// GL10.GL_TEXTURE_ENV_COLOR, factor, 0);
				// gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL11.GL_CONSTANT);
				// //GL11.GL_CONSTANT_COLOR ??

				bindtracker.bind(pageBackground);

				if (doublePageTexture) {
					if (useTriangleStrips) {
						if (!Gdx.graphics.isGL20Available()) {
							mesh.render(wireframe ? GL10.GL_LINE_STRIP
									: GL10.GL_TRIANGLE_STRIP);
						} else {
							(useColorShading ? shaderColor : shaderNoColor)
									.begin();
							(useColorShading ? shaderColor : shaderNoColor)
									.setUniformi(
											LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_X_UNIFORM + 0,
											0);
							(useColorShading ? shaderColor : shaderNoColor)
									.setUniformi(
											LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_Y_UNIFORM + 0,
											0);
							(useColorShading ? shaderColor : shaderNoColor)
									.setUniformi(
											LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_X_UNIFORM + 1,
											0);
							(useColorShading ? shaderColor : shaderNoColor)
									.setUniformi(
											LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_Y_UNIFORM + 1,
											0);
							(useColorShading ? shaderColor : shaderNoColor)
									.setUniformMatrix(
											LibGDXPlayground.ShaderProgram_PROJECTION_VIEW_MATRIX_UNIFORM,
											matrix);
							if (!useColorShading)
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformf(
												LibGDXPlayground.ShaderProgram_VERTEX_COLOR_UNIFORM,
												1, 1, 1, 1);
							(useColorShading ? shaderColor : shaderNoColor)
									.setUniformi(
											LibGDXPlayground.ShaderProgram_TEXTURE_SAMPLER_UNIFORM + 0,
											0);
							(useColorShading ? shaderColor : shaderNoColor)
									.setUniformi(
											LibGDXPlayground.ShaderProgram_TEXTURE_SAMPLER_UNIFORM + 1,
											1);

							if (pageBackground.flipX) {
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformi(
												LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_X_UNIFORM + 0,
												1);
							}
							if (pageBackground.flipY) {
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformi(
												LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_Y_UNIFORM + 0,
												1);
							}

							mesh.render((useColorShading ? shaderColor
									: shaderNoColor),
									wireframe ? GL10.GL_LINE_STRIP
											: GL10.GL_TRIANGLE_STRIP);

							(useColorShading ? shaderColor : shaderNoColor)
									.end();
						}
					} else {
						if (!Gdx.graphics.isGL20Available()) {
							mesh.render(wireframe ? GL10.GL_LINE_STRIP
									: GL10.GL_TRIANGLES);
						} else {
							(useColorShading ? shaderColor : shaderNoColor)
									.begin();
							(useColorShading ? shaderColor : shaderNoColor)
									.setUniformi(
											LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_X_UNIFORM + 0,
											0);
							(useColorShading ? shaderColor : shaderNoColor)
									.setUniformi(
											LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_Y_UNIFORM + 0,
											0);
							(useColorShading ? shaderColor : shaderNoColor)
									.setUniformi(
											LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_X_UNIFORM + 1,
											0);
							(useColorShading ? shaderColor : shaderNoColor)
									.setUniformi(
											LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_Y_UNIFORM + 1,
											0);
							(useColorShading ? shaderColor : shaderNoColor)
									.setUniformMatrix(
											LibGDXPlayground.ShaderProgram_PROJECTION_VIEW_MATRIX_UNIFORM,
											matrix);
							if (!useColorShading)
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformf(
												LibGDXPlayground.ShaderProgram_VERTEX_COLOR_UNIFORM,
												1, 1, 1, 1);
							(useColorShading ? shaderColor : shaderNoColor)
									.setUniformi(
											LibGDXPlayground.ShaderProgram_TEXTURE_SAMPLER_UNIFORM + 0,
											0);
							(useColorShading ? shaderColor : shaderNoColor)
									.setUniformi(
											LibGDXPlayground.ShaderProgram_TEXTURE_SAMPLER_UNIFORM + 1,
											1);
							if (pageBackground.flipX) {
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformi(
												LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_X_UNIFORM + 0,
												1);
							}
							if (pageBackground.flipY) {
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformi(
												LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_Y_UNIFORM + 0,
												1);
							}

							mesh.render((useColorShading ? shaderColor
									: shaderNoColor),
									wireframe ? GL10.GL_LINE_STRIP
											: GL10.GL_TRIANGLES);

							(useColorShading ? shaderColor : shaderNoColor)
									.end();
						}
					}
				} else {
					if (useTriangleStrips) {
						if (!Gdx.graphics.isGL20Available()) {
							mesh.render(wireframe ? GL10.GL_LINE_STRIP
									: GL10.GL_TRIANGLE_STRIP, 0, offset);
						} else {
							(useColorShading ? shaderColor : shaderNoColor)
									.begin();
							(useColorShading ? shaderColor : shaderNoColor)
									.setUniformi(
											LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_X_UNIFORM + 0,
											0);
							(useColorShading ? shaderColor : shaderNoColor)
									.setUniformi(
											LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_Y_UNIFORM + 0,
											0);
							(useColorShading ? shaderColor : shaderNoColor)
									.setUniformi(
											LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_X_UNIFORM + 1,
											0);
							(useColorShading ? shaderColor : shaderNoColor)
									.setUniformi(
											LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_Y_UNIFORM + 1,
											0);
							(useColorShading ? shaderColor : shaderNoColor)
									.setUniformMatrix(
											LibGDXPlayground.ShaderProgram_PROJECTION_VIEW_MATRIX_UNIFORM,
											matrix);
							if (!useColorShading)
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformf(
												LibGDXPlayground.ShaderProgram_VERTEX_COLOR_UNIFORM,
												1, 1, 1, 1);
							(useColorShading ? shaderColor : shaderNoColor)
									.setUniformi(
											LibGDXPlayground.ShaderProgram_TEXTURE_SAMPLER_UNIFORM + 0,
											0);
							(useColorShading ? shaderColor : shaderNoColor)
									.setUniformi(
											LibGDXPlayground.ShaderProgram_TEXTURE_SAMPLER_UNIFORM + 1,
											1);
							if (pageBackground.flipX) {
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformi(
												LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_X_UNIFORM + 0,
												1);
							}
							if (pageBackground.flipY) {
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformi(
												LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_Y_UNIFORM + 0,
												1);
							}

							mesh.render((useColorShading ? shaderColor
									: shaderNoColor),
									wireframe ? GL10.GL_LINE_STRIP
											: GL10.GL_TRIANGLE_STRIP, 0, offset);

							(useColorShading ? shaderColor : shaderNoColor)
									.end();
						}
					} else {
						if (!Gdx.graphics.isGL20Available()) {
							mesh.render(wireframe ? GL10.GL_LINE_STRIP
									: GL10.GL_TRIANGLES, 0, offset);
						} else {
							(useColorShading ? shaderColor : shaderNoColor)
									.begin();
							(useColorShading ? shaderColor : shaderNoColor)
									.setUniformi(
											LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_X_UNIFORM + 0,
											0);
							(useColorShading ? shaderColor : shaderNoColor)
									.setUniformi(
											LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_Y_UNIFORM + 0,
											0);
							(useColorShading ? shaderColor : shaderNoColor)
									.setUniformi(
											LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_X_UNIFORM + 1,
											0);
							(useColorShading ? shaderColor : shaderNoColor)
									.setUniformi(
											LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_Y_UNIFORM + 1,
											0);
							(useColorShading ? shaderColor : shaderNoColor)
									.setUniformMatrix(
											LibGDXPlayground.ShaderProgram_PROJECTION_VIEW_MATRIX_UNIFORM,
											matrix);
							if (!useColorShading)
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformf(
												LibGDXPlayground.ShaderProgram_VERTEX_COLOR_UNIFORM,
												1, 1, 1, 1);
							(useColorShading ? shaderColor : shaderNoColor)
									.setUniformi(
											LibGDXPlayground.ShaderProgram_TEXTURE_SAMPLER_UNIFORM + 0,
											0);
							(useColorShading ? shaderColor : shaderNoColor)
									.setUniformi(
											LibGDXPlayground.ShaderProgram_TEXTURE_SAMPLER_UNIFORM + 1,
											1);
							if (pageBackground.flipX) {
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformi(
												LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_X_UNIFORM + 0,
												1);
							}
							if (pageBackground.flipY) {
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformi(
												LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_Y_UNIFORM + 0,
												1);
							}

							mesh.render((useColorShading ? shaderColor
									: shaderNoColor),
									wireframe ? GL10.GL_LINE_STRIP
											: GL10.GL_TRIANGLES, 0, offset);

							(useColorShading ? shaderColor : shaderNoColor)
									.end();
						}
					}
				}

				gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA); // usual
																				// alpha
																				// blending

				if (contentTexture1 != null) {
					// bindtracker.bind(contentTexture1);
					contentTexture1.texture.bind();

					if (doublePageTexture) {
						if (useTriangleStrips) {
							if (!Gdx.graphics.isGL20Available()) {
								mesh.render(wireframe ? GL10.GL_LINE_STRIP
										: GL10.GL_TRIANGLE_STRIP);
							} else {
								(useColorShading ? shaderColor : shaderNoColor)
										.begin();
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformi(
												LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_X_UNIFORM + 0,
												0);
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformi(
												LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_Y_UNIFORM + 0,
												0);
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformi(
												LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_X_UNIFORM + 1,
												0);
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformi(
												LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_Y_UNIFORM + 1,
												0);
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformMatrix(
												LibGDXPlayground.ShaderProgram_PROJECTION_VIEW_MATRIX_UNIFORM,
												matrix);
								if (!useColorShading)
									(useColorShading ? shaderColor
											: shaderNoColor)
											.setUniformf(
													LibGDXPlayground.ShaderProgram_VERTEX_COLOR_UNIFORM,
													1, 1, 1, 1);
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformi(
												LibGDXPlayground.ShaderProgram_TEXTURE_SAMPLER_UNIFORM + 0,
												0);
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformi(
												LibGDXPlayground.ShaderProgram_TEXTURE_SAMPLER_UNIFORM + 1,
												1);
								if (contentTexture1.flipX) {
									(useColorShading ? shaderColor
											: shaderNoColor)
											.setUniformi(
													LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_X_UNIFORM + 0,
													1);
								}
								if (contentTexture1.flipY) {
									(useColorShading ? shaderColor
											: shaderNoColor)
											.setUniformi(
													LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_Y_UNIFORM + 0,
													1);
								}

								mesh.render((useColorShading ? shaderColor
										: shaderNoColor),
										wireframe ? GL10.GL_LINE_STRIP
												: GL10.GL_TRIANGLE_STRIP);

								(useColorShading ? shaderColor : shaderNoColor)
										.end();
							}
						} else {
							if (!Gdx.graphics.isGL20Available()) {
								mesh.render(wireframe ? GL10.GL_LINE_STRIP
										: GL10.GL_TRIANGLES);
							} else {
								(useColorShading ? shaderColor : shaderNoColor)
										.begin();
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformi(
												LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_X_UNIFORM + 0,
												0);
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformi(
												LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_Y_UNIFORM + 0,
												0);
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformi(
												LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_X_UNIFORM + 1,
												0);
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformi(
												LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_Y_UNIFORM + 1,
												0);
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformMatrix(
												LibGDXPlayground.ShaderProgram_PROJECTION_VIEW_MATRIX_UNIFORM,
												matrix);
								if (!useColorShading)
									(useColorShading ? shaderColor
											: shaderNoColor)
											.setUniformf(
													LibGDXPlayground.ShaderProgram_VERTEX_COLOR_UNIFORM,
													1, 1, 1, 1);
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformi(
												LibGDXPlayground.ShaderProgram_TEXTURE_SAMPLER_UNIFORM + 0,
												0);
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformi(
												LibGDXPlayground.ShaderProgram_TEXTURE_SAMPLER_UNIFORM + 1,
												1);
								if (contentTexture1.flipX) {
									(useColorShading ? shaderColor
											: shaderNoColor)
											.setUniformi(
													LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_X_UNIFORM + 0,
													1);
								}
								if (contentTexture1.flipY) {
									(useColorShading ? shaderColor
											: shaderNoColor)
											.setUniformi(
													LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_Y_UNIFORM + 0,
													1);
								}

								mesh.render((useColorShading ? shaderColor
										: shaderNoColor),
										wireframe ? GL10.GL_LINE_STRIP
												: GL10.GL_TRIANGLES);

								(useColorShading ? shaderColor : shaderNoColor)
										.end();
							}
						}
					} else {
						boolean adjustCoplanarOverlap = true;
						if (adjustCoplanarOverlap) {
							float polyfactor = -1f; // +/- 0.1
							float polyunits = -1f; // +/- 1;

							gl.glEnable(GL10.GL_POLYGON_OFFSET_FILL);
							gl.glPolygonOffset(polyfactor, polyunits);
						}

						if (useTriangleStrips) {
							if (!Gdx.graphics.isGL20Available()) {
								mesh.render(wireframe ? GL10.GL_LINE_STRIP
										: GL10.GL_TRIANGLE_STRIP, 0, offset);
							} else {
								(useColorShading ? shaderColor : shaderNoColor)
										.begin();
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformi(
												LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_X_UNIFORM + 0,
												0);
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformi(
												LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_Y_UNIFORM + 0,
												0);
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformi(
												LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_X_UNIFORM + 1,
												0);
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformi(
												LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_Y_UNIFORM + 1,
												0);
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformMatrix(
												LibGDXPlayground.ShaderProgram_PROJECTION_VIEW_MATRIX_UNIFORM,
												matrix);
								if (!useColorShading)
									(useColorShading ? shaderColor
											: shaderNoColor)
											.setUniformf(
													LibGDXPlayground.ShaderProgram_VERTEX_COLOR_UNIFORM,
													1, 1, 1, 1);
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformi(
												LibGDXPlayground.ShaderProgram_TEXTURE_SAMPLER_UNIFORM + 0,
												0);
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformi(
												LibGDXPlayground.ShaderProgram_TEXTURE_SAMPLER_UNIFORM + 1,
												1);
								if (contentTexture1.flipX) {
									(useColorShading ? shaderColor
											: shaderNoColor)
											.setUniformi(
													LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_X_UNIFORM + 0,
													1);
								}
								if (contentTexture1.flipY) {
									(useColorShading ? shaderColor
											: shaderNoColor)
											.setUniformi(
													LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_Y_UNIFORM + 0,
													1);
								}

								mesh.render((useColorShading ? shaderColor
										: shaderNoColor),
										wireframe ? GL10.GL_LINE_STRIP
												: GL10.GL_TRIANGLE_STRIP, 0,
										offset);

								(useColorShading ? shaderColor : shaderNoColor)
										.end();
							}
						} else {
							if (!Gdx.graphics.isGL20Available()) {
								mesh.render(wireframe ? GL10.GL_LINE_STRIP
										: GL10.GL_TRIANGLES, 0, offset);
							} else {
								(useColorShading ? shaderColor : shaderNoColor)
										.begin();
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformi(
												LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_X_UNIFORM + 0,
												0);
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformi(
												LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_Y_UNIFORM + 0,
												0);
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformi(
												LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_X_UNIFORM + 1,
												0);
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformi(
												LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_Y_UNIFORM + 1,
												0);
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformMatrix(
												LibGDXPlayground.ShaderProgram_PROJECTION_VIEW_MATRIX_UNIFORM,
												matrix);
								if (!useColorShading)
									(useColorShading ? shaderColor
											: shaderNoColor)
											.setUniformf(
													LibGDXPlayground.ShaderProgram_VERTEX_COLOR_UNIFORM,
													1, 1, 1, 1);
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformi(
												LibGDXPlayground.ShaderProgram_TEXTURE_SAMPLER_UNIFORM + 0,
												0);
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformi(
												LibGDXPlayground.ShaderProgram_TEXTURE_SAMPLER_UNIFORM + 1,
												1);
								if (contentTexture1.flipX) {
									(useColorShading ? shaderColor
											: shaderNoColor)
											.setUniformi(
													LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_X_UNIFORM + 0,
													1);
								}
								if (contentTexture1.flipY) {
									(useColorShading ? shaderColor
											: shaderNoColor)
											.setUniformi(
													LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_Y_UNIFORM + 0,
													1);
								}

								mesh.render((useColorShading ? shaderColor
										: shaderNoColor),
										wireframe ? GL10.GL_LINE_STRIP
												: GL10.GL_TRIANGLES, 0, offset);

								(useColorShading ? shaderColor : shaderNoColor)
										.end();
							}
						}

						if (adjustCoplanarOverlap) {
							gl.glDisable(GL10.GL_POLYGON_OFFSET_FILL);
						}
					}
				}

				if (!doublePageTexture) {

					bindtracker.bind(pageBackground);

					if (!doubleMeshLayer) { // flip-x
						if (Gdx.graphics.isGL20Available()) {

						} else {
							Gdx.gl10.glMatrixMode(GL10.GL_TEXTURE);
							Gdx.gl10.glPushMatrix();
							Gdx.gl10.glLoadIdentity();
							Gdx.gl10.glScalef(-1, 1, 1);
							// gl.glRotatef(45, 0, 0, 1);
							Gdx.gl10.glTranslatef(-1f, 0, 0);
						}
					}

					gl.glBlendFunc(GL10.GL_SRC_ALPHA,
							GL10.GL_ONE_MINUS_SRC_ALPHA); // usual alpha
															// blending
					// gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ZERO); //
					// transparent
					// area
					// becomes
					// black

					if (doublePageTexture) {
						/*
						 * if (useTriangleStrips) { if
						 * (!Gdx.graphics.isGL20Available()) {
						 * mesh.render(wireframe ? GL10.GL_LINE_STRIP :
						 * GL10.GL_TRIANGLE_STRIP); } else { (useColors ?
						 * shaderColor : shaderNoColor) .begin(); (useColors ?
						 * shaderColor : shaderNoColor) .setUniformMatrix(
						 * LibGDXPlayground.
						 * ShaderProgram_PROJECTION_VIEW_MATRIX_UNIFORM,
						 * matrix); (useColors ? shaderColor : shaderNoColor)
						 * .setUniformf(
						 * LibGDXPlayground.ShaderProgram_VERTEX_COLOR_UNIFORM,
						 * 1, 1, 1, 1); mesh.render((useColors ? shaderColor :
						 * shaderNoColor), wireframe ? GL10.GL_LINE_STRIP :
						 * GL10.GL_TRIANGLE_STRIP); (useColors ? shaderColor :
						 * shaderNoColor).end(); } } else { if
						 * (!Gdx.graphics.isGL20Available()) {
						 * mesh.render(wireframe ? GL10.GL_LINE_STRIP :
						 * GL10.GL_TRIANGLES); } else { (useColors ? shaderColor
						 * : shaderNoColor) .begin(); (useColors ? shaderColor :
						 * shaderNoColor) .setUniformMatrix( LibGDXPlayground
						 * .ShaderProgram_PROJECTION_VIEW_MATRIX_UNIFORM,
						 * matrix); (useColors ? shaderColor : shaderNoColor)
						 * .setUniformf(
						 * LibGDXPlayground.ShaderProgram_VERTEX_COLOR_UNIFORM,
						 * 1, 1, 1, 1); mesh.render((useColors ? shaderColor :
						 * shaderNoColor), wireframe ? GL10.GL_LINE_STRIP :
						 * GL10.GL_TRIANGLES); (useColors ? shaderColor :
						 * shaderNoColor).end(); } }
						 */
					} else {
						if (useTriangleStrips) {
							if (!Gdx.graphics.isGL20Available()) {
								mesh.render(wireframe ? GL10.GL_LINE_STRIP
										: GL10.GL_TRIANGLE_STRIP, offset, count);
							} else {
								(useColorShading ? shaderColor : shaderNoColor)
										.begin();
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformi(
												LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_X_UNIFORM + 0,
												0);
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformi(
												LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_Y_UNIFORM + 0,
												0);
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformi(
												LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_X_UNIFORM + 1,
												0);
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformi(
												LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_Y_UNIFORM + 1,
												0);
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformMatrix(
												LibGDXPlayground.ShaderProgram_PROJECTION_VIEW_MATRIX_UNIFORM,
												matrix);
								if (!useColorShading)
									(useColorShading ? shaderColor
											: shaderNoColor)
											.setUniformf(
													LibGDXPlayground.ShaderProgram_VERTEX_COLOR_UNIFORM,
													1, 1, 1, 1);
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformi(
												LibGDXPlayground.ShaderProgram_TEXTURE_SAMPLER_UNIFORM + 0,
												0);
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformi(
												LibGDXPlayground.ShaderProgram_TEXTURE_SAMPLER_UNIFORM + 1,
												1);

								if (!doubleMeshLayer) { // flip-x

									(useColorShading ? shaderColor
											: shaderNoColor)
											.setUniformi(
													LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_X_UNIFORM + 0,
													1);
								}
								if (pageBackground.flipX) {
									(useColorShading ? shaderColor
											: shaderNoColor)
											.setUniformi(
													LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_X_UNIFORM + 0,
													!doubleMeshLayer ? 0 : 1);
								}
								if (pageBackground.flipY) {
									(useColorShading ? shaderColor
											: shaderNoColor)
											.setUniformi(
													LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_Y_UNIFORM + 0,
													1);
								}

								mesh.render((useColorShading ? shaderColor
										: shaderNoColor),
										wireframe ? GL10.GL_LINE_STRIP
												: GL10.GL_TRIANGLE_STRIP,
										offset, count);

								(useColorShading ? shaderColor : shaderNoColor)
										.end();
							}
						} else {
							if (!Gdx.graphics.isGL20Available()) {
								mesh.render(wireframe ? GL10.GL_LINE_STRIP
										: GL10.GL_TRIANGLES, offset, count);
							} else {
								(useColorShading ? shaderColor : shaderNoColor)
										.begin();
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformi(
												LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_X_UNIFORM + 0,
												0);
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformi(
												LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_Y_UNIFORM + 0,
												0);
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformi(
												LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_X_UNIFORM + 1,
												0);
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformi(
												LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_Y_UNIFORM + 1,
												0);
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformMatrix(
												LibGDXPlayground.ShaderProgram_PROJECTION_VIEW_MATRIX_UNIFORM,
												matrix);
								if (!useColorShading)
									(useColorShading ? shaderColor
											: shaderNoColor)
											.setUniformf(
													LibGDXPlayground.ShaderProgram_VERTEX_COLOR_UNIFORM,
													1, 1, 1, 1);
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformi(
												LibGDXPlayground.ShaderProgram_TEXTURE_SAMPLER_UNIFORM + 0,
												0);
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformi(
												LibGDXPlayground.ShaderProgram_TEXTURE_SAMPLER_UNIFORM + 1,
												1);

								if (!doubleMeshLayer) { // flip-x

									(useColorShading ? shaderColor
											: shaderNoColor)
											.setUniformi(
													LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_X_UNIFORM + 0,
													1);

								}

								if (pageBackground.flipX) {
									(useColorShading ? shaderColor
											: shaderNoColor)
											.setUniformi(
													LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_X_UNIFORM + 0,
													!doubleMeshLayer ? 0 : 1);
								}
								if (pageBackground.flipY) {
									(useColorShading ? shaderColor
											: shaderNoColor)
											.setUniformi(
													LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_Y_UNIFORM + 0,
													1);
								}

								mesh.render((useColorShading ? shaderColor
										: shaderNoColor),
										wireframe ? GL10.GL_LINE_STRIP
												: GL10.GL_TRIANGLES, offset,
										count);

								(useColorShading ? shaderColor : shaderNoColor)
										.end();
							}
						}
					}

					if (!doubleMeshLayer) {
						if (Gdx.graphics.isGL20Available()) {

						} else {
							Gdx.gl10.glMatrixMode(GL10.GL_TEXTURE);
							Gdx.gl10.glPopMatrix();
							Gdx.gl10.glMatrixMode(GL10.GL_MODELVIEW);
						}
						// cameraSCENE.update();
					}

					gl.glBlendFunc(GL10.GL_SRC_ALPHA,
							GL10.GL_ONE_MINUS_SRC_ALPHA); // usual alpha
															// blending

					if (contentTexture2 != null) {
						// bindtracker.bind(contentTexture2);
						contentTexture2.texture.bind();

						if (!doubleMeshLayer) { // flip-x
							if (Gdx.graphics.isGL20Available()) {

							} else {
								Gdx.gl10.glMatrixMode(GL10.GL_TEXTURE);
								Gdx.gl10.glPushMatrix();
								Gdx.gl10.glLoadIdentity();
								Gdx.gl10.glScalef(-1, 1, 1);
								// gl.glRotatef(45, 0, 0, 1);
								Gdx.gl10.glTranslatef(-1f, 0, 0);
							}
						}
						boolean adjustCoplanarOverlap = true;
						if (adjustCoplanarOverlap) {
							float polyfactor = -1f; // +/- 0.1
							float polyunits = -1f; // +/- 1;

							gl.glEnable(GL10.GL_POLYGON_OFFSET_FILL);
							gl.glPolygonOffset(polyfactor, polyunits);
						}

						if (useTriangleStrips) {
							if (!Gdx.graphics.isGL20Available()) {
								mesh.render(wireframe ? GL10.GL_LINE_STRIP
										: GL10.GL_TRIANGLE_STRIP, offset, count);
							} else {
								(useColorShading ? shaderColor : shaderNoColor)
										.begin();
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformi(
												LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_X_UNIFORM + 0,
												0);
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformi(
												LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_Y_UNIFORM + 0,
												0);
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformi(
												LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_X_UNIFORM + 1,
												0);
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformi(
												LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_Y_UNIFORM + 1,
												0);
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformMatrix(
												LibGDXPlayground.ShaderProgram_PROJECTION_VIEW_MATRIX_UNIFORM,
												matrix);
								if (!useColorShading)
									(useColorShading ? shaderColor
											: shaderNoColor)
											.setUniformf(
													LibGDXPlayground.ShaderProgram_VERTEX_COLOR_UNIFORM,
													1, 1, 1, 1);
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformi(
												LibGDXPlayground.ShaderProgram_TEXTURE_SAMPLER_UNIFORM + 0,
												0);
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformi(
												LibGDXPlayground.ShaderProgram_TEXTURE_SAMPLER_UNIFORM + 1,
												1);
								if (!doubleMeshLayer) { // flip-x

									(useColorShading ? shaderColor
											: shaderNoColor)
											.setUniformi(
													LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_X_UNIFORM + 0,
													1);

								}

								if (contentTexture2.flipX) {
									(useColorShading ? shaderColor
											: shaderNoColor)
											.setUniformi(
													LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_X_UNIFORM + 0,
													!doubleMeshLayer ? 0 : 1);
								}
								if (contentTexture2.flipY) {
									(useColorShading ? shaderColor
											: shaderNoColor)
											.setUniformi(
													LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_Y_UNIFORM + 0,
													1);
								}
								mesh.render((useColorShading ? shaderColor
										: shaderNoColor),
										wireframe ? GL10.GL_LINE_STRIP
												: GL10.GL_TRIANGLE_STRIP,
										offset, count);

								(useColorShading ? shaderColor : shaderNoColor)
										.end();
							}
						} else {
							if (!Gdx.graphics.isGL20Available()) {
								mesh.render(wireframe ? GL10.GL_LINE_STRIP
										: GL10.GL_TRIANGLES, offset, count);
							} else {
								(useColorShading ? shaderColor : shaderNoColor)
										.begin();
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformi(
												LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_X_UNIFORM + 0,
												0);
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformi(
												LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_Y_UNIFORM + 0,
												0);
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformi(
												LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_X_UNIFORM + 1,
												0);
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformi(
												LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_Y_UNIFORM + 1,
												0);
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformMatrix(
												LibGDXPlayground.ShaderProgram_PROJECTION_VIEW_MATRIX_UNIFORM,
												matrix);
								if (!useColorShading)
									(useColorShading ? shaderColor
											: shaderNoColor)
											.setUniformf(
													LibGDXPlayground.ShaderProgram_VERTEX_COLOR_UNIFORM,
													1, 1, 1, 1);
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformi(
												LibGDXPlayground.ShaderProgram_TEXTURE_SAMPLER_UNIFORM + 0,
												0);
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformi(
												LibGDXPlayground.ShaderProgram_TEXTURE_SAMPLER_UNIFORM + 1,
												1);
								if (!doubleMeshLayer) { // flip-x

									(useColorShading ? shaderColor
											: shaderNoColor)
											.setUniformi(
													LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_X_UNIFORM + 0,
													1);

								}

								if (contentTexture2.flipX) {
									(useColorShading ? shaderColor
											: shaderNoColor)
											.setUniformi(
													LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_X_UNIFORM + 0,
													!doubleMeshLayer ? 0 : 1);
								}
								if (contentTexture2.flipY) {
									(useColorShading ? shaderColor
											: shaderNoColor)
											.setUniformi(
													LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_Y_UNIFORM + 0,
													1);
								}
								mesh.render((useColorShading ? shaderColor
										: shaderNoColor),
										wireframe ? GL10.GL_LINE_STRIP
												: GL10.GL_TRIANGLES, offset,
										count);

								(useColorShading ? shaderColor : shaderNoColor)
										.end();
							}
						}

						if (adjustCoplanarOverlap) {
							gl.glDisable(GL10.GL_POLYGON_OFFSET_FILL);
						}

						if (!doubleMeshLayer) {
							if (Gdx.graphics.isGL20Available()) {

							} else {
								Gdx.gl10.glMatrixMode(GL10.GL_TEXTURE);
								Gdx.gl10.glPopMatrix();
								Gdx.gl10.glMatrixMode(GL10.GL_MODELVIEW);
							}
							// cameraSCENE.update();
						}
					}
				}

				// -------------------------------------------------------
				// -------------------------------------------------------
				// -------------------------------------------------------
				// -------------------------------------------------------
			} else { // multitexturing ( http://www.andersriggelsen.dk/OpenGL/ )

				gl.glActiveTexture(GL10.GL_TEXTURE0 + 0);
				gl.glEnable(GL10.GL_TEXTURE_2D);

				if (!Gdx.graphics.isGL20Available()) {
					Gdx.gl10.glColor4f(1, 1, 1, 1);
				}

				gl.glEnable(GL10.GL_BLEND);

				gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
				// usual alpha blending

				// gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ZERO);
				// transparent area becomes black

				bindtracker.bind(pageBackground);

				if (contentTexture1 != null) {

					gl.glActiveTexture(GL10.GL_TEXTURE0 + 1);
					gl.glEnable(GL10.GL_TEXTURE_2D);

					if (!Gdx.graphics.isGL20Available()) {
						Gdx.gl10.glColor4f(1, 1, 1, 1);
					}

					gl.glEnable(GL10.GL_BLEND);
					// gl.glBlendFunc(GL10.GL_ONE, GL10.GL_ONE_MINUS_SRC_COLOR);
					gl.glBlendFunc(GL10.GL_SRC_ALPHA,
							GL10.GL_ONE_MINUS_SRC_ALPHA); // usual alpha
					// blending
					// gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ZERO); //
					// transparent area becomes black

					// bindtracker.bind(contentTexture1);
					contentTexture1.texture.bind();

					if (Gdx.graphics.isGL11Available()) {
						Gdx.gl11.glTexEnvi(GL10.GL_TEXTURE_ENV,
								GL10.GL_TEXTURE_ENV_MODE, GL11.GL_DECAL);
					}
				} else {
					gl.glActiveTexture(GL10.GL_TEXTURE0 + 1);
					gl.glDisable(GL10.GL_TEXTURE_2D);
				}

				if (doublePageTexture) {
					if (true)
						throw new RuntimeException("doublePageTexture?!");

					if (useTriangleStrips) {
						if (!Gdx.graphics.isGL20Available()) {
							mesh.render(wireframe ? GL10.GL_LINE_STRIP
									: GL10.GL_TRIANGLE_STRIP);
						} else {
							(useColorShading ? shaderColor : shaderNoColor)
									.begin();
							(useColorShading ? shaderColor : shaderNoColor)
									.setUniformi(
											LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_X_UNIFORM + 0,
											0);
							(useColorShading ? shaderColor : shaderNoColor)
									.setUniformi(
											LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_Y_UNIFORM + 0,
											0);
							(useColorShading ? shaderColor : shaderNoColor)
									.setUniformi(
											LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_X_UNIFORM + 1,
											0);
							(useColorShading ? shaderColor : shaderNoColor)
									.setUniformi(
											LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_Y_UNIFORM + 1,
											0);
							(useColorShading ? shaderColor : shaderNoColor)
									.setUniformMatrix(
											LibGDXPlayground.ShaderProgram_PROJECTION_VIEW_MATRIX_UNIFORM,
											matrix);
							if (!useColorShading)
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformf(
												LibGDXPlayground.ShaderProgram_VERTEX_COLOR_UNIFORM,
												1, 1, 1, 1);
							(useColorShading ? shaderColor : shaderNoColor)
									.setUniformi(
											LibGDXPlayground.ShaderProgram_TEXTURE_SAMPLER_UNIFORM + 0,
											0);
							(useColorShading ? shaderColor : shaderNoColor)
									.setUniformi(
											LibGDXPlayground.ShaderProgram_TEXTURE_SAMPLER_UNIFORM + 1,
											1);

							if (pageBackground.flipX) {
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformi(
												LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_X_UNIFORM + 0,
												1);
							}
							if (pageBackground.flipY) {
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformi(
												LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_Y_UNIFORM + 0,
												1);
							}
							if (contentTexture1 != null) {
								if (contentTexture1.flipX) {
									(useColorShading ? shaderColor
											: shaderNoColor)
											.setUniformi(
													LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_X_UNIFORM + 1,
													1);
								}
								if (contentTexture1.flipY) {
									(useColorShading ? shaderColor
											: shaderNoColor)
											.setUniformi(
													LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_Y_UNIFORM + 1,
													1);
								}
							}
							mesh.render((useColorShading ? shaderColor
									: shaderNoColor),
									wireframe ? GL10.GL_LINE_STRIP
											: GL10.GL_TRIANGLE_STRIP);

							(useColorShading ? shaderColor : shaderNoColor)
									.end();
						}
					} else {
						if (!Gdx.graphics.isGL20Available()) {
							mesh.render(wireframe ? GL10.GL_LINE_STRIP
									: GL10.GL_TRIANGLES);
						} else {
							(useColorShading ? shaderColor : shaderNoColor)
									.begin();
							(useColorShading ? shaderColor : shaderNoColor)
									.setUniformi(
											LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_X_UNIFORM + 0,
											0);
							(useColorShading ? shaderColor : shaderNoColor)
									.setUniformi(
											LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_Y_UNIFORM + 0,
											0);
							(useColorShading ? shaderColor : shaderNoColor)
									.setUniformi(
											LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_X_UNIFORM + 1,
											0);
							(useColorShading ? shaderColor : shaderNoColor)
									.setUniformi(
											LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_Y_UNIFORM + 1,
											0);
							(useColorShading ? shaderColor : shaderNoColor)
									.setUniformMatrix(
											LibGDXPlayground.ShaderProgram_PROJECTION_VIEW_MATRIX_UNIFORM,
											matrix);
							if (!useColorShading)
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformf(
												LibGDXPlayground.ShaderProgram_VERTEX_COLOR_UNIFORM,
												1, 1, 1, 1);
							(useColorShading ? shaderColor : shaderNoColor)
									.setUniformi(
											LibGDXPlayground.ShaderProgram_TEXTURE_SAMPLER_UNIFORM + 0,
											0);
							(useColorShading ? shaderColor : shaderNoColor)
									.setUniformi(
											LibGDXPlayground.ShaderProgram_TEXTURE_SAMPLER_UNIFORM + 1,
											1);

							if (pageBackground.flipX) {
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformi(
												LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_X_UNIFORM + 0,
												1);
							}
							if (pageBackground.flipY) {
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformi(
												LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_Y_UNIFORM + 0,
												1);
							}
							if (contentTexture1 != null) {
								if (contentTexture1.flipX) {
									(useColorShading ? shaderColor
											: shaderNoColor)
											.setUniformi(
													LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_X_UNIFORM + 1,
													1);
								}
								if (contentTexture1.flipY) {
									(useColorShading ? shaderColor
											: shaderNoColor)
											.setUniformi(
													LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_Y_UNIFORM + 1,
													1);
								}
							}
							mesh.render((useColorShading ? shaderColor
									: shaderNoColor),
									wireframe ? GL10.GL_LINE_STRIP
											: GL10.GL_TRIANGLES);

							(useColorShading ? shaderColor : shaderNoColor)
									.end();
						}
					}
				} else {
					if (useTriangleStrips) {
						if (!Gdx.graphics.isGL20Available()) {
							mesh.render(wireframe ? GL10.GL_LINE_STRIP
									: GL10.GL_TRIANGLE_STRIP, 0, offset);
						} else {
							(useColorShading ? shaderColor : shaderNoColor)
									.begin();
							(useColorShading ? shaderColor : shaderNoColor)
									.setUniformi(
											LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_X_UNIFORM + 0,
											0);
							(useColorShading ? shaderColor : shaderNoColor)
									.setUniformi(
											LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_Y_UNIFORM + 0,
											0);
							(useColorShading ? shaderColor : shaderNoColor)
									.setUniformi(
											LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_X_UNIFORM + 1,
											0);
							(useColorShading ? shaderColor : shaderNoColor)
									.setUniformi(
											LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_Y_UNIFORM + 1,
											0);
							(useColorShading ? shaderColor : shaderNoColor)
									.setUniformMatrix(
											LibGDXPlayground.ShaderProgram_PROJECTION_VIEW_MATRIX_UNIFORM,
											matrix);
							if (!useColorShading)
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformf(
												LibGDXPlayground.ShaderProgram_VERTEX_COLOR_UNIFORM,
												1, 1, 1, 1);
							(useColorShading ? shaderColor : shaderNoColor)
									.setUniformi(
											LibGDXPlayground.ShaderProgram_TEXTURE_SAMPLER_UNIFORM + 0,
											0);
							(useColorShading ? shaderColor : shaderNoColor)
									.setUniformi(
											LibGDXPlayground.ShaderProgram_TEXTURE_SAMPLER_UNIFORM + 1,
											1);

							if (pageBackground.flipX) {
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformi(
												LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_X_UNIFORM + 0,
												1);
							}
							if (pageBackground.flipY) {
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformi(
												LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_Y_UNIFORM + 0,
												1);
							}
							if (contentTexture1 != null) {
								if (contentTexture1.flipX) {
									(useColorShading ? shaderColor
											: shaderNoColor)
											.setUniformi(
													LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_X_UNIFORM + 1,
													1);
								}
								if (contentTexture1.flipY) {
									(useColorShading ? shaderColor
											: shaderNoColor)
											.setUniformi(
													LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_Y_UNIFORM + 1,
													1);
								}
							}
							mesh.render((useColorShading ? shaderColor
									: shaderNoColor),
									wireframe ? GL10.GL_LINE_STRIP
											: GL10.GL_TRIANGLE_STRIP, 0, offset);

							(useColorShading ? shaderColor : shaderNoColor)
									.end();
						}
					} else {
						if (!Gdx.graphics.isGL20Available()) {
							mesh.render(wireframe ? GL10.GL_LINE_STRIP
									: GL10.GL_TRIANGLES, 0, offset);
						} else {
							(useColorShading ? shaderColor : shaderNoColor)
									.begin();
							(useColorShading ? shaderColor : shaderNoColor)
									.setUniformi(
											LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_X_UNIFORM + 0,
											0);
							(useColorShading ? shaderColor : shaderNoColor)
									.setUniformi(
											LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_Y_UNIFORM + 0,
											0);
							(useColorShading ? shaderColor : shaderNoColor)
									.setUniformi(
											LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_X_UNIFORM + 1,
											0);
							(useColorShading ? shaderColor : shaderNoColor)
									.setUniformi(
											LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_Y_UNIFORM + 1,
											0);
							(useColorShading ? shaderColor : shaderNoColor)
									.setUniformMatrix(
											LibGDXPlayground.ShaderProgram_PROJECTION_VIEW_MATRIX_UNIFORM,
											matrix);
							if (!useColorShading)
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformf(
												LibGDXPlayground.ShaderProgram_VERTEX_COLOR_UNIFORM,
												1, 1, 1, 1);
							(useColorShading ? shaderColor : shaderNoColor)
									.setUniformi(
											LibGDXPlayground.ShaderProgram_TEXTURE_SAMPLER_UNIFORM + 0,
											0);
							(useColorShading ? shaderColor : shaderNoColor)
									.setUniformi(
											LibGDXPlayground.ShaderProgram_TEXTURE_SAMPLER_UNIFORM + 1,
											1);

							if (pageBackground.flipX) {
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformi(
												LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_X_UNIFORM + 0,
												1);
							}
							if (pageBackground.flipY) {
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformi(
												LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_Y_UNIFORM + 0,
												1);
							}
							if (contentTexture1 != null) {
								if (contentTexture1.flipX) {
									(useColorShading ? shaderColor
											: shaderNoColor)
											.setUniformi(
													LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_X_UNIFORM + 1,
													1);
								}
								if (contentTexture1.flipY) {
									(useColorShading ? shaderColor
											: shaderNoColor)
											.setUniformi(
													LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_Y_UNIFORM + 1,
													1);
								}
							}
							mesh.render((useColorShading ? shaderColor
									: shaderNoColor),
									wireframe ? GL10.GL_LINE_STRIP
											: GL10.GL_TRIANGLES, 0, offset);

							(useColorShading ? shaderColor : shaderNoColor)
									.end();
						}
					}

					gl.glActiveTexture(GL10.GL_TEXTURE0 + 0);
					gl.glEnable(GL10.GL_TEXTURE_2D);

					if (!Gdx.graphics.isGL20Available()) {
						Gdx.gl10.glColor4f(1, 1, 1, 1);
					}

					gl.glEnable(GL10.GL_BLEND);

					gl.glBlendFunc(GL10.GL_SRC_ALPHA,
							GL10.GL_ONE_MINUS_SRC_ALPHA);
					// usual alpha blending

					// gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ZERO);
					// transparent area becomes black

					bindtracker.bind(pageBackground);

					final boolean reverseBack = true;

					if (reverseBack && !doubleMeshLayer) { // flip-x
						if (Gdx.graphics.isGL20Available()) {

						} else {
							Gdx.gl10.glMatrixMode(GL10.GL_TEXTURE);
							Gdx.gl10.glPushMatrix();
							Gdx.gl10.glLoadIdentity();
							Gdx.gl10.glScalef(-1, 1, 1);
							// gl.glRotatef(45, 0, 0, 1);
							Gdx.gl10.glTranslatef(-1f, 0, 0);
						}
					}

					if (contentTexture2 != null) {

						gl.glActiveTexture(GL10.GL_TEXTURE0 + 1);
						gl.glEnable(GL10.GL_TEXTURE_2D);

						if (!Gdx.graphics.isGL20Available()) {
							Gdx.gl10.glColor4f(1, 1, 1, 1);
						}

						gl.glEnable(GL10.GL_BLEND);
						// gl.glBlendFunc(GL10.GL_ONE,
						// GL10.GL_ONE_MINUS_SRC_COLOR);
						gl.glBlendFunc(GL10.GL_SRC_ALPHA,
								GL10.GL_ONE_MINUS_SRC_ALPHA); // usual alpha
						// blending
						// gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ZERO); //
						// transparent area becomes black

						// bindtracker.bind(contentTexture2);
						contentTexture2.texture.bind();

						if (Gdx.graphics.isGL11Available()) {
							Gdx.gl11.glTexEnvi(GL10.GL_TEXTURE_ENV,
									GL10.GL_TEXTURE_ENV_MODE, GL11.GL_DECAL);
						}

						if (!doubleMeshLayer) { // flip-x
							if (Gdx.graphics.isGL20Available()) {

							} else {
								Gdx.gl10.glMatrixMode(GL10.GL_TEXTURE);
								Gdx.gl10.glPushMatrix();
								Gdx.gl10.glLoadIdentity();
								Gdx.gl10.glScalef(-1, 1, 1);
								// gl.glRotatef(45, 0, 0, 1);
								Gdx.gl10.glTranslatef(-1f, 0, 0);
							}
						}

						if (useTriangleStrips) {
							if (!Gdx.graphics.isGL20Available()) {
								mesh.render(wireframe ? GL10.GL_LINE_STRIP
										: GL10.GL_TRIANGLE_STRIP, offset, count);
							} else {
								(useColorShading ? shaderColor : shaderNoColor)
										.begin();
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformi(
												LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_X_UNIFORM + 0,
												0);
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformi(
												LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_Y_UNIFORM + 0,
												0);
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformi(
												LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_X_UNIFORM + 1,
												0);
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformi(
												LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_Y_UNIFORM + 1,
												0);
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformMatrix(
												LibGDXPlayground.ShaderProgram_PROJECTION_VIEW_MATRIX_UNIFORM,
												matrix);
								if (!useColorShading)
									(useColorShading ? shaderColor
											: shaderNoColor)
											.setUniformf(
													LibGDXPlayground.ShaderProgram_VERTEX_COLOR_UNIFORM,
													1, 1, 1, 1);
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformi(
												LibGDXPlayground.ShaderProgram_TEXTURE_SAMPLER_UNIFORM + 0,
												0);
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformi(
												LibGDXPlayground.ShaderProgram_TEXTURE_SAMPLER_UNIFORM + 1,
												1);
								if (!doubleMeshLayer) { // flip-x

									(useColorShading ? shaderColor
											: shaderNoColor)
											.setUniformi(
													LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_X_UNIFORM + 0,
													1);
									(useColorShading ? shaderColor
											: shaderNoColor)
											.setUniformi(
													LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_X_UNIFORM + 1,
													1);

								}
								if (pageBackground.flipX) {
									(useColorShading ? shaderColor
											: shaderNoColor)
											.setUniformi(
													LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_X_UNIFORM + 0,
													!doubleMeshLayer ? 0 : 1);
								}
								if (pageBackground.flipY) {
									(useColorShading ? shaderColor
											: shaderNoColor)
											.setUniformi(
													LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_Y_UNIFORM + 0,
													1);
								}
								if (contentTexture2.flipX) {
									(useColorShading ? shaderColor
											: shaderNoColor)
											.setUniformi(
													LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_X_UNIFORM + 1,
													!doubleMeshLayer ? 0 : 1);
								}
								if (contentTexture2.flipY) {
									(useColorShading ? shaderColor
											: shaderNoColor)
											.setUniformi(
													LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_Y_UNIFORM + 1,
													1);
								}
								mesh.render((useColorShading ? shaderColor
										: shaderNoColor),
										wireframe ? GL10.GL_LINE_STRIP
												: GL10.GL_TRIANGLE_STRIP,
										offset, count);

								(useColorShading ? shaderColor : shaderNoColor)
										.end();
							}
						} else {
							if (!Gdx.graphics.isGL20Available()) {
								mesh.render(wireframe ? GL10.GL_LINE_STRIP
										: GL10.GL_TRIANGLES, offset, count);
							} else {
								(useColorShading ? shaderColor : shaderNoColor)
										.begin();
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformi(
												LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_X_UNIFORM + 0,
												0);
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformi(
												LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_Y_UNIFORM + 0,
												0);
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformi(
												LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_X_UNIFORM + 1,
												0);
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformi(
												LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_Y_UNIFORM + 1,
												0);
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformMatrix(
												LibGDXPlayground.ShaderProgram_PROJECTION_VIEW_MATRIX_UNIFORM,
												matrix);
								if (!useColorShading)
									(useColorShading ? shaderColor
											: shaderNoColor)
											.setUniformf(
													LibGDXPlayground.ShaderProgram_VERTEX_COLOR_UNIFORM,
													1, 1, 1, 1);
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformi(
												LibGDXPlayground.ShaderProgram_TEXTURE_SAMPLER_UNIFORM + 0,
												0);
								(useColorShading ? shaderColor : shaderNoColor)
										.setUniformi(
												LibGDXPlayground.ShaderProgram_TEXTURE_SAMPLER_UNIFORM + 1,
												1);
								if (!doubleMeshLayer) { // flip-x
									(useColorShading ? shaderColor
											: shaderNoColor)
											.setUniformi(
													LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_X_UNIFORM + 0,
													1);
									(useColorShading ? shaderColor
											: shaderNoColor)
											.setUniformi(
													LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_X_UNIFORM + 1,
													1);
								}
								if (pageBackground.flipX) {
									(useColorShading ? shaderColor
											: shaderNoColor)
											.setUniformi(
													LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_X_UNIFORM + 0,
													!doubleMeshLayer ? 0 : 1);
								}
								if (pageBackground.flipY) {
									(useColorShading ? shaderColor
											: shaderNoColor)
											.setUniformi(
													LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_Y_UNIFORM + 0,
													1);
								}
								if (contentTexture2.flipX) {
									(useColorShading ? shaderColor
											: shaderNoColor)
											.setUniformi(
													LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_X_UNIFORM + 1,
													!doubleMeshLayer ? 0 : 1);
								}
								if (contentTexture2.flipY) {
									(useColorShading ? shaderColor
											: shaderNoColor)
											.setUniformi(
													LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_Y_UNIFORM + 1,
													1);
								}
								mesh.render((useColorShading ? shaderColor
										: shaderNoColor),
										wireframe ? GL10.GL_LINE_STRIP
												: GL10.GL_TRIANGLES, offset,
										count);

								(useColorShading ? shaderColor : shaderNoColor)
										.end();
							}
						}

						if (!doubleMeshLayer) {
							if (Gdx.graphics.isGL20Available()) {
							} else {
								gl.glActiveTexture(GL10.GL_TEXTURE0 + 1);
								Gdx.gl10.glMatrixMode(GL10.GL_TEXTURE);
								Gdx.gl10.glPopMatrix();
								Gdx.gl10.glMatrixMode(GL10.GL_MODELVIEW);

								if (reverseBack) {
									gl.glActiveTexture(GL10.GL_TEXTURE0 + 0);
									Gdx.gl10.glMatrixMode(GL10.GL_TEXTURE);
									Gdx.gl10.glPopMatrix();
									Gdx.gl10.glMatrixMode(GL10.GL_MODELVIEW);
								}
							}
						}
					} else {
						if (!doubleMeshLayer) {
							if (Gdx.graphics.isGL20Available()) {
							} else {
								if (reverseBack) {
									gl.glActiveTexture(GL10.GL_TEXTURE0 + 0);
									Gdx.gl10.glMatrixMode(GL10.GL_TEXTURE);
									Gdx.gl10.glPopMatrix();
									Gdx.gl10.glMatrixMode(GL10.GL_MODELVIEW);
								}
							}
						}
						gl.glActiveTexture(GL10.GL_TEXTURE0 + 1);
						gl.glDisable(GL10.GL_TEXTURE_2D);
					}
				}
			}

			if (Gdx.graphics.isGL11Available()
					|| Gdx.graphics.isGL20Available()) {

				gl.glActiveTexture(GL10.GL_TEXTURE0 + 0);
				if (Gdx.graphics.isGL11Available()) {
					Gdx.gl11.glTexEnvi(GL10.GL_TEXTURE_ENV,
							GL10.GL_TEXTURE_ENV_MODE, GL10.GL_MODULATE);
				}
				gl.glDisable(GL10.GL_TEXTURE_2D);

				gl.glActiveTexture(GL10.GL_TEXTURE0 + 1);
				if (Gdx.graphics.isGL11Available()) {
					Gdx.gl11.glTexEnvi(GL10.GL_TEXTURE_ENV,
							GL10.GL_TEXTURE_ENV_MODE, GL10.GL_MODULATE);
				}
				gl.glDisable(GL10.GL_TEXTURE_2D);
			}

			if (false) // should be equivalent to
						// gl11.glTexEnvi(GL10.GL_TEXTURE_ENV,
						// GL10.GL_TEXTURE_ENV_MODE, GL11.GL_MODULATE);
			{
				final float[] factor = { 1, 1, 1, 1f };
				Gdx.gl11.glTexEnvfv(GL10.GL_TEXTURE_ENV,
						GL10.GL_TEXTURE_ENV_COLOR, factor, 0);
				gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL11.GL_CONSTANT);
				// GL11.GL_CONSTANT_COLOR ??

				Gdx.gl11.glTexEnvi(GL10.GL_TEXTURE_ENV,
						GL10.GL_TEXTURE_ENV_MODE, GL11.GL_COMBINE);

				Gdx.gl11.glTexEnvi(GL10.GL_TEXTURE_2D, GL11.GL_COMBINE_RGB,
						GL10.GL_MODULATE);
				Gdx.gl11.glTexEnvi(GL10.GL_TEXTURE_2D, GL11.GL_SRC0_RGB,
						GL11.GL_PREVIOUS);// GL_PRIMARY_COLOR
				Gdx.gl11.glTexEnvi(GL10.GL_TEXTURE_2D, GL11.GL_OPERAND0_RGB,
						GL10.GL_SRC_COLOR);
				Gdx.gl11.glTexEnvi(GL10.GL_TEXTURE_2D, GL11.GL_SRC1_RGB,
						GL10.GL_TEXTURE);
				Gdx.gl11.glTexEnvi(GL10.GL_TEXTURE_2D, GL11.GL_OPERAND1_RGB,
						GL10.GL_SRC_COLOR);

				Gdx.gl11.glTexEnvi(GL10.GL_TEXTURE_2D, GL11.GL_COMBINE_ALPHA,
						GL10.GL_MODULATE);
				Gdx.gl11.glTexEnvi(GL10.GL_TEXTURE_2D, GL11.GL_SRC0_ALPHA,
						GL11.GL_PREVIOUS);// GL_PRIMARY_COLOR
				Gdx.gl11.glTexEnvi(GL10.GL_TEXTURE_2D, GL11.GL_OPERAND0_ALPHA,
						GL10.GL_SRC_ALPHA);
				Gdx.gl11.glTexEnvi(GL10.GL_TEXTURE_2D, GL11.GL_SRC1_ALPHA,
						GL10.GL_TEXTURE);
				Gdx.gl11.glTexEnvi(GL10.GL_TEXTURE_2D, GL11.GL_OPERAND1_ALPHA,
						GL10.GL_SRC_ALPHA);
			}

			if (false) {
				Gdx.gl11.glTexEnvi(GL10.GL_TEXTURE_ENV,
						GL10.GL_TEXTURE_ENV_MODE, GL11.GL_COMBINE);

				Gdx.gl11.glTexEnvi(GL10.GL_TEXTURE_2D, GL11.GL_COMBINE_RGB,
						GL10.GL_REPLACE);
				Gdx.gl11.glTexEnvi(GL10.GL_TEXTURE_2D, GL11.GL_SRC0_RGB,
						GL10.GL_TEXTURE);
				Gdx.gl11.glTexEnvi(GL10.GL_TEXTURE_2D, GL11.GL_OPERAND0_RGB,
						GL10.GL_SRC_COLOR);
				// gl11.glTexEnvi(GL10.GL_TEXTURE_2D, GL11.GL_SRC1_RGB,
				// GL11.GL_PREVIOUS);
				// gl11.glTexEnvi(GL10.GL_TEXTURE_2D,
				// GL11.GL_OPERAND1_RGB, GL10.GL_SRC_COLOR);

				Gdx.gl11.glTexEnvi(GL10.GL_TEXTURE_2D, GL11.GL_COMBINE_ALPHA,
						GL10.GL_REPLACE);
				Gdx.gl11.glTexEnvi(GL10.GL_TEXTURE_2D, GL11.GL_SRC0_ALPHA,
						GL11.GL_TEXTURE);
				Gdx.gl11.glTexEnvi(GL10.GL_TEXTURE_2D, GL11.GL_OPERAND0_ALPHA,
						GL10.GL_SRC_ALPHA);
				Gdx.gl11.glTexEnvi(GL10.GL_TEXTURE_2D, GL11.GL_SRC1_ALPHA,
						GL11.GL_PREVIOUS);
				Gdx.gl11.glTexEnvi(GL10.GL_TEXTURE_2D, GL11.GL_OPERAND1_ALPHA,
						GL10.GL_SRC_ALPHA);

				// gl11.glTexEnvi(GL10.GL_TEXTURE_2D,
				// GL11.GL_COMBINE_RGB,
				// GL10.GL_MODULATE);
				// gl11.glTexEnvi(GL10.GL_TEXTURE_2D, GL11.GL_SRC0_RGB,
				// GL10.GL_TEXTURE);
				// gl11.glTexEnvi(GL10.GL_TEXTURE_2D,
				// GL11.GL_OPERAND0_RGB, GL10.GL_SRC_COLOR);
				// gl11.glTexEnvi(GL10.GL_TEXTURE_2D, GL11.GL_SRC1_RGB,
				// GL11.GL_PREVIOUS);
				// gl11.glTexEnvi(GL10.GL_TEXTURE_2D,
				// GL11.GL_OPERAND1_RGB, GL10.GL_SRC_COLOR);
				// gl11.glTexEnvi(GL10.GL_TEXTURE_2D, GL11.GL_SRC2_RGB,
				// GL10.GL_TEXTURE);
				// gl11.glTexEnvi(GL10.GL_TEXTURE_2D,
				// GL11.GL_OPERAND2_RGB, GL10.GL_SRC_ALPHA);
				//
				// gl11.glTexEnvi(GL10.GL_TEXTURE_2D,
				// GL11.GL_COMBINE_ALPHA, GL10.GL_MODULATE);
				// gl11.glTexEnvi(GL10.GL_TEXTURE_2D,
				// GL11.GL_SRC0_ALPHA,
				// GL11.GL_PREVIOUS);
				// gl11.glTexEnvi(GL10.GL_TEXTURE_2D,
				// GL11.GL_OPERAND0_ALPHA, GL10.GL_SRC_ALPHA);
				// gl11.glTexEnvi(GL10.GL_TEXTURE_2D,
				// GL11.GL_SRC1_ALPHA,
				// GL10.GL_TEXTURE);
				// gl11.glTexEnvi(GL10.GL_TEXTURE_2D,
				// GL11.GL_OPERAND1_ALPHA,
				// GL10.GL_ONE_MINUS_SRC_ALPHA);
				//
				// gl.glEnable(GL10.GL_BLEND);
				// gl.glBlendFunc(GL10.GL_ONE_MINUS_SRC_ALPHA,
				// GL10.GL_SRC_ALPHA);
			}

			if (false) {
				Gdx.gl11.glTexEnvi(GL10.GL_TEXTURE_ENV,
						GL10.GL_TEXTURE_ENV_MODE, GL11.GL_COMBINE);
				// rgb = texture1.rgb * texture1.alpha + previous.rgb *
				// (1 - texture1.alpha)
				Gdx.gl11.glTexEnvi(GL10.GL_TEXTURE_2D, GL11.GL_COMBINE_RGB,
						GL11.GL_INTERPOLATE);
				Gdx.gl11.glTexEnvi(GL10.GL_TEXTURE_2D, GL11.GL_SRC0_RGB,
						GL10.GL_TEXTURE);
				Gdx.gl11.glTexEnvi(GL10.GL_TEXTURE_2D, GL11.GL_OPERAND0_RGB,
						GL10.GL_SRC_COLOR);
				Gdx.gl11.glTexEnvi(GL10.GL_TEXTURE_2D, GL11.GL_SRC1_RGB,
						GL11.GL_PREVIOUS);
				Gdx.gl11.glTexEnvi(GL10.GL_TEXTURE_2D, GL11.GL_OPERAND1_RGB,
						GL10.GL_SRC_COLOR);
				Gdx.gl11.glTexEnvi(GL10.GL_TEXTURE_2D, GL11.GL_SRC2_RGB,
						GL10.GL_TEXTURE);
				Gdx.gl11.glTexEnvi(GL10.GL_TEXTURE_2D, GL11.GL_OPERAND2_RGB,
						GL10.GL_SRC_ALPHA);
				// alpha = (1 - texture1.alpha) * (1 - previous.alpha)
				Gdx.gl11.glTexEnvi(GL10.GL_TEXTURE_2D, GL11.GL_COMBINE_ALPHA,
						GL10.GL_MODULATE);
				Gdx.gl11.glTexEnvi(GL10.GL_TEXTURE_2D, GL11.GL_SRC0_ALPHA,
						GL10.GL_TEXTURE);
				Gdx.gl11.glTexEnvi(GL10.GL_TEXTURE_2D, GL11.GL_OPERAND0_ALPHA,
						GL10.GL_ONE_MINUS_SRC_ALPHA);
				Gdx.gl11.glTexEnvi(GL10.GL_TEXTURE_2D, GL11.GL_SRC1_ALPHA,
						GL11.GL_PREVIOUS);
				Gdx.gl11.glTexEnvi(GL10.GL_TEXTURE_2D, GL11.GL_OPERAND1_ALPHA,
						GL10.GL_ONE_MINUS_SRC_ALPHA);
			}

			//
			// // gl.glPushAttrib(GL10.GL_TEXTURE_BIT);
			// //
			// // float brightness = 0.5f;
			// //
			// // gl11.glTexEnvi(GL10.GL_TEXTURE_ENV,
			// GL10.GL_TEXTURE_ENV_MODE,
			// // GL11.GL_COMBINE);
			// // if (brightness >= 1.0f) {
			// // gl11.glTexEnvi(GL10.GL_TEXTURE_ENV, GL11.GL_COMBINE_RGB,
			// // GL10.GL_ADD);
			// // gl.glColor4f(brightness - 1, brightness - 1, brightness -
			// 1,
			// // brightness - 1);
			// // } else {
			// // gl11.glTexEnvi(GL10.GL_TEXTURE_ENV, GL11.GL_COMBINE_RGB,
			// // GL11.GL_SUBTRACT);
			// // gl.glColor4f(1 - brightness, 1 - brightness, 1 -
			// brightness,
			// // 1 - brightness);
			// // }
			// // gl11.glTexEnvi(GL10.GL_TEXTURE_ENV, GL11.GL_SRC0_RGB,
			// // GL10.GL_TEXTURE);
			// // gl11.glTexEnvi(GL10.GL_TEXTURE_ENV, GL11.GL_SRC1_RGB,
			// // GL11.GL_PRIMARY_COLOR);
			// // gl11.glTexEnvi(GL10.GL_TEXTURE_ENV, GL11.GL_COMBINE_ALPHA,
			// // GL10.GL_REPLACE);
			// // gl11.glTexEnvi(GL10.GL_TEXTURE_ENV, GL11.GL_SRC0_ALPHA,
			// // GL10.GL_TEXTURE);
			//
			// // gl.glPopAttrib();
			//
			// IntBuffer maxTextureUnits = IntBuffer.allocate(1);
			// gl.glGetIntegerv(GL10.GL_MAX_TEXTURE_UNITS, maxTextureUnits);
			// int n = maxTextureUnits.get(0);
			//
			// if (false && n > 1) {
			// // Texture tex2 = sprite_Textures.get(5);
			// Texture tex2 = backgroundTexture_Aqua;
			//
			// // Gdx.gl.glActiveTexture(GL10.GL_TEXTURE0 + 1);
			//
			// tex2.bind(1);
			//
			// gl11.glEnable(GL10.GL_TEXTURE_2D);
			//
			// gl11.glEnable(GL10.GL_BLEND);
			// gl11.glBlendFunc(GL10.GL_SRC_ALPHA,
			// GL10.GL_ONE_MINUS_SRC_ALPHA);
			//
			// gl11.glColor4f(1, 1, 1, 0.5f);
			//
			// gl11.glTexEnvi(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE,
			// GL11.GL_COMBINE);
			// // / RGB
			// gl11.glTexEnvi(GL10.GL_TEXTURE_ENV, GL11.GL_COMBINE_RGB,
			// GL10.GL_BLEND); // GL_ADD GL_MODULATE GL_REPLACE
			// // GL_DECAL GL_BLEND
			// gl11.glTexEnvi(GL10.GL_TEXTURE_ENV, GL11.GL_SRC0_RGB,
			// GL11.GL_PREVIOUS);
			// gl11.glTexEnvi(GL10.GL_TEXTURE_ENV, GL11.GL_SRC1_RGB,
			// GL10.GL_TEXTURE);
			// gl11.glTexEnvi(GL10.GL_TEXTURE_ENV, GL11.GL_OPERAND0_RGB,
			// GL10.GL_SRC_COLOR);
			// gl11.glTexEnvi(GL10.GL_TEXTURE_ENV, GL11.GL_OPERAND1_RGB,
			// GL10.GL_SRC_COLOR);
			// // // / ALPHA
			// // gl11.glTexEnvi(GL10.GL_TEXTURE_ENV, GL11.GL_COMBINE_ALPHA,
			// // GL10.GL_REPLACE);
			// // gl11.glTexEnvi(GL10.GL_TEXTURE_ENV, GL11.GL_SRC0_ALPHA,
			// // GL10.GL_TEXTURE); // GL_PREVIOUS
			// // gl11.glTexEnvi(GL10.GL_TEXTURE_ENV, GL11.GL_SRC1_ALPHA,
			// // GL10.GL_TEXTURE); // GL_PREVIOUS
			// // gl11.glTexEnvi(GL10.GL_TEXTURE_ENV,
			// GL11.GL_OPERAND0_ALPHA,
			// // GL10.GL_SRC_ALPHA);
			// // gl11.glTexEnvi(GL10.GL_TEXTURE_ENV,
			// GL11.GL_OPERAND1_ALPHA,
			// // GL10.GL_SRC_ALPHA);
			// }
		}

		Color interpolate(Color c1, Color c2, float t) {
			if (t < 0)
				t = 0;
			if (t > 1)
				t = 1;
			if (t == 0)
				return c1;
			if (t == 1)
				return c2;

			float r = (1 - t) * c1.r + t * c2.r;
			float g = (1 - t) * c1.g + t * c2.g;
			float b = (1 - t) * c1.b + t * c2.b;

			float r2 = c1.r + (c2.r - c1.r) * t;
			float g2 = c1.g + (c2.g - c1.g) * t;
			float b2 = c1.b + (c2.b - c1.b) * t;

			return new Color(r, g, b, 1);
		}
	}
}

// //Reset cylinder properties, positioning it on the right side, oriented
// vertically
// self.cylinderPosition = CGPointMake(frame.size.width,
// frame.size.height/2);
// self.cylinderAngle = M_PI_2;
// self.cylinderRadius = 20;
//
// self.cylinderPosition = CGPointMake(0, 0);
// self.cylinderAngle = M_PI_2;
// self.cylinderRadius = 32;
//
// positionHandle = glGetAttribLocation(program,
// ShaderProgram.POSITION_ATTRIBUTE);
// texCoordHandle = glGetAttribLocation(program,
// ShaderProgram.TEXCOORD_ATTRIBUTE);
// mvpHandle = glGetUniformLocation(program,
// LibGDXPlayground.ShaderProgram_PROJECTION_VIEW_MATRIX_UNIFORM);
// samplerHandle = glGetUniformLocation(program,
// LibGDXPlayground.ShaderProgram_TEXTURE_SAMPLER_UNIFORM);
// texSizeHandle = glGetUniformLocation(program, "u_texSize");
// cylinderPositionHandle = glGetUniformLocation(program,
// "u_cylinderPosition");
// cylinderDirectionHandle = glGetUniformLocation(program,
// "u_cylinderDirection");
// cylinderRadiusHandle = glGetUniformLocation(program, "u_cylinderRadius");
//
// glUniform2f(cylinderPositionHandle, _cylinderPosition.x,
// _cylinderPosition.y);
// glUniform2f(cylinderDirectionHandle, cosf(_cylinderAngle),
// sinf(_cylinderAngle));
// glUniform1f(cylinderRadiusHandle, _cylinderRadius);
// glUniform2f(texSizeHandle, textureWidth, textureHeight);
// glUniformMatrix4fv(mvpHandle, 1, GL_FALSE, mvp);
// glUniform1i(samplerHandle, 0);
//
//
// GLsizeiptr verticesSize = (xRes+1)*(yRes+1)*sizeof(Vertex);
// Vertex *vertices = malloc(verticesSize);
//
// for (int y=0; y<yRes+1; ++y) {
// GLfloat vy = ((GLfloat)y/yRes)*viewportHeight;
// GLfloat tv = vy;///viewportHeight;
// for (int x=0; x<xRes+1; ++x) {
// Vertex *v = &vertices[y*(xRes+1) + x];
// v->x = ((GLfloat)x/xRes)*viewportWidth;
// v->y = vy;
// v->z = 0;
// v->u = v->x;///viewportWidth;
// v->v = tv;
// }
// }
//
// elementCount = xRes*yRes*2*3;
// GLsizeiptr indicesSize = elementCount*sizeof(GLushort);//Two triangles
// per square, 3 indices per triangle
// GLushort *indices = malloc(indicesSize);
//
// for (int y=0; y<yRes; ++y) {
// for (int x=0; x<xRes; ++x) {
// int i = y*(xRes+1) + x;
// int idx = y*xRes + x;
// assert(i < elementCount*3-1);
// indices[idx*6+0] = i;
// indices[idx*6+1] = i + 1;
// indices[idx*6+2] = i + xRes + 1;
// indices[idx*6+3] = i + 1;
// indices[idx*6+4] = i + xRes + 2;
// indices[idx*6+5] = i + xRes + 1;
// }
// }
// - (void)touchesBegan:(NSSet *)touches withEvent:(UIEvent *)event
// {
// [super touchesBegan:touches withEvent:event];
// CGPoint p = [[touches anyObject] locationInView:self];
// p.y = self.bounds.size.height - p.y;
// startPickingPosition.x = self.bounds.size.width;
// startPickingPosition.y = p.y;
//
// [self setCylinderPosition:p animatedWithDuration:kDuration];
//
// CGPoint dir = CGPointMake(startPickingPosition.x-p.x,
// startPickingPosition.y-p.y);
// dir = CGPointMake(-dir.y, dir.x);
// CGFloat length = sqrtf(dir.x*dir.x + dir.y*dir.y);
// dir.x /= length, dir.y /= length;
// CGFloat angle = atan2f(dir.y, dir.x);
//
// [self setCylinderAngle:angle animatedWithDuration:kDuration];
// [self setCylinderRadius:16+length/4 animatedWithDuration:kDuration];
// }
//
// - (void)touchesMoved:(NSSet *)touches withEvent:(UIEvent *)event
// {
// [super touchesMoved:touches withEvent:event];
// CGPoint p = [[touches anyObject] locationInView:self];
// p.y = self.bounds.size.height - p.y;
// self.cylinderPosition = p;
// CGPoint dir = CGPointMake(startPickingPosition.x-self.cylinderPosition.x,
// startPickingPosition.y-self.cylinderPosition.y);
// dir = CGPointMake(-dir.y, dir.x);
// CGFloat length = sqrtf(dir.x*dir.x + dir.y*dir.y);
// dir.x /= length, dir.y /= length;
// CGFloat angle = atan2f(dir.y, dir.x);
//
// self.cylinderAngle = angle;
// self.cylinderRadius = 16 + length/4;
// }

//
//
// String vertexShader = "uniform mat4 "
// + LibGDXPlayground.ShaderProgram_PROJECTION_VIEW_MATRIX_UNIFORM
// + ";"
// + "attribute vec4 "
// + ShaderProgram.POSITION_ATTRIBUTE
// + ";"
// + "attribute vec2 "
// + ShaderProgram.TEXCOORD_ATTRIBUTE
// + ";"
// + "uniform vec2 u_texSize;"
// + "uniform vec2 u_cylinderPosition;"
// + "uniform vec2 u_cylinderDirection;"
// + "uniform float u_cylinderRadius;"
// + "varying vec2 v_texCoord;"
// + "varying vec3 v_normal;"
// + "#define M_PI 3.14159265358979323846264338327950288"
// + "#define M_PI_2 1.57079632679489661923"
// + "void main()"
// + "{"
// + "vec4 v = "
// + ShaderProgram.POSITION_ATTRIBUTE
// + ";"
// + "vec2 n = vec2(u_cylinderDirection.y, -u_cylinderDirection.x);"
// + "vec2 w = v.xy - u_cylinderPosition;"
// + "float d = dot(w, n);"
// + "v_normal = vec3(0.0, 0.0, 1.0);"
// + "//vertices after the cylinder"
// + "if (d > 0.0) {"
// + "//vertices that should pass over the cylinder"
// + "if (d > M_PI*u_cylinderRadius) {"
// + "vec2 dv = n * (2.0*d - M_PI*u_cylinderRadius);"
// + "v.xy -=  dv;"
// + "v.z = 2.0*u_cylinderRadius;"
// + "v_normal = vec3(0.0, 0.0, -1.0);"
// + "}"
// + "//vertices that should be projected on the half of the cylinder"
// + "else {"
// + "float dr = d/u_cylinderRadius;//projection angle"
// + "float s = sin(dr);"
// + "float c = cos(dr);"
// +
// "vec2 proj = v.xy - n*d;//projection of vertex on the cylinder axis projected on the xy plane"
// + "v.xyz = vec3(s*n.x, s*n.y, 1.0 - c)*u_cylinderRadius;"
// + "v.xy += proj;" + "vec3 center = vec3(proj, u_cylinderRadius);"
// + "v_normal = (center - v.xyz)/u_cylinderRadius;" + "/*"
// + "if (dr < M_PI/2.0) { //lower part of curl"
// + "v_normal = -v_normal;" + "}*/" + "}" + "}" + "gl_Position = "
// + LibGDXPlayground.ShaderProgram_PROJECTION_VIEW_MATRIX_UNIFORM
// + " * v;" + "v_texCoord = " + ShaderProgram.TEXCOORD_ATTRIBUTE
// + "/u_texSize;" + "}";
//
// String fragmentShader = "precision mediump float;"
// + "uniform sampler2D "
// + LibGDXPlayground.ShaderProgram_TEXTURE_SAMPLER_UNIFORM
// + ";"
// + "varying vec2 v_texCoord;"
// + "varying vec3 v_normal;"
// + "void main()"
// + "{"
// + "vec4 color = texture2D("
// + LibGDXPlayground.ShaderProgram_TEXTURE_SAMPLER_UNIFORM
// + ", v_texCoord);"
// + "if (color.a < 0.2) {"
// + "discard;"
// + "}"
// + "vec3 n = normalize(v_normal);"
// + "float fFrontFacing = float(gl_FrontFacing);"
// +
// "gl_FragColor = vec4(color.rgb * (n.z * (fFrontFacing-0.5)*2.0) + vec3(0.1,0.1,0.1)*(1.0-fFrontFacing), color.a);"
// + "}";
//
// String fragmentShader_ = "precision mediump float;" + "uniform sampler2D "
// + LibGDXPlayground.ShaderProgram_TEXTURE_SAMPLER_UNIFORM + ";"
// + "varying vec2 v_texCoord;" + "void main()" + "{"
// + "gl_FragColor = vec4(texture2D("
// + LibGDXPlayground.ShaderProgram_TEXTURE_SAMPLER_UNIFORM
// + ", v_texCoord).rgb, 1.0);" + "}";
//
// String vertexShader_ = "uniform mat4 "
// + LibGDXPlayground.ShaderProgram_PROJECTION_VIEW_MATRIX_UNIFORM
// + ";" + "uniform vec2 u_texSize;" + "attribute vec4 "
// + ShaderProgram.POSITION_ATTRIBUTE + ";" + "attribute vec2 "
// + ShaderProgram.TEXCOORD_ATTRIBUTE + ";"
// + "varying vec2 v_texCoord;" + "void main()" + "{"
// + "gl_Position = "
// + LibGDXPlayground.ShaderProgram_PROJECTION_VIEW_MATRIX_UNIFORM
// + " * " + ShaderProgram.POSITION_ATTRIBUTE + ";" + "v_texCoord = "
// + ShaderProgram.TEXCOORD_ATTRIBUTE + "/u_texSize;" + "}";
