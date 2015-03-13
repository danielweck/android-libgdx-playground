package daniel.weck;

import com.badlogic.gdx.graphics.Texture;

public class SubTexture {
	Texture texture;
	TextureSqueezer textureSqueezer;

	boolean flipX = false;
	boolean flipY = false;
	
	int top = -1;
	int left = -1;
	int width = -1;
	int height = -1;

	public SubTexture(Texture texture) {
		// this(texture, 0, 0, texture.getWidth(), texture.getHeight());
		this.texture = texture;
		this.left = 0;
		this.top = 0;
		this.width = texture.getWidth();
		this.height = texture.getHeight();
	}

	// SubTexture(Texture texture, int left, int top, int width, int height) {
	// this.texture = texture;
	// this.left = left;
	// this.top = top;
	// this.width = width;
	// this.height = height;
	// }

	public SubTexture(TextureSqueezer textureSqueezer, int left, int top,
			int width, int height) {
		this.textureSqueezer = textureSqueezer;
		this.left = left;
		this.top = top;
		this.width = width;
		this.height = height;
	}

	// SubTexture copy() {
	// return new SubTexture(this.texture, this.left, this.top, this.width,
	// this.height);
	// }
}