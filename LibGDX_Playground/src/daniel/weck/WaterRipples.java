package daniel.weck;

import java.nio.FloatBuffer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Plane;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;

public class WaterRipples {

	final float DAMPING = 0.9f;
	final float DISPLACEMENT = -10;
	final float TICK = 0.033f;
	final int RADIUS = 3;

	float accum;

	Mesh mesh;
	Plane plane;

	Vector3 point3 = new Vector3();
	Vector2 point2 = new Vector2();

	float[][] last;
	float[][] curr;
	// float[][] intp;

	Camera camera;
	float zDepthCoord = 0;

	float posx;
	float posy;

	SubTexture subtexture;

	short width = 50;
	short height = 50;

	ShaderProgram shader;

	public WaterRipples(ShaderProgram s, Camera c, float z, float xpos,
			float ypos, short w, short h, SubTexture subtex) {

		shader = s;

		subtexture = subtex;

		camera = c;

		posx = xpos;
		posy = ypos;
		zDepthCoord = z;

		width = w;
		height = h;

		plane = new Plane(new Vector3(0, 0, zDepthCoord), new Vector3(1, 0,
				zDepthCoord), new Vector3(0, 1, zDepthCoord));

		last = new float[width + 1][height + 1];
		curr = new float[width + 1][height + 1];
		// intp = new float[width + 1][height + 1];

		int nIndices = width * height * 6;

		int nVertices = (width + 1) * (height + 1);

		mesh = new Mesh(true, nVertices, nIndices, new VertexAttribute(
				VertexAttributes.Usage.Position, 3,
				ShaderProgram.POSITION_ATTRIBUTE), new VertexAttribute(
				VertexAttributes.Usage.TextureCoordinates, 2,
				ShaderProgram.TEXCOORD_ATTRIBUTE + "0"));

		// init indices
		short[] indices = new short[nIndices];
		int idx = 0;
		short vidx = 0;
		for (int y = 0; y < height; y++) {
			vidx = (short) (y * (width + 1));

			for (int x = 0; x < width; x++) {
				indices[idx++] = vidx;
				indices[idx++] = (short) (vidx + 1);
				indices[idx++] = (short) (vidx + width + 1);

				indices[idx++] = (short) (vidx + 1);
				indices[idx++] = (short) (vidx + width + 2);
				indices[idx++] = (short) (vidx + width + 1);

				vidx++;
			}
		}
		mesh.setIndices(indices);

		vertices = new float[nVertices * 5];
		idx = 0;
		for (int y = 0; y <= height; y++) {
			for (int x = 0; x <= width; x++) {

				vertices[idx++] = 0;
				vertices[idx++] = 0;
				vertices[idx++] = 0;
				vertices[idx++] = 0;
				vertices[idx++] = 0;
			}
		}
		mesh.setVertices(vertices);

		updateVertices(curr, 0);

		noripple = true;
	}

	float[] vertices;
	boolean updateDirectBufferAccess = true;

	boolean noripple = false;

	float interpolate(float alpha, int x, int y) {

		// // interpolation
		// for (int y = 0; y <= height; y++) {
		// for (int x = 0; x <= width; x++) {
		// intp[x][y] = (alpha * last[x][y] + (1 - alpha) * curr[x][y]);
		// }
		// }

		return alpha * last[x][y] + (1 - alpha) * curr[x][y];
	}

	final float NO_RIPPLE_TOLERANCE = 0.05f;

