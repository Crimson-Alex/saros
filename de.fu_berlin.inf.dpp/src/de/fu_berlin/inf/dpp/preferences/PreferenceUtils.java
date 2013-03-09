package de.fu_berlin.inf.dpp.preferences;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.Mixer;

import org.eclipse.jface.preference.IPreferenceStore;
import org.xiph.speex.spi.SpeexEncoding;

import de.fu_berlin.inf.dpp.annotations.Component;
import de.fu_berlin.inf.dpp.communication.audio.MixerManager;
import de.fu_berlin.inf.dpp.editor.colorstorage.UserColorID;

@Component(module = "prefs")
public class PreferenceUtils {

    private IPreferenceStore preferenceStore;
    private MixerManager mixerManager;

    public PreferenceUtils(IPreferenceStore preferenceStore,
        MixerManager mixerManager) {
        this.preferenceStore = preferenceStore;
        this.mixerManager = mixerManager;
    }

    public boolean isDebugEnabled() {
        return preferenceStore.getBoolean(PreferenceConstants.DEBUG);
    }

    public boolean isAutoReuseExisting() {
        return preferenceStore
            .getBoolean(PreferenceConstants.AUTO_REUSE_PROJECT);
    }

    /**
     * Returns Saros's XMPP/Jabber server dns address.
     * 
     * @return
     */
    public String getSarosXMPPServer() {
        return "saros-con.imp.fu-berlin.de";
    }

    /**
     * Returns the default server.<br/>
     * Is never empty or null.
     * 
     * @return
     */
    public String getDefaultServer() {
        return getSarosXMPPServer();
    }

    /**
     * Returns whether auto-connect is enabled or not.
     * 
     * @return true if auto-connect is enabled.
     */
    public boolean isAutoConnecting() {
        return preferenceStore.getBoolean(PreferenceConstants.AUTO_CONNECT);
    }

    /**
     * Returns whether port mapping is enabled or not by evaluating the stored
     * deviceID to be empty or not.
     * 
     * @return true of port mapping is enabled, false otherwise
     */
    public boolean isAutoPortmappingEnabled() {
        return preferenceStore.getString(
            PreferenceConstants.AUTO_PORTMAPPING_DEVICEID).isEmpty() == false;
    }

    /**
     * Returns the device ID of the gateway to perform port mapping on.
     * 
     * @return Device ID of the gateway or empty String if disabled.
     */
    public String getAutoPortmappingGatewayID() {
        return preferenceStore
            .getString(PreferenceConstants.AUTO_PORTMAPPING_DEVICEID);
    }

    public int getAutoPortmappingLastPort() {
        return preferenceStore
            .getInt(PreferenceConstants.AUTO_PORTMAPPING_LASTMAPPEDPORT);
    }

    /**
     * Returns the Skype user name or an empty string if none was specified.
     * 
     * @return the user name.for Skype or an empty string
     */
    public String getSkypeUserName() {
        return preferenceStore.getString(PreferenceConstants.SKYPE_USERNAME);
    }

    /**
     * Returns the port for SOCKS5 file transfer. If
     * {@link PreferenceConstants#USE_NEXT_PORTS_FOR_FILE_TRANSFER} is set, a
     * negative number is returned (smacks will try next free ports above this
     * number)
     * 
     * @return port for smacks configuration (negative if to try out ports
     *         above)
     */
    public int getFileTransferPort() {
        int port = preferenceStore
            .getInt(PreferenceConstants.FILE_TRANSFER_PORT);

        if (preferenceStore
            .getBoolean(PreferenceConstants.USE_NEXT_PORTS_FOR_FILE_TRANSFER))
            return -port;
        else
            return port;
    }

    public boolean isSkipSyncSelectable() {
        return preferenceStore
            .getBoolean(PreferenceConstants.SKIP_SYNC_SELECTABLE);
    }

    public boolean forceFileTranserByChat() {
        return preferenceStore
            .getBoolean(PreferenceConstants.FORCE_FILETRANSFER_BY_CHAT);
    }

    public boolean isConcurrentUndoActivated() {
        return preferenceStore.getBoolean(PreferenceConstants.CONCURRENT_UNDO);
    }

