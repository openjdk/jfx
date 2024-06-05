/*
 * Copyright (c) 2013, 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package javafx.scene.paint;

import com.sun.javafx.beans.event.AbstractNotifyListener;
import com.sun.javafx.scene.paint.MaterialHelper;
import com.sun.javafx.sg.prism.NGPhongMaterial;
import com.sun.javafx.tk.Toolkit;

import javafx.beans.Observable;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.AmbientLight;
import javafx.scene.LightBase;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.shape.Shape3D;
import javafx.scene.shape.TriangleMesh;

/**
 * A material based on the Phong shading model. This material has several independent components that together give an
 * object its appearance using the <i>Phong shading model</i>. The material interacts with each illuminating light
 * separately, and the contribution from each light is summed.
 * <p>
 * The <i>diffuse</i> and <i>specular</i> components can be specified by a (solid) color and/or a texture map
 * (represented as an image). If both are applied, their values are multiplied. For example:<br>
 * {@link javafx.scene.paint.Color#LIMEGREEN Color#LIMEGREEN} *
 * <img style="vertical-align:middle;height:128px" src="doc-files/color_and_map/gradient.svg" alt="Rainbow gradient"> =
 * <img style="vertical-align:middle;height:128px" src="doc-files/color_and_map/green_gradient.svg" alt="Green gradient">
 * <p>
 * {@link javafx.scene.paint.Color#CYAN Color#CYAN} *
 * <img style="vertical-align:middle;height:128px" src="doc-files/color_and_map/map.jpg" alt="Map"> =
 * <img style="vertical-align:middle;height:128px" src="doc-files/color_and_map/map_tint.jpg" alt="Tinted map">
 * <p>
 * Note: the <i>self-illumination</i> component can not currently be specified as a color. However, a color behaves like
 * a map (of any size) of a single color. Creating a 1x1 pixel map of that color will have the same effect.
 * <p>
 * {@code PhongMaterial} is not suitable for surfaces that act like mirrors and reflect their environment, such as
 * reflective metals, water, and reflective ceramics. Neither does light refract (bend) when passing through transparent
 * or translucent materials such as water, glass, or ice. These materials rely on <i>Fresnel effects</i> that are not
 * implemented for this material.
 *
 * <h2>Components</h2>
 * <img style="float:left;height:180px" src="doc-files/components/all.svg" alt="Incident ray schematic">
 * While in the physical world each light ray goes through a single path of reflection, transmission, or absorption, in
 * the computational world a number of adjacent rays are averaged into a single one that can split into multiple paths.
 * This approximation simplifies the computation model greatly while still allowing realistic rendering. The validity of
 * this approximation depends on microscopic details of the material, but it holds well for the vast majority of cases.
 * When an averaged incident ray (blue) hits the surface, it can split into many rays depending on the values of the
 * components of the material: rays that are either transmitted through the material (green) or reflected in all
 * directions via scattering (purple) depend on the diffuse component; rays that are reflected (orange), which depend on
 * the incident angle, are controlled by the specular component.
 * <div style="overflow:auto">
 * <table style="float:right;text-align:center">
 *   <caption>Material types</caption>
 *   <tr>
 *     <td><img style="height:60px" src="doc-files/components/transparent.svg" alt="Transparent schematic"></td>
 *     <td><img style="height:60px" src="doc-files/components/lambertian.svg" alt="Lambertian schematic"></td>
 *     <td><img style="height:60px" src="doc-files/components/reflective.svg" alt="Reflective schematic"></td>
 *   </tr>
 *   <tr>
 *     <td>Transparent</td>
 *     <td>Lambertian</td>
 *     <td>Reflective</td>
 *   </tr>
 * </table>
 * <p>
 * Materials whose diffuse component allows only transmitted rays are transparent. These still have a specular component,
 * otherwise they will be invisible (no such material exists). Materials without a specular component and whose diffuse
 * component allows only reflected rays exhibit <i>Lambertian reflectance</i>. Lambertian materials reflect light in all
 * directions equally. Materials with a specular component and a diffuse component that only allows weak reflectance are
 * reflective.
 * </div>
 *
 * <h3>Diffuse</h3>
 * The diffuse component, sometimes called <i>albedo</i>, serves as the base color of the surface. It represents light
 * that is not reflected directly from the surface and instead enters the material.<br>
 * The alpha channel of the diffuse component controls the light that passes through it (transmitted). Decreasing the
 * alpha value increases the transparency of the material and causes the object to appear translucent, and ultimately
 * makes it transparent. Materials such as glass and plastics can be simulated with a low alpha value.<br>
 * Light that isn't transmitted undergoes <i>subsurface scattering</i> that causes it to be absorbed in the material or
 * be reflected back to the surface, exiting in (approximately) all directions (irrespective of the incident angle). The
 * RGB channels of the diffuse component controls which colors are absorbed and which are reflected, giving the material
 * its base color. The higher one of the RGB values is, the more that material reflects that color.
 * <p>
 * The diffuse component interacts with all lights - both those that have directionality and {@code AmbientLight}, which
 * simulates a light that comes from all directions.
 * <p>
 * <b>Important:</b> there is currently a bug that causes objects with 0 opacity to not render at all (despite having a
 * specular or a self-illumination component). Setting the opacity to 1/255 instead will give the desirable result.
 *
 * <h3>Specular</h3>
 * The specular component represents light that is reflected directly from the surface. For most materials, the color of
 * the specular component is on the gray scale regardless of the diffuse component's color. This means that the specular
 * highlight will be the light's color and not the material's color. These materials are sometimes called
 * <i>dielectrics</i>. Metals, on the other hand, reflect a color similar to their diffuse color (like yellow for gold
 * or reddish for copper) and get most of their appearance from the specular color. These materials are sometimes called
 * <i>conductors</i>.
 * <p>
 * The spread of the surface-reflected rays simulates the microgeometry that causes adjacent beams to be reflected in
 * different directions. Smooth surfaces' microgeometry varies little, causing them to have a strong specular component
 * that results in a glossy look, such as plastics, finished wood, and polished metals. Conversely, rough surfaces have
 * a varying microgeometry, weak specular component, and a matte look, such as unfinished wood, fabric, and cardboard.
 * This spread is controlled by the specular power, sometimes called <i>smoothness</i> or, conversely, <i>roughness</i>.
 * A larger specular power simulates a smoother object, which results in a smaller reflection.
 * <p>
 * The specular component interacts only with lights that have directionality (not {@code AmbientLight}) as it depends
 * on the incident ray direction, and also on the viewer (camera) position since it depends on the reflectance direction.
 * <p>
 * The alpha component of the specular color is not used at this time.
 *
 * <h3>Self-Illumination</h3>
 * The self-illumination component, also called <i>emissive</i>, represents light emitted by the object. It does not
 * interact with light sources and as such the viewer position does not matter. Specifying this component does not cause
 * the object to serve as a light source - a light has to be added at the position of the object with a color that
 * matches this color. If a multi-colored map is used, several lights of matching colors can be positioned appropriately
 * in the object's volume to give a realistic appearance.
 * <p>
 * The alpha component of the self-illumination color is not used at this time.
 *
 * <h3>Bump</h3>
 * The bump component gives the illusion of small height changes on the surface, like bumps and ridges. It is a
 * <i>normal map</i> (not a <i>height map</i> or a <i>displacement map</i>), which works by modifying the normals of
 * surfaces on the object, causing light to interact differently with the surface than it would have without it.
 * Tree trunks and rough stones can be simulated with a bump map.
 * <p>
 * Bump maps are less expensive than changing a mesh by subdividing a surface into many polygons facing different ways.
 * If the physical geometry of the surface is not important (for example, for intersection calculations), it's advised
 * to use a bump map.
 * <p>
 * The alpha component of the bump map is not used at this time.
 *
 * <h2>Mathematical Model</h2>
 * <div style="overflow:auto">
 * <img style="float:left;height:180px" src="doc-files/math/vectors.svg" alt="Vectors in the Phong model"> The image on
 * the left depicts a standard schematic of a scene with a mesh, a light source, and a camera. The black curve is the
 * required geometry, and the blue lines are the polygons (mesh) representing this geometry. Four normalized vectors are
 * considered for each point on the surface:<br>
 * <i>L</i> - the vector from the surface to the light source;<br>
 * <i>N</i> - the normal vector of the surface;<br>
 * <i>V</i> - the vector from the surface to the viewer (camera);<br>
 * <i>R</i> - the reflection vector of <i>L</i> from the surface. <i>R</i> can be calculated from <i>L</i> and <i>N</i>:
 * <i>R=2(L⋅N)N - L</i>.
 * <p>
 * The diffuse and specular components are comprised of 3 factors: the geometry, the light's color, and the material's
 * color, each considered at every point on the surface. The light's color computation is described in {@link LightBase}
 * (and its subclasses). The material's color computation, as described above, is the multiplication of the color and
 * map properties. These factors are multiplied to get the final color.
 * </div>
 *
 * <h3>Bump</h3>
 * The default normal vector of a point on the polygon is <i>N=(0, 0, 1)</i> (facing away from the surface). If a bump
 * map is specified, this vector will have a different value based on the RGB values in the bump map:
 * <i>N=2 * RGB - 1</i>. The default value for a bump map (corresponding to the default normal) is
 * <i>RGB=(0.5, 0.5, 1)</i>, which is why bump maps tend to be blueish.
 * <p>
 * We will treat <i>N</i> as the normal vector after applying a bump map, if available.
 *
 * <h3>Diffuse</h3>
 * The diffuse component represents light scattered from the surface in all directions, hence, it depends on the
 * interaction between the light and the surface (and independent of the viewer position): <i>L⋅N</i>. <i>L⋅N</i>
 * is the geometric factor of the diffuse component. It moderates the intensity of the color resulting from the light
 * hitting the surface at different angles. If the light ray is parallel to the surface, <i>L⋅N=0</i> and the diffuse
 * contribution of the light will be 0; if the light ray is perpendicular to the surface (coincides with the normal
 * vector), <i>L⋅N=1</i> and no reduction in intensity occurs.
 * <p>
 * Defining the light's color as <i>C<sub>L</sub></i>, and the material's diffuse color as <i>C<sub>DM</sub></i>, we
 * multiply the 3 factors described above: <i>L⋅N * C<sub>L</sub> * C<sub>DM</sub></i>. For <i>i</i> lights illuminating
 * the surface, the contribution of each light is summed:<br>
 * <i>Σ<sub>i</sub>(L<sub>i</sub>⋅N * C<sub>Li</sub> * C<sub>DM</sub>)
 * = Σ<sub>i</sub>(L<sub>i</sub>⋅N * C<sub>Li</sub>) * C<sub>DM</sub></i>
 * (since <i>C<sub>DM</sub></i> is a property of the material and is the same for all lights).
 * <p>
 * Since {@link AmbientLight} simulates a light coming from and scattered in all directions, it contributes fully to the
 * diffuse component (<i>L⋅N=1</i>). We will define all the ambient lights' contribution as
 * <i>A=Σ<sub>i</sub>(C<sub>Li</sub>)</i> and all the other lights' (that have a light vector) as
 * <i>D=Σ<sub>i</sub>(L<sub>i</sub>⋅N * C<sub>Li</sub>)</i>. The total diffuse component contribution is then
 * <i>(A+D) * C<sub>DM</sub></i>.
 *
 * <h3>Specular</h3>
 * The specular component represents light reflected from the surface in a mirror-like reflection, hence, it depends on
 * the interaction between the reflected light and the viewer position: <i>R⋅V</i>. As similarly explained in the
 * diffuse component section, the geometric contribution is strongest when the viewer is aligned with the reflection
 * vector and is non-existent when they are perpendicular.
 * <p>
 * <img style="float:right;height:100px" src="doc-files/math/specular_power_high.svg" alt="High specular power">
 * <img style="float:right;height:100px" src="doc-files/math/specular_power_low.svg" alt="Low specular power">
 * The specular power, <i>P</i>, represents the smoothness of the surface. Smoother surfaces have more narrow
 * reflections and their specular power is smaller (right image), while rougher surfaces have more dispersed reflections
 * and their specular power is larger (left image). Since <i>0≤R⋅V≤1</i>, the term <i>(R⋅V)<sup>P</sup></i> decreases
 * as <i>P</i> increases, giving a smaller contribution.
 * <p>
 * Like with the diffuse component, the resulting specular color is computed by multiplying the geometric factor, the
 * light's color, and the material's specular color, <i>C<sub>SM</sub></i>, for each light:<br>
 * <i>Σ<sub>i</sub>((R<sub>i</sub>⋅V)<sup>P</sup> * C<sub>Li</sub>) * C<sub>SM</sub></i>,
 * and defining the specular lights' contribution as
 * <i>S=Σ<sub>i</sub>((R<sub>i</sub>⋅V)<sup>P</sup> * C<sub>Li</sub>)</i>,
 * the total specular component contribution is <i>S * C<sub>SM</sub></i>.
 *
 * <h3>Self-Illumination</h3>
 * The self-illumination component represents light emanating from the surface, hence, it is not affected by lights, the
 * geometry, or the viewer position. Its contribution is just the material's self-illumination color,
 * <i>C<sub>LM</sub></i>.
 *
 * <h3>Summary</h3>
 *
 * The final color at the point of the computation is then:
 * <i>(A+D) * C<sub>DM</sub> + S * C<sub>SM</sub> + C<sub>LM</sub></i>.
 *
 * <h2>Examples</h2>
 * This section shows examples for simulating various common materials. Each image will be accompanied by the values
 * used for the material. Values that aren't specified are the default ones.
 *
 * <h3>Gloss</h3>
 * The specular power controls the size of specular highlights, which changes the gloss or smoothness look. Lower powers
 * create larger highlights and vice versa. Some plastics and marble exhibit this behavior, as shown here with 2
 * billiard balls:
 *
 * <table class="striped">
 *   <caption>Materials values</caption>
 *   <tr>
 *     <td>Image</td>
 *     <td><img style="height:220px" src="doc-files/gloss/yellow_low_spec.png" alt="Yellow ball with low specular power"></td>
 *     <td><img style="height:220px" src="doc-files/gloss/red_high_spec.png" alt="Red ball with high specular power"></td>
 *   </tr>
 *   <tr>
 *     <td>Diffuse color</td>
 *     <td>{@code Color.YELLOW.darker()}</td>
 *     <td>{@code Color.RED.darker()}</td>
 *   </tr>
 *   <tr>
 *     <td>Specular color</td>
 *     <td>{@code Color.WHITE}</td>
 *     <td>{@code Color.WHITE}</td>
 *   </tr>
 *   <tr>
 *     <td>Specular power</td>
 *     <td>10</td>
 *     <td>150</td>
 *   </tr>
 * </table>
 *
 * <h3>Transparency</h3>
 * Some materials are transparent/translucent, allowing most of the light through, like glass and plastics. This is
 * achieved with low diffuse opacity (alpha) values. Tint can be achieved with small RGB values in addition. The
 * smoothness of these materials also means a specular component is present with strength that depends on the
 * finish/polish of the material. A high brightness specular color gives a more glossy look and a low brightness one
 * gives a more matte look.
 *
 * <table class="striped">
 *   <caption>Material values</caption>
 *   <tr>
 *     <td>Image</td>
 *     <td><img style="height:220px" src="doc-files/transparency/no_spec.png" alt="Transparency with no specular reflection"></td>
 *     <td><img style="height:220px" src="doc-files/transparency/low_spec.png" alt="Transparency with low specular reflection"></td>
 *     <td><img style="height:220px" src="doc-files/transparency/high_spec.png" alt="Transparency with high specular reflection"></td>
 *     <td><img style="height:220px" src="doc-files/transparency/low_spec_tint.png" alt="Tinted transparency with low specular reflection"></td>
 *   </tr>
 *   <tr>
 *     <td>Diffuse color</td>
 *     <td>{@code Color.rgb(0, 0, 0, 0.3)}</td>
 *     <td>{@code Color.rgb(0, 0, 0, 0.3)}</td>
 *     <td>{@code Color.rgb(0, 0, 0, 0.3)}</td>
 *     <td>{@code Color.rgb(75, 0, 0, 0.15)}</td>
 *   </tr>
 *   <tr>
 *     <td>Specular color</td>
 *     <td>{@code Color.hsb(0, 0, 0)}</td>
 *     <td>{@code Color.hsb(0, 0, 45)}</td>
 *     <td>{@code Color.hsb(0, 0, 90)}</td>
 *     <td>{@code Color.hsb(0, 0, 45)}</td>
 *   </tr>
 * </table>
 *
 * <h3>Specular Color</h3>
 * Metals reflect their own color rather than the light's full color. In this case, the specular color should be similar
 * to the diffuse color, with its brightness affecting the shininess/polish levels. Copper and gold are shown here.
 *
 * <table class="striped">
 *   <caption>Material values</caption>
 *   <tr>
 *     <td>Image</td>
 *     <td><img style="height:220px" src="doc-files/specular_color/copper_low.png" alt="Copper with low specular reflection"></td>
 *     <td><img style="height:220px" src="doc-files/specular_color/copper_medium.png" alt="Copper with medium specular reflection"></td>
 *     <td><img style="height:220px" src="doc-files/specular_color/copper_high.png" alt="Copper with high specular reflection"></td>
 *     <td><img style="height:220px" src="doc-files/specular_color/gold_low.png" alt="Gold with low specular reflection"></td>
 *     <td><img style="height:220px" src="doc-files/specular_color/gold_high.png" alt="Gold with high specular reflection"></td>
 *   </tr>
 *   <tr>
 *     <td>Diffuse color</td>
 *     <td>{@code Color.hsb(20, 85, 70)}</td>
 *     <td>{@code Color.hsb(20, 85, 70)}</td>
 *     <td>{@code Color.hsb(20, 85, 70)}</td>
 *     <td>{@code Color.hsb(41, 82, 92)}</td>
 *     <td>{@code Color.hsb(41, 82, 92)}</td>
 *   </tr>
 *   <tr>
 *     <td>Specular color</td>
 *     <td>{@code Color.hsb(20, 85, 40)}</td>
 *     <td>{@code Color.hsb(20, 85, 70)}</td>
 *     <td>{@code Color.hsb(20, 85, 100)}</td>
 *     <td>{@code Color.hsb(41, 82, 30)}</td>
 *     <td>{@code Color.hsb(41, 82, 92)}</td>
 *   </tr>
 * </table>
 *
 * <h3>Maps and Surface Detail</h3>
 * The specular and bump maps can provide surface details that make the object look more realistic. A tree trunk, which
 * has none-to-low specularity, has a lot of grooves that can be emphasized with a bump map:
 * <div style="overflow:auto">
 *   <figure style="float:left">
 *     <img style="height:180px" src="doc-files/map_detail/diff/diff_map.png" alt="Diffuse map">
 *     <figcaption style="text-align:center">Diffuse map</figcaption>
 *   </figure>
 *   <figure style="float:left">
 *     <img style="height:180px" src="doc-files/map_detail/diff/bump_map.png" alt="Bump map">
 *     <figcaption style="text-align:center">Bump map</figcaption>
 *   </figure>
 * </div>
 *
 * <table style="text-align:center">
 *   <caption>Model with maps applied</caption>
 *   <tr>
 *     <td><img style="height:220px" src="doc-files/map_detail/diff/bump.png" alt="Tree trunk with bump map"></td>
 *     <td><img style="height:220px" src="doc-files/map_detail/diff/diff.png" alt="Tree trunk with diffuse map"></td>
 *     <td><img style="height:220px" src="doc-files/map_detail/diff/diff+bump.png" alt="Tree trunk with diffuse and bump maps"></td>
 *   </tr>
 *   <tr>
 *     <td>Bump</td>
 *     <td>Diffuse</td>
 *     <td>Bump+Diffuse</td>
 *   </tr>
 * </table>
 * A diffuse color of {@code HSB=(0, 0, 60)} has been used to darken the wood.
 * <p>
 * Polished wood, like that used in housing, has a strong specular component due to the finish and buff. A combination
 * of a specular and a bump map highlights the details in the wood:
 * <div style="overflow:auto">
 *   <figure style="float:left">
 *     <img style="height:180px" src="doc-files/map_detail/spec/diff_map.png" alt="Diffuse map">
 *     <figcaption style="text-align:center">Diffuse map</figcaption>
 *   </figure>
 *   <figure style="float:left">
 *     <img style="height:180px" src="doc-files/map_detail/spec/spec_map.png" alt="Specular map">
 *     <figcaption style="text-align:center">Specular map</figcaption>
 *   </figure>
 *   <figure style="float:left">
 *     <img style="height:180px" src="doc-files/map_detail/spec/bump_map.png" alt="Bump map">
 *     <figcaption style="text-align:center">Bump map</figcaption>
 *   </figure>
 * </div>
 *
 * <table style="text-align:center">
 *   <caption>Model with maps applied</caption>
 *   <tr>
 *     <td><img style="height:220px" src="doc-files/map_detail/spec/diff.png" alt="Finished wood with diffuse map"></td>
 *     <td><img style="height:220px" src="doc-files/map_detail/spec/diff+spec.png" alt="Finished wood with diffuse and specular maps"></td>
 *     <td><img style="height:220px" src="doc-files/map_detail/spec/diff+bump.png" alt="Finished wood with diffuse and bump maps"></td>
 *     <td><img style="height:220px" src="doc-files/map_detail/spec/diff+spec+bump.png" alt="Finished wood with diffuse, specular, and bump maps"></td>
 *   </tr>
 *   <tr>
 *     <td>Diffuse</td>
 *     <td>Diffuse+Specular</td>
 *     <td>Diffuse+Bump</td>
 *     <td>Diffuse+Specular+Bump</td>
 *   </tr>
 * </table>
 * A specular power of 100 has been used to give a more smooth look.
 *
 * <h3>Texture Animation</h3>
 * Texture animation and runtime effects can be achieved in different ways. Firstly, an animated GIF can be used as the
 * {@code Image} for texture maps, as demonstrated here when used as a diffuse map:<br>
 * <img style="height:220px" src="doc-files/texture_animation/animated_gif.gif" alt="Animation gif">
 * <img style="height:220px" src="doc-files/texture_animation/animated_gif_map.gif" alt="Animation gif as map">
 * <p>
 * Secondly, by using a {@link WritableImage}, the pixel values can be changed programmatically, creating a live texture
 * as demonstrated for the diffuse map by this code snippet that repaints the image left to right and top to bottom:
 * <div style="overflow:auto">
 * <img style="float:right;height:220px" src="doc-files/texture_animation/animated_writable.gif" alt="Writable image as map">
 * <pre>{@code WritableImage diffuseMap = ...
 * material.setDiffuseMap(diffuseMap);
 * var timer = new AnimationTimer() {
 *     int x, y;
 *
 *     @Override
 *     public void handle(long now) {
 *         diffuseMap.getPixelWriter().setColor(x, y, Color.color(0, 0, 1, 0.5));
 *         x++;
 *         if (x > diffuseMap.getWidth() - 1) {
 *             x = 0;
 *             y++;
 *             if (y > diffuseMap.getHeight() - 1) {
 *                 stop();
 *             }
 *         }
 *     }
 * };
 * timer.start();
 * }</pre>
 * </div>
 * Other maps can be modified as well, producing various effects.<br>
 * Another way to animate textures is done through changing the {@link TriangleMesh#getTexCoords() texture coordinates}
 * of the mesh, the explanation for which is out of scope for this class.
 *
 * @see LightBase
 * @see Shape3D
 * @since JavaFX 8.0
 */
