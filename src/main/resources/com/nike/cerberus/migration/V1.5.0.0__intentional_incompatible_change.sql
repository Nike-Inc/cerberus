#
# This is a dummy migration script.  It was created to introduce an intentional incompatible
# change in the database between the new and old Cerberus architecture.  This will cause CMS deployment
# to fail if someone simply tries to deploy the next generation of a CMS AMI over the previous
# generation instead of using the correct migration path (e.g. standing up a whole new environment,
# copying the data over, updating DNS).
#
# For local development, you can get past the introduced issue by dropping all of your data with the
# gradle command `./gradlew flywayClean`.
#
# More here:
# https://flywaydb.org/documentation/gradle/
#
# Next generation of Cerberus architecture being released in 2018.
#