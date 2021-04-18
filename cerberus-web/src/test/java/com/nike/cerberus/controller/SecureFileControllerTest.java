package com.nike.cerberus.controller;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.domain.SecureDataType;
import com.nike.cerberus.domain.SecureFileCurrent;
import com.nike.cerberus.domain.SecureFileSummary;
import com.nike.cerberus.domain.SecureFileVersion;
import com.nike.cerberus.error.DefaultApiError;
import com.nike.cerberus.security.CerberusPrincipal;
import com.nike.cerberus.service.SecureDataService;
import com.nike.cerberus.service.SecureDataVersionService;
import com.nike.cerberus.util.SdbAccessRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
import org.springframework.web.multipart.MultipartFile;

public class SecureFileControllerTest {

  @Mock private SecureDataService secureDataService;
  @Mock private SecureDataVersionService secureDataVersionService;
  @Mock private SdbAccessRequest sdbAccessRequest;

  @InjectMocks private SecureFileController secureFileController;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testHeadSecureFileWhenFileMetadataIsNotPresent() {
    Mockito.when(sdbAccessRequest.getSdbId()).thenReturn("sdbId");
    Mockito.when(sdbAccessRequest.getPath()).thenReturn("path");
    Mockito.when(secureDataService.readFileMetadataOnly("sdbId", "path"))
        .thenReturn(Optional.empty());
    ApiError apiError = null;
    try {
      secureFileController.headSecureFile();
    } catch (ApiException apiException) {
      apiError = apiException.getApiErrors().get(0);
    }
    Assert.assertEquals(DefaultApiError.ENTITY_NOT_FOUND, apiError);
  }

  @Test
  public void testHeadSecureFile() {
    Mockito.when(sdbAccessRequest.getSdbId()).thenReturn("sdbId");
    Mockito.when(sdbAccessRequest.getPath()).thenReturn("path");
    SecureFileSummary secureFileSummary = Mockito.mock(SecureFileSummary.class);
    Mockito.when(secureFileSummary.getName()).thenReturn("sample.txt");
    Mockito.when(secureDataService.readFileMetadataOnly("sdbId", "path"))
        .thenReturn(Optional.of(secureFileSummary));
    ResponseEntity<Void> voidResponseEntity = secureFileController.headSecureFile();
    Assert.assertEquals(HttpStatus.OK, voidResponseEntity.getStatusCode());
    HttpHeaders httpHeaders = voidResponseEntity.getHeaders();
    Assert.assertEquals("text/plain", httpHeaders.get("Content-Type").get(0));
    Assert.assertEquals("0", httpHeaders.get("Content-Length").get(0));
    Assert.assertEquals(
        "attachment; filename=\"sample.txt\"", httpHeaders.get("Content-Disposition").get(0));
  }

  @Test
  public void testGetSecureFileWhenFileIsNotPresent() {
    Mockito.when(sdbAccessRequest.getSdbId()).thenReturn("sdbId");
    Mockito.when(sdbAccessRequest.getPath()).thenReturn("path");
    Mockito.when(secureDataService.readFile("sdbId", "path")).thenReturn(Optional.empty());
    ApiError apiError = null;
    try {
      secureFileController.getSecureFile();
    } catch (ApiException apiException) {
      apiError = apiException.getApiErrors().get(0);
    }
    Assert.assertEquals(DefaultApiError.ENTITY_NOT_FOUND, apiError);
  }

  @Test
  public void testGetSecureFile() {
    Mockito.when(sdbAccessRequest.getSdbId()).thenReturn("sdbId");
    Mockito.when(sdbAccessRequest.getPath()).thenReturn("path");
    SecureFileCurrent secureFileCurrent = Mockito.mock(SecureFileCurrent.class);
    Mockito.when(secureFileCurrent.getName()).thenReturn("sample.txt");
    Mockito.when(secureFileCurrent.getData()).thenReturn("data".getBytes(StandardCharsets.UTF_8));
    Mockito.when(secureDataService.readFile("sdbId", "path"))
        .thenReturn(Optional.of(secureFileCurrent));
    ResponseEntity<?> responseEntity = secureFileController.getSecureFile();
    Assert.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    HttpHeaders httpHeaders = responseEntity.getHeaders();
    Assert.assertEquals("text/plain", httpHeaders.get("Content-Type").get(0));
    Assert.assertEquals(
        "attachment; filename=\"sample.txt\"", httpHeaders.get("Content-Disposition").get(0));
  }

