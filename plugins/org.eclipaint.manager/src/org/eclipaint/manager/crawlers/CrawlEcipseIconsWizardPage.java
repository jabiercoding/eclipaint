package org.eclipaint.manager.crawlers;

import java.io.File;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.eclipaint.utils.WorkbenchUtils;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;

/**
 * 
 * @author Jabier Martinez
 */
public class CrawlEcipseIconsWizardPage extends WizardPage {
	private static final String ORG_ECLIPSE = "org.eclipse.*";
	String destDir = null;
	Text destDirText;
	Text srcDirText;
	Text filterText;

	protected CrawlEcipseIconsWizardPage(String pageName) {
		super(pageName);
	}

	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		container.setLayout(layout);
		layout.numColumns = 3;
		layout.verticalSpacing = 9;

		// SrcDir
		Label labelSrcDir = new Label(container, SWT.NULL);
		labelSrcDir.setText("Eclipse plugins folder:");

		srcDirText = new Text(container, SWT.BORDER | SWT.SINGLE);
		GridData gdsrcDir = new GridData(GridData.FILL_HORIZONTAL);
		srcDirText.setLayoutData(gdsrcDir);
		srcDirText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				dialogChanged();
			}
		});

		Button button = new Button(container, SWT.PUSH);
		button.setText("Browse...");
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleDirectoryBrowse();
			}
		});

		// filter
		Label labelRegex = new Label(container, SWT.NULL);
		labelRegex.setText("Filter plugins:");

		filterText = new Text(container, SWT.BORDER | SWT.SINGLE);
		GridData gdfilter = new GridData(GridData.FILL_HORIZONTAL);
		gdfilter.horizontalSpan = 2;
		filterText.setLayoutData(gdfilter);
		filterText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				dialogChanged();
			}
		});

		// output container
		Label labelDestDir = new Label(container, SWT.NULL);
		labelDestDir.setText("Crawled Icons output container:");

		destDirText = new Text(container, SWT.BORDER | SWT.SINGLE);
		GridData gdDestDir = new GridData(GridData.FILL_HORIZONTAL);
		destDirText.setLayoutData(gdDestDir);
		destDirText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				dialogChanged();
			}
		});

		Button buttondestDi = new Button(container, SWT.PUSH);
		buttondestDi.setText("Browse...");
		buttondestDi.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleWorkspaceContainerBrowse();
			}
		});

		initialize();
		dialogChanged();
		setControl(container);
	}

	private void handleWorkspaceContainerBrowse() {
		ContainerSelectionDialog dialog = new ContainerSelectionDialog(getShell(), ResourcesPlugin.getWorkspace()
				.getRoot(), false, "Select crawled icons output container");
		if (dialog.open() == ContainerSelectionDialog.OK) {
			Object[] result = dialog.getResult();
			if (result.length == 1) {
				destDirText.setText(((Path) result[0]).toString());
			}
		}
	}

	private void handleDirectoryBrowse() {
		DirectoryDialog dialog = new DirectoryDialog(getShell());
		String path = dialog.open();
		if (path != null) {
			srcDirText.setText(path);
		}
	}

	private void dialogChanged() {
		// Src dir checking
		if (getSrcDir().length() == 0) {
			updateStatus("Eclipse plugins folder must be defined");
			return;
		}

		File file = new File(getSrcDir());
		if (!file.exists()) {
			updateStatus("Eclipse plugins folder must exist");
			return;
		}

		if (!file.canRead()) {
			updateStatus("Eclipse plugins folder must be readable");
			return;
		}

		// filter
		if (getFilter().length() == 0) {
			updateStatus("Filter should be defined. Use * if you want to crawl all the plugins.");
			return;
		}
		if (!getFilter().equals(ORG_ECLIPSE)) {
			setMessage(
					"You modified the filter. Notice that org.eclipse.* assures you an EPL license for crawled icons.",
					WARNING);
		}

		try {
			String filter = this.getFilter().replaceAll("\\*", ".*").replaceAll("\\?", ".");
			Pattern.compile(filter);
		} catch (PatternSyntaxException e) {
			updateStatus("A valid regex expression must be introduced.");
			return;
		}

		// Dest dir checking
		IResource container = ResourcesPlugin.getWorkspace().getRoot().findMember(new Path(getDestDir()));

		if (getDestDir().length() == 0) {
			updateStatus("Crawled icons output container must be specified");
			return;
		}
		if (container == null || (container.getType() & (IResource.PROJECT | IResource.FOLDER)) == 0) {
			updateStatus("Crawled icons output container must exist");
			return;
		}
		if (!container.isAccessible()) {
			updateStatus("Crawled icons output container be writable");
			return;
		}

		updateStatus(null);
	}

	private void updateStatus(String message) {
		setErrorMessage(message);
		setPageComplete(message == null);
	}

	public String getSrcDir() {
		return srcDirText.getText();
	}

	public String getFilter() {
		return filterText.getText();
	}

	public String getDestDir() {
		return destDirText.getText();
	}

	private void initialize() {
		// Get plugins folder of this installation
		srcDirText.setText(WorkbenchUtils.getEclipseAbsolutePath() + File.separator + "plugins");
		filterText.setText(ORG_ECLIPSE);
		destDirText.setText("");
	}

}
