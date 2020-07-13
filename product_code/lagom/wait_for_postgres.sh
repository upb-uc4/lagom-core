until psql -h "localhost:5432" -U "admin" -c '\q'; do
  sleep 10
done