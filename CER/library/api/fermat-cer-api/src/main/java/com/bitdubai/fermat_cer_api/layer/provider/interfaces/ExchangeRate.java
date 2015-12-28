package com.bitdubai.fermat_cer_api.layer.provider.interfaces;


import com.bitdubai.fermat_api.layer.world.interfaces.Currency;

/**
 * Created by Alejandro Bicelis on 12/7/2015.
 */
public interface ExchangeRate {

    Currency getFromCurrency();
    Currency getToCurrency();
    double getSalePrice();
    double getPurchasePrice();
    long getTimestamp();
}
