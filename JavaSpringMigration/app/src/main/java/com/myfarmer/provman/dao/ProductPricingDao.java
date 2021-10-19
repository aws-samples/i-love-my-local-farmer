package com.myfarmer.provman.dao;

import com.myfarmer.provman.model.ProductPricing;

import java.util.List;

public interface ProductPricingDao {

  List<ProductPricing> findByProductId(int id);

  void saveProductPricing(ProductPricing productPricing);

  void saveOrUpdate(ProductPricing product);

  ProductPricing findById(Integer id);

  void deleteById(Integer id);
}
