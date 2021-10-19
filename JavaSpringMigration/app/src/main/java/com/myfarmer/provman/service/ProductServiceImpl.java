package com.myfarmer.provman.service;

import com.myfarmer.provman.dao.ProductDao;
import com.myfarmer.provman.model.Product;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service("productService")
@Transactional
public class ProductServiceImpl implements ProductService {

  @Autowired
  ProductDao dao;

  @Override
  public Product findById(Integer id) {
    return dao.findById(id);
  }

  @Override
  public void saveProduct(Product product) {
    dao.saveProduct(product);
  }

  @Override
  public void updateProduct(Product product) {
    dao.saveOrUpdate(product);
  }

  @Override
  public List<Product> getProductsByFarmId(Integer farmId) {
    return dao.findProductsByFarmId(farmId);
  }

  @Override
  public void deleteProductByNameAndFarmId(String name, Integer farmId) {
    dao.deleteProductByNameAndFarmId(name, farmId);
  }

  public void deleteProductById(Integer id) {
    dao.deleteProductById(id);
  }
}
