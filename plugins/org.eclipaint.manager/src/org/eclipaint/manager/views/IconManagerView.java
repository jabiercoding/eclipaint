package org.eclipaint.manager.views;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipaint.manager.activator.Activator;
import org.eclipaint.manager.crawlers.CrawlEclipseIconsAction;
import org.eclipaint.manager.crawlers.CrawlISharedImagesAction;
import org.eclipaint.manager.crawlers.CrawlWebAction;
import org.eclipaint.manager.crawlers.CrawlWhiteBackgroundIconsAction;
import org.eclipaint.utils.ImageUtils;
import org.eclipaint.utils.WorkbenchUtils;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceEffect;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetEffect;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;
import org.eclipse.ui.part.ResourceTransfer;
import org.eclipse.ui.part.ViewPart;

/**
 * Icon Manager View
 * 
 * @author Jabier Martinez
 */
public class IconManagerView extends ViewPart {

	public IconManagerView() {
		super();
		String defaultIconsPath = WorkbenchUtils.getFileAbsolutePathFromPlugin(Activator.getDefault(),
				"icons/manager/default");
		String baseDefaultPath = defaultIconsPath + "/base.png";
		String overlayDefaultPath = defaultIconsPath + "/overlay.png";
		baseIcons = new String[] { baseDefaultPath };
		overlayIcons = new String[] { overlayDefaultPath };
	}

	public static final String ID = "org.eclipaint.manager.views.IconManager";

	private TreeViewer viewer;
	private Action selectOverlayIconAction;
	private Action selectBaseIconAction;
	private Action saveAction;
	private Action overlayNextIconAction;
	private Action overlayPreviousIconAction;
	private Action baseNextIconAction;
	private Action basePreviousIconAction;

	private String[] baseIcons;
	private String[] overlayIcons;
	private int baseCurrentIndex = 0;
	private int overlayCurrentIndex = 0;

	private int outputFormat = SWT.IMAGE_PNG;

	/**
	 * Content Provider
	 */
	class IconsViewContentProvider implements IStructuredContentProvider, ITreeContentProvider {

		private IconCategory invisibleRoot;

		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		}

		public Object[] getElements(Object parent) {
			if (parent.equals(getViewSite())) {
				if (invisibleRoot == null)
					initialize();
				return getChildren(invisibleRoot);
			}
			return getChildren(parent);
		}

		public Object getParent(Object child) {
			if (child instanceof Icon) {
				return ((Icon) child).getParent();
			}
			return null;
		}

		public Object[] getChildren(Object parent) {
			if (parent instanceof IconCategory) {
				return ((IconCategory) parent).getIcons();
			}
			return new Object[0];
		}

		public boolean hasChildren(Object parent) {
			if (parent instanceof IconCategory)
				return ((IconCategory) parent).hasIcons();
			return false;
		}

