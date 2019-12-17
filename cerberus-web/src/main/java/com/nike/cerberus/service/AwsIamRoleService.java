/*
 * Copyright (c) 2019 Nike, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.nike.cerberus.service;

import static com.nike.cerberus.service.AuthenticationService.SYSTEM_USER;

import com.nike.cerberus.dao.AwsIamRoleDao;
import com.nike.cerberus.record.AwsIamRoleRecord;
import com.nike.cerberus.util.DateTimeSupplier;
import com.nike.cerberus.util.UuidSupplier;
import java.time.OffsetDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AwsIamRoleService {

  private final AwsIamRoleDao awsIamRoleDao;
  private final UuidSupplier uuidSupplier;
  private final DateTimeSupplier dateTimeSupplier;

  @Autowired
  public AwsIamRoleService(
      AwsIamRoleDao awsIamRoleDao, UuidSupplier uuidSupplier, DateTimeSupplier dateTimeSupplier) {

    this.awsIamRoleDao = awsIamRoleDao;
    this.uuidSupplier = uuidSupplier;
    this.dateTimeSupplier = dateTimeSupplier;
  }

  @Transactional
  public AwsIamRoleRecord createIamRole(String iamPrincipalArn) {
    String iamRoleId = uuidSupplier.get();
    OffsetDateTime dateTime = dateTimeSupplier.get();
    AwsIamRoleRecord awsIamRoleRecord = new AwsIamRoleRecord();
    awsIamRoleRecord.setId(iamRoleId);
    awsIamRoleRecord.setAwsIamRoleArn(iamPrincipalArn);
    awsIamRoleRecord.setCreatedBy(SYSTEM_USER);
    awsIamRoleRecord.setLastUpdatedBy(SYSTEM_USER);
    awsIamRoleRecord.setCreatedTs(dateTime);
    awsIamRoleRecord.setLastUpdatedTs(dateTime);
    awsIamRoleDao.createIamRole(awsIamRoleRecord);

    return awsIamRoleRecord;
  }
}
