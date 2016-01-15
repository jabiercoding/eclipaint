package org.eclipaint.manager.label.decorator;

import org.eclipaint.utils.ImageUtils;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;

/**
 * Icons Label Decorator
 * 
 * @author Jabier Martinez
 */
public class IconsLabelDecorator implements ILabelDecorator {

	@Override
	public void addListener(ILabelProviderListener listener) {
	}

	@Override
	public void dispose() {
	}

	@Override
	public boolean isLabelProperty(Object element, String property) {
		return false;
	}

	@Override
	public void removeListener(ILabelProviderListener listener) {
	}

	@Override
	public Image decorateImage(Image image, Object element) {
		if (isGoingToBeDecorated(element)) {
			Image resourceImage = ImageUtils.getImageFromResource((IResource) element);
			if (resourceImage == null) {
				return null;
			}
			if (resourceImage.getBounds().width > 20 || resourceImage.getBounds().height > 20) {
				return ImageDescriptor.createFromImageData(
						ImageUtils.scaleImage(resourceImage.getImageData(), 16, 16, false)).createImage();
			} else {
				return resourceImage;
			}
		}
		return null;
	}

	@Override
	public String decorateText(String text, Object element) {
		// null means no decoration
		return null;
	}

	public boolean isGoingToBeDecorated(Object element) {
		IResource file = (IResource) element;
		// check if it is an image
		if (ImageUtils.isImageFile(file)) {
			return true;
		}
		return false;
	}

}
