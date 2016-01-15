package org.eclipaint.manager.views;

import org.eclipse.swt.graphics.Image;

/**
 * @author Jabier Martinez
 */
public class Icon {

	public static final String BASE_ICON = "Base icon";
	public static final String CENTERED_OVERLAY_ICON = "Centered overlay icon";

	public static final String TOP_LEFT_CORNER_OVERLAY_ICON = "Top left corner overlay icon";
	public static final String TOP_RIGHT_CORNER_OVERLAY_ICON = "Top right corner overlay icon";
	public static final String BOTTOM_LEFT_CORNER_OVERLAY_ICON = "Bottom left corner overlay icon";
	public static final String BOTTOM_RIGHT_CORNER_OVERLAY_ICON = "Bottom right corner overlay icon";

	public static final String TOP_SIDE_OVERLAY_ICON = "Top side overlay icon";
	public static final String RIGHT_SIDE_OVERLAY_ICON = "Right side overlay icon";
	public static final String LEFT_SIDE_OVERLAY_ICON = "Left side overlay icon";
	public static final String BOTTOM_SIDE_OVERLAY_ICON = "Bottom side overlay icon";

	public static final String FLIP_HORIZONTAL_BASE_ICON = "Flip horizontal base icon";
	public static final String FLIP_VERTICAL_BASE_ICON = "Flip vertical base icon";
	public static final String ROTATE_RIGHT_BASE_ICON = "Rotate right base icon";
	public static final String ROTATE_LEFT_BASE_ICON = "Rotate left base icon";
	public static final String ROTATE_180_BASE_ICON = "Rotate 180 base icon";

	public static final String COLOR_DISABLED = "Color disabled";
	public static final String COLOR_GRAY = "Color gray";
	public static final String COLOR_RGB = "Color RGB";

	public static final String SCALE_16X16 = "Scale 16x16 base icon";
	public static final String SCALE_200 = "Scale 200 base icon";
	public static final String SCALE_75 = "Scale 75 base icon";
	public static final String SCALE_60 = "Scale 60 base icon";
	public static final String SCALE_50 = "Scale 50 base icon";
	public static final String SCALE_40 = "Scale 40 base icon";

	private String name;
	private String id;
	private IconCategory parent;

	public Icon(String id, String name) {
		this.name = name;
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getId() {
		return id;
	}

	public void setParent(IconCategory parent) {
		this.parent = parent;
	}

	public IconCategory getParent() {
		return parent;
	}

	public String toString() {
		return getName();
	}

	public Image processImage(String overlayIcon, String baseIcon) {
		return ImageProcessor.process(getId(), overlayIcon, baseIcon);
	}
}
