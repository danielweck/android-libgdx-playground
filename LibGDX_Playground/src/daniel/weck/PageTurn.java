package daniel.weck;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.GLCommon;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Plane;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;

import com.badlogic.gdx.utils.Array;

public class PageTurn {

	Vertex[] rectangle = new Vertex[4];

	Mesh mesh;

	int currentVerticesCount;
	int currentVerticesCountFront;
	int currentVerticesCountBack;

	Vector3[] vertices;
	Vector2[] uvs;
	Color[] colors;
	int meshVerticeStride;
	float[] meshVertices;

	int maxCurlSplits; // 10
	int maxVerticesCount;

	Array<Vertex> mTempVertices;
	Array<Vertex> mIntersections;
	Array<Vertex> mOutputVertices;
	Array<Vertex> mRotatedVertices;

	Array<Float> mScanLines;

	Camera camera;
	SubTexture subtexture;
	ShaderProgram shader;

	Plane plane;

	public PageTurn(ShaderProgram s, Camera c, float z, Vector2 topLeft,
			Vector2 bottomLeft, Vector2 topRight, Vector2 bottomRight,
			int maxCurlSplits, SubTexture subtex) {

		shader = s;
		subtexture = subtex;
		camera = c;

		// Corners: 0 = top-left, 1 = bottom-left, 2 = top-right, 3 =
		// bottom-right
		rectangle[0] = new Vertex();
		rectangle[0].mPos.x = topLeft.x;
		rectangle[0].mPos.y = topLeft.y;
		rectangle[0].mPos.z = z;
		rectangle[0].mTex.x = 0;
		rectangle[0].mTex.y = 0;

		rectangle[1] = new Vertex();
		rectangle[1].mPos.x = bottomLeft.x;
		rectangle[1].mPos.y = bottomLeft.y;
		rectangle[1].mPos.z = z;
		rectangle[1].mTex.x = 0;
		rectangle[1].mTex.y = 1;

		rectangle[2] = new Vertex();
		rectangle[2].mPos.x = topRight.x;
		rectangle[2].mPos.y = topRight.y;
		rectangle[2].mPos.z = z;
		rectangle[2].mTex.x = 1;
		rectangle[2].mTex.y = 0;

		rectangle[3] = new Vertex();
		rectangle[3].mPos.x = bottomRight.x;
		rectangle[3].mPos.y = bottomRight.y;
		rectangle[3].mPos.z = z;
		rectangle[3].mTex.x = 1;
		rectangle[3].mTex.y = 1;

		plane = new Plane(new Vector3(0, 0, z), new Vector3(1, 0, z),
				new Vector3(0, 1, z));

		resetArrays(maxCurlSplits);

		updateMeshVertices();
	}

	void resetArrays(int maxCurls) {
		maxCurlSplits = maxCurls < 1 ? 1 : maxCurls;
		maxVerticesCount = 4 + 2 + (2 * maxCurlSplits);

		meshVerticeStride = 6;
		mesh = new Mesh(true, maxVerticesCount, 0, new VertexAttribute(
				Usage.Position, 3, ShaderProgram.POSITION_ATTRIBUTE), // 3
																		// floats
				new VertexAttribute(Usage.ColorPacked, 4,
						ShaderProgram.COLOR_ATTRIBUTE), // 1
				// float
				new VertexAttribute(Usage.TextureCoordinates, 2,
						ShaderProgram.TEXCOORD_ATTRIBUTE + "0") // 2
																// floats
																// new
																// VertexAttribute(Usage.TextureCoordinates,
																// 2,
		// ShaderProgram.TEXCOORD_ATTRIBUTE + "1") // 2
		// // floats
		);

		meshVertices = new float[maxVerticesCount * meshVerticeStride];
		vertices = new Vector3[maxVerticesCount];
		uvs = new Vector2[maxVerticesCount];
		colors = new Color[maxVerticesCount];

		currentVerticesCountFront = 4;
		currentVerticesCountBack = 0;

		currentVerticesCount = 0;
		for (int i = 0; i < 4; ++i) {
			addVertex(rectangle[i]);
		}
		//
		mScanLines = new Array<Float>(maxCurlSplits + 2);
		mOutputVertices = new Array<Vertex>(7);
		mRotatedVertices = new Array<Vertex>(4);
		mIntersections = new Array<Vertex>(2);
		mTempVertices = new Array<Vertex>(7 + 4);
		for (int i = 0; i < 7 + 4; ++i) {
			mTempVertices.add(new Vertex());
		}
		//
		rectangle[0].mPenumbra.x = rectangle[1].mPenumbra.x = rectangle[1].mPenumbra.y = rectangle[3].mPenumbra.y = -1;
		rectangle[0].mPenumbra.y = rectangle[2].mPenumbra.x = rectangle[2].mPenumbra.y = rectangle[3].mPenumbra.x = 1;
	}

