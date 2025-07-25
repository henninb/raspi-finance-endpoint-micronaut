#!/bin/sh

# API Endpoint Testing Script
# Tests all GET endpoints found in the Micronaut Finance API controllers

# Configuration
BASE_URL="http://localhost:8080"
TIMEOUT=10

# Colors for output removed

# Counters
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Function to test an endpoint
test_endpoint() {
    local method="$1"
    local endpoint="$2"
    local description="$3"
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    
    echo "curl -s -w \"%{http_code}\" -m $TIMEOUT --connect-timeout $TIMEOUT \"$BASE_URL$endpoint\""
    echo -n "Testing: $description [$endpoint] ... "
    response=$(curl -s -w "%{http_code}" -m $TIMEOUT --connect-timeout $TIMEOUT "$BASE_URL$endpoint" 2>/dev/null)
    http_code=$(echo "$response" | tail -c 4)

    if [ -z "$http_code" ]; then
        echo "FAILED (Connection error)"
        FAILED_TESTS=$((FAILED_TESTS + 1))
    elif [ "$http_code" = "200" ] || [ "$http_code" = "201" ]; then
        echo "PASSED ($http_code)"
        PASSED_TESTS=$((PASSED_TESTS + 1))
    elif [ "$http_code" = "404" ]; then
        echo "NOT FOUND ($http_code)"
        FAILED_TESTS=$((FAILED_TESTS + 1))
    elif [ "$http_code" = "401" ] || [ "$http_code" = "403" ]; then
        echo "AUTH REQUIRED ($http_code)"
        PASSED_TESTS=$((PASSED_TESTS + 1))  # Consider auth errors as expected
    else
        echo "FAILED ($http_code)"
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi
}

echo "=========================================="
echo "API Endpoint Testing - Finance Application"
echo "Base URL: $BASE_URL"
echo "=========================================="
echo

# Account Controller Tests
echo "--- Account Controller ---"
test_endpoint "GET" "/account/totals" "Account totals"
test_endpoint "GET" "/account/payment/required" "Payment required accounts"
test_endpoint "GET" "/account/select/active" "All active accounts"
test_endpoint "GET" "/account/select/chase_brian" "Account by name/owner (example)"

# Transaction Controller Tests
echo
echo "--- Transaction Controller ---"
test_endpoint "GET" "/transaction/account/select/chase_brian" "Transactions by account (example)"
test_endpoint "GET" "/transaction/account/totals/chase_brian" "Transaction totals by account (example)"
test_endpoint "GET" "/transaction/select/00000000-0000-0000-0000-000000000000" "Transaction by GUID (example)"
test_endpoint "GET" "/transaction/payment/required" "Payment required transactions"

# Category Controller Tests
echo
echo "--- Category Controller ---"
test_endpoint "GET" "/category/select/active" "All active categories"
test_endpoint "GET" "/category/select/example_category" "Category by name (example)"

# Description Controller Tests
echo
echo "--- Description Controller ---"
test_endpoint "GET" "/description/select/all" "All descriptions"
test_endpoint "GET" "/description/select/example_description" "Description by name (example)"

# Parameter Controller Tests
echo
echo "--- Parameter Controller ---"
test_endpoint "GET" "/parameter/select/example_parameter" "Parameter by name (example)"

# Payment Controller Tests
echo
echo "--- Payment Controller ---"
test_endpoint "GET" "/payment/select" "All payments"

# Pending Transaction Controller Tests
echo
echo "--- Pending Transaction Controller ---"
test_endpoint "GET" "/pending/transaction/all" "All pending transactions"

# Transfer Controller Tests
echo
echo "--- Transfer Controller ---"
test_endpoint "GET" "/transfer/select" "All transfers"

# Validation Amount Controller Tests
echo
echo "--- Validation Amount Controller ---"
test_endpoint "GET" "/validation/amount/select/example_account/cleared" "Validation amount by account/state (example)"

# Excel File Controller Tests
echo
echo "--- Excel File Controller ---"
test_endpoint "GET" "/excel/file/export" "Export Excel file"

# Login Controller Tests
echo
echo "--- Login Controller ---"
test_endpoint "GET" "/api/me" "Current user info"

echo
echo "=========================================="
echo "Test Results Summary"
echo "=========================================="
echo "Total Tests: $TOTAL_TESTS"
echo "Passed: $PASSED_TESTS"
echo "Failed: $FAILED_TESTS"

if [ $FAILED_TESTS -eq 0 ]; then
    echo "All tests passed!"
    exit 0
else
    echo "Some tests failed. Check the API server status and endpoint implementations."
    exit 1
fi
