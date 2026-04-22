package ru.example.otpcodes.sender;

import ru.example.otpcodes.domain.Channel;

public interface NotificationSender {

    Channel channel();

    void send(String destination, String code);
}
