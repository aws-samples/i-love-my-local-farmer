package com.myfarmer.provman.model;

import lombok.Data;
import org.springframework.stereotype.Component;

import javax.validation.Valid;

@Component
@Data
public class ProductAndPrice {

  @Valid
  private Product product;

  @Valid
  private ProductPricing pricing;
}
