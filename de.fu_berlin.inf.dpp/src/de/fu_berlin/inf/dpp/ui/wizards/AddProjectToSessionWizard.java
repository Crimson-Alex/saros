package de.fu_berlin.inf.dpp.ui.wizards;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.picocontainer.annotations.Inject;

import de.fu_berlin.inf.dpp.Saros;
import de.fu_berlin.inf.dpp.SarosPluginContext;
import de.fu_berlin.inf.dpp.editor.internal.EditorAPI;
import de.fu_berlin.inf.dpp.filesystem.IChecksumCache;
import de.fu_berlin.inf.dpp.filesystem.ResourceAdapterFactory;
import de.fu_berlin.inf.dpp.invitation.FileList;
import de.fu_berlin.inf.dpp.invitation.FileListDiff;
import de.fu_berlin.inf.dpp.invitation.FileListFactory;
import de.fu_berlin.inf.dpp.invitation.IncomingProjectNegotiation;
import de.fu_berlin.inf.dpp.invitation.ProcessTools.CancelLocation;
import de.fu_berlin.inf.dpp.invitation.ProcessTools.CancelOption;
import de.fu_berlin.inf.dpp.invitation.ProjectNegotiation;
import de.fu_berlin.inf.dpp.monitoring.ProgressMonitorAdapterFactory;
import de.fu_berlin.inf.dpp.net.IConnectionManager;
import de.fu_berlin.inf.dpp.net.xmpp.JID;
import de.fu_berlin.inf.dpp.preferences.PreferenceUtils;
import de.fu_berlin.inf.dpp.project.ISarosSessionManager;
import de.fu_berlin.inf.dpp.session.ISarosSession;
import de.fu_berlin.inf.dpp.ui.Messages;
import de.fu_berlin.inf.dpp.ui.util.DialogUtils;
import de.fu_berlin.inf.dpp.ui.util.SWTUtils;
import de.fu_berlin.inf.dpp.ui.views.SarosView;
import de.fu_berlin.inf.dpp.ui.wizards.dialogs.WizardDialogAccessable;
import de.fu_berlin.inf.dpp.ui.wizards.pages.EnterProjectNamePage;
import de.fu_berlin.inf.dpp.util.ThreadUtils;
import de.fu_berlin.inf.dpp.vcs.VCSAdapter;

public class AddProjectToSessionWizard extends Wizard {

    private static Logger LOG = Logger
        .getLogger(AddProjectToSessionWizard.class);

    private EnterProjectNamePage namePage;
    private WizardDialogAccessable wizardDialog;
    private IncomingProjectNegotiation process;
    private JID peer;
    private List<FileList> fileLists;
    private boolean isExceptionCancel;

    @Inject
    private EditorAPI editorAPI;

    @Inject
    private IChecksumCache checksumCache;

    @Inject
    private IConnectionManager connectionManager;

    @Inject
    private PreferenceUtils preferenceUtils;

    @Inject
    private ISarosSessionManager sessionManager;

    private static class OverwriteErrorDialog extends ErrorDialog {

        public OverwriteErrorDialog(Shell parentShell, String dialogTitle,
            String dialogMessage, IStatus status) {
            super(parentShell, dialogTitle, dialogMessage, status, IStatus.OK
                | IStatus.INFO | IStatus.WARNING | IStatus.ERROR);
        }

        @Override
        protected void createButtonsForButtonBar(Composite parent) {
            super.createButtonsForButtonBar(parent);
            Button ok = getButton(IDialogConstants.OK_ID);
            ok.setText(Messages.JoinSessionWizard_yes);
            Button no = createButton(parent, IDialogConstants.CANCEL_ID,
                Messages.JoinSessionWizard_no, true);
            no.moveBelow(ok);
            no.setFocus();
        }
    }

    public AddProjectToSessionWizard(IncomingProjectNegotiation process,
        JID peer, List<FileList> fileLists) {

        SarosPluginContext.initComponent(this);

        this.process = process;
        this.peer = peer;
        this.fileLists = fileLists;
        setWindowTitle(Messages.AddProjectToSessionWizard_title);
        setHelpAvailable(true);
        setNeedsProgressMonitor(true);

        process.setProjectInvitationUI(this);

        /* holds if the wizard close is because of an exception or not */
        isExceptionCancel = false;

    }

