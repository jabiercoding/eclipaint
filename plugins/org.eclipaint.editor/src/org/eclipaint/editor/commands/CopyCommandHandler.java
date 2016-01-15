package org.eclipaint.editor.commands;

import org.eclipaint.editor.ImageEditor;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * CopyCommandHandler
 * 
 * @author Jabier Martinez
 */
public class CopyCommandHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchWindow wwindow = HandlerUtil.getActiveWorkbenchWindow(event);
		IWorkbenchPage page = wwindow.getActivePage();
		IEditorPart editorPart = page.getActiveEditor();
		if (editorPart instanceof ImageEditor) {
			ImageEditor editor = (ImageEditor) editorPart;
			editor.editorUtils.copy();
		}
		return null;
	}

}