	void addVertex(Vertex vertex) {

		if (vertices[currentVerticesCount] == null)
			vertices[currentVerticesCount] = new Vector3();
		vertices[currentVerticesCount].x = vertex.mPos.x;
		vertices[currentVerticesCount].y = vertex.mPos.y;
		vertices[currentVerticesCount].z = vertex.mPos.z;

		if (colors[currentVerticesCount] == null)
			colors[currentVerticesCount] = new Color();
		colors[currentVerticesCount].r = vertex.mColorMultiplier;
		colors[currentVerticesCount].g = vertex.mColorMultiplier;
		colors[currentVerticesCount].b = vertex.mColorMultiplier;
		colors[currentVerticesCount].a = vertex.mColorAlpha;

		if (uvs[currentVerticesCount] == null)
			uvs[currentVerticesCount] = new Vector2();
		uvs[currentVerticesCount].x = vertex.mTex.x;
		uvs[currentVerticesCount].y = vertex.mTex.y;

		currentVerticesCount++;
	}

	Vector2 startPoint = null;

	void touchPage(Vector2 point) {

		if (startPoint == null) {
			startPoint = new Vector2(point);
			return;
		}

		float pageX = point.x - rectangle[0].mPos.x;
		float pageY = point.y - rectangle[0].mPos.y;

		float pageWidth = rectangle[2].mPos.x - rectangle[0].mPos.x;
		float pageHeight = rectangle[0].mPos.y - rectangle[1].mPos.y;

		// float radius = pageWidth / 3;

		Vector2 dir = new Vector2(point.x - startPoint.x, point.y
				- startPoint.y);

		float dist = (float) Math.sqrt(dir.x * dir.x + dir.y * dir.y);

		// float angle = (float) Math.atan2(dir.y / dist, dir.x / dist);

		float radius = 16 + dist / 4;

		float curlLen = radius * (float) Math.PI;
		if (dist > (pageWidth * 2) - curlLen) {
			curlLen = Math.max((pageWidth * 2) - dist, 0f);
			radius = curlLen / (float) Math.PI;
		}

		if (dist >= curlLen) {
			float translate = (dist - curlLen) / 2;
			point.x -= dir.x * translate / dist;
			point.y -= dir.y * translate / dist;
		} else {
			float angle = (float) (Math.PI * Math.sqrt(dist / curlLen));
			float translate = radius * (float) Math.sin(angle);
			point.x += dir.x * translate / dist;
			point.y += dir.y * translate / dist;
		}

		curl(point, dir, radius);
	}