    public void setWizardDlg(WizardDialogAccessable wizardDialog) {
        this.wizardDialog = wizardDialog;
    }

    @Override
    public void addPages() {
        ISarosSession session = sessionManager.getSarosSession();

        if (session == null)
            return;

        namePage = new EnterProjectNamePage(session, connectionManager,
            preferenceUtils, fileLists, peer, process.getProjectNames());

        addPage(namePage);
    }

    @Override
    public boolean performFinish() {

        if (namePage == null)
            return true;

        final Map<String, IProject> targetProjectMapping = getTargetProjectMapping();

        final boolean useVersionControl = namePage.useVersionControl();

        final Collection<IEditorPart> openEditors = getOpenEditorsForSharedProjects(targetProjectMapping
            .values());

        final List<IEditorPart> dirtyEditors = new ArrayList<IEditorPart>();

        boolean containsDirtyEditors = false;

        for (IEditorPart editor : openEditors) {
            if (editor.isDirty()) {
                containsDirtyEditors = true;
                dirtyEditors.add(editor);
            }
        }

        if (containsDirtyEditors) {
            SWTUtils.runSafeSWTAsync(LOG, new Runnable() {
                @Override
                public void run() {
                    if (AddProjectToSessionWizard.this.getShell().isDisposed())
                        return;

                    displaySaveDirtyEditorsDialog(dirtyEditors);

                }
            });

            return false;
        }

        final Map<String, FileListDiff> modifiedResources;

        try {
            modifiedResources = createProjectsAndGetModifiedResources(
                targetProjectMapping, useVersionControl);
        } catch (CoreException e) {
            LOG.error("could not compute local file list", e);
            MessageDialog.openError(getShell(), "Error computing file list",
                "Could not compute local file list: " + e.getMessage());
            return false;
        } catch (RuntimeException e) {
            LOG.error("internal error while computing file list", e);
            MessageDialog.openError(getShell(), "Error computing file list",
                "Internal error: " + e.getMessage());

            return false;
        }

        if (!confirmOverwritingResources(modifiedResources))
            return false;

        triggerProjectNegotiation(targetProjectMapping, useVersionControl,
            openEditors);

        return true;
    }

    public void cancelWizard(final JID jid, final String errorMsg,
        final CancelLocation cancelLocation) {

        SWTUtils.runSafeSWTAsync(LOG, new Runnable() {
            @Override
            public void run() {
                Shell shell = wizardDialog.getShell();
                if (shell == null || shell.isDisposed())
                    return;
                isExceptionCancel = true;
                wizardDialog.close();
            }
        });

        SWTUtils.runSafeSWTAsync(LOG, new Runnable() {
            @Override
            public void run() {
                showCancelMessage(jid, errorMsg, cancelLocation);
            }
        });
    }

    @Override
    public boolean performCancel() {
        if (!isExceptionCancel) {
            if (!DialogUtils.popUpYesNoQuestion(
                Messages.AddProjectToSessionWizard_leave_session,
                Messages.AddProjectToSessionWizard_leave_session_text, false)) {
                return false;
            }
            ThreadUtils.runSafeAsync("CancelAddProjectWizard", LOG,
                new Runnable() {
                    @Override
                    public void run() {
                        process.localCancel(null, CancelOption.NOTIFY_PEER);
                    }
                });
        }
        isExceptionCancel = false;
        return true;
    }

