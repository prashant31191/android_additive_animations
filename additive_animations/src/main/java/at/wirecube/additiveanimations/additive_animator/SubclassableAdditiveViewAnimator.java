package at.wirecube.additiveanimations.additive_animator;

import android.annotation.SuppressLint;
import android.graphics.Path;
import android.os.Build;
import android.support.v4.view.ViewCompat;
import android.util.Property;
import android.view.View;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import at.wirecube.additiveanimations.helper.AnimationUtils;
import at.wirecube.additiveanimations.helper.evaluators.ColorEvaluator;
import at.wirecube.additiveanimations.helper.propertywrappers.ColorProperties;
import at.wirecube.additiveanimations.helper.propertywrappers.ElevationProperties;
import at.wirecube.additiveanimations.helper.propertywrappers.MarginProperties;
import at.wirecube.additiveanimations.helper.propertywrappers.PaddingProperties;
import at.wirecube.additiveanimations.helper.propertywrappers.ScrollProperties;
import at.wirecube.additiveanimations.helper.propertywrappers.SizeProperties;

public abstract class SubclassableAdditiveViewAnimator<T extends SubclassableAdditiveViewAnimator> extends BaseAdditiveAnimator<T, View> {

    protected boolean mSkipRequestLayout = false;
    protected boolean mWithLayer = false;

    public static float getTargetPropertyValue(Property<View, Float> property, View v) {
        return AdditiveAnimationStateManager.from(v).getActualPropertyValue(property);
    }

    public static Float getTargetPropertyValue(String propertyName, View v) {
        return AdditiveAnimationStateManager.from(v).getLastTargetValue(propertyName);
    }

    protected static Float getQueuedPropertyValue(String propertyName, View v) {
        return AdditiveAnimationStateManager.from(v).getQueuedPropertyValue(propertyName);
    }

    @Override
    public T target(View view) {
        if(mWithLayer) {
            withLayer();
        }
        return super.target(view);
    }

    @Override
    protected T setParent(T other) {
        super.setParent(other);
        mSkipRequestLayout = other.mSkipRequestLayout;
        mWithLayer = other.mWithLayer;
        return self();
    }

    @Override
    void applyChanges(List<AccumulatedAnimationValue<View>> accumulatedAnimations) {
        Map<View, List<AccumulatedAnimationValue<View>>> unknownProperties = null;
        Set<View> viewsToRequestLayoutFor = new HashSet<>(1);
        for(AccumulatedAnimationValue<View> accumulatedAnimationValue : accumulatedAnimations) {
            View targetView = accumulatedAnimationValue.animation.getTarget();
            viewsToRequestLayoutFor.add(targetView);
            if(accumulatedAnimationValue.animation.getProperty() != null) {
                accumulatedAnimationValue.animation.getProperty().set(targetView, accumulatedAnimationValue.tempValue);
            } else {
                if(unknownProperties == null) {
                    unknownProperties = new HashMap<>();
                }
                List<AccumulatedAnimationValue<View>> accumulatedValues = unknownProperties.get(targetView);
                if(accumulatedValues == null) {
                    accumulatedValues = new ArrayList<>();
                    unknownProperties.put(targetView, accumulatedValues);
                }
                accumulatedValues.add(accumulatedAnimationValue);
            }
        }

        if(unknownProperties != null) {
            for (View v : unknownProperties.keySet()) {
                HashMap<String, Float> properties = new HashMap<>();
                for(AccumulatedAnimationValue value : unknownProperties.get(v)) {
                    properties.put(value.animation.getTag(), value.tempValue);
                }
                applyCustomProperties(properties, v);
            }
        }

        for(View v : viewsToRequestLayoutFor) {
            if(!ViewCompat.isInLayout(v) && !mSkipRequestLayout) {
                v.requestLayout();
            }
        }
    }

    public T skipRequestLayout() {
        mSkipRequestLayout = true;
        return self();
    }

    /**
     * Activates hardware layers.
     * This property will be applied to all subsequent target views and child animators (created by `then...()` methods) as well.
     * Note that
     */
    public T withLayer() {
        if(mCurrentStateManager != null) {
            mCurrentStateManager.setUseHardwareLayer(true);
        }
        mSkipRequestLayout = true;
        mWithLayer = true;
        return self();
    }

    /**
     * Deactivates hardware layers for the current view and all subsequently added ones.
     */
    public T withoutLayer() {
        if(mCurrentStateManager != null) {
            mCurrentStateManager.setUseHardwareLayer(false);
        }
        mWithLayer = false;
        return self();
    }