public class PhongMaterial extends Material {

    private boolean diffuseColorDirty = true;
    private boolean specularColorDirty = true;
    private boolean specularPowerDirty = true;
    private boolean diffuseMapDirty = true;
    private boolean specularMapDirty = true;
    private boolean bumpMapDirty = true;
    private boolean selfIlluminationMapDirty = true;

    /**
     * Creates a new instance of {@code PhongMaterial} class with a default {@code Color.WHITE diffuseColor} property.
     */
    public PhongMaterial() {
        setDiffuseColor(Color.WHITE);
    }

    /**
     * Creates a new instance of {@code PhongMaterial} class using the specified
     * color for its {@code diffuseColor} property.
     *
     * @param diffuseColor the color of the diffuseColor property
     */
    public PhongMaterial(Color diffuseColor) {
        setDiffuseColor(diffuseColor);
    }

    /**
     * Creates a new instance of {@code PhongMaterial} class using the specified
     * colors and images for its {@code diffuseColor} properties.
     *
     * @param diffuseColor the color of the diffuseColor property
     * @param diffuseMap the image of the diffuseMap property
     * @param specularMap the image of the specularMap property
     * @param bumpMap the image of the bumpMap property
     * @param selfIlluminationMap the image of the selfIlluminationMap property
     */
    public PhongMaterial(Color diffuseColor, Image diffuseMap,
            Image specularMap, Image bumpMap, Image selfIlluminationMap) {
        setDiffuseColor(diffuseColor);
        setDiffuseMap(diffuseMap);
        setSpecularMap(specularMap);
        setBumpMap(bumpMap);
        setSelfIlluminationMap(selfIlluminationMap);
    }

