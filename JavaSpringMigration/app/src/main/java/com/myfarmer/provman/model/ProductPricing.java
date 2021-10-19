package com.myfarmer.provman.model;

import lombok.Data;
import lombok.ToString;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

@Entity
@Table(name = "pricing")
@Data
public class ProductPricing {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private int id;

  @NotNull
  @Column(name = "price", nullable = false)
  private float price;

  @NotNull
  @Column(name = "weight", nullable = false)
  private float weight;

  @ManyToOne
  @JoinColumn(name = "product_id")
  @ToString.Exclude	// prevent stackoverflow from circular dependency
  private Product product;
}
