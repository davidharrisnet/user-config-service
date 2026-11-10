package com.example.demo.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Reference data seeded by data.sql; ids are fixed, not generated. Fruits are retired by
 * setting active to false, never deleted.
 */
@Entity
@Table(name = "fruit")
@Getter
@NoArgsConstructor
public class Fruit {

	@Id
	private Long id;

	@Column(nullable = false, unique = true)
	private String name;

	@Column(nullable = false)
	private boolean active;

}