    /**
     * The diffuse color of this {@code PhongMaterial}.
     *
     * @defaultValue {@code Color.WHITE}
     */
    private ObjectProperty<Color> diffuseColor;

    public final void setDiffuseColor(Color value) {
        diffuseColorProperty().set(value);
    }

    public final Color getDiffuseColor() {
        return diffuseColor == null ? null : diffuseColor.get();
    }

    public final ObjectProperty<Color> diffuseColorProperty() {
        if (diffuseColor == null) {
            diffuseColor = new SimpleObjectProperty<>(PhongMaterial.this, "diffuseColor") {
                @Override
                protected void invalidated() {
                    diffuseColorDirty = true;
                    setDirty(true);
                }
            };
        }
        return diffuseColor;
    }

    /**
     * The specular color of this {@code PhongMaterial}.
     *
     * @defaultValue {@code null}
     */
    private ObjectProperty<Color> specularColor;

    public final void setSpecularColor(Color value) {
        specularColorProperty().set(value);
    }

    public final Color getSpecularColor() {
        return specularColor == null ? null : specularColor.get();
    }

    public final ObjectProperty<Color> specularColorProperty() {
        if (specularColor == null) {
            specularColor = new SimpleObjectProperty<>(PhongMaterial.this, "specularColor") {
                @Override
                protected void invalidated() {
                    specularColorDirty = true;
                    setDirty(true);
                }
            };
        }
        return specularColor;
    }

