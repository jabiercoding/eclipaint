package org.eclipaint.editor;

import org.eclipaint.editor.activator.Activator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

/**
 * Zoom Utils
 * 
 * @author Jabier Martinez
 */
public class ZoomUtils {

	// Zoom
	public static final int ZOOM_MAXIMUM = 50;
	public static final int ZOOM_MINIMUM = 1;
	public Scale zoomScale = null;

	private ImageEditor editor;

	public ZoomUtils(ImageEditor editor) {
		this.editor = editor;
	}

	/**
	 * Update pixel positions based on new pixelLength
	 */
	public void updatePixelsPositions() {
		int x = 0;
		int y = 0;
		for (PixelItem pixelItem : editor.pixels) {
			pixelItem.pixelRectangle = new Rectangle(x, y, editor.pixelLength, editor.pixelLength);
			x += editor.pixelLength;
			if (x >= editor.pixelLength * editor.iconWidth) {
				x = 0;
				y += editor.pixelLength;
			}
		}
	}

	/**
	 * Update selected pixel positions based on new pixelLength Remember that
	 * editor.selectionRectangle contains pixel size information (1,1) and not
	 * (pixelLength, pixelLength)
	 */
	public void updateSelectedPixelsPositions() {
		if (editor.selectionRectangle != null) {
			int x = editor.selectionRectangle.x * editor.pixelLength;
			int y = editor.selectionRectangle.y * editor.pixelLength;
			for (PixelItem pixelItem : editor.selectedPixels) {
				pixelItem.pixelRectangle = new Rectangle(x, y, editor.pixelLength, editor.pixelLength);
				x += editor.pixelLength;
				if (x >= (editor.pixelLength * editor.selectionRectangle.width)
						+ (editor.selectionRectangle.x * editor.pixelLength)) {
					x = editor.selectionRectangle.x * editor.pixelLength;
					y += editor.pixelLength;
				}
			}
		}
	}

	/**
	 * Apply Zoom
	 * 
	 * @param zoomValue
	 */
	public void applyZoom(int zoomValue) {
		// zoomValue inside zoom boundaries
		if (zoomValue < ZOOM_MINIMUM) {
			zoomValue = ZOOM_MINIMUM;
		} else if (zoomValue > ZOOM_MAXIMUM) {
			zoomValue = ZOOM_MAXIMUM;
		}
		// update and redraw
		editor.pixelLength = zoomValue;
		updatePixelsPositions();
		updateSelectedPixelsPositions();
		zoomScale.setSelection(editor.pixelLength);
		editor.canvas.setBounds(editor.canvas.getBounds().x, editor.canvas.getBounds().y, editor.pixelLength
				* editor.iconWidth + 1, editor.pixelLength * editor.iconHeight + 1);
		editor.canvas.redraw();
		// Notify scrolls by sending a resize event
		editor.canvas.getParent().notifyListeners(SWT.Resize, null);
		editor.canvas.getParent().notifyListeners(SWT.Resize, null);
	}

	/**
	 * 
	 * @param toolBar
	 */
	public void addZoomToolItems(ToolBar toolBar) {
		ToolItem zoomOriginal = new ToolItem(toolBar, SWT.PUSH);
		zoomOriginal.setToolTipText("Original size");
		zoomOriginal.setImage(Activator.getImageDescriptor("icons/actions/zoomOriginal.png").createImage());
		zoomOriginal.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				applyZoom(1);
			}
		});

		ToolItem zoomIn = new ToolItem(toolBar, SWT.PUSH);
		zoomIn.setToolTipText("Zoom In");
		zoomIn.setImage(Activator.getImageDescriptor("icons/actions/zoomIn.png").createImage());
		zoomIn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				applyZoom(editor.pixelLength + 10);
			}
		});

		ToolItem zoomOut = new ToolItem(toolBar, SWT.PUSH);
		zoomOut.setToolTipText("Zoom Out");
		zoomOut.setImage(Activator.getImageDescriptor("icons/actions/zoomOut.png").createImage());
		zoomOut.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				applyZoom(editor.pixelLength - 10);
			}
		});

		ToolItem zoomFit = new ToolItem(toolBar, SWT.PUSH);
		zoomFit.setToolTipText("Fit to screen");
		zoomFit.setImage(Activator.getImageDescriptor("icons/actions/zoomFit.png").createImage());
		zoomFit.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				int newZoom = getFitToScreenZoom();
				applyZoom(newZoom);
			}
		});
	}

	/**
	 * create zoom scale
	 * 
	 * @param parent
	 * @return
	 */
	public Scale createZoomScale(Composite parent) {
		zoomScale = new Scale(parent, SWT.NONE);
		zoomScale.setToolTipText("Zoom");
		zoomScale.setMinimum(ZOOM_MINIMUM);
		zoomScale.setMaximum(ZOOM_MAXIMUM);
		zoomScale.setSelection(getInitialZoom());
		zoomScale.setIncrement(1);

		zoomScale.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				applyZoom(zoomScale.getSelection());
				// Put the focus in the canvas, otherwise scale will take
				// control of arrows
				editor.canvas.setFocus();
			}
		});
		GridData gridData = new GridData(SWT.RIGHT, SWT.BEGINNING, false, false);
		zoomScale.setLayoutData(gridData);
		return zoomScale;
	}

	/**
	 * Get fit to screen zoom level
	 */
	public int getFitToScreenZoom() {
		int compositeWidth = editor.canvas.getParent().getClientArea().width;
		int zoomWidth = compositeWidth / editor.iconWidth;

		int compositeHeight = editor.canvas.getParent().getClientArea().height;
		int zoomHeight = compositeHeight / editor.iconHeight;

		return Math.max(1, Math.min(zoomWidth, zoomHeight));
	}

	// TODO Implement a better approach trying to get canvas size
	public int getInitialZoom() {
		int max = Math.max(editor.iconWidth, editor.iconWidth);
		// normal icons 16x16
		if (max <= 19) {
			return 20;
			// wizbans 75x66
		} else if (max <= 80) {
			return 5;
		}
		// big images
		return 1;
	}

}
