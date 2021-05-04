package com.nike.cerberus.dao;

import com.nike.cerberus.mapper.AuthTokenMapper;
import com.nike.cerberus.record.AuthTokenRecord;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class AuthTokenDaoTest {

  @Mock private AuthTokenMapper authTokenMapper;

  @InjectMocks private AuthTokenDao authTokenDao;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testCreteToken() {
    AuthTokenRecord authTokenRecord = Mockito.mock(AuthTokenRecord.class);
    authTokenDao.createAuthToken(authTokenRecord);
    Mockito.verify(authTokenMapper).createAuthToken(authTokenRecord);
  }

  @Test
  public void testGetAuthTokenFromHash() {
    AuthTokenRecord authTokenRecord = Mockito.mock(AuthTokenRecord.class);
    Mockito.when(authTokenMapper.getAuthTokenFromHash("hash")).thenReturn(authTokenRecord);
    Optional<AuthTokenRecord> optionalAuthTokenRecord = authTokenDao.getAuthTokenFromHash("hash");
    Assert.assertEquals(authTokenRecord, optionalAuthTokenRecord.get());
  }

  @Test
  public void testGetAuthTokenFromHashWhenAuthTokenMapperReturnNull() {
    Optional<AuthTokenRecord> optionalAuthTokenRecord = authTokenDao.getAuthTokenFromHash("hash");
    Assert.assertFalse(optionalAuthTokenRecord.isPresent());
  }

  @Test
  public void testDeleteAuthTokenFromHash() {
    authTokenDao.deleteAuthTokenFromHash("hash");
    Mockito.verify(authTokenMapper).deleteAuthTokenFromHash("hash");
  }

  @Test
  public void testDeletedExpiredTokens() {
    Mockito.when(authTokenMapper.deleteExpiredTokens(5)).thenReturn(5);
    authTokenDao.deleteExpiredTokens(10, 5, 0);
    Mockito.verify(authTokenMapper, Mockito.times(2)).deleteExpiredTokens(5);
  }
}