	public void curl(Vector2 curlPos, Vector2 curlDir, float radius) {

		float curlAngle = (float) Math.acos(curlDir.x);
		curlAngle = curlDir.y > 0 ? -curlAngle : curlAngle;

		mTempVertices.addAll(mRotatedVertices);
		mRotatedVertices.clear();
		for (int i = 0; i < 4; ++i) {
			Vertex v = mTempVertices.removeIndex(0);
			v.set(rectangle[i]);
			v.translate(-curlPos.x, -curlPos.y);
			v.rotateZ(-curlAngle);
			int j = 0;
			for (; j < mRotatedVertices.size; ++j) {
				Vertex v2 = mRotatedVertices.get(j);
				if (v.mPos.x > v2.mPos.x) {
					break;
				}
				if (v.mPos.x == v2.mPos.x && v.mPos.y > v2.mPos.y) {
					break;
				}
			}
			mRotatedVertices.insert(j, v);
		}

		int lines[][] = { { 0, 1 }, { 0, 2 }, { 1, 3 }, { 2, 3 } };
		{
			Vertex v0 = mRotatedVertices.get(0);
			Vertex v2 = mRotatedVertices.get(2);
			Vertex v3 = mRotatedVertices.get(3);
			float dist2 = (float) Math.sqrt((v0.mPos.x - v2.mPos.x)
					* (v0.mPos.x - v2.mPos.x) + (v0.mPos.y - v2.mPos.y)
					* (v0.mPos.y - v2.mPos.y));
			float dist3 = (float) Math.sqrt((v0.mPos.x - v3.mPos.x)
					* (v0.mPos.x - v3.mPos.x) + (v0.mPos.y - v3.mPos.y)
					* (v0.mPos.y - v3.mPos.y));
			if (dist2 > dist3) {
				lines[1][1] = 3;
				lines[2][1] = 2;
			}
		}

		currentVerticesCount = currentVerticesCountFront = currentVerticesCountBack = 0;

		float curlLength = (float) Math.PI * radius;

		mScanLines.clear();
		if (maxCurlSplits > 0) {
			mScanLines.add((float) 0);
		}
		for (int i = 1; i < maxCurlSplits; ++i) {
			mScanLines.add((-curlLength * i) / (maxCurlSplits - 1));
		}

		mScanLines.add(mRotatedVertices.get(3).mPos.x - 1);

		float scanXmax = mRotatedVertices.get(0).mPos.x + 1;

		for (int i = 0; i < mScanLines.size; ++i) {

			float scanXmin = mScanLines.get(i);

			for (int j = 0; j < mRotatedVertices.size; ++j) {
				Vertex v = mRotatedVertices.get(j);
				if (v.mPos.x >= scanXmin && v.mPos.x <= scanXmax) {

					Vertex n = mTempVertices.removeIndex(0);
					n.set(v);

					Array<Vertex> intersections = getIntersections(
							mRotatedVertices, lines, n.mPos.x);

					if (intersections.size == 1
							&& intersections.get(0).mPos.y > v.mPos.y) {
						mOutputVertices.addAll(intersections);
						mOutputVertices.add(n);
					} else if (intersections.size <= 1) {
						mOutputVertices.add(n);
						mOutputVertices.addAll(intersections);
					} else {
						mTempVertices.add(n);
						mTempVertices.addAll(intersections);
					}
				}
			}

			Array<Vertex> intersections = getIntersections(mRotatedVertices,
					lines, scanXmin);

			if (intersections.size == 2) {
				Vertex v1 = intersections.get(0);
				Vertex v2 = intersections.get(1);
				if (v1.mPos.y < v2.mPos.y) {
					mOutputVertices.add(v2);
					mOutputVertices.add(v1);
				} else {
					mOutputVertices.addAll(intersections);
				}
			} else if (intersections.size != 0) {
				mTempVertices.addAll(intersections);
			}

			while (mOutputVertices.size > 0) {
				Vertex v = mOutputVertices.removeIndex(0);
				mTempVertices.add(v);

				if (i == 0) {
					v.mColorAlpha = 1.0f;
					currentVerticesCountFront++;
				} else if (i == mScanLines.size - 1 || curlLength == 0) {
					v.mPos.x = -(curlLength + v.mPos.x);
					v.mPos.z = 2 * radius;
					v.mPenumbra.x = -v.mPenumbra.x;

					v.mColorAlpha = 1.0f;
					currentVerticesCountBack++;
				} else {
					float rotY = (float) Math.PI * (v.mPos.x / curlLength);
					v.mPos.x = radius * (float) Math.sin(rotY);
					v.mPos.z = radius - (radius * (float) Math.cos(rotY));
					v.mPenumbra.x *= (float) Math.cos(rotY);

					// Map color multiplier to [.1f, 1f] range.
					v.mColorMultiplier = 0.1f + 0.9f * (float) Math.sqrt(Math
							.sin(rotY) + 1);

					if (v.mPos.z >= radius) {
						v.mColorAlpha = 1.0f;
						currentVerticesCountBack++;
					} else {
						v.mColorAlpha = 1.0f;
						currentVerticesCountFront++;
					}
				}

				v.rotateZ(curlAngle);
				v.translate(curlPos.x, curlPos.y);
				addVertex(v);
			}

			scanXmax = scanXmin;
		}

		assert (currentVerticesCount == currentVerticesCountFront
				+ currentVerticesCountBack);

		updateMeshVertices();
	}