	void updateVertices(float[][] curr, float alpha) {

		FloatBuffer buffer = null;
		float floatsPerVertex = mesh.getVertexSize() / 4f;

		if (updateDirectBufferAccess) {
			int numVertices = mesh.getNumVertices();
			float floatBufferSize = numVertices * floatsPerVertex;

			buffer = mesh.getVerticesBuffer();
			// for (int bi = 0 ; bi< buffer.limit(); bi++)
			assert (floatBufferSize == buffer.limit());
			// for (int bi = 0; bi < floatBufferSize; bi += floatsPerVertex)

		}

		noripple = true;

		int idx = 0;
		for (int y = 0; y <= height; y++) {
			for (int x = 0; x <= width; x++) {
				float xOffset = 0;
				float yOffset = 0;

				if (x > 0 && x < width && y > 0 && y < height) {

					float c1 = interpolate(alpha, x - 1, y);
					float c2 = interpolate(alpha, x + 1, y);
					float c3 = interpolate(alpha, x, y - 1);
					float c4 = interpolate(alpha, x, y + 1);

					xOffset = (c1 - c2);
					yOffset = (c3 - c4);

					noripple = noripple && xOffset >= -NO_RIPPLE_TOLERANCE
							&& xOffset <= NO_RIPPLE_TOLERANCE
							&& yOffset >= -NO_RIPPLE_TOLERANCE
							&& yOffset <= NO_RIPPLE_TOLERANCE;
				}

				if (updateDirectBufferAccess) {
					buffer.put(idx + 0, x + posx); // x
					buffer.put(idx + 1, y + posy); // y
					buffer.put(idx + 2, zDepthCoord); // z
				} else {
					vertices[idx++] = x + posx; // x
					vertices[idx++] = y + posy; // y
					vertices[idx++] = zDepthCoord; // z
				}

				float u = (x + xOffset) / (float)width;
				float v = 1 - ((y + yOffset) / (float)height); // (FLIPPED)

				float wRatio = subtexture.width / (float)width;
				float hRatio = subtexture.height / (float)height;

				u = ((x + xOffset) * wRatio + subtexture.left)
						/ (float)subtexture.texture.getWidth();
				v = 1 - (((y + yOffset) * hRatio + subtexture.top) / (float)subtexture.texture
						.getHeight());

				if (updateDirectBufferAccess) {
					buffer.put(idx + 3, u); // u
					buffer.put(idx + 4, v); // v
					idx += floatsPerVertex;
				} else {
					vertices[idx++] = u; // u
					vertices[idx++] = v; // v
				}
			}
		}

		if (!updateDirectBufferAccess) {
			mesh.setVertices(vertices);
		}

		// } else {

		// noripple = true;
		//
		// int idx = 0;
		// for (int y = 0; y <= height; y++) {
		// for (int x = 0; x <= width; x++) {
		// float xOffset = 0;
		// float yOffset = 0;
		//
		// if (x > 0 && x < width && y > 0 && y < height) {
		//
		// float c1 = interpolate(alpha, x - 1, y);
		// float c2 = interpolate(alpha, x + 1, y);
		// float c3 = interpolate(alpha, x, y - 1);
		// float c4 = interpolate(alpha, x, y + 1);
		//
		// xOffset = (c1 - c2);
		// yOffset = (c3 - c4);
		//
		// noripple = noripple && xOffset >= -NO_RIPPLE_TOLERANCE
		// && xOffset <= NO_RIPPLE_TOLERANCE
		// && yOffset >= -NO_RIPPLE_TOLERANCE
		// && yOffset <= NO_RIPPLE_TOLERANCE;
		// }
		//
		// vertices[idx++] = x + posx; // x
		// vertices[idx++] = y + posy; // y
		// vertices[idx++] = zDepthCoord; // z
		//
		// float u = (x + xOffset) / width;
		// float v = 1 - ((y + yOffset) / height); // (FLIPPED)
		//
		// float wRatio = subtexture.width / width;
		// float hRatio = subtexture.height / height;
		//
		// u = ((x + xOffset) * wRatio + subtexture.left)
		// / subtexture.texture.getWidth();
		// v = 1 - (((y + yOffset) * hRatio + subtexture.top) /
		// subtexture.texture
		// .getHeight());
		//
		// vertices[idx++] = u; // u
		// vertices[idx++] = v; // v
		// }
		// }
		// mesh.setVertices(vertices);
		// }
	}

	boolean computingTouchArray = false;

	void touchWater(Vector2 point) {

		computingTouchArray = true;

		float px = point.x - posx;
		float py = point.y - posy;

		for (int y = Math.max(0, (int) py - RADIUS); y < Math.min(height,
				(int) py + RADIUS); y++) {
			for (int x = Math.max(0, (int) px - RADIUS); x < Math.min(width,
					(int) px + RADIUS); x++) {

				// point.dst2(x, y, zDepthCoord)
				float a = x - px;
				float b = y - py;
				a *= a;
				b *= b;
				float dst2 = a + b;

				float val = curr[x][y]
						+ DISPLACEMENT
						* Math.max(
								0,
								(float) Math.cos(Math.PI / 2 * Math.sqrt(dst2)
										/ (float)RADIUS));
				if (val < DISPLACEMENT)
					val = DISPLACEMENT;
				else if (val > -DISPLACEMENT)
					val = -DISPLACEMENT;
				curr[x][y] = val;
			}
		}

		computingTouchArray = false;

		noripple = false;
	}

	public void touchScreen(int x, int y) {
		Ray ray = camera.getPickRay(x, y);
		Intersector.intersectRayPlane(ray, plane, point3);
		touchWater(point2.set(point3.x, point3.y));
	}

	public void render(boolean directBufferAccess) {

		updateDirectBufferAccess = directBufferAccess;

		Gdx.gl.glDisable(GL10.GL_BLEND);
		Gdx.gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);

