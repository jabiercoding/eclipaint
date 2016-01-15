package org.eclipaint.manager.views;

import java.util.ArrayList;

import org.eclipaint.manager.activator.Activator;
import org.eclipse.swt.graphics.Image;

/**
 * Icon Category
 * 
 * @author Jabier Martinez
 */
public class IconCategory extends Icon {

	static final String BASE = "Base";
	static final String SIDES_OVERLAY = "Sides overlay";
	static final String CORNERS_OVERLAY = "Corners overlay";
	static final String CENTERED_OVERLAY = "Centered overlay";
	static final String FLIP_ROTATE = "Flip rotate";
	static final String COLOR = "Color";
	static final String SCALE = "Scale";

	private ArrayList<Icon> children;

	public IconCategory(String id, String name) {
		super(id, name);
		children = new ArrayList<Icon>();
	}

	public void addIcon(Icon child) {
		children.add(child);
		child.setParent(this);
	}

	public void removeIcon(Icon child) {
		children.remove(child);
		child.setParent(null);
	}

	public Icon[] getIcons() {
		return (Icon[]) children.toArray(new Icon[children.size()]);
	}

	public boolean hasIcons() {
		return children.size() > 0;
	}

	public Image getImage() {
		// naming convention for category icons
		String pathToCategoryImage = "icons/manager/categories/" + getId().replaceAll(" ", "_") + ".png";
		return Activator.getImageDescriptor(pathToCategoryImage).createImage();
	}

}
