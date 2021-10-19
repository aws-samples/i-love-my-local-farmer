package com.myfarmer.provman;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.myfarmer.provman.dao.ProviderDao;
import com.myfarmer.provman.model.Provider;
import com.myfarmer.provman.service.ProviderServiceImpl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.HibernateException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProviderServiceImplTest {

	// allow this class to use Mocked objects
	@InjectMocks
	private ProviderServiceImpl service;

	// Mock the DB component
	@Mock
	private ProviderDao dao;

	@Test
	void testFindById() {
		Provider provider = new Provider(1, "name", LocalDate.now(), "nationality", "code");
		Mockito.doReturn(provider).when(dao).findById(1);

		// Execute the service call
		Provider returnedProvider = service.findById(1);

		// Assert the response
		assertNotNull(returnedProvider, "Provider was not found");
		assertSame(returnedProvider, provider, "The Provider returned was not the same as the mock");
	}

	@Test
	void testFindAllProviders() {
		List<Provider> allProviders = new ArrayList<Provider>();
		Provider provider = new Provider(1, "name", LocalDate.now(), "nationality", "code");
		Provider provider2 = new Provider(2, "name2", LocalDate.now(), "nationality2", "code2");
		allProviders.add(provider);
		allProviders.add(provider2);
		Mockito.doReturn(allProviders).when(dao).findAllProviders();

		// Execute the service call
		List<Provider> returnedProviders = service.findAllProviders();

		// Assert the response
		assertTrue(returnedProviders != null && !returnedProviders.isEmpty(), "Providers not found");
		assertTrue(returnedProviders.size() == 2, "The Provider returned was not the same as the mock");
	}

	@Test
	void testFindByIdNotExists() {
		// Execute the service call
		Provider returnedProvider = service.findById(999);

		// Assert the response
		assertTrue(returnedProvider == null, "Bad Provider returned");
	}

	@Test
	void testFindProviderByCode() {
		Provider provider = new Provider(1, "name", LocalDate.now(), "nationality", "code");
		Mockito.doReturn(provider).when(dao).findProviderByCode(Mockito.any());

		// Execute the service call
		Provider returnedProvider = service.findProviderByCode("a");

		// Assert the response
		assertNotNull(returnedProvider, "Provider was not found");
		assertSame(returnedProvider, provider, "The Provider returned was not the same as the mock");
	}

	@Test
	void testFindProviderByCodeNotExists() {
		// Execute the service call
		Provider returnedProvider = service.findProviderByCode("a");

		// Assert the response
		assertNull(returnedProvider, "Bad Provider was found");
	}

	@Test
	void testIsProviderCodeUniqueSameExists() {
		Provider provider = new Provider(1, "name-a", LocalDate.now(), "nationality-a", "code-a");
		Mockito.when(dao.findProviderByCode(Mockito.any())).thenReturn(provider);

		boolean retVal = service.isProviderCodeUnique(1, "a");
		assertTrue(retVal);
	}

	@Test
	void testIsProviderCodeUniqueDoesntExist() {
		boolean retVal = service.isProviderCodeUnique(1, "a");
		assertTrue(retVal);
	}

	@Test
	void testIsProviderCodeUniqueOtherExists() {
		Provider provider = new Provider(1, "name-a", LocalDate.now(), "nationality-a", "code-a");
		Mockito.when(dao.findProviderByCode(Mockito.any())).thenReturn(provider);

		boolean retVal = service.isProviderCodeUnique(2, "a");
		assertFalse(retVal);
	}
}