    /**
     * The specular power of this {@code PhongMaterial}.
     *
     * @defaultValue 32.0
     */
    private DoubleProperty specularPower;

    public final void setSpecularPower(double value) {
        specularPowerProperty().set(value);
    }

    public final double getSpecularPower() {
        return specularPower == null ? 32 : specularPower.get();
    }

    public final DoubleProperty specularPowerProperty() {
        if (specularPower == null) {
            specularPower = new SimpleDoubleProperty(PhongMaterial.this, "specularPower", 32.0) {
                @Override
                public void invalidated() {
                    specularPowerDirty = true;
                    setDirty(true);
                }
            };
        }
        return specularPower;
    }

    private final AbstractNotifyListener platformImageChangeListener = new AbstractNotifyListener() {
        @Override
        public void invalidated(Observable valueModel) {
            if (oldDiffuseMap != null
                    && valueModel == Toolkit.getImageAccessor().getImageProperty(oldDiffuseMap)) {
                diffuseMapDirty = true;
            } else if (oldSpecularMap != null
                    && valueModel == Toolkit.getImageAccessor().getImageProperty(oldSpecularMap)) {
                specularMapDirty = true;
            } else if (oldBumpMap != null
                    && valueModel == Toolkit.getImageAccessor().getImageProperty(oldBumpMap)) {
                bumpMapDirty = true;
            } else if (oldSelfIlluminationMap != null
                    && valueModel == Toolkit.getImageAccessor().getImageProperty(oldSelfIlluminationMap)) {
                selfIlluminationMapDirty = true;
            }
            setDirty(true);
        }
    };

