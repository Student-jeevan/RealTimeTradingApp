package com.jeevan.TradingApp.service;

import com.jeevan.TradingApp.modal.Asset;
import com.jeevan.TradingApp.modal.Coin;
import com.jeevan.TradingApp.modal.User;
import com.jeevan.TradingApp.repository.AssetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AssetServiceImpl implements  AssetService{
    @Autowired
    private AssetRepository assetRepository;
    @Override
    public Asset createAsset(User user, Coin coin, double quantity) {
        Asset asset = new Asset();
        asset.setUser(user);
        asset.setQuantity(quantity);
        asset.setCoin(coin);
        asset.setBuyPrice(coin.getCurrentPrice());

        return assetRepository.save(asset);
    }

    @Override
    public Asset getAssetById(Long assetId) throws Exception {

        return assetRepository.findById(assetId)
                .orElseThrow(()->new Exception("asset not found"));
    }

    @Override
    public Asset getAssetByUserAndId(Long userId, Long assetId) {

        return null;
    }

    @Override
    public List<Asset> getUserAssets(Long userId) {
        return assetRepository.findByUserId(userId);
    }

    @Override
    public Asset updateAsset(Long assetId, double quantity) throws Exception {
        Asset oldAsset  = getAssetById(assetId);
        oldAsset.setQuantity(quantity + oldAsset.getQuantity());
        return assetRepository.save(oldAsset);
    }

    @Override
    public Asset findAssetByUserIdAndCoinId(Long userId, String coinId) {
        return assetRepository.findByUserIdAndCoinId(userId , coinId);
    }

    @Override
    public void deleteAsset(Long AssetId) {
        assetRepository.deleteById(AssetId);
    }
}
