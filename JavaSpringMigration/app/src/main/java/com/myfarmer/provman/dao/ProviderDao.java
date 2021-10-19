package com.myfarmer.provman.dao;

import java.util.List;

import com.myfarmer.provman.model.Provider;

public interface ProviderDao {

	Provider findById(int id);

	void saveProvider(Provider provider);
	
	public void saveOrUpdate(Provider provider);
	
	void deleteProviderByCode(String code);
	
	List<Provider> findAllProviders();

	Provider findProviderByCode(String code);

}
