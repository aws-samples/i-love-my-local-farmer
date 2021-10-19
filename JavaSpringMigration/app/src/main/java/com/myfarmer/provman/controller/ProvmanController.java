package com.myfarmer.provman.controller;

import java.util.List;
import java.util.Locale;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.myfarmer.provman.model.Product;
import com.myfarmer.provman.model.ProductAndPrice;
import com.myfarmer.provman.model.ProductPricing;
import com.myfarmer.provman.model.Provider;
import com.myfarmer.provman.service.ProductPricingService;
import com.myfarmer.provman.service.ProductService;
import com.myfarmer.provman.service.ProviderService;

@Controller
@RequestMapping("/")
public class ProvmanController {

	@Autowired
	ProviderService providerService;

	@Autowired
	ProductService productService;

	@Autowired
	ProductPricingService pricingService;
	
	@Autowired
	MessageSource messageSource;

	/*
	 * List all existing Providers.
	 */
	@RequestMapping(value = { "/", "/provider/list" }, method = RequestMethod.GET)
	public String listProviders(ModelMap model) {
		List<Provider> providers = providerService.findAllProviders();
		model.addAttribute("providers", providers);
		return Views.ALLPROVIDERS.getViewName();
	}

	/*
	 * Add a new Provider.
	 */
	@RequestMapping(value = { "/provider/new" }, method = RequestMethod.GET)
	public String newProvider(ModelMap model) {
		Provider provider = new Provider();
		model.addAttribute("provider", provider);
		model.addAttribute("edit", false);
		return Views.REGISTRATION.getViewName();
	}

	/*
	 * Handling POST request for validating the user input and saving Provider in database.
	 */
	@RequestMapping(value = { "/provider/new" }, method = RequestMethod.POST)
	public String saveProvider(@Valid Provider provider, BindingResult result,
			ModelMap model) {

		if (result.hasErrors()) {
			return Views.REGISTRATION.getViewName();
		}
		
		if(!providerService.isProviderCodeUnique(provider.getId(), provider.getCode())){
			FieldError codeError =new FieldError("Provider","code",messageSource.getMessage("non.unique.code", new String[]{provider.getCode()}, Locale.getDefault()));
		    result.addError(codeError);
			return Views.REGISTRATION.getViewName();
		}
		
		providerService.saveProvider(provider);

		model.addAttribute("success", "Provider " + provider.getName() + " registered successfully.");
		return Views.SUCCESS.getViewName();
	}


	/*
	 * Provide the existing Provider for updating.
	 */
	@RequestMapping(value = { "/edit-{code}-provider" }, method = RequestMethod.GET)
	public String editProvider(@PathVariable String code, ModelMap model) {
		Provider provider = providerService.findProviderByCode(code);
		model.addAttribute("provider", provider);
		model.addAttribute("edit", true);
		return Views.REGISTRATION.getViewName();
	}
	
	/*
	 * Handling POST request for validating the user input and updating Provider in database.
	 */
	@RequestMapping(value = { "/edit-{code}-provider" }, method = RequestMethod.POST)
	public String updateProvider(@Valid Provider provider, BindingResult result,
			ModelMap model, @PathVariable String code) {

		if (result.hasErrors()) {
			return Views.REGISTRATION.getViewName();
		}

		if(!providerService.isProviderCodeUnique(provider.getId(), provider.getCode())){
			FieldError codeError =new FieldError("Provider","code",messageSource.getMessage("non.unique.code", new String[]{provider.getCode()}, Locale.getDefault()));
		    result.addError(codeError);
			return Views.REGISTRATION.getViewName();
		}

		providerService.updateProvider(provider);

		model.addAttribute("success", "Provider " + provider.getName()	+ " updated successfully");
		return Views.SUCCESS.getViewName();
	}

	
	/*
	 * Delete a Provider by it's CODE value.
	 */
	@RequestMapping(value = { "/delete-{code}-provider" }, method = RequestMethod.GET)
	public String deleteProvider(@PathVariable String code, ModelMap model) {
		providerService.deleteProviderByCode(code);
		List<Provider> providers = providerService.findAllProviders();
		model.addAttribute("providers", providers);
		return Views.ALLPROVIDERS.getViewName();
	}

	/*
	 * Create a new Product
	 */
	@RequestMapping(value = {"/product/new/{farmId}"}, method = RequestMethod.GET)
	public String newProduct(@PathVariable Integer farmId, ModelMap modelMap) {
		Product product = new Product();
		product.setFarmId(farmId);
		modelMap.addAttribute("product", product);
		modelMap.addAttribute("edit", false);

		return Views.PRODUCT.getViewName();
	}

	@RequestMapping(value = {"/product/new/{farmId}"}, method = RequestMethod.POST)
	public String saveProduct(@Valid Product product, ModelMap modelMap) {
		productService.saveProduct(product);

		modelMap.addAttribute("returnPage", Views.PRODUCT.getViewName());
		modelMap.addAttribute("farmId", product.getFarmId());
		modelMap.addAttribute("success", "Product " + product.getName() + " saved successfully");

		return Views.SUCCESS.getViewName();
	}

