package org.eclipaint.editor;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

/**
 * Pixel item class to be shown in the canvas and also used to save the modified
 * image
 * 
 * @author Jabier Martinez
 */
public class PixelItem implements Cloneable {

	// Color
	public RGB color;

	// 0 transparent, 255 opaque
	public int alpha;

	// The pixel rectangle in the canvas
	public Rectangle pixelRectangle;

	// Position relative to the canvas. Ex: (0,0)
	public Point realPosition;

	/**
	 * How to render it in the canvas
	 * 
	 * @param gc
	 */
	public void paint(GC gc) {
		gc.setAlpha(alpha);
		Color backgroundColor = new Color(Display.getCurrent(), color);
		gc.setBackground(backgroundColor);
		gc.fillRectangle(pixelRectangle.x, pixelRectangle.y, pixelRectangle.width, pixelRectangle.height);
		backgroundColor.dispose();
	}

	/**
	 * Clone to get same data but different object
	 */
	public Object clone() {
		PixelItem clonedPixelItem = null;
		try {
			clonedPixelItem = (PixelItem) super.clone();
			// We need to take care of creating new Rectangle and Point,
			// otherwise it will be the same objects
			if (pixelRectangle != null) {
				clonedPixelItem.pixelRectangle = new Rectangle(this.pixelRectangle.x, this.pixelRectangle.y,
						this.pixelRectangle.width, this.pixelRectangle.height);
			}
			if (realPosition != null) {
				clonedPixelItem.realPosition = new Point(realPosition.x, realPosition.y);
			}
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		return clonedPixelItem;
	}

	/**
	 * Print pixel information
	 */
	@Override
	public String toString() {
		String _toString = "";
		if (realPosition != null) {
			_toString = "[" + realPosition.x + "," + realPosition.y + "]";
		}
		if (color != null) {
			_toString = _toString + " color=" + color + " alpha=" + alpha;
		}
		return _toString;
	}
}
