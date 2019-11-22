package com.nike.cerberus.config.database;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.beans.PropertyVetoException;
import java.util.Properties;

import static java.util.Optional.ofNullable;

/**
 * This is a port of the C3p0DataSourceProvider from the MyBaits Guice package.
 * There might be a better way to do this in Spring land with the Spring Mybatis package, however...
 * We are doing this to ensure matching behavior when porting CMS from Guice -> Spring Context, as we have battle tested that config.
 *
 * This creates the C3P0 DataSource bean.
 */
@Configuration
public class C3p0DataSourceConfiguration {

  /**
   * The ComboPooledDataSource reference.
   */
  private final ComboPooledDataSource dataSource = new ComboPooledDataSource();
  private String username;
  private String password;

  @Autowired
  public C3p0DataSourceConfiguration(@Value("${jdbc.driver}") String driver,
                                     @Value("${jdbc.url}") String url) {

    try {
      dataSource.setDriverClass(driver);
    } catch (PropertyVetoException e) {
      throw new RuntimeException(
        "Impossible to initialize C3P0 Data Source with driver class '" + driver + "', see nested exceptions", e);
    }
    dataSource.setJdbcUrl(url);
  }

  /**
   * Sets the user.
   *
   * @param username the new user
   */
  @Autowired
  public void setUser(@Value("${jdbc.username:#{null}}") final String username) {
    this.username = username;
  }

  /**
   * Sets the password.
   *
   * @param password the new password
   */
  @Autowired
  public void setPassword(@Value("${jdbc.password:#{null}}") final String password) {
    this.password = password;
  }

  /**
   * Sets the acquire increment.
   *
   * @param acquireIncrement the new acquire increment
   */
  @Autowired
  public void setAcquireIncrement(@Value("${c3p0.acquireIncrement:#{null}}") final Integer acquireIncrement) {
    ofNullable(acquireIncrement).ifPresent(dataSource::setAcquireIncrement);
  }

  /**
   * Sets the acquire retry attempts.
   *
   * @param acquireRetryAttempts the new acquire retry attempts
   */
  @Autowired
  public void setAcquireRetryAttempts(@Value("${c3p0.acquireRetryAttempts:#{null}}") final Integer acquireRetryAttempts) {
    ofNullable(acquireRetryAttempts).ifPresent(dataSource::setAcquireRetryAttempts);
  }

  /**
   * Sets the acquire retry delay.
   *
   * @param acquireRetryDelay the new acquire retry delay
   */
  @Autowired
  public void setAcquireRetryDelay(@Value("${c3p0.acquireRetryDelay:#{null}}") final Integer acquireRetryDelay) {
    ofNullable(acquireRetryDelay).ifPresent(dataSource::setAcquireRetryDelay);
  }

  /**
   * Sets the auto commit on close.
   *
   * @param autoCommit the new auto commit on close
   */
  @Autowired
  public void setAutoCommitOnClose(@Value("${jdbc.autoCommit:#{null}}") final Boolean autoCommit) {
    ofNullable(autoCommit).ifPresent(dataSource::setAutoCommitOnClose);
  }

  /**
   * Sets the driver properties.
   *
   * @param driverProperties the new driver properties
   */
  @Autowired
  public void setDriverProperties(@Value("${jdbc.driverProperties:#{null}}") final Properties driverProperties) {
    ofNullable(driverProperties).ifPresent(dataSource::setProperties);
  }

  /**
   * Sets the aautomatic test table.
   *
   * @param automaticTestTable the new aautomatic test table
   */
  @Autowired
  public void setAautomaticTestTable(@Value("${c3p0.automaticTestTable:#{null}}") final String automaticTestTable) {
    ofNullable(automaticTestTable).ifPresent(dataSource::setAutomaticTestTable);
  }

  /**
   * Sets the break after acquire failure.
   *
   * @param breakAfterAcquireFailure the new break after acquire failure
   */
  @Autowired
  public void setBreakAfterAcquireFailure(
    @Value("${c3p0.breakAfterAcquireFailure:#{null}}") final Boolean breakAfterAcquireFailure) {
    ofNullable(breakAfterAcquireFailure).ifPresent(dataSource::setBreakAfterAcquireFailure);
  }

