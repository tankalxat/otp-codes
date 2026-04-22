package ru.example.otpcodes.sender;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.example.otpcodes.domain.Channel;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class NotificationRouter {

    private final Map<Channel, NotificationSender> byChannel;

    public NotificationRouter(List<NotificationSender> senders) {
        this.byChannel = new EnumMap<>(Channel.class);
        for (NotificationSender sender : senders) {
            byChannel.put(sender.channel(), sender);
        }
        log.info("Registered {} notification senders: {}", byChannel.size(), byChannel.keySet());
    }

    public void route(Channel channel, String destination, String code) {
        NotificationSender sender = byChannel.get(channel);
        if (sender == null) {
            throw new IllegalStateException("No sender registered for channel " + channel);
        }
        sender.send(destination, code);
    }
}
