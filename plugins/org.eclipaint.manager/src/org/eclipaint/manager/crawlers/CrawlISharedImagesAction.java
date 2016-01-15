package org.eclipaint.manager.crawlers;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.eclipaint.manager.activator.Activator;
import org.eclipaint.utils.ImageUtils;
import org.eclipaint.utils.WorkbenchUtils;
import org.eclipaint.utils.ui.SaveAsContainerSelectionDialog;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

/**
 * @author Jabier Martinez
 */
public class CrawlISharedImagesAction extends Action {

	final int COLUMN_COUNT = 1;
	final int TEXT_MARGIN = 3;
	final String KEY_WIDTHS = "widths";
	final String KEY_IMAGES = "images";
	String selectedElement = "";

	Text iconNameText;
	Text iconImageDescriptorText;
	Text iconImageText;
	Button saveButton;

	public CrawlISharedImagesAction() {
		this.setImageDescriptor(Activator.getImageDescriptor("icons/crawlers/crawlEclipseIconsAction.png"));
		this.setText("Crawl Eclipse ISharedImages");
		this.setToolTipText("Crawl Eclipse ISharedImages");
	}

	public void run() {
		Display display = Display.getCurrent();
		Shell shell = new Shell(display);
		shell.setText("ISharedImages"); //$NON-NLS-1$
		selectedElement = "";

		GridLayout gridLayout = new GridLayout(1, false);
		shell.setLayout(gridLayout);

		Label label = new Label(shell, SWT.NONE);
		label.setText("Selected icon info:");

		iconNameText = new Text(shell, SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY);
		GridData gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gridData.grabExcessHorizontalSpace = true;
		iconNameText.setLayoutData(gridData);
		iconNameText.setText(selectedElement);

		iconImageDescriptorText = new Text(shell, SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY);
		iconImageDescriptorText.setLayoutData(gridData);

		iconImageText = new Text(shell, SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY);
		iconImageText.setLayoutData(gridData);

		saveButton = new Button(shell, SWT.PUSH);
		saveButton.setText("Save");
		saveButton.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_ETOOL_SAVE_EDIT));
		saveButton.setEnabled(false);

		Listener listener = new Listener() {
			public void handleEvent(Event event) {
				SaveAsContainerSelectionDialog dialog = new SaveAsContainerSelectionDialog(Display.getCurrent()
						.getActiveShell(), ResourcesPlugin.getWorkspace().getRoot(), false,
						"Select image container and name", selectedElement + ".png");
				if (dialog.open() == Dialog.OK) {
					IPath selectedContainer = (IPath) dialog.getResult()[0];
					IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
					IResource res = root.findMember(selectedContainer);
					String location = res.getLocation().append(dialog.getFileName()).toOSString();
					ImageUtils.saveImageToFile(PlatformUI.getWorkbench().getSharedImages().getImage(selectedElement)
							.getImageData(), location, SWT.IMAGE_PNG);
					WorkbenchUtils.refreshWorkspace(location);
				}
			}
		};
		saveButton.addListener(SWT.Selection, listener);

		Label labelList = new Label(shell, SWT.NONE);
		labelList.setText("Icons list:");

		Table table = new Table(shell, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		GridData gridDataTable = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_FILL);
		gridDataTable.grabExcessVerticalSpace = true;
		gridDataTable.grabExcessHorizontalSpace = true;
		table.setLayoutData(gridDataTable);

		final int itemHeight = table.getItemHeight();
		GC gc = new GC(table);
		FontMetrics metrics = gc.getFontMetrics();
		final int fontHeight = metrics.getHeight();
		gc.dispose();

		Listener paintListener = new Listener() {
			public void handleEvent(Event event) {

				switch (event.type) {
				case SWT.MeasureItem: {
					int column = event.index;
					TableItem item = (TableItem) event.item;
					Image[] images = (Image[]) item.getData(KEY_IMAGES);
					Image image = images[column];
					if (image == null) {
						/* don't change the native-calculated event.width */
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
				TableItem ti = (TableItem) event.item;
				selectedElement = ti.getText();
				if (!saveButton.isEnabled()) {
					saveButton.setEnabled(true);
				}
				iconNameText.setText(selectedElement);
				iconImageDescriptorText
						.setText("PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages." + selectedElement + ")"); //$NON-NLS-1$ //$NON-NLS-2$
				iconImageText
						.setText("PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages." + selectedElement + ")"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		});

		Field[] o = ISharedImages.class.getFields();
		for (Field f : o) {
			// public final static
			if (Modifier.isPublic(f.getModifiers()) && Modifier.isFinal(f.getModifiers())
					&& Modifier.isStatic(f.getModifiers())) {
				// ISharedImages does not use Deprecated as annotation but as
				// javadoc annotation so the next line will be always true, but
				// we will keep it
				if (f.getAnnotation(Deprecated.class) == null) {
					try {
						// ISharedImages f.getName() is the same as its String
						// value but we will get its value
						String fieldValue = f.get(ISharedImages.class).toString();
						// Check if deprecated
						if (!isDeprecatedInISharedImages(fieldValue)) {
							ImageDescriptor imageDescriptor = PlatformUI.getWorkbench().getSharedImages()
									.getImageDescriptor(fieldValue);
							// finally we check that the image exists
							if (imageDescriptor != null) {
								TableItem item = new TableItem(table, SWT.NONE);
								item.setText(fieldValue);
								Image[] itemImages = new Image[COLUMN_COUNT];
								item.setData(KEY_IMAGES, itemImages);
								for (int j = 0; j < COLUMN_COUNT; j++) {
									itemImages[j] = imageDescriptor.createImage();
								}
							}
						}
					} catch (IllegalArgumentException exception_p) {
						exception_p.printStackTrace();
					} catch (IllegalAccessException exception_p) {
						exception_p.printStackTrace();
					}
				}
			}
		}
		shell.pack();
		shell.setBounds(shell.getBounds().x, shell.getBounds().y, shell.getBounds().width + 100, 500);
		shell.open();
	}

	public static boolean isDeprecatedInISharedImages(String name) {
		for (String element : deprecatedList) {
			if (name.equals(element)) {
				return true;
			}
		}
		return false;
	}

	// Unfortunately the deprecated annotation is only in the javadoc, not as a
	// real annotation. So lets use at least a static list
	public static final String[] deprecatedList = { "IMG_OBJ_PROJECT", "IMG_OBJ_PROJECT_CLOSED", "IMG_OBJS_BKMRK_TSK",
			"IMG_OBJS_TASK_TSK", "IMG_OPEN_MARKER", "IMG_TOOL_BACK_HOVER", "IMG_TOOL_COPY_HOVER", "IMG_TOOL_CUT_HOVER",
			"IMG_TOOL_DELETE_HOVER", "IMG_TOOL_FORWARD_HOVER", "IMG_TOOL_NEW_WIZARD_HOVER", "IMG_TOOL_PASTE_HOVER",
			"IMG_TOOL_REDO_HOVER", "IMG_TOOL_UNDO_HOVER", "IMG_TOOL_UP_HOVER" };

}