    public boolean isPingPongActivated() {
        return preferenceStore.getBoolean(PreferenceConstants.PING_PONG);
    }

    public boolean useVersionControl() {
        return !preferenceStore
            .getBoolean(PreferenceConstants.DISABLE_VERSION_CONTROL);
    }

    public void setUseVersionControl(boolean value) {
        preferenceStore.setValue(PreferenceConstants.DISABLE_VERSION_CONTROL,
            !value);
    }

    public Mixer getRecordingMixer() {
        return mixerManager.getMixerByName(preferenceStore
            .getString(PreferenceConstants.AUDIO_RECORD_DEVICE));
    }

    public Mixer getPlaybackMixer() {
        return mixerManager.getMixerByName(preferenceStore
            .getString(PreferenceConstants.AUDIO_PLAYBACK_DEVICE));
    }

    public AudioFormat getEncodingFormat() {
        Encoding encoding;
        float sampleRate = Float.parseFloat(preferenceStore
            .getString(PreferenceConstants.AUDIO_SAMPLERATE));
        int quality = Integer.parseInt(preferenceStore
            .getString(PreferenceConstants.AUDIO_QUALITY_LEVEL));
        boolean vbr = preferenceStore.getBoolean(PreferenceConstants.AUDIO_VBR);

        Encoding encodingsVbr[] = new Encoding[] { SpeexEncoding.SPEEX_VBR0,
            SpeexEncoding.SPEEX_VBR1, SpeexEncoding.SPEEX_VBR2,
            SpeexEncoding.SPEEX_VBR3, SpeexEncoding.SPEEX_VBR4,
            SpeexEncoding.SPEEX_VBR5, SpeexEncoding.SPEEX_VBR6,
            SpeexEncoding.SPEEX_VBR7, SpeexEncoding.SPEEX_VBR8,
            SpeexEncoding.SPEEX_VBR9, SpeexEncoding.SPEEX_VBR10 };

        Encoding encodingsCbr[] = new Encoding[] { SpeexEncoding.SPEEX_Q0,
            SpeexEncoding.SPEEX_Q1, SpeexEncoding.SPEEX_Q2,
            SpeexEncoding.SPEEX_Q3, SpeexEncoding.SPEEX_Q4,
            SpeexEncoding.SPEEX_Q5, SpeexEncoding.SPEEX_Q6,
            SpeexEncoding.SPEEX_Q7, SpeexEncoding.SPEEX_Q8,
            SpeexEncoding.SPEEX_Q9, SpeexEncoding.SPEEX_Q10 };

        if (vbr) {
            encoding = encodingsVbr[quality];
        } else {
            encoding = encodingsCbr[quality];
        }
        return new AudioFormat(encoding, sampleRate, 16, 1, 2, sampleRate,
            false);
    }

    public boolean isDtxEnabled() {
        return preferenceStore.getBoolean(PreferenceConstants.AUDIO_ENABLE_DTX);
    }

    public boolean isLocalSOCKS5ProxyEnabled() {
        return !preferenceStore
            .getBoolean(PreferenceConstants.LOCAL_SOCKS5_PROXY_DISABLED);
    }

    public String getStunIP() {
        return preferenceStore.getString(PreferenceConstants.STUN);
    }

    public int getStunPort() {
        return preferenceStore.getInt(PreferenceConstants.STUN_PORT);
    }

    public String isNeedsBasedSyncEnabled() {
        return preferenceStore.getString(PreferenceConstants.NEEDS_BASED_SYNC);
    }

    public void setNeedsBasedSyncEnabled(boolean value) {
        preferenceStore.setValue(PreferenceConstants.NEEDS_BASED_SYNC, value);
    }

    public boolean isVOIPEnabled() {
        return preferenceStore.getBoolean(PreferenceConstants.VOIP_ENABLED);
    }

    /**
     * Returns the favorite color ID that should be used during a session.
     * 
     * @return the favorite color ID or {@value UserColorID#UNKNOWN} if no
     *         favorite color ID is available
     */
    public int getFavoriteColorID() {
        return preferenceStore
            .getInt(PreferenceConstants.FAVORITE_SESSION_COLOR_ID);
    }
}
