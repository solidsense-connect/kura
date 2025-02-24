/*******************************************************************************
 * Copyright (c) 2011, 2023 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 *  Sterwen-Technology
 *******************************************************************************/
package org.eclipse.kura.web.client.ui.network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.ui.EntryClassUi;
import org.eclipse.kura.web.client.ui.NewPasswordInput;
import org.eclipse.kura.web.client.ui.validator.GwtValidators;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.client.util.HelpButton;
import org.eclipse.kura.web.client.util.MessageUtils;
import org.eclipse.kura.web.shared.GwtSafeHtmlUtils;
import org.eclipse.kura.web.shared.model.GwtConsoleUserOptions;
import org.eclipse.kura.web.shared.model.GwtNetIfStatus;
import org.eclipse.kura.web.shared.model.GwtNetInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtSession;
import org.eclipse.kura.web.shared.model.GwtWifiBgscanModule;
import org.eclipse.kura.web.shared.model.GwtWifiChannelFrequency;
import org.eclipse.kura.web.shared.model.GwtWifiChannelModel;
import org.eclipse.kura.web.shared.model.GwtWifiCiphers;
import org.eclipse.kura.web.shared.model.GwtWifiConfig;
import org.eclipse.kura.web.shared.model.GwtWifiHotspotEntry;
import org.eclipse.kura.web.shared.model.GwtWifiNetInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtWifiRadioMode;
import org.eclipse.kura.web.shared.model.GwtWifiSecurity;
import org.eclipse.kura.web.shared.model.GwtWifiWirelessMode;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtNetworkService;
import org.eclipse.kura.web.shared.service.GwtNetworkServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.gwtbootstrap3.client.ui.Alert;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.Form;
import org.gwtbootstrap3.client.ui.FormGroup;
import org.gwtbootstrap3.client.ui.FormLabel;
import org.gwtbootstrap3.client.ui.HelpBlock;
import org.gwtbootstrap3.client.ui.InlineRadio;
import org.gwtbootstrap3.client.ui.Input;
import org.gwtbootstrap3.client.ui.ListBox;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.ModalBody;
import org.gwtbootstrap3.client.ui.ModalFooter;
import org.gwtbootstrap3.client.ui.PanelHeader;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.client.ui.constants.ValidationState;
import org.gwtbootstrap3.client.ui.form.error.BasicEditorError;
import org.gwtbootstrap3.client.ui.form.validator.Validator;
import org.gwtbootstrap3.client.ui.gwt.CellTable;
import org.gwtbootstrap3.client.ui.html.Span;
import org.gwtbootstrap3.client.ui.html.Text;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.editor.client.Editor;
import com.google.gwt.editor.client.EditorError;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;

public class TabWirelessUi extends Composite implements NetworkTab {

    private static final String AUTOMATIC_CHANNEL_DESCRIPTION = "Automatic";
    private static final String WORLD_REGION_COUNTRY_CODE = "00";

    private static final String STATUS_TABLE_ROW = "status-table-row";
    private static final String WIFI_MODE_STATION = GwtWifiWirelessMode.netWifiWirelessModeStation.name();
    private static final String WIFI_MODE_AP = GwtWifiWirelessMode.netWifiWirelessModeAccessPoint.name();
    private static final String WIFI_MODE_STATION_MESSAGE = MessageUtils.get(WIFI_MODE_STATION);
    private static final String WIFI_MODE_ACCESS_POINT_MESSAGE = MessageUtils.get(WIFI_MODE_AP);
    private static final String WIFI_SECURITY_WEP_MESSAGE = MessageUtils.get(GwtWifiSecurity.netWifiSecurityWEP.name());
    private static final String WIFI_SECURITY_WPA_MESSAGE = MessageUtils.get(GwtWifiSecurity.netWifiSecurityWPA.name());
    private static final String WIFI_SECURITY_WPA2_MESSAGE = MessageUtils
            .get(GwtWifiSecurity.netWifiSecurityWPA2.name());
    private static final String WIFI_SECURITY_WPA_WPA2_MESSAGE = MessageUtils
            .get(GwtWifiSecurity.netWifiSecurityWPA_WPA2.name());
    private static final String WIFI_BGSCAN_NONE_MESSAGE = MessageUtils
            .get(GwtWifiBgscanModule.netWifiBgscanMode_NONE.name());
    private static final String WIFI_CIPHERS_CCMP_TKIP_MESSAGE = MessageUtils
            .get(GwtWifiCiphers.netWifiCiphers_CCMP_TKIP.name());

    private static final String WIFI_RADIO_BGN = GwtWifiRadioMode.netWifiRadioModeBGN.name();
    private static final String WIFI_RADIO_ANAC = GwtWifiRadioMode.netWifiRadioModeANAC.name();
    private static final String WIFI_RADIO_BG = GwtWifiRadioMode.netWifiRadioModeBG.name();
    private static final String WIFI_RADIO_B = GwtWifiRadioMode.netWifiRadioModeB.name();
    private static final String WIFI_RADIO_A = GwtWifiRadioMode.netWifiRadioModeA.name();

    private static final String WIFI_RADIO_BGN_MESSAGE = MessageUtils.get(WIFI_RADIO_BGN);

    private static final String WIFI_BAND_5GHZ_MESSAGE = MessageUtils.get("netWifiBand5Ghz");
    private static final String WIFI_BAND_2GHZ_MESSAGE = MessageUtils.get("netWifiBand2Ghz");
    private static final String WIFI_BAND_BOTH_MESSAGE = MessageUtils.get("netWifiBandBoth");

    private static final String WIFI_SECURITY_NONE_MESSAGE = MessageUtils
            .get(GwtWifiSecurity.netWifiSecurityNONE.name());
    private static final String IPV4_STATUS_WAN_MESSAGE = MessageUtils
            .get(GwtNetIfStatus.netIPv4StatusEnabledWAN.name());

    private static TabWirelessUiUiBinder uiBinder = GWT.create(TabWirelessUiUiBinder.class);
    private static final Logger logger = Logger.getLogger(TabWirelessUi.class.getSimpleName());

    interface TabWirelessUiUiBinder extends UiBinder<Widget, TabWirelessUi> {
    }

    private static final Messages MSGS = GWT.create(Messages.class);

