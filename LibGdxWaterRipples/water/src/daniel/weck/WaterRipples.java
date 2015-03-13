package daniel.weck;

import java.nio.FloatBuffer;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Texture;
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
	final float DISPLACEMENT = -8;
	final float TICK = 0.033f;
	final int RADIUS = 2;

	final static short CellSuggestedDimension = 26;

	boolean drawWireframeWhenNoRipple = true;

	float accum;

	Mesh mesh;
	Plane plane;

	Vector3 point3 = new Vector3();
	Vector2 point2 = new Vector2();

	float[][] last;
	float[][] curr;
	// float[][] intp;

	float zDepthCoord = 0;
	float posx;
	float posy;
	short width = (short) (1024 / (float) CellSuggestedDimension);
	short height = (short) (600 / (float) CellSuggestedDimension);

	ShaderProgram shader;
	Texture texture;

	public void updateZ(float z){
		zDepthCoord = z;

		plane = new Plane(new Vector3(0, 0, zDepthCoord), new Vector3(1, 0,
				zDepthCoord), new Vector3(0, 1, zDepthCoord));
	}
	
	public WaterRipples( //
			float z, //
			float xpos, float ypos, //
			short w, short h, //
			Texture tex) {

		if (Gdx.graphics.isGL20Available()) {
			createShaders();
		}

		updateZ(z);
		
		texture = tex;

		posx = xpos;
		posy = ypos;

		width = w;
		height = h;


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

				float u = (x + xOffset) / (float) width;
				float v = 1 - ((y + yOffset) / (float) height); // (FLIPPED)

				float wRatio = texture.getWidth() / (float) width;
				float hRatio = texture.getHeight() / (float) height;

				u = ((x + xOffset) * wRatio + 0) / (float) texture.getWidth();
				v = 1 - (((y + yOffset) * hRatio + 0) / (float) texture
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
										/ (float) RADIUS));
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

	public void touchScreen(Camera camera, int x, int y) {
		Ray ray = camera.getPickRay(x, y);
		Intersector.intersectRayPlane(ray, plane, point3);
		touchWater(point2.set(point3.x, point3.y));
	}

	public void render(Camera camera, boolean directBufferAccess) {

		updateDirectBufferAccess = directBufferAccess;

		Gdx.gl.glDisable(GL10.GL_BLEND);
		Gdx.gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);

		Gdx.gl.glLineWidth(1.0f);

		if (!Gdx.graphics.isGL20Available()) {
			Gdx.gl10.glColor4f(1, 1, 1, 1);
		}

		if (noripple) {
			accum = TICK;

			Gdx.gl.glActiveTexture(GL10.GL_TEXTURE0 + 0);
			Gdx.gl.glEnable(GL10.GL_TEXTURE_2D);

			if (Gdx.graphics.isGL20Available()) {
				shader.begin();
				shader.setUniformMatrix(
						ShaderProgram_PROJECTION_VIEW_MATRIX_UNIFORM,
						camera.combined);
				shader.setUniformf(ShaderProgram_VERTEX_COLOR_UNIFORM, 1, 1, 1,
						1);
				shader.setUniformi(ShaderProgram_TEXTURE_SAMPLER_UNIFORM, 1);
				shader.setUniformi(ShaderProgram_TEXTURE_SAMPLER_UNIFORM + 0, 0);

				mesh.render(shader, GL10.GL_TRIANGLES);

				shader.end();
			} else {
				mesh.render(GL10.GL_TRIANGLES);
			}

			if (drawWireframeWhenNoRipple) {

				boolean disableTexture = Gdx.app.getType() != ApplicationType.Desktop;

				if (disableTexture) {
					Gdx.gl.glActiveTexture(GL10.GL_TEXTURE0 + 0);
					Gdx.gl.glDisable(GL10.GL_TEXTURE_2D);
				}

				if (Gdx.graphics.isGL20Available()) {

					shader.begin();
					shader.setUniformMatrix(
							ShaderProgram_PROJECTION_VIEW_MATRIX_UNIFORM,
							camera.combined);
					shader.setUniformf(ShaderProgram_VERTEX_COLOR_UNIFORM, 1,
							0, 0, 1);

					shader.setUniformi(ShaderProgram_TEXTURE_SAMPLER_UNIFORM,
							disableTexture ? 0 : 1);

					shader.setUniformi(
							ShaderProgram_TEXTURE_SAMPLER_UNIFORM + 0, 0);

					mesh.render(shader, GL10.GL_LINE_STRIP); // GL_TRIANGLES

					shader.end();
				} else {
					Gdx.gl10.glColor4f(1, 0, 0, 1);

					// Gdx.gl10.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
					Gdx.gl10.glDisableClientState(GL10.GL_COLOR_ARRAY);

					mesh.render(GL10.GL_LINE_STRIP); // GL_TRIANGLES
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
					ShaderProgram_PROJECTION_VIEW_MATRIX_UNIFORM,
					camera.combined);
			shader.setUniformf(ShaderProgram_VERTEX_COLOR_UNIFORM, 1, 1, 1, 1);
			shader.setUniformi(ShaderProgram_TEXTURE_SAMPLER_UNIFORM, 1);
			shader.setUniformi(ShaderProgram_TEXTURE_SAMPLER_UNIFORM + 0, 0);

			mesh.render(shader, GL10.GL_TRIANGLES);

			shader.end();
		} else {
			mesh.render(GL10.GL_TRIANGLES);
		}
	}

	protected void createShaders() {
		String vertexShader = createVertexShader(false, false, 1);
		String fragmentShader = createFragmentShader(false, false, 1);
		shader = new ShaderProgram(vertexShader, fragmentShader);
		if (!shader.isCompiled())
			throw new IllegalArgumentException("Couldn't compile shader: "
					+ shader.getLog());
	}

	public static final String ShaderProgram_PROJECTION_VIEW_MATRIX_UNIFORM = "u_projectionViewMatrix";
	public static final String ShaderProgram_TEXTURE_SAMPLER_UNIFORM = "u_texture";

	public static final String ShaderProgram_VERTEX_COLOR_UNIFORM = "u_color";
	public static final String ShaderProgram_FLIP_TEXTURE_X_UNIFORM = "u_flipTexU";
	public static final String ShaderProgram_FLIP_TEXTURE_Y_UNIFORM = "u_flipTexV";

	public static String createVertexShader(boolean hasNormals,
			boolean hasColors, int numTexCoords) {

		String shader = "#ifdef GL_ES\n" + "#define LOWP lowp\n"
				+ "precision mediump float;\n" + "#else\n" + "#define LOWP \n"
				// + "precision highp float;\n"
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

		String shader = "#ifdef GL_ES\n" + "#define LOWP lowp\n"
				+ "precision mediump float;\n" + "#else\n" + "#define LOWP \n"
				// + "precision highp float;\n"
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

		shader += "uniform int " + ShaderProgram_TEXTURE_SAMPLER_UNIFORM
				+ ";\n";

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

					+ "\n vec4 texture" + 0 + " = ("
					+ ShaderProgram_TEXTURE_SAMPLER_UNIFORM
					+ " == 1 ? texture2D("
					+ ShaderProgram_TEXTURE_SAMPLER_UNIFORM + 0 + ",  texCoord"
					+ 0 + ") : vec4(1,1,1,1));";

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

					+ "\n vec4 texture" + 1 + " = ("
					+ ShaderProgram_TEXTURE_SAMPLER_UNIFORM
					+ " == 1 ? texture2D("
					+ ShaderProgram_TEXTURE_SAMPLER_UNIFORM + 1 + ",  texCoord"
					+ 1 + ") : vec4(1,1,1,1));";

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

				shader += " (" + ShaderProgram_TEXTURE_SAMPLER_UNIFORM
						+ " == 1 ? texture2D("
						+ ShaderProgram_TEXTURE_SAMPLER_UNIFORM + i
						+ ",  v_tex" + i + ") : vec4(1,1,1,1))";

				if (i != numTexCoords - 1) {
					shader += "*";
				}
			}
		}

		shader += ";\n}";

		return shader;
	}
}