		private void initialize() {
			IconCategory baseCategory = new IconCategory(IconCategory.BASE, IconCategory.BASE);
			Icon base = new Icon(Icon.BASE_ICON, "base");
			baseCategory.addIcon(base);

			IconCategory centeredOverlayCategory = new IconCategory(IconCategory.CENTERED_OVERLAY.replaceAll(" ", "_"),
					IconCategory.CENTERED_OVERLAY);
			Icon iconCenteredOverlay = new Icon(Icon.CENTERED_OVERLAY_ICON, "base_overlay");
			centeredOverlayCategory.addIcon(iconCenteredOverlay);

			IconCategory cornersOverlayCategory = new IconCategory(IconCategory.CORNERS_OVERLAY,
					IconCategory.CORNERS_OVERLAY);
			Icon iconCorners1Overlay = new Icon(Icon.TOP_LEFT_CORNER_OVERLAY_ICON, "base_top_left_corner_overlay");
			Icon iconCorners2Overlay = new Icon(Icon.TOP_RIGHT_CORNER_OVERLAY_ICON, "base_top_right_corner_overlay");
			Icon iconCorners3Overlay = new Icon(Icon.BOTTOM_RIGHT_CORNER_OVERLAY_ICON,
					"base_bottom_right_corner_overlay");
			Icon iconCorners4Overlay = new Icon(Icon.BOTTOM_LEFT_CORNER_OVERLAY_ICON, "base_bottom_left_corner_overlay");
			cornersOverlayCategory.addIcon(iconCorners1Overlay);
			cornersOverlayCategory.addIcon(iconCorners2Overlay);
			cornersOverlayCategory.addIcon(iconCorners3Overlay);
			cornersOverlayCategory.addIcon(iconCorners4Overlay);

			IconCategory sidesOverlayCategory = new IconCategory(IconCategory.SIDES_OVERLAY, IconCategory.SIDES_OVERLAY);
			Icon iconSide1Overlay = new Icon(Icon.TOP_SIDE_OVERLAY_ICON, "base_top_side_overlay");
			Icon iconSide2Overlay = new Icon(Icon.RIGHT_SIDE_OVERLAY_ICON, "base_right_side_overlay");
			Icon iconSide3Overlay = new Icon(Icon.BOTTOM_SIDE_OVERLAY_ICON, "base_bottom_side_overlay");
			Icon iconSide4Overlay = new Icon(Icon.LEFT_SIDE_OVERLAY_ICON, "base_left_side_overlay");
			sidesOverlayCategory.addIcon(iconSide1Overlay);
			sidesOverlayCategory.addIcon(iconSide2Overlay);
			sidesOverlayCategory.addIcon(iconSide3Overlay);
			sidesOverlayCategory.addIcon(iconSide4Overlay);

			IconCategory flipRotateCategory = new IconCategory(IconCategory.FLIP_ROTATE, IconCategory.FLIP_ROTATE);
			Icon iconRotateLeft = new Icon(Icon.ROTATE_LEFT_BASE_ICON, "base_left_rotate");
			Icon iconRotateRight = new Icon(Icon.ROTATE_RIGHT_BASE_ICON, "base_right_rotate");
			Icon iconRotate180 = new Icon(Icon.ROTATE_180_BASE_ICON, "base_180_rotate");
			Icon iconFlipHorizontal = new Icon(Icon.FLIP_HORIZONTAL_BASE_ICON, "base_flip_horizontal");
			Icon iconFlipVertical = new Icon(Icon.FLIP_VERTICAL_BASE_ICON, "base_flip_vertical");
			flipRotateCategory.addIcon(iconRotateLeft);
			flipRotateCategory.addIcon(iconRotateRight);
			flipRotateCategory.addIcon(iconRotate180);
			flipRotateCategory.addIcon(iconFlipHorizontal);
			flipRotateCategory.addIcon(iconFlipVertical);

			IconCategory colorCategory = new IconCategory(IconCategory.COLOR, IconCategory.COLOR);
			Icon iconDisable = new Icon(Icon.COLOR_DISABLED, "base_disabled");
			Icon iconGray = new Icon(Icon.COLOR_GRAY, "base_gray");
			Icon iconColor8 = new Icon(Icon.COLOR_RGB + "255,255,255", "base_white");
			Icon iconColor7 = new Icon(Icon.COLOR_RGB + "255,255,0", "base_yellow");
			Icon iconColor6 = new Icon(Icon.COLOR_RGB + "255,0,255", "base_pink");
			Icon iconColor5 = new Icon(Icon.COLOR_RGB + "255,0,0", "base_red");
			Icon iconColor4 = new Icon(Icon.COLOR_RGB + "0,255,255", "base_cyan");
			Icon iconColor3 = new Icon(Icon.COLOR_RGB + "0,255,0", "base_green");
			Icon iconColor2 = new Icon(Icon.COLOR_RGB + "0,0,255", "base_blue");
			Icon iconColor1 = new Icon(Icon.COLOR_RGB + "0,0,0", "base_black");

			colorCategory.addIcon(iconDisable);
			colorCategory.addIcon(iconGray);
			colorCategory.addIcon(iconColor8);
			colorCategory.addIcon(iconColor7);
			colorCategory.addIcon(iconColor6);
			colorCategory.addIcon(iconColor5);
			colorCategory.addIcon(iconColor4);
			colorCategory.addIcon(iconColor3);
			colorCategory.addIcon(iconColor2);
			colorCategory.addIcon(iconColor1);

			IconCategory scaleCategory = new IconCategory(IconCategory.SCALE, IconCategory.SCALE);
			Icon iconScale16x16 = new Icon(Icon.SCALE_16X16, "base_16x16");
			Icon iconScale200 = new Icon(Icon.SCALE_200, "base_double_size");
			Icon iconScale75 = new Icon(Icon.SCALE_75, "base_0.75_size");
			Icon iconScale60 = new Icon(Icon.SCALE_60, "base_0.6_size");
			Icon iconScale50 = new Icon(Icon.SCALE_50, "base_half_size");
			Icon iconScale40 = new Icon(Icon.SCALE_40, "base_0.4_size");

			scaleCategory.addIcon(iconScale16x16);
			scaleCategory.addIcon(iconScale200);
			scaleCategory.addIcon(iconScale75);
			scaleCategory.addIcon(iconScale60);
			scaleCategory.addIcon(iconScale50);
			scaleCategory.addIcon(iconScale40);

			// Categories order
			invisibleRoot = new IconCategory("", "");
			invisibleRoot.addIcon(baseCategory);
			invisibleRoot.addIcon(centeredOverlayCategory);
			invisibleRoot.addIcon(cornersOverlayCategory);
			invisibleRoot.addIcon(sidesOverlayCategory);
			invisibleRoot.addIcon(colorCategory);
			invisibleRoot.addIcon(flipRotateCategory);
			invisibleRoot.addIcon(scaleCategory);
		}

