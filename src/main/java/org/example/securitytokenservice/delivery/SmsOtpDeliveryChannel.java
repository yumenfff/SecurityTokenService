package org.example.securitytokenservice.delivery;

import org.example.securitytokenservice.config.AppConfig;
import org.example.securitytokenservice.model.OtpCode;
import org.jsmpp.bean.Alphabet;
import org.jsmpp.bean.BindType;
import org.jsmpp.bean.ESMClass;
import org.jsmpp.bean.GeneralDataCoding;
import org.jsmpp.bean.NumberingPlanIndicator;
import org.jsmpp.bean.RegisteredDelivery;
import org.jsmpp.bean.SMSCDeliveryReceipt;
import org.jsmpp.bean.TypeOfNumber;
import org.jsmpp.session.BindParameter;
import org.jsmpp.session.SMPPSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class SmsOtpDeliveryChannel implements OtpDeliveryChannel {
    private static final Logger log = LoggerFactory.getLogger(SmsOtpDeliveryChannel.class);
    private final String host;
    private final int port;
    private final String systemId;
    private final String password;
    private final String systemType;
    private final String sourceAddress;
    private SMPPSession session;

    public SmsOtpDeliveryChannel(AppConfig config) {
        Properties properties = config.loadResourceProperties(config.smsResource());
        this.host = require(properties, "smpp.host");
        this.port = Integer.parseInt(require(properties, "smpp.port"));
        this.systemId = require(properties, "smpp.system_id");
        this.password = require(properties, "smpp.password");
        this.systemType = require(properties, "smpp.system_type");
        this.sourceAddress = require(properties, "smpp.source_addr");

        try {
            ensureBoundSession();
        } catch (Exception e) {
            log.warn("SMPP warm-up failed, will retry on first SMS send: {}", e.getMessage());
        }
    }

    @Override
    public synchronized void send(OtpCode otpCode, String code) {
        String message = OtpMessageFormatter.format(otpCode, code);
        try {
            submitMessage(ensureBoundSession(), otpCode.destination(), message);
            log.info("OTP sent by SMS to {}", otpCode.destination());
        } catch (Exception e) {
            try {
                closeSession();
                submitMessage(ensureBoundSession(), otpCode.destination(), message);
                log.info("OTP sent by SMS to {} after reconnect", otpCode.destination());
            } catch (Exception retryError) {
                throw new IllegalStateException("Failed to send OTP by SMS", retryError);
            }
        }
    }

    @Override
    public synchronized void close() {
        closeSession();
    }

    private SMPPSession ensureBoundSession() {
        if (session != null) {
            return session;
        }
        try {
            SMPPSession newSession = new SMPPSession();
            newSession.connectAndBind(host, port, new BindParameter(
                    BindType.BIND_TX,
                    systemId,
                    password,
                    systemType,
                    TypeOfNumber.UNKNOWN,
                    NumberingPlanIndicator.UNKNOWN,
                    sourceAddress
            ));
            session = newSession;
            log.info("SMPP session established to {}:{}", host, port);
            return newSession;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to establish SMPP session", e);
        }
    }

    private void submitMessage(SMPPSession smppSession, String destination, String message) throws Exception {
        smppSession.submitShortMessage(
                systemType,
                TypeOfNumber.UNKNOWN,
                NumberingPlanIndicator.UNKNOWN,
                sourceAddress,
                TypeOfNumber.UNKNOWN,
                NumberingPlanIndicator.UNKNOWN,
                destination,
                new ESMClass(),
                (byte) 0,
                (byte) 1,
                null,
                null,
                new RegisteredDelivery(SMSCDeliveryReceipt.DEFAULT),
                (byte) 0,
                new GeneralDataCoding(Alphabet.ALPHA_DEFAULT),
                (byte) 0,
                message.getBytes(StandardCharsets.UTF_8)
        );
    }

    private void closeSession() {
        if (session == null) {
            return;
        }
        try {
            session.unbindAndClose();
        } catch (Exception ignored) {
        } finally {
            session = null;
        }
    }

    private static String require(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing SMS config property: " + key);
        }
        return value;
    }
}
