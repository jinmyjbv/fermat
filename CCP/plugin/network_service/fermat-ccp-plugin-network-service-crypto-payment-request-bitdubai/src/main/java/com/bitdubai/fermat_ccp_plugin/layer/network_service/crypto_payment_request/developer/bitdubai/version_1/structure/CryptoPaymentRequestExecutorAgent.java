package com.bitdubai.fermat_ccp_plugin.layer.network_service.crypto_payment_request.developer.bitdubai.version_1.structure;

import com.bitdubai.fermat_api.CantStartAgentException;
import com.bitdubai.fermat_api.FermatAgent;
import com.bitdubai.fermat_api.FermatException;
import com.bitdubai.fermat_api.layer.all_definition.components.enums.PlatformComponentType;
import com.bitdubai.fermat_api.layer.all_definition.components.interfaces.PlatformComponentProfile;
import com.bitdubai.fermat_api.layer.all_definition.enums.Actors;
import com.bitdubai.fermat_api.layer.all_definition.enums.AgentStatus;
import com.bitdubai.fermat_api.layer.all_definition.enums.Plugins;
import com.bitdubai.fermat_api.layer.all_definition.events.interfaces.FermatEvent;
import com.bitdubai.fermat_api.layer.all_definition.exceptions.InvalidParameterException;
import com.bitdubai.fermat_api.layer.all_definition.network_service.enums.NetworkServiceType;
import com.bitdubai.fermat_api.layer.all_definition.network_service.interfaces.NetworkServiceLocal;
import com.bitdubai.fermat_api.layer.osa_android.database_system.PluginDatabaseSystem;
import com.bitdubai.fermat_ccp_api.all_definition.enums.EventType;
import com.bitdubai.fermat_ccp_api.layer.network_service.crypto_payment_request.enums.RequestProtocolState;
import com.bitdubai.fermat_ccp_api.layer.network_service.crypto_payment_request.exceptions.RequestNotFoundException;
import com.bitdubai.fermat_ccp_api.layer.network_service.crypto_payment_request.interfaces.CryptoPaymentRequestEvent;
import com.bitdubai.fermat_ccp_api.layer.network_service.crypto_payment_request.interfaces.CryptoPaymentRequest;
import com.bitdubai.fermat_ccp_plugin.layer.network_service.crypto_payment_request.developer.bitdubai.version_1.CryptoPaymentRequestNetworkServicePluginRoot;
import com.bitdubai.fermat_ccp_plugin.layer.network_service.crypto_payment_request.developer.bitdubai.version_1.communication.structure.CommunicationNetworkServiceConnectionManager;
import com.bitdubai.fermat_ccp_plugin.layer.network_service.crypto_payment_request.developer.bitdubai.version_1.database.CryptoPaymentRequestNetworkServiceDao;
import com.bitdubai.fermat_ccp_plugin.layer.network_service.crypto_payment_request.developer.bitdubai.version_1.exceptions.CantChangeRequestProtocolStateException;
import com.bitdubai.fermat_ccp_plugin.layer.network_service.crypto_payment_request.developer.bitdubai.version_1.exceptions.CantInitializeCryptoPaymentRequestNetworkServiceDatabaseException;
import com.bitdubai.fermat_ccp_plugin.layer.network_service.crypto_payment_request.developer.bitdubai.version_1.exceptions.CantInitializeExecutorAgentException;
import com.bitdubai.fermat_ccp_plugin.layer.network_service.crypto_payment_request.developer.bitdubai.version_1.exceptions.CantListRequestsException;
import com.bitdubai.fermat_ccp_plugin.layer.network_service.crypto_payment_request.developer.bitdubai.version_1.messages.InformationMessage;
import com.bitdubai.fermat_ccp_plugin.layer.network_service.crypto_payment_request.developer.bitdubai.version_1.messages.RequestMessage;
import com.bitdubai.fermat_p2p_api.layer.p2p_communication.WsCommunicationsCloudClientManager;
import com.bitdubai.fermat_pip_api.layer.platform_service.error_manager.ErrorManager;
import com.bitdubai.fermat_pip_api.layer.platform_service.error_manager.UnexpectedPluginExceptionSeverity;
import com.bitdubai.fermat_pip_api.layer.platform_service.event_manager.interfaces.EventManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The class <code>com.bitdubai.fermat_ccp_plugin.layer.network_service.crypto_payment_request.developer.bitdubai.version_1.structure.CryptoPaymentRequestExecutorAgent</code>
 * haves all the necessary business logic to execute all required actions.
 *
 * Created by Leon Acosta - (laion.cj91@gmail.com) on 06/10/2015.
 */
