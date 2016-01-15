/*
 * Copyright (c) 2011
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.eclipaint.manager.crawlers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipaint.utils.WorkbenchUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

/**
 * Scan for and extract icons from Eclipse plugins.
 * 
 * @author dcarew
 * @author Jabier Martinez
 */
public class CrawlEclipseIconsJob extends Job {

	File srcDir, destDir;
	String[] filters;
	private static int pluginCount;
	private static int iconCount;

	public CrawlEclipseIconsJob(String srcDirPath, String destDirPath, String[] filters) {
		super("Crawling Eclipse Icons at " + srcDirPath);

		srcDir = new File(srcDirPath);
		destDir = new File(destDirPath);
		destDir.mkdirs();

		// build wildcards and asterisk pattern
		for (int i = 0; i < filters.length; i++) {
			filters[i] = filters[i].replaceAll("\\*", ".*").replaceAll("\\?", ".");
		}
		this.filters = filters;
	}

	private static File createIndex(File dir) throws IOException {
		// find all the images
		List<IconInfo> icons = new ArrayList<IconInfo>();

		collectIconInfo(dir, dir, icons);

		// sort and organize them
		Collections.sort(icons);

		// write out index.html
		return createIndexFile(dir, icons);
	}

	private static void collectIconInfo(File destDir, File dir, List<IconInfo> icons) {
		for (File child : dir.listFiles()) {
			if (child.isDirectory()) {
				collectIconInfo(destDir, child, icons);
			} else if (isImageName(child.getName())) {
				icons.add(new IconInfo(destDir, child));
			}
		}
	}

	private static File createIndexFile(File dir, List<IconInfo> icons) throws IOException {
		File indexFile = new File(dir.getParentFile(), "index.html");

		PrintWriter writer = new PrintWriter(indexFile);

		writer.println("<html>");
		writer.println("<style type=\"text/css\">");
		writer.println(".rounded { border-radius: 6px; background: #DDD; padding: 0px 0px 0px 6px; margin: 16px 0px 2px 0px; }");
		writer.println(".title { font-weight: bold; }");
		writer.println(".subtitle { font-weight: normal; padding: 0px 6px 0px 0px; }");
		writer.println(".content { margin: 0px 0px 0px 20px; }");
		writer.println(".footer { border-radius: 6px; background: #DDD; padding: 2px 2px 2px 6px; }");
		writer.println("</style>");
		writer.println("<body>");

		writer.println("<h3>Standard Eclipse icons</h3>");
		writer.println("These icons were pulled from an Eclipse distribution.");
		writer.println(iconCount + " icons from " + pluginCount + " plugins.");

		writeIcons("16x16 Icons", writer, get16x16Icons(icons), 3);
		writeIcons("Overlay Icons", writer, getOverlayIcons(icons), 3);
		writeIcons("Wizard Images", writer, getWizardIcons(icons), 3);

		writer.println("<br/><div class=\"footer\">Generated " + DateFormat.getDateInstance().format(new Date())
				+ "</div>");

		writer.println("</body></html");

		writer.close();

		return indexFile;
	}

	private static List<IconInfo> getOverlayIcons(List<IconInfo> icons) {
		List<IconInfo> results = new ArrayList<IconInfo>();

		for (IconInfo icon : icons) {
			if (icon.isOverlayIcon()) {
				results.add(icon);
			}
		}

		return results;
	}

	private static List<IconInfo> get16x16Icons(List<IconInfo> icons) {
		List<IconInfo> results = new ArrayList<IconInfo>();

		for (IconInfo icon : icons) {
			if (icon.is16x16Icon()) {
				results.add(icon);
			}
		}

		return results;
	}

	private static List<IconInfo> getWizardIcons(List<IconInfo> icons) {
		List<IconInfo> results = new ArrayList<IconInfo>();

		for (IconInfo icon : icons) {
			if (icon.isWizardIcon()) {
				results.add(icon);
			}
		}

		return results;
	}

	private static void writeIcons(String title, PrintWriter writer, List<IconInfo> icons, int colCount) {
		writer.println("<div class=\"rounded\"><table width=\"100%\"><tr><td class=\"title\">" + title);
		writer.println("</td><td align=\"right\" class=\"subtitle\">" + NumberFormat.getInstance().format(icons.size())
				+ " icons");
		writer.println("</td></tr></table></div>");

		writer.println("<table class=\"content\">");
		writer.println("<tr>");

		int count = 0;

		for (IconInfo icon : icons) {
			if (0 == (count % colCount)) {
				writer.println("</tr>");
				writer.println("<tr>");
			}

			writer.print("  <td><a href=\"" + icon.getPath() + "\">");
			writer.print("<img src=\"icons/" + icon.getPath() + "\" width=\"" + icon.getWidth() + "\" height=\""
					+ icon.getHeight() + "\">");
			writer.println("</a></td><td>" + icon.getName() + "</td>");

			count++;
		}

		writer.println("</tr>");
		writer.println("</table>");
	}

	private void handleDirectory(File directory) {
		if (!shouldHandlePlugin(directory.getName())) {
			return;
		}

		pluginCount++;

		File iconDir = new File(directory, "icons");

		if (iconDir.exists() && iconDir.canRead()) {
			copyImages(iconDir, destDir);
		}
	}

