#!/usr/bin/env bash
# Exercises: register -> create session -> add player -> add buy-in
# Captures every id/token along the way so you can check history_projections.
#
# Requires: curl, jq
# Usage:    ./test_history_flow.sh [base_url]

set -euo pipefail

BASE_URL="${1:-http://localhost:8080}"
OUT_FILE="test-run-vars.sh"

command -v jq >/dev/null 2>&1 || { echo "jq is required (e.g. 'brew install jq' / 'apt install jq')"; exit 1; }

TS="$(date +%s)"
USERNAME="testuser_${TS}"
EMAIL="testuser_${TS}@example.com"
PASSWORD="password123"

echo "== 1. Register test user ($USERNAME) =="
REGISTER_RESPONSE=$(curl -s -X POST "$BASE_URL/api/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"username\": \"${USERNAME}\", \"email\": \"${EMAIL}\", \"password\": \"${PASSWORD}\"}")

echo "$REGISTER_RESPONSE"
TOKEN=$(echo "$REGISTER_RESPONSE" | jq -r '.token')
USER_ID=$(echo "$REGISTER_RESPONSE" | jq -r '.userId')

if [[ "$TOKEN" == "null" || -z "$TOKEN" ]]; then
  echo "Registration failed, aborting. Response above."
  exit 1
fi

echo
echo "== 2. Create test session =="
SESSION_RESPONSE=$(curl -s -X POST "$BASE_URL/api/sessions" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"name": "History Test Session", "description": "checking the audit feed"}')

echo "$SESSION_RESPONSE"
SESSION_ID=$(echo "$SESSION_RESPONSE" | jq -r '.sessionId')

if [[ "$SESSION_ID" == "null" || -z "$SESSION_ID" ]]; then
  echo "Session creation failed, aborting. Response above."
  exit 1
fi

echo
echo "== 3. Add player =="
PLAYER_RESPONSE=$(curl -s -X POST "$BASE_URL/api/sessions/${SESSION_ID}/players" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"displayName": "Raj"}')

echo "$PLAYER_RESPONSE"
PLAYER_ID=$(echo "$PLAYER_RESPONSE" | jq -r '.playerId')

if [[ "$PLAYER_ID" == "null" || -z "$PLAYER_ID" ]]; then
  echo "Add player failed, aborting. Response above."
  exit 1
fi

echo
echo "== 4. Add buy-in for player =="
BUYIN_RESPONSE=$(curl -s -X POST "$BASE_URL/api/sessions/${SESSION_ID}/buyins" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d "{\"playerId\": \"${PLAYER_ID}\", \"amount\": 500}")

echo "$BUYIN_RESPONSE"
BUYIN_ID=$(echo "$BUYIN_RESPONSE" | jq -r '.buyInId')

echo
echo "== 5. Fetch session summary =="
curl -s "$BASE_URL/api/sessions/${SESSION_ID}" \
  -H "Authorization: Bearer ${TOKEN}" | jq '.'

# Save everything to a sourceable file for reuse in later runs
cat > "$OUT_FILE" <<EOF
# Generated $(date). Run: source ${OUT_FILE}
export BASE_URL="${BASE_URL}"
export USERNAME="${USERNAME}"
export TOKEN="${TOKEN}"
export USER_ID="${USER_ID}"
export SESSION_ID="${SESSION_ID}"
export PLAYER_ID="${PLAYER_ID}"
export BUYIN_ID="${BUYIN_ID}"
EOF

echo
echo "=================================================="
echo "Summary (also saved to ./${OUT_FILE}, source it to reuse):"
echo "  username:  $USERNAME"
echo "  userId:    $USER_ID"
echo "  token:     $TOKEN"
echo "  sessionId: $SESSION_ID"
echo "  playerId:  $PLAYER_ID"
echo "  buyInId:   $BUYIN_ID"
echo "=================================================="
echo
echo "Now check in mongosh (connected to poker_ledger):"
echo "  db.history_projections.find({ sessionId: \"${SESSION_ID}\" })"