		public void dispose() {
		}
	}

	/**
	 * Label Provider
	 */
	class IconsViewLabelProvider extends LabelProvider {

		// Get Image
		public Image getImage(Object obj) {
			if (obj instanceof IconCategory) {
				return ((IconCategory) obj).getImage();
			} else { // It is an Icon
				// Process the image
				Image image = ((Icon) obj).processImage(overlayIcons[overlayCurrentIndex], baseIcons[baseCurrentIndex]);
				return image;
			}
		}

		// Get Text
		public String getText(Object obj) {
			// Show category name
			if (obj instanceof IconCategory) {
				return super.getText(obj);
			}
			return computeIconName((Icon) obj, overlayCurrentIndex, baseCurrentIndex);
		}
	}

	public void createPartControl(Composite parent) {
		viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		viewer.setContentProvider(new IconsViewContentProvider());
		viewer.setLabelProvider(new IconsViewLabelProvider());
		viewer.setInput(getViewSite());
		createActions();
		createContextMenu();
		contributeToActionBars();
		viewer.expandAll();
		addDropSupport();
		addDragSupport();
	}

	private void addDragSupport() {
		DragSourceEffect drag = new DragSourceEffect(viewer.getControl()) {

			File tempFolder;
			String[] iconAbsPaths;

			@Override
			public void dragStart(DragSourceEvent event) {
				IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
				if (selection != null && !selection.isEmpty()) {
					// Get or create temporary folder
					String tempDir = System.getProperty("java.io.tmpdir");
					tempFolder = new File(tempDir + "EclipseIconsEditorTemp" + System.currentTimeMillis());
					if (!tempFolder.exists()) {
						tempFolder.mkdir();
						// Delete on exit
						tempFolder.deleteOnExit();
					}
					@SuppressWarnings("unchecked")
					List<Icon> selectionList = selection.toList();
					for (Icon selectedElement : selectionList) {
						// Perform the Save for each selected element
						save(selectedElement, tempFolder.getAbsolutePath());
					}
					// create the paths needed for event.data
					iconAbsPaths = new String[tempFolder.listFiles().length];
					for (int i = 0; i < iconAbsPaths.length; i++) {
						iconAbsPaths[i] = tempFolder.listFiles()[i].getAbsolutePath();
						// mark also all the files as delete on exit
						tempFolder.listFiles()[i].deleteOnExit();
					}
				}
			}

			@Override
			public void dragSetData(DragSourceEvent event) {
				if (FileTransfer.getInstance().isSupportedType(event.dataType)) {
					event.data = iconAbsPaths;
				}
			}
		};
		int operations = DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_LINK;
		Transfer[] types = new Transfer[] { FileTransfer.getInstance() };
		DragSource source = new DragSource(viewer.getControl(), operations);
		source.setTransfer(types);
		source.addDragListener(drag);
	}

	private void addDropSupport() {

		// Listener for menuItems
		// selected will be null if the user doesn't select this option
		// in the popup menu
		class PopUpMenuSelectionListener implements Listener {
			private String selected;

			public void handleEvent(Event e) {
				selected = e.widget.toString();
			}

			public String getSelected() {
				return selected;
			}
		}

		// Create the dropTargetEffect
		DropTargetEffect drop = new DropTargetEffect(viewer.getControl()) {

			@Override
			public void drop(DropTargetEvent event) {
				// If the it is supported by File or Resource transfer
				if (FileTransfer.getInstance().isSupportedType(event.currentDataType)
						|| ResourceTransfer.getInstance().isSupportedType(event.currentDataType)) {
					// We change this because we don't want the resource to be
					// removed as it is a move operation.
					event.detail = DND.DROP_NONE;

					// We create the filePaths depending on the transfer type
					List<String> filePaths = new ArrayList<String>();

					// Resource Transfer
					if (ResourceTransfer.getInstance().isSupportedType(event.currentDataType)) {
						// Get the dropped resources
						IResource[] resources = (IResource[]) event.data;

						if (resources != null) {
							// Get paths
							for (IResource resource : resources) {
								filePaths.add(resource.getLocation().toOSString());
							}
						}

						// File Transfer
					} else if (FileTransfer.getInstance().isSupportedType(event.currentDataType)) {
						// Get the file abs paths
						String[] files = (String[]) event.data;
						if (files != null) {
							for (String file : files) {
								filePaths.add(file);
							}
						}
					}

					// Check if they are image files
					List<String> imageFiles = new ArrayList<String>();
					for (String filePath : filePaths) {
						if (ImageUtils.isImageFile(filePath)) {
							imageFiles.add(filePath);
						}
					}

					// Check if images were selected
					if (!imageFiles.isEmpty()) {
						Menu menu = new Menu(viewer.getControl().getShell(), SWT.POP_UP);
						// These listeners will be used to know which was the
						// selected option
						PopUpMenuSelectionListener baseMenuListener = new PopUpMenuSelectionListener();
						PopUpMenuSelectionListener overlayMenuListener = new PopUpMenuSelectionListener();

						// create menuItems
						MenuItem overlayItem = new MenuItem(menu, SWT.PUSH);
						overlayItem.setText("Overlay");
						overlayItem.setImage(Activator.getImageDescriptor("icons/manager/selectOverlayIcon.png")
								.createImage());
						MenuItem baseItem = new MenuItem(menu, SWT.PUSH);
						baseItem.setText("Base");
						baseItem.setImage(Activator.getImageDescriptor("icons/manager/selectBaseIcon.png")
								.createImage());
						// add listeners
						baseItem.addListener(SWT.Selection, baseMenuListener);
						overlayItem.addListener(SWT.Selection, overlayMenuListener);
						// show the menu
						menu.setLocation(event.x, event.y);
						menu.setVisible(true);
						// wait for the user to select
						while (!menu.isDisposed() && menu.isVisible()) {
							if (!Display.getCurrent().readAndDispatch())
								Display.getCurrent().sleep();
						}

						// update view based on selection
						if (baseMenuListener.getSelected() != null) {
							baseIcons = imageFiles.toArray(baseIcons);
							baseCurrentIndex = 0;
							viewer.refresh();
						} else if (overlayMenuListener.getSelected() != null) {
							overlayIcons = imageFiles.toArray(overlayIcons);
							overlayCurrentIndex = 0;
							viewer.refresh();
						}
						// dispose
						menu.dispose();
					}
				}
			}
		};

		// Add the drop listener to the viewer
		int operations = DND.DROP_MOVE;
		Transfer[] types = new Transfer[] { ResourceTransfer.getInstance(), FileTransfer.getInstance() };
		DropTarget target = new DropTarget(viewer.getControl(), operations);
		target.setTransfer(types);
		target.addDropListener(drop);
	}

	private void createContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				IconManagerView.this.fillContextMenu(manager);
			}
		});
		Menu menu = menuMgr.createContextMenu(viewer.getControl());
		viewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, viewer);
	}

	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		fillLocalToolBar(bars.getToolBarManager());
	}

	private void fillContextMenu(IMenuManager manager) {
		manager.add(saveAction);
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}

	private void fillLocalToolBar(IToolBarManager manager) {
		manager.add(selectOverlayIconAction);
		manager.add(selectBaseIconAction);
		manager.add(new Separator());
		manager.add(overlayPreviousIconAction);
		manager.add(overlayNextIconAction);
		manager.add(basePreviousIconAction);
		manager.add(baseNextIconAction);
		manager.add(new Separator());
		manager.add(saveAction);

		Action act = new Action("Crawl/Reuse", SWT.DROP_DOWN) {
		};
		act.setImageDescriptor(Activator.getImageDescriptor("icons/crawlers/crawlEclipseIconsAction.png"));
		act.setMenuCreator(new CrawlMenuCreator() {
		});
		manager.add(new Separator());
		manager.add(act);

	}

	class CrawlMenuCreator implements IMenuCreator {

		private MenuManager pullDownMenuManager;

		private void createDropDownMenuMgr() {
			if (pullDownMenuManager == null) {
				pullDownMenuManager = new MenuManager();
			}
		}

		@Override
		public void dispose() {
			if (pullDownMenuManager != null) {
				pullDownMenuManager.dispose();
				pullDownMenuManager = null;
			}
		}

		@Override
		public Menu getMenu(Control parent) {
			createDropDownMenuMgr();
			Menu menu = new Menu(parent);
			ActionContributionItem item = new ActionContributionItem(new CrawlEclipseIconsAction());
			item.fill(menu, -1);
			ActionContributionItem item2 = new ActionContributionItem(new CrawlISharedImagesAction());
			item2.fill(menu, -1);
			ActionContributionItem item3 = new ActionContributionItem(new CrawlWebAction());
			item3.fill(menu, -1);
			ActionContributionItem item4 = new ActionContributionItem(new CrawlWhiteBackgroundIconsAction());
			item4.fill(menu, -1);
			return menu;
		}

		@Override
		public Menu getMenu(Menu parent) {
			// No use
			return null;
		}
	}

	/**
	 * Opens a external files selector for images
	 * 
	 * @param type
	 *            : should be base or overlay
	 * @return the absolutepath of the selected files
	 */
	private String[] selectIcons(String type) {
		FileDialog fd = new FileDialog(Display.getCurrent().getActiveShell(), SWT.OPEN | SWT.MULTI);

		// customize by type
		fd.setText("Select " + type + " Image");
		if (type.equalsIgnoreCase("base")) {
			fd.setFilterPath(WorkbenchUtils.getFileAbsolutePathFromPlugin(Activator.getDefault(), "gallery/base"));
		} else if (type.equalsIgnoreCase("overlay")) {
			fd.setFilterPath(WorkbenchUtils.getFileAbsolutePathFromPlugin(Activator.getDefault(), "gallery/overlay"));
		}

		// Filter extensions by type but allows also *.* because there could be
		// different
		// extensions (gif and png) in the same folder.
		String[] filterExt = new String[ImageUtils.IMAGE_EXTENSIONS.length + 1];
		System.arraycopy(ImageUtils.IMAGE_EXTENSIONS, 0, filterExt, 0, ImageUtils.IMAGE_EXTENSIONS.length);
		filterExt[ImageUtils.IMAGE_EXTENSIONS.length] = "*.*";
		fd.setFilterExtensions(filterExt);

		// Open it
		String selection = fd.open();
		if (selection != null) {
			// Something was selected. The user doesn't press Cancel
			String[] files = fd.getFileNames();
			// now we have the relative paths, make them absolute
			String filterPath = fd.getFilterPath();
			// we add the separatorChar just in case
			if (filterPath.charAt(filterPath.length() - 1) != File.separatorChar) {
				filterPath = filterPath + File.separatorChar;
			}
			for (int i = 0, n = files.length; i < n; i++) {
				files[i] = filterPath + files[i];
			}
			return files;
		}
		// Selection cancelled
		return null;
	}

	/**
	 * Create Actions
	 */
	private void createActions() {
		// Overlay Next Action
		overlayNextIconAction = new Action() {
			public void run() {
				// check if it is the last
				if (overlayCurrentIndex < overlayIcons.length - 1) {
					overlayCurrentIndex++;
					viewer.refresh();
				}
			}
		};
		overlayNextIconAction.setText("Next Overlay");
		overlayNextIconAction.setToolTipText("Next Overlay");
		overlayNextIconAction.setImageDescriptor(Activator.getImageDescriptor("icons/manager/goNextOverlay.png"));

		// Overlay Previous Action
		overlayPreviousIconAction = new Action() {
			public void run() {
				// check if it is the last
				if (overlayCurrentIndex != 0) {
					overlayCurrentIndex--;
					viewer.refresh();
				}
			}
		};
		overlayPreviousIconAction.setText("Previous Overlay");
		overlayPreviousIconAction.setToolTipText("Previous Overlay");
		overlayPreviousIconAction.setImageDescriptor(Activator
				.getImageDescriptor("icons/manager/goPreviousOverlay.png"));

		// Base Next Action
		baseNextIconAction = new Action() {
			public void run() {
				// check if it is the last
				if (baseCurrentIndex < baseIcons.length - 1) {
					baseCurrentIndex++;
					viewer.refresh();
				}
			}
		};
		baseNextIconAction.setText("Next Base");
		baseNextIconAction.setToolTipText("Next Base");
		baseNextIconAction.setImageDescriptor(Activator.getImageDescriptor("icons/manager/goNextBase.png"));

		// Base Previous Action
		basePreviousIconAction = new Action() {
			public void run() {
				// check if it is the last
				if (baseCurrentIndex != 0) {
					baseCurrentIndex--;
					viewer.refresh();
				}
			}
		};
		basePreviousIconAction.setText("Previous Base");
		basePreviousIconAction.setToolTipText("Previous Base");
		basePreviousIconAction.setImageDescriptor(Activator.getImageDescriptor("icons/manager/goPreviousBase.png"));

		// Select Overlay
		selectOverlayIconAction = new Action() {
			public void run() {
				String[] selected = selectIcons("Overlay");
				if (selected != null) {
					overlayIcons = selected;
					overlayCurrentIndex = 0;
					viewer.refresh();
				}
			}
		};
		selectOverlayIconAction.setText("Select Overlay Icon");
		selectOverlayIconAction.setToolTipText("Select Overlay Icon");
		selectOverlayIconAction.setImageDescriptor(Activator.getImageDescriptor("icons/manager/selectOverlayIcon.png"));

		// Select Base
		selectBaseIconAction = new Action() {
			public void run() {
				String[] selected = selectIcons("Base");
				if (selected != null) {
					baseIcons = selected;
					baseCurrentIndex = 0;
					viewer.refresh();
				}
			}
		};
		selectBaseIconAction.setText("Select Base Icon");
		selectBaseIconAction.setToolTipText("Select Base Icon");
		selectBaseIconAction.setImageDescriptor(Activator.getImageDescriptor("icons/manager/selectBaseIcon.png"));

		/**
		 * Save Action
		 */
		class SaveAction extends Action {
			public void run() {
				IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
				// No selection
				if (selection == null || selection.isEmpty()) {
					MessageDialog.openInformation(viewer.getControl().getShell(), "Eclipse Icons Editor",
							"No icons were selected");
				} else {
					// Select output folder
					String path;
					ContainerSelectionDialog dialog = new ContainerSelectionDialog(viewer.getControl().getShell(),
							ResourcesPlugin.getWorkspace().getRoot(), false, "Select output folder");
					if (dialog.open() == ContainerSelectionDialog.OK) {
						// Dialog OK
						Object[] result = dialog.getResult();
						path = (((Path) result[0]).toString());
						String workspacePath = WorkbenchUtils.getWorkspacePath().toOSString();
						path = workspacePath + path;
						List<?> selectionList = selection.toList();
						for (Object selectedElement : selectionList) {
							// Perform the Save for each selected element
							if (selectedElement instanceof Icon) {
								save((Icon) selectedElement, path);
							}
						}
						// Refresh workspace
						WorkbenchUtils.refreshWorkspace(path);
					}
				}
			}
		}

		// Save
		saveAction = new SaveAction();
		saveAction.setText("Save");
		saveAction.setToolTipText("Save");
		saveAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages()
				.getImageDescriptor(ISharedImages.IMG_ETOOL_SAVE_EDIT));

	}

	public void setFocus() {
		viewer.getControl().setFocus();
	}

	/**
	 * Save icons
	 * 
	 * @param icon
	 *            A selected element in the tree
	 * @param outputPath
	 *            The containing folder absolute path
	 */
	private void save(Icon icon, String outputPath) {
		if (icon instanceof IconCategory) {
			for (Icon child : ((IconCategory) icon).getIcons()) {
				save(child, outputPath);
			}
		} else {
			try {
				// Loop through all the combinations and save them
				for (int indexOverlay = 0; indexOverlay < overlayIcons.length; indexOverlay++) {
					for (int indexBase = 0; indexBase < baseIcons.length; indexBase++) {
						// Create new icon absolute path
						String newIconAbsPath = outputPath + File.separator
								+ computeIconName(icon, indexOverlay, indexBase);
						Image image = icon.processImage(overlayIcons[indexOverlay], baseIcons[indexBase]);
						newIconAbsPath = newIconAbsPath + "." + ImageUtils.getExtension(outputFormat);
						// Save it
						ImageUtils.saveImageToFile(image.getImageData(), newIconAbsPath, outputFormat);
					}
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	/**
	 * Given an absolute Path it returns the file name removing the extension
	 * 
	 * @param absolute
	 *            path string
	 * @return file name without extension
	 */
	private String getFileNameFromAbsolutePath(String string) {
		File file = new File(string);
		String name = file.getName();
		if (name != null && name.contains(".")) {
			return name.substring(0, name.lastIndexOf("."));
		} // else
		return name;
	}

	private String computeIconName(Icon icon, int overlayIndex, int baseIndex) {
		String iconName = icon.getName();
		iconName = iconName.replaceAll("base", getFileNameFromAbsolutePath(baseIcons[baseIndex]));
		iconName = iconName.replaceAll("overlay", getFileNameFromAbsolutePath(overlayIcons[overlayIndex]));
		return iconName;
	}
}