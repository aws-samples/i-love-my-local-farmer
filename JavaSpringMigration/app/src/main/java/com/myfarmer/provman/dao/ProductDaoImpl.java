package com.myfarmer.provman.dao;

import com.myfarmer.provman.model.Product;
import com.myfarmer.provman.model.ProductPricing;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("productDao")
public class ProductDaoImpl extends AbstractDao<Integer, Product> implements ProductDao {

  @Override
  public Product findById(int id) {
    return super.getByKey(id);
  }

  @Override
  public void saveProduct(Product product) {
    super.persist(product);
  }

  @Override
  public void saveOrUpdate(Product product) {
    super.saveOrUpdate(product);
  }

  @Override
  public void deleteProductByNameAndFarmId(String name, Integer farmId) {
    Query query = super.getSession().createQuery("delete from Product where name = :name and farm_id = :farmId");
    query.setString("name", name);
    query.setInteger("farmId", farmId);
    query.executeUpdate();
  }

  @Override
  public List<ProductPricing> findAllProductPricings(Product product) {
    return null;
  }

  @Override
  public List<Product> findProductsByFarmId(Integer farmId) {
    Query query = super.getSession().createQuery("from Product where farm_id = :farmId");
    query.setInteger("farmId", farmId);
    return query.list();
  }

  @Override
  public void deleteProductById(Integer id) {
    Product product = findById(id);
    Session session = super.getSession();

    if (product.getProductPricings().size() != 0) {
      for (ProductPricing pricing : product.getProductPricings()) {
        session.delete(pricing);
      }
    }

    session.delete(product);
  }
}
