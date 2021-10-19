package com.myfarmer.provman.dao;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Repository;

import com.myfarmer.provman.model.Provider;

@Repository("providerDao")
public class ProviderDaoImpl extends AbstractDao<Integer, Provider> implements ProviderDao {

	public Provider findById(int id) {
		return getByKey(id);
	}

	public void saveProvider(Provider provider) {
		persist(provider);
	}
	
	public void saveOrUpdate(Provider provider){
		super.saveOrUpdate(provider);
	}
	
	public void deleteProviderByCode(String code) {
		Query query = getSession().createSQLQuery("delete from provider where code = :code");
		query.setString("code", code);
		query.executeUpdate();
	}

	@SuppressWarnings("unchecked")
	public List<Provider> findAllProviders() {
		Criteria criteria = createEntityCriteria();
		return (List<Provider>) criteria.list();
	}

	public Provider findProviderByCode(String code) {
		Criteria criteria = createEntityCriteria();
		criteria.add(Restrictions.eq("code", code));
		return (Provider) criteria.uniqueResult();
	}
}
