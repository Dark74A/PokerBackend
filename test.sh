#!/bin/bash
set -e

echo "=== 1. Create a fresh session ==="
SESSION_RESPONSE=$(curl -s -X POST http://localhost:8080/api/sessions \
  -H "Content-Type: application/json" \
  -d '{"name": "BuyIn Concurrency Test", "description": "stress testing atomic increment"}')
echo "$SESSION_RESPONSE"
SESSION_ID=$(echo "$SESSION_RESPONSE" | grep -o '"sessionId":"[^"]*"' | cut -d'"' -f4)
echo "sessionId: $SESSION_ID"

echo ""
echo "=== 2. Add a player ==="
PLAYER_RESPONSE=$(curl -s -X POST "http://localhost:8080/api/sessions/$SESSION_ID/players" \
  -H "Content-Type: application/json" \
  -d '{"displayName": "Raj"}')
echo "$PLAYER_RESPONSE"
PLAYER_ID=$(echo "$PLAYER_RESPONSE" | grep -o '"playerId":"[^"]*"' | cut -d'"' -f4)
echo "playerId: $PLAYER_ID"

echo ""
echo "=== 3. Fire 5 concurrent buy-ins of 100 each for the SAME player ==="
for i in 1 2 3 4 5; do
  curl -s -X POST "http://localhost:8080/api/sessions/$SESSION_ID/buyins" \
    -H "Content-Type: application/json" \
    -d "{\"playerId\": \"$PLAYER_ID\", \"amount\": 100}" &
done
wait

echo ""
echo "=== 4. Check the projection directly ==="
sudo docker exec -it poker-mongo mongosh poker_ledger --eval "
db.session_projection.findOne({_id: '$SESSION_ID'})
"

echo ""
echo "=== 5. Check the event store — should show 5 BuyInAdded events, versions 3-7 ==="
sudo docker exec -it poker-mongo mongosh poker_ledger --eval "
db.event_store.find({aggregateId: '$SESSION_ID', eventType: 'BuyInAdded'}).sort({version: 1}).pretty()
"
