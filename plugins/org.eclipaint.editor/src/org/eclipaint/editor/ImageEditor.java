package org.eclipaint.editor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import org.eclipaint.editor.activator.Activator;
import org.eclipaint.utils.ImageUtils;
import org.eclipaint.utils.WorkbenchUtils;
import org.eclipaint.utils.ui.SaveAsContainerSelectionDialog;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.FileEditorInput;

/**
 * Icons Editor
 * 
 * @author Jabier Martinez
 */
public class ImageEditor extends EditorPart {

	// Editor
	public static final String ID = "org.eclipaint.editor";
	protected FileEditorInput input;
	protected Canvas canvas = null;

	// The original icon dimension
	protected int iconHeight;
	protected int iconWidth;

	// used for isDirty editor property
	protected boolean modified = false;

	protected ImageData imageData;

	// pixels list. Size: iconWidth * iconHeight
	protected List<PixelItem> pixels = new ArrayList<PixelItem>();

	protected PixelItem colorPickerSelection = null;

	// Paint rectangle with real canvas dimensions
	protected Rectangle paintRectangle = null;

	// Selection rectangle with pixelItems dimensions
	protected Rectangle selectionRectangle = null;
	// Information about selectedPixels
	protected List<PixelItem> selectedPixels = new ArrayList<PixelItem>();

	// Whether the user is drawing/erasing something
	protected Boolean drawing = false;
	public Boolean selected = false;
	protected Boolean selectedAndMoved = false;
	protected Boolean mouseMoving = false;
	protected Point mousePoint = new Point(0, 0);

	// States
	protected ToolItem currentColorToolItem;
	protected ToolItem colorPickerToolItem;
	protected ToolItem selectToolItem;
	protected ToolItem paintToolItem;
	protected ToolItem unfilledRectangleToolItem;
	protected ToolItem filledRectangleToolItem;
	protected ToolItem fillToolItem;
	protected ToolItem eraseToolItem;

	protected int pixelLength = 1;

	// Editor Utils
	public EditorUtils editorUtils;
	public ZoomUtils zoomUtils;

	public boolean nativelyDoubleBufferedCanvas = false;

	// Undo/Redo stacks
	// TODO Management of dirty state with undo/redo
	public Stack<List<PixelItem>> undoStack;
	public Stack<List<PixelItem>> redoStack;
	public List<PixelItem> previousNonDirty = null;

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		if (!(input instanceof FileEditorInput)) {
			// TODO improve error handling. For example dropping an icon from
			// the icons view
			MessageDialog.openError(Display.getCurrent().getActiveShell(), "Icons editor",
					"Wrong input. Try to save the resource in the workspace first.");
			return;
		}
		this.input = (FileEditorInput) input;
		setSite(site);
		setInput(input);

		// Sets the name of the editor with file name
		setPartName(((FileEditorInput) input).getName());
		Image image = ImageUtils.getImageFromResource(this.input.getFile());

		// Error loading the resource
		if (image == null) {
			throw new PartInitException("It was not possible to load the resource as a valid image.");
		}

		editorUtils = new EditorUtils(this);
		zoomUtils = new ZoomUtils(this);
		imageData = image.getImageData();
		iconWidth = imageData.width;
		iconHeight = imageData.height;
		pixels = EditorUtils.initializePixels(imageData);
		pixelLength = zoomUtils.getInitialZoom();

