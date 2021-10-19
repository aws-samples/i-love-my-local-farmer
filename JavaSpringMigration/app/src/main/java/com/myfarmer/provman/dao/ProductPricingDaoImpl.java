package com.myfarmer.provman.dao;

import com.myfarmer.provman.model.ProductPricing;
import org.hibernate.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("productPricingDao")
public class ProductPricingDaoImpl extends AbstractDao<Integer, ProductPricing> implements ProductPricingDao {

  @Override
  public List<ProductPricing> findByProductId(int prodId) {
    Query query = super.getSession().createQuery("from ProductPricing where product_id = :prodId");
    query.setInteger("prodId", prodId);

    return query.list();
  }

  @Override
  public void saveProductPricing(ProductPricing productPricing) {
    super.saveOrUpdate(productPricing);
  }

  @Override
  public void saveOrUpdate(ProductPricing productPricing) {
    super.saveOrUpdate(productPricing);
  }

  @Override
  public ProductPricing findById(Integer id) {
    Query query = super.getSession().createQuery("from ProductPricing where id = :id");
    query.setInteger("id", id);

    return (ProductPricing) query.list().get(0);
  }

  @Override
  public void deleteById(Integer id) {
    Query query = super.getSession().createQuery("delete from ProductPricing where id = :id");
    query.setInteger("id", id);
    query.executeUpdate();
  }
}