    /**
     * The diffuse map of this {@code PhongMaterial}.
     *
     * @defaultValue {@code null}
     */
    // TODO: 3D - Texture or Image? For Media it might be better to have it as a Texture
    private ObjectProperty<Image> diffuseMap;

    public final void setDiffuseMap(Image value) {
        diffuseMapProperty().set(value);
    }

    public final Image getDiffuseMap() {
        return diffuseMap == null ? null : diffuseMap.get();
    }

    private Image oldDiffuseMap;

    public final ObjectProperty<Image> diffuseMapProperty() {
        if (diffuseMap == null) {
            diffuseMap = new SimpleObjectProperty<>(PhongMaterial.this, "diffuseMap") {

                private boolean needsListeners = false;

                @Override
                public void invalidated() {
                    Image _image = get();

                    if (needsListeners) {
                        Toolkit.getImageAccessor().getImageProperty(oldDiffuseMap).
                                removeListener(platformImageChangeListener.getWeakListener());
                    }

                    needsListeners = _image != null && (Toolkit.getImageAccessor().isAnimation(_image)
                            || _image.getProgress() < 1);

                    if (needsListeners) {
                        Toolkit.getImageAccessor().getImageProperty(_image).
                                addListener(platformImageChangeListener.getWeakListener());
                    }
                    oldDiffuseMap = _image;
                    diffuseMapDirty = true;
                    setDirty(true);
                }
            };
        }
        return diffuseMap;
    }

