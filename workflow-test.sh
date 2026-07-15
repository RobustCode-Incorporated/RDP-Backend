#!/bin/bash

BASE_URL="http://localhost:8080"

MYSQL_CONTAINER="delivery-mysql"
MYSQL_USER="root"
MYSQL_PASSWORD="root_password"
MYSQL_DB="delivery_db"


echo "Cleaning database..."

docker exec $MYSQL_CONTAINER mysql \
-u $MYSQL_USER \
-p$MYSQL_PASSWORD \
-D $MYSQL_DB \
-e "

SET FOREIGN_KEY_CHECKS=0;

DELETE FROM order_status_history;
DELETE FROM orders;
DELETE FROM deliveries;
DELETE FROM drivers;
DELETE FROM vehicles;
DELETE FROM users;
DELETE FROM restaurants;

SET FOREIGN_KEY_CHECKS=1;

"


echo "====================================="
echo " DELIVERY PLATFORM E2E WORKFLOW TEST "
echo "====================================="



fail()
{
    echo ""
    echo "FAILED ❌"
    exit 1
}



check_response()
{
    RESPONSE="$1"
    STEP="$2"

    if [ -z "$RESPONSE" ]; then
        echo "$STEP ERROR: EMPTY RESPONSE"
        fail
    fi


    if echo "$RESPONSE" | grep -q -E '"error"|Internal Server Error'; then
        echo ""
        echo "$STEP ERROR:"
        echo "$RESPONSE"
        fail
    fi
}



echo ""
echo "1. HEALTH CHECK"
echo "----------------"


HEALTH=$(curl -s $BASE_URL/actuator/health)

echo $HEALTH


echo $HEALTH | grep -q "UP" || fail



echo ""
echo "2. CREATE ADMIN"
echo "----------------"


