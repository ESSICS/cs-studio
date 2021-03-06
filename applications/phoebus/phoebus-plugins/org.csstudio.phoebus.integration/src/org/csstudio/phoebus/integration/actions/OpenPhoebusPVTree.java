package org.csstudio.phoebus.integration.actions;

import java.net.URI;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.csstudio.csdata.ProcessVariable;
import org.csstudio.phoebus.integration.PhoebusLauncherService;
import org.csstudio.ui.util.AdapterUtil;
import org.csstudio.ui.util.dialogs.ExceptionDetailsErrorDialog;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Command handler for opening phoebus pv tree on the current selection.
 *
 * @author carcassi
 * @author Kay Kasemir
 */
public class OpenPhoebusPVTree extends AbstractHandler implements IHandler {
    /** {@inheritDoc} */
    @Override
    public Object execute(final ExecutionEvent event) throws ExecutionException {
        final Shell shell = HandlerUtil.getActiveShell(event);
        try {
            // Retrieve the selection and the current page
            final ISelection selection = HandlerUtil.getActiveMenuSelection(event);
            final ProcessVariable[] pvs = AdapterUtil.convert(selection, ProcessVariable.class);

            // Were PVs provided (opened from context menu?)
            if (pvs == null || pvs.length <= 0) {
                PhoebusLauncherService.launchApplication(Messages.PVTree);
            } else {
                // Confirm if this could open many new probe instances
                if (pvs.length > 5 && !MessageDialog.openConfirm(shell,
                        Messages.MultipleInstancesTitle,
                        NLS.bind(Messages.MultipleInstancesFmt, pvs.length)))
                    return null;

                String query = Arrays.asList(pvs).stream().map(pv -> pv.getName()).collect(Collectors.joining("&"));
                query = query + "&" + "app="+Messages.PVTree;
                URI uri = new URI("pv", "", null, query, null);
                PhoebusLauncherService.launchResource(uri.toString());
            }
        } catch (Exception ex) {
            ExceptionDetailsErrorDialog.openError(shell, Messages.PVTreeErrorOpenPvtree, ex);
        }
        return null;
    }

}
