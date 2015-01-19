package de.jdevel.acaldav.widget;

/**
 * @author Joseph Weigl
 */
public interface DrawableClickListener {

    public void onClick(DrawablePosition target);

    public static enum DrawablePosition {TOP, BOTTOM, LEFT, RIGHT}
}
