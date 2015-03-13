package daniel.weck;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

public class TextureSqueezer {

	// configuration
	final int xPadding = 0;
	final int yPadding = 0;
	final boolean allowRotate = false;

	class Node {
		Node child1, child2;
		SubTexture subTexture;

		Pixmap pix;
		String name;
		boolean rotate;

		public Node(TextureSqueezer squeezer, int left, int top, int width,
				int height) {
			this.subTexture = new SubTexture(squeezer, left, top, width, height);
		}

		public SubTexture subTexture(String name) {
			// System.out.println("--- " + name + "=>" + this.name);

			if (name.equals(this.name))
				return this.subTexture;

			if (this.child1 != null) {
				SubTexture sub = this.child1.subTexture(name);
				if (sub != null) {
					return sub;
				}
			}
			if (this.child2 != null) {
				SubTexture sub = this.child2.subTexture(name);
				if (sub != null) {
					return sub;
				}
			}

			return null;
		}

		public Node insert(Pixmap pixmap, int imageLeft, int imageTop,
				int imageWidth, int imageHeight, String name, boolean rotate) {

			if (pixmap != null && this.pix != null || pixmap == null
					&& this.name != null) {
				return null;
			}

			if (this.child1 != null) {
				Node newNode = this.child1.insert(pixmap, imageLeft, imageTop,
						imageWidth, imageHeight, name, false);
				if (newNode == null) {
					if (allowRotate) {
						newNode = this.child1.insert(pixmap, imageLeft,
								imageTop, imageWidth, imageHeight, name, true);
					}
				}
				if (newNode != null) {
					return newNode;
				}

				newNode = this.child2.insert(pixmap, imageLeft, imageTop,
						imageWidth, imageHeight, name, false);
				if (newNode == null) {
					if (allowRotate) {
						newNode = this.child2.insert(pixmap, imageLeft,
								imageTop, imageWidth, imageHeight, name, true);
					}
				}
				return newNode;
			}
			// int imageWidth = pixmap.getWidth();
			// int imageHeight = pixmap.getHeight();
			if (rotate) {
				int temp = imageWidth;
				imageWidth = imageHeight;
				imageHeight = temp;
			}

			int neededWidth = imageWidth + xPadding;
			int neededHeight = imageHeight + yPadding;
			if (neededWidth > this.subTexture.width
					|| neededHeight > this.subTexture.height) {
				return null;
			}

			if (neededWidth == this.subTexture.width
					&& neededHeight == this.subTexture.height) {
				if (pixmap == null) {
					assert (imageLeft == this.subTexture.left);
					assert (imageTop == this.subTexture.top);
				}
				this.pix = pixmap;
				this.name = name;
				this.rotate = rotate;
				return this;
			}

			int dw = this.subTexture.width - neededWidth;
			int dh = this.subTexture.height - neededHeight;
			if (dw > dh) {
				this.child1 = new Node(this.subTexture.textureSqueezer,
						this.subTexture.left, this.subTexture.top, neededWidth,
						this.subTexture.height);
				this.child2 = new Node(this.subTexture.textureSqueezer,
						this.subTexture.left + neededWidth,
						this.subTexture.top, this.subTexture.width
								- neededWidth, this.subTexture.height);
			} else {
				this.child1 = new Node(this.subTexture.textureSqueezer,
						this.subTexture.left, this.subTexture.top,
						this.subTexture.width, neededHeight);
				this.child2 = new Node(this.subTexture.textureSqueezer,
						this.subTexture.left, this.subTexture.top
								+ neededHeight, this.subTexture.width,
						this.subTexture.height - neededHeight);
			}
			return this.child1.insert(pixmap, imageLeft, imageTop, imageWidth,
					imageHeight, name, rotate);
		}

		void draw(Pixmap pixmap) {
			if (this.pix != null) {
				pixmap.drawPixmap(this.pix, this.subTexture.left + xPadding,
						this.subTexture.top + yPadding, 0, 0,
						this.pix.getWidth(), this.pix.getHeight());
			}

			if (this.child1 != null) {
				this.child1.draw(pixmap);
			}

			if (this.child2 != null) {
				this.child2.draw(pixmap);
			}
		}

		void disposePixmap() {
			if (this.pix != null) {
				this.pix.dispose();
			}

			if (this.child1 != null) {
				this.child1.disposePixmap();
			}

			if (this.child2 != null) {
				this.child2.disposePixmap();
			}
		}

		void write(OutputStreamWriter writer) throws IOException {

			if (this.name != null) {
				writer.write(this.name);
				writer.write('\n');
				writer.write(Integer.toString(this.subTexture.left));
				writer.write('\n');
				writer.write(Integer.toString(this.subTexture.top));
				writer.write('\n');
				writer.write(Integer.toString(this.subTexture.width));
				writer.write('\n');
				writer.write(Integer.toString(this.subTexture.height));
				writer.write('\n');
				writer.write(this.rotate ? '1' : '0');
				writer.write('\n');
			}

			if (this.child1 != null) {
				this.child1.write(writer);
			}

			if (this.child2 != null) {
				this.child2.write(writer);
			}
		}

		public void texture(Texture texture) {

			if (this.pix != null) {
				if (this.subTexture.texture != null) {
					if (!this.subTexture.texture.isManaged()) {
						this.pix.dispose();
					}
					this.subTexture.texture.dispose();
				} else {
					this.pix.dispose();
				}
			}
			this.subTexture.texture = texture;

			if (this.child1 != null) {
				this.child1.texture(texture);
			}

			if (this.child2 != null) {
				this.child2.texture(texture);
			}
		}
	}

