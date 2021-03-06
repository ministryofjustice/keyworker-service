#!/usr/bin/env bash
echo -e
aws --endpoint-url=http://localhost:4566 sns create-topic --name offender_events
aws --endpoint-url=http://localhost:4566 sqs create-queue --queue-name keyworker_api_queue
aws --endpoint-url=http://localhost:4566 sns subscribe \
    --topic-arn arn:aws:sns:eu-west-2:000000000000:offender_events \
    --protocol sqs \
    --notification-endpoint http://localhost:4566/queue/keyworker_api_queue \
    --attributes '{"FilterPolicy":"{\"eventType\":[\"EXTERNAL_MOVEMENT_RECORD-INSERTED\", \"BOOKING_NUMBER-CHANGED\"]}"}'

aws --endpoint-url=http://localhost:4566 sns create-topic --name complexity_of_need
aws --endpoint-url=http://localhost:4566 sqs create-queue --queue-name complexity_of_need_sqs
aws --endpoint-url=http://localhost:4566 sns subscribe \
    --topic-arn arn:aws:sns:eu-west-2:000000000000:complexity_of_need \
    --protocol sqs \
    --notification-endpoint http://localhost:4566/queue/complexity_of_need_sqs \
    --attributes '{"FilterPolicy":"{\"eventType\":[\"complexity-of-need.level.changed\"]}"}'