  @Test
  public void testGetSecureFileVersionWhenFileIsNotFound() {
    Mockito.when(sdbAccessRequest.getSdbId()).thenReturn("sdbId");
    Mockito.when(sdbAccessRequest.getPath()).thenReturn("path");
    Mockito.when(sdbAccessRequest.getCategory()).thenReturn("category");
    Mockito.when(
            secureDataVersionService.getSecureFileVersionById(
                "sdbId", "versionId", "category", "path"))
        .thenReturn(Optional.empty());
    ApiError apiError = null;
    try {
      secureFileController.getSecureFileVersion("versionId");
    } catch (ApiException apiException) {
      apiError = apiException.getApiErrors().get(0);
    }
    Assert.assertEquals(DefaultApiError.ENTITY_NOT_FOUND, apiError);
  }

  @Test
  public void testGetSecureFileVersion() {
    Mockito.when(sdbAccessRequest.getSdbId()).thenReturn("sdbId");
    Mockito.when(sdbAccessRequest.getPath()).thenReturn("path");
    Mockito.when(sdbAccessRequest.getCategory()).thenReturn("category");
    SecureFileVersion secureFileVersion = Mockito.mock(SecureFileVersion.class);
    Mockito.when(secureFileVersion.getName()).thenReturn("sample.txt");
    Mockito.when(secureFileVersion.getData()).thenReturn("data".getBytes(StandardCharsets.UTF_8));
    Mockito.when(
            secureDataVersionService.getSecureFileVersionById(
                "sdbId", "versionId", "category", "path"))
        .thenReturn(Optional.of(secureFileVersion));

    ResponseEntity<?> responseEntity = secureFileController.getSecureFileVersion("versionId");
    Assert.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    HttpHeaders httpHeaders = responseEntity.getHeaders();
    Assert.assertEquals("text/plain", httpHeaders.get("Content-Type").get(0));
    Assert.assertEquals(
        "attachment; filename=\"sample.txt\"", httpHeaders.get("Content-Disposition").get(0));
  }

  @Test
  public void testDeleteSecureFile() {
    Mockito.when(sdbAccessRequest.getSdbId()).thenReturn("sdbId");
    Mockito.when(sdbAccessRequest.getPath()).thenReturn("path");
    CerberusPrincipal cerberusPrincipal = Mockito.mock(CerberusPrincipal.class);
    Mockito.when(sdbAccessRequest.getPrincipal()).thenReturn(cerberusPrincipal);
    Mockito.when(cerberusPrincipal.getName()).thenReturn("name");
    secureFileController.deleteSecureFile();
    Mockito.verify(secureDataService).deleteSecret("sdbId", "path", SecureDataType.FILE, "name");
  }

  @Test
  public void testWriteSecureFile() throws IOException {
    Mockito.when(sdbAccessRequest.getSdbId()).thenReturn("sdbId");
    Mockito.when(sdbAccessRequest.getPath()).thenReturn("path");
    CerberusPrincipal cerberusPrincipal = Mockito.mock(CerberusPrincipal.class);
    Mockito.when(sdbAccessRequest.getPrincipal()).thenReturn(cerberusPrincipal);
    Mockito.when(cerberusPrincipal.getName()).thenReturn("name");
    MultipartFile multipartFile = Mockito.mock(MultipartFile.class);
    byte[] data = "data".getBytes(StandardCharsets.UTF_8);
    Mockito.when(multipartFile.getBytes()).thenReturn(data);
    secureFileController.writeSecureFile(multipartFile);
    Mockito.verify(secureDataService).writeSecureFile("sdbId", "path", data, data.length, "name");
  }

  @Test
  public void testWriteSecureFileThrowsException() throws IOException {

    MultipartFile multipartFile = Mockito.mock(MultipartFile.class);
    String message = "no data";
    Mockito.when(multipartFile.getBytes()).thenThrow(new IOException(message));
    ApiException apiException = null;

    try {
      secureFileController.writeSecureFile(multipartFile);
    } catch (ApiException e) {

      apiException = e;
    }
    Assert.assertEquals("Failed to get contents from multipart file", apiException.getMessage());
  }
}