	void updateMeshVertices() {

		int j = -1;
		int offset = -1;

		for (int idxVertice = 0; idxVertice < currentVerticesCount; idxVertice++) {

			j = idxVertice * meshVerticeStride;
			offset = -1;

			++offset;
			meshVertices[j + offset] = vertices[idxVertice].x;
			++offset;
			meshVertices[j + offset] = vertices[idxVertice].y;
			++offset;
			meshVertices[j + offset] = vertices[idxVertice].z;

			++offset;
			meshVertices[j + offset] = colors[idxVertice].toFloatBits();

			++offset;
			meshVertices[j + offset] = uvs[idxVertice].x;
			++offset;
			meshVertices[j + offset] = uvs[idxVertice].y;
			//
			// ++offset;
			// meshVertices[j + offset] = uvs[idxVertice].x;
			// ++offset;
			// meshVertices[j + offset] = uvs[idxVertice].y;
		}

		mesh.setVertices(meshVertices, 0, currentVerticesCount
				* meshVerticeStride);
	}

	public void render() {

		GLCommon gl = null;
		if (Gdx.graphics.isGL20Available())
			gl = Gdx.gl20;
		else
			gl = Gdx.gl10;

		gl.glDisable(GL10.GL_BLEND);
		gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);

		gl.glLineWidth(1.0f);

		if (!Gdx.graphics.isGL20Available()) {
			Gdx.gl10.glColor4f(1, 1, 1, 1);
		}

