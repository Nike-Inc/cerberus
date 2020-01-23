/*
 * Copyright (c) 2020 Nike, inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nike.cerberus.controller;

import static com.nike.cerberus.security.CerberusPrincipal.ROLE_ADMIN;
import static com.nike.cerberus.security.CerberusPrincipal.ROLE_USER;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.*;

import com.nike.cerberus.domain.Category;
import com.nike.cerberus.service.CategoryService;
import java.util.List;
import javax.annotation.security.RolesAllowed;
import javax.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Validated
@RestController
@RequestMapping("/v1/category")
public class CategoryController {

  private final CategoryService categoryService;

  @Autowired
  public CategoryController(CategoryService categoryService) {
    this.categoryService = categoryService;
  }

  @RolesAllowed(ROLE_ADMIN)
  @RequestMapping(method = POST, consumes = APPLICATION_JSON_VALUE)
  public ResponseEntity<Category> createCategory(
      @Valid @RequestBody Category category, UriComponentsBuilder b) {
    String id =
        categoryService.createCategory(
            category, SecurityContextHolder.getContext().getAuthentication().getName());
    UriComponents uriComponents = b.path("/v1/category/{id}").buildAndExpand(id);
    return ResponseEntity.created(uriComponents.toUri()).build();
  }

  @RolesAllowed(ROLE_ADMIN)
  @RequestMapping(value = "/{categoryId:.+}", method = DELETE)
  public ResponseEntity<Void> deleteCategory(@PathVariable String categoryId) {
    boolean isDeleted = categoryService.deleteCategory(categoryId);
    return ResponseEntity.status(isDeleted ? HttpStatus.NO_CONTENT : HttpStatus.NOT_FOUND).build();
  }

  @RolesAllowed(ROLE_USER)
  @RequestMapping(value = "/{categoryId:.+}", method = GET)
  public ResponseEntity<Category> getCategory(@PathVariable String categoryId) {
    return ResponseEntity.of(categoryService.getCategory(categoryId));
  }

  @RolesAllowed(ROLE_USER)
  @RequestMapping(method = GET)
  public List<Category> listCategories() {
    return categoryService.getAllCategories();
  }
}