public class CryptoPaymentRequestExecutorAgent extends FermatAgent {

    // Represent the sleep time for the cycles of receive and send in this agent.
    private static final long SLEEP_TIME = 15000;

    // Represent the receive and send cycles for this agent.
    private Thread agentThread;

    // network services registered
    private Map<String, String> poolConnectionsWaitingForResponse;

    private CryptoPaymentRequestNetworkServiceDao cryptoPaymentRequestNetworkServiceDao;

    private final CryptoPaymentRequestNetworkServicePluginRoot cryptoPaymentRequestNetworkServicePluginRoot;
    private final ErrorManager                                 errorManager                                ;
    private final EventManager                                 eventManager                                ;
    private final PluginDatabaseSystem                         pluginDatabaseSystem                        ;
    private final UUID                                         pluginId                                    ;
    private final WsCommunicationsCloudClientManager           wsCommunicationsCloudClientManager          ;

    public CryptoPaymentRequestExecutorAgent(final CryptoPaymentRequestNetworkServicePluginRoot cryptoPaymentRequestNetworkServicePluginRoot,
                                             final ErrorManager                                 errorManager                                ,
                                             final EventManager                                 eventManager                                ,
                                             final PluginDatabaseSystem                         pluginDatabaseSystem                        ,
                                             final UUID                                         pluginId                                    ,
                                             final WsCommunicationsCloudClientManager           wsCommunicationsCloudClientManager) {

        this.cryptoPaymentRequestNetworkServicePluginRoot = cryptoPaymentRequestNetworkServicePluginRoot;
        this.errorManager                                 = errorManager                                ;
        this.eventManager                                 = eventManager                                ;
        this.pluginDatabaseSystem                         = pluginDatabaseSystem                        ;
        this.pluginId                                     = pluginId                                    ;
        this.wsCommunicationsCloudClientManager           = wsCommunicationsCloudClientManager          ;

        this.status                                       = AgentStatus.CREATED                         ;

        poolConnectionsWaitingForResponse = new HashMap<>();

        //Create a thread to send the messages
        this.agentThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (isRunning()) {
                    sendCycle();
                    receiveCycle();
                }

            }
        });
    }

    public void start() throws CantStartAgentException {

        try {
            try {

                this.cryptoPaymentRequestNetworkServiceDao = new CryptoPaymentRequestNetworkServiceDao(
                        this.pluginDatabaseSystem,
                        this.pluginId
                );

                cryptoPaymentRequestNetworkServiceDao.initialize();
            } catch(CantInitializeCryptoPaymentRequestNetworkServiceDatabaseException e) {

                throw new CantInitializeExecutorAgentException(e, "", "Problem initializing Crypto Payment Request DAO from Executor Agent.");
            }

            agentThread.start();

            this.status = AgentStatus.STARTED;

        } catch (Exception exception) {

            throw new CantStartAgentException(FermatException.wrapException(exception), null, "You should inspect the cause.");
        }
    }

    // TODO MANAGE PAUSE, STOP AND RESUME METHODS.

    public void sendCycle() {

        try {

            if(cryptoPaymentRequestNetworkServicePluginRoot.isRegister()) {

                // function to process and send the rigth message to the counterparts.
                processSend();
            }

            //Sleep for a time
            Thread.sleep(SLEEP_TIME);

        } catch (InterruptedException e) {

            reportUnexpectedError(FermatException.wrapException(e));
        } catch(Exception e) {

            reportUnexpectedError(e);
        }

    }

    private void processSend() {
        try {

            List<CryptoPaymentRequest> cryptoPaymentRequestList = cryptoPaymentRequestNetworkServiceDao.listRequestsByProtocolState(
                    RequestProtocolState.PROCESSING_SEND
            );

            for(CryptoPaymentRequest cpr : cryptoPaymentRequestList) {
                switch (cpr.getAction()) {

                    case INFORM_APPROVAL:

                        if (sendMessageToActor(
                                buildJsonInformationMessage(cpr),
                                cpr.getActorPublicKey(),
                                cpr.getActorType(),
                                cpr.getIdentityPublicKey(),
                                cpr.getIdentityType()
                        )) {
                            toDone(cpr.getRequestId());
                        }
                        break;

                    case INFORM_DENIAL:

                        if (sendMessageToActor(
                                buildJsonInformationMessage(cpr),
                                cpr.getActorPublicKey(),
                                cpr.getActorType(),
                                cpr.getIdentityPublicKey(),
                                cpr.getIdentityType()
                        )) {
                            toDone(cpr.getRequestId());
                        }

                        break;

                    case INFORM_RECEPTION:

                        if (sendMessageToActor(
                                buildJsonInformationMessage(cpr),
                                cpr.getActorPublicKey(),
                                cpr.getActorType(),
                                cpr.getIdentityPublicKey(),
                                cpr.getIdentityType()
                        )) {
                            toDone(cpr.getRequestId());
                        }

                        break;

                    case INFORM_REFUSAL:

                        if (sendMessageToActor(
                                buildJsonInformationMessage(cpr),
                                cpr.getActorPublicKey(),
                                cpr.getActorType(),
                                cpr.getIdentityPublicKey(),
                                cpr.getIdentityType()
                        )) {
                            toDone(cpr.getRequestId());
                        }

                        break;

                    case REQUEST:

                        if (sendMessageToActor(
                                buildJsonRequestMessage(cpr),
                                cpr.getActorPublicKey(),
                                cpr.getActorType(),
                                cpr.getIdentityPublicKey(),
                                cpr.getIdentityType()
                        )) {
                            toWaitingResponse(cpr.getRequestId());
                        }

                        break;
                }
            }

        } catch(CantListRequestsException               |
                CantChangeRequestProtocolStateException |
                RequestNotFoundException                e) {

            reportUnexpectedError(e);
        }
    }

    public void receiveCycle() {

        try {

            if(cryptoPaymentRequestNetworkServicePluginRoot.isRegister()) {

                // function to process and send the rigth message to the counterparts.
                processReceive();
            }

            //Sleep for a time
            Thread.sleep(SLEEP_TIME);

        } catch (InterruptedException e) {

            reportUnexpectedError(FermatException.wrapException(e));
        } catch(Exception e) {

            reportUnexpectedError(e);
        }

    }

    public void processReceive(){

        try {

            List<CryptoPaymentRequest> cryptoPaymentRequestList = cryptoPaymentRequestNetworkServiceDao.listRequestsByProtocolState(
                    RequestProtocolState.PROCESSING_RECEIVE
            );

            for(CryptoPaymentRequest cpr : cryptoPaymentRequestList) {
                switch (cpr.getAction()) {

                    case INFORM_APPROVAL:
                        raiseEvent(EventType.CRYPTO_PAYMENT_REQUEST_APPROVED, cpr.getRequestId());
                        toPendingAction(cpr.getRequestId());
                        break;

                    case INFORM_DENIAL:
                        raiseEvent(EventType.CRYPTO_PAYMENT_REQUEST_DENIED, cpr.getRequestId());
                        toPendingAction(cpr.getRequestId());
                        break;

                    case INFORM_RECEPTION:
                        raiseEvent(EventType.CRYPTO_PAYMENT_REQUEST_CONFIRMED_RECEPTION, cpr.getRequestId());
                        toPendingAction(cpr.getRequestId());
                        break;

                    case INFORM_REFUSAL:
                        raiseEvent(EventType.CRYPTO_PAYMENT_REQUEST_REFUSED, cpr.getRequestId());
                        toPendingAction(cpr.getRequestId());
                        break;

                    case REQUEST:
                        raiseEvent(EventType.CRYPTO_PAYMENT_REQUEST_RECEIVED, cpr.getRequestId());
                        toPendingAction(cpr.getRequestId());
                        break;
                }
            }

            cryptoPaymentRequestList = cryptoPaymentRequestNetworkServiceDao.listRequestsByProtocolState(
                    RequestProtocolState.PENDING_ACTION
            );

            for(CryptoPaymentRequest cpr : cryptoPaymentRequestList) {
                switch (cpr.getAction()) {

                    case INFORM_APPROVAL:raiseEvent(EventType.CRYPTO_PAYMENT_REQUEST_APPROVED, cpr.getRequestId());
                        break;
                    case INFORM_DENIAL:raiseEvent(EventType.CRYPTO_PAYMENT_REQUEST_DENIED, cpr.getRequestId());
                        break;
                    case INFORM_RECEPTION:raiseEvent(EventType.CRYPTO_PAYMENT_REQUEST_CONFIRMED_RECEPTION, cpr.getRequestId());
                        break;
                    case INFORM_REFUSAL:raiseEvent(EventType.CRYPTO_PAYMENT_REQUEST_REFUSED, cpr.getRequestId());
                        break;
                    case REQUEST:raiseEvent(EventType.CRYPTO_PAYMENT_REQUEST_RECEIVED, cpr.getRequestId());
                        break;
                }
            }

        } catch(CantListRequestsException               |
                CantChangeRequestProtocolStateException |
                RequestNotFoundException                e) {

            reportUnexpectedError(e);
        }
    }

    private boolean sendMessageToActor(final String jsonMessage      ,
                                       final String identityPublicKey,
                                       final Actors identityType     ,
                                       final String actorPublicKey   ,
                                       final Actors actorType        ) {

        try {

            if (!poolConnectionsWaitingForResponse.containsKey(actorPublicKey)) {

                if (cryptoPaymentRequestNetworkServicePluginRoot.getNetworkServiceConnectionManager().getNetworkServiceLocalInstance(actorPublicKey) == null) {

                    if (wsCommunicationsCloudClientManager != null) {

                        if (cryptoPaymentRequestNetworkServicePluginRoot.getPlatformComponentProfilePluginRoot() != null) {

                            PlatformComponentProfile applicantParticipant = wsCommunicationsCloudClientManager.getCommunicationsCloudClientConnection()
                                    .constructBasicPlatformComponentProfileFactory(
                                            identityPublicKey,
                                            NetworkServiceType.UNDEFINED,
                                            platformComponentTypeSelectorByActorType(identityType));
                            PlatformComponentProfile remoteParticipant = wsCommunicationsCloudClientManager.getCommunicationsCloudClientConnection()
                                    .constructBasicPlatformComponentProfileFactory(
                                            actorPublicKey,
                                            NetworkServiceType.UNDEFINED,
                                            platformComponentTypeSelectorByActorType(actorType));

                            cryptoPaymentRequestNetworkServicePluginRoot.getNetworkServiceConnectionManager().connectTo(
                                    applicantParticipant,
                                    cryptoPaymentRequestNetworkServicePluginRoot.getPlatformComponentProfilePluginRoot(),
                                    remoteParticipant
                            );

                            // i put the actor in the pool of connections waiting for response-
                            poolConnectionsWaitingForResponse.put(actorPublicKey, actorPublicKey);
                        }

                    }

                    return false;

                } else {

                    return sendMessage(identityPublicKey, actorPublicKey, jsonMessage);

                }
            } else {

                return sendMessage(identityPublicKey, actorPublicKey, jsonMessage);
            }


        } catch (Exception z) {

            reportUnexpectedError(FermatException.wrapException(z));
            return false;
        }
    }

    private boolean sendMessage(final String identityPublicKey,
                                final String actorPublicKey   ,
                                final String jsonMessage      ) {

        NetworkServiceLocal communicationNetworkServiceLocal = cryptoPaymentRequestNetworkServicePluginRoot.getNetworkServiceConnectionManager().getNetworkServiceLocalInstance(actorPublicKey);

        if (communicationNetworkServiceLocal != null) {

            communicationNetworkServiceLocal.sendMessage(
                    identityPublicKey,
                    actorPublicKey,
                    jsonMessage
            );

            poolConnectionsWaitingForResponse.remove(actorPublicKey);

            return true;
        }

        return false;
    }

    private PlatformComponentType platformComponentTypeSelectorByActorType(Actors type) throws InvalidParameterException {

        switch (type) {

            case INTRA_USER  : return PlatformComponentType.ACTOR_INTRA_USER  ;
            case DAP_ASSET_ISSUER: return PlatformComponentType.ACTOR_ASSET_ISSUER;
            case DAP_ASSET_USER  : return PlatformComponentType.ACTOR_ASSET_USER  ;

            default: throw new InvalidParameterException(
                  " actor type: "+type.name()+"  type-code: "+type.getCode(),
                  " type of actor not expected."
            );
        }
    }

    private String buildJsonInformationMessage(CryptoPaymentRequest cpr) {

        return new InformationMessage(
                cpr.getRequestId(),
                cpr.getAction()
        ).toJson();
    }

    private String buildJsonRequestMessage(CryptoPaymentRequest cpr) {

        return new RequestMessage(
                cpr.getRequestId()        ,
                cpr.getIdentityPublicKey(),
                cpr.getIdentityType()     ,
                cpr.getActorPublicKey()   ,
                cpr.getActorType()        ,
                cpr.getDescription()      ,
                cpr.getCryptoAddress()    ,
                cpr.getAmount()           ,
                cpr.getStartTimeStamp()   ,
                cpr.getAction()           ,
                cpr.getNetworkType()
        ).toJson();
    }

    private void toDone(UUID requestId) throws CantChangeRequestProtocolStateException,
                                               RequestNotFoundException               {

        cryptoPaymentRequestNetworkServiceDao.changeProtocolState(requestId, RequestProtocolState.DONE);
    }

    private void toPendingAction(UUID requestId) throws CantChangeRequestProtocolStateException,
                                                        RequestNotFoundException {

        cryptoPaymentRequestNetworkServiceDao.changeProtocolState(requestId, RequestProtocolState.PENDING_ACTION);
    }

    private void toWaitingResponse(UUID requestId) throws CantChangeRequestProtocolStateException,
                                                          RequestNotFoundException                            {

        cryptoPaymentRequestNetworkServiceDao.changeProtocolState(requestId, RequestProtocolState.WAITING_RESPONSE);
    }

    private void raiseEvent(final EventType eventType,
                            final UUID      requestId) {

        FermatEvent eventToRaise = eventManager.getNewEvent(eventType);
        ((CryptoPaymentRequestEvent) eventToRaise).setRequestId(requestId);
        eventToRaise.setSource(cryptoPaymentRequestNetworkServicePluginRoot.getEventSource());
        eventManager.raiseEvent(eventToRaise);
    }

    private void reportUnexpectedError(Exception e) {
        errorManager.reportUnexpectedPluginException(Plugins.BITDUBAI_CCP_CRYPTO_PAYMENT_REQUEST_NETWORK_SERVICE, UnexpectedPluginExceptionSeverity.DISABLES_SOME_FUNCTIONALITY_WITHIN_THIS_PLUGIN, e);
    }

    public void connectionFailure(String identityPublicKey){
        this.poolConnectionsWaitingForResponse.remove(identityPublicKey);
    }

}
