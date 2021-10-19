package com.myfarmer.provman.model;

import java.time.LocalDate;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.format.annotation.DateTimeFormat;

@Entity
@Table(name="provider")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Provider {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@Size(min=3, max=50)
	@Column(name = "name", nullable = false)
	private String name;

	@NotNull
	@DateTimeFormat(pattern="dd/MM/yyyy") 
	@Column(name = "entering_date", nullable = false)
	private LocalDate enteringDate;

	@NotNull
	@Column(name = "nationality", nullable = false)
	private String nationality;
	
	@NotEmpty
	@Column(name = "code", unique=true, nullable = false)
	private String code;

}
