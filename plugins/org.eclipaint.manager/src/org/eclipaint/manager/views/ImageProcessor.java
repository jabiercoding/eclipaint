package org.eclipaint.manager.views;

import org.eclipaint.manager.activator.Activator;
import org.eclipaint.utils.ImageUtils;
import org.eclipse.draw2d.ImageUtilities;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;

/**
 * Image processor
 * 
 * @author Jabier Martinez
 */
public class ImageProcessor {

	public static Image process(String id, String overlayIcon, String baseIcon) {

		Image baseImage = new Image(Display.getCurrent(), baseIcon);
		Image overlayImage = new Image(Display.getCurrent(), overlayIcon);

		int baseWidth = baseImage.getBounds().width;
		int baseHeight = baseImage.getBounds().height;
		int overlayWidth = overlayImage.getBounds().width;
		int overlayHeight = overlayImage.getBounds().height;

		if (id.equals(Icon.BASE_ICON)) {
			return baseImage;
		}

		if (id.equals(Icon.CENTERED_OVERLAY_ICON)) {
			int x = (baseWidth / 2) - (overlayWidth / 2);
			int y = (baseHeight / 2) - (overlayHeight / 2);
			return ImageUtils.createOverlapedImage(overlayImage, baseImage, new Point(x, y));
		}

		if (id.equals(Icon.TOP_LEFT_CORNER_OVERLAY_ICON)) {
			int x = 0;
			int y = 0;
			return ImageUtils.createOverlapedImage(overlayImage, baseImage, new Point(x, y));
		}
		if (id.equals(Icon.TOP_RIGHT_CORNER_OVERLAY_ICON)) {
			int x = baseWidth - overlayWidth;
			int y = 0;
			return ImageUtils.createOverlapedImage(overlayImage, baseImage, new Point(x, y));
		}
		if (id.equals(Icon.BOTTOM_RIGHT_CORNER_OVERLAY_ICON)) {
			int x = baseWidth - overlayWidth;
			int y = baseHeight - overlayHeight;
			return ImageUtils.createOverlapedImage(overlayImage, baseImage, new Point(x, y));
		}
		if (id.equals(Icon.BOTTOM_LEFT_CORNER_OVERLAY_ICON)) {
			int x = 0;
			int y = baseHeight - overlayHeight;
			return ImageUtils.createOverlapedImage(overlayImage, baseImage, new Point(x, y));
		}

		if (id.equals(Icon.TOP_SIDE_OVERLAY_ICON)) {
			int x = (baseWidth / 2) - (overlayWidth / 2);
			int y = 0;
			return ImageUtils.createOverlapedImage(overlayImage, baseImage, new Point(x, y));
		}
		if (id.equals(Icon.RIGHT_SIDE_OVERLAY_ICON)) {
			int x = baseWidth - overlayWidth;
			int y = (baseHeight / 2) - (overlayHeight / 2);
			return ImageUtils.createOverlapedImage(overlayImage, baseImage, new Point(x, y));
		}
		if (id.equals(Icon.BOTTOM_SIDE_OVERLAY_ICON)) {
			int x = (baseWidth / 2) - (overlayWidth / 2);
			int y = baseHeight - overlayHeight;
			return ImageUtils.createOverlapedImage(overlayImage, baseImage, new Point(x, y));
		}
		if (id.equals(Icon.LEFT_SIDE_OVERLAY_ICON)) {
			int x = 0;
			int y = (baseHeight / 2) - (overlayHeight / 2);
			return ImageUtils.createOverlapedImage(overlayImage, baseImage, new Point(x, y));
		}

		if (id.equals(Icon.ROTATE_LEFT_BASE_ICON)) {
			return new Image(baseImage.getDevice(), ImageUtils.rotateOrFlip(baseImage, SWT.LEFT).getImageData());
		}
		if (id.equals(Icon.ROTATE_RIGHT_BASE_ICON)) {
			return new Image(baseImage.getDevice(), ImageUtils.rotateOrFlip(baseImage, SWT.RIGHT).getImageData());
		}
		if (id.equals(Icon.ROTATE_180_BASE_ICON)) {
			return new Image(baseImage.getDevice(), ImageUtils.rotateOrFlip(baseImage, SWT.DOWN).getImageData());
		}
		if (id.equals(Icon.FLIP_HORIZONTAL_BASE_ICON)) {
			return new Image(baseImage.getDevice(), ImageUtils.rotateOrFlip(baseImage, SWT.HORIZONTAL).getImageData());
		}
		if (id.equals(Icon.FLIP_VERTICAL_BASE_ICON)) {
			return new Image(baseImage.getDevice(), ImageUtils.rotateOrFlip(baseImage, SWT.VERTICAL).getImageData());
		}
		if (id.equals(Icon.COLOR_DISABLED)) {
			return new Image(baseImage.getDevice(), baseImage, SWT.IMAGE_DISABLE);
		}
		if (id.equals(Icon.COLOR_GRAY)) {
			return new Image(baseImage.getDevice(), baseImage, SWT.IMAGE_GRAY);
		}
		if (id.contains(Icon.COLOR_RGB)) {
			// input example COLOR_RGB255,0,0
			String[] rgb = id.substring(Icon.COLOR_RGB.length()).split(",");
			return new Image(baseImage.getDevice(), ImageUtilities.createShadedImage(
					baseImage,
					new Color(baseImage.getDevice(), Integer.parseInt(rgb[0]), Integer.parseInt(rgb[1]), Integer
							.parseInt(rgb[2]))));
		}
		if (id.equals(Icon.SCALE_16X16)) {
			return new Image(baseImage.getDevice(), ImageUtils.scaleImage(baseImage.getImageData(), 16, 16, true));
		}
		if (id.equals(Icon.SCALE_200)) {
			return new Image(baseImage.getDevice(), ImageUtils.scaleImage(baseImage.getImageData(), 2));
		}
		if (id.equals(Icon.SCALE_75)) {
			return new Image(baseImage.getDevice(), ImageUtils.scaleImage(baseImage.getImageData(), 0.75));
		}
		if (id.equals(Icon.SCALE_60)) {
			return new Image(baseImage.getDevice(), ImageUtils.scaleImage(baseImage.getImageData(), 0.6));
		}
		if (id.equals(Icon.SCALE_50)) {
			return new Image(baseImage.getDevice(), ImageUtils.scaleImage(baseImage.getImageData(), 0.5));
		}
		if (id.equals(Icon.SCALE_40)) {
			return new Image(baseImage.getDevice(), ImageUtils.scaleImage(baseImage.getImageData(), 0.4));
		}

		else {
			return Activator.getImageDescriptor("icons/manager/default/base.png").createImage();
		}
	}

}
