package com.myfarmer.provman.controller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.myfarmer.provman.model.Product;
import com.myfarmer.provman.model.ProductAndPrice;
import com.myfarmer.provman.model.ProductPricing;
import com.myfarmer.provman.model.Provider;
import com.myfarmer.provman.service.ProductPricingService;
import com.myfarmer.provman.service.ProductService;
import com.myfarmer.provman.service.ProviderService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;

@ExtendWith(MockitoExtension.class)
class ProvmanControllerTest {

	@InjectMocks
	ProvmanController controller;

	@Mock
	ProviderService providerService;

	@Mock
	ProductService productService;

	@Mock
	ProductPricingService pricingService;

	@Mock
	MessageSource messageSource;

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
	}

	@BeforeEach
	void setUp() throws Exception {
	}

	@Test
	void testListProviders() {
		Provider provider = new Provider(1, "name", LocalDate.now(), "nationality", "code");
		Mockito.doReturn(new ArrayList<Provider>(List.of(provider))).when(providerService).findAllProviders();
		ModelMap modelMap = new ModelMap();
		String retVal = controller.listProviders(modelMap);

		assertTrue(modelMap.containsKey("providers"));
		@SuppressWarnings("unchecked")
		List<Provider> returnedList = (List<Provider>) modelMap.get("providers");
		assertTrue(returnedList.size() == 1);
		assertTrue(((Provider) returnedList.get(0)).equals(provider));
		assertSame(Views.ALLPROVIDERS.getViewName(), retVal);
	}

	@Test
	void testNewProvider() {
		ModelMap modelMap = new ModelMap();
		String retVal = controller.newProvider(modelMap);

		assertTrue(modelMap.containsKey("provider"));
		assertNotNull(modelMap.get("provider"));

		assertTrue(modelMap.containsKey("edit"));
		assertFalse((Boolean) modelMap.get("edit"));

		assertSame(Views.REGISTRATION.getViewName(), retVal);
	}

	@Test
	void testSaveProvider() {
		Provider provider = new Provider();
		ModelMap modelMap = new ModelMap();
		BindingResult result = Mockito.mock(BindingResult.class);
		Mockito.when(result.hasErrors()).thenReturn(false);
		Mockito.when(providerService.isProviderCodeUnique(Mockito.any(), Mockito.any())).thenReturn(true);

		String retVal = controller.saveProvider(provider, result, modelMap);
		assertTrue(modelMap.containsKey("success"));
		assertSame(Views.SUCCESS.getViewName(), retVal);
	}

	@Test
	void testSaveProviderBindingErrors() {
		BindingResult result = Mockito.mock(BindingResult.class);
		Mockito.when(result.hasErrors()).thenReturn(true);
		String retVal = controller.saveProvider(new Provider(), result, new ModelMap());
		assertSame(Views.REGISTRATION.getViewName(), retVal);
	}

	@Test
	void testSaveProviderNonUniqueProvider() {
		ModelMap modelMap = new ModelMap();
		Provider provider = new Provider(1, "name", LocalDate.now(), "nationality", "code");
		BindingResult result = Mockito.mock(BindingResult.class);
		Mockito.when(result.hasErrors()).thenReturn(false);
		Mockito.when(providerService.isProviderCodeUnique(Mockito.any(), Mockito.any())).thenReturn(false);

		String retVal = controller.saveProvider(provider, result, modelMap);
		assertSame(Views.REGISTRATION.getViewName(), retVal);
	}

	@Test
	public void testEditProvider() {
		ModelMap modelMap = new ModelMap();
		Provider provider = new Provider();
		Mockito.when(providerService.findProviderByCode(Mockito.any())).thenReturn(provider);

		String retVal = controller.editProvider("a", modelMap);
		assertTrue(modelMap.containsKey("provider"));
		assertSame(provider, modelMap.get("provider"));

		assertTrue(modelMap.containsKey("edit"));
		assertTrue((Boolean) modelMap.get("edit"));

		assertSame(Views.REGISTRATION.getViewName(), retVal);
	}

	@Test
	void testUpdateProvider() {
		Provider provider = new Provider(1, "name", LocalDate.now(), "nationality", "code");
		ModelMap modelMap = new ModelMap();

		BindingResult result = Mockito.mock(BindingResult.class);
		Mockito.when(result.hasErrors()).thenReturn(false);
		Mockito.when(providerService.isProviderCodeUnique(Mockito.any(), Mockito.any())).thenReturn(true);

		String retVal = controller.updateProvider(provider, result, modelMap, "a");
		assertTrue(modelMap.containsKey("success"));
		assertSame(Views.SUCCESS.getViewName(), retVal);
	}

	@Test
	void testUpdateProviderBindingErrors() {
		BindingResult result = Mockito.mock(BindingResult.class);
		Mockito.when(result.hasErrors()).thenReturn(true);
		String retVal = controller.updateProvider(new Provider(), result, new ModelMap(), "a");
		assertSame(Views.REGISTRATION.getViewName(), retVal);
	}

	@Test
	void testUpdateProviderNonUniqueProvider() {
		ModelMap modelMap = new ModelMap();
		BindingResult result = Mockito.mock(BindingResult.class);
		Mockito.when(result.hasErrors()).thenReturn(false);
		Mockito.when(providerService.isProviderCodeUnique(Mockito.any(), Mockito.any())).thenReturn(false);

		String retVal = controller.updateProvider(new Provider(), result, modelMap, "a");
		assertSame(Views.REGISTRATION.getViewName(), retVal);
	}

	@Test
	void testDeleteProvider() {
		ModelMap modelMap = new ModelMap();
		Provider provider = new Provider(1, "name", LocalDate.now(), "nationality", "code");
		Mockito.doReturn(new ArrayList<Provider>(List.of(provider))).when(providerService).findAllProviders();

		String retVal = controller.deleteProvider("a", modelMap);
		assertTrue(modelMap.containsKey("providers"));
		@SuppressWarnings("unchecked")
		List<Provider> returnedList = (List<Provider>) modelMap.get("providers");
		assertTrue(returnedList.size() == 1);
		assertTrue(((Provider) returnedList.get(0)).equals(provider));
		assertSame(Views.ALLPROVIDERS.getViewName(), retVal);
	}

	@Test
	void testNewProduct() {
		ModelMap modelMap = new ModelMap();

		String retVal = controller.newProduct(1, modelMap);
		assertTrue(modelMap.containsKey("product"));
		Product product = (Product) modelMap.get("product");
		assertSame(product.getFarmId(), 1);

		assertTrue(modelMap.containsKey("edit"));
		assertFalse((Boolean) modelMap.get("edit"));
		assertSame(Views.PRODUCT.getViewName(), retVal);
	}

	@Test
	void testSaveProduct() {
		ModelMap modelMap = new ModelMap();
		Product product = helperCreateProduct();

		String retVal = controller.saveProduct(product, modelMap);
		assertTrue(modelMap.containsKey("returnPage"));
		assertSame(Views.PRODUCT.getViewName(), (String) modelMap.get("returnPage"));

		assertTrue(modelMap.containsKey("farmId"));
		assertSame(product.getFarmId(), (Integer) modelMap.get("farmId"));

		assertTrue(modelMap.containsKey("success"));
		assertSame(Views.SUCCESS.getViewName(), retVal);
	}

	@Test
	void testGetProducts() {
		ModelMap modelMap = new ModelMap();
		Product product = helperCreateProduct();
		Mockito.when(productService.getProductsByFarmId(Mockito.any()))
				.thenReturn(new ArrayList<Product>(List.of(product)));

		String retVal = controller.getProducts(1, modelMap);
		assertTrue(modelMap.containsKey("products"));
		@SuppressWarnings("unchecked")
		List<Product> returnedList = (List<Product>) modelMap.get("products");
		assertTrue(returnedList.size() == 1);
		assertTrue(((Product) returnedList.get(0)).equals(product));

		assertTrue(modelMap.containsKey("farmId"));
		assertSame(product.getFarmId(), 1);
		assertSame(Views.PRODUCTLIST.getViewName(), retVal);
	}

	@Test
	void testEditProduct() {
		ModelMap modelMap = new ModelMap();
		Product product = helperCreateProduct();
		Mockito.when(productService.findById(Mockito.any())).thenReturn(product);

		String retVal = controller.editProduct(product.getId(), modelMap);
		assertTrue(modelMap.containsKey("product"));
		Product retProduct = (Product) modelMap.get("product");
		assertSame(retProduct, product);

		assertTrue(modelMap.containsKey("edit"));
		assertTrue((Boolean) modelMap.get("edit"));
		assertSame(Views.PRICINGLIST.getViewName(), retVal);
	}

	@Test
	void testUpdateProduct() {
		ModelMap modelMap = new ModelMap();
		ProductAndPrice productAndPrice = new ProductAndPrice();
		productAndPrice.setProduct(new Product());
		productAndPrice.getProduct().setFarmId(1);

		String retVal = controller.updateProduct(productAndPrice, modelMap);
		assertTrue(modelMap.containsKey("returnPage"));
		assertSame(Views.PRODUCT.getViewName(), (String) modelMap.get("returnPage"));

		assertTrue(modelMap.containsKey("farmId"));
		assertSame((Integer) modelMap.get("farmId"), 1);

		assertTrue(modelMap.containsKey("success"));
		assertSame(Views.SUCCESS.getViewName(), retVal);
	}

	@Test
	void testDeleteProduct() {
		ModelMap modelMap = new ModelMap();
		String retVal = controller.deleteProduct(1, 2, modelMap);

		assertTrue(modelMap.containsKey("returnPage"));
		assertSame(Views.PRODUCT.getViewName(), (String) modelMap.get("returnPage"));

		assertTrue(modelMap.containsKey("farmId"));
		assertSame((Integer) modelMap.get("farmId"), 1);

		assertTrue(modelMap.containsKey("success"));
		assertSame(Views.SUCCESS.getViewName(), retVal);
	}

	@Test
	void testNewPricing() {
		Product product = new Product();
		ModelMap modelMap = new ModelMap();
		Mockito.when(productService.findById(Mockito.any())).thenReturn(product);

		String retVal = controller.newPricing(1, modelMap);
		assertTrue(modelMap.containsKey("product"));
		assertSame((Product) modelMap.get("product"), product);

		assertTrue(modelMap.containsKey("pricing"));
		assertSame(Views.PRICING.getViewName(), retVal);
	}

	@Test
	void testSavePricing() {
		ModelMap modelMap = new ModelMap();
		ProductPricing prodPricing = helperCreateProductPricing();

		String retVal = controller.savePricing(prodPricing, modelMap);
		assertTrue(modelMap.containsKey("returnPage"));
		assertSame(Views.PRICING.getViewName(), (String) modelMap.get("returnPage"));

		assertTrue(modelMap.containsKey("prodId"));
		assertSame((Integer) modelMap.get("prodId"), 1);

		assertTrue(modelMap.containsKey("success"));
		assertSame(Views.SUCCESS.getViewName(), retVal);
	}

	@Test
	void testGetPricings() {
		ModelMap modelMap = new ModelMap();
		List<ProductPricing> pricings = new ArrayList<ProductPricing>(List.of(new ProductPricing()));
		Mockito.when(pricingService.findProductPricingsByProductId(Mockito.any())).thenReturn(pricings);

		String retVal = controller.getPricings(1, modelMap);
		assertTrue(modelMap.containsKey("pricings"));
		@SuppressWarnings("unchecked")
		List<ProductPricing> returnedList = (ArrayList<ProductPricing>) modelMap.get("pricings");
		assertTrue(returnedList.size() == 1);
		assertTrue(((ProductPricing) returnedList.get(0)).equals(pricings.get(0)));

		assertSame(Views.PRICINGLIST.getViewName(), retVal);
	}

	@Test
	void testEditPricing() {
		ModelMap modelMap = new ModelMap();
		ProductPricing pricing = new ProductPricing();
		Mockito.when(pricingService.findById(Mockito.any())).thenReturn(pricing);

		String retVal = controller.editPricing(1, modelMap);
		assertTrue(modelMap.containsKey("pricing"));
		assertSame((ProductPricing) modelMap.get("pricing"), pricing);

		assertTrue(modelMap.containsKey("edit"));
		assertTrue((Boolean) modelMap.get("edit"));

		assertSame(Views.PRICING.getViewName(), retVal);
	}

	@Test
	void testUpdatePricing() {
		ProductPricing pricing = helperCreateProductPricing();
		ModelMap modelMap = new ModelMap();

		String retVal = controller.updatePricing(pricing, modelMap);
		assertTrue(modelMap.containsKey("returnPage"));
		assertSame(Views.PRICING.getViewName(), (String) modelMap.get("returnPage"));

		assertTrue(modelMap.containsKey("prodId"));
		assertSame((Integer) modelMap.get("prodId"), 1);

		assertTrue(modelMap.containsKey("success"));
		assertSame(Views.SUCCESS.getViewName(), retVal);
	}

	@Test
	void testDeletePricing() {
		Integer prodId = 1;
		ModelMap modelMap = new ModelMap();

		String retVal = controller.deletePricing(prodId, 1, modelMap);

		assertTrue(modelMap.containsKey("returnPage"));
		assertSame(Views.PRICING.getViewName(), (String) modelMap.get("returnPage"));

		assertTrue(modelMap.containsKey("prodId"));
		assertSame((Integer) modelMap.get("prodId"), prodId);

		assertTrue(modelMap.containsKey("success"));
		assertSame(Views.SUCCESS.getViewName(), retVal);
	}

	private ProductPricing helperCreateProductPricing() {
		ProductPricing pricing = new ProductPricing();
		pricing.setProduct(new Product());
		pricing.getProduct().setId(1);
		return pricing;
	}

	private Product helperCreateProduct() {
		Product product = new Product();
		product.setId(1);
		product.setFarmId(1);
		return product;
	}

}
