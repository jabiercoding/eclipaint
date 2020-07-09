package org.eclipaint.manager.crawlers;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipaint.manager.activator.Activator;
import org.eclipaint.utils.ImageUtils;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

/**
 * @author Jabier Martinez
 */
public class CrawlWhiteBackgroundIconsAction extends Action {

	public CrawlWhiteBackgroundIconsAction() {
		this.setImageDescriptor(Activator.getImageDescriptor("icons/crawlers/crawlWhiteBackgroundIconsAction.png"));
		this.setText("Crawl WhiteBackground Icons");
		this.setToolTipText("Crawl WhiteBackground Icons");
	}

	final int COLUMN_COUNT = 1;
	final int TEXT_MARGIN = 3;
	final String KEY_WIDTHS = "widths";
	final String KEY_IMAGES = "images";
	List<String> paths = new ArrayList<String>();
	Button copyClipboardButton;
	Button discardButton;

	public void run() {

		Display display = Display.getCurrent();
		DirectoryDialog dialog = new DirectoryDialog(display.getActiveShell(), SWT.OPEN);
		dialog.setText("Crawl WhiteBackground icons");
		dialog.setMessage("Select the folder which contents will be analyzed");
		String result = dialog.open();
		if (result != null) {

			// Select the folder to analyze
			File selectedFolder = new File(result);

			// Process
			loopAndProcess(selectedFolder);

			// Open result
			if (paths.isEmpty()) {
				MessageDialog.openInformation(Display.getCurrent().getActiveShell(), "Crawl WhiteBackground icons",
						"No potential WhiteBackground Icons found.");
			} else {

				GridLayout gridLayout = new GridLayout(1, false);
				Shell shell = new Shell(display);
				shell.setText("Crawl WhiteBackground icons");
				shell.setImage(Activator.getImageDescriptor("icons/crawlers/crawlWhiteBackgroundIconsAction.png").createImage());
				shell.setLayout(gridLayout);

				Label label = new Label(shell, SWT.NONE);
				label.setText("Potential white background icons found: " + paths.size());

				final Table table = new Table(shell, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI);
				GridData gridDataTable = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_FILL);
				gridDataTable.grabExcessVerticalSpace = true;
				gridDataTable.grabExcessHorizontalSpace = true;
				table.setLayoutData(gridDataTable);

				final int itemHeight = table.getItemHeight();
				GC gc = new GC(table);
				FontMetrics metrics = gc.getFontMetrics();
				final int fontHeight = metrics.getHeight();
				gc.dispose();

				copyClipboardButton = new Button(shell, SWT.PUSH);
				copyClipboardButton.setText("Copy paths to clipboard");
				copyClipboardButton.setImage(PlatformUI.getWorkbench().getSharedImages()
						.getImage(ISharedImages.IMG_TOOL_COPY));
				copyClipboardButton.setEnabled(false);
				final Clipboard cb = new Clipboard(display);
				Listener listener = new Listener() {
					public void handleEvent(Event event) {
						String textData = "";
						TableItem[] selectedItems = table.getSelection();
						for (TableItem item : selectedItems) {
							textData = textData + item.getText() + "\n";
						}
						TextTransfer textTransfer = TextTransfer.getInstance();
						cb.setContents(new Object[] { textData }, new Transfer[] { textTransfer });
					}
				};
				copyClipboardButton.addListener(SWT.Selection, listener);

				discardButton = new Button(shell, SWT.PUSH);
				discardButton.setText("Discard");
				discardButton.setImage(PlatformUI.getWorkbench().getSharedImages()
						.getImage(ISharedImages.IMG_ETOOL_CLEAR));
				discardButton.setEnabled(false);
				Listener discardListener = new Listener() {
					public void handleEvent(Event event) {
						TableItem[] selectedItems = table.getSelection();
						for (TableItem item : selectedItems) {
							String foundPath = null;
							for (String path : paths) {
								if (item.getText().equals(path)) {
									foundPath = path;
									break;
								}
							}
							if (foundPath != null) {
								paths.remove(foundPath);
							}
						}
						table.removeAll();
						fillTable(table);
						table.redraw();
					}
				};
				discardButton.addListener(SWT.Selection, discardListener);

				Listener paintListener = new Listener() {
					public void handleEvent(Event event) {

						switch (event.type) {
						case SWT.MeasureItem: {
							int column = event.index;
							TableItem item = (TableItem) event.item;
							Image[] images = (Image[]) item.getData(KEY_IMAGES);
							Image image = images[column];
							if (image == null) {
								/*
								 * don't change the native-calculated
								 * event.width
								 */
								break;
							}
							int[] cachedWidths = (int[]) item.getData(KEY_WIDTHS);
							if (cachedWidths == null) {
								cachedWidths = new int[COLUMN_COUNT];
								item.setData(KEY_WIDTHS, cachedWidths);
							}
							if (cachedWidths[column] == 0) {
								int width = image.getBounds().width + 2 * TEXT_MARGIN;
								GC gc = new GC(item.getParent());
								width += gc.stringExtent(item.getText()).x;
								gc.dispose();
								cachedWidths[column] = width;
							}
							event.width = cachedWidths[column];
							break;
						}
						case SWT.EraseItem: {
							int column = event.index;
							TableItem item = (TableItem) event.item;
							Image[] images = (Image[]) item.getData(KEY_IMAGES);
							Image image = images[column];
							if (image == null) {
								break;
							}
							/* disable the native drawing of this item */
							event.detail &= ~SWT.FOREGROUND;
							break;
						}
						case SWT.PaintItem: {
							int column = event.index;
							TableItem item = (TableItem) event.item;
							Image[] images = (Image[]) item.getData(KEY_IMAGES);
							Image image = images[column];
							if (image == null) {
								/* this item is drawn natively, don't touch it */
								break;
							}

							int x = event.x;
							event.gc.drawImage(image, x, event.y + (itemHeight - image.getBounds().height) / 2);
							x += image.getBounds().width + TEXT_MARGIN;
							event.gc.drawString(item.getText(column), x, event.y + (itemHeight - fontHeight) / 2);
							break;
						}
						}
					}
				};

				table.addListener(SWT.MeasureItem, paintListener);
				table.addListener(SWT.EraseItem, paintListener);
				table.addListener(SWT.PaintItem, paintListener);

				table.addListener(SWT.Selection, new Listener() {
					public void handleEvent(Event event) {
						if (!copyClipboardButton.isEnabled()) {
							copyClipboardButton.setEnabled(true);
						}
						if (!discardButton.isEnabled()) {
							discardButton.setEnabled(true);
						}
					};
				});

				table.setBackground(display.getSystemColor(SWT.COLOR_BLACK));

				// fill table
				fillTable(table);

				// open table
				shell.pack();
				shell.setBounds(shell.getBounds().x, shell.getBounds().y, shell.getBounds().width + 100, 500);
				shell.open();
			}
		}
	}

	public void fillTable(Table table) {
		// fill table
		for (String path : paths) {
			TableItem item = new TableItem(table, SWT.NONE);
			item.setText(path);
			item.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
			Image[] itemImages = new Image[COLUMN_COUNT];
			item.setData(KEY_IMAGES, itemImages);
			for (int j = 0; j < COLUMN_COUNT; j++) {
				itemImages[j] = ImageUtils.getImage(path);
			}
		}
	}

	// Iteratively loop files and check if whitebackground
	private void loopAndProcess(File file) {
		if (file.isDirectory()) {
			for (File subFile : file.listFiles()) {
				loopAndProcess(subFile);
			}
		} else if (ImageUtils.isImageFile(file)) {
			Image image = ImageUtils.getImage(file.getAbsolutePath());
			if (image != null) {
				ImageData imageData = image.getImageData();
				if (imageData.width <= 200 && imageData.height <= 200 && hasWhiteBackground(imageData)) {
					// found
					paths.add(file.getAbsolutePath());
				}
				image.dispose();
			}
		}
	}

	// white pixel found in the border?
	private boolean hasWhiteBackground(ImageData imageData) {
		// top and bottom lines
		for (int x = 0; x < imageData.width; x++) {
			if (isWhite(imageData, x, 0)) {
				return true;
			}
			if (isWhite(imageData, x, imageData.height - 1)) {
				return true;
			}
		}
		// left and right lines
		for (int y = 1; y < imageData.height - 1; y++) {
			if (isWhite(imageData, 0, y)) {
				return true;
			}
			if (isWhite(imageData, imageData.width - 1, y)) {
				return true;
			}
		}
		// no white pixel found
		return false;
	}

	// Check if the pixel x,y of the image is white
	private boolean isWhite(ImageData imageData, int x, int y) {
		int alpha = imageData.getAlpha(x, y);
		int pixel = imageData.getPixel(x, y);
		// direct palette
		if (imageData.palette.isDirect) {
			if (alpha == 255) {
				RGB rgb = imageData.palette.getRGB(pixel);
				if (rgb.blue == 255 && rgb.green == 255 && rgb.blue == 255) {
					return true;
				}
			}
			// indexed palette
		} else {
			if (alpha == 255 && imageData.transparentPixel != pixel) {
				RGB rgb = imageData.getRGBs()[pixel];
				if (rgb.blue == 255 && rgb.green == 255 && rgb.blue == 255) {
					return true;
				}
			}
		}
		return false;
	}

}
