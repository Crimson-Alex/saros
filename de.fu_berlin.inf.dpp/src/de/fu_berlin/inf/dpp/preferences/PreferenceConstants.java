/*
 * DPP - Serious Distributed Pair Programming
 * (c) Freie Universitaet Berlin - Fachbereich Mathematik und Informatik - 2006
 * (c) Riad Djemili - 2006
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 1, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package de.fu_berlin.inf.dpp.preferences;

import de.fu_berlin.inf.dpp.StartupSaros;

/**
 * Constant definitions for plug-in preferences
 * 
 * @author rdjemili
 * @author coezbek
 */
public class PreferenceConstants {

    public static final String SERVER = "server";

    public static final String USERNAME = "username";

    public static final String PASSWORD = "password";

    public static final String AUTO_CONNECT = "autoconnect";

    public static final String AUTO_FOLLOW_MODE = "autofollowmode";

    public static final String SKYPE_USERNAME = "skypename";

    public static final String CONCURRENT_UNDO = "concurrent_undo";

    public static final String DEBUG = "debug";

    public static final String FILE_TRANSFER_PORT = "port";

    public static final String FORCE_FILETRANSFER_BY_CHAT = "chatfiletransfer";

    public static final String CHATFILETRANSFER_CHUNKSIZE = "chatfiletransfer_chunksize";

    public static final String STUN = "stun_server";

    public static final String STUN_PORT = "stun_server_port";

    public static final String MULTI_DRIVER = "multidriver";

    public static final String AUTO_ACCEPT_INVITATION = "AUTO_ACCEPT_INVITATION";

    public static final String FOLLOW_EXCLUSIVE_DRIVER = "FOLLOW_EXCLUSIVE_DRIVER";

    public static final String AUTO_INVITE = "AUTO_INVITE";

    public static final String AUTO_REUSE_PROJECT = "AUTO_REUSE_PROJECT";

    /** Preference of the InvitationDialog */
    public static final String AUTO_CLOSE_DIALOG = "invitation.dialog.auto.close";

    public static final String SKIP_SYNC_SELECTABLE = "invitation.dialog.skip.enabled";

    /*
     * Preferences of the feedback preferences page
     * 
     * These preferences are kept both in the workspace scope and globally (in
     * the configuration).
     */

    /** Can be set and read by the FeedbackManager */
    public static final String FEEDBACK_SURVEY_DISABLED = "feedback.survey.disabled";

    /** Can be set and read by the FeedbackManager */
    public static final String FEEDBACK_SURVEY_INTERVAL = "feedback.survey.interval";

    /** Can be set and read by the StatisticManager */
    public static final String STATISTIC_ALLOW_SUBMISSION = "statistic.allow.submission";

    /** Can be set and read by the ErrorLogManager */
    public static final String ERROR_LOG_ALLOW_SUBMISSION = "error.log.allow.submission";

    /** Can be set and read by the ErrorLogManager */
    public static final String ERROR_LOG_ALLOW_SUBMISSION_FULL = "error.log.allow.submission.full";

    /** Is used for early startup in StartupSaros */
    public static final String NEW_WORKSPACE = "eclipse.new.workspace";

    /*
     * Global preferences, not initialized i.e. no default values
     */

    /** Can be set and read by the StatisticManager */
    public static final String SESSION_COUNT = "session.count";

    /** Can be set and read by the FeedbackManager */
    public static final String SESSIONS_UNTIL_NEXT = "sessions.until.next.survey";

    /**
     * Preference key for preference containing the version of Saros with which
     * Eclipse was last started.
     * 
     * Used by {@link StartupSaros} to determine whether there was a version
     * update.
     */
    public static final String SAROS_VERSION = "saros.version";

    /** Can be read by the StatisticManager */
    public static final String RANDOM_USER_ID = "user.id";

    /**
     * Preference used for a way to let the user identify himself.
     * 
     * For instance, this might get a value such as "coezbek" or "rdjemili".
     */
    public static final String STATISTICS_PSEUDONYM_ID = "STATISTICS_PSEUDONYM_ID";

    /**
     * Preference used to let the user declare whether he wants a self defined
     * pseudonym be transmitted during statistics or error log transmission
     */
    public static final String STATISTIC_ALLOW_PSEUDONYM = "STATISTIC_ALLOW_PSEUDONYM";

    public static final String PING_PONG = "PINGPONG";

    /**
     * Preferences for Communication
     */

    /** Chatserver for MUC e.g. conference.jabber.ccc.de **/
    public static final String CHATSERVER = "chatserver";

    /**
     * User specified chatroom name. If no chatroom is specified the session ID
     * is chatroom name
     **/
    public static final String CHATROOM = "chatroom";

    /** The checkbox preference for the chatroom **/
    public static final String USER_DEFINED_CHATROOM = "USER_DEFINED_CHATROOM";

    /**
     * User specified chatroom password. Only available at
     * CommunicationPreferencePage if USER_DEFINED_CHATROOM == true. If no
     * chatroom password is specified a password will be generated
     **/
    public static final String CHATROOM_PASSWORD = "CHATROOM_PASSWORD";

    /** The checkbox preference for the chatroom password **/
    public static final String USER_DEFINED_CHATROOM_PASSWORD = "USER_DEFINED_CHATROOM_PASSWORD";

    /** Audio quality level (1-10, 10 is best) **/
    public static final String AUDIO_QUALITY_LEVEL = "AUDIO_QUALITY_LEVEL";

    /** Checkbox if variable bit rate encoding is used */
    public static final String AUDIO_VBR = "AUDIO_VBR";

    /** Sample rate for audio encoder **/
    public static final String AUDIO_SAMPLERATE = "AUDIO_SAMPLERATE";

    /** Checkbox if discontinues transmission transmission is used */
    public static final String AUDIO_ENABLE_DTX = "AUDIO_ENABLE_DTX";

    /** Record device for VoIP **/
    public static final String AUDIO_RECORD_DEVICE = "AUDIO_RECORD_DEVICE";

    /** Playback device for VoIP **/
    public static final String AUDIO_PLAYBACK_DEVICE = "AUDIO_PLAYBACK_DEVICE";

}
