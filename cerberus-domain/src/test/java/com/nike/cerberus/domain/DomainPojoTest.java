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

package com.nike.cerberus.domain;

import com.openpojo.reflection.PojoClass;
import com.openpojo.reflection.PojoClassFilter;
import com.openpojo.reflection.impl.PojoClassFactory;
import com.openpojo.validation.Validator;
import com.openpojo.validation.ValidatorBuilder;
import com.openpojo.validation.rule.impl.GetterMustExistRule;
import com.openpojo.validation.rule.impl.SetterMustExistRule;
import com.openpojo.validation.test.impl.GetterTester;
import com.openpojo.validation.test.impl.SetterTester;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class DomainPojoTest {

  @Test
  public void test_pojo_structure_and_behavior() {

    PojoClassFilter pojoClassFilter =
        pojoClass -> !(pojoClass.isNestedClass() && pojoClass.getName().endsWith("Builder"));

    List<PojoClass> pojoClasses =
        PojoClassFactory.getPojoClasses("com.nike.cerberus.domain", pojoClassFilter);

    pojoClasses.remove(PojoClassFactory.getPojoClass(CerberusAuthToken.class));
    pojoClasses.remove(PojoClassFactory.getPojoClass(UserCredentials.class));

    Assert.assertTrue(pojoClasses.size() > 1);

    Validator validator =
        ValidatorBuilder.create()
            .with(new GetterMustExistRule())
            .with(new SetterMustExistRule())
            .with(new SetterTester())
            .with(new GetterTester())
            .build();

    validator.validate(pojoClasses);
  }
}
