package com.myfarmer.provman.service;

import com.myfarmer.provman.dao.ProviderDao;
import com.myfarmer.provman.model.Provider;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;



@Service("providerService")
@Transactional
public class ProviderServiceImpl implements ProviderService {

	@Autowired
	private ProviderDao dao;
	
	public Provider findById(int id) {
		return dao.findById(id);
	}

	public void saveProvider(Provider provider) {
		dao.saveProvider(provider);
	}
	
	public void updateProvider(Provider provider) {
		dao.saveOrUpdate(provider);
	}

	public void deleteProviderByCode(String code) {
		dao.deleteProviderByCode(code);
	}
	
	public List<Provider> findAllProviders() {
		return dao.findAllProviders();
	}

	public Provider findProviderByCode(String code) {
		return dao.findProviderByCode(code);
	}

	public boolean isProviderCodeUnique(Integer id, String code) {
		Provider provider = findProviderByCode(code);
		return ( provider == null || ((id != null) && (provider.getId() == id)));
	}
	
}
