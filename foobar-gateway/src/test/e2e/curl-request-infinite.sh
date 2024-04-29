while true; do
  for((i=0; i<30; i++)); do
    curl -X GET localhost:9000/healthcheck/ready -H "Content-Type: application/json" -H "USER-ID:'A'";
  done
done