  /**
   * Sets the checkout timeout.
   *
   * @param checkoutTimeout the new checkout timeout
   */
  @Autowired
  public void setCheckoutTimeout(@Value("${c3p0.checkoutTimeout:#{null}}") final Integer checkoutTimeout) {
    ofNullable(checkoutTimeout).ifPresent(dataSource::setCheckoutTimeout);
  }

  /**
   * Sets the connection customizer class name.
   *
   * @param connectionCustomizerClassName the new connection customizer class name
   */
  @Autowired
  public void setConnectionCustomizerClassName(
    @Value("${c3p0.connectionCustomizerClassName:#{null}}") final String connectionCustomizerClassName) {
    ofNullable(connectionCustomizerClassName).ifPresent(dataSource::setConnectionCustomizerClassName);
  }

  /**
   * Sets the connection tester class name.
   *
   * @param connectionTesterClassName the new connection tester class name
   */
  @Autowired
  public void setConnectionTesterClassName(
    @Value("${c3p0.connectionTesterClassName:#{null}}") final String connectionTesterClassName) {
      ofNullable(connectionTesterClassName).ifPresent(className -> {
        try {
          dataSource.setConnectionTesterClassName(className);
        } catch (PropertyVetoException e) {
          throw new RuntimeException("Impossible to set C3P0 Data Source connection tester class name '"
            + connectionTesterClassName + "', see nested exceptions", e);
        }
      });
  }

  /**
   * Sets the idle connection test period.
   *
   * @param idleConnectionTestPeriod the new idle connection test period
   */
  @Autowired
  public void setIdleConnectionTestPeriod(@Value("${c3p0.idleConnectionTestPeriod:#{null}}") final Integer idleConnectionTestPeriod) {
    ofNullable(idleConnectionTestPeriod).ifPresent(dataSource::setIdleConnectionTestPeriod);
  }

  /**
   * Sets the initial pool size.
   *
   * @param initialPoolSize the new initial pool size
   */
  @Autowired
  public void setInitialPoolSize(@Value("${c3p0.initialPoolSize:#{null}}") final Integer initialPoolSize) {
    ofNullable(initialPoolSize).ifPresent(dataSource::setInitialPoolSize);
  }

  /**
   * Sets the max administrative task time.
   *
   * @param maxAdministrativeTaskTime the new max administrative task time
   */
  @Autowired
  public void setMaxAdministrativeTaskTime(
    @Value("${c3p0.maxAdministrativeTaskTime:#{null}}") final Integer maxAdministrativeTaskTime) {
    ofNullable(maxAdministrativeTaskTime).ifPresent(dataSource::setMaxAdministrativeTaskTime);
  }

  /**
   * Sets the max connection age.
   *
   * @param maxConnectionAge the new max connection age
   */
  @Autowired
  public void setMaxConnectionAge(@Value("${c3p0.maxConnectionAge:#{null}}") final Integer maxConnectionAge) {
    ofNullable(maxConnectionAge).ifPresent(dataSource::setMaxConnectionAge);
  }

  /**
   * Sets the max idle time.
   *
   * @param maxIdleTime the new max idle time
   */
  @Autowired
  public void setMaxIdleTime(@Value("${c3p0.maxIdleTime:#{null}}") final Integer maxIdleTime) {
    ofNullable(maxIdleTime).ifPresent(dataSource::setMaxIdleTime);
  }

  /**
   * Sets the max idle time excess connections.
   *
   * @param maxIdleTimeExcessConnections the new max idle time excess connections
   */
  @Autowired
  public void setMaxIdleTimeExcessConnections(
    @Value("${c3p0.maxIdleTimeExcessConnections:#{null}}") final Integer maxIdleTimeExcessConnections) {
    ofNullable(maxIdleTimeExcessConnections).ifPresent(dataSource::setMaxIdleTimeExcessConnections);
  }

