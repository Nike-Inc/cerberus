package com.nike.cerberus.controller;

import com.nike.cerberus.domain.Category;
import com.nike.cerberus.service.CategoryService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.util.UriComponentsBuilder;

public class CategoryControllerTest {

  @Mock private CategoryService categoryService;

  @InjectMocks private CategoryController categoryController;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    categoryController = Mockito.spy(categoryController);
  }

  @Test
  public void testCreateCategory() {
    Category category = Mockito.mock(Category.class);
    Authentication authentication = Mockito.mock(Authentication.class);
    Mockito.when(authentication.getName()).thenReturn("name");
    SecurityContextHolder.getContext().setAuthentication(authentication);
    Mockito.when(categoryService.createCategory(Mockito.eq(category), Mockito.anyString()))
        .thenReturn("value");
    UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.newInstance();
    ResponseEntity<Category> response =
        categoryController.createCategory(category, uriComponentsBuilder);
    HttpHeaders responseHeaders = response.getHeaders();
    Assert.assertEquals("/v1/category/value", responseHeaders.get("Location").get(0));
  }

  @Test
  public void testDeleteCategoryWhenDeletionIsSuccessful() {
    Mockito.when(categoryService.deleteCategory("categoryId")).thenReturn(true);
    ResponseEntity<Void> categoryIdResponse = categoryController.deleteCategory("categoryId");
    Assert.assertEquals(HttpStatus.NO_CONTENT, categoryIdResponse.getStatusCode());
  }

  @Test
  public void testDeleteCategoryWhenDeletionIsFailed() {
    Mockito.when(categoryService.deleteCategory("categoryId")).thenReturn(false);
    ResponseEntity<Void> categoryIdResponse = categoryController.deleteCategory("categoryId");
    Assert.assertEquals(HttpStatus.NOT_FOUND, categoryIdResponse.getStatusCode());
  }

  @Test
  public void testGetCategory() {
    Category category = Mockito.mock(Category.class);
    Mockito.when(categoryService.getCategory("categoryId")).thenReturn(Optional.of(category));
    ResponseEntity<Category> categoryResponseEntity = categoryController.getCategory("categoryId");
    Assert.assertEquals(HttpStatus.OK, categoryResponseEntity.getStatusCode());
    Assert.assertEquals(category, categoryResponseEntity.getBody());
  }

  @Test
  public void testGetCategoryWhenCategoryIsNotPresent() {
    Mockito.when(categoryService.getCategory("categoryId")).thenReturn(Optional.empty());
    ResponseEntity<Category> categoryResponseEntity = categoryController.getCategory("categoryId");
    Assert.assertEquals(HttpStatus.NOT_FOUND, categoryResponseEntity.getStatusCode());
  }

  @Test
  public void testListCategories() {
    Category category = Mockito.mock(Category.class);
    List<Category> categoryList = new ArrayList<>();
    categoryList.add(category);
    Mockito.when(categoryService.getAllCategories()).thenReturn(categoryList);
    List<Category> categories = categoryController.listCategories();
    Assert.assertSame(categoryList, categories);
    Assert.assertEquals(categoryList.size(), categories.size());
    Assert.assertSame(categoryList.get(0), categories.get(0));
  }
}
