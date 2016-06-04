package com.bitdubai.android_core.app.common.version_1.sessions;

import com.bitdubai.fermat_android_api.layer.definition.wallet.interfaces.AppConnections;
import com.bitdubai.fermat_android_api.layer.definition.wallet.interfaces.FermatSession;
import com.bitdubai.fermat_api.layer.all_definition.common.system.interfaces.ErrorManager;
import com.bitdubai.fermat_api.layer.all_definition.runtime.FermatApp;
import com.bitdubai.fermat_api.layer.modules.interfaces.ModuleManager;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Matias Furszyfer on 2016.02.22..
 */
public class FermatSessionManager {

    private Map<String, FermatSession> lstAppSession;


    public FermatSessionManager() {
        lstAppSession = new HashMap<>();
    }


    public Map<String, FermatSession> listOpenApps() {
        return lstAppSession;
    }

    public FermatSession openAppSession(FermatApp app, ErrorManager errorManager, ModuleManager moduleManager, AppConnections appConnections) {
        FermatSession AppsSession  = appConnections.buildReferenceSession(app, moduleManager, errorManager);
        lstAppSession.put(app.getAppPublicKey(), AppsSession);
        return AppsSession;
    }

    public FermatSession openAppSession(FermatApp app, ErrorManager errorManager,AppConnections appConnections, ModuleManager... moduleManager) {
        FermatSession AppsSession  = appConnections.buildComboAppSession(app, errorManager, moduleManager);
        lstAppSession.put(app.getAppPublicKey(), AppsSession);
        return AppsSession;
    }


    public boolean closeAppSession(String subAppPublicKey) {
        try {
            lstAppSession.remove(subAppPublicKey);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;

    }

    public FermatSession getAppsSession(String appPublicKey) {
        if(appPublicKey == null) throw new NullPointerException("Publick key de la app se encuentra en null");
        return lstAppSession.get(appPublicKey);
    }

    public boolean isSessionOpen(String appPublicKey) {
        return lstAppSession.containsKey(appPublicKey);
    }
}
