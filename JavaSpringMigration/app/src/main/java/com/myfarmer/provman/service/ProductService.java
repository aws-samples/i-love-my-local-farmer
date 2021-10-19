package com.myfarmer.provman.service;

import com.myfarmer.provman.model.Product;

import java.util.List;

public interface ProductService {

  Product findById(Integer id);

  void saveProduct(Product product);

  void updateProduct(Product product);

  List<Product> getProductsByFarmId(Integer farmId);

  void deleteProductByNameAndFarmId(String name, Integer farmId);

  void deleteProductById(Integer id);
}
