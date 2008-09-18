package org.csstudio.trends.databrowser.plotpart;

import org.csstudio.archive.ArchiveServer;
import org.csstudio.archive.cache.ArchiveCache;
import org.csstudio.archive.crawl.BatchIterator;
import org.csstudio.platform.data.ITimestamp;
import org.csstudio.platform.data.IValue;
import org.csstudio.platform.model.IArchiveDataSource;
import org.csstudio.trends.databrowser.Plugin;
import org.csstudio.trends.databrowser.model.IPVModelItem;
import org.csstudio.trends.databrowser.preferences.Preferences;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

/** Eclipse background job for fetching samples from the data server.
 *  @author Kay Kasemir
 */
class ArchiveFetchJob extends Job
{
    final private IPVModelItem item;
    final private ITimestamp start, end;
    final private ArchiveFetchJobListener listener;
    
    /** Construct job that fetches data.
     *  @param item Item for which to fetch samples
     *  @param start Start time
     *  @param end End time
     */
    public ArchiveFetchJob(final IPVModelItem item, final ITimestamp start,
            final ITimestamp end, final ArchiveFetchJobListener listener)
    {
        super(Messages.FetchDataForPV
                + "'" + item.getName() + "'"); //$NON-NLS-1$ //$NON-NLS-2$
        this.item = item;
        this.start = start;
        this.end = end;
        this.listener = listener;
        // Do we need to assert that only one data fetch runs at a time?
        // setRule()...?
    }
    
    @SuppressWarnings("nls")
    @Override
    protected IStatus run(final IProgressMonitor monitor)
    {
        final IArchiveDataSource archives[] = item.getArchiveDataSources();
        monitor.beginTask(Messages.FetchingSample, archives.length);
        for (int i=0; i<archives.length; ++i)
        {
            // Display "N/total", using '1' for the first sub-archive.
            monitor.subTask(Messages.Fetch_Archive
                + "'" + archives[i].getName()
                + "' ("
                + (i+1) + "/" + archives.length + ")");
            final ArchiveCache cache = ArchiveCache.getInstance();
            try
            {   // Invoke the possibly lengthy search.
                final ArchiveServer server =
                    cache.getServer(archives[i].getUrl());
                
                String request_type;
                Object[] request_parms;
                final int bins = Preferences.getPlotBins();
                if (item.getRequestType() == IPVModelItem.RequestType.RAW)
                {
                    request_type = ArchiveServer.GET_RAW;
                    request_parms = new Object[] { new Integer(bins) };
                }
                else
                {
                    request_type = ArchiveServer.GET_AVERAGE;
                    final double interval =
                        (end.toDouble() - start.toDouble()) / bins;
                    request_parms = new Object[] { new Double(interval) };
                }
                
                final BatchIterator batch = new BatchIterator(server,
                                archives[i].getKey(), item.getName(),
                                start, end, request_type, request_parms);
                IValue result[] = batch.getBatch();
                while (result != null)
                {   // Notify model of new samples.
                    // Even when monitor.isCanceled at this point?
                    // Yes, since we have the samples, might as well show them
                    // before bailing out.
                    if (result.length > 0)
                        item.addArchiveSamples(server.getServerName(), result);
                    if (monitor.isCanceled())
                        break;
                    result = batch.next();
                }
            }
            catch (Exception ex)
            {
                Plugin.getLogger().error("ArchiveFetchJob", ex);
            }
            // Stop and ignore further results when canceled.
            if (monitor.isCanceled())
                return Status.CANCEL_STATUS;
            // Handled one sub-archive.
            monitor.worked(1);
        }
        monitor.done();
        listener.fetchCompleted(this);
        return Status.OK_STATUS;
    }
}