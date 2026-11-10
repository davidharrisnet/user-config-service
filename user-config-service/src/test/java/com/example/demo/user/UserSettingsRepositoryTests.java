package com.example.demo.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import jakarta.persistence.EntityManager;

@DataJpaTest(properties = "spring.datasource.url=jdbc:sqlite:build/test.db?foreign_keys=true")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(UserService.class)
class UserSettingsRepositoryTests {

	@Autowired
	UserRepository users;

	@Autowired
	UserSettingsRepository settings;

	@Autowired
	FruitRepository fruits;

	@Autowired
	EntityManager em;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	UserService userService;

	@Test
	void newUserGetsDefaultSettings() {
		User erin = userService.createUser("erin", "erin@example.com");
		users.flush();

		assertThat(jdbc.queryForMap(
				"SELECT results_size, font_size FROM user_settings WHERE user_id = ?", erin.getId()))
			.containsEntry("results_size", 20)
			.containsEntry("font_size", 12);

		assertThat(jdbc.queryForList("""
				SELECT f.name
				FROM user_settings s
				JOIN user_settings_fruit usf ON usf.user_settings_id = s.id
				JOIN fruit f ON f.id = usf.fruit_id
				WHERE s.user_id = ?
				ORDER BY usf.position
				""", String.class, erin.getId()))
			.containsExactly("apple", "pear", "banana", "strawberry", "melon", "orange");
	}

	@Test
	void insertsUserWithSettings() {
		User dave = new User("dave", "dave@example.com");
		dave.getSettings().setResultsSize(50);
		dave.getSettings().setFontSize(16);
		dave.getSettings().getFruits().addAll(fruitsInOrder(4L, 5L, 2L)); // strawberry, melon, pear
		users.saveAndFlush(dave); // settings are saved with the user

		// Check the rows actually written, bypassing JPA.
		assertThat(jdbc.queryForMap("SELECT username, email FROM user WHERE id = ?", dave.getId()))
			.containsEntry("username", "dave")
			.containsEntry("email", "dave@example.com");

		assertThat(jdbc.queryForMap(
				"SELECT results_size, font_size FROM user_settings WHERE user_id = ?", dave.getId()))
			.containsEntry("results_size", 50)
			.containsEntry("font_size", 16);

		assertThat(jdbc.queryForList("""
				SELECT f.name
				FROM user_settings s
				JOIN user_settings_fruit usf ON usf.user_settings_id = s.id
				JOIN fruit f ON f.id = usf.fruit_id
				WHERE s.user_id = ?
				ORDER BY usf.position
				""", String.class, dave.getId()))
			.containsExactly("strawberry", "melon", "pear");
	}

	@Test
	void rejectsUserWithoutUsername() {
		assertThatThrownBy(() -> users.saveAndFlush(new User(null, "nobody@example.com")))
			.isInstanceOf(DataAccessException.class);

		assertThatThrownBy(() -> jdbc.update(
				"INSERT INTO user (username, email, created_at) VALUES (NULL, 'x@example.com', CURRENT_TIMESTAMP)"))
			.isInstanceOf(DataAccessException.class)
			.hasMessageContaining("NOT NULL constraint failed: user.username");
	}

	@Test
	void rejectsUserWithoutEmail() {
		assertThatThrownBy(() -> users.saveAndFlush(new User("noemail", null)))
			.isInstanceOf(DataAccessException.class);

		assertThatThrownBy(() -> jdbc.update(
				"INSERT INTO user (username, email, created_at) VALUES ('noemail', NULL, CURRENT_TIMESTAMP)"))
			.isInstanceOf(DataAccessException.class)
			.hasMessageContaining("NOT NULL constraint failed: user.email");
	}

	@Test
	void fruitTableIsSeeded() {
		assertThat(fruits.findAll()).extracting(Fruit::getName)
			.containsExactlyInAnyOrder("apple", "pear", "banana", "strawberry", "melon", "orange");
	}

	@Test
	void savesSettingsWithFruitsInOrder() {
		User alice = new User("alice", "alice@example.com");
		alice.getSettings().setResultsSize(25);
		alice.getSettings().setFontSize(14);
		alice.getSettings().getFruits().addAll(fruitsInOrder(6L, 1L, 3L));
		users.saveAndFlush(alice);
		em.clear();

		UserSettings loaded = settings.findByUserId(alice.getId()).orElseThrow();
		assertThat(loaded.getResultsSize()).isEqualTo(25);
		assertThat(loaded.getFontSize()).isEqualTo(14);
		assertThat(loaded.getFruits()).extracting(Fruit::getName)
			.containsExactly("orange", "apple", "banana");
	}

	@Test
	void reorderingFruitsIsPersisted() {
		User carol = new User("carol", "carol@example.com");
		carol.getSettings().getFruits().addAll(fruitsInOrder(1L, 2L, 3L));
		users.saveAndFlush(carol);
		em.clear();

		UserSettings loaded = settings.findByUserId(carol.getId()).orElseThrow();
		loaded.getFruits().clear();
		loaded.getFruits().addAll(fruitsInOrder(3L, 1L));
		settings.saveAndFlush(loaded);
		em.clear();

		assertThat(settings.findByUserId(carol.getId()).orElseThrow().getFruits())
			.extracting(Fruit::getName).containsExactly("banana", "apple");
	}

	@Test
	void allowsOnlyOneSettingsRowPerUser() {
		User bob = users.saveAndFlush(new User("bob", "bob@example.com"));

		assertThatThrownBy(() -> settings.saveAndFlush(new UserSettings(bob)))
			.isInstanceOf(DataAccessException.class)
			.hasMessageContaining("UNIQUE constraint failed");
	}

	/** findAllById does not preserve argument order, so look each one up. */
	private List<Fruit> fruitsInOrder(Long... ids) {
		return Arrays.stream(ids).map(fruits::getReferenceById).toList();
	}

}
