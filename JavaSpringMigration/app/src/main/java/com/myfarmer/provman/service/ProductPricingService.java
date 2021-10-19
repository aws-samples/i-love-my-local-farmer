package com.myfarmer.provman.service;

import com.myfarmer.provman.model.ProductPricing;

import java.util.List;

public interface ProductPricingService {

  void saveProductPricing(ProductPricing productPricing);

  void updateProductPricing(ProductPricing productPricing);

  void deleteProductPricingById(Integer id);

  List<ProductPricing> findProductPricingsByProductId(Integer productId);

  ProductPricing findById(Integer id);
}
