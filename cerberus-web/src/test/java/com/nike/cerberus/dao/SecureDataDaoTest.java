package com.nike.cerberus.dao;

import com.nike.cerberus.domain.SecureDataType;
import com.nike.cerberus.domain.Source;
import com.nike.cerberus.mapper.SecureDataMapper;
import com.nike.cerberus.record.DataKeyInfo;
import com.nike.cerberus.record.SecureDataRecord;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class SecureDataDaoTest {

  @Mock private SecureDataMapper secureDataMapper;

  @InjectMocks private SecureDataDao secureDataDao;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testUpdateSecureData() {
    SecureDataRecord secureDataRecord = getSecureDataRecord();
    Mockito.when(secureDataMapper.updateSecureData(secureDataRecord)).thenReturn(5);
    int response = secureDataDao.updateSecureData(secureDataRecord);
    Assert.assertEquals(5, response);
  }

  @Test
  public void testReadSecureDataByPath() {
    SecureDataRecord secureDataRecord = getSecureDataRecord();
    Mockito.when(secureDataMapper.readSecureDataByPath("sdbId", "path"))
        .thenReturn(secureDataRecord);
    Optional<SecureDataRecord> optionalSecureDataRecord =
        secureDataDao.readSecureDataByPath("sdbId", "path");
    Assert.assertTrue(optionalSecureDataRecord.isPresent());
    Assert.assertSame(secureDataRecord, optionalSecureDataRecord.get());
  }

  @Test
  public void testReadSecureDataByPathWhenReadByPathReturnsNull() {
    Optional<SecureDataRecord> optionalSecureDataRecord =
        secureDataDao.readSecureDataByPath("sdbId", "path");
    Assert.assertFalse(optionalSecureDataRecord.isPresent());
  }

  @Test
  public void testReadSecureDataByIdLocking() {
    SecureDataRecord secureDataRecord = getSecureDataRecord();
    Mockito.when(secureDataMapper.readSecureDataByIdLocking("6")).thenReturn(secureDataRecord);
    Optional<SecureDataRecord> optionalSecureDataRecord =
        secureDataDao.readSecureDataByIdLocking("6");
    Assert.assertTrue(optionalSecureDataRecord.isPresent());
    Assert.assertSame(secureDataRecord, optionalSecureDataRecord.get());
  }

  @Test
  public void testReadSecureDataByIdLockingWhenSecureDataByIdIsNotPresent() {
    Optional<SecureDataRecord> optionalSecureDataRecord =
        secureDataDao.readSecureDataByIdLocking("6");
    Assert.assertFalse(optionalSecureDataRecord.isPresent());
  }

  @Test
  public void testReadSecureDataByPathAndType() {
    SecureDataRecord secureDataRecord = getSecureDataRecord();
    Mockito.when(secureDataMapper.readSecureDataByPathAndType("sdbId", "path", SecureDataType.FILE))
        .thenReturn(secureDataRecord);
    Optional<SecureDataRecord> optionalSecureDataRecord =
        secureDataDao.readSecureDataByPathAndType("sdbId", "path", SecureDataType.FILE);
    Assert.assertTrue(optionalSecureDataRecord.isPresent());
    Assert.assertSame(secureDataRecord, optionalSecureDataRecord.get());
  }

  @Test
  public void testReadSecureDataByPathAndTypeWhenReadSecureDataByPathAndTypeIsNotPresent() {
    Optional<SecureDataRecord> optionalSecureDataRecord =
        secureDataDao.readSecureDataByPathAndType("sdbId", "path", SecureDataType.FILE);
    Assert.assertFalse(optionalSecureDataRecord.isPresent());
  }

  @Test
  public void testReadMetadataByPathAndType() {
    SecureDataRecord secureDataRecord = getSecureDataRecord();
    Mockito.when(secureDataMapper.readMetadataByPathAndType("sdbId", "path", SecureDataType.FILE))
        .thenReturn(secureDataRecord);
    Optional<SecureDataRecord> optionalSecureDataRecord =
        secureDataDao.readMetadataByPathAndType("sdbId", "path", SecureDataType.FILE);
    Assert.assertTrue(optionalSecureDataRecord.isPresent());
    Assert.assertSame(secureDataRecord, optionalSecureDataRecord.get());
  }

  @Test
  public void testReadMetadataByPathAndTypeWhenReadMetadataByPathAndTypeIsNotPresent() {
    Optional<SecureDataRecord> optionalSecureDataRecord =
        secureDataDao.readMetadataByPathAndType("sdbId", "path", SecureDataType.FILE);
    Assert.assertFalse(optionalSecureDataRecord.isPresent());
  }

  @Test
  public void testGetPathsByPartialPath() {
    String[] paths = new String[3];
    Mockito.when(secureDataMapper.getPathsByPartialPath("sdbId", "partialPath")).thenReturn(paths);
    String[] pathsByPartialPath = secureDataDao.getPathsByPartialPath("sdbId", "partialPath");
    Assert.assertSame(paths, pathsByPartialPath);
  }

  @Test
  public void testGetPathsByPartialPathAndType() {
    String[] paths = new String[3];
    Mockito.when(
            secureDataMapper.getPathsByPartialPathAndType(
                "sdbId", "partialPath", SecureDataType.FILE))
        .thenReturn(paths);
    String[] pathsByPartialPath =
        secureDataDao.getPathsByPartialPathAndType("sdbId", "partialPath", SecureDataType.FILE);
    Assert.assertSame(paths, pathsByPartialPath);
  }

  @Test
  public void testGetPathsBySdbId() {
    Set<String> paths = new HashSet<>();
    paths.add("path1");
    Mockito.when(secureDataMapper.getPathsBySdbId("sdbId")).thenReturn(paths);
    Set<String> actualPaths = secureDataDao.getPathsBySdbId("sdbId");
    Assert.assertEquals(paths, actualPaths);
  }

  @Test
  public void testListSecureDataByPartialPathAndType() {
    SecureDataRecord secureDataRecord = getSecureDataRecord();
    List<SecureDataRecord> secureDataRecords = new ArrayList<>();
    secureDataRecords.add(secureDataRecord);
    Mockito.when(
            secureDataMapper.listSecureDataByPartialPathAndType(
                "sdbId", "partialPath", SecureDataType.FILE, 0, 0))
        .thenReturn(secureDataRecords);
    List<SecureDataRecord> actualSecureDataRecords =
        secureDataDao.listSecureDataByPartialPathAndType(
            "sdbId", "partialPath", SecureDataType.FILE, 0, 0);
    Assert.assertEquals(secureDataRecords, actualSecureDataRecords);
  }

  @Test
  public void testCountByPartialPathAndType() {
    Mockito.when(secureDataMapper.countByPartialPathAndType("partialPath", SecureDataType.FILE))
        .thenReturn(6);
    int partialPathCount =
        secureDataDao.countByPartialPathAndType("partialPath", SecureDataType.FILE);
    Assert.assertEquals(6, partialPathCount);
  }

  @Test
  public void testCountByType() {
    Mockito.when(secureDataMapper.countByType(SecureDataType.FILE)).thenReturn(7);
    int countByType = secureDataDao.countByType(SecureDataType.FILE);
    Assert.assertEquals(7, countByType);
  }

  @Test
  public void testGetTotalNumberOfDataNodes() {
    Mockito.when(secureDataMapper.getTotalNumberOfDataNodes()).thenReturn(8);
    int totalNumberOfDataNodes = secureDataDao.getTotalNumberOfDataNodes();
    Assert.assertEquals(8, totalNumberOfDataNodes);
  }

  @Test
  public void testDeleteAllSecretsThatStartWithGivenPartialPath() {
    secureDataDao.deleteAllSecretsThatStartWithGivenPartialPath("sdbId", "partialPath");
    Mockito.verify(secureDataMapper)
        .deleteAllSecretsThatStartWithGivenPartialPath("sdbId", "partialPath");
  }

  @Test
  public void testDeleteSecret() {
    secureDataDao.deleteSecret("sdbId", "path");
    Mockito.verify(secureDataMapper).deleteSecret("sdbId", "path");
  }

  @Test
  public void testGetSumTopLevelKeyValuePairs() {
    Mockito.when(secureDataMapper.getSumTopLevelKeyValuePairs()).thenReturn(9);
    int sumTopLevelKeyValuePairs = secureDataDao.getSumTopLevelKeyValuePairs();
    Assert.assertEquals(9, sumTopLevelKeyValuePairs);
  }

  @Test
  public void testGetSumTopLevelKeyValuePairsWhenSumIsNull() {
    int sumTopLevelKeyValuePairs = secureDataDao.getSumTopLevelKeyValuePairs();
    Assert.assertEquals(0, sumTopLevelKeyValuePairs);
  }

  @Test
  public void testGetOldestDataKeyInfo() {
    DataKeyInfo dataKeyInfo = getDataKeyInfo();
    List<DataKeyInfo> dataKeyInfoList = new ArrayList<>();
    dataKeyInfoList.add(dataKeyInfo);
    Mockito.when(secureDataMapper.getOldestDataKeyInfo(OffsetDateTime.MAX, 1))
        .thenReturn(dataKeyInfoList);
    List<DataKeyInfo> oldestDataKeyInfoList =
        secureDataDao.getOldestDataKeyInfo(OffsetDateTime.MAX, 1);
    Assert.assertEquals(dataKeyInfoList, oldestDataKeyInfoList);
  }

  @Test
  public void testWriteSecureData() {
    SecureDataRecord secureDataRecord = getSecureDataRecord();
    secureDataDao.writeSecureData(
        "sdbBoxId",
        "path",
        "blob".getBytes(StandardCharsets.UTF_8),
        2,
        SecureDataType.FILE,
        1,
        "user",
        OffsetDateTime.MAX,
        "user",
        OffsetDateTime.MAX);
    secureDataRecord.setId("path".hashCode());
    Mockito.verify(secureDataMapper).writeSecureData(secureDataRecord);
  }

  @Test
  public void testUpdateSecureDataWithParameters() {
    SecureDataRecord secureDataRecord = getSecureDataRecord();
    secureDataDao.updateSecureData(
        "sdbBoxId",
        "path",
        "blob".getBytes(StandardCharsets.UTF_8),
        2,
        SecureDataType.FILE,
        1,
        "user",
        OffsetDateTime.MAX,
        "user",
        OffsetDateTime.MAX,
        OffsetDateTime.MAX);
    secureDataRecord.setId("path".hashCode());
    Mockito.verify(secureDataMapper).updateSecureData(secureDataRecord);
  }

  private DataKeyInfo getDataKeyInfo() {
    DataKeyInfo dataKeyInfo =
        new DataKeyInfo()
            .setId("id")
            .setLastRotatedTs(OffsetDateTime.MAX)
            .setSource(Source.SECURE_DATA);
    return dataKeyInfo;
  }

  private SecureDataRecord getSecureDataRecord() {
    SecureDataRecord secureDataRecord =
        new SecureDataRecord()
            .setId(1)
            .setSdboxId("sdbBoxId")
            .setPath("path")
            .setEncryptedBlob("blob".getBytes(StandardCharsets.UTF_8))
            .setType(SecureDataType.FILE)
            .setSizeInBytes(1)
            .setTopLevelKVCount(2)
            .setCreatedBy("user")
            .setCreatedTs(OffsetDateTime.MAX)
            .setLastUpdatedBy("user")
            .setLastUpdatedTs(OffsetDateTime.MAX)
            .setLastRotatedTs(OffsetDateTime.MAX);
    return secureDataRecord;
  }
}