	private void handleJar(File jarFile) {
		if (!shouldHandlePlugin(jarFile.getName())) {
			return;
		}

		pluginCount++;

		ZipFile zipFile = null;
		try {
			zipFile = new ZipFile(jarFile);
			Enumeration<? extends ZipEntry> enumeration = zipFile.entries();

			while (enumeration.hasMoreElements()) {
				ZipEntry entry = enumeration.nextElement();

				if (entry.isDirectory()) {
					continue;
				}

				String name = entry.getName();

				if (!name.startsWith("icons/")) {
					continue;
				}

				name = name.substring("icons/".length());

				if (isImageName(name)) {
					copyFile(name, zipFile.getInputStream(entry), destDir);
				}
			}

			if (zipFile != null) {
				zipFile.close();
			}
		} catch (IOException ioe) {
			// ignore - unable to read jarFile
		}

	}

	private static void copyFile(String name, InputStream in, File destDir) {
		iconCount++;

		File destFile = new File(destDir, name);

		File parentDir = destFile.getParentFile();

		if (!parentDir.exists()) {
			parentDir.mkdirs();
		}

		OutputStream out = null;

		try {
			out = new FileOutputStream(destFile);

			copy(in, out);
		} catch (IOException ioe) {
			// ignore
		} finally {
			if (out != null) {
				close(out);
			}
		}

		deleteIfNonStandard(destFile);
	}

	private static void deleteIfNonStandard(File file) {
		if (file.exists() && file.canWrite()) {
			IconInfo icon = new IconInfo(null, file);

			if (!icon.isStandardIcon()) {
				file.delete();

				iconCount--;
			}
		}
	}

	private static void copy(InputStream in, OutputStream out) throws IOException {
		final byte[] buffer = new byte[10240];

		int count = in.read(buffer);

		while (count != -1) {
			out.write(buffer, 0, count);
			count = in.read(buffer);
		}
	}

	private static void close(OutputStream out) {
		try {
			out.close();
		} catch (IOException ioe) {
			// ignore
		}
	}

	private static boolean isImageName(String name) {
		String[] suffixes = new String[] { ".gif", ".png", ".jpg", ".bmp" };

		for (String suffix : suffixes) {
			if (name.endsWith(suffix)) {
				return true;
			}
		}

		return false;
	}

	private static void copyImages(File srcDir, File destDir) {
		for (File child : srcDir.listFiles()) {
			if (child.isDirectory()) {
				copyImages(child, new File(destDir, child.getName()));
			} else if (isImageName(child.getName())) {
				copyFile(child, destDir);
			}
		}
	}

	private static void copyFile(File file, File destDir) {
		File destFile = new File(destDir, file.getName());

		if (!destDir.exists()) {
			destDir.mkdirs();
		}

		InputStream in = null;
		OutputStream out = null;

		try {
			in = new FileInputStream(file);
			out = new FileOutputStream(destFile);

			copy(in, out);
		} catch (IOException ioe) {
			// ignore
		} finally {
			if (in != null) {
				close(in);
			}
			if (out != null) {
				close(out);
			}
		}

		deleteIfNonStandard(destFile);
	}

	private static void close(InputStream in) {
		try {
			in.close();
		} catch (IOException ioe) {
			// ignore
		}
	}

	private boolean shouldHandlePlugin(String name) {
		for (String filter : filters) {
			if (name.matches(filter)) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {

		try {
			// workUnits are the plugins length
			// plus one for creating the index.html
			// plus one for refreshing
			// plus one for opening browser
			int workUnits = srcDir.listFiles().length + 3;
			monitor.beginTask("Processing " + srcDir.getPath() + "...", workUnits);

			for (File child : srcDir.listFiles()) {
				if (monitor.isCanceled())
					throw new OperationCanceledException();
				monitor.subTask(child.getAbsolutePath());
				if (child.isDirectory()) {
					handleDirectory(child);
				} else if (child.getName().endsWith(".jar")) {
					handleJar(child);
				}
				monitor.worked(1);
			}

			monitor.subTask("Creating index.html for crawled icons visualization");
			try {
				createIndex(destDir);
			} catch (IOException e) {
				e.printStackTrace();
			}
			monitor.worked(1);

			monitor.subTask("Refreshing workspace");
			WorkbenchUtils.refreshWorkspace(destDir.getParentFile().getAbsolutePath());
			monitor.worked(1);

			monitor.subTask("Opening external browser");
			try {
				URL url = new URL(destDir.getParentFile().toURI().toURL(), "index.html");
				PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(url);
			} catch (PartInitException e1) {
				MessageDialog.openError(Display.getCurrent().getActiveShell(), "Error",
						"Error opening the external Browser\nindex.html at " + destDir);
			} catch (MalformedURLException e1) {
				e1.printStackTrace();
			}
			monitor.worked(1);

		} catch (OperationCanceledException e) {
			return Status.CANCEL_STATUS;
		} finally {
			monitor.done();
		}
		return Status.OK_STATUS;
	}

}
