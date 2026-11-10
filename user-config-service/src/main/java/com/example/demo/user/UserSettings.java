package com.example.demo.user;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_settings")
@Getter
@Setter
@NoArgsConstructor
public class UserSettings {

	public static final int DEFAULT_RESULTS_SIZE = 20;

	public static final int DEFAULT_FONT_SIZE = 12;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false, unique = true)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private User user;

	@Column(name = "results_size", nullable = false)
	private int resultsSize = DEFAULT_RESULTS_SIZE;

	@Column(name = "font_size", nullable = false)
	private int fontSize = DEFAULT_FONT_SIZE;

	/** Ordered; list index is persisted as user_settings_fruit.position. */
	@ManyToMany
	@OrderColumn(name = "position", nullable = false)
	@JoinTable(name = "user_settings_fruit",
			joinColumns = @JoinColumn(name = "user_settings_id"),
			inverseJoinColumns = @JoinColumn(name = "fruit_id"))
	private List<Fruit> fruits = new ArrayList<>();

	/** Created by {@link User}'s constructor; defaults match schema.sql. */
	UserSettings(User user) {
		this.user = user;
	}

}
