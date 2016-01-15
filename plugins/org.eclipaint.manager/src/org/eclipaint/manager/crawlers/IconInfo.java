/*
 * Copyright (c) 2011
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.eclipaint.manager.crawlers;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

/**
 * 
 * 
 * @author dcarew
 */
public class IconInfo implements Comparable<IconInfo> {

	private File file;
	private String path;

	private Dimension size;

	public IconInfo(File destDir, File file) {
		this.file = file;

		if (destDir != null) {
			path = destDir.toURI().relativize(file.toURI()).getPath();
		}
	}

	public String getName() {
		return file.getName();
	}

	public String getPath() {
		return path;
	}

	public int compareTo(IconInfo other) {
		return file.getName().compareToIgnoreCase(other.file.getName());
	}

	public boolean isOverlayIcon() {
		Dimension dim = getSize();

		return dim.width < 16 && dim.height < 16;
	}

	public boolean is16x16Icon() {
		if (isOverlayIcon()) {
			return false;
		}

		Dimension dim = getSize();

		return dim.width <= 16 && dim.height <= 16;
	}

	public boolean isWizardIcon() {
		Dimension dim = getSize();

		return dim.width == 75 && dim.height == 66;
	}

	public boolean isStandardIcon() {
		if (!isOverlayIcon() && !is16x16Icon() && !isWizardIcon()) {
			return false;
		} else {
			return true;
		}
	}

	private Dimension getSize() {
		if (size == null) {
			try {
				BufferedImage image = ImageIO.read(file);

				size = new Dimension(image.getWidth(), image.getHeight());
			} catch (IOException e) {
				size = new Dimension(0, 0);
			} catch (Exception e) {
				// ImageIO.read throws IndexOutOfBoundException with
				// corrupted images
				size = new Dimension(0, 0);
			}
		}

		return size;
	}

	public int getWidth() {
		return getSize().width;
	}

	public int getHeight() {
		return getSize().height;
	}

}