    private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);
    private final GwtNetworkServiceAsync gwtNetworkService = GWT.create(GwtNetworkService.class);

    private static final String REGEX_PASS_WPA = "^[ -~]{8,63}$";
    private static final String REGEX_PASS_WEP = "^(?:[\\x00-\\x7F]{5}|[\\x00-\\x7F]{13}|[a-fA-F0-9]{10}|[a-fA-F0-9]{26})$";
    private static final String REGEX_WIFI_SID = "^[^!#;+\\]/\"\\t][^+\\]/\"\\t]{0,31}$";
    private static final int MAX_SSID_LENGTH = 32;

    private final GwtSession session;
    private final TabTcpIpUi tcpTab;
    private final NetworkTabsUi netTabs;
    private final ListDataProvider<GwtWifiHotspotEntry> ssidDataProvider = new ListDataProvider<>();
    private final SingleSelectionModel<GwtWifiHotspotEntry> ssidSelectionModel = new SingleSelectionModel<>();

    private boolean dirty;
    private boolean ssidInit;
    private boolean isNet2;
    private GwtWifiNetInterfaceConfig selectedNetIfConfig;
    private String tcpStatus;

    GwtWifiConfig activeConfig;
    GwtWifiChannelModel previousSelection;

    @UiField
    Alert noChannels;
    @UiField
    Text noChannelsText;

    @UiField
    Form form;

    @UiField
    FormLabel labelWireless;
    @UiField
    FormLabel labelSsid;
    @UiField
    FormLabel labelRadio;
    @UiField
    FormLabel labelSecurity;
    @UiField
    FormLabel labelPassword;
    @UiField
    FormLabel labelVerify;
    @UiField
    FormLabel labelPairwise;
    @UiField
    FormLabel labelGroup;
    @UiField
    FormLabel labelBgscan;
    @UiField
    FormLabel labelRssi;
    @UiField
    FormLabel labelShortI;
    @UiField
    FormLabel labelLongI;
    @UiField
    FormLabel labelPing;
    @UiField
    FormLabel labelIgnore;
    @UiField
    FormLabel labelChannelList;
    @UiField
    FormLabel labelCountryCode;

    @UiField
    InlineRadio radio1;
    @UiField
    InlineRadio radio2;
    @UiField
    InlineRadio radio3;
    @UiField
    InlineRadio radio4;

    @UiField
    ListBox wireless;
    @UiField
    ListBox radio;
    @UiField
    ListBox security;
    @UiField
    ListBox pairwise;
    @UiField
    ListBox group;
    @UiField
    ListBox bgscan;
    @UiField
    ListBox channelList;

    @UiField
    TextBox ssid;
    @UiField
    TextBox shortI;
    @UiField
    TextBox longI;

    @UiField
    NewPasswordInput password;
    @UiField
    Input verify;

    @UiField
    TextBox rssi;

    @UiField
    TextBox countryCode;

    @UiField
    PanelHeader helpTitle;

    @UiField
    ScrollPanel helpText;

    @UiField
    Button buttonSsid;
    @UiField
    Button buttonPassword;

    @UiField
    FormGroup groupVerify;
    @UiField
    FormGroup groupRssi;
    @UiField
    FormGroup groupPassword;
    @UiField
    FormGroup groupWireless;
    @UiField
    FormGroup groupShortI;
    @UiField
    FormGroup groupLongI;

    @UiField
    HelpBlock helpWireless;
    @UiField
    HelpBlock helpPassword;
    @UiField
    HelpBlock helpVerify;
    @UiField
    HelpBlock helpShortI;
    @UiField
    HelpBlock helpLongI;

    @UiField
    Modal ssidModal;

    @UiField
    PanelHeader ssidTitle;

    @UiField
    CellTable<GwtWifiHotspotEntry> ssidGrid = new CellTable<>();

    @UiField
    Alert searching;
    @UiField
    Alert noSsid;
    @UiField
    Alert scanFail;

    @UiField
    Text searchingText;
    @UiField
    Text noSsidText;
    @UiField
    Text scanFailText;

    @UiField
    HelpButton wirelessHelp;
    @UiField
    HelpButton ssidHelp;
    @UiField
    HelpButton radioHelp;
    @UiField
    HelpButton securityHelp;
    @UiField
    HelpButton passwordHelp;
    @UiField
    HelpButton verifyHelp;
    @UiField
    HelpButton pairwiseHelp;
    @UiField
    HelpButton groupHelp;
    @UiField
    HelpButton bgscanHelp;
    @UiField
    HelpButton rssiHelp;
    @UiField
    HelpButton shortIHelp;
    @UiField
    HelpButton longIHelp;
    @UiField
    HelpButton pingHelp;
    @UiField
    HelpButton ignoreHelp;
    @UiField
    HelpButton channelListHelp;
    @UiField
    HelpButton countryCodeHelp;
    @UiField
    Modal regDomErrorModal;
    @UiField
    Alert unavailableChannelError;
    @UiField
    Text unavailableChannelErrorText;

    public TabWirelessUi(GwtSession currentSession, TabTcpIpUi tcp, NetworkTabsUi tabs) {
        this.ssidInit = false;
        initWidget(uiBinder.createAndBindUi(this));
        this.session = currentSession;
        this.tcpTab = tcp;
        this.netTabs = tabs;

        detectIfNet2();

        initForm();
        initHelpButtons();
        initRegDomErrorModal();
        setPasswordValidation();

        this.tcpTab.status.addChangeHandler(event -> {
            if (TabWirelessUi.this.selectedNetIfConfig != null) {
                // set the default values for wireless mode if tcp/ip status was changed
                String tcpIpStatus = TabWirelessUi.this.tcpTab.getStatus();
                if (!tcpIpStatus.equals(TabWirelessUi.this.tcpStatus)) {
                    if (tcpIpStatus.equals(MessageUtils.get(GwtNetIfStatus.netIPv4StatusEnabledWAN.name()))) {
                        TabWirelessUi.this.activeConfig = TabWirelessUi.this.selectedNetIfConfig.getStationWifiConfig();
                    } else {
                        TabWirelessUi.this.activeConfig = TabWirelessUi.this.selectedNetIfConfig.getActiveWifiConfig();
                    }

                    TabWirelessUi.this.tcpStatus = tcpIpStatus;
                    TabWirelessUi.this.netTabs.adjustInterfaceTabs();
                }
            }

            update();
        });

        logger.info("Constructor done.");
    }

    @UiHandler(value = { "wireless", "ssid", "radio", "security", "password", "verify", "pairwise", "group", "bgscan",
            "longI", "shortI", "radio1", "radio2", "radio3", "radio4", "rssi", "channelList", "countryCode" })
    public void onChange(ChangeEvent e) {
        setDirty(true);
    }

    public GwtWifiWirelessMode getWirelessMode() {
        if (this.wireless != null) {
            for (GwtWifiWirelessMode mode : GwtWifiWirelessMode.values()) {
                if (this.wireless.getSelectedItemText().equals(MessageUtils.get(mode.name()))) {
                    return mode;
                }
            }
        } else {
            if (this.activeConfig != null) {
                return GwtWifiWirelessMode.valueOf(this.activeConfig.getWirelessMode());
            }
        }
        return null;
    }

    @Override
    public void setDirty(boolean flag) {
        this.dirty = flag;
        if (this.netTabs.getButtons() != null) {
            this.netTabs.getButtons().setButtonsDirty(flag);
        }
    }

    @Override
    public boolean isDirty() {
        return this.dirty;
    }

    @Override
    public boolean isValid() {
        boolean result = this.form.validate();
        result &= checkPassword();

        result = result && !this.groupWireless.getValidationState().equals(ValidationState.ERROR)
                && !this.groupPassword.getValidationState().equals(ValidationState.ERROR)
                && !this.groupVerify.getValidationState().equals(ValidationState.ERROR);

        result = result && !this.groupShortI.getValidationState().equals(ValidationState.ERROR)
                && !this.groupLongI.getValidationState().equals(ValidationState.ERROR);

        return result;
    }

    @Override
    public void setNetInterface(GwtNetInterfaceConfig config) {
        logger.info("Start setNetInterface");

        setDirty(true);
        if (this.tcpStatus == null || this.selectedNetIfConfig != config) {
            this.tcpStatus = this.tcpTab.getStatus();
        }
        if (config instanceof GwtWifiNetInterfaceConfig) {
            this.selectedNetIfConfig = (GwtWifiNetInterfaceConfig) config;
            logger.info("Selected ifconfig: " + selectedNetIfConfig.getProperties());

            this.activeConfig = this.selectedNetIfConfig.getActiveWifiConfig();
            if (Objects.nonNull(this.activeConfig)) {
                logger.info("Active config: " + this.activeConfig.getProperties());
                logger.info("Wireless mode: " + this.activeConfig.getWirelessMode());
                logger.info("Channels " + this.activeConfig.getChannels());
                if (this.activeConfig.getChannels() != null
                        && !TabWirelessUi.this.activeConfig.getChannels().isEmpty()) {
                    updateChanneList(TabWirelessUi.this.activeConfig);
                }
            }
            loadCountryCode();
        }

        logger.info("Finished setNetInterface");
    }

    private void updateChanneList(GwtWifiConfig config) {
        int channelToSelect = 0;
        if (config.getChannels() != null) {
            channelToSelect = config.getChannels().get(0);
        }

        this.channelList.setSelectedIndex(getChannelIndexFromValue(channelToSelect));

    }

    @Override
    public void refresh() {
        if (isDirty()) {
            setDirty(false);
            if (this.selectedNetIfConfig == null) {
                reset();
            } else {
                update();
            }
        }

    }

    @Override
    public void getUpdatedNetInterface(GwtNetInterfaceConfig updatedNetIf) {
        GwtWifiNetInterfaceConfig updatedWifiNetIf = (GwtWifiNetInterfaceConfig) updatedNetIf;

        if (this.session != null) {
            GwtWifiConfig updatedWifiConfig = getGwtWifiConfig();
            updatedWifiNetIf.setWirelessMode(updatedWifiConfig.getWirelessMode());

            // update the wifi config
            updatedWifiNetIf.setWifiConfig(updatedWifiConfig);
        } else {
            if (this.selectedNetIfConfig != null) {
                updatedWifiNetIf.setAccessPointWifiConfig(this.selectedNetIfConfig.getAccessPointWifiConfigProps());
                updatedWifiNetIf.setStationWifiConfig(this.selectedNetIfConfig.getStationWifiConfigProps());

                // select the correct mode
                for (GwtWifiWirelessMode mode : GwtWifiWirelessMode.values()) {
                    if (mode.name().equals(this.selectedNetIfConfig.getWirelessMode())) {
                        updatedWifiNetIf.setWirelessMode(mode.name());
                    }
                }
            }
        }
    }

    @Override
    public void clear() {
        // Not needed
    }

    // -----Private methods-------//

    private void detectIfNet2() {
        this.gwtNetworkService.isNet2(new AsyncCallback<Boolean>() {

            @Override
            public void onFailure(Throwable caught) {
                FailureHandler.handle(caught);
            }

            @Override
            public void onSuccess(Boolean isNet2) {
                TabWirelessUi.this.isNet2 = isNet2;
                changeRadioModeToBand(isNet2);
            }
        });
    }

    private void changeRadioModeToBand(boolean isNet2) {
        if (isNet2) {
            this.radioHelp.setHelpText(MSGS.netWifiToolTipBand());
            this.labelRadio.setText(MSGS.netWifiBand());
        }
    }

    private void update() {
        setValues();
        refreshForm();
        setPasswordValidation();
    }

    private void setValues() {
        logger.info("Start setValues");

        if (this.activeConfig == null) {
            return;
        }

        for (int i = 0; i < this.wireless.getItemCount(); i++) {
            if (this.wireless.getItemText(i).equals(MessageUtils.get(this.activeConfig.getWirelessMode()))) {
                this.wireless.setSelectedIndex(i);
            }
        }

        this.ssid.setValue(GwtSafeHtmlUtils.htmlUnescape(this.activeConfig.getWirelessSsid()));

        // ------------

        setRadioModeByValue(this.activeConfig.getRadioMode());

        // select proper channels
        updateChanneList(this.activeConfig);

        String activeSecurity = this.activeConfig.getSecurity();
        if (activeSecurity != null) {
            for (int i = 0; i < this.security.getItemCount(); i++) {
                if (this.security.getItemText(i).equals(MessageUtils.get(activeSecurity))) {
                    this.security.setSelectedIndex(i);
                    break;
                }
            }
        }

        String activePairwiseCiphers = this.activeConfig.getPairwiseCiphers();
        if (activePairwiseCiphers != null) {
            for (int i = 0; i < this.pairwise.getItemCount(); i++) {
                if (this.pairwise.getItemText(i).equals(MessageUtils.get(activePairwiseCiphers))) {
                    this.pairwise.setSelectedIndex(i);
                    break;
                }
            }
        }

        String activeGroupCiphers = this.activeConfig.getPairwiseCiphers();
        if (activeGroupCiphers != null) {
            for (int i = 0; i < this.group.getItemCount(); i++) {
                if (this.group.getItemText(i).equals(MessageUtils.get(activeGroupCiphers))) { // activeConfig.getGroupCiphers()
                    this.group.setSelectedIndex(i);
                    break;
                }
            }
        }

        String activeBgscanModule = this.activeConfig.getBgscanModule();
        if (activeBgscanModule != null) {
            for (int i = 0; i < this.bgscan.getItemCount(); i++) {
                if (this.bgscan.getItemText(i).equals(MessageUtils.get(activeBgscanModule))) {
                    this.bgscan.setSelectedIndex(i);
                    break;
                }
            }
        }

        this.rssi.setValue(String.valueOf(this.activeConfig.getBgscanRssiThreshold()));
        this.shortI.setValue(String.valueOf(this.activeConfig.getBgscanShortInterval()));
        this.longI.setValue(String.valueOf(this.activeConfig.getBgscanLongInterval()));
        this.password.setValue(this.activeConfig.getPassword());
        this.verify.setValue(this.activeConfig.getPassword());
        this.radio1.setValue(this.activeConfig.pingAccessPoint());
        this.radio2.setValue(!this.activeConfig.pingAccessPoint());
        this.radio3.setValue(this.activeConfig.ignoreSSID());
        this.radio4.setValue(!this.activeConfig.ignoreSSID());

        logger.info("Finished setValues");
    }

    private void setRadioModeByValue(String radioModeValue) {
        if (radioModeValue != null) {

            if (this.isNet2) {
                setBandFromRadioMode(radioModeValue);
            } else {
                for (int i = 0; i < this.radio.getItemCount(); i++) {
                    if (this.radio.getItemText(i).equals(MessageUtils.get(radioModeValue))
                            || this.radio.getItemText(i).equals(radioModeValue)) {
                        this.radio.setSelectedIndex(i);
                        break;
                    }
                }
            }
        }
    }

    private void setBandFromRadioMode(String radioModeValue) {
        boolean isValue5GHz = radioModeValue.equals(WIFI_RADIO_ANAC)
                || radioModeValue.equals(WIFI_RADIO_A);
        boolean isValue2GHz = radioModeValue.equals(WIFI_RADIO_BG)
                || radioModeValue.equals(WIFI_RADIO_B);
        boolean isValueBoth = radioModeValue.equals(WIFI_RADIO_BGN);

        for (int i = 0; i < this.radio.getItemCount(); i++) {
            String selectedText = this.radio.getItemText(i);

            if (selectedText.equals(WIFI_BAND_5GHZ_MESSAGE) && isValue5GHz) {
                this.radio.setSelectedIndex(i);
            }

            if (selectedText.equals(WIFI_BAND_2GHZ_MESSAGE) && isValue2GHz) {
                this.radio.setSelectedIndex(i);
            }

            if (selectedText.equals(WIFI_BAND_BOTH_MESSAGE) && isValueBoth) {
                this.radio.setSelectedIndex(i);
            }
        }
    }

    private void refreshForm() {
        logger.info("Start refreshForm");

        String tcpipStatus = this.tcpTab.getStatus();

        // Tcp/IP disabled
        if (tcpipStatus.equals(GwtNetIfStatus.netIPv4StatusDisabled.name())) {
            setForm(false);
        } else {
            setForm(true);
            // Station mode
            if (WIFI_MODE_STATION_MESSAGE.equals(this.wireless.getSelectedItemText())) {

                if (tcpipStatus.equals(IPV4_STATUS_WAN_MESSAGE)) {
                    this.wireless.setEnabled(false);
                }
                this.groupVerify.setVisible(false);

            } else if (WIFI_MODE_ACCESS_POINT_MESSAGE.equals(this.wireless.getSelectedItemText())) {
                // access point mode
                // disable access point when TCP/IP is set to WAN
                if (tcpipStatus.equals(IPV4_STATUS_WAN_MESSAGE)) {
                    setForm(false);
                }

                this.groupVerify.setVisible(true);
            }

            this.radio.setEnabled(true);
            this.channelList.setEnabled(true);
            loadRadioMode();
            
            if (this.isNet2) {
                this.buttonPassword.setVisible(false);
            }

            // disable Password if security is none
            if (this.security.getSelectedItemText().equals(WIFI_SECURITY_NONE_MESSAGE)) {
                this.password.setEnabled(false);
                this.verify.setEnabled(false);
                this.buttonPassword.setEnabled(false);
            }

            if (WIFI_MODE_STATION_MESSAGE.equals(this.wireless.getSelectedItemText())) {
                this.ssid.setEnabled(true);
                this.buttonSsid.setEnabled(true);
                this.verify.setEnabled(false);
                if (!this.security.getSelectedItemText().equals(WIFI_SECURITY_NONE_MESSAGE)) {
                    if (this.password.getValue() != null && this.password.getValue().length() > 0) {
                        this.password.setEnabled(true);
                        this.buttonPassword.setEnabled(true);
                    } else {
                        this.password.setEnabled(true);
                        this.buttonPassword.setEnabled(false);
                    }
                }

                this.bgscan.setEnabled(true);

                if (this.bgscan.getSelectedItemText()
                        .equals(MessageUtils.get(GwtWifiBgscanModule.netWifiBgscanMode_SIMPLE.name()))
                        || this.bgscan.getSelectedItemText()
                                .equals(MessageUtils.get(GwtWifiBgscanModule.netWifiBgscanMode_LEARN.name()))) {
                    this.rssi.setEnabled(true);
                    this.shortI.setEnabled(true);
                    this.longI.setEnabled(true);
                } else {
                    this.rssi.setEnabled(false);
                    this.shortI.setEnabled(false);
                    this.longI.setEnabled(false);
                }
            } else {
                this.ssid.setEnabled(true);
                this.buttonSsid.setEnabled(false);
                if (!this.security.getSelectedItemText().equals(WIFI_SECURITY_NONE_MESSAGE)) {
                    this.password.setEnabled(true);
                    this.buttonPassword.setEnabled(false);
                }
                this.bgscan.setEnabled(false);
                this.rssi.setEnabled(false);
                this.shortI.setEnabled(false);
                this.longI.setEnabled(false);
                this.radio1.setEnabled(false);
                this.radio2.setEnabled(false);
            }

            if (this.security.getSelectedItemText().equals(WIFI_SECURITY_WPA2_MESSAGE)
                    || this.security.getSelectedItemText().equals(WIFI_SECURITY_WPA_MESSAGE)
                    || this.security.getSelectedItemText()
                            .equals(MessageUtils.get(GwtWifiSecurity.netWifiSecurityWPA_WPA2.name()))) {
                if (WIFI_MODE_STATION_MESSAGE.equals(this.wireless.getSelectedItemText())) {
                    this.pairwise.setEnabled(true);
                    this.group.setEnabled(true);
                } else {
                    this.pairwise.setEnabled(true);
                    this.group.setEnabled(false);
                }
            } else {
                this.pairwise.setEnabled(false);
                this.group.setEnabled(false);
            }
        }

        this.netTabs.adjustInterfaceTabs();

        logger.info("Finish refreshForm");
    }

    private void reset() {

        for (int i = 0; i < this.wireless.getItemCount(); i++) {
            if (this.wireless.getSelectedItemText().equals(WIFI_MODE_STATION_MESSAGE)) {
                this.wireless.setSelectedIndex(i);
            }
        }
        this.ssid.setText("");
        for (int i = 0; i < this.radio.getItemCount(); i++) {
            if (this.radio.getItemText(i).equals(WIFI_RADIO_BGN_MESSAGE)) {
                this.radio.setSelectedIndex(i);
            }
        }

        for (int i = 0; i < this.security.getItemCount(); i++) {
            if (this.security.getItemText(i).equals(WIFI_SECURITY_WPA2_MESSAGE)) {
                this.security.setSelectedIndex(i);
            }
        }

        this.password.setText("");
        this.verify.setText("");

        for (int i = 0; i < this.pairwise.getItemCount(); i++) {
            if (this.pairwise.getItemText(i).equals(WIFI_CIPHERS_CCMP_TKIP_MESSAGE)) {
                this.pairwise.setSelectedIndex(i);
            }
        }

        for (int i = 0; i < this.group.getItemCount(); i++) {
            if (this.group.getItemText(i).equals(WIFI_CIPHERS_CCMP_TKIP_MESSAGE)) {
                this.group.setSelectedIndex(i);
            }
        }

        for (int i = 0; i < this.bgscan.getItemCount(); i++) {
            if (this.bgscan.getItemText(i).equals(WIFI_BGSCAN_NONE_MESSAGE)) {
                this.bgscan.setSelectedIndex(i);
            }
        }

        this.rssi.setValue("");
        this.shortI.setValue("");
        this.longI.setValue("");
        this.radio2.setValue(true);
        this.radio4.setValue(true);

        update();
    }

    private void initHelpButtons() {
        this.wirelessHelp.setHelpTextProvider(() -> {
            if (TabWirelessUi.this.wireless.getSelectedItemText().equals(MessageUtils.get(WIFI_MODE_STATION))) {
                return MSGS.netWifiToolTipWirelessModeStation();
            } else {
                return MSGS.netWifiToolTipWirelessModeAccessPoint();
            }
        });
        this.ssidHelp.setHelpText(MSGS.netWifiToolTipNetworkName());

        if (!this.isNet2) {
            this.radioHelp.setHelpText(MSGS.netWifiToolTipRadioMode());
        } else {
            this.radioHelp.setHelpText(MSGS.netWifiToolTipBand());
        }
        this.securityHelp.setHelpText(MSGS.netWifiToolTipSecurity());
        this.passwordHelp.setHelpText(MSGS.netWifiToolTipPassword());
        this.verifyHelp.setHelpText(MSGS.netWifiToolTipPassword());
        this.pairwiseHelp.setHelpText(MSGS.netWifiToolTipPairwiseCiphers());
        this.groupHelp.setHelpText(MSGS.netWifiToolTipGroupCiphers());
        this.bgscanHelp.setHelpText(MSGS.netWifiToolTipBgScan());
        this.rssiHelp.setHelpText(MSGS.netWifiToolTipBgScanStrength());
        this.shortIHelp.setHelpText(MSGS.netWifiToolTipBgScanShortInterval());
        this.longIHelp.setHelpText(MSGS.netWifiToolTipBgScanLongInterval());
        this.pingHelp.setHelpText(MSGS.netWifiToolTipPingAccessPoint());
        this.ignoreHelp.setHelpText(MSGS.netWifiToolTipIgnoreSSID());
        this.channelListHelp.setHelpText(MSGS.netWifiToolTipChannelList());
        this.countryCodeHelp.setHelpText(MSGS.netWifiToolTipCountryCode());
    }

    private void initForm() {
        logger.info("Start initForm");

        // Wireless Mode

        this.labelWireless.setText(MSGS.netWifiWirelessMode());
        this.wireless.addItem(WIFI_MODE_STATION_MESSAGE);
        this.wireless.addItem(WIFI_MODE_ACCESS_POINT_MESSAGE);
        this.wireless.addMouseOverHandler(event -> {
            if (TabWirelessUi.this.wireless.getSelectedItemText().equals(MessageUtils.get(WIFI_MODE_STATION_MESSAGE))) {
                TabWirelessUi.this.helpText.clear();
                TabWirelessUi.this.helpText.add(new Span(MSGS.netWifiToolTipWirelessModeStation()));
            } else {
                TabWirelessUi.this.helpText.clear();
                TabWirelessUi.this.helpText.add(new Span(MSGS.netWifiToolTipWirelessModeAccessPoint()));
            }
        });
        this.wireless.addMouseOutHandler(event -> resetHelp());

        this.wireless.addChangeHandler(event -> {
            setDirty(true);
            TabWirelessUi.this.helpWireless.setText("");
            TabWirelessUi.this.groupWireless.setValidationState(ValidationState.NONE);

            if (TabWirelessUi.this.wireless.getSelectedItemText().equals(WIFI_MODE_STATION_MESSAGE)) {
                TabWirelessUi.this.activeConfig = TabWirelessUi.this.selectedNetIfConfig.getStationWifiConfig();
            } else {
                // use values from access point config
                TabWirelessUi.this.activeConfig = TabWirelessUi.this.selectedNetIfConfig.getAccessPointWifiConfig();
            }
            TabWirelessUi.this.netTabs.adjustInterfaceTabs();
            update();
            checkPassword();
            TabWirelessUi.this.wirelessHelp.updateHelpText();
        });

        // SSID
        this.labelSsid.setText(MSGS.netWifiNetworkName());
        this.labelSsid.setShowRequiredIndicator(true);
        this.ssid.setMaxLength(MAX_SSID_LENGTH);
        this.ssid.setAllowBlank(false);
        this.ssid.addValidator(GwtValidators.regex(REGEX_WIFI_SID, MSGS.netWifiWirelessInvalidSSID()));
        this.ssid.setValidateOnBlur(true);
        this.ssid.addMouseOverHandler(event -> {
            if (TabWirelessUi.this.ssid.isEnabled()) {
                TabWirelessUi.this.helpText.clear();
                TabWirelessUi.this.helpText.add(new Span(MSGS.netWifiToolTipNetworkName()));
            }
        });
        this.ssid.addMouseOutHandler(event -> resetHelp());
        this.buttonSsid.addClickHandler(event -> {
            if (!TabWirelessUi.this.ssidInit) {
                initSsid();
                TabWirelessUi.this.ssidDataProvider.getList().clear();
                TabWirelessUi.this.searching.setVisible(true);
                TabWirelessUi.this.noSsid.setVisible(false);
                TabWirelessUi.this.ssidGrid.setVisible(false);
                TabWirelessUi.this.scanFail.setVisible(false);
            }
            initModal();
            loadSsidData();
        });

        // Radio Mode
        if (!this.isNet2) {
            this.labelRadio.setText(MSGS.netWifiRadioMode());
        } else {
            this.labelRadio.setText(MSGS.netWifiBand());
        }
        this.radio.addMouseOverHandler(event -> {
            if (TabWirelessUi.this.radio.isEnabled()) {
                TabWirelessUi.this.helpText.clear();

                if (!TabWirelessUi.this.isNet2) {
                    TabWirelessUi.this.helpText.add(new Span(MSGS.netWifiToolTipRadioMode()));
                } else {
                    TabWirelessUi.this.helpText.add(new Span(MSGS.netWifiToolTipBand()));
                }
            }
        });
        this.radio.addMouseOutHandler(event -> resetHelp());
        this.radio.addChangeHandler(event -> {
            setDirty(true);
            refreshForm();
        });

        // Wireless Security
        this.labelSecurity.setText(MSGS.netWifiWirelessSecurity());
        this.security.addMouseOverHandler(event -> {
            if (TabWirelessUi.this.security.isEnabled()) {
                TabWirelessUi.this.helpText.clear();
                TabWirelessUi.this.helpText.add(new Span(MSGS.netWifiToolTipSecurity()));
            }
        });
        this.security.addMouseOutHandler(event -> resetHelp());
        for (GwtWifiSecurity mode : GwtWifiSecurity.values()) {
            this.security.addItem(MessageUtils.get(mode.name()));
        }
        this.security.addChangeHandler(event -> {
            setDirty(true);
            setPasswordValidation();
            refreshForm();
            checkPassword();
        });

        // Password
        this.labelPassword.setText(MSGS.netWifiWirelessPassword());
        this.password.addMouseOverHandler(event -> {
            if (TabWirelessUi.this.password.isEnabled()) {
                TabWirelessUi.this.helpText.clear();
                TabWirelessUi.this.helpText.add(new Span(MSGS.netWifiToolTipPassword()));
            }
        });

        this.password.addBlurHandler(e -> this.password.validate());
        this.password.setAllowBlank(false);
        this.password.addMouseOutHandler(event -> resetHelp());
        
        this.buttonPassword.addClickHandler(event -> {
            EntryClassUi.showWaitModal();
            TabWirelessUi.this.buttonPassword.setEnabled(false);
            final GwtWifiConfig gwtWifiConfig = getGwtWifiConfig();
            TabWirelessUi.this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

                @Override
                public void onFailure(Throwable ex) {
                    FailureHandler.handle(ex);
                }

                @Override
                public void onSuccess(GwtXSRFToken token) {
                    TabWirelessUi.this.gwtNetworkService.verifyWifiCredentials(token,
                            TabWirelessUi.this.selectedNetIfConfig.getName(), gwtWifiConfig,
                            new AsyncCallback<Boolean>() {

                                @Override
                                public void onFailure(Throwable caught) {
                                    FailureHandler.handle(caught);
                                    EntryClassUi.hideWaitModal();
                                    TabWirelessUi.this.buttonPassword.setEnabled(true);
                                    showPasswordVerificationStatus(MSGS.netWifiPasswordVerificationFailed());
                                }

                                @Override
                                public void onSuccess(Boolean result) {
                                    if (!result.booleanValue()) {
                                        showPasswordVerificationStatus(MSGS.netWifiPasswordVerificationFailed());
                                    } else {
                                        showPasswordVerificationStatus(MSGS.netWifiPasswordVerificationSuccess());
                                    }
                                    EntryClassUi.hideWaitModal();
                                    TabWirelessUi.this.buttonPassword.setEnabled(true);
                                }
                            });
                }

            });
        });
        this.password.addKeyUpHandler(event -> {
            this.password.validate();

            if (TabWirelessUi.this.groupVerify.isVisible()
                    && !TabWirelessUi.this.verify.getText().equals(TabWirelessUi.this.password.getText())) {
                TabWirelessUi.this.groupVerify.setValidationState(ValidationState.ERROR);
            } else {
                TabWirelessUi.this.groupVerify.setValidationState(ValidationState.NONE);
            }
        });
        this.password.addChangeHandler(event -> {
            refreshForm();
            checkPassword();
        });

        // Verify Password
        this.labelVerify.setText(MSGS.netWifiWirelessVerifyPassword());
        this.verify.addMouseOverHandler(event -> {
            if (TabWirelessUi.this.verify.isEnabled()) {
                TabWirelessUi.this.helpText.clear();
                TabWirelessUi.this.helpText.add(new Span(MSGS.netWifiToolTipPassword()));
            }
        });
        this.verify.addMouseOutHandler(event -> resetHelp());
        this.verify.addChangeHandler(event -> {
            refreshForm();
            checkPassword();
        });

        this.verify.addKeyUpHandler(event -> {
            refreshForm();
            checkPassword();
        });

        // Pairwise ciphers
        this.labelPairwise.setText(MSGS.netWifiWirelessPairwiseCiphers());
        this.pairwise.addMouseOverHandler(event -> {
            if (TabWirelessUi.this.pairwise.isEnabled()) {
                TabWirelessUi.this.helpText.clear();
                TabWirelessUi.this.helpText.add(new Span(MSGS.netWifiToolTipPairwiseCiphers()));
            }
        });
        this.pairwise.addMouseOutHandler(event -> resetHelp());
        for (GwtWifiCiphers cipher : GwtWifiCiphers.values()) {
            if (GwtWifiCiphers.netWifiCiphers_NONE == cipher) {
                continue;
            }
            this.pairwise.addItem(MessageUtils.get(cipher.name()));
        }
        this.pairwise.addChangeHandler(event -> {
            setDirty(true);
            refreshForm();
        });

        // Groupwise Ciphers
        this.labelGroup.setText(MSGS.netWifiWirelessGroupCiphers());
        this.group.addMouseOverHandler(event -> {
            if (TabWirelessUi.this.group.isEnabled()) {
                TabWirelessUi.this.helpText.clear();
                TabWirelessUi.this.helpText.add(new Span(MSGS.netWifiToolTipGroupCiphers()));
            }
        });
        this.group.addMouseOutHandler(event -> resetHelp());
        for (GwtWifiCiphers cipher : GwtWifiCiphers.values()) {
            if (GwtWifiCiphers.netWifiCiphers_NONE == cipher) {
                continue;
            }
            this.group.addItem(MessageUtils.get(cipher.name()));
        }
        this.group.addChangeHandler(event -> {
            setDirty(true);
            refreshForm();
        });

        // Bgscan module
        this.labelBgscan.setText(MSGS.netWifiWirelessBgscanModule());
        this.bgscan.addMouseOverHandler(event -> {
            if (TabWirelessUi.this.bgscan.isEnabled()) {
                TabWirelessUi.this.helpText.clear();
                TabWirelessUi.this.helpText.add(new Span(MSGS.netWifiToolTipBgScan()));
            }
        });
        this.bgscan.addMouseOutHandler(event -> resetHelp());
        for (GwtWifiBgscanModule module : GwtWifiBgscanModule.values()) {
            this.bgscan.addItem(MessageUtils.get(module.name()));
        }
        this.bgscan.addChangeHandler(event -> {
            setDirty(true);
            refreshForm();
        });

        // BgScan RSSI threshold
        this.labelRssi.setText(MSGS.netWifiWirelessBgscanSignalStrengthThreshold());
        this.rssi.addMouseOverHandler(event -> {
            if (this.rssi.isEnabled()) {
                this.helpText.clear();
                this.helpText.add(new Span(MSGS.netWifiToolTipBgScanStrength()));
            }
        });

        // Bgscan short Interval
        this.labelShortI.setText(MSGS.netWifiWirelessBgscanShortInterval());
        this.shortI.addMouseOverHandler(event -> {
            if (TabWirelessUi.this.shortI.isEnabled()) {
                TabWirelessUi.this.helpText.clear();
                TabWirelessUi.this.helpText.add(new Span(MSGS.netWifiToolTipBgScanShortInterval()));
            }
        });
        this.shortI.addMouseOutHandler(event -> resetHelp());
        this.shortI.addValidator(newBgScanValidator(this.shortI));
        this.shortI.addChangeHandler(event -> this.longI.validate());

        // Bgscan long interval
        this.labelLongI.setText(MSGS.netWifiWirelessBgscanLongInterval());
        this.longI.addValidator(newBgScanValidator(this.longI));
        this.longI.addChangeHandler(event -> this.shortI.validate());
        this.longI.addMouseOverHandler(event -> {
            if (TabWirelessUi.this.longI.isEnabled()) {
                TabWirelessUi.this.helpText.clear();
                TabWirelessUi.this.helpText.add(new Span(MSGS.netWifiToolTipBgScanLongInterval()));
            }
        });
        this.longI.addMouseOutHandler(event -> resetHelp());

        // Ping Access Point ----
        this.labelPing.setText(MSGS.netWifiWirelessPingAccessPoint());
        this.radio1.setText(MSGS.trueLabel());
        this.radio1.addMouseOverHandler(event -> {
            if (TabWirelessUi.this.radio1.isEnabled()) {
                TabWirelessUi.this.helpText.clear();
                TabWirelessUi.this.helpText.add(new Span(MSGS.netWifiToolTipPingAccessPoint()));
            }
        });
        this.radio1.addMouseOutHandler(event -> resetHelp());
        this.radio1.addChangeHandler(event -> setDirty(true));
        this.radio2.setText(MSGS.falseLabel());
        this.radio2.addMouseOverHandler(event -> {
            if (TabWirelessUi.this.radio2.isEnabled()) {
                TabWirelessUi.this.helpText.clear();
                TabWirelessUi.this.helpText.add(new Span(MSGS.netWifiToolTipPingAccessPoint()));
            }
        });
        this.radio2.addMouseOutHandler(event -> resetHelp());
        this.radio2.addChangeHandler(event -> setDirty(true));

        // Ignore Broadcast SSID
        this.labelIgnore.setText(MSGS.netWifiWirelessIgnoreSSID());
        this.radio3.setText(MSGS.trueLabel());
        this.radio3.addMouseOverHandler(event -> {
            if (TabWirelessUi.this.radio3.isEnabled()) {
                TabWirelessUi.this.helpText.clear();
                TabWirelessUi.this.helpText.add(new Span(MSGS.netWifiToolTipIgnoreSSID()));
            }
        });
        this.radio3.addMouseOutHandler(event -> resetHelp());
        this.radio3.addChangeHandler(event -> setDirty(true));
        this.radio4.setText(MSGS.falseLabel());
        this.radio4.addMouseOverHandler(event -> {
            if (TabWirelessUi.this.radio4.isEnabled()) {
                TabWirelessUi.this.helpText.clear();
                TabWirelessUi.this.helpText.add(new Span(MSGS.netWifiToolTipIgnoreSSID()));
            }
        });
        this.radio4.addMouseOutHandler(event -> resetHelp());
        this.radio4.addChangeHandler(event -> setDirty(true));

        // Channel List
        this.labelChannelList.setText(MSGS.netWifiChannelList());
        this.channelList.addMouseOverHandler(event -> {
            if (TabWirelessUi.this.channelList.isEnabled()) {
                TabWirelessUi.this.helpText.clear();
                TabWirelessUi.this.helpText.add(new Span(MSGS.netWifiToolTipChannelList()));
            }
        });
        this.channelList.addChangeHandler(event -> {
            TabWirelessUi.this.activeConfig.setChannels(
                    Collections.singletonList(getChannelValueByIndex(this.channelList.getSelectedIndex())));
            TabWirelessUi.this.activeConfig
                    .setChannelsFrequency(getChannelFrequencyByIndex(this.channelList.getSelectedIndex()));
        });
        this.channelList.addMouseOutHandler(event -> resetHelp());
        this.helpTitle.setText(MSGS.netHelpTitle());

        // Country Code
        this.labelCountryCode.setText(MSGS.netWifiCountryCodeLabel());

        this.noChannelsText.setText(MSGS.netWifiAlertNoChannels());

        logger.info("Finish initForm");
    }

    private void initRegDomErrorModal() {
        this.regDomErrorModal.setTitle(MSGS.error());
        this.unavailableChannelErrorText.setText(MSGS.netWifiChannelMissingError());
        this.regDomErrorModal.addHideHandler(evt -> this.setDirty(true));
    }

    private List<GwtWifiHotspotEntry> getChannelFrequencyByIndex(int selectedIndex) {
        String selectedItem = this.channelList.getItemText(selectedIndex);

        GwtWifiHotspotEntry frequencyEntry = new GwtWifiHotspotEntry();
        
        if (selectedItem.equals(AUTOMATIC_CHANNEL_DESCRIPTION)) {
            frequencyEntry.setChannel(0);
            frequencyEntry.setFrequency(0);
        } else {
            String[] itemtext = selectedItem.split(" ");
            frequencyEntry.setChannel(Integer.parseInt(itemtext[1]));
            frequencyEntry.setFrequency(Integer.parseInt(itemtext[3]));
        }

        return Collections.singletonList(frequencyEntry);
    }

    private Validator<String> newBgScanValidator(TextBox field) {
        return new Validator<String>() {

            @Override
            public List<EditorError> validate(Editor<String> editor, String value) {
                List<EditorError> result = new ArrayList<>();
                if (TabWirelessUi.this.shortI.isEnabled() && TabWirelessUi.this.longI.isEnabled()) {
                    try {
                        if (field.getText().trim().contains(".") || field.getText().trim().contains("-")
                                || !field.getText().trim().matches("[0-9]+")) {
                            result.add(new BasicEditorError(field, value, MSGS.netWifiBgScanInterval()));
                        } else if (Integer.parseInt(TabWirelessUi.this.shortI.getText().trim()) >= Integer
                                .parseInt(TabWirelessUi.this.longI.getText().trim())) {
                            result.add(new BasicEditorError(field, value, MSGS.netWifiBgScanIntervalValues()));
                        }
                    } catch (NumberFormatException e) {
                        result.add(new BasicEditorError(field, value, MSGS.deviceConfigError()));
                    }
                }
                return result;
            }

            @Override
            public int getPriority() {
                return 0;
            }
        };
    }

    private void resetHelp() {
        this.helpText.clear();
        this.helpText.add(new Span(MSGS.netHelpDefaultHint()));
    }

    private void loadCountryCode() {

        this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onFailure(Throwable ex) {
                FailureHandler.handle(ex);
            }

            @Override
            public void onSuccess(GwtXSRFToken token) {
                TabWirelessUi.this.gwtNetworkService.getWifiCountryCode(token, new AsyncCallback<String>() {

                    @Override
                    public void onFailure(Throwable caught) {
                        logger.info("getWifiCountryCode Failure");
                    }

                    @Override
                    public void onSuccess(String countryCode) {
                        logger.info("getWifiCountryCode Success - " + countryCode);
                        TabWirelessUi.this.countryCode.setText(countryCode);
                        TabWirelessUi.this.activeConfig.setCountryCode(countryCode);

                        loadRadioMode();
                    }
                });
            }
        });

    }

    private void setPasswordValidation() {

        final GwtConsoleUserOptions configUserOptions = EntryClassUi.getUserOptions();

        if (getWirelessMode() != GwtWifiWirelessMode.netWifiWirelessModeAccessPoint) {
            configUserOptions.allowAnyPassword();
        }

        if (this.security.getSelectedItemText().equals(WIFI_SECURITY_WPA_MESSAGE)
                || this.security.getSelectedItemText().equals(WIFI_SECURITY_WPA2_MESSAGE)
                || this.security.getSelectedItemText().contentEquals(WIFI_SECURITY_WPA_WPA2_MESSAGE)) {

            this.password.setValidatorsFrom(configUserOptions);
            configUserOptions.setPasswordMinimumLength(Math.min(configUserOptions.getPasswordMinimumLength(), 63));

            this.password.addValidator(GwtValidators.regex(REGEX_PASS_WPA, MSGS.netWifiWirelessInvalidWPAPassword()));

        } else if (this.security.getSelectedItemText().equals(WIFI_SECURITY_WEP_MESSAGE)) {

            configUserOptions.setPasswordRequireSpecialChars(false);
            configUserOptions.setPasswordMinimumLength(Math.min(configUserOptions.getPasswordMinimumLength(), 26));
            this.password.setValidatorsFrom(configUserOptions);
            this.password.addValidator(GwtValidators.regex(REGEX_PASS_WEP, MSGS.netWifiWirelessInvalidWEPPassword()));
        } else {
            configUserOptions.allowAnyPassword();
            this.password.setValidatorsFrom(configUserOptions);
        }

    }

    private void initModal() {
        this.ssidModal.setTitle("Wireless Networks");
        this.ssidTitle.setText("Available networks in range");
        this.ssidModal.show();

        this.searchingText.setText(MSGS.netWifiAlertScanning());
        this.noSsidText.setText(MSGS.netWifiAlertNoSSID());
        this.scanFailText.setText(MSGS.netWifiAlertScanFail());
    }

    private void initSsid() {
        this.ssidInit = true;
        TextColumn<GwtWifiHotspotEntry> col1 = new TextColumn<GwtWifiHotspotEntry>() {

            @Override
            public String getValue(GwtWifiHotspotEntry object) {
                return GwtSafeHtmlUtils.htmlUnescape(object.getSSID());
            }
        };
        col1.setCellStyleNames(STATUS_TABLE_ROW);
        this.ssidGrid.addColumn(col1, "SSID");
        this.ssidGrid.setColumnWidth(col1, "240px");

        TextColumn<GwtWifiHotspotEntry> col2 = new TextColumn<GwtWifiHotspotEntry>() {

            @Override
            public String getValue(GwtWifiHotspotEntry object) {
                return object.getMacAddress();
            }
        };
        col2.setCellStyleNames(STATUS_TABLE_ROW);
        this.ssidGrid.addColumn(col2, "MAC Address");
        this.ssidGrid.setColumnWidth(col2, "140px");

        TextColumn<GwtWifiHotspotEntry> col3 = new TextColumn<GwtWifiHotspotEntry>() {

            @Override
            public String getValue(GwtWifiHotspotEntry object) {
                return String.valueOf(object.getSignalStrength());
            }
        };
        col3.setCellStyleNames(STATUS_TABLE_ROW);
        this.ssidGrid.addColumn(col3, "Signal Strength (dBm)");
        this.ssidGrid.setColumnWidth(col3, "70px");

        TextColumn<GwtWifiHotspotEntry> col4 = new TextColumn<GwtWifiHotspotEntry>() {

            @Override
            public String getValue(GwtWifiHotspotEntry object) {
                return String.valueOf(object.getChannel());
            }
        };
        col4.setCellStyleNames(STATUS_TABLE_ROW);
        this.ssidGrid.addColumn(col4, "Channel");
        this.ssidGrid.setColumnWidth(col4, "70px");

        TextColumn<GwtWifiHotspotEntry> col5 = new TextColumn<GwtWifiHotspotEntry>() {

            @Override
            public String getValue(GwtWifiHotspotEntry object) {
                return String.valueOf(object.getFrequency());
            }
        };
        col5.setCellStyleNames(STATUS_TABLE_ROW);
        this.ssidGrid.addColumn(col5, "Frequency");
        this.ssidGrid.setColumnWidth(col5, "70px");

        TextColumn<GwtWifiHotspotEntry> col6 = new TextColumn<GwtWifiHotspotEntry>() {

            @Override
            public String getValue(GwtWifiHotspotEntry object) {
                return object.getSecurity();
            }
        };
        col6.setCellStyleNames(STATUS_TABLE_ROW);
        this.ssidGrid.addColumn(col6, "Security");
        this.ssidGrid.setColumnWidth(col6, "70px");
        this.ssidDataProvider.addDataDisplay(this.ssidGrid);

        this.ssidGrid.setSelectionModel(this.ssidSelectionModel);

        this.ssidSelectionModel.addSelectionChangeHandler(event -> {
            GwtWifiHotspotEntry wifiHotspotEntry = TabWirelessUi.this.ssidSelectionModel.getSelectedObject();
            if (wifiHotspotEntry != null) {
                int channelToSelect = getChannelIndexFromValue(wifiHotspotEntry.getChannel());

                if (channelToSelect < 0) {
                    this.regDomErrorModal.show();
                    logger.info("SSID Selected channel not in regdom: " + wifiHotspotEntry.getChannel());
                } else {
                    this.channelList.setSelectedIndex(channelToSelect);
                    this.activeConfig.setChannels(Collections.singletonList(wifiHotspotEntry.getChannel()));
                    logger.info("SSID Selected channel: " + wifiHotspotEntry.getChannel());
                }

                TabWirelessUi.this.ssid.setValue(GwtSafeHtmlUtils.htmlUnescape(wifiHotspotEntry.getSSID()));
                String sec = wifiHotspotEntry.getSecurity();
                for (int i1 = 0; i1 < TabWirelessUi.this.security.getItemCount(); i1++) {
                    if (sec.equals(TabWirelessUi.this.security.getItemText(i1))) {
                        TabWirelessUi.this.security.setSelectedIndex(i1);
                        DomEvent.fireNativeEvent(Document.get().createChangeEvent(), TabWirelessUi.this.security);
                        break;
                    }
                }

                String pairwiseCiphers = wifiHotspotEntry.getPairwiseCiphersEnum().name();
                for (int i2 = 0; i2 < TabWirelessUi.this.pairwise.getItemCount(); i2++) {
                    if (MessageUtils.get(pairwiseCiphers).equals(TabWirelessUi.this.pairwise.getItemText(i2))) {
                        TabWirelessUi.this.pairwise.setSelectedIndex(i2);
                        break;
                    }
                }

                String groupCiphers = wifiHotspotEntry.getGroupCiphersEnum().name();
                for (int i3 = 0; i3 < TabWirelessUi.this.group.getItemCount(); i3++) {
                    if (MessageUtils.get(groupCiphers).equals(TabWirelessUi.this.group.getItemText(i3))) {
                        TabWirelessUi.this.group.setSelectedIndex(i3);
                        break;
                    }
                }

                TabWirelessUi.this.ssidModal.hide();

            }
        });
    }

    private void addItemChannelList(GwtWifiChannelFrequency channelFrequency) {

        if (channelFrequency.getChannel() == 0 && channelFrequency.getFrequency() == 0) {
            this.channelList.addItem(AUTOMATIC_CHANNEL_DESCRIPTION);
            return;
        }

        String frequencyString = channelFrequency.getFrequency() > -1 ? " - " + channelFrequency.getFrequency() + " MHz"
                : "";

        this.channelList.addItem("Channel " + channelFrequency.getChannel() + frequencyString);

        if (shouldBeDisabled(channelFrequency)) {
            disableLastChannelItem();
        }
    }

    private boolean shouldBeDisabled(GwtWifiChannelFrequency channelFrequency) {
        boolean isNotValidCountryCode = this.countryCode.getValue().isEmpty()
                || this.countryCode.getValue().equals(WORLD_REGION_COUNTRY_CODE);

        return channelFrequency.isDisabled()
                || channelFrequency.isNoIrradiation() && !channelFrequency.isRadarDetection()
                || channelFrequency.isNoIrradiation() && channelFrequency.isRadarDetection() && isNotValidCountryCode;
    }

    private void disableLastChannelItem() {

        NodeList<Element> channelElements = this.channelList.getElement().getElementsByTagName("option");

        int channelLength = channelElements.getLength();
        Element element = channelElements.getItem(channelLength - 1);

        element.setAttribute("disabled", "disabled");
        element.addClassName("list-group-item disabled");
    }

    private void loadSsidData() {
        this.ssidDataProvider.getList().clear();
        this.searching.setVisible(true);
        this.noSsid.setVisible(false);
        this.ssidGrid.setVisible(false);
        this.scanFail.setVisible(false);
        if (this.selectedNetIfConfig != null) {
            this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

                @Override
                public void onFailure(Throwable ex) {
                    FailureHandler.handle(ex);
                }

                @Override
                public void onSuccess(GwtXSRFToken token) {
                    TabWirelessUi.this.gwtNetworkService.findWifiHotspots(token,
                            TabWirelessUi.this.selectedNetIfConfig.getName(),
                            TabWirelessUi.this.selectedNetIfConfig.getAccessPointWifiConfig().getWirelessSsid(),
                            new AsyncCallback<List<GwtWifiHotspotEntry>>() {

                                @Override
                                public void onFailure(Throwable caught) {
                                    TabWirelessUi.this.searching.setVisible(false);
                                    TabWirelessUi.this.noSsid.setVisible(false);
                                    TabWirelessUi.this.ssidGrid.setVisible(false);
                                    TabWirelessUi.this.scanFail.setVisible(true);
                                }

                                @Override
                                public void onSuccess(List<GwtWifiHotspotEntry> result) {
                                    TabWirelessUi.this.ssidDataProvider.getList().clear();

                                    for (GwtWifiHotspotEntry pair : result) {
                                        TabWirelessUi.this.ssidDataProvider.getList().add(pair);
                                    }
                                    TabWirelessUi.this.ssidDataProvider.flush();
                                    if (!TabWirelessUi.this.ssidDataProvider.getList().isEmpty()) {
                                        TabWirelessUi.this.searching.setVisible(false);
                                        TabWirelessUi.this.noSsid.setVisible(false);
                                        int size = TabWirelessUi.this.ssidDataProvider.getList().size();
                                        TabWirelessUi.this.ssidGrid.setVisibleRange(0, size);
                                        TabWirelessUi.this.ssidGrid.setVisible(true);
                                        TabWirelessUi.this.scanFail.setVisible(false);
                                    } else {
                                        TabWirelessUi.this.searching.setVisible(false);
                                        TabWirelessUi.this.noSsid.setVisible(true);
                                        TabWirelessUi.this.ssidGrid.setVisible(false);
                                        TabWirelessUi.this.scanFail.setVisible(false);
                                    }
                                }
                            });
                }
            });
        }
    }

    private int getChannelIndexFromValue(int channelValue) {
        
        logger.info("getChannelIndexFromValue for channel: " + channelValue);

        if (channelValue == 0) {
            return 0;
        }

        for (int i = 0; i < this.channelList.getItemCount(); i++) {
            String[] values = this.channelList.getItemText(i).split(" ");

            try {
                int channel = Integer.parseInt(values[1]);
                if (channel == channelValue) {
                    return i;
                }
            } catch (NumberFormatException automaticChannel) {
            }
        }
        return -1;
    }

    private int getChannelValueByIndex(int index) {
        String itemText = this.channelList.getItemText(index);

        if (itemText.equalsIgnoreCase(AUTOMATIC_CHANNEL_DESCRIPTION)) {
            return 0;
        }

        String[] values = itemText.split(" ");
        return Integer.parseInt(values[1]);
    }

    private GwtWifiConfig getGwtWifiConfig() {
        GwtWifiConfig gwtWifiConfig = new GwtWifiConfig();

        // mode
        GwtWifiWirelessMode wifiMode;
        if (this.wireless.getSelectedItemText().equals(MessageUtils.get(WIFI_MODE_STATION))) {
            wifiMode = GwtWifiWirelessMode.netWifiWirelessModeStation;
        } else {
            wifiMode = GwtWifiWirelessMode.netWifiWirelessModeAccessPoint;
        }
        gwtWifiConfig.setWirelessMode(wifiMode.name());

        // ssid
        gwtWifiConfig.setWirelessSsid(GwtSafeHtmlUtils.htmlUnescape(this.ssid.getText().trim()));

        // driver
        String driver = "";
        if (GwtWifiWirelessMode.netWifiWirelessModeAccessPoint.equals(wifiMode)) {
            driver = this.selectedNetIfConfig.getAccessPointWifiConfig().getDriver();
        } else if (GwtWifiWirelessMode.netWifiWirelessModeAdHoc.equals(wifiMode)) {
            driver = this.selectedNetIfConfig.getAdhocWifiConfig().getDriver();
        } else if (GwtWifiWirelessMode.netWifiWirelessModeStation.equals(wifiMode)) {
            driver = this.selectedNetIfConfig.getStationWifiConfig().getDriver();
        }
        gwtWifiConfig.setDriver(driver); // use previous value

        // radio mode
        String radioValue = this.radio.getSelectedValue();

        gwtWifiConfig.setRadioMode(radioValueToRadioMode(radioValue).name());

        gwtWifiConfig
                .setChannels(Collections.singletonList(getChannelValueByIndex(this.channelList.getSelectedIndex())));

        // security
        String secValue = this.security.getSelectedItemText();
        for (GwtWifiSecurity sec : GwtWifiSecurity.values()) {
            if (MessageUtils.get(sec.name()).equals(secValue)) {
                gwtWifiConfig.setSecurity(sec.name());
            }
        }

        // Pairwise Ciphers
        String pairWiseCiphersValue = this.pairwise.getSelectedItemText();
        for (GwtWifiCiphers ciphers : GwtWifiCiphers.values()) {
            if (MessageUtils.get(ciphers.name()).equals(pairWiseCiphersValue)) {
                gwtWifiConfig.setPairwiseCiphers(ciphers.name());
            }
        }

        // Group Ciphers value
        String groupCiphersValue = this.group.getSelectedItemText();
        for (GwtWifiCiphers ciphers : GwtWifiCiphers.values()) {
            if (MessageUtils.get(ciphers.name()).equals(groupCiphersValue)) {
                gwtWifiConfig.setGroupCiphers(ciphers.name());
            }
        }

        // bgscan
        String bgscanModuleValue = this.bgscan.getSelectedItemText();
        for (GwtWifiBgscanModule module : GwtWifiBgscanModule.values()) {
            if (MessageUtils.get(module.name()).equals(bgscanModuleValue)) {
                gwtWifiConfig.setBgscanModule(module.name());
            }
        }

        String bgscanRssiThreshold = this.rssi.getText().trim();
        if (!bgscanRssiThreshold.isEmpty()) {
            gwtWifiConfig.setBgscanRssiThreshold(Integer.parseInt(bgscanRssiThreshold));
        } else {
            gwtWifiConfig.setBgscanRssiThreshold(0);
        }
        String bgscanShortInterval = this.shortI.getText().trim();
        if (!bgscanShortInterval.isEmpty()) {
            gwtWifiConfig.setBgscanShortInterval(Integer.parseInt(bgscanShortInterval));
        } else {
            gwtWifiConfig.setBgscanShortInterval(0);
        }
        String bgscanLongInterval = this.longI.getText().trim();
        if (!bgscanLongInterval.isEmpty()) {
            gwtWifiConfig.setBgscanLongInterval(Integer.parseInt(bgscanLongInterval));
        } else {
            gwtWifiConfig.setBgscanLongInterval(0);
        }

        // password
        if (this.groupPassword.getValidationState().equals(ValidationState.NONE)) {
            gwtWifiConfig.setPassword(this.password.getText());
        }

        // ping access point
        gwtWifiConfig.setPingAccessPoint(this.radio1.getValue());

        // ignore SSID
        gwtWifiConfig.setIgnoreSSID(this.radio3.getValue());

        // Country Code is not editable at moment.
        gwtWifiConfig.setCountryCode(this.countryCode.getValue());

        return gwtWifiConfig;
    }

    private GwtWifiRadioMode radioValueToRadioMode(String radioValue) {

        for (GwtWifiRadioMode mode : GwtWifiRadioMode.values()) {
            if (mode.name().equals(radioValue)) {
                return mode;
            }
        }

        return GwtWifiRadioMode.netWifiRadioModeBGN;
    }

    private void setForm(boolean visible) {
        this.wireless.setEnabled(visible);
        this.ssid.setEnabled(visible);
        this.buttonSsid.setEnabled(visible);
        this.radio.setEnabled(visible);
        this.security.setEnabled(visible);
        this.password.setEnabled(visible);
        this.buttonPassword.setEnabled(visible);
        this.verify.setEnabled(visible);
        this.pairwise.setEnabled(visible);
        this.group.setEnabled(visible);
        this.bgscan.setEnabled(visible);

        this.shortI.setEnabled(visible);
        this.longI.setEnabled(visible);

        this.radio1.setEnabled(visible);
        this.radio2.setEnabled(visible);
        this.radio3.setEnabled(visible);
        this.radio4.setEnabled(visible);

        this.groupVerify.setVisible(visible);
        this.channelList.setEnabled(visible);
        this.noChannels.setVisible(false);

        this.countryCode.setVisible(visible);
        this.countryCode.setEnabled(false);
    }

    private boolean checkPassword() {
        boolean result = true;

        if (!this.password.validate() && this.password.isEnabled()) {
            this.groupPassword.setValidationState(ValidationState.ERROR);
            result = false;
        } else {
            this.groupPassword.setValidationState(ValidationState.NONE);
        }

        if (this.verify.isEnabled() && TabWirelessUi.this.password != null
                && !TabWirelessUi.this.verify.getText().equals(TabWirelessUi.this.password.getText())) {
            TabWirelessUi.this.helpVerify.setText(MSGS.netWifiWirelessPasswordDoesNotMatch());
            TabWirelessUi.this.groupVerify.setValidationState(ValidationState.ERROR);
            result = false;
        } else {
            TabWirelessUi.this.helpVerify.setText("");
            TabWirelessUi.this.groupVerify.setValidationState(ValidationState.NONE);
        }

        return result;
    }

    private void showPasswordVerificationStatus(String statusMessage) {
        final Modal confirm = new Modal();
        ModalBody confirmBody = new ModalBody();
        ModalFooter confirmFooter = new ModalFooter();

        confirm.setTitle(MSGS.netWifiPasswordVerificationStatus());
        confirmBody.add(new Span(statusMessage));

        confirmFooter.add(new Button(MSGS.closeButton(), event -> confirm.hide()));
        confirm.add(confirmBody);
        confirm.add(confirmFooter);
        confirm.show();
    }

    private void loadChannelFrequencies() {
        if (this.selectedNetIfConfig != null) {
            this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

                @Override
                public void onFailure(Throwable ex) {
                    FailureHandler.handle(ex);
                }

                @Override
                public void onSuccess(GwtXSRFToken token) {
                    GwtWifiRadioMode radioMode = radioValueToRadioMode(TabWirelessUi.this.radio.getSelectedValue());

                    TabWirelessUi.this.gwtNetworkService.findFrequencies(token,
                            TabWirelessUi.this.selectedNetIfConfig.getName(), radioMode,
                            new AsyncCallback<List<GwtWifiChannelFrequency>>() {

                                @Override
                                public void onFailure(Throwable caught) {
                                    logger.info("findFrequencies Failure");
                                }

                                @Override
                                public void onSuccess(List<GwtWifiChannelFrequency> freqChannels) {
                                    
                                    logger.info("Processing result from findFrequencies for "
                                            + TabWirelessUi.this.selectedNetIfConfig.getName() + "/" + radioMode);

                                    TabWirelessUi.this.channelList.clear();

                                    addAutomaticChannel(freqChannels);

                                    freqChannels.stream().forEach(TabWirelessUi.this::addItemChannelList);

                                    int channel = TabWirelessUi.this.activeConfig.getChannels().get(0);

                                    int selectedChannelIndex = getChannelIndexFromValue(channel);
                                    int channelIndex = selectedChannelIndex == -1 ? 0 : selectedChannelIndex;

                                    logger.info("Setting channel to: " + channel);
                                    TabWirelessUi.this.channelList.setSelectedIndex(channelIndex);

                                    boolean hasChannels = TabWirelessUi.this.channelList.getItemCount() > 0;

                                    TabWirelessUi.this.noChannels.setVisible(!hasChannels);

                                    logger.info("Finished processing result from findFrequencies for "
                                            + TabWirelessUi.this.selectedNetIfConfig.getName() + "/" + radioMode);

                                }
                            });
                }
            });
        }

    }

    private void addAutomaticChannel(List<GwtWifiChannelFrequency> freqs) {
        if (!containsChannel0(freqs)) {
            GwtWifiChannelFrequency automaticChannel = new GwtWifiChannelFrequency(0, 0);
            freqs.add(0, automaticChannel);
        }
    }

    private boolean containsChannel0(List<GwtWifiChannelFrequency> freqs) {
        for (GwtWifiChannelFrequency freq : freqs) {
            if (freq.getChannel() == 0) {
                return true;
            }
        }

        return false;
    }

    private void loadRadioMode() {

        this.gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onFailure(Throwable ex) {
                FailureHandler.handle(ex);
            }

            @Override
            public void onSuccess(GwtXSRFToken token) {

                TabWirelessUi.this.gwtNetworkService.isIEEE80211ACSupported(token,
                        TabWirelessUi.this.selectedNetIfConfig.getName(), new AsyncCallback<Boolean>() {

                            @Override
                            public void onFailure(Throwable caught) {
                                logger.info("Unable to read 802.11ac capability");
                            }

                            @Override
                            public void onSuccess(Boolean acSupported) {

                                String selectedRadioMode = TabWirelessUi.this.radio.getSelectedValue() != null
                                        ? TabWirelessUi.this.radio.getSelectedValue()
                                        : null;

                                TabWirelessUi.this.radio.clear();

                                fillRadioMode(acSupported);

                                if (selectedRadioMode != null) {
                                    logger.info("selectedRadioModeText: " + selectedRadioMode);
                                    setRadioModeByValue(selectedRadioMode);
                                } else {
                                    logger.info("TabWirelessUi.this.activeConfig.getRadioMode(): "
                                            + TabWirelessUi.this.activeConfig.getRadioMode());
                                    setRadioModeByValue(TabWirelessUi.this.activeConfig.getRadioMode());
                                }

                                loadChannelFrequencies();
                            }
                        });
            }

        });
    }

    private void fillRadioMode(boolean acSupported) {
        if (!this.isNet2) {
            for (GwtWifiRadioMode mode : GwtWifiRadioMode.values()) {
                if (Boolean.FALSE.equals(acSupported)
                        && mode == GwtWifiRadioMode.netWifiRadioModeANAC) {
                    continue;
                }
                this.radio.addItem(MessageUtils.get(mode.name()));
            }
        } else {
            this.radio.addItem(WIFI_BAND_2GHZ_MESSAGE, WIFI_RADIO_BG);
            this.radio.addItem(WIFI_BAND_5GHZ_MESSAGE, WIFI_RADIO_A);
            this.radio.addItem(WIFI_BAND_BOTH_MESSAGE, WIFI_RADIO_BGN);
        }
    }

}
