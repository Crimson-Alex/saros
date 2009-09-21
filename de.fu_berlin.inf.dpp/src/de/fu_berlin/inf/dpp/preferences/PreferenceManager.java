package de.fu_berlin.inf.dpp.preferences;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;

import de.fu_berlin.inf.dpp.annotations.Component;
import de.fu_berlin.inf.dpp.optional.jdt.JDTFacade;
import de.fu_berlin.inf.dpp.preferences.IPreferenceManipulator.IRestorePoint;
import de.fu_berlin.inf.dpp.project.AbstractSessionListener;
import de.fu_berlin.inf.dpp.project.ISharedProject;
import de.fu_berlin.inf.dpp.project.SessionManager;

/**
 * The Preference Manager is responsible for
 * 
 * a.) changing dangerous preferences on the host or client before a
 * SharedProject session is started
 * 
 * b.) restoring the preference after the end of the session
 * 
 * c.) TODO Tracking changes to the preferences during the session and warning
 * the user
 * 
 * d.) TODO Show proper warnings to the user
 */
@Component(module = "prefs")
public class PreferenceManager {

    protected List<IPreferenceManipulator> manipulators = new ArrayList<IPreferenceManipulator>();

    protected List<IRestorePoint> restorePoints = new ArrayList<IRestorePoint>();

    public PreferenceManager(JDTFacade jdtFacade, SessionManager sessionManager) {

        // collect PreferenceManipulators
        if (jdtFacade.isJDTAvailable()) {
            manipulators.addAll(jdtFacade.getPreferenceManipulators());
        }
        manipulators.add(new LineDelimiterManipulator());
        manipulators.add(new EncodingManipulator());

        sessionManager.addSessionListener(new AbstractSessionListener() {

            @Override
            public void sessionStarted(ISharedProject session) {

                // TODO The user should be told that we are changing options...
                IProject project = session.getProject();
                if (session.isHost()) {
                    for (IPreferenceManipulator manipulator : manipulators) {
                        if (manipulator.isDangerousForHost()
                            && manipulator.isEnabled(project)) {
                            restorePoints.add(manipulator.change(project));
                        }
                    }
                } else {
                    for (IPreferenceManipulator manipulator : manipulators) {
                        if (manipulator.isDangerousForClient()
                            && manipulator.isEnabled(project)) {
                            restorePoints.add(manipulator.change(project));
                        }
                    }
                }
            }

            @Override
            public void sessionEnded(ISharedProject session) {
                // TODO we should monitor the preferences for changes during the
                // session
                for (IRestorePoint restorePoint : restorePoints) {
                    restorePoint.restore();
                }
            }
        });
    }
}