    public T backgroundColor(int color) {
        return animate(ColorProperties.BACKGROUND_COLOR, color, new ColorEvaluator());
    }

    public T scaleX(float scaleX) {
        return animate(View.SCALE_X, scaleX);
    }

    public T scaleXBy(float scaleXBy) {
        return animatePropertyBy(View.SCALE_X, scaleXBy);
    }

    public T scaleY(float scaleY) {
        return animate(View.SCALE_Y, scaleY);
    }

    public T scaleYBy(float scaleYBy) {
        return animatePropertyBy(View.SCALE_Y, scaleYBy);
    }

    public T scale(float scale) {
        scaleY(scale);
        scaleX(scale);
        return (T)this;
    }

    public T scaleBy(float scalesBy) {
        scaleYBy(scalesBy);
        scaleXBy(scalesBy);
        return self();
    }

    public T translationX(float translationX) {
        return animate(View.TRANSLATION_X, translationX);
    }

    public T translationXBy(float translationXBy) {
        return animatePropertyBy(View.TRANSLATION_X, translationXBy);
    }

    public T translationY(float translationY) {
        return animate(View.TRANSLATION_Y, translationY);
    }

    public T translationYBy(float translationYBy) {
        return animatePropertyBy(View.TRANSLATION_Y, translationYBy);
    }