		if (false) { // wireFrame

			// Gdx.gl.glActiveTexture(GL10.GL_TEXTURE0 + 0);
			// Gdx.gl.glDisable(GL10.GL_TEXTURE_2D);

			if (Gdx.graphics.isGL20Available()) {

				shader.begin();
				shader.setUniformi(
						LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_X_UNIFORM + 0,
						0);
				shader.setUniformi(
						LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_Y_UNIFORM + 0,
						0);
				shader.setUniformi(
						LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_X_UNIFORM + 1,
						0);
				shader.setUniformi(
						LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_Y_UNIFORM + 1,
						0);
				shader.setUniformMatrix(
						LibGDXPlayground.ShaderProgram_PROJECTION_VIEW_MATRIX_UNIFORM,
						camera.combined);
				shader.setUniformf(
						LibGDXPlayground.ShaderProgram_VERTEX_COLOR_UNIFORM, 1,
						0, 0, 1);
				shader.setUniformi(
						LibGDXPlayground.ShaderProgram_TEXTURE_SAMPLER_UNIFORM + 0,
						0);
				shader.setUniformi(
						LibGDXPlayground.ShaderProgram_TEXTURE_SAMPLER_UNIFORM + 1,
						1);

				renderMesh(GL10.GL_LINE_STRIP, shader);

				shader.end();
			} else {
				Gdx.gl10.glColor4f(1, 0, 0, 1);

				// Gdx.gl10.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
				Gdx.gl10.glDisableClientState(GL10.GL_COLOR_ARRAY);

				renderMesh(GL10.GL_LINE_STRIP, null);
			}
		} else {

			gl.glActiveTexture(GL10.GL_TEXTURE0 + 0);
			gl.glEnable(GL10.GL_TEXTURE_2D);

			if (Gdx.graphics.isGL20Available()) {
				shader.begin();
				shader.setUniformi(
						LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_X_UNIFORM + 0,
						0);
				shader.setUniformi(
						LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_Y_UNIFORM + 0,
						0);
				shader.setUniformi(
						LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_X_UNIFORM + 1,
						0);
				shader.setUniformi(
						LibGDXPlayground.ShaderProgram_FLIP_TEXTURE_Y_UNIFORM + 1,
						0);
				shader.setUniformMatrix(
						LibGDXPlayground.ShaderProgram_PROJECTION_VIEW_MATRIX_UNIFORM,
						camera.combined);
				shader.setUniformf(
						LibGDXPlayground.ShaderProgram_VERTEX_COLOR_UNIFORM, 1,
						0, 0, 1);
				shader.setUniformi(
						LibGDXPlayground.ShaderProgram_TEXTURE_SAMPLER_UNIFORM + 0,
						0);
				shader.setUniformi(
						LibGDXPlayground.ShaderProgram_TEXTURE_SAMPLER_UNIFORM + 1,
						1);

				renderMesh(GL10.GL_TRIANGLE_STRIP, shader);

				shader.end();
			} else {
				renderMesh(GL10.GL_TRIANGLE_STRIP, null);
			}
		}
	}

	void renderMesh(int primitiveType, ShaderProgram shader) {
		if (true || currentVerticesCountBack == 0) {
			if (shader == null)
				mesh.render(primitiveType, 0, currentVerticesCountFront); // front
			else
				mesh.render(shader, primitiveType, 0, currentVerticesCountFront); // front
		} else {
			int backStartIdx = Math.max(0, currentVerticesCountFront - 2);
			int backCount = currentVerticesCountFront - backStartIdx
					+ currentVerticesCountBack;
			if (shader == null) {
				mesh.render(primitiveType, 0, currentVerticesCountFront); // front
				mesh.render(primitiveType, backStartIdx, backCount); // back
			} else {
				mesh.render(shader, primitiveType, 0, currentVerticesCountFront); // front
				mesh.render(shader, primitiveType, backStartIdx, backCount); // back
			}
		}
	}

	Vector3 point3 = new Vector3();
	Vector2 point2 = new Vector2();

	public void touchScreen(int x, int y) {
		Ray ray = camera.getPickRay(x, y);
		Intersector.intersectRayPlane(ray, plane, point3);
		touchPage(point2.set(point3.x, point3.y));
	}

	Array<Vertex> getIntersections(Array<Vertex> vertices, int[][] lineIndices,
			float scanX) {
		mIntersections.clear();

		for (int j = 0; j < lineIndices.length; j++) {
			Vertex v1 = vertices.get(lineIndices[j][0]);
			Vertex v2 = vertices.get(lineIndices[j][1]);

			if (v1.mPos.x > scanX && v2.mPos.x < scanX) {

				float c = (scanX - v2.mPos.x) / (v1.mPos.x - v2.mPos.x);
				Vertex n = mTempVertices.removeIndex(0);
				n.set(v2);
				n.mPos.x = scanX;
				n.mPos.y += (v1.mPos.y - v2.mPos.y) * c;

				n.mTex.x += (v1.mTex.x - v2.mTex.x) * c;
				n.mTex.y += (v1.mTex.y - v2.mTex.y) * c;

				n.mPenumbra.x += (v1.mPenumbra.x - v2.mPenumbra.x) * c;
				n.mPenumbra.y += (v1.mPenumbra.y - v2.mPenumbra.y) * c;

				mIntersections.add(n);
			}
		}
		return mIntersections;
	}

	int getNextHighestPO2(int n) {
		n -= 1;
		n = n | (n >> 1);
		n = n | (n >> 2);
		n = n | (n >> 4);
		n = n | (n >> 8);
		n = n | (n >> 16);
		n = n | (n >> 32);
		return n + 1;
	}

	class Vertex {
		public Vector3 mPos;
		public Vector2 mTex;
		public Vector2 mPenumbra;

		public float mColorAlpha;
		public float mColorMultiplier;

		public Vertex() {
			mPos = new Vector3();
			mTex = new Vector2();
			mPenumbra = new Vector2();

			mColorAlpha = 1;
			mColorMultiplier = 1;
		}

		public void rotateZ(float theta) {
			float cos = (float) Math.cos(theta);
			float sin = (float) Math.sin(theta);

			mPos.x = (float) (mPos.x * cos + mPos.y * sin);
			mPos.y = (float) (mPos.x * -sin + mPos.y * cos);

			mPenumbra.x = (float) (mPenumbra.x * cos + mPenumbra.y * sin);
			mPenumbra.y = (float) (mPenumbra.x * -sin + mPenumbra.y * cos);
		}

		public void set(Vertex vertex) {
			mPos.set(vertex.mPos);
			mTex.set(vertex.mTex);
			mPenumbra.set(vertex.mPenumbra);
			mColorAlpha = vertex.mColorAlpha;
			mColorMultiplier = vertex.mColorMultiplier;
		}

		public void translate(float dx, float dy) {
			mPos.add(dx, dy, 0);
		}
	}
}
