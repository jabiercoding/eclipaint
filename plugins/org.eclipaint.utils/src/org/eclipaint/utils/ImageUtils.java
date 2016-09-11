package org.eclipaint.utils;

import java.awt.Dimension;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;

import org.eclipaint.utils.overlap.OverlayImageDescriptor;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

public class ImageUtils {

	public static final String[] IMAGE_EXTENSIONS = { "*.gif", "*.png", "*.bmp", "*.jpg", "*.ico" };

	public static void saveImageToFile(ImageData imageData, String imagePath, int format) {
		ImageLoader loader = new ImageLoader();
		loader.data = new ImageData[] { imageData };
		loader.save(imagePath, format);
	}

	// TODO for the moment only png file is allowed for saving the image
	public static String getExtension(int outputFormat) {
		if (outputFormat == SWT.IMAGE_PNG) {
			return "png";
		}
		return null;
	}

	// TODO Improve this method, make it more precise.
	public static int getImageFormat(ImageData imageData, String extension) {
		int imageFormat = SWT.IMAGE_PNG;
		if (extension.equalsIgnoreCase("png")) {
			imageFormat = SWT.IMAGE_PNG;
		} else if (extension.equalsIgnoreCase("bmp")) {
			imageFormat = SWT.IMAGE_BMP;
		} else if (extension.equalsIgnoreCase("gif")) {
			imageFormat = SWT.IMAGE_GIF;
		} else if (extension.equalsIgnoreCase("jpg") || extension.equalsIgnoreCase("jpeg")) {
			imageFormat = SWT.IMAGE_JPEG;
		} else if (extension.equalsIgnoreCase("tiff")) {
			imageFormat = SWT.IMAGE_TIFF;
		} else if (extension.equalsIgnoreCase("ico")) {
			imageFormat = SWT.IMAGE_ICO;
		}
		// Corrections
		// Gif maximum depth is 8. Otherwise probably it is a renamed png
		if (imageFormat == SWT.IMAGE_GIF && imageData.depth > 8) {
			imageFormat = SWT.IMAGE_PNG;
		}
		return imageFormat;
	}

	public static Image cropImage(Image sourceImage, int x, int y, int height, int width) {
		Image croppedImage = new Image(Display.getCurrent(), width, height);
		GC gc = new GC(sourceImage);
		gc.copyArea(croppedImage, x, y);
		gc.dispose();
		return croppedImage;
	}

	/**
	 * 
	 * @param image
	 * @param direction
	 *            Rotate: SWT.LEFT, SWT.RIGHT, SWT.DOWN for 180�, Flip:
	 *            SWT.HORIZONTAL, SWT.VERTICAL
	 * @return
	 */
	public static Image rotateOrFlip(Image image, int direction) {
		try {
			// Get the current display
			Display display = Display.getCurrent();
			if (display == null)
				SWT.error(SWT.ERROR_THREAD_INVALID_ACCESS);

			// Use the image's data to create a rotated image's data
			ImageData sd = image.getImageData();

			// Manage alpha layer
			boolean containsAlpha = sd.alphaData != null;
			byte[] newAlphaData = null;
			if (containsAlpha) {
				newAlphaData = new byte[sd.alphaData.length];
			}

			// Create new ImageData taking into account new size
			ImageData dd;
			if (direction == SWT.LEFT || direction == SWT.RIGHT) {
				dd = new ImageData(sd.height, sd.width, sd.depth, sd.palette);
			} else { // direction == SWT.HORIZONTAL || direction == SWT.VERTICAL
						// || direction == SWT.DOWN
				dd = new ImageData(sd.width, sd.height, sd.depth, sd.palette);
			}

			if (containsAlpha) {
				dd.alphaData = newAlphaData;
			}
			dd.alpha = sd.alpha;
			dd.transparentPixel = sd.transparentPixel;

			// Run through the horizontal pixels
			for (int sx = 0; sx < sd.width; sx++) {
				// Run through the vertical pixels
				for (int sy = 0; sy < sd.height; sy++) {
					int dx = 0, dy = 0;
					switch (direction) {
					case SWT.LEFT: // left 90 degrees
						dx = sy;
						dy = sd.width - sx - 1;
						break;
					case SWT.RIGHT: // right 90 degrees
						dx = sd.height - sy - 1;
						dy = sx;
						break;
					case SWT.DOWN: // 180 degrees
						dx = sd.width - sx - 1;
						dy = sd.height - sy - 1;
						break;
					case SWT.HORIZONTAL: // flip horizontal
						dx = sd.width - sx - 1;
						dy = sy;
						break;
					case SWT.VERTICAL: // flip vertical
						dx = sx;
						dy = sd.height - sy - 1;
						break;
					}

					// Swap the x, y source data to y, x in the destination
					dd.setPixel(dx, dy, sd.getPixel(sx, sy));
					// Swap also the alpha layer
					if (containsAlpha) {
						dd.setAlpha(dx, dy, sd.getAlpha(sx, sy));
					}
				}
			}

			// Create the vertical image
			return new Image(display, dd);
		} catch (Exception e) {
			// Exception. Return original image
			e.printStackTrace();
			return image;
		}
	}

	/**
	 * Scale image
	 * 
	 * @param image
	 * @param scaleRatio
	 *            0,5 half of the size
	 * @return scaled image
	 */
	public static ImageData scaleImage(ImageData imageData, double scaleRatio) {
		return scaleImage(imageData, (int) (imageData.width * scaleRatio), (int) (imageData.height * scaleRatio), false);
	}

