package com.myfarmer.provman.dao;

import com.myfarmer.provman.model.Product;
import com.myfarmer.provman.model.ProductPricing;

import java.util.List;

public interface ProductDao {

  Product findById(int id);

  void saveProduct(Product product);

  void saveOrUpdate(Product product);

  void deleteProductByNameAndFarmId(String name, Integer farmId);

  List<ProductPricing> findAllProductPricings(Product product);

  List<Product> findProductsByFarmId(Integer farmId);

  void deleteProductById(Integer id);

}
