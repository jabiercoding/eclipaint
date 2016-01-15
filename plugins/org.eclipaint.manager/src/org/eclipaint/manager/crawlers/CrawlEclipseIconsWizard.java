package org.eclipaint.manager.crawlers;

import java.io.File;

import org.eclipaint.utils.WorkbenchUtils;
import org.eclipse.jface.wizard.Wizard;

/**
 * @author Jabier Martinez
 */
public class CrawlEclipseIconsWizard extends Wizard {

	CrawlEcipseIconsWizardPage crawlEclipseIconsWizardPage = null;

	public CrawlEclipseIconsWizard() {
		setWindowTitle("Crawl Eclipse Icons");
		setNeedsProgressMonitor(true);
		crawlEclipseIconsWizardPage = new CrawlEcipseIconsWizardPage("Crawl Eclipse Icons");
	}

	@Override
	public void addPages() {
		addPage(crawlEclipseIconsWizardPage);
	}

	@Override
	public boolean performFinish() {
		String srcDir = crawlEclipseIconsWizardPage.getSrcDir();
		String filterOption = crawlEclipseIconsWizardPage.getFilter();
		String[] filters = new String[] { filterOption };
		String destDir = crawlEclipseIconsWizardPage.getDestDir();
		String workspacePath = WorkbenchUtils.getWorkspacePath().toOSString();
		destDir = workspacePath + destDir + File.separatorChar + "icons";

		CrawlEclipseIconsJob job = new CrawlEclipseIconsJob(srcDir, destDir, filters);
		job.setUser(true);
		job.schedule();

		return true;
	}

}
