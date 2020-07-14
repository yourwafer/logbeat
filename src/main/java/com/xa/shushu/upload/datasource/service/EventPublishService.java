package com.xa.shushu.upload.datasource.service;

import com.xa.shushu.upload.datasource.config.EventConfig;
import com.xa.shushu.upload.datasource.service.push.EventPush;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

@Service
@Slf4j
public class EventPublishService {

    private final EventPushAdapter eventPush;

    public EventPublishService(Collection<EventPush> eventPushes) {
        this.eventPush = new EventPushAdapter(eventPushes);
    }

    public EventPush get() {
        return eventPush;
    }

    private static class EventPushAdapter implements EventPush {

        private final Collection<EventPush> eventPushes;

        public EventPushAdapter(Collection<EventPush> eventPushes) {
            this.eventPushes = eventPushes;
        }

        @Override
        public void push(EventConfig eventConfig, List<String> values) {
            for (EventPush eventPush : eventPushes) {
                eventPush.push(eventConfig, values);
            }
        }
    }
}
