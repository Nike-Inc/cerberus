package com.nike.cerberus.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nike.cerberus.domain.SecureData;
import com.nike.cerberus.domain.SecureDataResponse;
import com.nike.cerberus.domain.SecureDataType;
import com.nike.cerberus.domain.SecureDataVersion;
import com.nike.cerberus.security.CerberusPrincipal;
import com.nike.cerberus.service.SecureDataService;
import com.nike.cerberus.service.SecureDataVersionService;
import com.nike.cerberus.util.SdbAccessRequest;
import java.util.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class SecureDataControllerTest {

  @Mock private SecureDataService secureDataService;
  @Mock private SecureDataVersionService secureDataVersionService;
  @Mock private SdbAccessRequest sdbAccessRequest;
  @InjectMocks private SecureDataController secureDataController;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testReadSecureDataWhenSecureDataIsNotPresent() {
    Mockito.when(sdbAccessRequest.getSdbId()).thenReturn("sdbId");
    Mockito.when(sdbAccessRequest.getPath()).thenReturn("path");
    Mockito.when(secureDataService.readSecret("sdbId", "path")).thenReturn(Optional.empty());
    ResponseEntity<?> responseEntity = secureDataController.readSecureData();
    Assert.assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
  }

  @Test
  public void testReadSecureData() {
    Mockito.when(sdbAccessRequest.getSdbId()).thenReturn("sdbId");
    Mockito.when(sdbAccessRequest.getPath()).thenReturn("path");
    SecureData secureData = Mockito.mock(SecureData.class);
    ObjectNode objectNode = new ObjectMapper().createObjectNode();
    objectNode.put("key", "value");
    Mockito.when(secureData.getData()).thenReturn(objectNode.toString());
    Mockito.when(secureDataService.readSecret("sdbId", "path")).thenReturn(Optional.of(secureData));
    Map<String, String> metadata = new HashMap<>();
    Mockito.when(secureDataService.parseSecretMetadata(secureData)).thenReturn(metadata);
    ResponseEntity<?> responseEntity = secureDataController.readSecureData();
    Assert.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    SecureDataResponse secureDataResponse = (SecureDataResponse) responseEntity.getBody();
    Assert.assertSame(metadata, secureDataResponse.getMetadata());
    Assert.assertNotNull(secureDataResponse.getRequestId());
    Assert.assertEquals(objectNode.toString(), secureDataResponse.getData().toString());
  }

  @Test
  public void testListKeys() {
    Mockito.when(sdbAccessRequest.getSdbId()).thenReturn("sdbId");
    Mockito.when(sdbAccessRequest.getPath()).thenReturn("path");
    Set<String> keys = new HashSet<>();
    keys.add("key");
    Mockito.when(secureDataService.listKeys("sdbId", "path")).thenReturn(keys);
    ResponseEntity<?> responseEntity = secureDataController.listKeys("true");
    Assert.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    SecureDataResponse secureDataResponse = (SecureDataResponse) responseEntity.getBody();
    Map<String, Set<String>> data = (Map<String, Set<String>>) secureDataResponse.getData();
    Assert.assertSame(keys, data.get("keys"));
    Assert.assertEquals(keys, data.get("keys"));
  }

  @Test
  public void testListKeysWhenFalse() {
    Mockito.when(sdbAccessRequest.getSdbId()).thenReturn("sdbId");
    Mockito.when(sdbAccessRequest.getPath()).thenReturn("path");
    Mockito.when(secureDataService.readSecret("sdbId", "path")).thenReturn(Optional.empty());
    ResponseEntity<?> responseEntity = secureDataController.listKeys("false");
    Assert.assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
  }

  @Test
  public void testReadSecureDataVersionWhenDataIsNotPresentByVersionId() {
    Mockito.when(sdbAccessRequest.getSdbId()).thenReturn("sdbId");
    Mockito.when(sdbAccessRequest.getPath()).thenReturn("path");
    Mockito.when(sdbAccessRequest.getCategory()).thenReturn("category");
    Mockito.when(
            secureDataVersionService.getSecureDataVersionById(
                "sdbId", "versionId", "category", "path"))
        .thenReturn(Optional.empty());
    ResponseEntity<?> responseEntity = secureDataController.readSecureDataVersion("versionId");
    Assert.assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
  }

  @Test
  public void testReadSecureDataVersion() {
    Mockito.when(sdbAccessRequest.getSdbId()).thenReturn("sdbId");
    Mockito.when(sdbAccessRequest.getPath()).thenReturn("path");
    Mockito.when(sdbAccessRequest.getCategory()).thenReturn("category");
    SecureDataVersion secureDataVersion = Mockito.mock(SecureDataVersion.class);
    ObjectNode objectNode = new ObjectMapper().createObjectNode();
    objectNode.put("key", "value");
    Mockito.when(secureDataVersion.getData()).thenReturn(objectNode.toString());
    Map<String, String> metadata = new HashMap<>();
    Mockito.when(secureDataVersionService.parseVersionMetadata(secureDataVersion))
        .thenReturn(metadata);
    Mockito.when(
            secureDataVersionService.getSecureDataVersionById(
                "sdbId", "versionId", "category", "path"))
        .thenReturn(Optional.of(secureDataVersion));
    ResponseEntity<?> responseEntity = secureDataController.readSecureDataVersion("versionId");
    Assert.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    SecureDataResponse secureDataResponse = (SecureDataResponse) responseEntity.getBody();
    Assert.assertSame(metadata, secureDataResponse.getMetadata());
    Assert.assertNotNull(secureDataResponse.getRequestId());
    Assert.assertEquals(objectNode.toString(), secureDataResponse.getData().toString());
  }

  @Test
  public void testWriteSecureDataWhenBodyIsNullInHttpEntity() {
    HttpEntity<String> httpEntity = new HttpEntity<>(new HttpHeaders());
    String exceptionMessage = "";
    try {
      secureDataController.writeSecureData(httpEntity);
    } catch (RuntimeException e) {
      exceptionMessage = e.getMessage();
    }
    Assert.assertEquals("The body must not be null", exceptionMessage);
  }

  @Test
  public void testWriteSecureData() {
    Mockito.when(sdbAccessRequest.getSdbId()).thenReturn("sdbId");
    Mockito.when(sdbAccessRequest.getPath()).thenReturn("path");
    CerberusPrincipal cerberusPrincipal = Mockito.mock(CerberusPrincipal.class);
    Mockito.when(sdbAccessRequest.getPrincipal()).thenReturn(cerberusPrincipal);
    Mockito.when(cerberusPrincipal.getName()).thenReturn("name");
    HttpEntity<String> httpEntity = new HttpEntity<>("body");
    secureDataController.writeSecureData(httpEntity);
    Mockito.verify(secureDataService).writeSecret("sdbId", "path", "body", "name");
  }

  @Test
  public void testDeleteSecureData() {
    Mockito.when(sdbAccessRequest.getSdbId()).thenReturn("sdbId");
    Mockito.when(sdbAccessRequest.getPath()).thenReturn("path");
    CerberusPrincipal cerberusPrincipal = Mockito.mock(CerberusPrincipal.class);
    Mockito.when(sdbAccessRequest.getPrincipal()).thenReturn(cerberusPrincipal);
    Mockito.when(cerberusPrincipal.getName()).thenReturn("name");
    secureDataController.deleteSecureData();
    Mockito.verify(secureDataService).deleteSecret("sdbId", "path", SecureDataType.OBJECT, "name");
  }
}
