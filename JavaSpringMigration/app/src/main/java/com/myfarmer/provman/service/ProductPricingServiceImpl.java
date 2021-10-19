package com.myfarmer.provman.service;

import com.myfarmer.provman.dao.ProductPricingDao;
import com.myfarmer.provman.model.ProductPricing;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service("productPricing")
@Transactional
public class ProductPricingServiceImpl implements ProductPricingService {

  @Autowired
  ProductPricingDao dao;

  @Override
  public void saveProductPricing(ProductPricing productPricing) {
    dao.saveProductPricing(productPricing);
  }

  @Override
  public void updateProductPricing(ProductPricing productPricing) {
    dao.saveOrUpdate(productPricing);
  }

  @Override
  public void deleteProductPricingById(Integer id) {
    dao.deleteById(id);
  }

  @Override
  public List<ProductPricing> findProductPricingsByProductId(Integer productId) {
    return null;
  }

  @Override
  public ProductPricing findById(Integer id) {
    return dao.findById(id);
  }


}
