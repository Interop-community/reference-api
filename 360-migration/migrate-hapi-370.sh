#!/usr/bin/env bash
#
#  * #%L
#  *
#  * %%
#  * Copyright (C) 2014-2019 Healthcare Services Platform Consortium
#  * %%
#  * Licensed under the Apache License, Version 2.0 (the "License");
#  * you may not use this file except in compliance with the License.
#  * You may obtain a copy of the License at
#  *
#  *      http://www.apache.org/licenses/LICENSE-2.0
#  *
#  * Unless required by applicable law or agreed to in writing, software
#  * distributed under the License is distributed on an "AS IS" BASIS,
#  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  * See the License for the specific language governing permissions and
#  * limitations under the License.
#  * #L%
#


MYSQL_USER=$1
MYSQL_PASS=$2
ENVIRONMENT=$3
FULL_NAME=$4
FHIR_VERSION=$5
BEARER_TOKEN=$6
JASYPT_PASSWORD=$7 || ""
HOST="127.0.0.1:3306"

case "${ENVIRONMENT}" in
    local)
        HOST="127.0.0.1:3306"
        ;;
    test)
        HOST="sandboxdb-test.hspconsortium.org"
        ;;
    prod)
        HOST="sandboxdb.hspconsortium.org"
        ;;
esac

#DB_STARTS_WITH="hspc_5"
#
#SQL_STRING="SELECT SCHEMA_NAME AS db FROM information_schema.SCHEMATA WHERE SCHEMA_NAME NOT IN ('mysql', 'information_schema') AND SCHEMA_NAME LIKE 'hspc_5%';"
## Pipe the SQL into mysql
#DBS=$(echo $SQL_STRING | mysql -u$MYSQL_USER -p$MYSQL_PASS -Bs)
#set -f                      # avoid globbing (expansion of *).
#array=($(echo "$DBS" | tr ',' '\n'))
## Display your result
#for FULL_NAME in "${array[@]}"
#do
#    SANDBOX_NAME=${FULL_NAME:7}
#	echo "$SANDBOX_NAME"
#done
SANDBOX_NAME=${FULL_NAME:7}
case "$FHIR_VERSION" in
    dstu2)
        PORT="8075"
        ;;
    stu3)
        PORT="8076"
        ;;
    r4)
        PORT="8077"
        ;;
esac
FHIR_HOST="http://127.0.0.1:$PORT"
case "${ENVIRONMENT}" in
    local)
        FHIR_HOST="http://127.0.0.1:$PORT"
        ;;
    test)
        FHIR_HOST="https://api-v5-$fhirVersion-test.hspconsortium.org"
        ;;
    prod)
        FHIR_HOST="https://api-v5-$fhirVersion.hspconsortium.org"
        ;;
esac

#if [[ ! -z "$(lsof -t -i:$PORT)" ]]; then
#        echo "Killing port $PORT."
#        kill "$(lsof -t -i:${PORT})"
#    fi

./run-fhir-server.sh $FHIR_VERSION $ENVIRONMENT $SANDBOX_NAME $JASYPT_PASSWORD

STARTED=0

OUTPUT=""
until [  $STARTED -eq 1 ]; do
sleep 60
    if [[ ! -z "$(lsof -t -i:$PORT)" ]]; then
        let STARTED=1
    else
        ps ax | grep nameForShutdown | grep -v grep | awk '{print $1}' | xargs kill
        ./run-fhir-server.sh $FHIR_VERSION $ENVIRONMENT $SANDBOX_NAME $JASYPT_PASSWORD
    fi

done

echo "Running server on port $PORT."

echo "curl --header \"Authorization: BEARER ${BEARER_TOKEN}\" \"$FHIR_HOST/$SANDBOX_NAME/data/\$mark-all-resources-for-reindexing\""
STARTED=0
until [  $STARTED -eq 1 ]; do
    if [[ "$(curl -X GET --header "Authorization: BEARER $BEARER_TOKEN" "$FHIR_HOST/$SANDBOX_NAME/data/\$mark-all-resources-for-reindexing")" != *"NullPointerException"* ]]; then
        let STARTED=1
        echo "Successful reindexing connection!"
    fi
    sleep 1
done

FINISHED=0
SQL_STRING="SELECT COUNT(*) FROM $FULL_NAME.HFJ_SPIDX_TOKEN WHERE HASH_IDENTITY IS NULL;"
SQL_STRING2="SELECT COUNT(*) FROM $FULL_NAME.HFJ_RES_REINDEX_JOB;"

until [  $FINISHED -eq 1 ]; do
    if [[ "$(echo $SQL_STRING | mysql -u$MYSQL_USER -p$MYSQL_PASS -Bs)" != "0" && "$(echo $SQL_STRING2 | mysql -u$MYSQL_USER -p$MYSQL_PASS -Bs)" == "0" ]]; then
        curl -X GET --header "Authorization: BEARER ${BEARER_TOKEN}" "$FHIR_HOST/$SANDBOX_NAME/data/\$mark-all-resources-for-reindexing"

    elif [[ "$(echo $SQL_STRING | mysql -u$MYSQL_USER -p$MYSQL_PASS -Bs)" == "0" && "$(echo $SQL_STRING2 | mysql -u$MYSQL_USER -p$MYSQL_PASS -Bs)" == "0" ]]; then
        let FINISHED=1
    fi
    sleep 15
done

#mysql --user="$MYSQL_USER" --password="$MYSQL_PASS" --database="$FULL_NAME" < postReindexing.sql

hapi-fhir-3.7.0-cli/hapi-fhir-cli migrate-database -d MYSQL_5_7 -u "jdbc:mysql://$HOST/$FULL_NAME?serverTimezone=America/Denver" -n "$MYSQL_USER" -p "$MYSQL_PASS" -f V3_4_0 -t V3_7_0

#declare -A my_dict
#FOUND=0
#IFS="$( echo -e '\t' )"
#mysql -u$MYSQL_USER -p$MYSQL_PASS -e "SELECT * FROM $FULL_NAME.HFJ_SPIDX_STRING WHERE HASH_IDENTITY is null;" |
#    while read SP_ID SP_MISSING SP_NAME RES_ID RES_TYPE SP_UPDATED SP_VALUE_EXACT SP_VALUE_NORMALIZED HASH_EXACT HASH_NORM_PREFIX HASH_IDENTITY; do
#
#    for key in ${!my_dict[@]}; do
#        if [[ ${key} == $RES_TYPE && ${my_dict[${key}]} == *",$SP_NAME,"* ]]; then
#            let FOUND=1
#        fi
#    done
#
#    if [[ $FOUND -eq 0 ]]; then
#        my_dict[$RES_TYPE]+=",$SP_NAME,"
#        HASH=$(curl --silent "http://localhost:8076/$SANDBOX_NAME/sandbox/hash/$RES_TYPE,$SP_NAME" --header "Authorization: BEARER ${BEARER_TOKEN}")
#        mysql -u$MYSQL_USER -p$MYSQL_PASS -e "UPDATE $FULL_NAME.HFJ_SPIDX_STRING SET HASH_IDENTITY='$HASH' WHERE RES_TYPE='$RES_TYPE' AND SP_NAME='$SP_NAME';"
#    fi
#    let FOUND=0
#done

echo "Shutting down sever"
ps ax | grep nameForShutdown | grep -v grep | awk '{print $1}' | xargs kill
