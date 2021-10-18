---
layout: documentation
title: Limitations
---

# Key / Value Store

Cerberus is designed for storing application secrets such as passwords, API keys, and certificates.  It is not
meant to be a general purpose Key/Value store for storing any kind of data. It is not a replacement for applications 
like Cassandra, DynamoDB, or Reddis.

# Request Body Size

When writing data to Cerberus the request body size should be less than 256 KB.

# KMS Keys

Cerberus lazily creates a KMS key for every unique configured IAM role the first time they authenticate.
By default Amazon limits the number of KMS keys per region per account to 1000. 