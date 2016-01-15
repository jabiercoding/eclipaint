package org.eclipaint.manager.crawlers;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipaint.manager.activator.Activator;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

public class CrawlWebAction extends Action {

	public CrawlWebAction() {
		this.setImageDescriptor(Activator.getImageDescriptor("icons/crawlers/crawlEclipseIconsAction.png"));
		this.setText("Crawl from Web (IconFinder)");
		this.setToolTipText("Crawl from Web (IconFinder)");
	}

	public void run() {
		try {
			PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser()
					.openURL(new URL("http://www.iconfinder.com/"));
		} catch (PartInitException e1) {
			MessageDialog.openError(Display.getCurrent().getActiveShell(), "Error", "Error opening the Browser");
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		}
	}
}