    /**
     * The specular map of this {@code PhongMaterial}.
     *
     * @defaultValue {@code null}
     */
    // TODO: 3D - Texture or Image? For Media it might be better to have it as a Texture
    private ObjectProperty<Image> specularMap;

    public final void setSpecularMap(Image value) {
        specularMapProperty().set(value);
    }

    public final Image getSpecularMap() {
        return specularMap == null ? null : specularMap.get();
    }

    private Image oldSpecularMap;

    public final ObjectProperty<Image> specularMapProperty() {
        if (specularMap == null) {
            specularMap = new SimpleObjectProperty<>(PhongMaterial.this, "specularMap") {

                private boolean needsListeners = false;

                @Override
                public void invalidated() {
                    Image _image = get();

                    if (needsListeners) {
                        Toolkit.getImageAccessor().getImageProperty(oldSpecularMap).
                                removeListener(platformImageChangeListener.getWeakListener());
                    }

                    needsListeners = _image != null && (Toolkit.getImageAccessor().isAnimation(_image)
                            || _image.getProgress() < 1);

                    if (needsListeners) {
                        Toolkit.getImageAccessor().getImageProperty(_image).
                                addListener(platformImageChangeListener.getWeakListener());
                    }

                    oldSpecularMap = _image;
                    specularMapDirty = true;
                    setDirty(true);
                }
            };
        }
        return specularMap;
    }