  /**
   * Sets the max pool size.
   *
   * @param maxPoolSize the new max pool size
   */
  @Autowired
  public void setMaxPoolSize(@Value("${c3p0.maxPoolSize:#{null}}") final Integer maxPoolSize) {
    ofNullable(maxPoolSize).ifPresent(dataSource::setMaxPoolSize);
  }

  /**
   * Sets the max statements.
   *
   * @param maxStatements the new max statements
   */
  @Autowired
  public void setMaxStatements(@Value("${c3p0.maxStatements:#{null}}") final Integer maxStatements) {
    ofNullable(maxStatements).ifPresent(dataSource::setMaxStatements);
  }

  /**
   * Sets the max statements per connection.
   *
   * @param maxStatementsPerConnection the new max statements per connection
   */
  @Autowired
  public void setMaxStatementsPerConnection(
    @Value("${c3p0.maxStatementsPerConnection:#{null}}") final Integer maxStatementsPerConnection) {
    ofNullable(maxStatementsPerConnection).ifPresent(dataSource::setMaxStatementsPerConnection);
  }

  /**
   * Sets the min pool size.
   *
   * @param minPoolSize the new min pool size
   */
  @Autowired
  public void setMinPoolSize(@Value("${c3p0.minPoolSize:#{null}}") final Integer minPoolSize) {
    ofNullable(minPoolSize).ifPresent(dataSource::setMinPoolSize);
  }

  /**
   * Sets the preferred test query.
   *
   * @param preferredTestQuery the new preferred test query
   */
  @Autowired
  public void setPreferredTestQuery(@Value("${c3p0.preferredTestQuery:#{null}}") final String preferredTestQuery) {
    ofNullable(preferredTestQuery).ifPresent(dataSource::setPreferredTestQuery);
  }

  /**
   * Sets the property cycle.
   *
   * @param propertyCycle the new property cycle
   */
  @Autowired
  public void setPropertyCycle(@Value("${c3p0.propertyCycle:#{null}}") final Integer propertyCycle) {
    ofNullable(propertyCycle).ifPresent(dataSource::setPropertyCycle);
  }

  /**
   * Sets the test connection on checkin.
   *
   * @param testConnectionOnCheckin the new test connection on checkin
   */
  @Autowired
  public void setTestConnectionOnCheckin(@Value("${c3p0.testConnectionOnCheckin:#{null}}") final Boolean testConnectionOnCheckin) {
    ofNullable(testConnectionOnCheckin).ifPresent(dataSource::setTestConnectionOnCheckin);
  }

  /**
   * Sets the test connection on checkout.
   *
   * @param testConnectionOnCheckout the new test connection on checkout
   */
  @Autowired
  public void setTestConnectionOnCheckout(
    @Value("${c3p0.testConnectionOnCheckout:#{null}}") final Boolean testConnectionOnCheckout) {
    ofNullable(testConnectionOnCheckout).ifPresent(dataSource::setTestConnectionOnCheckout);
  }

  /**
   * Sets the unreturned connection timeout.
   *
   * @param unreturnedConnectionTimeout the new unreturned connection timeout
   */
  @Autowired
  public void setUnreturnedConnectionTimeout(
    @Value("${c3p0.unreturnedConnectionTimeout:#{null}}") final Integer unreturnedConnectionTimeout) {
    ofNullable(unreturnedConnectionTimeout).ifPresent(dataSource::setUnreturnedConnectionTimeout);
  }

  /**
   * Sets the uses traditional reflective proxies.
   *
   * @param usesTraditionalReflectiveProxies the new uses traditional reflective proxies
   */
  @Autowired
  public void setUsesTraditionalReflectiveProxies(
    @Value("${c3p0.usesTraditionalReflectiveProxies:#{null}}") final Boolean usesTraditionalReflectiveProxies) {
    ofNullable(usesTraditionalReflectiveProxies).ifPresent(dataSource::setUsesTraditionalReflectiveProxies);
  }

  @Bean
  public DataSource dataSource() {
    ofNullable(username).ifPresent(dataSource::setUser);
    ofNullable(password).ifPresent(dataSource::setPassword);
    return dataSource;
  }
}
