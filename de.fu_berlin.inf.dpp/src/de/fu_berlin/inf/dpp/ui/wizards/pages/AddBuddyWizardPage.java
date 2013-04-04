package de.fu_berlin.inf.dpp.ui.wizards.pages;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.jivesoftware.smack.Roster;
import org.picocontainer.annotations.Inject;

import de.fu_berlin.inf.dpp.SarosPluginContext;
import de.fu_berlin.inf.dpp.net.JID;
import de.fu_berlin.inf.dpp.net.SarosNet;
import de.fu_berlin.inf.dpp.ui.Messages;
import de.fu_berlin.inf.dpp.ui.widgets.decoration.EmptyText;
import de.fu_berlin.inf.dpp.ui.widgets.decoration.JIDCombo;
import de.fu_berlin.inf.nebula.utils.FontUtils;
import de.fu_berlin.inf.nebula.utils.LayoutUtils;

/**
 * Allows the user to enter a {@link JID}.
 * <p>
 * The wizard page is not supposed to show an error as long as the user did not
 * type the {@link JID} correctly at least once.
 * <p>
 * This conforms to Eclipse Usability Guideline 5.3: Start the wizard with a
 * prompt, not an error message.
 * 
 * @see <a
 *      href="http://wiki.eclipse.org/User_Interface_Guidelines#Wizards">Eclipse
 *      User Interface Guidelines</a>
 * @author bkahlert
 */

public class AddBuddyWizardPage extends WizardPage {
    public static final String TITLE = "Add Buddy"; //$NON-NLS-1$
    public static final String DESCRIPTION = "Enter the XMPP/Jabber ID of the buddy you want to add."; //$NON-NLS-1$

    @Inject
    protected SarosNet sarosNet;

    protected JIDCombo jidCombo;

    protected EmptyText nicknameText;

    /**
     * This flag is true as soon as the typed in {@link JID} was correctly
     * formatted.
     */
    protected boolean wasJIDValid = false;

    /**
     * True if the buddy is already in the {@link Roster}.
     */
    protected boolean isBuddyAlreadyAdded = false;

    public AddBuddyWizardPage() {
        super(AddBuddyWizardPage.class.getName());
        SarosPluginContext.initComponent(this);
        setTitle(TITLE);
        setDescription(DESCRIPTION);
    }

    @Override
    public void createControl(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        setControl(composite);

        composite.setLayout(LayoutUtils.createGridLayout(2, false, 10, 0));
        int space = new GridLayout().horizontalSpacing;
        GridData gridData;

        /*
         * Row 1
         */
        Label jidLabel = new Label(composite, SWT.NONE);
        gridData = new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
        jidLabel.setLayoutData(gridData);
        jidLabel.setText(Messages.jid_longform);

        Combo jidCombo = new Combo(composite, SWT.BORDER);
        gridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        gridData.horizontalIndent = space;
        jidCombo.setLayoutData(gridData);
        this.jidCombo = new JIDCombo(jidCombo);

        /*
         * Row 2
         */
        new Label(composite, SWT.NONE);

        Label exampleLabel = new Label(composite, SWT.NONE);
        gridData = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
        gridData.horizontalIndent = space;
        exampleLabel.setLayoutData(gridData);
        exampleLabel.setText(Messages.jid_example);
        FontUtils.changeFontSizeBy(exampleLabel, -1);

        /*
         * Row 3
         */
        Label nicknameLabel = new Label(composite, SWT.NONE);
        gridData = new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
        gridData.verticalIndent = space;
        nicknameLabel.setLayoutData(gridData);
        nicknameLabel.setText(Messages.AddBuddyWizardPage_nickname);

        Text nicknameText = new Text(composite, SWT.BORDER);
        gridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        gridData.horizontalIndent = space;
        gridData.verticalIndent = space;
        nicknameText.setLayoutData(gridData);
        this.nicknameText = new EmptyText(nicknameText,
            Messages.AddBuddyWizardPage_optional);

        hookListeners();
        updatePageCompletion();
    }

    protected void hookListeners() {
        this.jidCombo.getControl().addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                updatePageCompletion();
            }
        });
    }

    protected void updatePageCompletion() {
        JID ownJid = sarosNet.getMyJID();
        JID foreignJid = this.getBuddy();

        if (foreignJid.isValid() && !foreignJid.equals(ownJid)
            && !foreignJid.isResourceQualifiedJID()) {
            /*
             * Page is complete
             */

            wasJIDValid = true;

            Roster roster = sarosNet.getRoster();
            if (roster != null && roster.contains(foreignJid.getBase())) {
                setMessage(Messages.roster_alreadyadded_errorMessage
                    + "\n" + Messages.wizard_finish_noeffect, //$NON-NLS-1$
                    IMessageProvider.INFORMATION);
                isBuddyAlreadyAdded = true;
            } else {
                this.setMessage(DESCRIPTION);
                isBuddyAlreadyAdded = false;
            }

            this.setErrorMessage(null);
            setPageComplete(true);
        } else {
            /*
             * Page is incomplete
             */

            if (foreignJid.equals(ownJid)) {
                this.setErrorMessage(Messages.roster_addself_errorMessage);
            } else if (wasJIDValid) {
                this.setErrorMessage(Messages.jid_format_errorMessage);
            }
            this.setPageComplete(false);
        }
    }

    /*
     * WizardPage Results
     */

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (!visible)
            return;

        this.jidCombo.setFocus();
    }

    public JID getBuddy() {
        return new JID(getText());
    }

    public String getNickname() {
        return this.nicknameText.getText().trim();
    }

    public boolean isBuddyAlreadyAdded() {
        return isBuddyAlreadyAdded;
    }

    protected String getText() {
        return this.jidCombo.getText().trim();
    }
}