		Gdx.gl.glLineWidth(1.0f);

		if (!Gdx.graphics.isGL20Available()) {
			Gdx.gl10.glColor4f(1, 1, 1, 1);
		}

		if (noripple) {
			accum = TICK;

			if (true) { // wireFrame

				// Gdx.gl.glActiveTexture(GL10.GL_TEXTURE0 + 0);
				// Gdx.gl.glDisable(GL10.GL_TEXTURE_2D);

				if (Gdx.graphics.isGL20Available()) {

					shader.begin();
					shader.setUniformMatrix(
							LibGDXPlayground.ShaderProgram_PROJECTION_VIEW_MATRIX_UNIFORM,
							camera.combined);
					shader.setUniformf(
							LibGDXPlayground.ShaderProgram_VERTEX_COLOR_UNIFORM,
							1, 0, 0, 1);
					shader.setUniformi(
							LibGDXPlayground.ShaderProgram_TEXTURE_SAMPLER_UNIFORM + 0,
							0);

					mesh.render(shader, GL10.GL_LINE_STRIP); //GL_TRIANGLES 

					shader.end();
				} else {
					Gdx.gl10.glColor4f(1, 0, 0, 1);

					//Gdx.gl10.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
					Gdx.gl10.glDisableClientState(GL10.GL_COLOR_ARRAY);

					mesh.render(GL10.GL_LINE_STRIP); //GL_TRIANGLES 
				}
			} else {

				Gdx.gl.glActiveTexture(GL10.GL_TEXTURE0 + 0);
				Gdx.gl.glEnable(GL10.GL_TEXTURE_2D);

				if (Gdx.graphics.isGL20Available()) {
					shader.begin();
					shader.setUniformMatrix(
							LibGDXPlayground.ShaderProgram_PROJECTION_VIEW_MATRIX_UNIFORM,
							camera.combined);
					shader.setUniformf(
							LibGDXPlayground.ShaderProgram_VERTEX_COLOR_UNIFORM,
							1, 1, 1, 1);
					shader.setUniformi(
							LibGDXPlayground.ShaderProgram_TEXTURE_SAMPLER_UNIFORM + 0,
							0);

					mesh.render(shader, GL10.GL_TRIANGLES);

					shader.end();
				} else {
					mesh.render(GL10.GL_TRIANGLES);
				}
			}
			return;
		}

		accum += Gdx.graphics.getDeltaTime();

		while (accum > TICK) {

			// // 5-point multitouch
			// for (int i = 0; i < 5; i++) {
			// if (Gdx.input.isTouched(i)) {
			// touchScreen(Gdx.input.getX(i), Gdx.input.getY(i));
			// }
			// }

			while (computingTouchArray) {

				// DO SOMETHING..ANYTHING :)
				updateDirectBufferAccess = directBufferAccess;

				// try {
				// Thread.sleep(3);
				// } catch (InterruptedException e) {
				// }
			}

			// ripple update
			for (int y = 0; y <= height; y++) {
				for (int x = 0; x <= width; x++) {
					if (x > 0 && x < width && y > 0 && y < height) {
						curr[x][y] = (last[x - 1][y] + last[x + 1][y]
								+ last[x][y + 1] + last[x][y - 1])
								/ 4 - curr[x][y];
					}
					curr[x][y] *= DAMPING;
				}
			}

			float[][] tmp = curr;
			curr = last;
			last = tmp;
			accum -= TICK;
		}

		float alpha = accum / TICK;

		// // interpolation
		// for (int y = 0; y <= height; y++) {
		// for (int x = 0; x <= width; x++) {
		// intp[x][y] = (alpha * last[x][y] + (1 - alpha) * curr[x][y]);
		// }
		// }

		// updateVertices(intp);

		updateVertices(curr, alpha);

		Gdx.gl.glActiveTexture(GL10.GL_TEXTURE0 + 0);
		Gdx.gl.glEnable(GL10.GL_TEXTURE_2D);

		if (Gdx.graphics.isGL20Available()) {

			shader.begin();
			shader.setUniformMatrix(
					LibGDXPlayground.ShaderProgram_PROJECTION_VIEW_MATRIX_UNIFORM,
					camera.combined);
			shader.setUniformf(
					LibGDXPlayground.ShaderProgram_VERTEX_COLOR_UNIFORM, 1, 1,
					1, 1);
			shader.setUniformi(
					LibGDXPlayground.ShaderProgram_TEXTURE_SAMPLER_UNIFORM + 0,
					0);

			mesh.render(shader, GL10.GL_TRIANGLES);

			shader.end();
		} else {
			mesh.render(GL10.GL_TRIANGLES);
		}
	}
}
