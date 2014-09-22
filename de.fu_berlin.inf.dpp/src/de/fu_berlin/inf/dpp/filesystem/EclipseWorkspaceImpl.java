package de.fu_berlin.inf.dpp.filesystem;

import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import de.fu_berlin.inf.dpp.Saros;
import de.fu_berlin.inf.dpp.monitoring.IProgressMonitor;
import de.fu_berlin.inf.dpp.monitoring.ProgressMonitorAdapterFactory;

/**
 * Eclipse implementation of {@link IWorkspace}. Lets you execute
 * {@link IWorkspaceRunnable}s via {@link #run(IWorkspaceRunnable) run()}.
 */
public class EclipseWorkspaceImpl implements IWorkspace {

    /**
     * Takes an IDE-independent {@link IWorkspaceRunnable Saros
     * WorkspaceRunnable} and wraps it, so it can be treated as an Eclipse
     * WorkspaceRunnable.
     * <p>
     * The {@link #run(org.eclipse.core.runtime.IProgressMonitor) run()}-method
     * takes care of converting the {@link IProgressMonitor progress monitor}
     * and any thrown Exceptions.
     */
    private static class EclipseRunnableAdapter implements
        org.eclipse.core.resources.IWorkspaceRunnable {

        private IWorkspaceRunnable delegate;

        /**
         * @param runnable
         *            a {@link IWorkspaceRunnable Saros WorkspaceRunnable}
         */
        public EclipseRunnableAdapter(IWorkspaceRunnable runnable) {
            this.delegate = runnable;
        }

        @Override
        public void run(org.eclipse.core.runtime.IProgressMonitor monitor)
            throws CoreException {
            IProgressMonitor mon = ProgressMonitorAdapterFactory
                .convertTo(monitor);
            try {
                delegate.run(mon);
            } catch (IOException e) {
                IStatus status = new Status(IStatus.ERROR, Saros.PLUGIN_ID,
                    e.getMessage(), e);
                throw new CoreException(status);
            }
        }
    }

    private org.eclipse.core.resources.IWorkspace delegate;

    public EclipseWorkspaceImpl(org.eclipse.core.resources.IWorkspace workspace) {
        this.delegate = workspace;
    }

    @Override
    public void run(final IWorkspaceRunnable runnable) throws IOException {
        org.eclipse.core.resources.IWorkspaceRunnable eclipseRunnable;

        /*
         * Don't wrap the runnable again if its actually a wrapped Eclipse
         * runnable, but extract the delegate instead.
         */
        if (runnable instanceof EclipseWorkspaceRunnableImpl) {
            eclipseRunnable = ((EclipseWorkspaceRunnableImpl) runnable)
                .getDelegate();
        } else {
            eclipseRunnable = new EclipseRunnableAdapter(runnable);
        }

        try {
            delegate.run(eclipseRunnable, delegate.getRoot(),
                org.eclipse.core.resources.IWorkspace.AVOID_UPDATE, null);
        } catch (CoreException e) {
            throw new IOException(e);
        }
    }

    @Override
    public IProject getProject(String project) {
        return ResourceAdapterFactory.create(delegate.getRoot().getProject(
            project));
    }

    @Override
    public IPath getLocation() {
        return ResourceAdapterFactory.create(delegate.getRoot().getLocation());
    }

}
