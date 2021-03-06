/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.swt.chart.actions;

import org.csstudio.swt.chart.Activator;
import org.csstudio.swt.chart.Chart;
import org.csstudio.swt.chart.Messages;
import org.eclipse.jface.action.Action;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.printing.PrintDialog;
import org.eclipse.swt.printing.Printer;
import org.eclipse.swt.printing.PrinterData;

/** An Action for printing the current image.
 *  <p>
 *  Suggested use is in the context menu of an editor or view that
 *  uses the InteractiveChart.
 *
 *  @author Kay Kasemir
 */
public class PrintCurrentImageAction extends Action
{
    /** Chart to print */
    private final Chart chart;

    /** Snapshot of the chart at time of print command */
    private Image snapshot;

    /** Printer */
    private Printer printer;

    /** Constructor */
    public PrintCurrentImageAction(Chart chart)
    {
        super(Messages.PrintImage_ActionName,
              Activator.getImageDescriptor("icons/print.gif")); //$NON-NLS-1$
        this.chart = chart;
        setToolTipText(Messages.PrintImage_ActionName_TT);
    }

    /** {@inheritDoc} */
    @Override
    public void run()
    {
        // Get snapshot. Disposed at end of printing
        snapshot = chart.createSnapshot();
        if (snapshot == null)
            return;

        // Printer GUI
        PrintDialog dlg = new PrintDialog(chart.getShell());
        PrinterData data = dlg.open();
        if (data == null)
        {
            snapshot.dispose();
            return;
        }
        // Get filename
        if (data.printToFile == true)
        {
            // Inconsistent: On the Mac, the file name is already set...
            // data.fileName = ImageFileName.get(chart.getShell());
        }
        printer = new Printer(data);
        // Print in background thread
        final Thread print_thread = new Thread("Print Thread") //$NON-NLS-1$
        {
            @Override
            public void run()
            {
                print();
            }
        };
        print_thread.start();
    }

    /** Print the <code>snapshot</code> to the <code>printer</code> */
    private void print()
    {
        try
        {
            if (!printer.startJob("Data Browser")) //$NON-NLS-1$
                return;
            // Printer page info
            final Rectangle area = printer.getClientArea();
            final Rectangle trim = printer.computeTrim(0, 0, 0, 0);
            final Point dpi = printer.getDPI();

            // Compute layout
            final Rectangle image_rect = snapshot.getBounds();
            // Leave one inch on each border.
            // (copied the computeTrim stuff from an SWT example.
            //  Really no clue...)
            final int left_right = dpi.x + trim.x;
            final int top_bottom = dpi.y + trim.y;
            final int printed_width = area.width - 2*left_right;
            // Try to scale height according to on-screen aspect ratio.
            final int max_height = area.height - 2*top_bottom;
            final int printed_height = Math.min(max_height,
               image_rect.height * printed_width / image_rect.width);


            // Print one page
            printer.startPage();
            final GC gc = new GC(printer);
            gc.drawImage(snapshot, 0, 0, image_rect.width, image_rect.height,
                        left_right, top_bottom, printed_width, printed_height);
            printer.endPage();
            // Done
            printer.endJob();
        }
        finally
        {
            snapshot.dispose();
        }
    }
}

