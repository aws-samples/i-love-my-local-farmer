package com.myfarmer.provman.service;

import java.util.List;

import com.myfarmer.provman.model.Provider;

public interface ProviderService {

	Provider findById(int id);
	
	void saveProvider(Provider provider);
	
	void updateProvider(Provider provider);
	
	void deleteProviderByCode(String code);

	List<Provider> findAllProviders(); 
	
	Provider findProviderByCode(String code);

	boolean isProviderCodeUnique(Integer id, String code);
	
}
