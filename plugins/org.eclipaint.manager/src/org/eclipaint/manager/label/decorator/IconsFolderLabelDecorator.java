package org.eclipaint.manager.label.decorator;

import org.eclipaint.utils.ImageUtils;
import org.eclipaint.utils.WorkbenchUtils;
import org.eclipse.core.resources.IResource;

/**
 * Icons Folder Label Decorator
 * 
 * @author Jabier Martinez
 */
public class IconsFolderLabelDecorator extends IconsLabelDecorator {

	@Override
	public boolean isGoingToBeDecorated(Object element) {
		IResource file = (IResource) element;
		// check if it is an image
		if (ImageUtils.isImageFile(file)) {
			// check if it is in an icons folder
			if (WorkbenchUtils.isTheResourceContainedInAConcreteFolder(file, "icons")) {
				return true;
			}
		}
		return false;
	}

}
