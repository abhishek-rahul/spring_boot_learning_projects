#!/bin/bash
for i in {1..40}; do
  curl -s -o /dev/null -w "%{http_code}\n" \
    -H "Content-Type: application/json" \
    -d '{"email":"x@y.com","password":"wrong"}' \
    http://localhost:8080/api/v1/auth/login
done