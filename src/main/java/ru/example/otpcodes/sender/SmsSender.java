package ru.example.otpcodes.sender;

import lombok.extern.slf4j.Slf4j;
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
import org.springframework.stereotype.Component;
import ru.example.otpcodes.domain.Channel;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

@Slf4j
@Component
public class SmsSender implements NotificationSender {

    private final String host;
    private final int port;
    private final String systemId;
    private final String password;
    private final String systemType;
    private final String sourceAddress;

    public SmsSender() {
        Properties config = loadConfig();
        this.host = config.getProperty("smpp.host");
        this.port = Integer.parseInt(config.getProperty("smpp.port"));
        this.systemId = config.getProperty("smpp.system_id");
        this.password = config.getProperty("smpp.password");
        this.systemType = config.getProperty("smpp.system_type");
        this.sourceAddress = config.getProperty("smpp.source_addr");
    }

    @Override
    public Channel channel() {
        return Channel.SMS;
    }

    @Override
    public void send(String destination, String code) {
        SMPPSession session = new SMPPSession();
        try {
            BindParameter bindParameter = new BindParameter(
                    BindType.BIND_TX,
                    systemId,
                    password,
                    systemType,
                    TypeOfNumber.UNKNOWN,
                    NumberingPlanIndicator.UNKNOWN,
                    sourceAddress
            );
            session.connectAndBind(host, port, bindParameter);
            session.submitShortMessage(
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
                    ("Your code: " + code).getBytes(StandardCharsets.UTF_8)
            );
            log.info("SMS with OTP sent to {}", destination);
        } catch (Exception e) {
            log.error("Failed to send SMS to {}: {}", destination, e.getMessage(), e);
            throw new RuntimeException("Failed to send SMS", e);
        } finally {
            session.unbindAndClose();
        }
    }

    private Properties loadConfig() {
        Properties props = new Properties();
        try (InputStream is = SmsSender.class.getClassLoader().getResourceAsStream("sms.properties")) {
            if (is == null) {
                throw new IllegalStateException("sms.properties not found on classpath");
            }
            props.load(is);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load SMS configuration", e);
        }
        overrideFromEnv(props, "smpp.host", "SMPP_HOST");
        overrideFromEnv(props, "smpp.port", "SMPP_PORT");
        overrideFromEnv(props, "smpp.system_id", "SMPP_SYSTEM_ID");
        overrideFromEnv(props, "smpp.password", "SMPP_PASSWORD");
        overrideFromEnv(props, "smpp.system_type", "SMPP_SYSTEM_TYPE");
        overrideFromEnv(props, "smpp.source_addr", "SMPP_SOURCE_ADDR");
        return props;
    }

    private static void overrideFromEnv(Properties props, String key, String envVar) {
        String value = System.getenv(envVar);
        if (value != null && !value.isBlank()) {
            props.setProperty(key, value);
        }
    }
}
