package com.siemens.internship;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(ItemController.class)
class InternshipApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private ItemService itemService;

	@Autowired
	private ObjectMapper objectMapper;

	private List<Item> testItems;
	private Item item;

	@BeforeEach
	public void setUp() {
		item = new Item(1L, "Test item", "Test", "SUBMITTED", "test@gmail.com");
		Item testItem2 = new Item(2L, "Next item", "Test 2", "SUBMITTED", "other@gmail.com");
		testItems = Arrays.asList(item, testItem2);
	}

	@Test
	public void getAllItems_testIfTheDatabaseIsNotEmpty() throws Exception {
		when(itemService.findAll()).thenReturn(testItems);

		mockMvc.perform(get("/api/items"))
				.andExpect(status().isOk())
				.andExpect((ResultMatcher) content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$[0].name").value("Test item"))
				.andExpect(jsonPath("$[1].name").value("Next item"));

		verify(itemService, times(1)).findAll();
	}

	@Test
	public void addNewItems_testIfNewValuesCanBeAdded_() throws Exception {
		Item itemToCreate = new Item(null, "New Item", "New Description", "NEW", "new@example.com");
		Item createdItem = new Item(3L, "New Item", "New Description", "NEW", "new@example.com");

		when(itemService.save(any(Item.class))).thenReturn(createdItem);

		mockMvc.perform(post("/api/items")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(itemToCreate)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(3))
				.andExpect(jsonPath("$.name").value("New Item"));

		verify(itemService, times(1)).save(any(Item.class));
	}

	@Test
	public void addNewItems_testWrongEmail() throws Exception {
		Item invalidItem = new Item(null, "", "", "", "invalid-email");

		mockMvc.perform(post("/api/items")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(invalidItem)))
				.andExpect(status().isBadRequest());

		verify(itemService, never()).save(any(Item.class));
	}

	@Test
	void getItem_whenExists() throws Exception {
		when(itemService.findById(1L)).thenReturn(Optional.of(item));

		mockMvc.perform(get("/api/items/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1L))
				.andExpect(jsonPath("$.name").value("New Item"));

		verify(itemService, times(1)).findById(1L);
	}

	@Test
	void getItem_whenNotExists() throws Exception {
		when(itemService.findById(10L)).thenReturn(Optional.empty());

		mockMvc.perform(get("/api/items/10"))
				.andExpect(status().isNotFound());

		verify(itemService, times(1)).findById(10L);
	}

	@Test
	void updateItem_testWhenItemExists() throws Exception {
		Item itemToUpdate = new Item(1L, "Updated Item", "Updated", "UPDATED", "updated@gmail.com");

		when(itemService.findById(1L)).thenReturn(Optional.of(item));
		when(itemService.save(any(Item.class))).thenReturn(itemToUpdate);

		mockMvc.perform(put("/api/items/1")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(itemToUpdate)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.name").value("Updated Item"))
				.andExpect(jsonPath("$.status").value("UPDATED"));

		verify(itemService, times(1)).findById(1L);
		verify(itemService, times(1)).save(any(Item.class));
	}

	@Test
	void deleteItem_testWhenItemExists() throws Exception {
		when(itemService.findById(1L)).thenReturn(Optional.of(item));
		doNothing().when(itemService).deleteById(1L);

		mockMvc.perform(delete("/api/items/1"))
				.andExpect(status().isOk());

		verify(itemService, times(1)).findById(1L);
		verify(itemService, times(1)).deleteById(1L);
	}

}
