package com.sun.javafx.scene.control.skin;

import com.sun.javafx.css.StyleablePropertyMetaData;
import com.sun.javafx.css.Origin;
import com.sun.javafx.css.converters.BooleanConverter;
import com.sun.javafx.css.converters.EnumConverter;
import com.sun.javafx.css.converters.PaintConverter;
import com.sun.javafx.css.converters.SizeConverter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javafx.beans.property.Property;
import javafx.beans.value.WritableValue;
import javafx.scene.control.Labeled;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

/**
 * LabeledText allows the Text to be styled by the CSS properties of Labeled
 * that are meant to style the textual component of the Labeled.
 *
 * LabeledText has the style class "text"
 */
public class LabeledText extends Text {

   private final Labeled labeled;

   public LabeledText(Labeled labeled) {
       super();

       if (labeled == null) {
           throw new IllegalArgumentException("labeled cannot be null");
       }

       this.labeled = labeled;

       //
       // init the state of this Text object to that of the Labeled
       //
       this.setFill(this.labeled.getTextFill());
       this.setFont(this.labeled.getFont());
       this.setTextAlignment(this.labeled.getTextAlignment());
       this.setUnderline(this.labeled.isUnderline());
       this.setLineSpacing(this.labeled.getLineSpacing());

       //
       // Bind the state of this Text object to that of the Labeled.
       // Binding these properties prevents CSS from setting them
       //
       this.fillProperty().bind(this.labeled.textFillProperty());
       this.fontProperty().bind(this.labeled.fontProperty());
       // do not bind text - Text doesn't have -fx-text
       this.textAlignmentProperty().bind(this.labeled.textAlignmentProperty());
       this.underlineProperty().bind(this.labeled.underlineProperty());
       this.lineSpacingProperty().bind(this.labeled.lineSpacingProperty());

       getStyleClass().addAll("text");
   }

   /**
    * @treatAsPrivate implementation detail
    * @deprecated This is an internal API that is not intended for use and will be removed in the next version
    */
   @Deprecated
   @Override public List<StyleablePropertyMetaData> getStyleablePropertyMetaData() {
       return getClassStyleablePropertyMetaData();
   }

   /**
    * @treatAsPrivate implementation detail
    * @deprecated This is an internal API that is not intended for use and will be removed in the next version
    */
   @Deprecated
   public static List<StyleablePropertyMetaData> getClassStyleablePropertyMetaData() {
       return StyleableProperties.STYLEABLES;
   }

   //
   // Replace all of Text's StyleablePropertyMetaData instances that overlap with Labeled
   // with instances of StyleablePropertyMetaData that redirect to Labeled. Thus, when
   // the Labeled is styled,
   //
   private static class StyleableProperties {

       private static final StyleablePropertyMetaData<LabeledText,Font> FONT =
           new StyleablePropertyMetaData.FONT<LabeledText>("-fx-font", Font.getDefault()) {

            @Override
            public void set(LabeledText node, Font value, Origin origin) {
                //
                // In the case where the Labeled's textFill was set by an
                // inline style, this inline style should override values
                // from lesser origins.
                //
                WritableValue<Font> prop = node.labeled.fontProperty();
                Origin propOrigin = StyleablePropertyMetaData.getOrigin(prop);

                //
                // if propOrigin is null, then the property is in init state
                // if origin is null, then some code is initializing this prop
                // if propOrigin is greater than origin, then the style should
                //    not override
                //
                if (propOrigin == null ||
                    origin == null ||
                    propOrigin.compareTo(origin) <= 0) {
                    super.set(node, value, origin);
                }
            }

           @Override
           public boolean isSettable(LabeledText node) {
               return node.labeled != null ? node.labeled.fontProperty().isBound() == false : true;
           }

           @Override
           public WritableValue<Font> getWritableValue(LabeledText node) {
               return node.labeled != null ? node.labeled.fontProperty() : null;
           }
       };


       private static final StyleablePropertyMetaData<LabeledText,Paint> FILL =
           new StyleablePropertyMetaData<LabeledText,Paint>("-fx-fill",
               PaintConverter.getInstance(), Color.BLACK) {

            @Override
            public void set(LabeledText node, Paint value, Origin origin) {
                //
                // In the case where the Labeled's textFill was set by an
                // inline style, this inline style should override values
                // from lesser origins.
                //
                WritableValue<Paint> prop = node.labeled.textFillProperty();
                Origin propOrigin = StyleablePropertyMetaData.getOrigin(prop);

                //
                // if propOrigin is null, then the property is in init state
                // if origin is null, then some code is initializing this prop
                // if propOrigin is greater than origin, then the style should
                //    not override
                //
                if (propOrigin == null ||
                    origin == null ||
                    propOrigin.compareTo(origin) <= 0) {
                    super.set(node, value, origin);
                }
            }

           @Override
           public boolean isSettable(LabeledText node) {
               return node.labeled.textFillProperty().isBound() == false;
           }

           @Override
           public WritableValue<Paint> getWritableValue(LabeledText node) {
               return node.labeled.textFillProperty();
           }
       };