	/**
	 * Scale Image
	 * 
	 * @param imageData
	 * @param width
	 * @param height
	 * @param aspectRatio
	 * @return scaledImageData
	 */
	public static ImageData scaleImage(ImageData imageData, int width, int height, boolean aspectRatio) {
		if (width == 0 || height == 0) {
			// error, we return the original
			return imageData;
		}
		if (!aspectRatio) {
			return imageData.scaledTo(width, height);
		}
		int originalWidth = imageData.width;
		int originalHeight = imageData.height;
		if (originalWidth >= originalHeight) {
			return imageData.scaledTo(width, (originalHeight * height) / originalWidth);
		}
		return imageData.scaledTo((originalWidth * width) / originalHeight, height);
	}

	public static Dimension getImageDim(final String path) {
		Dimension result = null;
		String suffix = WorkbenchUtils.getFileSuffix(path);
		Iterator<ImageReader> iter = ImageIO.getImageReadersBySuffix(suffix);
		if (iter.hasNext()) {
			ImageReader reader = iter.next();
			try {
				ImageInputStream stream = new FileImageInputStream(new File(path));
				reader.setInput(stream);
				int width = reader.getWidth(reader.getMinIndex());
				int height = reader.getHeight(reader.getMinIndex());
				result = new Dimension(width, height);
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				reader.dispose();
			}
		} else {
			// No reader found for given format
			return null;
		}
		return result;
	}

	public static ImageData createEmptyDirectPaletteImageData(int width, int height, int depth) {
		// direct palette with standard rgb masks
		PaletteData paletteData = new PaletteData(0xff, 0xff00, 0xff0000);
		ImageData newImageData = new ImageData(width, height, depth, paletteData);
		// transparency, otherwise it starts in black
		byte[] alphaData = new byte[width * height];
		for (int i = 0; i < alphaData.length; i++) {
			// 0 means transparent
			alphaData[i] = 0;
		}
		newImageData.alphaData = alphaData;
		return newImageData;
	}

	public static int getRGBPositionInPalette(ImageData imageData, RGB rgb) {
		if (imageData.palette != null && imageData.palette.getRGBs() != null) {
			for (int i = 0; i < imageData.palette.getRGBs().length; i++) {
				if (imageData.palette.getRGBs()[i].equals(rgb)) {
					// Same color found
					if (imageData.getTransparencyType() == SWT.TRANSPARENCY_PIXEL) {
						// And it is not the transparency pixel position in the
						// case of transparency pixel image
						if (imageData.transparentPixel != i) {
							return i;
						}
					} else {
						return i;
					}
				}
			}
		}
		return -1;
	}

	public static Image getImageFromResource(IResource resource) {
		return getImage(WorkbenchUtils.getIResourceAbsPath(resource));
	}

	public static Image getImage(String absolutePath) {
		FileInputStream fileInputStream = null;
	    try {
			if (absolutePath == null) {
				return null;
			}
			fileInputStream = new FileInputStream(absolutePath);
			return new Image(Display.getDefault(), fileInputStream);
		} catch (Exception e) {
			// If any exception happens return null
			return null;
		} finally {
            if (fileInputStream != null){
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    //do nothing
                }
            }
        }
	}

	public static boolean isImageFile(IResource resource) {
		return isImageFile(resource.getName());
	}

	public static boolean isImageFile(File file) {
		return isImageFile(file.getName());
	}

	public static boolean isTransparentImageFile(IResource iResource) {
		String extension = iResource.getFileExtension();
		if (extension != null && extension.equalsIgnoreCase("png") || extension.equalsIgnoreCase("gif")) {
			return true;
		}
		return false;
	}

	public static boolean folderContainsImages(IFolder folder) {
		try {
			for (IResource resource : folder.members()) {
				if (resource instanceof IFolder) {
					if (folderContainsImages((IFolder) resource)) {
						return true;
					}
				} else {
					if (isImageFile(resource)) {
						return true;
					}
				}
			}
		} catch (CoreException e) {
			return false;
		}
		return false;
	}

	public static boolean isImageFile(String fileName) {
		int dot = fileName.lastIndexOf(".");
		if (dot == -1) {
			return false;
		} else {
			String fileExt = fileName.substring(dot, fileName.length());
			for (String extension : IMAGE_EXTENSIONS) {
				// remove *
				extension = extension.substring(1);
				if (fileExt.equalsIgnoreCase(extension)) {
					return true;
				}
			}
		}
		return false;
	}

	public static Image createOverlapedImage(String overlayImagePath, String baseImagePath, Point overlayPoint) {
		Image baseImage = new Image(Display.getCurrent(), baseImagePath);
		Image overlayImage = new Image(Display.getCurrent(), overlayImagePath);
		return createOverlapedImage(overlayImage, baseImage, overlayPoint);
	}

	public static Image createOverlapedImage(Image overlayImage, Image baseImage, Point overlayPoint) {
		OverlayImageDescriptor oid = new OverlayImageDescriptor(ImageDescriptor.createFromImage(baseImage),
				ImageDescriptor.createFromImage(overlayImage), overlayPoint);
		return oid.createImage();
	}

	public static Image createOverlapedImage(String overlayImagePath, Image baseImage, Point overlayPoint) {
		Image overlayImage = new Image(Display.getCurrent(), overlayImagePath);
		OverlayImageDescriptor oid = new OverlayImageDescriptor(ImageDescriptor.createFromImage(baseImage),
				ImageDescriptor.createFromImage(overlayImage), overlayPoint);
		return oid.createImage();
	}

}