	Node rootNode;
	Pixmap pixmap;
	String pixmapPath;
	boolean textureFinalized;
	boolean allDrawn;

	public TextureSqueezer(int width, int height) {
		this.rootNode = new Node(this, 0, 0, width, height);
		allDrawn = true;
		textureFinalized = false;
		pixmap = null;
	}

	public SubTexture subTexture(String name) {
		SubTexture sub = this.rootNode.subTexture(name);
		// if (sub == null) {
		// int debugbreaker = 1;
		// }
		return sub;
	}

	public SubTexture insert(FileHandle file,
	// Pixmap pixmap, String name,
			boolean draw) {
		Pixmap pixmap = new Pixmap(file);
		String name = file.name();

		Node node = this.rootNode.insert(pixmap, 0, 0, pixmap.getWidth(),
				pixmap.getHeight(), name, false);
		if (node == null) {
			if (this.allowRotate) {
				node = this.rootNode.insert(pixmap, 0, 0, pixmap.getWidth(),
						pixmap.getHeight(), name, true);
			}
			if (node == null) {
				pixmap.dispose();
				return null;
			}
		}
		if (draw) {
			checkPixmap();
			node.draw(this.pixmap);
		} else {
			this.allDrawn = false;
		}
		return node.subTexture;
	}

	void read(BufferedReader reader) throws IOException {

		String line;
		while ((line = reader.readLine()) != null && line != "") {
			String name = line;

			int left = Integer.parseInt(reader.readLine());
			int top = Integer.parseInt(reader.readLine());
			int width = Integer.parseInt(reader.readLine());
			int height = Integer.parseInt(reader.readLine());
			String rotate = reader.readLine();

			Node node = this.rootNode.insert(null, left, top, width, height,
					name, false);
			if (node == null) {
				if (this.allowRotate) {
					assert (rotate.equals("1"));
					node = this.rootNode.insert(null, left, top, width, height,
							name, true);
				}
			} else {
				assert (rotate.equals("0"));
			}
		}
	}

	// void read(BufferedReader reader) throws IOException {
	// this.rootNode.read(reader, this.allowRotate);
	// }

	boolean open(String directoryPath, String fileNameNoExt) {

		String path = directoryPath + "/" + fileNameNoExt + ".png";
		FileHandle image = Gdx.files.external(path);
		FileHandle data = Gdx.files.external(directoryPath + "/"
				+ fileNameNoExt + ".txt");
		if (!image.exists() || !data.exists()) {
			return false;
		}

		InputStream stream = data.read();
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				stream, Charset.forName("US-ASCII")));

		try {
			String line = reader.readLine();
			assert (line.equals(fileNameNoExt + ".png"));

			read(reader);

			reader.close();

		} catch (Exception e) {
			// e.printStackTrace();
			try {
				reader.close();
			} catch (IOException e1) {
				// e1.printStackTrace();
			}

			// if (data.exists()) {
			// data.delete();
			// }

			return false;
		}

		// pixmap = new Pixmap(image);
		pixmapPath = path;

		Texture texture = new Texture(image);
		rootNode.texture(texture);
		textureFinalized = true;

		return true;
	}

	public boolean save(String directoryPath, String fileNameNoExt) {

		String path = directoryPath + "/" + fileNameNoExt + ".png";
		FileHandle image = Gdx.files.external(path);
		FileHandle data = Gdx.files.external(directoryPath + "/"
				+ fileNameNoExt + ".txt");

		OutputStream stream = data.write(false);
		OutputStreamWriter writer = new OutputStreamWriter(stream,
				Charset.forName("US-ASCII"));
		try {
			writer.write(image.name());
			writer.write('\n');

			write(writer);

			writer.close();

		} catch (IOException e) {
			// e.printStackTrace();

			try {
				writer.close();
			} catch (IOException e1) {
				// e1.printStackTrace();
			}
			return false;
		}

		if (!allDrawn) {
			draw();
		}

		pixmapPath = path;

		// PngEncoder png = new PngEncoder(pixmap, true, 0, 0);
		// byte[] bytes = png.pngEncode();

		OutputStream stream_ = image.write(false);
		try {
			byte[] bytes = PNG.toPNG(pixmap);
			stream_.write(bytes);
			stream_.close();
		} catch (IOException e) {
			// e.printStackTrace();
			try {
				stream_.close();
			} catch (IOException e1) {
				// e.printStackTrace();
			}
			return false;
		}

		// this.pixmap.dispose();

		return true;
	}

	void checkPixmap() {
		if (this.pixmap == null) {
			this.pixmap = new Pixmap(this.rootNode.subTexture.width,
					this.rootNode.subTexture.height, Format.RGBA8888);
		}
	}

	Pixmap draw() {
		checkPixmap();

		this.rootNode.draw(this.pixmap);

		return this.pixmap;
	}

	void write(OutputStreamWriter writer) throws IOException {
		this.rootNode.write(writer);
	}

	public void textureFinalize() {
		this.pixmap.dispose();
		FileHandle file = Gdx.files.external(this.pixmapPath);
		Texture texture = new Texture(file);
		rootNode.texture(texture);
		textureFinalized = true;
	}
}