    @SuppressLint("NewApi")
    public T translationZ(float translationZ) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            animate(View.TRANSLATION_Z, translationZ);
        }
        return self();
    }

    @SuppressLint("NewApi")
    public T translationZBy(float translationZBy) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            animatePropertyBy(View.TRANSLATION_Z, translationZBy);
        }
        return self();
    }

    public T alpha(float alpha) {
        return animate(View.ALPHA, alpha);
    }

    public T alphaBy(float alphaBy) {
        return animatePropertyBy(View.ALPHA, alphaBy);
    }

    public T rotation(float rotation) {
        float currentValue = getTargetPropertyValue(View.ROTATION);
        if(getQueuedPropertyValue(View.ROTATION.getName()) != null) {
            currentValue = getQueuedPropertyValue(View.ROTATION.getName());
        }
        float shortestDistance = AnimationUtils.shortestAngleBetween(currentValue, rotation);
        return animatePropertyBy(View.ROTATION, shortestDistance);
    }

    public T rotationBy(float rotationBy) {
        return animatePropertyBy(View.ROTATION, rotationBy);
    }

    public T rotationX(float rotationX) {
        float currentValue = getTargetPropertyValue(View.ROTATION_X);
        if(getQueuedPropertyValue(View.ROTATION_X.getName()) != null) {
            currentValue = getQueuedPropertyValue(View.ROTATION_X.getName());
        }
        float shortestDistance = AnimationUtils.shortestAngleBetween(currentValue, rotationX);
        return animatePropertyBy(View.ROTATION_X, shortestDistance);
    }

    public T rotationXBy(float rotationXBy) {
        return animatePropertyBy(View.ROTATION_X, rotationXBy);
    }

    public T rotationY(float rotationY) {
        float currentValue = getTargetPropertyValue(View.ROTATION_Y);
        if(getQueuedPropertyValue(View.ROTATION_Y.getName()) != null) {
            currentValue = getQueuedPropertyValue(View.ROTATION_Y.getName());
        }
        float shortestDistance = AnimationUtils.shortestAngleBetween(getTargetPropertyValue(View.ROTATION_Y), rotationY);
        return animatePropertyBy(View.ROTATION_Y, shortestDistance);
    }

    public T rotationYBy(float rotationYBy) {
        return animatePropertyBy(View.ROTATION_Y, rotationYBy);
    }

    public T x(float x) {
        return animate(View.X, x);
    }

    public T xBy(float xBy) {
        return animatePropertyBy(View.X, xBy);
    }

    public T centerX(float centerX) {
        return animate(View.X, centerX - mCurrentTarget.getWidth() / 2);
    }

    public T y(float y) {
        return animate(View.Y, y);
    }

    public T yBy(float yBy) {
        return animatePropertyBy(View.Y, yBy);
    }

    public T centerY(float centerY) {
        return animate(View.Y, centerY - mCurrentTarget.getHeight() / 2);
    }

    @SuppressLint("NewApi")
    public T z(float z) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            animate(View.Z, z);
        }
        return self();
    }

    @SuppressLint("NewApi")
    public T zBy(float zBy) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            animatePropertyBy(View.Z, zBy);
        }
        return self();
    }

    public T xyAlongPath(Path path) {
        return animatePropertiesAlongPath(View.X, View.Y, null, path);
    }

    public T translationXYAlongPath(Path path) {
        return animatePropertiesAlongPath(View.TRANSLATION_X, View.TRANSLATION_Y, null, path);
    }

    public T xyRotationAlongPath(Path path) {
        return animatePropertiesAlongPath(View.X, View.Y, View.ROTATION, path);
    }

    public T translationXYRotationAlongPath(Path path) {
        return animatePropertiesAlongPath(View.TRANSLATION_X, View.TRANSLATION_Y, View.ROTATION, path);
    }

    public T leftMargin(int leftMargin) {
        return animate(MarginProperties.MARGIN_LEFT, leftMargin);
    }

    public T leftMarginBy(int leftMarginBy) {
        return animatePropertyBy(MarginProperties.MARGIN_LEFT, leftMarginBy);
    }

    public T topMargin(int topMargin) {
        return animate(MarginProperties.MARGIN_TOP, topMargin);
    }

    public T topMarginBy(int topMarginBy) {
        return animatePropertyBy(MarginProperties.MARGIN_TOP, topMarginBy);
    }

    public T rightMargin(int rightMargin) {
        return animate(MarginProperties.MARGIN_RIGHT, rightMargin);
    }

    public T rightMarginBy(int rightMarginBy) {
        return animatePropertyBy(MarginProperties.MARGIN_RIGHT, rightMarginBy);
    }

    public T bottomMargin(int bottomMargin) {
        return animate(MarginProperties.MARGIN_BOTTOM, bottomMargin);
    }

    public T bottomMarginBy(int bottomMarginBy) {
        return animatePropertyBy(MarginProperties.MARGIN_BOTTOM, bottomMarginBy);
    }

    public T horizontalMargin(int horizontalMargin) {
        leftMargin(horizontalMargin);
        rightMargin(horizontalMargin);
        return self();
    }

    public T horizontalMarginBy(int horizontalMarginBy) {
        leftMarginBy(horizontalMarginBy);
        rightMarginBy(horizontalMarginBy);
        return self();
    }

    public T verticalMargin(int verticalMargin) {
        topMargin(verticalMargin);
        bottomMargin(verticalMargin);
        return self();
    }

    public T verticalMarginBy(int verticalMarginBy) {
        topMarginBy(verticalMarginBy);
        bottomMarginBy(verticalMarginBy);
        return self();
    }

    public T margin(int margin) {
        leftMargin(margin);
        rightMargin(margin);
        topMargin(margin);
        bottomMargin(margin);
        return self();
    }

    public T marginBy(int marginBy) {
        leftMarginBy(marginBy);
        rightMarginBy(marginBy);
        topMarginBy(marginBy);
        bottomMarginBy(marginBy);
        return self();
    }

    public T topLeftMarginAlongPath(Path path) {
        return animatePropertiesAlongPath(MarginProperties.MARGIN_LEFT, MarginProperties.MARGIN_TOP, null, path);
    }

    public T topRightMarginAlongPath(Path path) {
        return animatePropertiesAlongPath(MarginProperties.MARGIN_RIGHT, MarginProperties.MARGIN_TOP, null, path);
    }

    public T bottomRightMarginAlongPath(Path path) {
        return animatePropertiesAlongPath(MarginProperties.MARGIN_RIGHT, MarginProperties.MARGIN_BOTTOM, null, path);
    }

    public T bottomLeftMarginAlongPath(Path path) {
        return animatePropertiesAlongPath(MarginProperties.MARGIN_LEFT, MarginProperties.MARGIN_BOTTOM, null, path);
    }

    public T width(int width) {
        return animate(SizeProperties.WIDTH, width);
    }

    public T widthBy(int widthBy) {
        return animatePropertyBy(SizeProperties.WIDTH, widthBy);
    }

    public T height(int height) {
        return animate(SizeProperties.HEIGHT, height);
    }

    public T heightBy(int heightBy) {
        return animatePropertyBy(SizeProperties.HEIGHT, heightBy);
    }

    public T size(int size) {
        animate(SizeProperties.WIDTH, size);
        animate(SizeProperties.HEIGHT, size);
        return self();
    }

    public T sizeBy(int sizeBy) {
        animatePropertyBy(SizeProperties.WIDTH, sizeBy);
        animatePropertyBy(SizeProperties.HEIGHT, sizeBy);
        return self();
    }

    public T leftPadding(int leftPadding) {
        return animate(PaddingProperties.PADDING_LEFT, leftPadding);
    }

    public T leftPaddingBy(int leftPaddingBy) {
        return animatePropertyBy(PaddingProperties.PADDING_LEFT, leftPaddingBy);
    }

    public T topPadding(int topPadding) {
        return animate(PaddingProperties.PADDING_TOP, topPadding);
    }

    public T topPaddingBy(int topPaddingBy) {
        return animatePropertyBy(PaddingProperties.PADDING_TOP, topPaddingBy);
    }

    public T rightPadding(int rightPadding) {
        return animate(PaddingProperties.PADDING_RIGHT, rightPadding);
    }

    public T rightPaddingBy(int rightPaddingBy) {
        return animatePropertyBy(PaddingProperties.PADDING_RIGHT, rightPaddingBy);
    }

    public T bottomPadding(int bottomPadding) {
        return animate(PaddingProperties.PADDING_BOTTOM, bottomPadding);
    }

    public T bottomPaddingBy(int bottomPaddingBy) {
        return animatePropertyBy(PaddingProperties.PADDING_BOTTOM, bottomPaddingBy);
    }

    public T horizontalPadding(int horizontalPadding) {
        animate(PaddingProperties.PADDING_LEFT, horizontalPadding);
        animate(PaddingProperties.PADDING_RIGHT, horizontalPadding);
        return self();
    }

    public T horizontalPaddingBy(int horizontalPaddingBy) {
        animatePropertyBy(PaddingProperties.PADDING_LEFT, horizontalPaddingBy);
        animatePropertyBy(PaddingProperties.PADDING_RIGHT, horizontalPaddingBy);
        return self();
    }

    public T verticalPadding(int verticalPadding) {
        animate(PaddingProperties.PADDING_TOP, verticalPadding);
        animate(PaddingProperties.PADDING_BOTTOM, verticalPadding);
        return self();
    }

    public T verticalPaddingBy(int verticalPaddingBy) {
        animatePropertyBy(PaddingProperties.PADDING_TOP, verticalPaddingBy);
        animatePropertyBy(PaddingProperties.PADDING_BOTTOM, verticalPaddingBy);
        return self();
    }

    public T padding(int padding) {
        animate(PaddingProperties.PADDING_LEFT, padding);
        animate(PaddingProperties.PADDING_RIGHT, padding);
        animate(PaddingProperties.PADDING_BOTTOM, padding);
        animate(PaddingProperties.PADDING_TOP, padding);
        return self();
    }

    public T paddingBy(int paddingBy) {
        animatePropertyBy(PaddingProperties.PADDING_LEFT, paddingBy);
        animatePropertyBy(PaddingProperties.PADDING_RIGHT, paddingBy);
        animatePropertyBy(PaddingProperties.PADDING_BOTTOM, paddingBy);
        animatePropertyBy(PaddingProperties.PADDING_TOP, paddingBy);
        return self();
    }

    public T scrollX(int scrollX) {
        return animate(ScrollProperties.SCROLL_X, scrollX);
    }

    public T scrollXBy(int scrollXBy) {
        return animatePropertyBy(ScrollProperties.SCROLL_X, scrollXBy);
    }

    public T scrollY(int scrollY) {
        return animate(ScrollProperties.SCROLL_Y, scrollY);
    }

    public T scrollYBy(int scrollYBy) {
        return animatePropertyBy(ScrollProperties.SCROLL_Y, scrollYBy);
    }

    public T scroll(int x, int y) {
        animate(ScrollProperties.SCROLL_X, x);
        animate(ScrollProperties.SCROLL_Y, y);
        return self();
    }

    public T scrollBy(int xBy, int yBy) {
        animatePropertyBy(ScrollProperties.SCROLL_X, xBy);
        animatePropertyBy(ScrollProperties.SCROLL_Y, yBy);
        return self();
    }

    @SuppressLint("NewApi")
    public T elevation(int elevation) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            animate(ElevationProperties.ELEVATION, elevation);
        }
        return self();
    }

    @SuppressLint("NewApi")
    public T elevationBy(int elevationBy) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            animatePropertyBy(ElevationProperties.ELEVATION, elevationBy);
        }
        return self();
    }


