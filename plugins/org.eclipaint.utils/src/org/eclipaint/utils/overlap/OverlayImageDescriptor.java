package org.eclipaint.utils.overlap;

import org.eclipse.jface.resource.CompositeImageDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;

public class OverlayImageDescriptor extends CompositeImageDescriptor {
	private ImageDescriptor imageDescriptor;
	private ImageDescriptor overlayImage;
	private Point overlayPoint;
	Point size;
	Point overlaySize;

	public OverlayImageDescriptor(ImageDescriptor imgDescriptor, ImageDescriptor overlayImage, Point overlayPoint) {
		setImageDescriptor(imgDescriptor);
		setOverlayImage(overlayImage);
		setOverlayPoint(overlayPoint);
	}

	@Override
	protected void drawCompositeImage(int arg0, int arg1) {
		drawImage(getImageDescriptor().getImageData(), 0, 0);
		ImageData overlayImageData = getOverlayImage().getImageData();
		drawImage(overlayImageData, overlayPoint.x, overlayPoint.y);
	}

	protected Point getSize() {
		return size;
	}

	public void setImageDescriptor(ImageDescriptor imageDescriptor) {
		this.imageDescriptor = imageDescriptor;
		Rectangle bounds = imageDescriptor.createImage().getBounds();
		size = new Point(bounds.width, bounds.height);
	}

	public ImageDescriptor getImageDescriptor() {
		return imageDescriptor;
	}

	public void setOverlayImage(ImageDescriptor overlayImage) {
		this.overlayImage = overlayImage;
		Rectangle bounds = overlayImage.createImage().getBounds();
		overlaySize = new Point(bounds.width, bounds.height);
	}

	public ImageDescriptor getOverlayImage() {
		return overlayImage;
	}

	public void setOverlayPoint(Point overlayPoint) {
		this.overlayPoint = overlayPoint;
	}

	public Point getOverlayPoint() {
		return overlayPoint;
	}

}
