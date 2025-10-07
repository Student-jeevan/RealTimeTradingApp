package com.jeevan.TradingApp.service;

import com.jeevan.TradingApp.modal.Asset;
import com.jeevan.TradingApp.modal.Coin;
import com.jeevan.TradingApp.modal.User;

import java.util.List;

public interface AssetService {
    Asset createAsset(User user , Coin coin , double quantity);

    Asset getAssetById(Long assetId) throws Exception;

    Asset getAssetByUserAndId(Long userId , Long assetId);

    List<Asset> getUserAssets(Long userId);

    Asset updateAsset(Long assetId , double quantity) throws Exception;

    Asset findAssetByUserIdAndCoinId(Long userId , String coinId);

    void deleteAsset(Long AssetId);

}