    /**
     * The bump map of this {@code PhongMaterial}, which is a normal map stored as an RGB image.
     *
     * @defaultValue {@code null}
     */
    // TODO: 3D - Texture or Image? For Media it might be better to have it as a Texture
    private ObjectProperty<Image> bumpMap;

    public final void setBumpMap(Image value) {
        bumpMapProperty().set(value);
    }

    public final Image getBumpMap() {
        return bumpMap == null ? null : bumpMap.get();
    }

    private Image oldBumpMap;

    public final ObjectProperty<Image> bumpMapProperty() {
        if (bumpMap == null) {
            bumpMap = new SimpleObjectProperty<>(PhongMaterial.this, "bumpMap") {

                private boolean needsListeners = false;

                @Override
                public void invalidated() {
                    Image _image = get();

                    if (needsListeners) {
                        Toolkit.getImageAccessor().getImageProperty(oldBumpMap).
                                removeListener(platformImageChangeListener.getWeakListener());
                    }

                    needsListeners = _image != null && (Toolkit.getImageAccessor().isAnimation(_image)
                            || _image.getProgress() < 1);

                    if (needsListeners) {
                        Toolkit.getImageAccessor().getImageProperty(_image).
                                addListener(platformImageChangeListener.getWeakListener());
                    }

                    oldBumpMap = _image;
                    bumpMapDirty = true;
                    setDirty(true);
                }
            };
        }
        return bumpMap;
    }

