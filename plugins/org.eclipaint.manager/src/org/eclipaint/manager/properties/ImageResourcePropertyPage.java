package org.eclipaint.manager.properties;

import org.eclipaint.utils.ImageUtils;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PropertyPage;

/**
 * @author Jabier Martinez
 */
public class ImageResourcePropertyPage extends PropertyPage {

	ImageData imageData;

	public ImageResourcePropertyPage() {
		super();
	}

	private void addErrorSection(Composite parent) {
		Composite composite = createDefaultComposite(parent);
		Label widthLabel = new Label(composite, SWT.NONE);
		widthLabel.setText("Error loading image or the resource is not an image.");
	}

	private void addDimensionsSection(Composite parent) {
		Composite composite = createDefaultComposite(parent);

		addText(composite, "Width:", String.valueOf(imageData.width));
		addText(composite, "Height:", String.valueOf(imageData.height));
		addText(composite, "Bits/pixel:", String.valueOf(imageData.depth));

		String paletteType = "";
		if (imageData.palette.isDirect) {
			paletteType = "Direct color reference";
		} else {
			paletteType = "Defined palette";
		}
		addText(composite, "Palette:", paletteType);

		String transparency = "";
		switch (imageData.getTransparencyType()) {
		case (SWT.TRANSPARENCY_NONE):
			transparency = "No";
			break;
		case (SWT.TRANSPARENCY_ALPHA):
			transparency = "Alpha information";
			break;
		case (SWT.TRANSPARENCY_PIXEL):

			transparency = "Palette pixel (" + imageData.transparentPixel + ")";
			break;
		case (SWT.TRANSPARENCY_MASK):
			transparency = "Mask";
			break;
		}
		addText(composite, "Transparency:", transparency);

	}

	private void addSeparator(Composite parent) {
		Label separator = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
		GridData gridData = new GridData();
		gridData.horizontalAlignment = GridData.FILL;
		gridData.grabExcessHorizontalSpace = true;
		separator.setLayoutData(gridData);
	}

	/**
	 * @see PreferencePage#createContents(Composite)
	 */
	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		composite.setLayout(layout);
		GridData data = new GridData(GridData.FILL);
		data.grabExcessHorizontalSpace = true;
		composite.setLayoutData(data);

		Image image = ImageUtils.getImageFromResource((IResource) getElement());

		// Not a real image
		if (image == null) {
			addErrorSection(composite);
			return composite;
		}
		imageData = image.getImageData();
		image.dispose();

		addDimensionsSection(composite);
		addSeparator(composite);

		if (!imageData.palette.isDirect) {
			addPaletteSection(composite);
		}
		return composite;
	}

	private void addPaletteSection(Composite parent) {
		PaletteCanvas paletteCanvas = new PaletteCanvas(parent, imageData);
		GridData gridData = new GridData();
		gridData.horizontalAlignment = GridData.FILL;
		gridData.verticalAlignment = GridData.FILL;
		gridData.heightHint = PaletteCanvas.SIZE;
		gridData.grabExcessHorizontalSpace = true;
		paletteCanvas.setLayoutData(gridData);
	}

	private Composite createDefaultComposite(Composite parent) {
		Composite composite = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		composite.setLayout(layout);

		GridData data = new GridData();
		data.verticalAlignment = GridData.FILL;
		data.horizontalAlignment = GridData.FILL;
		composite.setLayoutData(data);

		return composite;
	}

	private void addText(Composite composite, String title, String value) {
		Label label = new Label(composite, SWT.NONE);
		label.setText(title);
		Text valueText = new Text(composite, SWT.WRAP | SWT.READ_ONLY);
		valueText.setText(value);
	}

}