    private void triggerProjectNegotiation(
        final Map<String, IProject> targetProjectMapping,
        final boolean useVersionControl,
        final Collection<IEditorPart> editorsToClose) {

        final Job job = new Job("Synchronizing") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {

                    SWTUtils.runSafeSWTSync(LOG, new Runnable() {
                        @Override
                        public void run() {
                            /*
                             * close all editors to avoid any conflicts.
                             */
                            for (final IEditorPart editor : editorsToClose)
                                editorAPI.closeEditor(editor);

                        }
                    });

                    final IWorkspace workspace = ResourcesPlugin.getWorkspace();

                    final IWorkspaceDescription description = workspace
                        .getDescription();

                    final boolean isAutoBuilding = description.isAutoBuilding();

                    if (isAutoBuilding) {
                        description.setAutoBuilding(false);
                        try {
                            workspace.setDescription(description);
                        } catch (CoreException e) {
                            LOG.warn("could not disable auto building", e);
                        }
                    }

                    final Map<String, de.fu_berlin.inf.dpp.filesystem.IProject> convertedMapping = new HashMap<String, de.fu_berlin.inf.dpp.filesystem.IProject>();

                    for (final Entry<String, IProject> entry : targetProjectMapping
                        .entrySet()) {
                        convertedMapping.put(entry.getKey(),
                            ResourceAdapterFactory.create(entry.getValue()));
                    }

                    final ProjectNegotiation.Status status = process.run(
                        convertedMapping,
                        ProgressMonitorAdapterFactory.convertTo(monitor),
                        useVersionControl);

                    if (isAutoBuilding) {
                        description.setAutoBuilding(true);
                        try {
                            workspace.setDescription(description);
                        } catch (CoreException e) {
                            LOG.warn("could not re-enable auto building", e);
                        }
                    }

                    if (status != ProjectNegotiation.Status.OK)
                        return Status.CANCEL_STATUS;

                    final List<String> projectNames = new ArrayList<String>();

                    for (final IProject project : targetProjectMapping.values())
                        projectNames.add(project.getName());

                    SarosView
                        .showNotification(
                            Messages.AddProjectToSessionWizard_synchronize_finished_notification_title,
                            MessageFormat
                                .format(
                                    Messages.AddProjectToSessionWizard_synchronize_finished_notification_text,
                                    StringUtils.join(projectNames, ", ")));

                } catch (RuntimeException e) {
                    LOG.error(
                        "unknown error during project negotiation: "
                            + e.getMessage(), e);
                    return Status.CANCEL_STATUS;
                } finally {
                    SWTUtils.runSafeSWTAsync(LOG, new Runnable() {
                        @Override
                        public void run() {
                            for (IEditorPart editor : editorsToClose) {
                                if (((IFileEditorInput) editor.getEditorInput())
                                    .getFile().exists())
                                    editorAPI.openEditor(editor);
                            }
                        }
                    });
                }
                return Status.OK_STATUS;
            }
        };
        job.setUser(true);
        job.schedule();

    }

    private boolean confirmOverwritingResources(
        final Map<String, FileListDiff> modifiedResources) {

        String message = Messages.AddProjectToSessionWizard_synchronize_projects;

        String PID = Saros.SAROS;
        MultiStatus info = new MultiStatus(PID, 1, message, null);
        for (String projectName : modifiedResources.keySet()) {
            FileListDiff diff = modifiedResources.get(projectName);
            info.add(new Status(IStatus.INFO, PID, 1,
                MessageFormat.format(
                    Messages.AddProjectToSessionWizard_files_affected,
                    projectName), null));
            for (String path : diff.getRemovedPaths()) {
                info.add(new Status(IStatus.WARNING, PID, 1, MessageFormat
                    .format(Messages.AddProjectToSessionWizard_file_toRemove,
                        path), null));
            }
            for (String path : diff.getAlteredPaths()) {
                info.add(new Status(IStatus.WARNING, PID, 1, MessageFormat
                    .format(
                        Messages.AddProjectToSessionWizard_file_overwritten,
                        path), null));
            }
            for (String path : diff.getAddedPaths()) {
                info.add(new Status(IStatus.INFO, PID, 1, MessageFormat.format(
                    Messages.AddProjectToSessionWizard_file_added, path), null));
            }
            info.add(new Status(IStatus.INFO, PID, 1, "", null)); //$NON-NLS-1$
        }

        return new OverwriteErrorDialog(getShell(),
            Messages.AddProjectToSessionWizard_delete_local_changes, null, info)
            .open() == IDialogConstants.OK_ID;
    }

    private void showCancelMessage(JID jid, String errorMsg,
        CancelLocation cancelLocation) {

        final String peer = jid.getBase();
        final Shell shell = SWTUtils.getShell();

        if (errorMsg != null) {
            switch (cancelLocation) {
            case LOCAL:
                DialogUtils
                    .openErrorMessageDialog(
                        shell,
                        Messages.AddProjectToSessionWizard_invitation_cancelled,
                        MessageFormat
                            .format(
                                Messages.AddProjectToSessionWizard_invitation_cancelled_text,
                                errorMsg));
                break;
            case REMOTE:
                DialogUtils
                    .openErrorMessageDialog(
                        shell,
                        Messages.AddProjectToSessionWizard_invitation_cancelled,
                        MessageFormat
                            .format(
                                Messages.AddProjectToSessionWizard_invitation_cancelled_text2,
                                peer, errorMsg));
            }
        } else {
            switch (cancelLocation) {
            case LOCAL:
                break;
            case REMOTE:
                DialogUtils
                    .openInformationMessageDialog(
                        shell,
                        Messages.AddProjectToSessionWizard_invitation_cancelled,
                        MessageFormat
                            .format(
                                Messages.AddProjectToSessionWizard_invitation_cancelled_text3,
                                peer));
            }
        }
    }

    private Collection<IEditorPart> getOpenEditorsForSharedProjects(
        Collection<IProject> projects) {

        List<IEditorPart> openEditors = new ArrayList<IEditorPart>();
        Set<IEditorPart> editors = EditorAPI.getOpenEditors();

        for (IProject project : projects) {
            for (IEditorPart editor : editors) {
                if (editor.getEditorInput() instanceof IFileEditorInput) {
                    IFile file = ((IFileEditorInput) editor.getEditorInput())
                        .getFile();
                    if (project.equals(file.getProject()))
                        openEditors.add(editor);
                }
            }
        }
        return openEditors;
    }

    /**
     * Returns all modified resources (either changed or deleted) for the
     * current project mapping. Creates non existing projects if necessary.
     */
    private Map<String, FileListDiff> createProjectsAndGetModifiedResources(
        final Map<String, IProject> projectMapping,
        final boolean useVersionControl) throws CoreException {

        final Map<String, IProject> modifiedProjects = getModifiedProjects(projectMapping);
        final Map<String, FileListDiff> result = new HashMap<String, FileListDiff>();

        try {
            getContainer().run(true, false, new IRunnableWithProgress() {
                @Override
                public void run(IProgressMonitor monitor)
                    throws InvocationTargetException, InterruptedException {
                    try {

                        for (final IProject project : projectMapping.values()) {
                            if (!modifiedProjects.values().contains(project)) {

                                if (!project.exists())
                                    project.create(null);
                            }

                            if (!project.isOpen())
                                project.open(null);
                        }

                        result.putAll(getModifiedResources(modifiedProjects,
                            useVersionControl, monitor));
                    } catch (CoreException e) {
                        throw new InvocationTargetException(e);
                    } catch (RuntimeException e) {
                        throw new InvocationTargetException(e);
                    }
                }
            });
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();

            if (cause instanceof CoreException)
                throw (CoreException) cause;
            else if (cause instanceof RuntimeException)
                throw (RuntimeException) cause;
            else
                throw new RuntimeException(cause.getMessage(), cause);
        } catch (InterruptedException e) {
            throw new RuntimeException("unexpected interruption", e);
        }

        return result;
    }

    /**
     * Returns all modified resources (either changed or deleted) for the
     * current project mapping.
     * <p>
     * <b>Important:</b> Do not call this inside the SWT Thread. This is a long
     * running operation !
     */
    private Map<String, FileListDiff> getModifiedResources(
        final Map<String, IProject> projectMapping,
        final boolean includeVCSData, final IProgressMonitor monitor)
        throws CoreException {

        final Map<String, FileListDiff> modifiedResources = new HashMap<String, FileListDiff>();

        final ISarosSession session = sessionManager.getSarosSession();

        // FIXME the wizard should handle the case that the session may stop in
        // the meantime !

        if (session == null)
            throw new IllegalStateException("no session running");

        final SubMonitor subMonitor = SubMonitor.convert(monitor,
            "Searching for files that will be modified...",
            projectMapping.size());

        for (final Entry<String, IProject> entry : projectMapping.entrySet()) {

            final String projectID = entry.getKey();
            final IProject project = entry.getValue();

            final de.fu_berlin.inf.dpp.filesystem.IProject adaptedProject = ResourceAdapterFactory
                .create(entry.getValue());

            /*
             * do not refresh already partially shared projects as this may
             * trigger resource change events
             */
            if (!session.isShared(adaptedProject))
                project.refreshLocal(IResource.DEPTH_INFINITE, null);

            final VCSAdapter vcs;

            // FIXME how to handle failure ?
            if (includeVCSData)
                vcs = VCSAdapter.getAdapter(project);
            else
                vcs = null;

            final FileList localFileList;

            /*
             * TODO optimize for partial shared projects a.k.a do not scan all
             * files
             */

            try {
                localFileList = FileListFactory.createFileList(adaptedProject,
                    null, checksumCache, vcs, ProgressMonitorAdapterFactory
                        .convertTo(subMonitor.newChild(1,
                            SubMonitor.SUPPRESS_ALL_LABELS)));
            } catch (IOException e) {
                Throwable cause = e.getCause();

                if (cause instanceof CoreException)
                    throw (CoreException) cause;

                throw new CoreException(new org.eclipse.core.runtime.Status(
                    IStatus.ERROR, Saros.SAROS,
                    "failed to compute local file list", e));
            }

            final FileListDiff diff = FileListDiff.diff(localFileList,
                process.getRemoteFileList(projectID));

            if (process.isPartialRemoteProject(projectID))
                diff.clearRemovedPaths();

            if (!diff.getRemovedPaths().isEmpty()
                || !diff.getAlteredPaths().isEmpty()) {
                modifiedResources.put(project.getName(), diff);
            }
        }

        return modifiedResources;
    }

    /**
     * Returns a project mapping that contains all projects that will be
     * modified on synchronization.
     */
    private Map<String, IProject> getModifiedProjects(
        Map<String, IProject> projectMapping) {
        Map<String, IProject> modifiedProjects = new HashMap<String, IProject>();

        for (Entry<String, IProject> entry : projectMapping.entrySet()) {
            if (!namePage.overwriteResources(entry.getKey()))
                continue;

            modifiedProjects.put(entry.getKey(), entry.getValue());
        }

        return modifiedProjects;
    }

    /**
     * Returns the project ids and their target project as selected in the
     * {@link EnterProjectNamePage}.
     * <p>
     * This method must only be called after the page in completed state !
     */
    private Map<String, IProject> getTargetProjectMapping() {

        final Map<String, IProject> result = new HashMap<String, IProject>();

        for (final FileList list : fileLists) {
            final String projectID = list.getProjectID();

            result.put(projectID, ResourcesPlugin.getWorkspace().getRoot()
                .getProject(namePage.getTargetProjectName(projectID)));
        }

        return result;
    }

    private void displaySaveDirtyEditorsDialog(
        final List<IEditorPart> dirtyEditors) {
        int max = Math.min(20, dirtyEditors.size());
        int more = dirtyEditors.size() - max;

        List<String> dirtyEditorNames = new ArrayList<String>();

        for (IEditorPart editor : dirtyEditors.subList(0, max))
            dirtyEditorNames.add(editor.getTitle());

        Collections.sort(dirtyEditorNames);

        if (more > 0)
            dirtyEditorNames.add(MessageFormat.format(
                Messages.AddProjectToSessionWizard_unsaved_changes_dialog_more,
                more));

        String allDirtyEditorNames = StringUtils.join(dirtyEditorNames, ", ");

        String dialogText = MessageFormat.format(
            Messages.AddProjectToSessionWizard_unsaved_changes_dialog_text,
            allDirtyEditorNames);

        boolean proceed = DialogUtils.openQuestionMessageDialog(getShell(),
            Messages.AddProjectToSessionWizard_unsaved_changes_dialog_title,
            dialogText);

        /*
         * The wizard can be closed automatically and so 'proceed' would be true
         * if this happens although the user did not clicked anything.
         */
        if (getShell().isDisposed())
            return;

        if (proceed) {
            for (IEditorPart editor : dirtyEditors)
                editor.doSave(new NullProgressMonitor());
        }
    }
}