	@RequestMapping(value = {"/product/{farmId}"}, method = RequestMethod.GET)
	public String getProducts(@PathVariable Integer farmId, ModelMap modelMap) {
		List<Product> products = productService.getProductsByFarmId(farmId);
		modelMap.addAttribute("products", products);
		modelMap.addAttribute("farmId", farmId);


		return Views.PRODUCTLIST.getViewName();
	}

	@RequestMapping(value = {"/product/edit/{productId}"}, method = RequestMethod.GET)
	public String editProduct(@PathVariable("productId") Integer productId, ModelMap modelMap) {
		Product product = productService.findById(productId);

		modelMap.addAttribute("product", product);
		modelMap.addAttribute("edit", true);


		return Views.PRICINGLIST.getViewName();
	}

	@RequestMapping(value = {"/product/edit/{productId}/{pricingId}"}, method = RequestMethod.POST)
	public String updateProduct(@Valid ProductAndPrice productAndPrice, ModelMap modelMap) {
		productService.updateProduct(productAndPrice.getProduct());
		pricingService.updateProductPricing(productAndPrice.getPricing());

		modelMap.addAttribute("returnPage", Views.PRODUCT.getViewName());
		modelMap.addAttribute("farmId", productAndPrice.getProduct().getFarmId());
		modelMap.addAttribute("success", "Successfully updated product " +
				productAndPrice.getProduct().getName());

		return Views.SUCCESS.getViewName();
	}

	@RequestMapping(value = {"/product/delete/{farmId}/{id}"}, method = RequestMethod.GET)
	public String deleteProduct(@PathVariable("farmId") Integer farmId, @PathVariable("id") Integer id, ModelMap modelMap) {
		productService.deleteProductById(id);

		modelMap.addAttribute("returnPage", Views.PRODUCT.getViewName());
		modelMap.addAttribute("farmId", farmId);
		modelMap.addAttribute("success", "Successfully deleted product " + id.toString());

		return Views.SUCCESS.getViewName();
	}

	@RequestMapping(value = {"/pricing/new/{prodId}"}, method = RequestMethod.GET)
	public String newPricing(@PathVariable Integer prodId, ModelMap modelMap) {
		ProductPricing productPricing = new ProductPricing();
		Product product = productService.findById(prodId);
		productPricing.setProduct(product);
		modelMap.addAttribute("product", product);
		modelMap.addAttribute("pricing", productPricing);


		return Views.PRICING.getViewName();
	}

	@RequestMapping(value = {"/pricing/new/{prodId}"}, method = RequestMethod.POST)
	public String savePricing(@Valid ProductPricing productPricing, ModelMap modelMap) {
		pricingService.saveProductPricing(productPricing);

		modelMap.addAttribute("returnPage", Views.PRICING.getViewName());
		modelMap.addAttribute("prodId", productPricing.getProduct().getId());
		modelMap.addAttribute("success", "Product pricing saved successfully");

		return Views.SUCCESS.getViewName();
	}

	@RequestMapping(value = {"/pricing/{prodId}"}, method = RequestMethod.GET)
	public String getPricings(@PathVariable Integer prodId, ModelMap modelMap) {
		List<ProductPricing> pricings = pricingService.findProductPricingsByProductId(prodId);

		modelMap.addAttribute("pricings", pricings);


		return Views.PRICINGLIST.getViewName();
	}

	@RequestMapping(value = {"/pricing/edit/{id}"}, method = RequestMethod.GET)
	public String editPricing(@PathVariable Integer id, ModelMap modelMap) {
		ProductPricing pricing = pricingService.findById(id);

		modelMap.addAttribute("pricing", pricing);
		modelMap.addAttribute("edit", true);

		return Views.PRICING.getViewName();
	}

	@RequestMapping(value = {"/pricing/edit/{id}"}, method = RequestMethod.POST)
	public String updatePricing(@Valid ProductPricing pricing, ModelMap modelMap) {
		Integer prodId = pricing.getProduct().getId();
		pricingService.updateProductPricing(pricing);

		modelMap.addAttribute("returnPage", Views.PRICING.getViewName());
		modelMap.addAttribute("prodId", prodId);
		modelMap.addAttribute("success", "Successfully updated pricing " + pricing.getId());

		return Views.SUCCESS.getViewName();
	}

	@RequestMapping(value = "/pricing/delete/{prodId}/{id}", method = RequestMethod.GET)
	public String deletePricing(@PathVariable("prodId") Integer prodId, @PathVariable("id") Integer id, ModelMap modelMap) {
		pricingService.deleteProductPricingById(id);

		modelMap.addAttribute("returnPage", Views.PRICING.getViewName());
		modelMap.addAttribute("prodId", prodId);
		modelMap.addAttribute("success", "Successfully deleted pricing " + id.toString());

		return Views.SUCCESS.getViewName();
	}

	/* Helper endpoint to test environment values for ECS containers, remove in prod deployment */
	@RequestMapping(value = { "/env", }, method = RequestMethod.GET)
	public String environment(ModelMap model) {
		model.addAttribute("username", System.getenv("DB_USERNAME"));
		model.addAttribute("password", System.getenv("DB_PASSWORD"));
		model.addAttribute("endpoint_url", System.getenv("ENDPOINT_URL"));
		return "environment";
	}

}
