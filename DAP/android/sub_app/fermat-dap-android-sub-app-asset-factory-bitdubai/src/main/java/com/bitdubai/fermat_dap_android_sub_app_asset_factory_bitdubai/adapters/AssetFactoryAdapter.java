package com.bitdubai.fermat_dap_android_sub_app_asset_factory_bitdubai.adapters;

import android.content.Context;
import android.view.View;

import com.bitdubai.fermat_android_api.ui.adapters.FermatAdapter;
import com.bitdubai.fermat_dap_android_sub_app_asset_factory_bitdubai.R;
import com.bitdubai.fermat_dap_android_sub_app_asset_factory_bitdubai.holders.AssetHolder;
import com.bitdubai.fermat_dap_android_sub_app_asset_factory_bitdubai.interfaces.PopupMenu;
import com.bitdubai.fermat_dap_api.layer.dap_middleware.dap_asset_factory.interfaces.AssetFactory;

import java.util.List;

/**
 * Created by francisco on 01/10/15.
 */
public class AssetFactoryAdapter extends FermatAdapter<AssetFactory, AssetHolder> {

    private PopupMenu menuItemClick;

    public AssetFactoryAdapter(Context context) {
        super(context);
    }

    public AssetFactoryAdapter(Context context, List<AssetFactory> dataSet) {
        super(context, dataSet);
    }

    @Override
    protected AssetHolder createHolder(View itemView, int type) {
        return new AssetHolder(itemView);
    }

    @Override
    protected int getCardViewResource() {
        return R.layout.row_draf_asset;
    }

    @Override
    protected void bindHolder(AssetHolder holder, AssetFactory data, int position) {
        holder.title.setTag(data.getName() != null ? data.getName() : "No name given...");
        holder.description.setText(data.getDescription() != null ? data.getDescription() : "");
    }

    public void setMenuItemClick(PopupMenu menuItemClick) {
        this.menuItemClick = menuItemClick;
    }
}
