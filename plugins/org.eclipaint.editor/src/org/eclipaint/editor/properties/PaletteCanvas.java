package org.eclipaint.editor.properties;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

/**
 * A Canvas showing imageData palette
 * 
 * @author Jabier Martinez
 */
public class PaletteCanvas extends Canvas {

	ImageData imageData;
	public static int SIZE = 300;

	PaletteCanvas(Composite composite, ImageData imageData) {
		super(composite, SWT.NONE);
		this.imageData = imageData;
		this.addPaintListener(paintListener);
	}

	PaintListener paintListener = new PaintListener() {
		public void paintControl(PaintEvent e) {
			GC gc = e.gc;
			int x = 0;
			int y = 0;
			int size = 10;
			int transparentIndex = imageData.transparentPixel;
			gc.setAlpha(255);
			for (int i = 0; i < imageData.palette.getRGBs().length; i++) {
				// normal pixel
				if (i != transparentIndex) {
					RGB rgb = imageData.palette.getRGB(i);
					gc.setBackground(new Color(Display.getCurrent(), rgb));
					gc.fillRectangle(x, y, size, size);
				} else {
					// transparent pixel
					gc.setBackground(Display.getDefault().getSystemColor(
							SWT.COLOR_WHITE));
					int halfPixel = size / 2;
					gc.fillRectangle(x, y, halfPixel, halfPixel);
					gc.fillRectangle(x + halfPixel, y + halfPixel, halfPixel,
							halfPixel);
					gc.setForeground(Display.getDefault().getSystemColor(
							SWT.COLOR_BLUE));
					gc.drawRectangle(x, y, size - 1, size - 1);
				}
				// calculate new x and y
				if (x <= SIZE) {
					x = x + size;
				} else {
					x = 0;
					y = y + size;
				}
			}
		}
	};

}
