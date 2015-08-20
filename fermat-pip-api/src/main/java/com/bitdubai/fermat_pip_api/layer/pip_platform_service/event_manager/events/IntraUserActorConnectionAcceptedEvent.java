package com.bitdubai.fermat_pip_api.layer.pip_platform_service.event_manager.events;

import com.bitdubai.fermat_api.layer.all_definition.event.EventSource;
import com.bitdubai.fermat_api.layer.all_definition.event.EventType;
import com.bitdubai.fermat_api.layer.all_definition.event.PlatformEvent;

import java.util.UUID;

/**
 * Created by natalia on 17/08/15.
 */
public class IntraUserActorConnectionAcceptedEvent implements PlatformEvent {
    private EventType eventType;
    private EventSource eventSource;
    String intraUserLoggedInPublicKey;
    String intraUserToAddPublicKey;

    public IntraUserActorConnectionAcceptedEvent(EventType eventType){
        this.eventType = eventType;
    }

    /**
     * Constructor with parameters
     *
     * @param eventType
     * @param intraUserLoggedInPublicKey
     * @param intraUserToAddPublicKey
     */
    public IntraUserActorConnectionAcceptedEvent(EventType eventType,String intraUserLoggedInPublicKey, String intraUserToAddPublicKey) {

        this.eventType = eventType;
        this.intraUserLoggedInPublicKey = intraUserLoggedInPublicKey;
        this.intraUserToAddPublicKey = intraUserToAddPublicKey;
    }
    @Override
    public EventType getEventType() {
        return this.eventType;
    }

    @Override
    public void setSource(EventSource eventSource) {
        this.eventSource = eventSource;
    }

    @Override
    public EventSource getSource() {
        return this.eventSource;
    }

    /**
     *
     * @return
     */
    public String getIntraUserLoggedInPublicKey() {
        return this.intraUserLoggedInPublicKey;
    }

    /**
     *
     * @return
     */
    public String getIntraUserToAddPublicKey() {
        return this.intraUserToAddPublicKey;
    }
}