        private static final StyleablePropertyMetaData<LabeledText,TextAlignment> TEXT_ALIGNMENT =
                new StyleablePropertyMetaData<LabeledText,TextAlignment>("-fx-text-alignment",
                new EnumConverter<TextAlignment>(TextAlignment.class),
                TextAlignment.LEFT) {

            @Override
            public void set(LabeledText node, TextAlignment value, Origin origin) {
                //
                // In the case where the Labeled's textFill was set by an
                // inline style, this inline style should override values
                // from lesser origins.
                //
                WritableValue<TextAlignment> prop = node.labeled.textAlignmentProperty();
                Origin propOrigin = StyleablePropertyMetaData.getOrigin(prop);

                //
                // if propOrigin is null, then the property is in init state
                // if origin is null, then some code is initializing this prop
                // if propOrigin is greater than origin, then the style should
                //    not override
                //
                if (propOrigin == null ||
                    origin == null ||
                    propOrigin.compareTo(origin) <= 0) {
                    super.set(node, value, origin);
                }
            }

            @Override
            public boolean isSettable(LabeledText node) {
                return node.labeled.textAlignmentProperty().isBound() == false;
            }

            @Override
            public WritableValue<TextAlignment> getWritableValue(LabeledText node) {
                return node.labeled.textAlignmentProperty();
            }
        };

        private static final StyleablePropertyMetaData<LabeledText,Boolean> UNDERLINE =
                new StyleablePropertyMetaData<LabeledText,Boolean>("-fx-underline",
                BooleanConverter.getInstance(),
                Boolean.FALSE) {

            @Override
            public void set(LabeledText node, Boolean value, Origin origin) {
                //
                // In the case where the Labeled's textFill was set by an
                // inline style, this inline style should override values
                // from lesser origins.
                //
                WritableValue<Boolean> prop = node.labeled.underlineProperty();
                Origin propOrigin = StyleablePropertyMetaData.getOrigin(prop);

                //
                // if propOrigin is null, then the property is in init state
                // if origin is null, then some code is initializing this prop
                // if propOrigin is greater than origin, then the style should
                //    not override
                //
                if (propOrigin == null ||
                    origin == null ||
                    propOrigin.compareTo(origin) <= 0) {
                    super.set(node, value, origin);
                }
            }

            @Override
            public boolean isSettable(LabeledText node) {
                return node.labeled.underlineProperty().isBound() == false;
            }

            @Override
            public WritableValue<Boolean> getWritableValue(LabeledText node) {
                return node.labeled.underlineProperty();
            }
        };

        private static final StyleablePropertyMetaData<LabeledText,Number> LINE_SPACING =
                new StyleablePropertyMetaData<LabeledText,Number>("-fx-line-spacing",
                SizeConverter.getInstance(),
                0) {

            @Override
            public void set(LabeledText node, Number value, Origin origin) {
                //
                // In the case where the Labeled's textFill was set by an
                // inline style, this inline style should override values
                // from lesser origins.
                //
                WritableValue<Number> prop = node.labeled.lineSpacingProperty();
                Origin propOrigin = StyleablePropertyMetaData.getOrigin(prop);

                //
                // if propOrigin is null, then the property is in init state
                // if origin is null, then some code is initializing this prop
                // if propOrigin is greater than origin, then the style should
                //    not override
                //
                if (propOrigin == null ||
                    origin == null ||
                    propOrigin.compareTo(origin) <= 0) {
                    super.set(node, value, origin);
                }
            }

            @Override
            public boolean isSettable(LabeledText node) {
                return node.labeled.lineSpacingProperty().isBound() == false;
            }

            @Override
            public WritableValue<Number> getWritableValue(LabeledText node) {
                return node.labeled.lineSpacingProperty();
            }
        };

       private static final List<StyleablePropertyMetaData> STYLEABLES;
       static {

           final List<StyleablePropertyMetaData> styleables =
               new ArrayList<StyleablePropertyMetaData>(Text.getClassStyleablePropertyMetaData());

           for (int n=0,nMax=styleables.size(); n<nMax; n++) {
               final String prop = styleables.get(n).getProperty();

               if ("-fx-fill".equals(prop)) {
                   styleables.set(n, FILL);
               } else if ("-fx-font".equals(prop)) {
                   styleables.set(n, FONT);
               } else if ("-fx-text-alignment".equals(prop)) {
                   styleables.set(n, TEXT_ALIGNMENT);
               } else if ("-fx-underline".equals(prop)) {
                   styleables.set(n, UNDERLINE);
               } else if ("-fx-line-spacing".equals(prop)) {
                   styleables.set(n, LINE_SPACING);
               }
           }

           STYLEABLES = Collections.unmodifiableList(styleables);
       }
   }
}