//    public T widthPercent(float widthPercent) {
//        if (initPercentListener()){
//            mPercentListener.widthPercent(widthPercent);
//        }
//        return self();
//    }
//
//    public T widthPercentBy(float widthPercentBy) {
//        if (initPercentListener()){
//            mPercentListener.widthPercentBy(widthPercentBy);
//        }
//        return self();
//    }
//
//    public T heightPercent(float heightPercent) {
//        if (initPercentListener()){
//            mPercentListener.heightPercent(heightPercent);
//        }
//        return self();
//    }
//
//    public T heightPercentBy(float heightPercentBy) {
//        if (initPercentListener()){
//            mPercentListener.heightPercentBy(heightPercentBy);
//        }
//        return self();
//    }
//
//    public T sizePercent(float sizePercent) {
//        if (initPercentListener()){
//            mPercentListener.sizePercent(sizePercent);
//        }
//        return self();
//    }
//
//    public T sizePercentBy(float sizePercentBy) {
//        if (initPercentListener()){
//            mPercentListener.sizePercentBy(sizePercentBy);
//        }
//        return self();
//    }
//
//    public T leftMarginPercent(float marginPercent) {
//        if (initPercentListener()){
//            mPercentListener.leftMarginPercent(marginPercent);
//        }
//        return self();
//    }
//
//    public T leftMarginPercentBy(float marginPercentBy) {
//        if (initPercentListener()){
//            mPercentListener.leftMarginPercentBy(marginPercentBy);
//        }
//        return self();
//    }
//
//    public T topMarginPercent(float marginPercent) {
//        if (initPercentListener()){
//            mPercentListener.topMarginPercent(marginPercent);
//        }
//        return self();
//    }
//
//    public T topMarginPercentBy(float marginPercentBy) {
//        if (initPercentListener()){
//            mPercentListener.topMarginPercentBy(marginPercentBy);
//        }
//        return self();
//    }
//
//    public T bottomMarginPercent(float marginPercent) {
//        if (initPercentListener()){
//            mPercentListener.bottomMarginPercent(marginPercent);
//        }
//        return self();
//    }
//
//    public T bottomMarginPercentBy(float marginPercentBy) {
//        if (initPercentListener()){
//            mPercentListener.bottomMarginPercentBy(marginPercentBy);
//        }
//        return self();
//    }
//
//    public T rightMarginPercent(float marginPercent) {
//        if (initPercentListener()){
//            mPercentListener.rightMarginPercent(marginPercent);
//        }
//        return self();
//    }
//
//    public T rightMarginPercentBy(float marginPercentBy) {
//        if (initPercentListener()){
//            mPercentListener.rightMarginPercentBy(marginPercentBy);
//        }
//        return self();
//    }
//
//    public T horizontalMarginPercent(float marginPercent) {
//        if (initPercentListener()){
//            mPercentListener.horizontalMarginPercent(marginPercent);
//        }
//        return self();
//    }
//
//    public T horizontalMarginPercentBy(float marginPercentBy) {
//        if (initPercentListener()){
//            mPercentListener.horizontalMarginPercentBy(marginPercentBy);
//        }
//        return self();
//    }
//
//    public T verticalMarginPercent(float marginPercent) {
//        if (initPercentListener()){
//            mPercentListener.verticalMarginPercent(marginPercent);
//        }
//        return self();
//    }
//
//    public T verticalMarginPercentBy(float marginPercentBy) {
//        if (initPercentListener()){
//            mPercentListener.verticalMarginPercentBy(marginPercentBy);
//        }
//        return self();
//    }
//
//    public T marginPercent(float marginPercent) {
//        if (initPercentListener()){
//            mPercentListener.marginPercent(marginPercent);
//        }
//        return self();
//    }
//
//    public T marginPercentBy(float marginPercentBy) {
//        if (initPercentListener()){
//            mPercentListener.marginPercentBy(marginPercentBy);
//        }
//        return self();
//    }
//
//    public T aspectRatio(float aspectRatio) {
//        if (initPercentListener()){
//            mPercentListener.aspectRatio(aspectRatio);
//        }
//        return self();
//    }
//
//    public T aspectRatioBy(float aspectRatioBy) {
//        if (initPercentListener()){
//            mPercentListener.aspectRatioBy(aspectRatioBy);
//        }
//        return self();
//    }
}
