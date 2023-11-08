package com.sun.javafx.scene.paint;

// When the public API is ready, move to the exported javafx.scene.paint
/**
 * Contains texture properties for a {@code Material}'s textures (maps).
 * <p>
 * Since {@code TextureData} does not depend on a {@code Material}, the same instance can be reused in multiple textures
 * (in the same or in different materials). This is useful because many times a {@link javafx.scene.shape.Shape3D} will
 * have all its textures use the same properties, or the same texture data will be used across the whole application.
 */
// Here we can support mipmaps, wrapping modes (which exists internally and can be pulled out), addressing modes etc.
public class TextureData {

    /**
     * Filtering methods for sampling a magnified or minified texture. When a texture is used to color a surface that is
     * larger or smaller than the texture size, the texture needs to be magnified or minified. Different algorithms
     * exist for sampling the texture elements (texels) used to color the surface.
     */
    public enum MinMagFilterType {

        /**
         * Uses the texel closest to the sampling point. This sampling method is the fastest, but the results can be
         * pixelated.
         */
        NEAREST_POINT,

        /**
         * Uses a weighted average of the 4 texels closest to the sampling point. This sampling method is slightly
         * slower than {@code NEAREST_POINT}, but results in a smoother image.
         */
        BILINEAR;
    }

    /**
     * Filtering methods for sampling between mipmaps. When a surface is at a distance that does not match a single
     * mipmap different algorithms exist for interpolating between the closest mipmap levels. Mipmapping is only valid
     * for minification since the base texture is the largest size image.
     */
    public enum MipmapFilterType {

        /**
         * No mimaping is used.
         */
        NONE,
        /**
         * Chooses the nearest mipmap level. This is a cheap method, but can result in abrupt changes when the mipmap
         * level is switched.
         */
        NEAREST,
        /**
         * Linearly interpolates between the 2 closest mipmap levels. This method is slightly slower than
         * {@code NEAREST}, but the result is a smooth transition between mipmap levels.
         */
        LINEAR;
    }

    private final MinMagFilterType minFilterType;
    private final MinMagFilterType magFilterType;
    private final MipmapFilterType mipmapFilterType;

    private TextureData(MinMagFilterType minFilterType, MinMagFilterType magFilterType, MipmapFilterType mipmapFilterType) {
        this.minFilterType = minFilterType;
        this.magFilterType = magFilterType;
        this.mipmapFilterType = mipmapFilterType;
    }

    public MinMagFilterType minFilterType() { return minFilterType; }
    public MinMagFilterType magFilterType() { return magFilterType; }
    public MipmapFilterType mipmapFilterType() { return mipmapFilterType; }

    public static class Builder {

        private MinMagFilterType minFilterType = MinMagFilterType.BILINEAR;
        private MinMagFilterType magFilterType = MinMagFilterType.BILINEAR;
        private MipmapFilterType mipmapFilterType = MipmapFilterType.NONE;

        public Builder() {}

        public Builder minFilterType(MinMagFilterType minFilterType) {
            this.minFilterType = minFilterType;
            return this;
        }

        public Builder magFilterType(MinMagFilterType magFilterType) {
            this.magFilterType = magFilterType;
            return this;
        }

        public Builder mipmapFilterType(MipmapFilterType mipmapFilterType) {
            this.mipmapFilterType = mipmapFilterType;
            return this;
        }

        public TextureData build() {
            return new TextureData(minFilterType, magFilterType, mipmapFilterType);
        }
    }

    /**
     * Mipmap images to be used when mipmapping is enabled. The images in this list must each be a power-of-2 smaller size
     * than the previous one. The image used as the main texture (level 0) should not be included.
     * To enable mipmapping, set {@link MipmapFilterType} to a value other than {@link MipmapFilterType#NONE NONE}.
     */
//    private ObservableList<Image> mipmaps;
//
//    private int mipmapLevels;

//    enum WrapMode { // see com.sun.prism.Texture.WrapMode
//    }
}