    /**
     * The self illumination map of this {@code PhongMaterial}.
     *
     * @defaultValue {@code null}
     */
    // TODO: 3D - Texture or Image? For Media it might be better to have it as a Texture
    private ObjectProperty<Image> selfIlluminationMap;

    public final void setSelfIlluminationMap(Image value) {
        selfIlluminationMapProperty().set(value);
    }

    public final Image getSelfIlluminationMap() {
        return selfIlluminationMap == null ? null : selfIlluminationMap.get();
    }

    private Image oldSelfIlluminationMap;

    public final ObjectProperty<Image> selfIlluminationMapProperty() {
        if (selfIlluminationMap == null) {
            selfIlluminationMap = new SimpleObjectProperty<>(PhongMaterial.this, "selfIlluminationMap") {

                private boolean needsListeners = false;

                @Override
                public void invalidated() {
                    Image _image = get();

                    if (needsListeners) {
                        Toolkit.getImageAccessor().getImageProperty(oldSelfIlluminationMap).
                                removeListener(platformImageChangeListener.getWeakListener());
                    }

                    needsListeners = _image != null && (Toolkit.getImageAccessor().isAnimation(_image)
                            || _image.getProgress() < 1);

                    if (needsListeners) {
                        Toolkit.getImageAccessor().getImageProperty(_image).
                                addListener(platformImageChangeListener.getWeakListener());
                    }

                    oldSelfIlluminationMap = _image;
                    selfIlluminationMapDirty = true;
                    setDirty(true);
                }
            };
        }
        return selfIlluminationMap;
    }

    @Override
    void setDirty(boolean value) {
        super.setDirty(value);
        if (!value) {
            diffuseColorDirty = false;
            specularColorDirty = false;
            specularPowerDirty = false;
            diffuseMapDirty = false;
            specularMapDirty = false;
            bumpMapDirty = false;
            selfIlluminationMapDirty = false;
        }
    }

    /** The peer node created by the graphics Toolkit/Pipeline implementation */
    private NGPhongMaterial peer;

    @Override
    NGPhongMaterial getNGMaterial() {
        if (peer == null) {
            peer = new NGPhongMaterial();
        }
        return peer;
    }

    @Override
    void updatePG() {
        if (!isDirty()) {
            return;
        }

        final NGPhongMaterial pMaterial = MaterialHelper.getNGMaterial(this);
        if (diffuseColorDirty) {
            pMaterial.setDiffuseColor(getDiffuseColor() == null ? null
                    : Toolkit.getPaintAccessor().getPlatformPaint(getDiffuseColor()));
        }
        if (specularColorDirty) {
            pMaterial.setSpecularColor(getSpecularColor() == null ? null
                    : Toolkit.getPaintAccessor().getPlatformPaint(getSpecularColor()));
        }
        if (specularPowerDirty) {
            pMaterial.setSpecularPower((float)getSpecularPower());
        }
        if (diffuseMapDirty) {
            pMaterial.setDiffuseMap(getDiffuseMap()
                    == null ? null : Toolkit.getImageAccessor().getPlatformImage(getDiffuseMap()));
        }
        if (specularMapDirty) {
            pMaterial.setSpecularMap(getSpecularMap()
                    == null ? null : Toolkit.getImageAccessor().getPlatformImage(getSpecularMap()));
        }
        if (bumpMapDirty) {
            pMaterial.setBumpMap(getBumpMap()
                    == null ? null : Toolkit.getImageAccessor().getPlatformImage(getBumpMap()));
        }
        if (selfIlluminationMapDirty) {
            pMaterial.setSelfIllumMap(getSelfIlluminationMap()
                    == null ? null : Toolkit.getImageAccessor().getPlatformImage(getSelfIlluminationMap()));
        }

        setDirty(false);
    }

    @Override public String toString() {
        return "PhongMaterial[" + "diffuseColor=" + getDiffuseColor() +
                ", specularColor=" + getSpecularColor() +
                ", specularPower=" + getSpecularPower() +
                ", diffuseMap=" + getDiffuseMap() +
                ", specularMap=" + getSpecularMap() +
                ", bumpMap=" + getBumpMap() +
                ", selfIlluminationMap=" + getSelfIlluminationMap() + "]";
    }

}