		// Undo redo stacks
		undoStack = new Stack<List<PixelItem>>();
		redoStack = new Stack<List<PixelItem>>();
	}

	/**
	 * Activate only the selected toolItem
	 * 
	 * @param toolItem
	 */
	protected void selectToolItem(ToolItem toolItem) {
		selectToolItem.setSelection(selectToolItem == toolItem);
		paintToolItem.setSelection(paintToolItem == toolItem);
		unfilledRectangleToolItem.setSelection(unfilledRectangleToolItem == toolItem);
		filledRectangleToolItem.setSelection(filledRectangleToolItem == toolItem);
		colorPickerToolItem.setSelection(colorPickerToolItem == toolItem);
		eraseToolItem.setSelection(eraseToolItem == toolItem);
		fillToolItem.setSelection(fillToolItem == toolItem);

		// Remove selection rectangle if it exists
		if (selected) {
			if (selectedAndMoved) {
				editorUtils.blendSelection();
			}
			deactivateSelection();
		}
	}

	public void deactivateSelection() {
		paintRectangle = null;
		selected = false;
		selectedAndMoved = false;
		selectedPixels = null;
		selectionRectangle = null;
		canvas.redraw();
	}

	/**
	 * Create Tool Items
	 * 
	 * @param toolBar
	 */
	private void createToolItems(ToolBar toolBar) {
		currentColorToolItem = new ToolItem(toolBar, SWT.CHECK);
		currentColorToolItem.setToolTipText("Current color");

		// initialize color selection
		// get first non transparent one
		for (PixelItem item : pixels) {
			if (item.alpha == 255) {
				colorPickerSelection = (PixelItem) item.clone();
				break;
			}
		}
		// if not found, get the first color
		if (colorPickerSelection == null) {
			colorPickerSelection = (PixelItem) pixels.get(0).clone();
		}
		currentColorToolItem.setImage(EditorUtils.createImageForColorSelection(colorPickerSelection.color,
				colorPickerSelection.alpha));

		new ToolItem(toolBar, SWT.SEPARATOR);

		colorPickerToolItem = new ToolItem(toolBar, SWT.CHECK);
		colorPickerToolItem.setToolTipText("Pick Color");
		colorPickerToolItem.setImage(Activator.getImageDescriptor("icons/actions/colorPicker.png").createImage());

		new ToolItem(toolBar, SWT.SEPARATOR);

		selectToolItem = new ToolItem(toolBar, SWT.CHECK);
		selectToolItem.setToolTipText("Select");
		selectToolItem.setImage(Activator.getImageDescriptor("icons/actions/select.png").createImage());

		new ToolItem(toolBar, SWT.SEPARATOR);

		paintToolItem = new ToolItem(toolBar, SWT.CHECK);
		paintToolItem.setToolTipText("Paint");
		paintToolItem.setImage(Activator.getImageDescriptor("icons/actions/paint.png").createImage());

		unfilledRectangleToolItem = new ToolItem(toolBar, SWT.CHECK);
		unfilledRectangleToolItem.setToolTipText("Rectangle");
		unfilledRectangleToolItem.setImage(Activator.getImageDescriptor("icons/actions/unfilledRectangle.png")
				.createImage());

		filledRectangleToolItem = new ToolItem(toolBar, SWT.CHECK);
		filledRectangleToolItem.setToolTipText("Filled Rectangle");
		filledRectangleToolItem.setImage(Activator.getImageDescriptor("icons/actions/filledRectangle.png")
				.createImage());

		fillToolItem = new ToolItem(toolBar, SWT.CHECK);
		fillToolItem.setToolTipText("Fill");
		fillToolItem.setImage(Activator.getImageDescriptor("icons/actions/fill.png").createImage());

		eraseToolItem = new ToolItem(toolBar, SWT.CHECK);
		eraseToolItem.setToolTipText("Erase");
		eraseToolItem.setImage(Activator.getImageDescriptor("icons/actions/erase.png").createImage());

		// Not enabled if bmp or jpg
		// it could have (imageData.getTransparencyType() ==
		// SWT.TRANSPARENCY_NONE) and still be a png or gif
		if (!ImageUtils.isTransparentImageFile(input.getFile())) {
			eraseToolItem.setToolTipText("Erase disabled in bmp files and transparency disabled images");
			eraseToolItem.setEnabled(false);
		}

		new ToolItem(toolBar, SWT.SEPARATOR);

		// Set the initial tool
		selectToolItem(paintToolItem);
	}

	@Override
	/**
	 * Create Part Control
	 */
	public void createPartControl(Composite parent_original) {

		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		parent_original.setLayout(gridLayout);

		parent_original.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false));

		ToolBar toolBar = new ToolBar(parent_original, SWT.FLAT);

		createToolItems(toolBar);
		createToolItemSelectionListeners();
		zoomUtils.addZoomToolItems(toolBar);

		GridData gridData = new GridData(GridData.FILL, SWT.BEGINNING, true, false);
		toolBar.setLayoutData(gridData);

		zoomUtils.createZoomScale(parent_original);

		createCanvasAndPaintControl(parent_original);

		gridData = new GridData(GridData.FILL, GridData.FILL, true, true);
		gridData.horizontalSpan = 2;
		canvas.setLayoutData(gridData);

		// Mouse listeners to draw
		createCanvasMouseListeners();

		// Keyboard listeners
		createCanvasKeyboardListeners();

		// Force applyZoom to initialize pixel items
		zoomUtils.applyZoom(zoomUtils.getInitialZoom());
	}

	private void createCanvasKeyboardListeners() {
		canvas.addKeyListener(new KeyListener() {

			@Override
			public void keyPressed(KeyEvent e) {
				if (selected) {
					if (e.keyCode == SWT.ARROW_RIGHT || e.keyCode == SWT.ARROW_LEFT || e.keyCode == SWT.ARROW_DOWN
							|| e.keyCode == SWT.ARROW_UP) {
						// moved for the first time
						if (!selectedAndMoved) {

							// Save previous in undoStack
							editorUtils.storeInUndoStack();

							editorUtils.delete(false);
							selectedAndMoved = true;
						}
						editorUtils.moveSelectedPixels(e.keyCode);
						canvas.redraw();
					}
				}
			}

			@Override
			public void keyReleased(KeyEvent e) {
				// Do nothing
			}

		});
	}

	/**
	 * Create canvas and how it is redrawn
	 * 
	 * @param parent_original
	 */
	private void createCanvasAndPaintControl(Composite parent_original) {
		canvas = createCanvas(parent_original, new PaintListener() {
			public void paintControl(PaintEvent e) {
				GC gc;
				Image bufferImage = null;
				if (!nativelyDoubleBufferedCanvas) {
					try {
						bufferImage = new Image(Display.getCurrent(), canvas.getBounds().width,
								canvas.getBounds().height);
						gc = new GC(bufferImage);
					} catch (SWTError noMoreHandlesError) {
						// No more handles error with big images
						bufferImage = null;
						gc = e.gc;
					}
				} else {
					gc = e.gc;
				}

				// Fill all canvas
				gc.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
				gc.fillRectangle(canvas.getClientArea());

				// global information for painting the background
				int halfPixel = pixelLength / 2;

				// paint pixels
				for (Iterator<PixelItem> i = pixels.iterator(); i.hasNext();) {
					PixelItem pixel = (PixelItem) i.next();
					// paint background squares for transparent (only if not
					// original size)
					if (pixelLength != 1 && pixel.alpha != 255) {
						gc.setAlpha(255);
						gc.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
						gc.fillRectangle(pixel.pixelRectangle.x, pixel.pixelRectangle.y, halfPixel, halfPixel);
						gc.fillRectangle(pixel.pixelRectangle.x + halfPixel, pixel.pixelRectangle.y + halfPixel,
								halfPixel, halfPixel);
					}
					// paint the pixel itself
					pixel.paint(gc);
				}

				// paint boundary rectangle (only if not original size)
				if (pixelLength != 1) {
					gc.setAlpha(255);
					gc.drawRectangle(0, 0, pixelLength * iconWidth, pixelLength * iconHeight);
				}

				// Paint rectangle selection
				if (unfilledRectangleToolItem.getSelection() || filledRectangleToolItem.getSelection()) {
					if (drawing) {
						if (paintRectangle != null) {
							gc.drawRectangle(paintRectangle);
						}
					}
				}

				// Selecting
				if (selectToolItem.getSelection()) {
					// Not yet finished the selection
					if (!selected && paintRectangle != null) {
						gc.setLineStyle(SWT.LINE_DOT);
						Rectangle adjustedRectangle = new Rectangle(paintRectangle.x, paintRectangle.y,
								paintRectangle.width, paintRectangle.height);
						editorUtils.adjustRectangle(adjustedRectangle);
						gc.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_BLUE));
						gc.drawRectangle(adjustedRectangle);
					}
					// Selection done. Paint selectionRectangle
					if (selected) {
						// Paint selection on top of everything to allow moving
						// it
						// paint pixels
						if (selectedAndMoved) {
							// Show moved
							for (Iterator<PixelItem> i = selectedPixels.iterator(); i.hasNext();) {
								PixelItem pixel = (PixelItem) i.next();
								// paint the pixel itself
								pixel.paint(gc);
							}
						}

						// Translate to canvas dimensions
						gc.setLineStyle(SWT.LINE_DOT);
						Rectangle translatedToCanvas = new Rectangle(selectionRectangle.x * pixelLength,
								selectionRectangle.y * pixelLength, selectionRectangle.width * pixelLength,
								selectionRectangle.height * pixelLength);
						gc.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_BLUE));
						gc.drawRectangle(translatedToCanvas);
					}
				}
				if (!nativelyDoubleBufferedCanvas && bufferImage != null) {
					e.gc.drawImage(bufferImage, 0, 0);
					bufferImage.dispose();
				}
			}

		});
	}

	/**
	 * Selection listeners for actions
	 */
	private void createToolItemSelectionListeners() {
		selectToolItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				selectToolItem(selectToolItem);

				// TODO For some reason, first applyZoom call in
				// createPartControl does not prevent the moved selections
				// outside of the icon bounds to be hided. So this is a hack to
				// be fixed. Same hack in EditorUtils.selectAll
				zoomUtils.applyZoom(zoomUtils.zoomScale.getSelection());
			}
		});

		currentColorToolItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {

				RGB selectedColor = null;
				if (imageData.palette.isDirect) {
					// Direct palette
					ColorDialog colorDialog = new ColorDialog(Display.getCurrent().getActiveShell());
					selectedColor = colorDialog.open();
				} else {
					// Indirect palette
					// Is there a non used position in the palette?
					int palettePosition = editorUtils.getAvailablePalettePosition(imageData);
					// No position... not supported yet.
					if (palettePosition == -1) {
						// Error image
						MessageDialog
								.openError(Display.getDefault().getActiveShell(), "Info",
										"Sorry but all positions in the image palette are being used.\nPick a color from the image");

						selectToolItem(colorPickerToolItem);
					} else {
						// The user selects a color
						ColorDialog colorDialog = new ColorDialog(Display.getCurrent().getActiveShell());
						selectedColor = colorDialog.open();
						if (selectedColor != null) {
							// Check if the selected color is already in the
							// palette
							int alreadyInPalettePosition = ImageUtils.getRGBPositionInPalette(imageData, selectedColor);
							// Not found so
							if (alreadyInPalettePosition == -1) {
								// Add the color to the palette
								imageData.palette.getRGBs()[palettePosition] = selectedColor;
							}
						}
					}
				}

				if (selectedColor != null) {
					// Update selectedPixel
					colorPickerSelection.color = selectedColor;
					colorPickerSelection.alpha = 255; // opaque
					currentColorToolItem.setImage(EditorUtils.createImageForColorSelection(colorPickerSelection.color,
							colorPickerSelection.alpha));
				}

				// Never show it as selected
				currentColorToolItem.setSelection(false);

				// If colorPicker was the active tool item we change it to paint
				// tool
				if (colorPickerToolItem.getSelection()) {
					selectToolItem(paintToolItem);
				}
			}
		});

		paintToolItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (colorPickerSelection == null) {
					MessageDialog.openInformation(Display.getDefault().getActiveShell(), "Info",
							"Pick a color from the image before painting");
					selectToolItem(colorPickerToolItem);
				} else {
					selectToolItem(paintToolItem);
				}
			}
		});

		unfilledRectangleToolItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (colorPickerSelection == null) {
					MessageDialog.openInformation(Display.getDefault().getActiveShell(), "Info",
							"Pick a color from the image before painting");
					selectToolItem(colorPickerToolItem);
				} else {
					selectToolItem(unfilledRectangleToolItem);
				}
			}
		});

		filledRectangleToolItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (colorPickerSelection == null) {
					MessageDialog.openInformation(Display.getDefault().getActiveShell(), "Info",
							"Pick a color from the image before painting");
					selectToolItem(colorPickerToolItem);
				} else {
					selectToolItem(filledRectangleToolItem);
				}
			}
		});

		fillToolItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				// for the moment, only selected colors with the color picker is
				// allowed
				if (colorPickerSelection == null) {
					MessageDialog.openInformation(Display.getDefault().getActiveShell(), "Info",
							"Pick a color from the image before painting");
					selectToolItem(colorPickerToolItem);
				} else {
					selectToolItem(fillToolItem);
				}
			}
		});

		colorPickerToolItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				selectToolItem(colorPickerToolItem);
			}
		});

		eraseToolItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				boolean possible = true;
				// Special case where we have a gif or png with transparency
				// none and we want to create a transparent pixels
				if (imageData.getTransparencyType() == SWT.TRANSPARENCY_NONE
						&& ImageUtils.isTransparentImageFile(input.getFile()) && !imageData.palette.isDirect) {
					// check if there is position
					int transparentPixel = editorUtils.getAvailablePalettePosition(imageData);
					// not found space in the palette for the transparent
					// pixel...
					if (transparentPixel == -1) {
						MessageDialog.openError(Display.getDefault().getActiveShell(), "Info",
								"All positions in the palette are being used. Imposible to create transparent pixel.");
						possible = false;
					} else {
						// we set the transparent pixel
						imageData.transparentPixel = transparentPixel;
					}
				}
				if (possible) {
					selectToolItem(eraseToolItem);
				} else {
					eraseToolItem.setSelection(false);
				}
			}
		});
	}

	/**
	 * Perform Save
	 * 
	 * @param fileAbsPath
	 * @param monitor
	 */
	public void performSave(String fileAbsPath, IProgressMonitor monitor) {

		boolean errorSaving = false;

		// First blend the selection if active
		if (selected) {
			if (selectedAndMoved) {
				editorUtils.blendSelection();
			}
			deactivateSelection();
		}

		monitor.beginTask("Save", 1);

		// Start the process
		ImageData newImageData = (ImageData) imageData.clone();

		// Modify imageData with pixels information
		for (PixelItem pixelItem : pixels) {

			// Save alpha
			newImageData.setAlpha(pixelItem.realPosition.x, pixelItem.realPosition.y, pixelItem.alpha);

			// Set transparency pixel
			if (newImageData.getTransparencyType() == SWT.TRANSPARENCY_PIXEL) {
				if (pixelItem.alpha == 0) {
					newImageData.setPixel(pixelItem.realPosition.x, pixelItem.realPosition.y,
							newImageData.transparentPixel);
				}
			}

			// Save colors
			RGB color = pixelItem.color;

			// The image has a non-direct color model
			if (!newImageData.palette.isDirect) {
				// Dont change the pixel data if it was already set as
				// transparency
				if ((newImageData.getTransparencyType() == SWT.TRANSPARENCY_PIXEL && pixelItem.alpha != 0)
						|| newImageData.getTransparencyType() != SWT.TRANSPARENCY_PIXEL) {
					// Get the index of the color in the palette
					int position = ImageUtils.getRGBPositionInPalette(newImageData, color);
					if (position == -1) {
						// not found!
						int availablePositionIndex = editorUtils.getAvailablePalettePosition(newImageData);
						if (availablePositionIndex != -1) {
							newImageData.getRGBs()[availablePositionIndex] = pixelItem.color;
							newImageData.setPixel(pixelItem.realPosition.x, pixelItem.realPosition.y,
									availablePositionIndex);
						} else {
							errorSaving = true;
						}
					} else {
						newImageData.setPixel(pixelItem.realPosition.x, pixelItem.realPosition.y, position);
					}
				}
			} else {
				// Direct color model
				int pixelValue = newImageData.palette.getPixel(color);
				newImageData.setPixel(pixelItem.realPosition.x, pixelItem.realPosition.y, pixelValue);
			}

		}

		// Save it
		if (!errorSaving) {
			int imageFormat = ImageUtils.getImageFormat(imageData, input.getFile().getFileExtension());
			ImageUtils.saveImageToFile(newImageData, fileAbsPath, imageFormat);

			monitor.worked(1);
			monitor.done();

			// refresh workspace
			WorkbenchUtils.refreshWorkspace(fileAbsPath);
		} else {
			MessageDialog.openError(Display.getDefault().getActiveShell(), "Error",
					"Error while saving. No available positions on image palette.");
		}
	}

	@Override
	public void doSave(IProgressMonitor monitor) {

		String absPath = input.getFile().getLocation().toOSString();
		if (WorkbenchUtils.checkIfWritable(absPath)) {
			performSave(absPath, monitor);
			// Set editor as no dirty
			modified = false;
			firePropertyChange(IEditorPart.PROP_DIRTY);
		}
	}

	@Override
	public void doSaveAs() {

		SaveAsContainerSelectionDialog dialog = new SaveAsContainerSelectionDialog(Display.getCurrent()
				.getActiveShell(), ResourcesPlugin.getWorkspace().getRoot(), false, "Select image container and name",
				input.getFile().getName());
		if (dialog.open() == Dialog.OK) {
			IPath selectedContainer = (IPath) dialog.getResult()[0];
			IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
			IResource res = root.findMember(selectedContainer);
			String absPath = res.getLocation().append(dialog.getFileName()).toOSString();
			if (WorkbenchUtils.checkIfWritable(absPath)) {
				performSave(absPath, new NullProgressMonitor());
				// Open the file
				WorkbenchUtils.openFile(absPath);
			}
		}
	}

	@Override
	public boolean isDirty() {
		return modified;
	}

	@Override
	public boolean isSaveAsAllowed() {
		// Always true
		return true;
	}

	/**
	 * Canvas Mouse Listeners
	 */
	private void createCanvasMouseListeners() {
		// Mouse Down
		canvas.addListener(SWT.MouseDown, new Listener() {
			public void handleEvent(Event e) {

				PixelItem selectedPixel = editorUtils.getCanvasPixel(e.x, e.y);
				if (selectedPixel != null) {
					// Erase
					if (eraseToolItem.getSelection()) {
						drawing = true;

						// Save previous in undoStack
						editorUtils.storeInUndoStack();

						boolean modified = EditorUtils.paintTransparentPixel(selectedPixel);
						if (modified) {
							notifyPixelModification(selectedPixel);
						}
					}
					// Paint
					else if (paintToolItem.getSelection()) {
						drawing = true;

						// Save previous in undoStack
						editorUtils.storeInUndoStack();

						boolean modified = editorUtils.paintPixel(colorPickerSelection, selectedPixel);
						if (modified) {
							notifyPixelModification(selectedPixel);
						}

					}
					// Unfilled Rectangle
					else if (unfilledRectangleToolItem.getSelection() || filledRectangleToolItem.getSelection()) {
						drawing = true;
						paintRectangle = new Rectangle(e.x, e.y, 2, 2);
						canvas.redraw();
					}
					// Selection
					else if (selectToolItem.getSelection()) {

						// Another selection was started?
						if (selected) {
							// Check if it clicked inside the selection
							boolean clickedInsideSelection = false;
							for (PixelItem pixelItem : selectedPixels) {
								if (selectedPixel.pixelRectangle.intersects(pixelItem.pixelRectangle)) {
									clickedInsideSelection = true;
									break;
								}
							}
							// If not clicked inside blend and remove
							if (!clickedInsideSelection) {
								if (selectedAndMoved) {
									editorUtils.blendSelection();
								}
								deactivateSelection();
							} else {
								// If clicked inside selection allows to move it
								mouseMoving = true;
								mousePoint.x = e.x;
								mousePoint.y = e.y;
							}
						}

						paintRectangle = new Rectangle(e.x, e.y, 2, 2);
						canvas.redraw();
					}
					// Fill
					else if (fillToolItem.getSelection()) {
						drawing = false;

						// Save previous in undoStack
						editorUtils.storeInUndoStack();

						boolean modified = editorUtils.fillPixels((PixelItem) selectedPixel.clone(), selectedPixel);
						if (modified) {
							notifyPixelModification(selectedPixel);
						}
					}
					// Pick Color
					else if (colorPickerToolItem.getSelection()) {
						drawing = false;
						colorPickerSelection = (PixelItem) selectedPixel.clone();
						selectToolItem(paintToolItem);
						currentColorToolItem.setImage(EditorUtils.createImageForColorSelection(
								colorPickerSelection.color, colorPickerSelection.alpha));
					}
				}
			}
		});

		// Mouse Move
		canvas.addListener(SWT.MouseMove, new Listener() {
			public void handleEvent(Event e) {
				// Only process this if drawing
				if (drawing) {
					PixelItem selectedPixel = editorUtils.getCanvasPixel(e.x, e.y);
					if (selectedPixel != null) {
						// Erase
						if (eraseToolItem.getSelection()) {
							boolean modified = EditorUtils.paintTransparentPixel(selectedPixel);
							if (modified) {
								notifyPixelModification(selectedPixel);
							}
						}
						// Paint
						else if (paintToolItem.getSelection()) {
							boolean modified = editorUtils.paintPixel(colorPickerSelection, selectedPixel);
							if (modified) {
								notifyPixelModification(selectedPixel);
							}
						}
						// filled or unfilled Rectangle
						else if (unfilledRectangleToolItem.getSelection() || filledRectangleToolItem.getSelection()) {
							paintRectangle.width = e.x - paintRectangle.x;
							paintRectangle.height = e.y - paintRectangle.y;
							canvas.redraw();
						}
					}
				}
				// Select
				else if (!mouseMoving && selectToolItem.getSelection()) {
					if (paintRectangle != null) {
						PixelItem selectedPixel = editorUtils.getCanvasPixel(e.x, e.y);
						if (selectedPixel != null) {
							paintRectangle.width = e.x - paintRectangle.x;
							paintRectangle.height = e.y - paintRectangle.y;
							canvas.redraw();
						}
					}
				}
				if (selected && mouseMoving) {
					int xDis = e.x - mousePoint.x;
					int yDis = e.y - mousePoint.y;
					if (Math.abs(xDis) >= pixelLength || Math.abs(yDis) >= pixelLength) {
						int xPos = ((e.x - mousePoint.x) / pixelLength);
						int yPos = ((e.y - mousePoint.y) / pixelLength);

						mousePoint.x = e.x;
						mousePoint.y = e.y;
						int xDir = SWT.ARROW_LEFT;
						if (xPos > 0) {
							xDir = SWT.ARROW_RIGHT;
						}
						int yDir = SWT.ARROW_UP;
						if (yPos > 0) {
							yDir = SWT.ARROW_DOWN;
						}
						// moved for the first time
						if (!selectedAndMoved) {

							// Save previous in undoStack
							editorUtils.storeInUndoStack();

							editorUtils.delete(false);
							selectedAndMoved = true;
						}
						for (int x = 0; x < Math.abs(xPos); x++) {
							editorUtils.moveSelectedPixels(xDir);
						}
						for (int x = 0; x < Math.abs(yPos); x++) {
							editorUtils.moveSelectedPixels(yDir);
						}
						canvas.redraw();
					}
				}
			}
		});

		// Mouse Up
		canvas.addListener(SWT.MouseUp, new Listener() {
			public void handleEvent(Event e) {
				if (drawing) {

					// UnfilledRectangle
					if (unfilledRectangleToolItem.getSelection()) {

						// Save previous in undoStack
						editorUtils.storeInUndoStack();

						editorUtils.paintUnfilledRectangle();
						paintRectangle = null;
						canvas.redraw();
					}

					// FilledRectangle
					if (filledRectangleToolItem.getSelection()) {

						// Save previous in undoStack
						editorUtils.storeInUndoStack();

						editorUtils.paintFilledRectangle();
						paintRectangle = null;
						canvas.redraw();
					}
				}
				drawing = false;

				// TODO Problems when selection starts ok but ends outside the
				// canvas
				if (!mouseMoving && selectToolItem.getSelection() && paintRectangle != null) {
					// Create selectionRectangle
					selected = true;
					selectedAndMoved = false;
					PixelItem topLeft = (PixelItem) editorUtils.getRectangleTopLeftPixelItem(paintRectangle).clone();
					PixelItem bottomRight = (PixelItem) editorUtils.getRectangleBottomRightPixelItem(paintRectangle)
							.clone();
					selectionRectangle = new Rectangle(topLeft.realPosition.x, topLeft.realPosition.y,
							bottomRight.realPosition.x - topLeft.realPosition.x + 1, bottomRight.realPosition.y
									- topLeft.realPosition.y + 1);

					// Create selectedPixels
					selectedPixels = new ArrayList<PixelItem>();
					for (int y = topLeft.realPosition.y; y <= bottomRight.realPosition.y; y++) {
						for (int x = topLeft.realPosition.x; x <= bottomRight.realPosition.x; x++) {
							PixelItem originalPixel = pixels.get(editorUtils.getPixelPositionInTheArray(x, y));
							PixelItem selectedPixel = (PixelItem) originalPixel.clone();
							selectedPixels.add(selectedPixel);
						}
					}
					canvas.redraw();
				}
				mouseMoving = false;
			}
		});
	}

	@Override
	public void setFocus() {
	}

	/**
	 * notify pixel modification
	 * 
	 * @param pixel
	 */
	protected void notifyPixelModification(PixelItem pixel) {
		// force isDirty method of the EditPart to register the
		// modification
		if (!modified) {
			modified = true;
			firePropertyChange(IEditorPart.PROP_DIRTY);
		}
		// only redraw the modified one
		canvas.redraw(pixel.pixelRectangle.x, pixel.pixelRectangle.y, pixel.pixelRectangle.width,
				pixel.pixelRectangle.height, false);
	}

	/**
	 * Create canvas
	 * 
	 * @param parent
	 * @param paintListener
	 * @return the canvas
	 */
	protected Canvas createCanvas(Composite parent, PaintListener pl) {
		final Composite sc = new Composite(parent, SWT.V_SCROLL | SWT.H_SCROLL);
		sc.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true, 2, 1));
		final Canvas c = new Canvas(sc, SWT.NO_BACKGROUND);
		nativelyDoubleBufferedCanvas = ((c.getStyle() & SWT.DOUBLE_BUFFERED) != 0);
		if (pl != null) {
			c.addPaintListener(pl);
		}

		// Cursor for editing
		c.setCursor(new Cursor(Display.getCurrent(), SWT.CURSOR_CROSS));

		// Scrolling code
		final ScrollBar hBar = sc.getHorizontalBar();
		hBar.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				Point location = c.getLocation();
				location.x = -hBar.getSelection();
				c.setLocation(location);
			}
		});
		final ScrollBar vBar = sc.getVerticalBar();
		vBar.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				Point location = c.getLocation();
				location.y = -vBar.getSelection();
				c.setLocation(location);
			}
		});
		sc.addListener(SWT.Resize, new Listener() {
			public void handleEvent(Event e) {
				Point size = c.getSize();
				Rectangle rect = sc.getClientArea();
				hBar.setMaximum(size.x);
				vBar.setMaximum(size.y);
				hBar.setThumb(Math.min(size.x, rect.width));
				vBar.setThumb(Math.min(size.y, rect.height));
				int hPage = size.x - rect.width;
				int vPage = size.y - rect.height;
				int hSelection = hBar.getSelection();
				int vSelection = vBar.getSelection();
				Point location = c.getLocation();
				if (hSelection >= hPage) {
					if (hPage <= 0)
						hSelection = 0;
					location.x = -hSelection;
				}
				if (vSelection >= vPage) {
					if (vPage <= 0)
						vSelection = 0;
					location.y = -vSelection;
				}
				c.setLocation(location);
			}
		});

		return c;
	}

	/**
	 * Overriding Dispose to dispose our elements
	 */
	@Override
	public void dispose() {
		input = null;
		if (canvas != null) {
			canvas.dispose();
		}
		super.dispose();
		if (pixels != null) {
			pixels.clear();
			pixels = null;
		}
		if (selectedPixels != null) {
			selectedPixels.clear();
			selectedPixels = null;
		}
		imageData = null;

		if (undoStack != null) {
			undoStack.clear();
			undoStack = null;
		}
		if (redoStack != null) {
			redoStack.clear();
			redoStack = null;
		}
		if (previousNonDirty != null) {
			previousNonDirty.clear();
			previousNonDirty = null;
		}
	}

	public void changeDirty() {
		firePropertyChange(IEditorPart.PROP_DIRTY);
	}

}