ADMIN=$(curl -s -X POST \
$BASE_URL/api/auth/register \
-H "Content-Type: application/json" \
-d '
{
"email":"admin@test.com",
"password":"password123",
"firstName":"Admin",
"lastName":"User",
"role":"ADMIN"
}
')


check_response "$ADMIN" "ADMIN CREATION"

echo $ADMIN



echo ""
echo "3. CREATE CUSTOMER"
echo "------------------"


CUSTOMER=$(curl -s -X POST \
$BASE_URL/api/auth/register \
-H "Content-Type: application/json" \
-d '
{
"email":"customer@test.com",
"password":"password123",
"firstName":"Jean",
"lastName":"Customer",
"role":"CUSTOMER"
}
')


check_response "$CUSTOMER" "CUSTOMER CREATION"

echo $CUSTOMER



CUSTOMER_ID=$(echo $CUSTOMER | jq -r '.id')



echo ""
echo "4. CREATE RESTAURANT OWNER + RESTAURANT"
echo "----------------------------------------"



RESTAURANT_USER=$(curl -s -X POST \
$BASE_URL/api/auth/register-restaurant \
-H "Content-Type: application/json" \
-d '
{
"restaurantName":"Pizza House",
"description":"Italian restaurant",
"phoneNumber":"0780000000",
"restaurantEmail":"pizza@test.com",
"address":"Brussels Belgium",
"city":"Brussels",
"country":"Belgium",

"adminEmail":"restaurant@test.com",
"adminPassword":"password123",
"adminFirstName":"Mario",
"adminLastName":"Restaurant"
}
')


check_response "$RESTAURANT_USER" "RESTAURANT CREATION"

echo $RESTAURANT_USER



echo ""
echo "5. CREATE DRIVER USER"
echo "---------------------"



DRIVER_USER=$(curl -s -X POST \
$BASE_URL/api/auth/register \
-H "Content-Type: application/json" \
-d '
{
"email":"driver@test.com",
"password":"password123",
"firstName":"John",
"lastName":"Driver",
"role":"DRIVER"
}
')



check_response "$DRIVER_USER" "DRIVER USER CREATION"

echo $DRIVER_USER


DRIVER_USER_ID=$(echo $DRIVER_USER | jq -r '.id')




echo ""
echo "6. LOGIN ADMIN"
echo "--------------"


ADMIN_LOGIN=$(curl -s -X POST \
$BASE_URL/api/auth/login \
-H "Content-Type: application/json" \
-d '
{
"email":"admin@test.com",
"password":"password123"
}
')


ADMIN_TOKEN=$(echo $ADMIN_LOGIN | jq -r '.token')


[ "$ADMIN_TOKEN" != "null" ] || fail



echo "Admin token OK"



echo ""
echo "7. LOGIN CUSTOMER"
echo "-----------------"



CUSTOMER_LOGIN=$(curl -s -X POST \
$BASE_URL/api/auth/login \
-H "Content-Type: application/json" \
-d '
{
"email":"customer@test.com",
"password":"password123"
}
')


CUSTOMER_TOKEN=$(echo $CUSTOMER_LOGIN | jq -r '.token')


[ "$CUSTOMER_TOKEN" != "null" ] || fail


echo "Customer token OK"




echo ""
echo "8. LOGIN RESTAURANT"
echo "-------------------"



RESTAURANT_LOGIN=$(curl -s -X POST \
$BASE_URL/api/auth/login \
-H "Content-Type: application/json" \
-d '
{
"email":"restaurant@test.com",
"password":"password123"
}
')


RESTAURANT_TOKEN=$(echo $RESTAURANT_LOGIN | jq -r '.token')


[ "$RESTAURANT_TOKEN" != "null" ] || fail



echo "Restaurant token OK"




echo ""
echo "9. LOGIN DRIVER"
echo "---------------"



DRIVER_LOGIN=$(curl -s -X POST \
$BASE_URL/api/auth/login \
-H "Content-Type: application/json" \
-d '
{
"email":"driver@test.com",
"password":"password123"
}
')


DRIVER_TOKEN=$(echo $DRIVER_LOGIN | jq -r '.token')


[ "$DRIVER_TOKEN" != "null" ] || fail


echo "Driver token OK"




echo ""
echo "10. CREATE DRIVER PROFILE"
echo "-------------------------"



DRIVER=$(curl -s -X POST \
$BASE_URL/api/drivers \
-H "Authorization: Bearer $ADMIN_TOKEN" \
-H "Content-Type: application/json" \
-d "
{
\"userId\":$DRIVER_USER_ID,
\"phoneNumber\":\"0788888888\",
\"vehicleType\":\"MOTORBIKE\",
\"vehiclePlate\":\"ABC-123\"
}
")



check_response "$DRIVER" "DRIVER PROFILE CREATION"


echo $DRIVER


DRIVER_ID=$(echo $DRIVER | jq -r '.id')


echo ""
echo "10.5. FORCE DRIVER AVAILABLE (DB PATCH)"
echo "------------------------------------"

# On passe le statut en AVAILABLE directement en base pour permettre l'assignation.
# Note : Si votre enum Java utilise 'AVAILABLE' au lieu de 'ONLINE', remplacez 'ONLINE' ci-dessous.
docker exec $MYSQL_CONTAINER mysql \
-u $MYSQL_USER \
-p$MYSQL_PASSWORD \
-D $MYSQL_DB \
-e "UPDATE drivers SET availability_status = 'AVAILABLE' WHERE id = $DRIVER_ID;"

echo "Driver availability updated to AVAILABLE in Database."


echo ""
echo "11. GET RESTAURANT ID"
echo "---------------------"



RESTAURANT_ID=$(docker exec $MYSQL_CONTAINER mysql \
-u $MYSQL_USER \
-p$MYSQL_PASSWORD \
-D $MYSQL_DB \
-N -e "
SELECT id FROM restaurants LIMIT 1;
")



echo "Restaurant ID=$RESTAURANT_ID"



[ -n "$RESTAURANT_ID" ] || fail




echo ""
echo "12. CREATE ORDER"
echo "----------------"



ORDER=$(curl -s -X POST \
$BASE_URL/api/orders \
-H "Authorization: Bearer $CUSTOMER_TOKEN" \
-H "Content-Type: application/json" \
-d "
{
\"restaurantId\":$RESTAURANT_ID,
\"totalPrice\":25.50,
\"deliveryAddress\":\"Kigali City\"
}
")



check_response "$ORDER" "ORDER CREATION"


echo $ORDER


ORDER_ID=$(echo $ORDER | jq -r '.id')




echo ""
echo "13. RESTAURANT ACCEPT ORDER"
echo "---------------------------"


RES_ACCEPT=$(curl -s -X PATCH \
$BASE_URL/api/orders/$ORDER_ID/accept \
-H "Authorization: Bearer $RESTAURANT_TOKEN")

check_response "$RES_ACCEPT" "RESTAURANT ACCEPT ORDER"
echo $RES_ACCEPT



echo ""
echo "14. PREPARE ORDER"
echo "-----------------"


RES_PREPARE=$(curl -s -X PATCH \
$BASE_URL/api/orders/$ORDER_ID/prepare \
-H "Authorization: Bearer $RESTAURANT_TOKEN")

check_response "$RES_PREPARE" "PREPARE ORDER"
echo $RES_PREPARE



echo ""
echo "15. READY ORDER"
echo "---------------"


RES_READY=$(curl -s -X PATCH \
$BASE_URL/api/orders/$ORDER_ID/ready \
-H "Authorization: Bearer $RESTAURANT_TOKEN")

check_response "$RES_READY" "READY ORDER"
echo $RES_READY



echo ""
echo "16. ASSIGN DRIVER"
echo "-----------------"


ADMIN_ASSIGN=$(curl -s -X PATCH \
$BASE_URL/api/orders/$ORDER_ID/assign-driver/$DRIVER_ID \
-H "Authorization: Bearer $ADMIN_TOKEN")

check_response "$ADMIN_ASSIGN" "ASSIGN DRIVER"
echo $ADMIN_ASSIGN



echo ""
echo "17. DRIVER PICKUP"
echo "-----------------"


DRIVER_PICKUP=$(curl -s -X PATCH \
$BASE_URL/api/orders/$ORDER_ID/pickup \
-H "Authorization: Bearer $DRIVER_TOKEN")

check_response "$DRIVER_PICKUP" "DRIVER PICKUP"
echo $DRIVER_PICKUP



echo ""
echo "18. DRIVER DELIVER"
echo "------------------"


DRIVER_DELIVER=$(curl -s -X PATCH \
$BASE_URL/api/orders/$ORDER_ID/deliver \
-H "Authorization: Bearer $DRIVER_TOKEN")

check_response "$DRIVER_DELIVER" "DRIVER DELIVER"
echo $DRIVER_DELIVER



echo ""
echo "19. DATABASE CHECK"
echo "------------------"



docker exec $MYSQL_CONTAINER mysql \
-u $MYSQL_USER \
-p$MYSQL_PASSWORD \
-D $MYSQL_DB \
-e "

SELECT COUNT(*) users FROM users;

SELECT COUNT(*) restaurants FROM restaurants;

SELECT COUNT(*) drivers FROM drivers;

SELECT COUNT(*) vehicles FROM vehicles;

SELECT COUNT(*) orders FROM orders;


SELECT 
id,
order_id,
old_status,
new_status,
changed_by
FROM order_status_history
ORDER BY id;


SELECT *
FROM orders;

"



echo ""
echo "====================================="
echo " WORKFLOW COMPLETED SUCCESSFULLY "
echo "====================================="