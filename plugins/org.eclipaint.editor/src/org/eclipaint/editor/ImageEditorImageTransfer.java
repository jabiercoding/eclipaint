package org.eclipaint.editor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.dnd.ByteArrayTransfer;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;

/**
 * Icons Editor Image Transfer
 * 
 * @author Jabier Martinez
 */
public class ImageEditorImageTransfer extends ByteArrayTransfer {

	private static final String TYPENAME = "icons_editor_image_transfer";

	private static final int TYPEID = registerType(TYPENAME);

	private static ImageEditorImageTransfer _instance = new ImageEditorImageTransfer();

	public static ImageEditorImageTransfer getInstance() {
		return _instance;
	}

	protected String[] getTypeNames() {
		return new String[] { TYPENAME };
	}

	protected int[] getTypeIds() {
		return new int[] { TYPEID };
	}

	boolean checkMyType(Object object) {
		if (object == null || !(object instanceof IconsEditorImageData)) {
			return false;
		}
		return true;
	}

	protected boolean validate(Object object) {
		return checkMyType(object);
	}

	static class IconsEditorImageData {
		List<PixelItem> pixels;
		int iconWidth;
		int iconHeight;
	}

	public void javaToNative(Object object, TransferData transferData) {
		if (!checkMyType(object) || !isSupportedType(transferData)) {
			DND.error(DND.ERROR_INVALID_DATA);
		}
		IconsEditorImageData myType = (IconsEditorImageData) object;
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			DataOutputStream writeOut = new DataOutputStream(out);
			writeOut.writeInt(myType.iconWidth);
			writeOut.writeInt(myType.iconHeight);
			for (int x = 0; x < myType.iconWidth * myType.iconHeight; x++) {
				writeOut.writeInt(myType.pixels.get(x).alpha);
				writeOut.writeInt(myType.pixels.get(x).color.red);
				writeOut.writeInt(myType.pixels.get(x).color.green);
				writeOut.writeInt(myType.pixels.get(x).color.blue);
			}
			byte[] buffer = out.toByteArray();
			writeOut.close();
			super.javaToNative(buffer, transferData);
		} catch (IOException e) {
		}
	}

	public Object nativeToJava(TransferData transferData) {
		if (isSupportedType(transferData)) {
			byte[] buffer = (byte[]) super.nativeToJava(transferData);
			if (buffer == null)
				return null;

			IconsEditorImageData myData = new IconsEditorImageData();
			try {
				ByteArrayInputStream in = new ByteArrayInputStream(buffer);
				DataInputStream readIn = new DataInputStream(in);
				int width = readIn.readInt();
				int height = readIn.readInt();
				myData.iconWidth = width;
				myData.iconHeight = height;
				myData.pixels = new ArrayList<PixelItem>();
				for (int y = 0; y < height; y++) {
					for (int x = 0; x < width; x++) {
						PixelItem pi = new PixelItem();
						pi.alpha = readIn.readInt();
						pi.color = new RGB(0, 0, 0);
						pi.color.red = readIn.readInt();
						pi.color.green = readIn.readInt();
						pi.color.blue = readIn.readInt();
						pi.realPosition = new Point(x, y);
						myData.pixels.add(pi);
					}
				}
				readIn.close();
			} catch (IOException ex) {
				return null;
			}
			return myData;
		}

		return null;
	}

}
